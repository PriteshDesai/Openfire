package org.jivesoftware.openfire.custom.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "relay")
public class Relay {

	private String relayTitle;
	private String relayFee;
	private String relayImageUrl;
	private String relayUUID;

	public Relay() {
		super();
	}

	public Relay(String relayTitle, String relayFee, String relayImageUrl, String relayUUID) {
		super();
		this.relayTitle = relayTitle;
		this.relayFee = relayFee;
		this.relayImageUrl = relayImageUrl;
		this.relayUUID = relayUUID;
	}

	@XmlElement
	public String getRelayTitle() {
		return relayTitle;
	}

	public void setRelayTitle(String relayTitle) {
		this.relayTitle = relayTitle;
	}

	@XmlElement
	public String getRelayFee() {
		return relayFee;
	}

	public void setRelayFee(String relayFee) {
		this.relayFee = relayFee;
	}

	@XmlElement
	public String getRelayImageUrl() {
		return relayImageUrl;
	}

	public void setRelayImageUrl(String relayImageUrl) {
		this.relayImageUrl = relayImageUrl;
	}

	@XmlElement
	public String getRelayUUID() {
		return relayUUID;
	}

	public void setRelayUUID(String relayUUID) {
		this.relayUUID = relayUUID;
	}

}
