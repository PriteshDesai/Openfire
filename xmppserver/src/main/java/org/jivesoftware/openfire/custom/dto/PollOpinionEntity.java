package org.jivesoftware.openfire.custom.dto;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class PollOpinionEntity {

	private List<PollOpinion> pollOpinions;

	public PollOpinionEntity() {
		super();
	}

	public PollOpinionEntity(List<PollOpinion> pollOpinions) {
		super();
		this.pollOpinions = pollOpinions;
	}

	@XmlElement
	// @JsonProperty(value = "pollOpinions")
	public List<PollOpinion> getPollOpinions() {
		return pollOpinions;
	}

	public void setPollOpinions(List<PollOpinion> pollOpinions) {
		this.pollOpinions = pollOpinions;
	}

	@Override
	public String toString() {
		return "{\"pollOpinions\":" + pollOpinions + "}";
	}

}
