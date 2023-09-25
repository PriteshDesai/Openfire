package org.jivesoftware.openfire.custom.dto;

import java.util.ArrayList;
import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class OpinionPollUpdatePayload {

	private String pollId;
	private String messageId;
	private String pollMessageId;
	private String roomId;
	private String isSelect;
	private ArrayList<Map<String, OptionPayload>> response;

	public OpinionPollUpdatePayload() {
		super();
	}

	public OpinionPollUpdatePayload(String pollId, String messageId, String pollMessageId, String roomId,
			String isSelect, ArrayList<Map<String, OptionPayload>> response) {
		super();
		this.pollId = pollId;
		this.messageId = messageId;
		this.pollMessageId = pollMessageId;
		this.roomId = roomId;
		this.isSelect = isSelect;
		this.response = response;
	}

	@XmlElement
	public String getPollId() {
		return pollId;
	}

	public void setPollId(String pollId) {
		this.pollId = pollId;
	}

	@XmlElement
	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	@XmlElement
	public String getRoomId() {
		return roomId;
	}

	public void setRoomId(String roomId) {
		this.roomId = roomId;
	}

	@XmlElement
	public ArrayList<Map<String, OptionPayload>> getResponse() {
		return response;
	}

	public void setResponse(ArrayList<Map<String, OptionPayload>> response) {
		this.response = response;
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
		return "OpinionPollUpdatePayload [pollId=" + pollId + ", messageId=" + messageId + ", pollMessageId="
				+ pollMessageId + ", roomId=" + roomId + ", isSelect=" + isSelect + ", response=" + response + "]";
	}

}
