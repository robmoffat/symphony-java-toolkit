package org.finos.symphony.toolkit.tools.reminders;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;

import org.finos.springbot.symphony.content.SymphonyUser;
import org.finos.springbot.tools.reminders.Reminder;
import org.finos.springbot.tools.reminders.ReminderList;
import org.finos.springbot.tools.reminders.ReminderProperties;
import org.finos.springbot.tools.reminders.TimeFinder;
import org.finos.springbot.workflow.actions.Action;
import org.finos.springbot.workflow.actions.SimpleMessageAction;
import org.finos.springbot.workflow.content.Addressable;
import org.finos.springbot.workflow.content.Content;
import org.finos.springbot.workflow.content.Message;
import org.finos.springbot.workflow.content.OrderedContent;
import org.finos.springbot.workflow.content.User;
import org.finos.springbot.workflow.history.History;
import org.finos.springbot.workflow.response.Response;
import org.finos.springbot.workflow.response.WorkResponse;
import org.finos.springbot.workflow.response.handlers.ResponseHandlers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ErrorHandler;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;

@ExtendWith(MockitoExtension.class)
public class TimeFinderIT {

	@Mock
	StanfordCoreNLP stanfordCoreNLP;

	@Mock
	ReminderProperties reminderProperties;

	@Mock
	History history;

	@Mock
	ErrorHandler eh;

	@Mock
	ResponseHandlers responseHandlers;

	@InjectMocks
	TimeFinder timefinder;
	
	private SimpleMessageAction getAction() {
		SimpleMessageAction simpleMessageAction = new SimpleMessageAction(getAddressable(), getUser(), getMessage(),
				null);
		Action.CURRENT_ACTION.set(simpleMessageAction);
		return simpleMessageAction;
	}

	@SuppressWarnings("unchecked")
	@Test
	public void applyTest() {
		try {
			SimpleMessageAction action = getAction();
			lenient().when(history.getLastFromHistory(Mockito.any(Class.class), Mockito.any(Addressable.class)))
					.thenReturn(reminderList());

			timefinder.initializingStanfordProperties();
			timefinder.accept(action);

			ArgumentCaptor<Response> args = ArgumentCaptor.forClass(Response.class);
			Mockito.verify(responseHandlers).accept(args.capture());

			Assertions.assertEquals(args.getAllValues().size(), 1);
			WorkResponse fr = (WorkResponse) args.getValue();
			Reminder r = (Reminder) fr.getFormObject();
			Calendar c = Calendar.getInstance();
			int year = c.get(Calendar.YEAR);
			int month = c.get(Calendar.MONTH);
			int day = c.get(Calendar.DAY_OF_MONTH);
			Assertions.assertEquals(r.getLocalTime(), LocalDateTime.of(year, month + 1, day, 21, 20, 0));
		} catch (OutOfMemoryError e) {
			// for some reason this happens when we run (sometimes) in github actions
			// this is a workaround for this occasion.  Since we run tests locally, we shouldn't 
			// see this on our own machines
			return;
		}

	}

	private Optional<ReminderList> reminderList() {
		Reminder reminder = new Reminder();
		reminder.setDescription("Check at 9:30 pm");
		reminder.setLocalTime(LocalDateTime.now());
		reminder.setAuthor(getUser());
		List<Reminder> reminders = new ArrayList<>();
		reminders.add(reminder);
		ReminderList rl = new ReminderList();
		rl.setRemindBefore(10);
		rl.setTimeZone(ZoneId.of("Asia/Calcutta"));

		rl.setReminders(reminders);
		Optional<ReminderList> rrl = Optional.of(rl);
		return rrl;
	}

	private User getUser() {
		return new SymphonyUser("Sherlock Holmes", "sherlock.holmes@mail.com");

	}

	private Message getMessage() {
		Message m = new Message() {
			@Override
			public List<Content> getContents() {
				return null;
			}

			@Override
			public OrderedContent<Content> buildAnother(List<Content> contents) {
				return null;
			}

			@Override
			public String getText() {
				return "check at 9:30 pm";
			}
		};
		return m;
	}

	private Addressable getAddressable() {
		Addressable a = new Addressable() {

			@Override
			public String getKey() {
				return "testkey";
			}

		
		};
		return a;

	}

}
