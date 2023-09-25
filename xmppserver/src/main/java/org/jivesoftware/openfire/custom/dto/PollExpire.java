package org.jivesoftware.openfire.custom.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "pollResult")
public class PollExpire {

	private String pollId;
	private String status;
	private String winnerOption;

	public PollExpire() {
		super();
	}
	
	public PollExpire(String pollId, String status, String winnerOption) {
		super();
		this.pollId = pollId;
		this.status = status;
		this.winnerOption = winnerOption;
	}

	@XmlElement
	public String getPollId() {
		return pollId;
	}

	public void setPollId(String pollId) {
		this.pollId = pollId;
	}

	@XmlElement
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@XmlElement
	public String getWinnerOption() {
		return winnerOption;
	}

	public void setWinnerOption(String winnerOption) {
		this.winnerOption = winnerOption;
	}

}
