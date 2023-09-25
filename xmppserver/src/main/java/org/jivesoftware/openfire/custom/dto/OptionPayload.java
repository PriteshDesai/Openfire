package org.jivesoftware.openfire.custom.dto;

import java.util.Arrays;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class OptionPayload {

	private String count;
	private String[] ids;

	public OptionPayload() {
		super();
	}

	public OptionPayload(String count, String[] userIds) {
		super();
		this.count = count;
		this.ids = userIds;
	}

	@XmlElement
	public String getCount() {
		return count;
	}

	public void setCount(String count) {
		this.count = count;
	}

	@XmlElement
	public String[] getIds() {
		return ids;
	}

	public void setIds(String[] userIds) {
		this.ids = userIds;
	}

	@Override
	public String toString() {
		return "OptionPayload [count=" + count + ", userIds=" + Arrays.toString(ids) + "]";
	}

}
