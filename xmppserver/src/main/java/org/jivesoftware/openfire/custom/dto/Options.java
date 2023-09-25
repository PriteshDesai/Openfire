package org.jivesoftware.openfire.custom.dto;

import java.util.List;

public class Options {

	private List<String> optionName;

	public Options() {
		super();
		// TODO Auto-generated constructor stub
	}

	public Options(List<String> optionName) {
		super();
		this.optionName = optionName;
	}

	public List<String> getOptionName() {
		return optionName;
	}

	public void setOptionName(List<String> optionName) {
		this.optionName = optionName;
	}

	@Override
	public String toString() {
		return "{\"optionName\":" + optionName + "}";
	}

}
