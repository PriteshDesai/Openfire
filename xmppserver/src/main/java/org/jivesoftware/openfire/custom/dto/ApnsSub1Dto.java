package org.jivesoftware.openfire.custom.dto;

import java.io.Serializable;

public class ApnsSub1Dto implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7766950636576819599L;

	private String fromJID;
	private String toJID;
	private String fromName;
	private String groupName;
	private String subject;
	private String type;
	private String id;
	private String body;
	private long messageTime;

	public ApnsSub1Dto() {
		super();
	}

	public ApnsSub1Dto(String fromJID, String toJID, String fromName, String groupName, String subject, String type,
			String id, String body, long messageTime) {
		super();
		this.fromJID = fromJID;
		this.toJID = toJID;
		this.fromName = fromName;
		this.groupName = groupName;
		this.subject = subject;
		this.type = type;
		this.id = id;
		this.body = body;
		this.messageTime = messageTime;
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

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
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

}
