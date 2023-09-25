package org.jivesoftware.openfire.custom.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "opinionPollUpdate")
public class OpinionPollUpdate {

	private String pollId;
	private String pollMessageId;
	private String roomId;
	private String isSelect;
	private OpinionResponse opinionResponse;

	public OpinionPollUpdate() {
	}

	public OpinionPollUpdate(String pollId, String pollMessageId, String roomId, String isSelect,
			OpinionResponse opinionResponse) {
		super();
		this.pollId = pollId;
		this.pollMessageId = pollMessageId;
		this.roomId = roomId;
		this.isSelect = isSelect;
		this.opinionResponse = opinionResponse;
	}

	@XmlElement
	public String getPollId() {
		return pollId;
	}

	public void setPollId(String pollId) {
		this.pollId = pollId;
	}

	@XmlElement
	public OpinionResponse getOpinionResponse() {
		return opinionResponse;
	}

	public void setOpinionResponse(OpinionResponse opinionResponse) {
		this.opinionResponse = opinionResponse;
	}

	@XmlElement
	public String getRoomId() {
		return roomId;
	}

	public void setRoomId(String roomId) {
		this.roomId = roomId;
	}

	@XmlElement
	public String getPollMessageId() {
		return pollMessageId;
	}

	public void setPollMessageId(String pollMessageId) {
		this.pollMessageId = pollMessageId;
	}

	@XmlElement
	public String getIsSelect() {
		return isSelect;
	}

	public void setIsSelect(String isSelect) {
		this.isSelect = isSelect;
	}

	@Override
	public String toString() {
		return "OpinionPollUpdate [pollId=" + pollId + ", pollMessageId=" + pollMessageId + ", roomId=" + roomId
				+ ", isSelect=" + isSelect + ", opinionResponse=" + opinionResponse + "]";
	}
}
