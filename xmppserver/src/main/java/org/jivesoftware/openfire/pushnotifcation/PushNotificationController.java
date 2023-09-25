package org.jivesoftware.openfire.pushnotifcation;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.apache.commons.text.StringEscapeUtils;
import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.openfire.com.huawei.push.exception.HuaweiMesssagingException;
import org.jivesoftware.openfire.com.huawei.push.message.Message;
import org.jivesoftware.openfire.com.huawei.push.messaging.HuaweiApp;
import org.jivesoftware.openfire.com.huawei.push.messaging.HuaweiMessaging;
import org.jivesoftware.openfire.com.huawei.push.reponse.SendResponse;
import org.jivesoftware.openfire.com.huawei.push.util.InitAppUtils;
import org.jivesoftware.openfire.custom.dto.AndroidExceptSubDto;
import org.jivesoftware.openfire.custom.dto.AndroidSub15Dto;
import org.jivesoftware.openfire.custom.dto.AndroidSub1Dto;
import org.jivesoftware.openfire.custom.dto.ApnsSub10Dto;
import org.jivesoftware.openfire.custom.dto.ApnsSub11Dto;
import org.jivesoftware.openfire.custom.dto.ApnsSub14Dto;
import org.jivesoftware.openfire.custom.dto.ApnsSub15Dto;
import org.jivesoftware.openfire.custom.dto.ApnsSub16Dto;
import org.jivesoftware.openfire.custom.dto.ApnsSub18Dto;
import org.jivesoftware.openfire.custom.dto.ApnsSub19Dto;
import org.jivesoftware.openfire.custom.dto.ApnsSub1Dto;
import org.jivesoftware.openfire.custom.dto.ApnsSub21Dto;
import org.jivesoftware.openfire.custom.dto.ApnsSub22Dto;
import org.jivesoftware.openfire.custom.dto.ApnsSub23Dto;
import org.jivesoftware.openfire.custom.dto.ApnsSub2Dto;
import org.jivesoftware.openfire.custom.dto.ApnsSub3Dto;
import org.jivesoftware.openfire.custom.dto.ApnsSub6Dto;
import org.jivesoftware.openfire.custom.dto.ApnsSub7Dto;
import org.jivesoftware.openfire.custom.dto.ApnsSub8Dto;
import org.jivesoftware.openfire.custom.dto.ApnsSub9Dto;
import org.jivesoftware.openfire.custom.dto.Call;
import org.jivesoftware.openfire.custom.dto.CreateCallConference;
import org.jivesoftware.openfire.custom.dto.DeviceType;
import org.jivesoftware.openfire.custom.dto.GroupDetailDto;
import org.jivesoftware.openfire.custom.dto.MUCRoomInvitePushNotificationDto;
import org.jivesoftware.openfire.custom.dto.Moment;
import org.jivesoftware.openfire.custom.dto.PollDelete;
import org.jivesoftware.openfire.custom.dto.PollExpire;
import org.jivesoftware.openfire.custom.dto.PushNotificationPayloadDto;
import org.jivesoftware.openfire.custom.dto.UpdateCallConference;
import org.jivesoftware.openfire.custom.dto.UserDeviceEntity;
import org.jivesoftware.openfire.custom.dto.UserDeviceRosterDetails;
import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.xmpp.packet.JID;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.ApnsClientBuilder;
import com.eatthepath.pushy.apns.DeliveryPriority;
import com.eatthepath.pushy.apns.PushNotificationResponse;
import com.eatthepath.pushy.apns.PushType;
import com.eatthepath.pushy.apns.util.ApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.eatthepath.pushy.apns.util.TokenUtil;
import com.eatthepath.pushy.apns.util.concurrent.PushNotificationFuture;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

public class PushNotificationController {

	private static final Logger Log = LoggerFactory.getLogger(PushNotificationController.class);

	private static String firebaseApiUrl = "https://fcm.googleapis.com/fcm/send";

	/** The Constant USER_MESSAGE_COUNT. */
	private static final String USER_MESSAGE_COUNT = "select COUNT(1) from ofMessageArchive a "
			+ "join ofPresence p on (a.sentDate > CAST (p.offlineDate as BIGINT)) "
			+ "WHERE a.toJID = ? AND p.username = ?";

	private static final StringBuffer SQL_GET_DEVICE_DETAIL_FROM_JID = new StringBuffer(
			"SELECT username, jid, devicetoken, devicetype, ishuaweipush, voiptoken, isactive, channelname from ofuserdevicedeatil where jid = ?");

	private static final StringBuffer SQL_GET_DEVICE_DETAIL_FROM_USERNAME = new StringBuffer(
			"SELECT username, jid, devicetoken, devicetype, ishuaweipush, voiptoken, isactive from ofuserdevicedeatil where username = ?");

	private static final StringBuffer SQL_GET_CHANNEL_NAME_FROM_USERNAME = new StringBuffer(
			"SELECT channelname from ofuserdevicedeatil where username = ?");

	private static final StringBuffer SQL_GET_NAME_FROM_USERNAME = new StringBuffer(
			"SELECT name from ofuser where username = ?");

	private static final StringBuffer SQL_GET_GROUP_NAME_FROM_ROOM_ID = new StringBuffer(
			"SELECT name,naturalname from ofmucroom where roomid = ?");

	public UserDeviceEntity getUserDeviceDetailFromJID(String JID) {

		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try {
			conn = DbConnectionManager.getConnection();
			pstmt = conn.prepareStatement(SQL_GET_DEVICE_DETAIL_FROM_JID.toString());
			pstmt.setString(1, JID);
			rs = pstmt.executeQuery();

			if (rs != null && rs.next()) {
				UserDeviceEntity userDeviceEntity = new UserDeviceEntity();
				userDeviceEntity.setUserName(rs.getString("username"));
				userDeviceEntity.setJid(rs.getString("jid"));
				userDeviceEntity.setDeviceToken(rs.getString("devicetoken"));
				userDeviceEntity.setDeviceType(DeviceType.valueOf(rs.getString("devicetype")));
				userDeviceEntity.setHuaweiPush(rs.getBoolean("ishuaweipush"));
				userDeviceEntity.setVoipToken(rs.getString("voiptoken"));
				userDeviceEntity.setActive(rs.getBoolean("isactive"));
				userDeviceEntity.setChannelName(rs.getString("channelname"));
				return userDeviceEntity;
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
		} finally {
			DbConnectionManager.closeConnection(pstmt, conn);
		}
		return null;
	}

	public UserDeviceEntity getUserDeviceDetailFromUserName(String userName) {

		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try {
			conn = DbConnectionManager.getConnection();
			pstmt = conn.prepareStatement(SQL_GET_DEVICE_DETAIL_FROM_USERNAME.toString());
			pstmt.setString(1, userName);
			rs = pstmt.executeQuery();

			if (rs != null && rs.next()) {
				UserDeviceEntity userDeviceEntity = new UserDeviceEntity();
				userDeviceEntity.setUserName(rs.getString("username"));
				userDeviceEntity.setJid(rs.getString("jid"));
				userDeviceEntity.setDeviceToken(rs.getString("devicetoken"));
				userDeviceEntity.setDeviceType(DeviceType.valueOf(rs.getString("devicetype")));
				userDeviceEntity.setHuaweiPush(rs.getBoolean("ishuaweipush"));
				userDeviceEntity.setVoipToken(rs.getString("voiptoken"));
				userDeviceEntity.setActive(rs.getBoolean("isactive"));
				return userDeviceEntity;
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
		} finally {
			DbConnectionManager.closeConnection(pstmt, conn);
		}
		return null;
	}

	public GroupDetailDto getGroupNamefromGroupJID(long roomId) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try {
			conn = DbConnectionManager.getConnection();
			pstmt = conn.prepareStatement(SQL_GET_GROUP_NAME_FROM_ROOM_ID.toString());
			pstmt.setLong(1, roomId);
			rs = pstmt.executeQuery();

			if (rs != null && rs.next()) {
				return new GroupDetailDto(rs.getString("name"), rs.getString("naturalname"));
			}
		} catch (SQLException ex) {
			Log.error("SQL Exception Get Group Name.");
			ex.printStackTrace();
		} catch (Exception ex) {
			Log.error("Generic Exception Get Group Name.");
			ex.printStackTrace();
		} finally {
			DbConnectionManager.closeConnection(pstmt, conn);
		}
		return null;

	}

	public String getNameFromUserName(String userName) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		Connection conn1 = null;
		PreparedStatement pstmt1 = null;
		ResultSet rs1 = null;

		try {
			// get the Channel name
			conn = DbConnectionManager.getConnection();
			pstmt = conn.prepareStatement(SQL_GET_CHANNEL_NAME_FROM_USERNAME.toString());
			if (userName.contains("@"))
				pstmt.setString(1, userName.substring(0, userName.indexOf("@")));
			else
				pstmt.setString(1, userName);
			rs = pstmt.executeQuery();

			if (rs != null && rs.next()) {
				if (rs.getString("channelname") != null) {
					return rs.getString("channelname");
				} else {
					// if Channel name is null then get the first name
					conn1 = DbConnectionManager.getConnection();
					pstmt1 = conn1.prepareStatement(SQL_GET_NAME_FROM_USERNAME.toString());
					if (userName.contains("@"))
						pstmt1.setString(1, userName.substring(0, userName.indexOf("@")));
					else
						pstmt1.setString(1, userName);
					rs1 = pstmt1.executeQuery();

					if (rs1 != null && rs1.next()) {
						return rs1.getString("name");
					}
				}
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			DbConnectionManager.closeConnection(pstmt, conn);
		}
		return null;
	}

	public void publishNotification(List<UserDeviceRosterDetails> userDeviceRoasterDetails,
			PushNotificationPayloadDto notiDto) {

		for (UserDeviceRosterDetails dto : userDeviceRoasterDetails) {
			Log.info("Group Notification for : " + notiDto.getGroupName());
			publishNotification(new UserDeviceEntity(dto.getUserName(), dto.getJid(), dto.getDeviceType(),
					dto.getDeviceToken(), dto.isHuaweiPush(), dto.getVoipToken(), dto.isActive(), dto.getChannelName()),
					notiDto);
		}

	}

	public void invitePushNotificationToOfflineUser(UserDeviceEntity userDeviceEntity,
			MUCRoomInvitePushNotificationDto dto) {

		String ownerUserName = getNameFromUserName(dto.getRoomOwnerJID());
		String body = StringEscapeUtils.escapeJava(ownerUserName) + " added you in " + dto.getRoomName();
		dto.setBody(body);

		DeviceType deviceType = userDeviceEntity.getDeviceType();
		Gson gson = new Gson();

		String data = gson.toJson(dto);
		String title = dto.getRoomName();

		switch (deviceType) {

		case ANDROID:

			if (!userDeviceEntity.isHuaweiPush()) {
				Log.info("FCM Data Payload : " + data);
				SendFCMNotification(userDeviceEntity.getDeviceToken(), body, title, data, true);
			} else {
//				sendJPUSHNotification(userDeviceEntity.getDeviceToken(), title, data, body);
				sendHuaweiPushNotification(userDeviceEntity.getDeviceToken(), title, data, body);
			}

			break;
		case IOS:

			try {
				ApnsPayloadBuilder payloadBuilder = new SimpleApnsPayloadBuilder();
				payloadBuilder.setAlertTitle(title);
				payloadBuilder.setAlertBody(body);
				payloadBuilder.setMutableContent(true);

				payloadBuilder.addCustomProperty("payload", data);
				SendAPNSPushNotification(userDeviceEntity.getDeviceToken(), payloadBuilder);

			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}

			break;
		default:
			break;
		}
	}

	public boolean publishNotification(UserDeviceEntity userDeviceEntity, PushNotificationPayloadDto dto) {
		DeviceType deviceType = userDeviceEntity.getDeviceType();
		Gson gson = new Gson();
		switch (deviceType) {
		case ANDROID:

			String data = null;
			String title = dto.getTitle() == null ? StringEscapeUtils.escapeJava(dto.getFromName()) : dto.getTitle();
			String body = dto.getAndroidBody();
			switch (dto.getSubject()) {
			case "1":
			case "51":
				AndroidSub1Dto sub1Dto = new AndroidSub1Dto();
				sub1Dto.setType(dto.getMessageType());
				sub1Dto.setMessageId(dto.getMessageId());
				sub1Dto.setFromJID(dto.getFromJID());
				sub1Dto.setToJID(dto.getToJID());
				sub1Dto.setFromName(StringEscapeUtils.escapeJava(dto.getFromName()));
				sub1Dto.setGroupName(dto.getGroupName());
				sub1Dto.setSenderImage(dto.getSenderImage());
				sub1Dto.setSenderUUID(dto.getSenderUUID());
				sub1Dto.setSubject(dto.getSubject());
				sub1Dto.setBody(dto.getPayload().toString());
				sub1Dto.setMessageTime(dto.getMessageTime());
				sub1Dto.setTitle(title);
				data = gson.toJson(sub1Dto);

				if (!userDeviceEntity.isHuaweiPush()) {
					Log.info("FCM Data Payload : " + data);
					SendFCMNotification(userDeviceEntity.getDeviceToken(), body, title, data,
							sub1Dto.getType().equalsIgnoreCase("groupchat"));
				} else {
//					sendJPUSHNotification(userDeviceEntity.getDeviceToken(), title, data, body);
					sendHuaweiPushNotification(userDeviceEntity.getDeviceToken(), title, data, body);
				}

				break;
			case "2":
			case "3":
			case "6":
			case "7":
			case "8":
			case "9":
			case "10":
			case "11":
			case "14":
			case "16":
			case "18":
			case "19":
			case "21":
			case "22":
			case "23":
			case "24":
				AndroidExceptSubDto remainSubjectDto = new AndroidExceptSubDto();
				remainSubjectDto.setType(dto.getMessageType());
				remainSubjectDto.setMessageId(dto.getMessageId());
				remainSubjectDto.setFromJID(dto.getFromJID());
				remainSubjectDto.setToJID(dto.getToJID());
				remainSubjectDto.setFromName(StringEscapeUtils.escapeJava(dto.getFromName()));
				remainSubjectDto.setGroupName(dto.getGroupName());
				remainSubjectDto.setMessageTime(dto.getMessageTime());
				remainSubjectDto.setSubject(dto.getSubject());
				remainSubjectDto.setPayload(dto.getPayload());
				remainSubjectDto.setTitle(title);
				remainSubjectDto.setBody(body);
//				if (dto.getSubject().equals("11")) {
				remainSubjectDto.setSenderImage(dto.getSenderImage());
//				}
				data = gson.toJson(remainSubjectDto);
				if (!userDeviceEntity.isHuaweiPush()) {
					SendFCMNotification(userDeviceEntity.getDeviceToken(), body, title, data,
							remainSubjectDto.getType().equalsIgnoreCase("groupchat"));
				} else {
//					sendJPUSHNotification(userDeviceEntity.getDeviceToken(), title, data, body);
					sendHuaweiPushNotification(userDeviceEntity.getDeviceToken(), title, data, body);
				}
				break;

			case "4":
			case "5":
				break;

			case "15":
				AndroidSub15Dto sub15Dto = new AndroidSub15Dto();
				Call call = (Call) dto.getPayload();
				sub15Dto.setType(dto.getMessageType());
				sub15Dto.setRoomId(call.getId());
				sub15Dto.setInitiatorId(call.getInitiatorId());
				sub15Dto.setSubject(dto.getSubject());
				sub15Dto.setUserId(call.getUsers().getUser());
				sub15Dto.setReason(call.getReason());
				sub15Dto.setMessageTime(dto.getMessageTime());
				sub15Dto.setCallType(call.getType());
				sub15Dto.setGroupName(dto.getGroupName());
				sub15Dto.setSenderImage(dto.getSenderImage());
				sub15Dto.setGroupJID(dto.getGroupJID() + "@conference." + JiveGlobals.getProperty("xmpp.domain"));
				data = gson.toJson(sub15Dto);
				Log.info("Android Call Notification Dto :: " + data);

				Log.info("****************** Android CASE CALLED");

				if (!userDeviceEntity.isHuaweiPush()) {
					SendFCMCallNotification(userDeviceEntity.getDeviceToken(), data);
				} else {
//					sendJPUSHNotification(userDeviceEntity.getDeviceToken(), "", data, "");
					sendHuaweiPushNotification(userDeviceEntity.getDeviceToken(), "", data, "");
				}
				break;

			default:
				break;
			}
			break;

		case IOS:
			try {
				title = dto.getTitle() == null ? dto.getFromName() : dto.getTitle();
				body = dto.getBody();
				ApnsPayloadBuilder payloadBuilder = null;

				int unreadMessageCount = getUnReadMessagesCount(new JID(userDeviceEntity.getJid())) + 1;
				Log.info(userDeviceEntity.getJid() + " :: Unread Message Count :: " + unreadMessageCount);
				switch (dto.getSubject()) {

				case "1":
				case "51":
					payloadBuilder = new SimpleApnsPayloadBuilder();
					payloadBuilder.setAlertTitle(title);
					payloadBuilder.setAlertBody(body);
					payloadBuilder.setMutableContent(true);
					payloadBuilder.setSound(ApnsPayloadBuilder.DEFAULT_SOUND_FILENAME);
					payloadBuilder.setBadgeNumber(unreadMessageCount);

					ApnsSub1Dto sub1Dto = new ApnsSub1Dto();
					sub1Dto.setFromJID(dto.getFromJID());
					sub1Dto.setToJID(dto.getToJID());
					sub1Dto.setFromName(dto.getFromName());
					sub1Dto.setGroupName(dto.getGroupName());
					sub1Dto.setSubject(dto.getSubject());
					sub1Dto.setType(dto.getMessageType());
					sub1Dto.setId(dto.getMessageId());
					sub1Dto.setBody(dto.getPayload().toString());
					sub1Dto.setMessageTime(dto.getMessageTime());

					payloadBuilder.addCustomProperty("payload", gson.toJson(sub1Dto));
					SendAPNSPushNotification(userDeviceEntity.getDeviceToken(), payloadBuilder);
					break;
				case "2":

					payloadBuilder = new SimpleApnsPayloadBuilder();
					payloadBuilder.setAlertTitle(title);
					payloadBuilder.setAlertBody(body);
					payloadBuilder.setMutableContent(true);
					payloadBuilder.setSound(ApnsPayloadBuilder.DEFAULT_SOUND_FILENAME);
					payloadBuilder.setBadgeNumber(unreadMessageCount);

					ApnsSub2Dto sub2Dto = new ApnsSub2Dto();

					sub2Dto.setMessageTime(dto.getMessageTime());
					sub2Dto.setFromJID(dto.getFromJID());
					sub2Dto.setToJID(dto.getToJID());
					sub2Dto.setFromName(dto.getFromName());
					sub2Dto.setGroupName(dto.getGroupName());
					sub2Dto.setType(dto.getMessageType());
					sub2Dto.setSubject(dto.getSubject());
					sub2Dto.setMessageMedia(dto.getPayload());

					payloadBuilder.addCustomProperty("payload", gson.toJson(sub2Dto));
					SendAPNSPushNotification(userDeviceEntity.getDeviceToken(), payloadBuilder);
					break;
				case "3":

					payloadBuilder = new SimpleApnsPayloadBuilder();
					payloadBuilder.setAlertTitle(title);
					payloadBuilder.setAlertBody(body);
					payloadBuilder.setMutableContent(true);
					payloadBuilder.setSound(ApnsPayloadBuilder.DEFAULT_SOUND_FILENAME);
					payloadBuilder.setBadgeNumber(unreadMessageCount);

					ApnsSub3Dto sub3Dto = new ApnsSub3Dto();
					sub3Dto.setType(dto.getMessageType());
					sub3Dto.setFromJID(dto.getFromJID());
					sub3Dto.setToJID(dto.getToJID());
					sub3Dto.setFromName(dto.getFromName());
					sub3Dto.setGroupName(dto.getGroupName());
					sub3Dto.setMessageTime(dto.getMessageTime());
					sub3Dto.setSubject(dto.getSubject());
					sub3Dto.setMessageMedia(dto.getPayload());

					payloadBuilder.addCustomProperty("payload", gson.toJson(sub3Dto));
					SendAPNSPushNotification(userDeviceEntity.getDeviceToken(), payloadBuilder);
					break;
				case "4":
				case "5":
					break;
				case "6":
					payloadBuilder = new SimpleApnsPayloadBuilder();
					payloadBuilder.setAlertTitle(title);
					payloadBuilder.setAlertBody(body);
					payloadBuilder.setMutableContent(true);
					payloadBuilder.setSound(ApnsPayloadBuilder.DEFAULT_SOUND_FILENAME);
					payloadBuilder.setBadgeNumber(unreadMessageCount);

					ApnsSub6Dto sub6Dto = new ApnsSub6Dto();
					sub6Dto.setType(dto.getMessageType());
					sub6Dto.setFromJID(dto.getFromJID());
					sub6Dto.setToJID(dto.getToJID());
					sub6Dto.setFromName(dto.getFromName());
					sub6Dto.setGroupName(dto.getGroupName());
					sub6Dto.setMessageTime(dto.getMessageTime());
					sub6Dto.setSubject(dto.getSubject());
					sub6Dto.setMessageMedia(dto.getPayload());

					payloadBuilder.addCustomProperty("payload", gson.toJson(sub6Dto));
					SendAPNSPushNotification(userDeviceEntity.getDeviceToken(), payloadBuilder);
					break;
				case "7":

					payloadBuilder = new SimpleApnsPayloadBuilder();
					payloadBuilder.setAlertTitle(title);
					payloadBuilder.setAlertBody(body);
					payloadBuilder.setMutableContent(true);
					payloadBuilder.setSound(ApnsPayloadBuilder.DEFAULT_SOUND_FILENAME);
					payloadBuilder.setBadgeNumber(unreadMessageCount);

					ApnsSub7Dto sub7Dto = new ApnsSub7Dto();
					sub7Dto.setType(dto.getMessageType());
					sub7Dto.setFromJID(dto.getFromJID());
					sub7Dto.setToJID(dto.getToJID());
					sub7Dto.setFromName(dto.getFromName());
					sub7Dto.setGroupName(dto.getGroupName());
					sub7Dto.setMessageTime(dto.getMessageTime());
					sub7Dto.setSubject(dto.getSubject());
					sub7Dto.setLocation(dto.getPayload());
					payloadBuilder.addCustomProperty("payload", gson.toJson(sub7Dto));
					SendAPNSPushNotification(userDeviceEntity.getDeviceToken(), payloadBuilder);
					break;
				case "8":
					payloadBuilder = new SimpleApnsPayloadBuilder();

					payloadBuilder.setAlertTitle(title);
					payloadBuilder.setAlertBody(body);
					payloadBuilder.setMutableContent(true);
					payloadBuilder.setSound(ApnsPayloadBuilder.DEFAULT_SOUND_FILENAME);
					payloadBuilder.setBadgeNumber(unreadMessageCount);

					ApnsSub8Dto sub8Dto = new ApnsSub8Dto();
					sub8Dto.setType(dto.getMessageType());
					sub8Dto.setFromJID(dto.getFromJID());
					sub8Dto.setToJID(dto.getToJID());
					sub8Dto.setFromName(dto.getFromName());
					sub8Dto.setGroupName(dto.getGroupName());
					sub8Dto.setMessageTime(dto.getMessageTime());
					sub8Dto.setSubject(dto.getSubject());
					sub8Dto.setContact(dto.getPayload());
					payloadBuilder.addCustomProperty("payload", gson.toJson(sub8Dto));
					SendAPNSPushNotification(userDeviceEntity.getDeviceToken(), payloadBuilder);
					break;
				case "9":
					payloadBuilder = new SimpleApnsPayloadBuilder();

					payloadBuilder.setAlertTitle(title);
					payloadBuilder.setAlertBody(body);
					payloadBuilder.setMutableContent(true);
					payloadBuilder.setSound(ApnsPayloadBuilder.DEFAULT_SOUND_FILENAME);
					payloadBuilder.setBadgeNumber(unreadMessageCount);

					ApnsSub9Dto sub9Dto = new ApnsSub9Dto();
					sub9Dto.setType(dto.getMessageType());
					sub9Dto.setFromJID(dto.getFromJID());
					sub9Dto.setToJID(dto.getToJID());
					sub9Dto.setFromName(dto.getFromName());
					sub9Dto.setGroupName(dto.getGroupName());
					sub9Dto.setMessageTime(dto.getMessageTime());
					sub9Dto.setSubject(dto.getSubject());
					sub9Dto.setRss(dto.getPayload());

					payloadBuilder.addCustomProperty("payload", gson.toJson(sub9Dto));
					SendAPNSPushNotification(userDeviceEntity.getDeviceToken(), payloadBuilder);
					break;
				case "10":
					payloadBuilder = new SimpleApnsPayloadBuilder();

					payloadBuilder.setAlertTitle(title);
					payloadBuilder.setAlertBody(body);
					payloadBuilder.setMutableContent(true);
					payloadBuilder.setSound(ApnsPayloadBuilder.DEFAULT_SOUND_FILENAME);
					payloadBuilder.setBadgeNumber(unreadMessageCount);

					ApnsSub10Dto sub10Dto = new ApnsSub10Dto();
					sub10Dto.setType(dto.getMessageType());
					sub10Dto.setFromJID(dto.getFromJID());
					sub10Dto.setToJID(dto.getToJID());
					sub10Dto.setFromName(dto.getFromName());
					sub10Dto.setGroupName(dto.getGroupName());
					sub10Dto.setMessageTime(dto.getMessageTime());
					sub10Dto.setSubject(dto.getSubject());
					sub10Dto.setRelay(dto.getPayload());

					payloadBuilder.addCustomProperty("payload", gson.toJson(sub10Dto));
					SendAPNSPushNotification(userDeviceEntity.getDeviceToken(), payloadBuilder);
					break;
				case "11":
					payloadBuilder = new SimpleApnsPayloadBuilder();
					payloadBuilder.setAlertTitle(title);
					payloadBuilder.setAlertBody(body);
					payloadBuilder.setMutableContent(true);
					payloadBuilder.setSound(ApnsPayloadBuilder.DEFAULT_SOUND_FILENAME);
					payloadBuilder.setBadgeNumber(unreadMessageCount);

					ApnsSub11Dto sub11Dto = new ApnsSub11Dto();
					sub11Dto.setType(dto.getMessageType());
					sub11Dto.setMessageId(dto.getMessageId());
					sub11Dto.setFromJID(dto.getFromJID());
					sub11Dto.setToJID(dto.getToJID());
					sub11Dto.setSenderImage(dto.getSenderImage());
					sub11Dto.setSenderUUID(dto.getSenderUUID());
					sub11Dto.setFromName(dto.getFromName());
					sub11Dto.setGroupName(dto.getGroupName());
					sub11Dto.setMessageTime(dto.getMessageTime());
					sub11Dto.setSubject(dto.getSubject());
					sub11Dto.setOpinionPoll(dto.getPayload());

					payloadBuilder.addCustomProperty("payload", gson.toJson(sub11Dto));
					SendAPNSPushNotification(userDeviceEntity.getDeviceToken(), payloadBuilder);
					break;
				case "14":
					payloadBuilder = new SimpleApnsPayloadBuilder();
					payloadBuilder.setAlertTitle(title);
					payloadBuilder.setAlertBody(body);
					payloadBuilder.setMutableContent(true);
					payloadBuilder.setSound(ApnsPayloadBuilder.DEFAULT_SOUND_FILENAME);
					payloadBuilder.setBadgeNumber(unreadMessageCount);

					ApnsSub14Dto sub14Dto = new ApnsSub14Dto();
					sub14Dto.setType(dto.getMessageType());
					sub14Dto.setFromJID(dto.getFromJID());
					sub14Dto.setToJID(dto.getToJID());
					sub14Dto.setFromName(dto.getFromName());
					sub14Dto.setGroupName(dto.getGroupName());
					sub14Dto.setMessageTime(dto.getMessageTime());
					sub14Dto.setSubject(dto.getSubject());
					sub14Dto.setPollUpdate(dto.getPayload());

					payloadBuilder.addCustomProperty("payload", gson.toJson(sub14Dto));
					SendAPNSPushNotification(userDeviceEntity.getDeviceToken(), payloadBuilder);
					break;
				case "15":
					payloadBuilder = new SimpleApnsPayloadBuilder();

					payloadBuilder.setAlertTitle(title);
					payloadBuilder.setAlertBody(body);
					payloadBuilder.setContentAvailable(false);
					payloadBuilder.setMutableContent(false);

					ApnsSub15Dto sub15Dto = new ApnsSub15Dto();
					Call call = (Call) dto.getPayload();
					sub15Dto.setRoomId(call.getId());
					sub15Dto.setInitiatorId(call.getInitiatorId());
					sub15Dto.setSubject(dto.getSubject());
					sub15Dto.setType(call.getType());
					sub15Dto.setUserId(call.getUsers().getUser());
					sub15Dto.setReason(call.getReason());
					sub15Dto.setMessageTime(dto.getMessageTime());
					sub15Dto.setGroupName(dto.getGroupName());
					sub15Dto.setOpponentImageUrl(call.getOpponentImageUrl());
					sub15Dto.setGroupJID(dto.getGroupJID() + "@conference." + JiveGlobals.getProperty("xmpp.domain"));
					payloadBuilder.addCustomProperty("payload", gson.toJson(sub15Dto));

//					if (call.getType().equals("audio"))
//						SendAPNSPushNotification(userDeviceEntity.getDeviceToken(), payloadBuilder);
//					else
					SendAPNVoipNotification(userDeviceEntity.getVoipToken(), payloadBuilder);

					break;
				case "16":
					payloadBuilder = new SimpleApnsPayloadBuilder();

					payloadBuilder.setAlertTitle(title);
					payloadBuilder.setAlertBody(body);
					payloadBuilder.setMutableContent(true);
					payloadBuilder.setSound(ApnsPayloadBuilder.DEFAULT_SOUND_FILENAME);
					payloadBuilder.setBadgeNumber(unreadMessageCount);

					ApnsSub16Dto sub16Dto = new ApnsSub16Dto();
					sub16Dto.setType(dto.getMessageType());
					sub16Dto.setMessageId(dto.getMessageId());
					sub16Dto.setFromJID(dto.getFromJID());
					sub16Dto.setToJID(dto.getToJID());
					sub16Dto.setFromName(dto.getFromName());
					sub16Dto.setGroupName(dto.getGroupName());
					sub16Dto.setMessageTime(dto.getMessageTime());
					sub16Dto.setSubject(dto.getSubject());
					sub16Dto.setNotification(dto.getPayload());

					payloadBuilder.addCustomProperty("payload", gson.toJson(sub16Dto));
					SendAPNSPushNotification(userDeviceEntity.getDeviceToken(), payloadBuilder);

					break;
				case "18":
					payloadBuilder = new SimpleApnsPayloadBuilder();
					payloadBuilder.setAlertTitle(title);
					payloadBuilder.setAlertBody(body);
					payloadBuilder.setMutableContent(true);
					payloadBuilder.setSound(ApnsPayloadBuilder.DEFAULT_SOUND_FILENAME);
					payloadBuilder.setBadgeNumber(unreadMessageCount);

					ApnsSub18Dto sub18Dto = new ApnsSub18Dto();
					PollDelete pollDelete = (PollDelete) dto.getPayload();
					sub18Dto.setType(dto.getMessageType());
					sub18Dto.setMessageId(dto.getMessageId());
					sub18Dto.setFromJID(dto.getFromJID());
					sub18Dto.setToJID(dto.getToJID());
					sub18Dto.setFromName(dto.getFromName());
					sub18Dto.setGroupName(dto.getGroupName());
					sub18Dto.setMessageTime(dto.getMessageTime());
					sub18Dto.setSubject(dto.getSubject());
					sub18Dto.setPollDelete(pollDelete);

					payloadBuilder.addCustomProperty("payload", gson.toJson(sub18Dto));
					SendAPNSPushNotification(userDeviceEntity.getDeviceToken(), payloadBuilder);
					break;
				case "19":
					payloadBuilder = new SimpleApnsPayloadBuilder();
					payloadBuilder.setAlertTitle(title);
					payloadBuilder.setAlertBody(body);
					payloadBuilder.setMutableContent(true);
					payloadBuilder.setSound(ApnsPayloadBuilder.DEFAULT_SOUND_FILENAME);
					payloadBuilder.setBadgeNumber(unreadMessageCount);

					ApnsSub19Dto sub19Dto = new ApnsSub19Dto();
					PollExpire pollExpire = (PollExpire) dto.getPayload();
					sub19Dto.setType(dto.getMessageType());
					sub19Dto.setMessageId(dto.getMessageId());
					sub19Dto.setFromJID(dto.getFromJID());
					sub19Dto.setToJID(dto.getToJID());
					sub19Dto.setFromName(dto.getFromName());
					sub19Dto.setGroupName(dto.getGroupName());
					sub19Dto.setMessageTime(dto.getMessageTime());
					sub19Dto.setSubject(dto.getSubject());
					sub19Dto.setPollResult(pollExpire);

					payloadBuilder.addCustomProperty("payload", gson.toJson(sub19Dto));
					SendAPNSPushNotification(userDeviceEntity.getDeviceToken(), payloadBuilder);
					break;
				case "21":
					payloadBuilder = new SimpleApnsPayloadBuilder();
					payloadBuilder.setAlertTitle(title);
					payloadBuilder.setAlertBody(body);
					payloadBuilder.setMutableContent(true);
					payloadBuilder.setSound(ApnsPayloadBuilder.DEFAULT_SOUND_FILENAME);
					payloadBuilder.setBadgeNumber(unreadMessageCount);

					ApnsSub21Dto sub21Dto = new ApnsSub21Dto();
					Moment moment = (Moment) dto.getPayload();
					sub21Dto.setType(dto.getMessageType());
					sub21Dto.setMessageId(dto.getMessageId());
					sub21Dto.setFromJID(dto.getFromJID());
					sub21Dto.setToJID(dto.getToJID());
					sub21Dto.setFromName(dto.getFromName());
					sub21Dto.setGroupName(dto.getGroupName());
					sub21Dto.setMessageTime(dto.getMessageTime());
					sub21Dto.setSubject(dto.getSubject());
					sub21Dto.setMoment(moment);

					payloadBuilder.addCustomProperty("payload", gson.toJson(sub21Dto));
					SendAPNSPushNotification(userDeviceEntity.getDeviceToken(), payloadBuilder);
					break;
				case "22":
					payloadBuilder = new SimpleApnsPayloadBuilder();
					payloadBuilder.setAlertTitle(title);
					payloadBuilder.setAlertBody(body);
					payloadBuilder.setMutableContent(true);
					payloadBuilder.setSound(ApnsPayloadBuilder.DEFAULT_SOUND_FILENAME);
					payloadBuilder.setBadgeNumber(unreadMessageCount);

					ApnsSub22Dto sub22Dto = new ApnsSub22Dto();
					CreateCallConference callConference = (CreateCallConference) dto.getPayload();
					sub22Dto.setType(dto.getMessageType());
					sub22Dto.setMessageId(dto.getMessageId());
					sub22Dto.setFromJID(dto.getFromJID());
					sub22Dto.setToJID(dto.getToJID());
					sub22Dto.setFromName(dto.getFromName());
					sub22Dto.setGroupName(dto.getGroupName());
					sub22Dto.setMessageTime(dto.getMessageTime());
					sub22Dto.setSubject(dto.getSubject());
					sub22Dto.setCallConference(callConference);

					payloadBuilder.addCustomProperty("payload", gson.toJson(sub22Dto));
					SendAPNSPushNotification(userDeviceEntity.getDeviceToken(), payloadBuilder);
					break;
				case "23":
					payloadBuilder = new SimpleApnsPayloadBuilder();
					payloadBuilder.setAlertTitle(title);
					payloadBuilder.setAlertBody(body);
					payloadBuilder.setMutableContent(true);
					payloadBuilder.setSound(ApnsPayloadBuilder.DEFAULT_SOUND_FILENAME);
					payloadBuilder.setBadgeNumber(unreadMessageCount);

					ApnsSub23Dto sub23Dto = new ApnsSub23Dto();
					UpdateCallConference updateCallConference = (UpdateCallConference) dto.getPayload();
					sub23Dto.setType(dto.getMessageType());
					sub23Dto.setMessageId(dto.getMessageId());
					sub23Dto.setFromJID(dto.getFromJID());
					sub23Dto.setToJID(dto.getToJID());
					sub23Dto.setFromName(dto.getFromName());
					sub23Dto.setGroupName(dto.getGroupName());
					sub23Dto.setMessageTime(dto.getMessageTime());
					sub23Dto.setSubject(dto.getSubject());
					sub23Dto.setUpdateCallConference(updateCallConference);

					payloadBuilder.addCustomProperty("payload", gson.toJson(sub23Dto));
					SendAPNSPushNotification(userDeviceEntity.getDeviceToken(), payloadBuilder);
					break;
				default:
					break;
				}

			} catch (Exception e) {
				Log.error("Fail to send APNS Notification" + e.getMessage());
				e.printStackTrace();
			}
			break;
		default:
			break;
		}

		return true;
	}

	public int getUnReadMessagesCount(JID jid) {
		int messageCount = 0;
		Connection con = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			con = DbConnectionManager.getConnection();
			pstmt = con.prepareStatement(USER_MESSAGE_COUNT);
			pstmt.setString(1, jid.toBareJID());
			pstmt.setString(2, jid.getNode());
			rs = pstmt.executeQuery();
			if (rs.next()) {
				messageCount = rs.getInt(1);
			}
		} catch (SQLException sqle) {
			Log.error(sqle.getMessage(), sqle);
		} finally {
			DbConnectionManager.closeConnection(rs, pstmt, con);
		}
		return messageCount;
	}

	public void SendAPNSPushNotification(String deviceToken, ApnsPayloadBuilder payloadBuilder)
			throws InterruptedException, ExecutionException {
		SimpleApnsPushNotification pushNotification;
		PushNotificationFuture<SimpleApnsPushNotification, PushNotificationResponse<SimpleApnsPushNotification>> sendNotificationFuture = null;
		try {
			// stagging environment
			if ("openfire.gatherhall.com".equals(JiveGlobals.getProperty("xmpp.domain"))) {
				final ApnsClient apnsClient = new ApnsClientBuilder()
						.setApnsServer(ApnsClientBuilder.DEVELOPMENT_APNS_HOST)
						.setClientCredentials(new File("/usr/share/openfire/conf/Certificates_GatherHall.p12"),
								JiveGlobals.getProperty("push.apns.pass"))
						.build();
				final String payload = payloadBuilder.build();
				Log.info("SendAPNSPushNotification(): deviceToken is : " + deviceToken);
				final String token = TokenUtil.sanitizeTokenString(deviceToken);
				Log.info("SendAPNSPushNotification(): sanitize deviceToken is : " + token);
				pushNotification = new SimpleApnsPushNotification(token, "com.gatherhall.app", payload);
				sendNotificationFuture = apnsClient.sendNotification(pushNotification);

				if (sendNotificationFuture.get().isAccepted()) {
					Log.info("SendAPNSPushNotification():: Push notification accepted by APNs gateway.");
				} else {
					Log.info("SendAPNSPushNotification():: Notification rejected by the APNs gateway: "
							+ sendNotificationFuture.get().getRejectionReason());

					if (sendNotificationFuture.get().getTokenInvalidationTimestamp() != null) {
						Log.info("\t…and the token is invalid as of "
								+ sendNotificationFuture.get().getTokenInvalidationTimestamp().get());
					}
				}
			}

			// Production environment
			if ("chat.gatherhall.com".equals(JiveGlobals.getProperty("xmpp.domain"))) {
				final ApnsClient apnsClient = new ApnsClientBuilder()
						.setApnsServer(ApnsClientBuilder.PRODUCTION_APNS_HOST)
						.setClientCredentials(new File("/usr/share/openfire/conf/Certificates_GatherHall.p12"),
								JiveGlobals.getProperty("push.apns.pass"))
						.build();
				final String payload = payloadBuilder.build();

				Log.info("SendAPNSPushNotification(): deviceToken is : " + deviceToken);
				final String token = TokenUtil.sanitizeTokenString(deviceToken);
				Log.info("SendAPNSPushNotification(): sanitize deviceToken is : " + token);
				pushNotification = new SimpleApnsPushNotification(token, "com.gatherhall.app", payload);
				sendNotificationFuture = apnsClient.sendNotification(pushNotification);

				if (sendNotificationFuture.get().isAccepted()) {
					Log.info("SendAPNSPushNotification():: Push notification accepted by APNs gateway.");
				} else {
					Log.info("SendAPNSPushNotification():: Notification rejected by the APNs gateway: "
							+ sendNotificationFuture.get().getRejectionReason());

					if (sendNotificationFuture.get().getTokenInvalidationTimestamp() != null) {
						Log.info("\t…and the token is invalid as of "
								+ sendNotificationFuture.get().getTokenInvalidationTimestamp().get());
					}
				}

			}

			Log.info("APNS Notification not send Rejection Reason is :: " + sendNotificationFuture.get().getRejectionReason());
		} catch (IOException e) {
			Log.error("Fail to send APNS Alert Push Notification :: " + e.getLocalizedMessage());
		}

	}

	public void sendHuaweiPushNotification(String deviceToken, String title, String data, String body) {

		try {
			// Create Huawei app
			HuaweiApp app = InitAppUtils.initializeApp();
			HuaweiMessaging huaweiMessaging = HuaweiMessaging.getInstance(app);

			Log.info("App ID :: " + app.getAppId());

			Message message = null;

			message = Message.builder().setData(data).addToken(deviceToken).build();

			Log.info("Message :: " + new ObjectMapper().writeValueAsString(message));

			SendResponse response = huaweiMessaging.sendMessage(message);

			Log.info("Response Message :: " + response.getMsg());
			Log.info("Response Code :: " + response.getCode());

		} catch (HuaweiMesssagingException e) {
			Log.info("Fail to Send Huawei Push Notification" + e.getLocalizedMessage());
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			Log.info("Fail to parse message" + e.getLocalizedMessage());
			e.printStackTrace();
		}
	}

	public Map<String, Object> SendFCMNotification(String deviceId, String body, String title, String data,
			boolean isGroupChat) {
		try {
			String fcmKey = JiveGlobals.getProperty("push.fcm.token");
			RestTemplate restTemplate = new RestTemplate();
			HttpHeaders headers = new HttpHeaders();
			headers.add("Content-Type", "application/json");
			headers.add("Authorization", "key=" + fcmKey);
			String message1 = null;
			if (!isGroupChat) {

				Gson gson = new Gson();
				String subject = gson.fromJson(data, AndroidExceptSubDto.class).getSubject();

				if (subject.equals("11") || subject.equals("14") || subject.equals("18") || subject.equals("19")) {
					message1 = String.format("{\"to\":\"%s\","
//							+ "\"notification\" : {\"sound\" : \"default\",\"body\" :  \"%s\", \"title\" : \"%s\",\"content_available\" : true,"
							+ "\"priority\" : \"high\", \"data\" : %s}", deviceId, data);
				} else {
					message1 = String.format("{\"to\":\"%s\","
							+ "\"notification\" : {\"sound\" : \"default\",\"body\" :  \"%s\", \"title\" : \"%s\",\"content_available\" : true,\"priority\" : \"high\"},"
							+ "\"data\" : %s}", deviceId, body, title, data);
				}
			} else {
				message1 = String.format("{\"to\":\"%s\","
						// + "\"notification\" : {\"sound\" : \"default\",\"content_available\" :
						// true,"+
						+ "\"priority\" : \"high\", \"android\":{\"ttl\":\"500s\"} ," + "\"data\" : %s}", deviceId,
						data);
			}
			HttpEntity<String> request = new HttpEntity<>(message1, headers);
			ResponseEntity<String> response = restTemplate.exchange(firebaseApiUrl, HttpMethod.POST, request,
					String.class);
			String str = response.getBody();
			Log.info("FCM SENDED DATA :: " + data);
			Log.info("Response From Rest client :: " + str);
			return null;
		} catch (Exception e) {
			Log.info("Fail to Send FCM Push Notification" + e.getLocalizedMessage());
			return null;
		}
	}

	public Map<String, Object> SendFCMCallNotification(String deviceId, String data) {
		try {
			String fcmKey = JiveGlobals.getProperty("push.fcm.token");
			RestTemplate restTemplate = new RestTemplate();
			HttpHeaders headers = new HttpHeaders();
			headers.add("Content-Type", "application/json");
			headers.add("Authorization", "key=" + fcmKey);
			String message1 = String.format("{\"to\":\"%s\","
					// + "\"notification\" : {\"sound\" : \"default\",\"content_available\" :
					// true,"+
					+ "\"priority\" : \"high\", \"android\":{\"ttl\":\"500s\"} ," + "\"data\" : %s}", deviceId, data);
			HttpEntity<String> request = new HttpEntity<>(message1, headers);
			ResponseEntity<String> response = restTemplate.exchange(firebaseApiUrl, HttpMethod.POST, request,
					String.class);
			String str = response.getBody();
			Log.info("Send Call notification to : " + deviceId);
			Log.info("Response From Rest client :: " + str);
			return null;
		} catch (Exception e) {
			Log.info("Fail to Send FCM Push Notification" + e.getLocalizedMessage());
			return null;
		}
	}

//	public void sendJPUSHNotification(String deviceToken, String title, String data, String body) {
//
//		try {
//			String masterSecret = JiveGlobals.getProperty("push.jpush.mastersecret");
//			String appKey = JiveGlobals.getProperty("push.jpush.appkey");
//			PushPayload pushPayload = buildPushPayload(deviceToken, title, data, body);
//			JPushClient jpushClient = new JPushClient(masterSecret, appKey);
//			PushResult pushResult = jpushClient.sendPush(pushPayload);
//		} catch (Exception e) {
//			Log.info("Fail to send JPush Push Notification" + e.getLocalizedMessage());
//			e.printStackTrace();
//		}
//	}
//
//	private PushPayload buildPushPayload(String deviceId, String title, String data, String body) {
//
//		Builder messageBuilder = new Message.Builder();
//		messageBuilder.setTitle(title);
//		messageBuilder.setContentType("application/json");
//		messageBuilder.setMsgContent(data);
//		messageBuilder.addCustom("body", body);
//
//		return PushPayload.newBuilder().setPlatform(Platform.all()).setNotification(Notification.alert(data))
//				.setAudience(Audience.registrationId(deviceId)).setMessage(messageBuilder.build()).build();
//	}

	public void SendAPNVoipNotification(String deviceToken, ApnsPayloadBuilder payloadBuilder)
			throws InterruptedException, ExecutionException {

		try {
			String topic = "com.gatherhall.app.voip";
			PushNotificationFuture<SimpleApnsPushNotification, PushNotificationResponse<SimpleApnsPushNotification>> sendNotificationFuture = null;
			// Staging environment
			if ("openfire.gatherhall.com".equals(JiveGlobals.getProperty("xmpp.domain"))) {
				final ApnsClient apnsClient = new ApnsClientBuilder()
						.setApnsServer(ApnsClientBuilder.DEVELOPMENT_APNS_HOST)
						.setClientCredentials(new File("/usr/share/openfire/conf/Gatherhall_VOIP.p12"),
								JiveGlobals.getProperty("push.apns.voip.pass") == null ? ""
										: JiveGlobals.getProperty("push.apns.voip.pass"))
						.build();
				DeliveryPriority priority = DeliveryPriority.IMMEDIATE;
				PushType pushType = PushType.VOIP;
				String collapseId = UUID.randomUUID().toString();
				UUID apnsId = UUID.randomUUID();
				SimpleApnsPushNotification voipNotification = new SimpleApnsPushNotification(deviceToken, topic,
						payloadBuilder.build(), null, priority, pushType, collapseId, apnsId);
				sendNotificationFuture = apnsClient.sendNotification(voipNotification);
			}
			// Production environment
			if ("chat.gatherhall.com".equals(JiveGlobals.getProperty("xmpp.domain"))) {
				final ApnsClient apnsClient = new ApnsClientBuilder()
						.setApnsServer(ApnsClientBuilder.PRODUCTION_APNS_HOST)
						.setClientCredentials(new File("/usr/share/openfire/conf/Gatherhall_VOIP.p12"),
								JiveGlobals.getProperty("push.apns.voip.pass") == null ? ""
										: JiveGlobals.getProperty("push.apns.voip.pass"))
						.build();
				DeliveryPriority priority = DeliveryPriority.IMMEDIATE;
				PushType pushType = PushType.VOIP;
				String collapseId = UUID.randomUUID().toString();
				UUID apnsId = UUID.randomUUID();
				SimpleApnsPushNotification voipNotification = new SimpleApnsPushNotification(deviceToken, topic,
						payloadBuilder.build(), null, priority, pushType, collapseId, apnsId);
				sendNotificationFuture = apnsClient.sendNotification(voipNotification);
			}
			Log.info("APNS VOIP PAYLOD :: " + payloadBuilder.build());
			Log.info("APNS Voip Notification send successfuly :: " + sendNotificationFuture.get().getRejectionReason());
		} catch (Exception ex) {
			Log.info("Fail to send APNS Voip Notification :: " + ex.getLocalizedMessage());
		}
	}

}
