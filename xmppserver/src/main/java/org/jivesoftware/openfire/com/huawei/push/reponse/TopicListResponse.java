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
package org.jivesoftware.openfire.com.huawei.push.reponse;

import com.google.gson.JsonArray;

public class TopicListResponse extends SendResponse {
	private final JsonArray topics;

	public JsonArray getTopics() {
		return topics;
	}

	private TopicListResponse(String code, String msg, String requestId, JsonArray topics) {
		super(code, msg, requestId);
		this.topics = topics;
	}

	public static TopicListResponse fromCode(String code, String msg, String requestId, JsonArray topics) {
		return new TopicListResponse(code, msg, requestId, topics);
	}
}
