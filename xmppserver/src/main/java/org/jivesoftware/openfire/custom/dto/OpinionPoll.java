package org.jivesoftware.openfire.custom.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "opinionPoll")
public class OpinionPoll {

	private String question;
	private String pollCreatedBy;
	private String roomId;
	private long expireDate;
	private String timeZone;
	private String pollId;
	private Options options;

	public OpinionPoll() {
	}

	public OpinionPoll(String question, String pollCreatedBy, String roomId, long expireDate, String timeZone,
			String pollId, Options options) {
		super();
		this.question = question;
		this.pollCreatedBy = pollCreatedBy;
		this.roomId = roomId;
		this.expireDate = expireDate;
		this.timeZone = timeZone;
		this.pollId = pollId;
		this.options = options;
	}

	@XmlElement
	public String getQuestion() {
		return question;
	}

	public void setQuestion(String question) {
		this.question = question;
	}

	@XmlElement
	public String getPollCreatedBy() {
		return pollCreatedBy;
	}

	public void setPollCreatedBy(String pollCreatedBy) {
		this.pollCreatedBy = pollCreatedBy;
	}

	@XmlElement
	public String getRoomId() {
		return roomId;
	}

	public void setRoomId(String roomId) {
		this.roomId = roomId;
	}

	@XmlElement
	public long getExpireDate() {
		return expireDate;
	}

	public void setExpireDate(long expireDate) {
		this.expireDate = expireDate;
	}

	@XmlElement
	public String getTimeZone() {
		return timeZone;
	}

	public void setTimeZone(String timeZone) {
		this.timeZone = timeZone;
	}

	@XmlElement
	public String getPollId() {
		return pollId;
	}

	public void setPollId(String pollId) {
		this.pollId = pollId;
	}

	@XmlElement
	public Options getOptions() {
		return options;
	}

	public void setOptions(Options options) {
		this.options = options;
	}

	@Override
	public String toString() {
		return "{\"question\":" + question + ", \"pollCreatedBy\":" + pollCreatedBy + ", \"roomId\":" + roomId
				+ ", \"expireDate\":" + expireDate + ", \"timeZone\":" + timeZone + ", \"pollId\":" + pollId
				+ ", \"options\":" + options + "}";
	}

}