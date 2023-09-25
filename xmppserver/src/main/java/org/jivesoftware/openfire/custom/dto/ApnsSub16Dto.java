package org.jivesoftware.openfire.custom.dto;

import java.io.Serializable;

public class ApnsSub16Dto implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8414275195931734785L;

	private String type;
	private String messageId;
	private String fromJID;
	private String toJID;
	private String fromName;
	private String groupName;
	private long messageTime;
	private String subject;
	private Object notification;

	public ApnsSub16Dto() {
		super();
	}

	public ApnsSub16Dto(String type, String messageId, String fromJID, String toJID, String fromName, String groupName,
			long messageTime, String subject, Object notification) {
		super();
		this.type = type;
		this.messageId = messageId;
		this.fromJID = fromJID;
		this.toJID = toJID;
		this.fromName = fromName;
		this.groupName = groupName;
		this.messageTime = messageTime;
		this.subject = subject;
		this.notification = notification;
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

	public Object getNotification() {
		return notification;
	}

	public void setNotification(Object notification) {
		this.notification = notification;
	}

}
