package org.jivesoftware.openfire.custom.dto;

import java.io.Serializable;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "mediaData")
public class MediaData implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1910235271762878428L;
	private List<MessageMedia> messageMedia;

	public MediaData() {
		super();
	}

	public MediaData(List<MessageMedia> messageMedia) {
		super();
		this.messageMedia = messageMedia;
	}

	@XmlElement(name = "messageMedia")
	public List<MessageMedia> getMessageMedia() {
		return messageMedia;
	}

	public void setMessageMedia(List<MessageMedia> messageMedia) {
		this.messageMedia = messageMedia;
	}

	@Override
	public String toString() {
		return "{\"messageMedia\":" + messageMedia + "}";
	}

}
