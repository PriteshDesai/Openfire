package org.jivesoftware.openfire.custom.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "pollDelete")
public class PollDelete {

	private String roomId;
	private String pollId;
	private String pollMessageId;

	public PollDelete() {
		super();
	}

	public PollDelete(String roomId, String pollId, String pollMessageId) {
		super();
		this.roomId = roomId;
		this.pollId = pollId;
		this.pollMessageId = pollMessageId;
	}

	@XmlElement
	public String getRoomId() {
		return roomId;
	}

	public void setRoomId(String roomId) {
		this.roomId = roomId;
	}

	@XmlElement
	public String getPollId() {
		return pollId;
	}

	public void setPollId(String pollId) {
		this.pollId = pollId;
	}

	@XmlElement
	public String getPollMessageId() {
		return pollMessageId;
	}

	public void setPollMessageId(String pollMessageId) {
		this.pollMessageId = pollMessageId;
	}

}
