/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2019-2029. All rights reserved.
 */

package org.jivesoftware.openfire.com.huawei.push.android;

import org.apache.commons.lang3.StringUtils;
import org.jivesoftware.openfire.com.huawei.push.util.ValidatorUtils;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 功能描述
 *
 * @author l00282963
 * @since 2020-01-19
 */
public class Button {
	@JsonProperty(value = "name")
	private String name;

	@JsonProperty(value = "action_type")
	private Integer actionType;

	@JsonProperty(value = "intent_type")
	private Integer intentType;

	@JsonProperty(value = "intent")
	private String intent;

	@JsonProperty(value = "data")
	private String data;

	public String getName() {
		return name;
	}

	public Integer getActionType() {
		return actionType;
	}

	public Integer getIntentType() {
		return intentType;
	}

	public String getIntent() {
		return intent;
	}

	public String getData() {
		return data;
	}

	public Button(Builder builder) {
		this.name = builder.name;
		this.actionType = builder.actionType;
		this.intentType = builder.intentType;
		this.intent = builder.intent;
		this.data = builder.data;
	}

	public void check() {
		if (this.actionType != null && this.actionType == 4) {
			ValidatorUtils.checkArgument(StringUtils.isNotEmpty(this.data), "data is needed when actionType is 4");
		}
	}

	/**
	 * builder
	 */
	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		@JsonProperty(value = "name")
		private String name;

		@JsonProperty(value = "actionType")
		private Integer actionType;

		@JsonProperty(value = "intentType")
		private Integer intentType;

		@JsonProperty(value = "intent")
		private String intent;

		@JsonProperty(value = "data")
		private String data;

		public Builder setName(String name) {
			this.name = name;
			return this;
		}

		public Builder setActionType(Integer actionType) {
			this.actionType = actionType;
			return this;
		}

		public Builder setIntentType(Integer intentType) {
			this.intentType = intentType;
			return this;
		}

		public Builder setIntent(String intent) {
			this.intent = intent;
			return this;
		}

		public Builder setData(String data) {
			this.data = data;
			return this;
		}

		public Button build() {
			return new Button(this);
		}
	}
}
