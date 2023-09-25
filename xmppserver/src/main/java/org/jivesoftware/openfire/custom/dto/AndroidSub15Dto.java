package org.jivesoftware.openfire.custom.dto;

import java.io.Serializable;
import java.util.List;

public class AndroidSub15Dto implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6924218909296854364L;

	private String type;
	private String roomId;
	private String initiatorId;
	private String subject;
	private List<String> userId;
	private String reason;
	private long messageTime;
	private String callType;
	private String groupName;
	private String senderImage;
	private String groupJID;

	public AndroidSub15Dto() {
		super();
		// TODO Auto-generated constructor stub
	}

	public AndroidSub15Dto(String type, String roomId, String initiatorId, String subject, List<String> userId,
			String reason, long messageTime, String callType, String groupName, String senderImage, String groupJID) {
		super();
		this.type = type;
		this.roomId = roomId;
		this.initiatorId = initiatorId;
		this.subject = subject;
		this.userId = userId;
		this.reason = reason;
		this.messageTime = messageTime;
		this.callType = callType;
		this.groupName = groupName;
		this.senderImage = senderImage;
		this.groupJID = groupJID;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
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

	public String getCallType() {
		return callType;
	}

	public void setCallType(String callType) {
		this.callType = callType;
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public String getGroupJID() {
		return groupJID;
	}

	public void setGroupJID(String groupJID) {
		this.groupJID = groupJID;
	}

	public String getSenderImage() {
		return senderImage;
	}

	public void setSenderImage(String senderImage) {
		this.senderImage = senderImage;
	}

}
