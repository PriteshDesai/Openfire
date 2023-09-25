package org.jivesoftware.openfire.custom.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class RSS {

	private String rssType;
	private String rssUUID;
	private String rssImageUrl;
	private String rssTitle;

	public RSS() {
		super();
		// TODO Auto-generated constructor stub
	}

	public RSS(String rssType, String rssUUID, String rssImageUrl, String rssTitle) {
		super();
		this.rssType = rssType;
		this.rssUUID = rssUUID;
		this.rssImageUrl = rssImageUrl;
		this.rssTitle = rssTitle;
	}

	@XmlElement
	public String getRssType() {
		return rssType;
	}

	public void setRssType(String rssType) {
		this.rssType = rssType;
	}

	@XmlElement
	public String getRssUUID() {
		return rssUUID;
	}

	public void setRssUUID(String rssUUID) {
		this.rssUUID = rssUUID;
	}

	@XmlElement
	public String getRssImageUrl() {
		return rssImageUrl;
	}

	public void setRssImageUrl(String rssImageUrl) {
		this.rssImageUrl = rssImageUrl;
	}

	@Override
	public String toString() {
		return "{\"rssType\":\"" + rssType + "\", \"rssUUID\":\"" + rssUUID + "\", \"rssImageUrl\":\"" + rssImageUrl
				+ "\"}";
	}

	@XmlElement
	public String getRssTitle() {
		return rssTitle;
	}

	public void setRssTitle(String rssTitle) {
		this.rssTitle = rssTitle;
	}

}