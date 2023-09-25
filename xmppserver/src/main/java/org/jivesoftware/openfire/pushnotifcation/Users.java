package org.jivesoftware.openfire.pushnotifcation;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "users")
public class Users {

	private List<String> user;

	public Users() {
		super();
	}

	public Users(List<String> user) {
		super();
		this.user = user;
	}

	public List<String> getUser() {
		return user;
	}

	public void setUser(List<String> user) {
		this.user = user;
	}

}
