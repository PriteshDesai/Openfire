package org.jivesoftware.openfire.custom.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "recallMessage")
public class RecallMessage {

	private String messageID;

	public RecallMessage() {
		super();
	}

	public RecallMessage(String messageID) {
		super();
		this.messageID = messageID;
	}

	@XmlElement
	public String getMessageID() {
		return messageID;
	}

	public void setMessageID(String messageID) {
		this.messageID = messageID;
	}

}
