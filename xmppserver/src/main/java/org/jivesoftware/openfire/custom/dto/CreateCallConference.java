package org.jivesoftware.openfire.custom.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "createCallConference")
public class CreateCallConference {

	private String title;
	private String description;
	private String callConferenceId;
	private String timeZone;
	private String timezoneLocation;
	private String organizerId;
	private String callStartTime;
	private String callEndTime;

	public CreateCallConference() {
	}

	public CreateCallConference(String title, String description, String callConferenceId, String timeZone,
			String timezoneLocation, String organizerId, String callStartTime, String callEndTime) {
		super();
		this.title = title;
		this.description = description;
		this.callConferenceId = callConferenceId;
		this.timeZone = timeZone;
		this.timezoneLocation = timezoneLocation;
		this.organizerId = organizerId;
		this.callStartTime = callStartTime;
		this.callEndTime = callEndTime;
	}

	@XmlElement
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	@XmlElement
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@XmlElement
	public String getCallConferenceId() {
		return callConferenceId;
	}

	public void setCallConferenceId(String callConferenceId) {
		this.callConferenceId = callConferenceId;
	}

	@XmlElement
	public String getTimeZone() {
		return timeZone;
	}

	public void setTimeZone(String timeZone) {
		this.timeZone = timeZone;
	}

	@XmlElement
	public String getTimezoneLocation() {
		return timezoneLocation;
	}

	public void setTimezoneLocation(String timezoneLocation) {
		this.timezoneLocation = timezoneLocation;
	}

	@XmlElement
	public String getOrganizerId() {
		return organizerId;
	}

	public void setOrganizerId(String organizerId) {
		this.organizerId = organizerId;
	}

	@XmlElement
	public String getCallStartTime() {
		return callStartTime;
	}

	public void setCallStartTime(String callStartTime) {
		this.callStartTime = callStartTime;
	}

	@XmlElement
	public String getCallEndTime() {
		return callEndTime;
	}

	public void setCallEndTime(String callEndTime) {
		this.callEndTime = callEndTime;
	}

}
