package org.finos.symphony.toolkit.koreai.spring;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.finos.springbot.symphony.stream.StreamEventConsumer;
import org.finos.springbot.symphony.stream.handler.SymphonyStreamHandler;
import org.finos.springbot.symphony.stream.handler.SymphonyStreamHandlerFactory;
import org.finos.springbot.workflow.data.EntityJsonConverter;
import org.finos.springbot.workflow.welcome.RoomWelcomeEventConsumer;
import org.finos.symphony.toolkit.koreai.output.KoreAIResponseHandler;
import org.finos.symphony.toolkit.koreai.output.KoreAIResponseHandlerImpl;
import org.finos.symphony.toolkit.koreai.request.KoreAIRequester;
import org.finos.symphony.toolkit.koreai.request.KoreAIRequesterImpl;
import org.finos.symphony.toolkit.koreai.response.KoreAIResponseBuilder;
import org.finos.symphony.toolkit.koreai.response.KoreAIResponseBuilderImpl;
import org.finos.symphony.toolkit.spring.api.factories.ApiInstance;
import org.finos.symphony.toolkit.spring.api.factories.ApiInstanceFactory;
import org.finos.symphony.toolkit.spring.api.properties.IdentityProperties;
import org.finos.symphony.toolkit.spring.api.properties.PodProperties;
import org.finos.symphony.toolkit.spring.api.properties.SymphonyApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ResourceLoader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.symphony.api.agent.MessagesApi;
import com.symphony.api.id.SymphonyIdentity;
import com.symphony.api.model.UserV2;
import com.symphony.api.pod.UsersApi;

public class KoreAIBridgeFactoryImpl implements KoreAIBridgeFactory {
	
	private static final Logger LOG = LoggerFactory.getLogger(KoreAIBridgeFactoryImpl.class);
	
	private ResourceLoader rl;
	private EntityJsonConverter ejc;
	private KoreAIProperties koreAIProperties;
	private SymphonyStreamHandlerFactory sshf;
	private ApiInstanceFactory apiInstanceFactory;
	private TrustManagerFactory tmf;
	private PodProperties podProperties;
	private SymphonyApiProperties apiProperties;
	
	public KoreAIBridgeFactoryImpl(ResourceLoader rl, 
			EntityJsonConverter ejc, 
			KoreAIProperties koreAIProperties,  
			SymphonyStreamHandlerFactory sshf,  
			ApiInstanceFactory aif, 
			TrustManagerFactory tmf, 
			SymphonyApiProperties apiProperties
			) {
		super();
		this.rl = rl;
		this.ejc = ejc;
		this.koreAIProperties = koreAIProperties;
		this.sshf = sshf;
		this.apiInstanceFactory = aif;
		this.tmf = tmf;
		this.podProperties = firstPodProperties(apiProperties);
		this.apiProperties = apiProperties;
	}


	@Override
	public SymphonyStreamHandler buildBridge(KoreAIInstanceProperties props) {
		String email = "--no email--";
		try {
			// build KoreAI pipeline
			ApiInstance apiInstance = symphonyAPIInstance(props);
			email = apiInstance.getIdentity().getEmail();
			KoreAIResponseBuilder koreAIResponseBuilder = koreAIResponseBuilder();
			KoreAIResponseHandler koreAIResponseHandler = responseMessageAdapter(apiInstance, props);
			KoreAIRequester requester = koreAIRequester(props, koreAIResponseHandler, koreAIResponseBuilder);
			List<StreamEventConsumer> consumers = new ArrayList<StreamEventConsumer>();
			consumers.add(koreAIEventHandler(requester, apiInstance, props));
			if (props.isSendWelcomeMessage()) {
				consumers.add(new RoomWelcomeEventConsumer(
					apiInstance.getAgentApi(MessagesApi.class), 
					apiInstance.getPodApi(UsersApi.class),
					apiInstance.getIdentity(),
					props.getWelcomeMessageML(),
					ejc
				));
			}
			
			
			// wire this up to a shared stream
			SymphonyStreamHandler out = sshf.createBean(apiInstance, consumers);
			return out;
		} catch (Exception e) {
			LOG.error("Couldn't construct Kore/Symphony bridge bean for "+email, e);
			return null;
		}
		
	}
	
	public KoreAIResponseHandler responseMessageAdapter(ApiInstance api, KoreAIInstanceProperties properties) throws IOException {
		return new KoreAIResponseHandlerImpl(api.getAgentApi(MessagesApi.class), rl, 
				properties.isSkipEmptyResponses(), 
				properties.isSendErrorsToSymphony(),
				ejc,
				koreAIProperties.getTemplatePrefix());	
	}
	
	public KoreAIRequester koreAIRequester(
			KoreAIInstanceProperties properties,
			KoreAIResponseHandler responseHandler, 
			KoreAIResponseBuilder responseBuilder) throws Exception {
		return new KoreAIRequesterImpl(responseHandler,
				responseBuilder,
				properties.getUrl(), 
				JsonNodeFactory.instance, 
				properties.getJwt());
	}
	
	public KoreAIResponseBuilder koreAIResponseBuilder() {
		return new KoreAIResponseBuilderImpl(new ObjectMapper(), JsonNodeFactory.instance);
	}
	
	public StreamEventConsumer koreAIEventHandler(KoreAIRequester requester, ApiInstance api, KoreAIInstanceProperties props) {
		UsersApi usersApi = api.getPodApi(UsersApi.class);
		UserV2 u = usersApi.v2UserGet(null, null, api.getIdentity().getEmail(), null, true);
		long id = 0;
		if (u != null) {
			id = u.getId();
		}
		
		return new KoreAIEventHandler(api.getIdentity(), id, requester, ejc, props.isOnlyAddressed());
	}
	
	public ApiInstance symphonyAPIInstance(KoreAIInstanceProperties props) {
		try {
			SymphonyIdentity symphonyBotIdentity = IdentityProperties.instantiateIdentityFromDetails(rl, props.getSymphonyBot(), ejc.getObjectMapper());
			TrustManager[] tms = tmf == null ? null: tmf.getTrustManagers();
			ApiInstance apiInstance = apiInstanceFactory.createApiInstance(symphonyBotIdentity, podProperties, tms);
			LOG.info("Constructed API Factory for {} ",props.getName());
			return apiInstance;
		} catch (Exception e) {
			LOG.error("Couldn't create API instance for {} ",props.getName());
			throw new UnsupportedOperationException("Couldn't get api instance: ", e);
		}
	}
	

	/**
	 * First pod is the only one used for bridging KoreAI.  In future, make this configurable.
	 */
	protected PodProperties firstPodProperties(SymphonyApiProperties apiProperties) {
		PodProperties pp;
		if (apiProperties.getApis().size() != 1) {
			throw new IllegalArgumentException("KoreAI Bridge must have the details of a single pod configured");
		}
		
		pp = apiProperties.getApis().get(0);
		return pp;
	}
	

}
