package org.jivesoftware.openfire.custom.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class PollOptionsResult {

	private String status;
	private PollOptions polloption;

	public PollOptionsResult() {
		super();
		// TODO Auto-generated constructor stub
	}

	public PollOptionsResult(String status, PollOptions polloption) {
		super();
		this.status = status;
		this.polloption = polloption;
	}

	@XmlElement
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@XmlElement
	public PollOptions getPolloption() {
		return polloption;
	}

	public void setPolloption(PollOptions polloption) {
		this.polloption = polloption;
	}

	@Override
	public String toString() {
		return "PollOptionsResult [status=" + status + ", polloption=" + polloption + "]";
	}

}
