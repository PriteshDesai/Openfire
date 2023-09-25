package org.jivesoftware.openfire.custom.dto;

public enum DeviceType {

	ANDROID("ANDROID"), IOS("IOS");

	public String deviceType;

	DeviceType() {
		// TODO Auto-generated constructor stub
	}

	DeviceType(String deviceType) {
		this.deviceType = deviceType;
	}

	public String value() {
		return deviceType;
	}

}
