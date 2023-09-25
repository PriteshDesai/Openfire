package org.jivesoftware.openfire.custom.dto;

public class OpinionResponse {

	private String optionName;
	private String optionAttendee;

	public OpinionResponse() {
		super();
	}

	public OpinionResponse(String optionName, String optionAttendee) {
		super();
		this.optionName = optionName;
		this.optionAttendee = optionAttendee;
	}

	public String getOptionName() {
		return optionName;
	}

	public void setOptionName(String optionName) {
		this.optionName = optionName;
	}

	public String getOptionAttendee() {
		return optionAttendee;
	}

	public void setOptionAttendee(String optionAttendee) {
		this.optionAttendee = optionAttendee;
	}

	@Override
	public String toString() {
		return "{\"optionName\":" + optionName + ", \"optionAttendee\":" + optionAttendee + "}";
	}

}
