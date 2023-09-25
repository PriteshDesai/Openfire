package org.jivesoftware.openfire.custom.dto;

import java.io.Serializable;

public class AndroidSub1Dto implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1928239652226346325L;

	private String type;
	private String messageId;
	private String fromJID;
	private String toJID;
	private String fromName;
	private String groupName;
	private String senderImage;
	private String senderUUID;
	private String subject;
	private String body;
	private long messageTime;
	private String title;

	public AndroidSub1Dto() {
		super();
	}

	public AndroidSub1Dto(String type, String messageId, String fromJID, String toJID, String fromName,
			String groupName, String senderImage, String senderUUID, String subject, String body, long messageTime,
			String title) {
		super();
		this.type = type;
		this.messageId = messageId;
		this.fromJID = fromJID;
		this.toJID = toJID;
		this.fromName = fromName;
		this.groupName = groupName;
		this.senderImage = senderImage;
		this.senderUUID = senderUUID;
		this.subject = subject;
		this.body = body;
		this.messageTime = messageTime;
		this.title = title;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
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

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public long getMessageTime() {
		return messageTime;
	}

	public void setMessageTime(long messageTime) {
		this.messageTime = messageTime;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
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

}
