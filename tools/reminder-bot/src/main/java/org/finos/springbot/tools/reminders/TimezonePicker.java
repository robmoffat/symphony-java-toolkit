package org.finos.springbot.tools.reminders;

import org.finos.springbot.workflow.annotations.Dropdown;
import org.finos.springbot.workflow.annotations.Work;

@Work
public class TimezonePicker {

	@Dropdown(data = "entity.timezones")
	public String timezone;
	
}
