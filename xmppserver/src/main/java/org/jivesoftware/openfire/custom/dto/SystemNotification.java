package org.jivesoftware.openfire.custom.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "notification")
public class SystemNotification {

	private String type;
	private String save;
	private String show;

	public SystemNotification() {
	}

	public SystemNotification(String type, String save, String show) {
		super();
		this.type = type;
		this.save = save;
		this.show = show;
	}

	@XmlElement
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@XmlElement
	public String getSave() {
		return save;
	}

	public void setSave(String save) {
		this.save = save;
	}

	@XmlElement
	public String getShow() {
		return show;
	}

	public void setShow(String show) {
		this.show = show;
	}

}
