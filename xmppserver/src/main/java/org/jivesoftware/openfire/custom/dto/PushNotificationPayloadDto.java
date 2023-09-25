package org.jivesoftware.openfire.custom.dto;

import java.io.Serializable;

public class PushNotificationPayloadDto implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5830667659647381249L;
	private String messageType;
	private String messageId;
	private String fromJID;
	private String toJID;
	private String senderImage;
	private String senderUUID;
	private String fromName;
	private String groupName;
	private String groupJID;
	private long messageTime;
	private String subject;
	private Object payload;
	private String body;
	private String androidBody;
	private String title;

	public PushNotificationPayloadDto() {
		super();
	}

	public PushNotificationPayloadDto(String messageType, String messageId, String fromJID, String toJID,
			String senderImage, String senderUUID, String fromName, String groupName, String groupJID, long messageTime,
			String subject, Object payload, String body, String androidBody, String title) {
		super();
		this.messageType = messageType;
		this.messageId = messageId;
		this.fromJID = fromJID;
		this.toJID = toJID;
		this.senderImage = senderImage;
		this.senderUUID = senderUUID;
		this.fromName = fromName;
		this.groupName = groupName;
		this.groupJID = groupJID;
		this.messageTime = messageTime;
		this.subject = subject;
		this.payload = payload;
		this.body = body;
		this.androidBody = androidBody;
		this.title = title;
	}

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	public String getMessageType() {
		return messageType;
	}

	public void setMessageType(String messageType) {
		this.messageType = messageType;
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

	public Object getPayload() {
		return payload;
	}

	public void setPayload(Object payload) {
		this.payload = payload;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getAndroidBody() {
		return androidBody;
	}

	public void setAndroidBody(String androidBody) {
		this.androidBody = androidBody;
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

	public String getGroupJID() {
		return groupJID;
	}

	public void setGroupJID(String groupJID) {
		this.groupJID = groupJID;
	}

}
