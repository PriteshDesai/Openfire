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
package org.jivesoftware.openfire.com.huawei.push.examples;

import org.jivesoftware.openfire.com.huawei.push.android.AndroidNotification;
import org.jivesoftware.openfire.com.huawei.push.android.BadgeNotification;
import org.jivesoftware.openfire.com.huawei.push.android.Button;
import org.jivesoftware.openfire.com.huawei.push.android.ClickAction;
import org.jivesoftware.openfire.com.huawei.push.android.Color;
import org.jivesoftware.openfire.com.huawei.push.android.LightSettings;
import org.jivesoftware.openfire.com.huawei.push.exception.HuaweiMesssagingException;
import org.jivesoftware.openfire.com.huawei.push.message.AndroidConfig;
import org.jivesoftware.openfire.com.huawei.push.message.Message;
import org.jivesoftware.openfire.com.huawei.push.message.Notification;
import org.jivesoftware.openfire.com.huawei.push.messaging.HuaweiApp;
import org.jivesoftware.openfire.com.huawei.push.messaging.HuaweiMessaging;
import org.jivesoftware.openfire.com.huawei.push.model.Importance;
import org.jivesoftware.openfire.com.huawei.push.model.Urgency;
import org.jivesoftware.openfire.com.huawei.push.model.Visibility;
import org.jivesoftware.openfire.com.huawei.push.reponse.SendResponse;
import org.jivesoftware.openfire.com.huawei.push.util.InitAppUtils;

import com.google.gson.JsonObject;

public class SendNotifyMessage {
	/**
	 * send notification message
	 *
	 * @throws HuaweiMesssagingException
	 */
	public void sendNotification() throws HuaweiMesssagingException {
		HuaweiApp app = InitAppUtils.initializeApp();
		HuaweiMessaging huaweiMessaging = HuaweiMessaging.getInstance(app);

		Notification notification = Notification.builder().setTitle("sample title").setBody("sample message body")
				.build();

		JsonObject multiLangKey = new JsonObject();
		JsonObject titleKey = new JsonObject();
		titleKey.addProperty("en", "好友请求");
		JsonObject bodyKey = new JsonObject();
		bodyKey.addProperty("en", "My name is %s, I am from %s.");
		multiLangKey.add("key1", titleKey);
		multiLangKey.add("key2", bodyKey);

		LightSettings lightSettings = LightSettings.builder()
				.setColor(Color.builder().setAlpha(0f).setRed(0f).setBlue(1f).setGreen(1f).build())
				.setLightOnDuration("3.5").setLightOffDuration("5S").build();

		AndroidNotification androidNotification = AndroidNotification.builder().setIcon("/raw/ic_launcher2")
				.setColor("#AACCDD").setSound("/raw/shake").setDefaultSound(true).setTag("tagBoom")
				.setClickAction(ClickAction.builder().setType(2).setUrl("https://www.huawei.com").build())
				.setBodyLocKey("key2").addBodyLocArgs("boy").addBodyLocArgs("dog").setTitleLocKey("key1")
				.addTitleLocArgs("Girl").addTitleLocArgs("Cat").setChannelId("Your Channel ID")
				.setNotifySummary("some summary").setMultiLangkey(multiLangKey).setStyle(1)
				.setBigTitle("Big Boom Title").setBigBody("Big Boom Body").setAutoClear(86400000).setNotifyId(486)
				.setGroup("Group1").setImportance(Importance.LOW.getValue()).setLightSettings(lightSettings)
				.setBadge(BadgeNotification.builder().setAddNum(1).setBadgeClass("Classic").build())
				.setVisibility(Visibility.PUBLIC.getValue()).setForegroundShow(true).addInboxContent("content1")
				.addInboxContent("content2").addInboxContent("content3").addInboxContent("content4")
				.addInboxContent("content5").addButton(Button.builder().setName("button1").setActionType(0).build())
				.addButton(Button.builder().setName("button2").setActionType(1).setIntentType(0)
						.setIntent("https://com.huawei.hms.hmsdemo/deeplink").build())
				.addButton(Button.builder().setName("button3").setActionType(4).setData("your share link").build())
				.build();

		AndroidConfig androidConfig = AndroidConfig.builder().setCollapseKey(-1).setUrgency(Urgency.HIGH.getValue())
				.setTtl("10000s").setBiTag("the_sample_bi_tag_for_receipt_service").setNotification(androidNotification)
				.build();

		Message message = Message.builder().setNotification(notification).setAndroidConfig(androidConfig).addToken(
				"AND8rUp4etqJvbakK7qQoCVgFHnROXzH8o7B8fTl9rMP5VRFN83zU3Nvmabm3xw7e3gZjyBbp_wfO1jP-UyDQcZN_CtjBpoa7nx1WaVFe_3mqXMJ6nXJNUZcDyO_-k3sSw")
				.build();

		SendResponse response = huaweiMessaging.sendMessage(message);

		System.out.println(response.getMsg());
	}
}
