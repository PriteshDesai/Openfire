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
package org.jivesoftware.openfire.com.huawei.push.messaging;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.jivesoftware.openfire.com.huawei.push.exception.HuaweiMesssagingException;
import org.jivesoftware.openfire.com.huawei.push.message.Message;
import org.jivesoftware.openfire.com.huawei.push.message.TopicMessage;
import org.jivesoftware.openfire.com.huawei.push.model.TopicOperation;
import org.jivesoftware.openfire.com.huawei.push.reponse.SendResponse;
import org.jivesoftware.openfire.com.huawei.push.reponse.TopicListResponse;
import org.jivesoftware.openfire.com.huawei.push.reponse.TopicSendResponse;
import org.jivesoftware.openfire.com.huawei.push.util.ValidatorUtils;
import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class HuaweiMessageClientImpl implements HuaweiMessageClient {
	private static final Logger logger = LoggerFactory.getLogger(HuaweiMessageClientImpl.class);
	private static final String PUSH_URL = JiveGlobals.getProperty("push.hms.base.url");

	private final String HcmPushUrl;
	private String hcmTopicUrl;
	private final CloseableHttpClient httpClient;

	private HuaweiMessageClientImpl(Builder builder) {
		this.HcmPushUrl = MessageFormat.format(PUSH_URL + "/v1/{0}/messages:send", builder.appId);
		this.hcmTopicUrl = MessageFormat.format(PUSH_URL + "/v1/{0}/topic:{1}", builder.appId);

		ValidatorUtils.checkArgument(builder.httpClient != null, "requestFactory must not be null");
		this.httpClient = builder.httpClient;
	}

	/**
	 * getter
	 */
	public String getHcmSendUrl() {
		return HcmPushUrl;
	}

	public CloseableHttpClient getHttpClient() {
		return httpClient;
	}

	@Override
	public SendResponse send(Message message, boolean validateOnly, String accessToken)
			throws HuaweiMesssagingException {
		try {
			return sendRequest(message, validateOnly, accessToken);
		} catch (IOException e) {
			throw new HuaweiMesssagingException(HuaweiMessaging.INTERNAL_ERROR,
					"Error while calling HCM backend service", e);
		}
	}

	@Override
	public SendResponse send(TopicMessage message, String operation, String accessToken)
			throws HuaweiMesssagingException {
		try {
			return sendRequest(message, operation, accessToken);
		} catch (IOException e) {
			throw new HuaweiMesssagingException(HuaweiMessaging.INTERNAL_ERROR,
					"Error while calling HCM backend service", e);
		}
	}

	private SendResponse sendRequest(TopicMessage message, String operation, String accessToken)
			throws IOException, HuaweiMesssagingException {
		this.hcmTopicUrl = MessageFormat.format(hcmTopicUrl, "", operation);
		HttpPost httpPost = new HttpPost(this.hcmTopicUrl);
		StringEntity entity = new StringEntity(new ObjectMapper().writeValueAsString(message), "UTF-8");
		httpPost.setHeader("Authorization", "Bearer " + accessToken);
		httpPost.setHeader("Content-Type", "application/json;charset=utf-8");
		httpPost.setEntity(entity);
		CloseableHttpResponse response = httpClient.execute(httpPost);
		String rpsContent = EntityUtils.toString(response.getEntity());
		int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode == 200) {
			JsonObject jsonObject = new Gson().fromJson(rpsContent, JsonObject.class);
			String code = jsonObject.get("code").getAsString();
			String msg = jsonObject.get("msg").getAsString();
			String requestId = jsonObject.get("requestId").getAsString();
			if (StringUtils.equals(code, "80000000")) {
				SendResponse sendResponse;
				if (StringUtils.equals(operation, TopicOperation.LIST.getValue())) {
					JsonArray topics = jsonObject.getAsJsonArray("topics");
					sendResponse = TopicListResponse.fromCode(code, msg, requestId, topics);
				} else {
					Integer failureCount = jsonObject.get("failureCount").getAsInt();
					Integer successCount = jsonObject.get("successCount").getAsInt();
					JsonArray errors = jsonObject.getAsJsonArray("errors");
					sendResponse = TopicSendResponse.fromCode(code, msg, requestId, failureCount, successCount, errors);
				}
				return sendResponse;
			} else {
				String errorMsg = MessageFormat.format("error code : {0}, error message : {1}", String.valueOf(code),
						msg);
				throw new HuaweiMesssagingException(HuaweiMessaging.KNOWN_ERROR, errorMsg);
			}
		}
		HttpResponseException exception = new HttpResponseException(statusCode, rpsContent);
		throw createExceptionFromResponse(exception);
	}

	/**
	 * send request
	 *
	 * @param message      message {@link Message}
	 * @param validateOnly A boolean indicating whether to send message for test or
	 *                     not.
	 * @param accessToken  A String for oauth
	 * @return {@link SendResponse}
	 * @throws IOException If a error occurs when sending request
	 */
	private SendResponse sendRequest(Message message, boolean validateOnly, String accessToken)
			throws IOException, HuaweiMesssagingException {

		logger.info("Inside Send Request for HMS Push Notification");

		Map<String, Object> map = createRequestMap(message, validateOnly);
		HttpPost httpPost = new HttpPost(this.HcmPushUrl);

		logger.info("HMS Push Notification Message :: " + new ObjectMapper().writeValueAsString(map));
		StringEntity entity = new StringEntity(new ObjectMapper().writeValueAsString(map), "UTF-8");

		httpPost.setHeader("Authorization", "Bearer " + accessToken);
		httpPost.setHeader("Content-Type", "application/json;charset=utf-8");
		httpPost.setEntity(entity);
		CloseableHttpResponse response = httpClient.execute(httpPost);
		String rpsContent = EntityUtils.toString(response.getEntity());
		int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode == 200) {
			JsonObject jsonObject = new Gson().fromJson(rpsContent, JsonObject.class);
			String code = jsonObject.get("code").getAsString();
			String msg = jsonObject.get("msg").getAsString();
			logger.info("After HMS request sent :: " + code + " :: " + msg);
			String requestId = jsonObject.get("requestId").getAsString();
			if (StringUtils.equals(code, "80000000")) {
				return SendResponse.fromCode(code, msg, requestId);
			} else {
				String errorMsg = MessageFormat.format("error code : {0}, error message : {1}", String.valueOf(code),
						msg);
				logger.info("HMS Push Notification Error:: " + errorMsg);
				throw new HuaweiMesssagingException(HuaweiMessaging.KNOWN_ERROR, errorMsg);
			}
		}
		HttpResponseException exception = new HttpResponseException(statusCode, rpsContent);
		throw createExceptionFromResponse(exception);
	}

	/**
	 * create the map of the request body, mostly for wrapping the message with
	 * validate_only
	 *
	 * @param message      A non-null {@link Message} to be sent.
	 * @param validateOnly A boolean indicating whether to send message for test or
	 *                     not.
	 * @return a map of request
	 */
	private Map<String, Object> createRequestMap(Message message, boolean validateOnly) {
		return new HashMap<String, Object>() {
			{
				put("validate_only", validateOnly);
				put("message", message);
			}
		};
	}

	private HuaweiMesssagingException createExceptionFromResponse(HttpResponseException e) {
		String msg = MessageFormat.format("Unexpected HTTP response with status : {0}, body : {1}", e.getStatusCode(),
				e.getMessage());
		return new HuaweiMesssagingException(HuaweiMessaging.UNKNOWN_ERROR, msg, e);
	}

	static HuaweiMessageClientImpl fromApp(HuaweiApp app) {
		String appId = ImplHuaweiTrampolines.getAppId(app);
		return HuaweiMessageClientImpl.builder().setAppId(appId).setHttpClient(app.getOption().getHttpClient()).build();
	}

	static Builder builder() {
		return new Builder();
	}

	static final class Builder {

		private String appId;
		private CloseableHttpClient httpClient;

		private Builder() {
		}

		public Builder setAppId(String appId) {
			this.appId = appId;
			return this;
		}

		public Builder setHttpClient(CloseableHttpClient httpClient) {
			this.httpClient = httpClient;
			return this;
		}

		public HuaweiMessageClientImpl build() {
			return new HuaweiMessageClientImpl(this);
		}
	}
}
