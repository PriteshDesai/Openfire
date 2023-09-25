package org.jivesoftware.openfire.custom.dto;

import java.io.Serializable;

public class ApnsSub9Dto implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7826940395449959757L;

	private String type;
	private String fromJID;
	private String toJID;
	private String fromName;
	private String groupName;
	private long messageTime;
	private String subject;
	private Object rss;

	public ApnsSub9Dto() {
		super();
		// TODO Auto-generated constructor stub
	}

	public ApnsSub9Dto(String type, String fromJID, String toJID, String fromName, String groupName, long messageTime,
			String subject, Object rss) {
		super();
		this.type = type;
		this.fromJID = fromJID;
		this.toJID = toJID;
		this.fromName = fromName;
		this.groupName = groupName;
		this.messageTime = messageTime;
		this.subject = subject;
		this.rss = rss;
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

	public Object getRss() {
		return rss;
	}

	public void setRss(Object rss) {
		this.rss = rss;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

}
