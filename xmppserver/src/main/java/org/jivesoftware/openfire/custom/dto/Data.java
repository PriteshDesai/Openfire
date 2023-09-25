package org.jivesoftware.openfire.custom.dto;

public class Data {

	private String senderName;
	private String senderImage;
	private String senderUUID;

	public Data() {
	}

	public Data(String senderName, String senderImage, String senderUUID) {
		super();
		this.senderName = senderName;
		this.senderImage = senderImage;
		this.senderUUID = senderUUID;
	}

	public String getSenderName() {
		return senderName;
	}

	public void setSenderName(String senderName) {
		this.senderName = senderName;
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

}
