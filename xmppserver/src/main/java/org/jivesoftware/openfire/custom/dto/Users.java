package org.jivesoftware.openfire.custom.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Users {

	private String name;
	private String userName;
	private String email;
	private String selectedOption;

	public Users() {
		super();
		// TODO Auto-generated constructor stub
	}

	public Users(String name, String userName, String email, String selectedOption) {
		super();
		this.name = name;
		this.userName = userName;
		this.email = email;
		this.selectedOption = selectedOption;
	}

	@XmlElement
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@XmlElement
	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	@XmlElement
	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	@XmlElement
	public String getSelectedOption() {
		return selectedOption;
	}

	public void setSelectedOption(String selectedOption) {
		this.selectedOption = selectedOption;
	}

	@Override
	public String toString() {
		return "{\"name\":" + name + ", \'userName\":" + userName + ", \"email\":" + email + ", \"selectedOption\":"
				+ selectedOption + "}";
	}

}