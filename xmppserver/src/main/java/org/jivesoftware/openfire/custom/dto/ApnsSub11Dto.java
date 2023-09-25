package org.jivesoftware.openfire.custom.dto;

import java.io.Serializable;

public class ApnsSub11Dto implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6719054563332724217L;

	private String type;
	private String messageId;
	private String fromJID;
	private String toJID;
	private String senderImage;
	private String senderUUID;
	private String fromName;
	private String groupName;
	private long messageTime;
	private String subject;
	private Object opinionPoll;

	public ApnsSub11Dto() {
		super();
	}

	public ApnsSub11Dto(String type, String messageId, String fromJID, String toJID, String senderImage,
			String senderUUID, String fromName, String groupName, long messageTime, String subject,
			Object opinionPoll) {
		super();
		this.type = type;
		this.messageId = messageId;
		this.fromJID = fromJID;
		this.toJID = toJID;
		this.senderImage = senderImage;
		this.senderUUID = senderUUID;
		this.fromName = fromName;
		this.groupName = groupName;
		this.messageTime = messageTime;
		this.subject = subject;
		this.setOpinionPoll(opinionPoll);
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getFromJID() {
		return fromJID;
	}

	public void setFromJID(String fromJID) {
		this.fromJID = fromJID;
	}

	public String getToJID() {
		return toJID;
	}

	public void setToJID(String toJID) {
		this.toJID = toJID;
	}

	public String getSenderImage() {
		return senderImage;
	}

	public void setSenderImage(String senderImage) {
		this.senderImage = senderImage;
	}

	public String getSenderUUID() {
		return senderUUID;
	}

	public void setSenderUUID(String senderUUID) {
		this.senderUUID = senderUUID;
	}

	public String getFromName() {
		return fromName;
	}

	public void setFromName(String fromName) {
		this.fromName = fromName;
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public long getMessageTime() {
		return messageTime;
	}

	public void setMessageTime(long messageTime) {
		this.messageTime = messageTime;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public Object getOpinionPoll() {
		return opinionPoll;
	}

	public void setOpinionPoll(Object opinionPoll) {
		this.opinionPoll = opinionPoll;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

}
