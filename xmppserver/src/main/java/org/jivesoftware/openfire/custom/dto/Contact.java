package org.jivesoftware.openfire.custom.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Contact {

	private String name;
	private String emails;
	private String phones;

	public Contact() {
		super();
	}

	public Contact(String name, String emails, String phones) {
		super();
		this.name = name;
		this.emails = emails;
		this.phones = phones;
	}

	@XmlElement
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@XmlElement
	public String getEmails() {
		return emails;
	}

	public void setEmails(String emails) {
		this.emails = emails;
	}

	@XmlElement
	public String getPhones() {
		return phones;
	}

	public void setPhones(String phones) {
		this.phones = phones;
	}

	@Override
	public String toString() {
		return "{\"name\":\"" + name + "\", \"emails\":\"" + emails + "\", \"phones\":\"" + phones + "\"}";
	}

}
