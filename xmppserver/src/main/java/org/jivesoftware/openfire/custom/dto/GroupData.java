package org.jivesoftware.openfire.custom.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "groupData")
public class GroupData {

	private Data data;

	public GroupData() {
	}

	public GroupData(Data data) {
		super();
		this.data = data;
	}

	@XmlElement
	public Data getData() {
		return data;
	}

	public void setData(Data data) {
		this.data = data;
	}

}
