package org.jivesoftware.openfire.custom.dto;

import java.util.Arrays;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class PollOptions {

	private String optionName;
	private int count;
	private String[] users;

	public PollOptions() {
		super();
		// TODO Auto-generated constructor stub
	}

	public PollOptions(String optionName, int count, String[] users) {
		super();
		this.optionName = optionName;
		this.count = count;
		this.users = users;
	}

	@XmlElement
	public String getOptionName() {
		return optionName;
	}

	public void setOptionName(String optionName) {
		this.optionName = optionName;
	}

	@XmlElement
	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	@XmlElement
	public String[] getUsers() {
		return users;
	}

	public void setUsers(String[] users) {
		this.users = users;
	}

	@Override
	public String toString() {
		return "{\"optionName\":" + optionName + ", \"count\":" + count + ", \"users\":" + Arrays.asList(users) + "}";
	}

}
