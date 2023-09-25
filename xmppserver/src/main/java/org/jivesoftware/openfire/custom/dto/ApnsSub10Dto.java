package org.jivesoftware.openfire.custom.dto;

import java.io.Serializable;

public class ApnsSub10Dto implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2687072021701915056L;

	private String type;
	private String fromJID;
	private String toJID;
	private String fromName;
	private String groupName;
	private long messageTime;
	private String subject;
	private Object relay;

	public ApnsSub10Dto() {
		super();
		// TODO Auto-generated constructor stub
	}

	public ApnsSub10Dto(String type, String fromJID, String toJID, String fromName, String groupName, long messageTime,
			String subject, Object relay) {
		super();
		this.type = type;
		this.fromJID = fromJID;
		this.toJID = toJID;
		this.fromName = fromName;
		this.groupName = groupName;
		this.messageTime = messageTime;
		this.subject = subject;
		this.relay = relay;
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

	public Object getRelay() {
		return relay;
	}

	public void setRelay(Object relay) {
		this.relay = relay;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}
}
