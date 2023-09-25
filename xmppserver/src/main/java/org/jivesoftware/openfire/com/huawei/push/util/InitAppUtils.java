/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2019-2024. All rights reserved.
 */
package org.jivesoftware.openfire.com.huawei.push.util;

import org.jivesoftware.openfire.com.huawei.push.messaging.HuaweiApp;
import org.jivesoftware.openfire.com.huawei.push.messaging.HuaweiCredential;
import org.jivesoftware.openfire.com.huawei.push.messaging.HuaweiOption;
import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InitAppUtils {
	/**
	 * @return HuaweiApp
	 */

	private static final Logger logger = LoggerFactory.getLogger(InitAppUtils.class);

	public static HuaweiApp initializeApp() {
		String appId = JiveGlobals.getProperty("push.hms.appid");
		String appSecret = JiveGlobals.getProperty("push.hms.appsecret");

		logger.info("App Id :: " + appId);
		logger.info("App Secret :: " + appSecret);

		// Create HuaweiCredential
		// This appId and appSecret come from Huawei Developer Alliance
		return initializeApp(appId, appSecret);
	}

	private static HuaweiApp initializeApp(String appId, String appSecret) {
		HuaweiCredential credential = HuaweiCredential.builder().setAppId(appId).setAppSecret(appSecret).build();

		// Create HuaweiOption
		HuaweiOption option = HuaweiOption.builder().setCredential(credential).build();

		// Initialize HuaweiApp
//        return HuaweiApp.initializeApp(option);
		return HuaweiApp.getInstance(option);
	}
}
