package org.jivesoftware.openfire.custom.dto;

import java.util.Arrays;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class PollOpinion {

	private String pollId;
	private String createdBy;
	private Long createdOn;
	private Long expiredAt;
	private String timeZone;
	private boolean isExpired;
	private String question;
	private int roomId;
	private String messageId;
	private String[] pollOptionsText;
	private Integer[] pollOptionIds;
	private List<PollOptions> pollOptions;
	private List<Users> users;
	private PollOptionsResult result;

	public PollOpinion() {
		super();
		// TODO Auto-generated constructor stub
	}

	public PollOpinion(String pollId, String createdBy, Long createdOn, Long expiredAt, String timeZone,
			boolean isExpired, String question, int roomId, String messageId, String[] pollOptionsText,
			Integer[] pollOptionIds, List<PollOptions> pollOptions, List<Users> users, PollOptionsResult result) {
		super();
		this.pollId = pollId;
		this.createdBy = createdBy;
		this.createdOn = createdOn;
		this.expiredAt = expiredAt;
		this.timeZone = timeZone;
		this.isExpired = isExpired;
		this.question = question;
		this.roomId = roomId;
		this.messageId = messageId;
		this.pollOptionsText = pollOptionsText;
		this.pollOptionIds = pollOptionIds;
		this.pollOptions = pollOptions;
		this.users = users;
		this.setResult(result);
	}

	@XmlElement
	public String getPollId() {
		return pollId;
	}

	public void setPollId(String pollId) {
		this.pollId = pollId;
	}

	@XmlElement
	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	@XmlElement
	public Long getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Long createdOn) {
		this.createdOn = createdOn;
	}

	@XmlElement
	public Long getExpiredAt() {
		return expiredAt;
	}

	public void setExpiredAt(Long expiredAt) {
		this.expiredAt = expiredAt;
	}

	@XmlElement
	public String getTimeZone() {
		return timeZone;
	}

	public void setTimeZone(String timeZone) {
		this.timeZone = timeZone;
	}

	@XmlElement
	public boolean isExpired() {
		return isExpired;
	}

	public void setExpired(boolean isExpired) {
		this.isExpired = isExpired;
	}

	@XmlElement
	public String getQuestion() {
		return question;
	}

	public void setQuestion(String question) {
		this.question = question;
	}

	@XmlElement
	public int getRoomId() {
		return roomId;
	}

	public void setRoomId(int roomId) {
		this.roomId = roomId;
	}

	@XmlElement
	public String[] getPollOptionsText() {
		return pollOptionsText;
	}

	public void setPollOptionsText(String[] pollOptionsText) {
		this.pollOptionsText = pollOptionsText;
	}

	@XmlElement
	public Integer[] getPollOptionIds() {
		return pollOptionIds;
	}

	public void setPollOptionIds(Integer[] pollOptionIds) {
		this.pollOptionIds = pollOptionIds;
	}

	@XmlElement
	public List<PollOptions> getPollOptions() {
		return pollOptions;
	}

	public void setPollOptions(List<PollOptions> pollOptions) {
		this.pollOptions = pollOptions;
	}

	@XmlElement
	public List<Users> getUsers() {
		return users;
	}

	public void setUsers(List<Users> users) {
		this.users = users;
	}

	public PollOptionsResult getResult() {
		return result;
	}

	public void setResult(PollOptionsResult result) {
		this.result = result;
	}

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	@Override
	public String toString() {
		return "PollOpinion [pollId=" + pollId + ", createdBy=" + createdBy + ", createdOn=" + createdOn
				+ ", expiredAt=" + expiredAt + ", timeZone=" + timeZone + ", isExpired=" + isExpired + ", question="
				+ question + ", roomId=" + roomId + ", messageId=" + messageId + ", pollOptionsText="
				+ Arrays.toString(pollOptionsText) + ", pollOptionIds=" + Arrays.toString(pollOptionIds)
				+ ", pollOptions=" + pollOptions + ", users=" + users + ", result=" + result + "]";
	}
}
