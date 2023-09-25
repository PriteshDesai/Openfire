package org.jivesoftware.openfire.custom.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class MessageTime {

	private Long time;

	public MessageTime() {
		super();
	}

	public MessageTime(Long time) {
		super();
		this.time = time;
	}

	@XmlElement
	public Long getTime() {
		return time;
	}

	public void setTime(Long time) {
		this.time = time;
	}

	@Override
	public String toString() {
		return "{\"time\":" + time + "}";
	}
	
		
}