/*
 * Copyright 2020. Huawei Technologies Co., Ltd. All rights reserved.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.jivesoftware.openfire.com.huawei.push.message;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.openfire.com.huawei.push.util.CollectionUtils;
import org.jivesoftware.openfire.com.huawei.push.util.ValidatorUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import com.google.common.primitives.Booleans;

public class Message {
	@JsonProperty(value = "data")
	private String data;

	@JsonProperty(value = "notification")
	private Notification notification;

	@JsonProperty(value = "android")
	private AndroidConfig androidConfig;

	@JsonProperty(value = "apns")
	private ApnsConfig apns;

	@JsonProperty(value = "webpush")
	private WebPushConfig webpush;

	@JsonProperty(value = "token")
	private List<String> token = new ArrayList<>();

	@JsonProperty(value = "topic")
	private String topic;

	@JsonProperty(value = "condition")
	private String condition;

	private Message(Builder builder) {
		this.data = builder.data;
		this.notification = builder.notification;
		this.androidConfig = builder.androidConfig;
		this.apns = builder.apns;
		this.webpush = builder.webpush;

		if (!CollectionUtils.isEmpty(builder.token)) {
			this.token.addAll(builder.token);
		} else {
			this.token = null;
		}

		this.topic = builder.topic;
		this.condition = builder.condition;

		/** check after message is created */
		check();
	}

	/**
	 * check message's parameters
	 */
	public void check() {

		int count = Booleans.countTrue(!CollectionUtils.isEmpty(this.token), !Strings.isNullOrEmpty(this.topic),
				!Strings.isNullOrEmpty(this.condition));

		ValidatorUtils.checkArgument(count == 1, "Exactly one of token, topic or condition must be specified");

		if (this.notification != null) {
			this.notification.check();
		}

		if (null != this.androidConfig) {
			this.androidConfig.check(this.notification);
		}

		if (this.apns != null) {
			this.apns.check();
		}

		if (this.webpush != null) {
			this.webpush.check();
		}
	}

	/**
	 * getter
	 */
	public String getData() {
		return data;
	}

	public Notification getNotification() {
		return notification;
	}

	public AndroidConfig getAndroidConfig() {
		return androidConfig;
	}

	public ApnsConfig getApns() {
		return apns;
	}

	public WebPushConfig getWebpush() {
		return webpush;
	}

	public List<String> getToken() {
		return token;
	}

	public String getTopic() {
		return topic;
	}

	public String getCondition() {
		return condition;
	}

	/**
	 * builder
	 *
	 * @return
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * push message builder
	 */
	public static class Builder {
		private String data;
		private Notification notification;
		private AndroidConfig androidConfig;
		private ApnsConfig apns;
		private WebPushConfig webpush;
		private List<String> token = new ArrayList<>();
		private String topic;
		private String condition;

		private Builder() {
		}

		public Builder setData(String data) {
			this.data = data;
			return this;
		}

		public Builder setNotification(Notification notification) {
			this.notification = notification;
			return this;
		}

		public Builder setAndroidConfig(AndroidConfig androidConfig) {
			this.androidConfig = androidConfig;
			return this;
		}

		public Builder setApns(ApnsConfig apns) {
			this.apns = apns;
			return this;
		}

		public Builder setWebpush(WebPushConfig webpush) {
			this.webpush = webpush;
			return this;
		}

		public Builder addToken(String token) {
			this.token.add(token);
			return this;
		}

		public Builder addAllToken(List<String> tokens) {
			this.token.addAll(tokens);
			return this;
		}

		public Builder setTopic(String topic) {
			this.topic = topic;
			return this;
		}

		public Builder setCondition(String condition) {
			this.condition = condition;
			return this;
		}

		public Message build() {
			return new Message(this);
		}
	}
}
