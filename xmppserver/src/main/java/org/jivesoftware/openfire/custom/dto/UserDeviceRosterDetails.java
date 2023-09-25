package org.jivesoftware.openfire.custom.dto;

public class UserDeviceRosterDetails {

	private String userName;
	private int roomId;
	private String jid;
	private String deviceToken;
	private DeviceType deviceType;
	private boolean isHuaweiPush;
	private String voipToken;
	private boolean isActive;
	private String channelName;

	public UserDeviceRosterDetails() {
		super();
		// TODO Auto-generated constructor stub
	}

	public UserDeviceRosterDetails(String userName, int roomId, String jid, String deviceToken, DeviceType deviceType,
			boolean isHuaweiPush, String voipToken, boolean isActive, String channelName) {
		super();
		this.userName = userName;
		this.roomId = roomId;
		this.jid = jid;
		this.deviceToken = deviceToken;
		this.deviceType = deviceType;
		this.isHuaweiPush = isHuaweiPush;
		this.voipToken = voipToken;
		this.isActive = isActive;
	}

	public String getChannelName() {
		return channelName;
	}

	public void setChannelName(String channelName) {
		this.channelName = channelName;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public int getRoomId() {
		return roomId;
	}

	public void setRoomId(int roomId) {
		this.roomId = roomId;
	}

	public String getJid() {
		return jid;
	}

	public void setJid(String jid) {
		this.jid = jid;
	}

	public String getDeviceToken() {
		return deviceToken;
	}

	public void setDeviceToken(String deviceToken) {
		this.deviceToken = deviceToken;
	}

	public DeviceType getDeviceType() {
		return deviceType;
	}

	public void setDeviceType(DeviceType deviceType) {
		this.deviceType = deviceType;
	}

	public boolean isHuaweiPush() {
		return isHuaweiPush;
	}

	public void setHuaweiPush(boolean isHuaweiPush) {
		this.isHuaweiPush = isHuaweiPush;
	}

	public String getVoipToken() {
		return voipToken;
	}

	public void setVoipToken(String voipToken) {
		this.voipToken = voipToken;
	}

	public boolean isActive() {
		return isActive;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}

	@Override
	public String toString() {
		return "UserDeviceRosterDetails [userName=" + userName + ", roomId=" + roomId + ", jid=" + jid
				+ ", deviceToken=" + deviceToken + ", deviceType=" + deviceType + ", isHuaweiPush=" + isHuaweiPush
				+ ", voipToken=" + voipToken + ", isActive=" + isActive + ", channelName=" + channelName + "]";
	}

}
