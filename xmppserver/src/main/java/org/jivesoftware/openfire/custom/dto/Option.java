package org.jivesoftware.openfire.custom.dto;

public class Option {

	private String optionName;

	public Option() {
		super();
		// TODO Auto-generated constructor stub
	}

	public Option(String optionName) {
		super();
		this.optionName = optionName;
	}

	public String getOptionName() {
		return optionName;
	}

	public void setOptionName(String optionName) {
		this.optionName = optionName;
	}

	@Override
	public String toString() {
		return "{\"optionName\":" + optionName + "}";
	}

}
