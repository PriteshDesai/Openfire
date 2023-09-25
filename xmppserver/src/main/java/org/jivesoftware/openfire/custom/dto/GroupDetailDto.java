package org.jivesoftware.openfire.custom.dto;

public class GroupDetailDto {

	private String roomOriginalName;
	private String roomNickName;

	public GroupDetailDto() {
		super();
	}

	public GroupDetailDto(String roomOriginalName, String roomNickName) {
		super();
		this.roomOriginalName = roomOriginalName;
		this.roomNickName = roomNickName;
	}

	public String getRoomOriginalName() {
		return roomOriginalName;
	}

	public void setRoomOriginalName(String roomOriginalName) {
		this.roomOriginalName = roomOriginalName;
	}

	public String getRoomNickName() {
		return roomNickName;
	}

	public void setRoomNickName(String roomNickName) {
		this.roomNickName = roomNickName;
	}

}
