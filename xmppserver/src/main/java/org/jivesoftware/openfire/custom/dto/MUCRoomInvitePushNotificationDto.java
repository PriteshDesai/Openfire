package org.jivesoftware.openfire.custom.dto;

import java.io.Serializable;
import java.util.List;

public class MUCRoomInvitePushNotificationDto implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2687478132449495769L;

	private String type;
	private String roomJID;
	private List<String> memberList;
	private String roomName;
	private String roomProfileURL;
	private long roomCreationDate;
	private String roomOwnerJID;
	private String body;
	private String title;

	public MUCRoomInvitePushNotificationDto() {
		super();
	}

	public MUCRoomInvitePushNotificationDto(String type, String roomJID, List<String> memberList, String roomName,
			String roomProfileURL, long roomCreationDate, String roomOwnerJID, String body, String title) {
		super();
		this.type = type;
		this.roomJID = roomJID;
		this.memberList = memberList;
		this.roomName = roomName;
		this.roomProfileURL = roomProfileURL;
		this.roomCreationDate = roomCreationDate;
		this.roomOwnerJID = roomOwnerJID;
		this.body = body;
		this.title = title;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getRoomJID() {
		return roomJID;
	}

	public void setRoomJID(String roomJID) {
		this.roomJID = roomJID;
	}

	public List<String> getMemberList() {
		return memberList;
	}

	public void setMemberList(List<String> memberList) {
		this.memberList = memberList;
	}

	public String getRoomName() {
		return roomName;
	}

	public void setRoomName(String roomName) {
		this.roomName = roomName;
	}

	public String getRoomProfileURL() {
		return roomProfileURL;
	}

	public void setRoomProfileURL(String roomProfileURL) {
		this.roomProfileURL = roomProfileURL;
	}

	public long getRoomCreationDate() {
		return roomCreationDate;
	}

	public void setRoomCreationDate(long roomCreationDate) {
		this.roomCreationDate = roomCreationDate;
	}

	public String getRoomOwnerJID() {
		return roomOwnerJID;
	}

	public void setRoomOwnerJID(String roomOwnerJID) {
		this.roomOwnerJID = roomOwnerJID;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

}
