package org.jivesoftware.openfire.custom.dto;

import java.io.Serializable;
import java.util.List;

public class ApnsSub15Dto implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6132071512462100128L;

	private String roomId;
	private String initiatorId;
	private String subject;
	private String type;
	private List<String> userId;
	private String reason;
	private String groupName;
	private String opponentImageUrl;
	private long messageTime;
	private String groupJID;

	public ApnsSub15Dto() {
		super();
	}

	public ApnsSub15Dto(String roomId, String initiatorId, String subject, String type, List<String> userId,
			String reason, long messageTime, String groupName, String opponentImageUrl, String groupJID) {
		super();
		this.roomId = roomId;
		this.initiatorId = initiatorId;
		this.subject = subject;
		this.type = type;
		this.userId = userId;
		this.reason = reason;
		this.messageTime = messageTime;
		this.groupName = groupName;
		this.opponentImageUrl = opponentImageUrl;
		this.groupJID = groupJID;
	}

	public String getRoomId() {
		return roomId;
	}

	public void setRoomId(String roomId) {
		this.roomId = roomId;
	}

	public String getInitiatorId() {
		return initiatorId;
	}

	public void setInitiatorId(String initiatorId) {
		this.initiatorId = initiatorId;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public List<String> getUserId() {
		return userId;
	}

	public void setUserId(List<String> userId) {
		this.userId = userId;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public long getMessageTime() {
		return messageTime;
	}

	public void setMessageTime(long messageTime) {
		this.messageTime = messageTime;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public String getOpponentImageUrl() {
		return opponentImageUrl;
	}

	public void setOpponentImageUrl(String opponentImageUrl) {
		this.opponentImageUrl = opponentImageUrl;
	}

	public String getGroupJID() {
		return groupJID;
	}

	public void setGroupJID(String groupJID) {
		this.groupJID = groupJID;
	}

}
