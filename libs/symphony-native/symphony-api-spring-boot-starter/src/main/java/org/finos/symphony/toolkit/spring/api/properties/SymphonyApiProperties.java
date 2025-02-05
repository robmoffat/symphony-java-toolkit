package org.finos.symphony.toolkit.spring.api.properties;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("symphony")
public class SymphonyApiProperties {

	private List<PodProperties> apis;
	private TrustStoreProperties trustStore;
	private boolean localPod = true;

	public TrustStoreProperties getTrustStore() {
		return trustStore;
	}

	public void setTrustStore(TrustStoreProperties trustStore) {
		this.trustStore = trustStore;
	}

	public List<PodProperties> getApis() {
		return apis;
	}

	public void setApis(List<PodProperties> apis) {
		this.apis = apis;
	}

	public boolean isLocalPod() {
		return localPod;
	}

	public void setLocalPod(boolean localPod) {
		this.localPod = localPod;
	}

	

}
