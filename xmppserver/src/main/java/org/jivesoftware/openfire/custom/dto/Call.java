package org.jivesoftware.openfire.custom.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.jivesoftware.openfire.pushnotifcation.Users;

@XmlRootElement(name = "call")
public class Call {

	private String id;
	private String initiatorId;
	private String type;
	private String reason;
	private Users users;
	private String groupName;
	private String opponentImageUrl;

	public Call() {
		super();
	}

	public Call(String id, String initiatorId, String type, String reason, Users users, String groupName,
			String opponentImageUrl) {
		super();
		this.id = id;
		this.initiatorId = initiatorId;
		this.type = type;
		this.reason = reason;
		this.users = users;
		this.groupName = groupName;
		this.opponentImageUrl = opponentImageUrl;
	}

	@XmlElement
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@XmlElement
	public String getInitiatorId() {
		return initiatorId;
	}

	public void setInitiatorId(String initiatorId) {
		this.initiatorId = initiatorId;
	}

	@XmlElement
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@XmlElement
	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	@XmlElement
	public Users getUsers() {
		return users;
	}

	public void setUsers(Users users) {
		this.users = users;
	}

	@XmlElement
	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	@Override
	public String toString() {
		return "{\"id\":\"" + id + "\", \"initiatorId\":\"" + initiatorId + "\", \"type\":\"" + type
				+ "\", \"reason\":\"" + reason + "\", \"users\":" + users + "}";
	}

	public String getOpponentImageUrl() {
		return opponentImageUrl;
	}

	public void setOpponentImageUrl(String opponentImageUrl) {
		this.opponentImageUrl = opponentImageUrl;
	}

}
