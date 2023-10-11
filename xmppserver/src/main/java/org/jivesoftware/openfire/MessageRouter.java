/*
 * Copyright (C) 2005-2008 Jive Software. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jivesoftware.openfire;

import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.Timer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.FilenameUtils;
import org.dom4j.Element;
import org.dom4j.QName;
import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.openfire.carbons.Sent;
import org.jivesoftware.openfire.container.BasicModule;
import org.jivesoftware.openfire.custom.dto.Call;
import org.jivesoftware.openfire.custom.dto.Contact;
import org.jivesoftware.openfire.custom.dto.CreateCallConference;
import org.jivesoftware.openfire.custom.dto.CustomPollOptionControler;
import org.jivesoftware.openfire.custom.dto.DeviceType;
import org.jivesoftware.openfire.custom.dto.MediaData;
import org.jivesoftware.openfire.custom.dto.MessageMedia;
import org.jivesoftware.openfire.custom.dto.MessageTime;
import org.jivesoftware.openfire.custom.dto.Moment;
import org.jivesoftware.openfire.custom.dto.OpinionPoll;
import org.jivesoftware.openfire.custom.dto.OpinionPollUpdate;
import org.jivesoftware.openfire.custom.dto.OpinionPollUpdatePayload;
import org.jivesoftware.openfire.custom.dto.OptionPayload;
import org.jivesoftware.openfire.custom.dto.PollDelete;
import org.jivesoftware.openfire.custom.dto.PollExpire;
import org.jivesoftware.openfire.custom.dto.PushNotificationPayloadDto;
import org.jivesoftware.openfire.custom.dto.RSS;
import org.jivesoftware.openfire.custom.dto.Relay;
import org.jivesoftware.openfire.custom.dto.UpdateCallConference;
import org.jivesoftware.openfire.custom.dto.UserDeviceEntity;
import org.jivesoftware.openfire.forward.Forwarded;
import org.jivesoftware.openfire.interceptor.InterceptorManager;
import org.jivesoftware.openfire.interceptor.PacketRejectedException;
import org.jivesoftware.openfire.muc.spi.OpinionPollExpireTimerTask;
import org.jivesoftware.openfire.pushnotifcation.PushNotificationController;
import org.jivesoftware.openfire.session.ClientSession;
import org.jivesoftware.openfire.session.LocalClientSession;
import org.jivesoftware.openfire.session.Session;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.openfire.user.UserManager;
import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;
import org.xmpp.packet.PacketError;
import org.xmpp.packet.Presence;

import com.google.gson.Gson;

/**
 * <p>
 * Route message packets throughout the server.
 * </p>
 * <p>
 * Routing is based on the recipient and sender addresses. The typical packet
 * will often be routed twice, once from the sender to some internal server
 * component for handling or processing, and then back to the router to be
 * delivered to it's final destination.
 * </p>
 *
 * @author Iain Shigeoka
 */
public class MessageRouter extends BasicModule {

	private static Logger log = LoggerFactory.getLogger(MessageRouter.class);

	private OfflineMessageStrategy messageStrategy;
	private RoutingTable routingTable;
	private SessionManager sessionManager;
	private MulticastRouter multicastRouter;
	private UserManager userManager;

	private String serverName;

	private static final SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
	private static final StringBuffer SQL_GET_SPECIFIC_OPINION_POLL_WITH_USER_DETAILS = new StringBuffer(
			"select pm.ofroomid, po.optionname, COALESCE(NULLIF(array_agg(pur.username), '{NULL}'), '{}') as userids, count(pur.username) as noofusers ")
			.append(" from ofpollmaster pm inner join ofpolloptions po on po.pollid = pm.pollid ")
			.append(" left join ofpolluserresponse pur ON pur.polloptionid = po.polloptionid where po.pollid = ? GROUP BY pm.ofroomid, po.optionname ");

	/**
	 * Constructs a message router.
	 */
	public MessageRouter() {
		super("XMPP Message Router");
	}

	/**
	 * <p>
	 * Performs the actual packet routing.
	 * </p>
	 * <p>
	 * You routing is considered 'quick' and implementations may not take excessive
	 * amounts of time to complete the routing. If routing will take a long amount
	 * of time, the actual routing should be done in another thread so this method
	 * returns quickly.
	 * </p>
	 * <h2>Warning</h2>
	 * <p>
	 * Be careful to enforce concurrency DbC of concurrent by synchronizing any
	 * accesses to class resources.
	 * </p>
	 *
	 * @param packet The packet to route
	 * @throws NullPointerException If the packet is null
	 */
	public void route(Message packet) {
		if (packet == null) {
			throw new NullPointerException();
		}
		ClientSession session = sessionManager.getSession(packet.getFrom());
        final PresenceManager presenceManager = XMPPServer.getInstance().getPresenceManager();

		try {
			// Invoke the interceptors before we process the read packet
			InterceptorManager.getInstance().invokeInterceptors(packet, session, true, false);
			if (session == null || session.getStatus() == Session.STATUS_AUTHENTICATED) {
				JID recipientJID = packet.getTo();

				// If the server receives a message stanza with no 'to' attribute, it MUST treat
				// the message as if the 'to' address were the bare JID <localpart@domainpart>
				// of the sending entity.
				if (recipientJID == null) {
					recipientJID = packet.getFrom().asBareJID();
				}

				// Check if the message was sent to the server hostname
				if (recipientJID.getNode() == null && recipientJID.getResource() == null
						&& serverName.equals(recipientJID.getDomain())) {
					if (packet.getElement().element("addresses") != null) {
						// Message includes multicast processing instructions. Ask the multicastRouter
						// to route this packet
						multicastRouter.route(packet);
					} else {
						// Message was sent to the server hostname so forward it to a configurable
						// set of JID's (probably admin users)
						sendMessageToAdmins(packet);
					}
					return;
				}

				boolean isAcceptable = true;
				if (session instanceof LocalClientSession) {
					// Check if we could process messages from the recipient.
					// If not, return a not-acceptable error as per XEP-0016:
					// If the user attempts to send an outbound stanza to a contact and that stanza
					// type is blocked, the user's server MUST NOT route the stanza to the contact
					// but instead MUST return a <not-acceptable/> error
					Message dummyMessage = packet.createCopy();
					dummyMessage.setFrom(packet.getTo());
					dummyMessage.setTo(packet.getFrom());
					if (!((LocalClientSession) session).canProcess(dummyMessage)) {
						packet.setTo(session.getAddress());
						packet.setFrom((JID) null);
						packet.setError(PacketError.Condition.not_acceptable);
						session.process(packet);
						isAcceptable = false;
					}
				}
				
				log.info("************ packet is : " + packet.toString());

				if (isAcceptable) {
					boolean isPrivate = packet.getElement().element(QName.get("private", "urn:xmpp:carbons:2")) != null;
					try {
						// Deliver stanza to requested route
						
						// Here implement a push notification logic for android device for call 
						Message message = (Message) packet;
						log.info("************ Message is : " + message.toString());
						String messageSubject = message.getSubject();

						// We need to send push notification if the subject type is "call"
						if (null != message && (null != messageSubject && messageSubject.equals("15") && !message.getType().toString().equalsIgnoreCase("groupchat"))) {
							log.info("Dhaval Message Router 143 Android 12 case:: " + packet.toString());
							PushNotificationController controller = new PushNotificationController();
							UserDeviceEntity userDeviceDetail = controller
									.getUserDeviceDetailFromJID(message.getTo().toString().replace("/chat", ""));

							if (null != userDeviceDetail && userDeviceDetail.isActive()
									&& userDeviceDetail.getDeviceType().value().equals("ANDROID")) {
								String deviceType = userDeviceDetail.getDeviceType().value();
								String fromName = controller.getNameFromUserName(message.getFrom().toString());
								Gson gson = new Gson();
								log.info("User Device Details :: " + gson.toJson(userDeviceDetail));
								if (deviceType.equalsIgnoreCase("ANDROID")) {
									// Create push notification only if the device is android
									JAXBContext jaxbContext = null;
									Unmarshaller jaxbUnMarshller = null;
									MessageTime msgTime = null;

									XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
									xmlInputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);
									Element messageTimeElement = message.getChildElement("messageTime",
											"urn:xmpp:time");
									XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(
											new StreamSource(new StringReader(messageTimeElement.asXML())));

									jaxbContext = JAXBContext.newInstance(MessageTime.class);
									jaxbUnMarshller = jaxbContext.createUnmarshaller();
									msgTime = (MessageTime) jaxbUnMarshller.unmarshal(xmlStreamReader);

									Element callElement = message.getChildElement("call", "urn:xmpp:call");
									xmlStreamReader = xmlInputFactory.createXMLStreamReader(
											new StreamSource(new StringReader(callElement.asXML())));
									jaxbContext = JAXBContext.newInstance(Call.class);
									jaxbUnMarshller = jaxbContext.createUnmarshaller();
									Call callData = (Call) jaxbUnMarshller.unmarshal(xmlStreamReader);

									// Logic to create push notification
									PushNotificationPayloadDto notiDto = new PushNotificationPayloadDto();
									notiDto.setMessageType("Call");
									notiDto.setMessageId(message.getID());
									notiDto.setFromJID(message.getFrom().toBareJID());
									notiDto.setToJID(message.getTo().toBareJID());
									notiDto.setFromName(fromName);
									notiDto.setGroupName(null);
									notiDto.setMessageTime(msgTime.getTime());
									notiDto.setSubject(messageSubject);
									notiDto.setPayload(callData);
									notiDto.setBody(callData.getType() + " Call");
									notiDto.setAndroidBody(callData.getType() + " Call");
									
									/*
									 * Now user will get push notification for any callData.getReason
									 * for initiate, hangup, rejected, accepted 
									 * */
									controller.publishNotification(userDeviceDetail, notiDto);

								} else {
									routingTable.routePacket(recipientJID, packet, false);
								}
							} else {
								routingTable.routePacket(recipientJID, packet, false);
							}
						} // send the push notification if the receive is IOS and type is Chat
						else if (null != message && (null != messageSubject) && !message.getType().toString().equalsIgnoreCase("groupchat")  && (message.getSubject().equalsIgnoreCase("1")
								|| message.getSubject().equalsIgnoreCase("2") || message.getSubject().equalsIgnoreCase("3")
								|| message.getSubject().equalsIgnoreCase("6") || message.getSubject().equalsIgnoreCase("7")
								|| message.getSubject().equalsIgnoreCase("8") || message.getSubject().equalsIgnoreCase("9")
								|| message.getSubject().equalsIgnoreCase("10")
								|| message.getSubject().equalsIgnoreCase("11")
								|| message.getSubject().equalsIgnoreCase("14")
								|| message.getSubject().equalsIgnoreCase("51") 
								|| message.getSubject().equalsIgnoreCase("16")
								|| message.getSubject().equalsIgnoreCase("18")
								|| message.getSubject().equalsIgnoreCase("19")
								|| message.getSubject().equalsIgnoreCase("21")
								|| message.getSubject().equalsIgnoreCase("22")
								|| message.getSubject().equalsIgnoreCase("23")
								|| message.getSubject().equalsIgnoreCase("24"))) {
							log.info("Dhaval Message Router 143 IOS Online user Receive Notification case:: "
									+ packet.toString());
							PushNotificationController controller = new PushNotificationController();
							UserDeviceEntity userDeviceDetail = controller
									.getUserDeviceDetailFromJID(message.getTo().toString().replace("/chat", ""));
							
							// check user is online or not if online then only we can send the Notification
							User user = userManager.getUser(message.getTo());
							boolean isAvailable = presenceManager.isAvailable(user);							
							if (userDeviceDetail != null && userDeviceDetail.isActive()
									&& userDeviceDetail.getDeviceType().value().equalsIgnoreCase("IOS") && isAvailable) {

								String deviceType = userDeviceDetail.getDeviceType().value();
								String fromName = controller.getNameFromUserName(message.getFrom().toString());
								Gson gson = new Gson();
								//log.info("User Device Details :: " + gson.toJson(userDeviceDetail));
								if (deviceType.equalsIgnoreCase("IOS")) {
									// Create push notification only if the device is IOS
									JAXBContext jaxbContext = null;
									Unmarshaller jaxbUnMarshller = null;

									MessageTime msgTime = null;
									Element messageTimeElement = message.getChildElement("messageTime",
											"urn:xmpp:time");
									XMLInputFactory xif = XMLInputFactory.newFactory();
									xif.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);
									String subject = message.getSubject();

									switch (subject) {
										case "1":
										case "51": // Text Message
											try {
												if (messageTimeElement != null) {
													StreamSource messageTimesource = new StreamSource(
															new StringReader(messageTimeElement.asXML()));
													XMLStreamReader xsr = xif.createXMLStreamReader(messageTimesource);
													jaxbContext = JAXBContext.newInstance(MessageTime.class);
													jaxbUnMarshller = jaxbContext.createUnmarshaller();
													msgTime = (MessageTime) jaxbUnMarshller.unmarshal(xsr);
												} else {
													msgTime = null;
												}
												log.info("Received Message Body :: " + message.getBody());
												PushNotificationPayloadDto notiDto = new PushNotificationPayloadDto();
												notiDto.setMessageType("Chat");
												notiDto.setMessageId(message.getID());
												notiDto.setFromJID(message.getFrom().toBareJID());
												notiDto.setToJID(message.getTo().toBareJID());
												notiDto.setFromName(fromName);
												notiDto.setGroupName(null);
												if (msgTime == null)
													notiDto.setMessageTime(0L);
												else
													notiDto.setMessageTime(msgTime.getTime());
												notiDto.setSubject(message.getSubject());
												notiDto.setPayload(message.getBody());
												notiDto.setBody(message.getBody());
												notiDto.setAndroidBody(message.getBody());
												log.info("After Setting into Notification Dto Payload is :: "
														+ notiDto.getPayload());
												log.info("After Setting into Notification Dto Body is :: "
														+ notiDto.getBody());
												controller.publishNotification(userDeviceDetail, notiDto);
											} catch (JAXBException ex) {
												ex.printStackTrace();
												log.error("Fail to parse TextMessage subject 1" + ex.getLocalizedMessage());
											} catch (XMLStreamException e) {
												log.error("Fail to parse TextMessage subject 1" + e.getLocalizedMessage());
												e.printStackTrace();
											}
											break;

										case "2": // Audio Message

											try {
												XMLStreamReader xsr = xif.createXMLStreamReader(
														new StreamSource(new StringReader(messageTimeElement.asXML())));

												jaxbContext = JAXBContext.newInstance(MessageTime.class);
												jaxbUnMarshller = jaxbContext.createUnmarshaller();
												msgTime = (MessageTime) jaxbUnMarshller.unmarshal(xsr);

												Element audioElement = message.getChildElement("mediaData",
														"urn:xmpp:media");

												xsr = xif.createXMLStreamReader(
														new StreamSource(new StringReader(audioElement.asXML())));
												jaxbContext = JAXBContext.newInstance(MediaData.class);
												jaxbUnMarshller = jaxbContext.createUnmarshaller();
												MediaData audioMessageMedia = (MediaData) jaxbUnMarshller.unmarshal(xsr);

												PushNotificationPayloadDto notiDto = new PushNotificationPayloadDto();
												notiDto.setMessageType("Chat");
												notiDto.setMessageId(message.getID());
												notiDto.setFromJID(message.getFrom().toBareJID());
												notiDto.setToJID(message.getTo().toBareJID());
												notiDto.setFromName(fromName);
												notiDto.setGroupName(null);
												notiDto.setMessageTime(msgTime.getTime());
												notiDto.setSubject(message.getSubject());
												notiDto.setPayload(audioMessageMedia);

												if (DeviceType.ANDROID.equals(userDeviceDetail.getDeviceType())) {
													if (audioMessageMedia.getMessageMedia().size() > 1) {
														notiDto.setBody("\\uD83C\\uDFB5 "
																+ audioMessageMedia.getMessageMedia().size() + " Audios");
														notiDto.setAndroidBody("\\uD83C\\uDFB5 "
																+ audioMessageMedia.getMessageMedia().size() + " Audios");
													} else {
														notiDto.setBody("\\uD83C\\uDFB5 "
																+ audioMessageMedia.getMessageMedia().size() + " Audio");
														notiDto.setAndroidBody("\\uD83C\\uDFB5 "
																+ audioMessageMedia.getMessageMedia().size() + " Audio");
													}
												} else if (DeviceType.IOS.equals(userDeviceDetail.getDeviceType())) {
													if (audioMessageMedia.getMessageMedia().size() > 1) {
														notiDto.setBody("🎵 " + audioMessageMedia.getMessageMedia().size()
																+ " Audios");
													} else {
														notiDto.setBody("🎵 " + audioMessageMedia.getMessageMedia().size()
																+ " Audio");
													}
												}

												controller.publishNotification(userDeviceDetail, notiDto);
											} catch (JAXBException ex) {
												log.error("Fail to parse subject 2");
											} catch (XMLStreamException e) {
												log.error("Fail to parse subject 2" + e.getLocalizedMessage());
												e.printStackTrace();
											}
											break;
										case "3": // Image or Video Element
											try {
												XMLStreamReader xsr = xif.createXMLStreamReader(
														new StreamSource(new StringReader(messageTimeElement.asXML())));

												jaxbContext = JAXBContext.newInstance(MessageTime.class);
												jaxbUnMarshller = jaxbContext.createUnmarshaller();
												msgTime = (MessageTime) jaxbUnMarshller.unmarshal(xsr);
												Element audioElement = message.getChildElement("mediaData",
														"urn:xmpp:media");
												xsr = xif.createXMLStreamReader(
														new StreamSource(new StringReader(audioElement.asXML())));
												jaxbContext = JAXBContext.newInstance(MediaData.class);
												jaxbUnMarshller = jaxbContext.createUnmarshaller();
												MediaData audioMessageMedia = (MediaData) jaxbUnMarshller.unmarshal(xsr);
												PushNotificationPayloadDto notiDto = new PushNotificationPayloadDto();
												notiDto.setMessageType("Chat");
												notiDto.setMessageId(message.getID());
												notiDto.setFromJID(message.getFrom().toBareJID());
												notiDto.setToJID(message.getTo().toBareJID());
												notiDto.setFromName(fromName);
												notiDto.setGroupName(null);
												notiDto.setMessageTime(msgTime.getTime());
												notiDto.setSubject(message.getSubject());
												notiDto.setPayload(audioMessageMedia);
												int imageCount = 0;
												int videoCount = 0;
												int gifCount = 0;
												for (MessageMedia media : audioMessageMedia.getMessageMedia()) {
													if (media.getFileType().equalsIgnoreCase("video")) {
														videoCount++;
													} else if (FilenameUtils.getExtension(media.getFileName())
															.equalsIgnoreCase("gif")) {
														log.info("File type GIF : " + media);
														gifCount++;
													} else if (media.getFileType().equalsIgnoreCase("image")) {
														imageCount++;
													}
												}

												String body = null;
												if (DeviceType.ANDROID.equals(userDeviceDetail.getDeviceType())) {

													// For Image types of file
													if (imageCount > 0) {
														if (imageCount > 1)
															body = "\\uD83D\\uDCF7 " + imageCount + " Photos";
														else
															body = "\\uD83D\\uDCF7 " + imageCount + " Photo";
													}

													// for GIF types of file in Android device
													if (gifCount > 0) {
														// This logic may usefull when user select multiple types of file
														if (body != null) {
															if (gifCount > 1) {
																/*
																* This --> //ud83d//udc7e <-- is the java eclipse encodding
																* for U+1F47E ()ALIEN MONSTER visit :
																* https://charbase.com/1f47e-unicode-alien-monster
																* https://unicode-table.com/en/1F47E/
																*/
																body = body + ", \\ud83d\\udc7e " + gifCount + " GIFs";
															} else {
																body = body + ", \\ud83d\\udc7e " + gifCount + " GIF";
															}
														} else {
															if (gifCount > 1) {
																body = "\\ud83d\\udc7e " + gifCount + " GIFs";
															} else {
																body = "\\ud83d\\udc7e " + gifCount + " GIF";
															}
														}
													}

													// For Image types of file
													if (videoCount > 0) {
														if (body != null) {
															if (videoCount > 1) {
																body = body + ", \\uD83C\\uDFA5 " + videoCount + " Videos";
															} else {
																body = body + ", \\uD83C\\uDFA5 " + videoCount + " Video";
															}
														} else {
															if (videoCount > 1) {
																body = "\\uD83C\\uDFA5 " + videoCount + " Videos";
															} else {
																body = "\\uD83C\\uDFA5 " + videoCount + " Video";
															}
														}
													}
												} else if (DeviceType.IOS.equals(userDeviceDetail.getDeviceType())) {
													if (imageCount > 0) {
														if (imageCount > 1)
															body = "📷 " + imageCount + " Photos";
														else
															body = "📷 " + imageCount + " Photo";
													}

													// for GIF types of file in Android device
													if (gifCount > 0) {

														if (body != null) {
															if (gifCount > 1) {
																/*
																* Visit : https://unicode-table.com/en/1F47E/
																*/
																body = body + ", 👾 " + gifCount + " GIFs";
															} else {
																body = body + ", 👾 " + gifCount + " GIF";
															}
														} else {
															if (gifCount > 1) {
																body = "👾 " + gifCount + " GIFs";
															} else {
																body = "👾 " + gifCount + " GIF";
															}
														}
													}

													if (videoCount > 0) {
														if (body != null) {
															if (videoCount > 1) {
																body = body + ", 📹 " + videoCount + " Videos";
															} else {
																body = body + ", 📹 " + videoCount + " Video";
															}
														} else {
															if (videoCount > 1) {
																body = "📹 " + videoCount + " Videos";
															} else {
																body = "📹 " + videoCount + " Video";
															}
														}
													}
												}

												notiDto.setBody(body);
												notiDto.setAndroidBody(body);
												controller.publishNotification(userDeviceDetail, notiDto);

											} catch (JAXBException ex) {
												log.error("Fail to parse subject 3");
											} catch (XMLStreamException e) {
												log.error("Fail to parse subject 3" + e.getLocalizedMessage());
												e.printStackTrace();
											} catch (Exception ex) {
												log.info("GSON EXCEPTION :: " + ex.getLocalizedMessage());
											}
											break;
										case "4":

											break;
										case "5":

											break;
										case "6": // Document
											try {
												XMLStreamReader xsr = xif.createXMLStreamReader(
														new StreamSource(new StringReader(messageTimeElement.asXML())));

												jaxbContext = JAXBContext.newInstance(MessageTime.class);
												jaxbUnMarshller = jaxbContext.createUnmarshaller();
												msgTime = (MessageTime) jaxbUnMarshller.unmarshal(xsr);

												Element audioElement = message.getChildElement("mediaData",
														"urn:xmpp:media");

												xsr = xif.createXMLStreamReader(
														new StreamSource(new StringReader(audioElement.asXML())));
												jaxbContext = JAXBContext.newInstance(MediaData.class);
												jaxbUnMarshller = jaxbContext.createUnmarshaller();
												MediaData documentMessageMedia = (MediaData) jaxbUnMarshller.unmarshal(xsr);

												PushNotificationPayloadDto notiDto = new PushNotificationPayloadDto();
												notiDto.setMessageType("Chat");
												notiDto.setMessageId(message.getID());
												notiDto.setFromJID(message.getFrom().toBareJID());
												notiDto.setToJID(message.getTo().toBareJID());
												notiDto.setFromName(fromName);
												notiDto.setGroupName(null);
												notiDto.setMessageTime(msgTime.getTime());
												notiDto.setSubject(message.getSubject());
												notiDto.setPayload(documentMessageMedia);

												if (DeviceType.ANDROID.equals(userDeviceDetail.getDeviceType())) {
													if (documentMessageMedia.getMessageMedia().size() > 1) {
														notiDto.setBody("\\uD83D\\uDCC4 "
																+ documentMessageMedia.getMessageMedia().size() + " Files");
														notiDto.setAndroidBody("\\uD83D\\uDCC4 "
																+ documentMessageMedia.getMessageMedia().size() + " Files");
													} else {
														notiDto.setBody("\\uD83D\\uDCC4 "
																+ documentMessageMedia.getMessageMedia().size() + " File");
														notiDto.setAndroidBody("\\uD83D\\uDCC4 "
																+ documentMessageMedia.getMessageMedia().size() + " File");
													}
												} else if (DeviceType.IOS.equals(userDeviceDetail.getDeviceType())) {
													if (documentMessageMedia.getMessageMedia().size() > 1) {
														notiDto.setBody("📄 "
																+ documentMessageMedia.getMessageMedia().size() + " Files");
													} else {
														notiDto.setBody("📄 "
																+ documentMessageMedia.getMessageMedia().size() + " File");
													}
												}

												controller.publishNotification(userDeviceDetail, notiDto);
											} catch (JAXBException ex) {
												log.error("Fail to parse messageMedia subject 6");
											} catch (XMLStreamException e) {
												// TODO Auto-generated catch block
												log.error("Fail to parse subject 6" + e.getLocalizedMessage());
												e.printStackTrace();
											}
											break;
										case "7": // location
											try {

												XMLStreamReader xsr = xif.createXMLStreamReader(
														new StreamSource(new StringReader(messageTimeElement.asXML())));

												jaxbContext = JAXBContext.newInstance(MessageTime.class);
												jaxbUnMarshller = jaxbContext.createUnmarshaller();
												msgTime = (MessageTime) jaxbUnMarshller.unmarshal(xsr);

												Element locationElement = message.getChildElement("location",
														"urn:xmpp:location");

												xsr = xif.createXMLStreamReader(
														new StreamSource(new StringReader(locationElement.asXML())));
												jaxbContext = JAXBContext.newInstance(Location.class);
												jaxbUnMarshller = jaxbContext.createUnmarshaller();
												Location locationData = (Location) jaxbUnMarshller.unmarshal(xsr);

												PushNotificationPayloadDto notiDto = new PushNotificationPayloadDto();
												notiDto.setMessageType("Chat");
												notiDto.setMessageId(message.getID());
												notiDto.setFromJID(message.getFrom().toBareJID());
												notiDto.setToJID(message.getTo().toBareJID());
												notiDto.setFromName(fromName);
												notiDto.setGroupName(null);
												notiDto.setMessageTime(msgTime.getTime());
												notiDto.setSubject(message.getSubject());
												notiDto.setPayload(locationData);

												if (DeviceType.ANDROID.equals(userDeviceDetail.getDeviceType())) {
													notiDto.setBody("\\uD83D\\uDCCD Location");
													notiDto.setAndroidBody("\\uD83D\\uDCCD Location");
												} else if (DeviceType.IOS.equals(userDeviceDetail.getDeviceType())) {
													notiDto.setBody("📍 Location");
												}

												controller.publishNotification(userDeviceDetail, notiDto);
											} catch (JAXBException ex) {
												log.error("Fail to parse messageMedia subject 7");
											} catch (XMLStreamException e) {
												log.error("Fail to parse subject 7" + e.getLocalizedMessage());
												e.printStackTrace();
											}
											break;
										case "8": // contact
											try {
												XMLStreamReader xsr = xif.createXMLStreamReader(
														new StreamSource(new StringReader(messageTimeElement.asXML())));

												jaxbContext = JAXBContext.newInstance(MessageTime.class);
												jaxbUnMarshller = jaxbContext.createUnmarshaller();
												msgTime = (MessageTime) jaxbUnMarshller.unmarshal(xsr);

												Element locationElement = message.getChildElement("contact",
														"urn:xmpp:contact");

												xsr = xif.createXMLStreamReader(
														new StreamSource(new StringReader(locationElement.asXML())));
												jaxbContext = JAXBContext.newInstance(Contact.class);
												jaxbUnMarshller = jaxbContext.createUnmarshaller();
												Contact contactData = (Contact) jaxbUnMarshller.unmarshal(xsr);

												PushNotificationPayloadDto notiDto = new PushNotificationPayloadDto();
												notiDto.setMessageType("Chat");
												notiDto.setMessageId(message.getID());
												notiDto.setFromJID(message.getFrom().toBareJID());
												notiDto.setToJID(message.getTo().toBareJID());
												notiDto.setFromName(fromName);
												notiDto.setGroupName(null);
												notiDto.setMessageTime(msgTime.getTime());
												notiDto.setSubject(message.getSubject());
												notiDto.setPayload(contactData);

												if (DeviceType.ANDROID.equals(userDeviceDetail.getDeviceType())) {
													notiDto.setBody("\\uD83D\\uDC64 Contact");
													notiDto.setAndroidBody("\\uD83D\\uDC64 Contact");
												} else if (DeviceType.IOS.equals(userDeviceDetail.getDeviceType())) {
													notiDto.setBody("👤 Contact");
												}
												controller.publishNotification(userDeviceDetail, notiDto);

											} catch (JAXBException ex) {
												log.error("Fail to parse subject 8");
											} catch (XMLStreamException e) {
												log.error("Fail to parse subject 8" + e.getLocalizedMessage());
												e.printStackTrace();
											}
											break;
										case "9": // RSS
											try {

												XMLStreamReader xsr = xif.createXMLStreamReader(
														new StreamSource(new StringReader(messageTimeElement.asXML())));

												jaxbContext = JAXBContext.newInstance(MessageTime.class);
												jaxbUnMarshller = jaxbContext.createUnmarshaller();
												msgTime = (MessageTime) jaxbUnMarshller.unmarshal(xsr);

												Element locationElement = message.getChildElement("rss", "urn:xmpp:rss");

												xsr = xif.createXMLStreamReader(
														new StreamSource(new StringReader(locationElement.asXML())));
												jaxbContext = JAXBContext.newInstance(RSS.class);
												jaxbUnMarshller = jaxbContext.createUnmarshaller();
												RSS rssData = (RSS) jaxbUnMarshller.unmarshal(xsr);

												PushNotificationPayloadDto notiDto = new PushNotificationPayloadDto();
												notiDto.setMessageType("Chat");
												notiDto.setMessageId(message.getID());
												notiDto.setFromJID(message.getFrom().toBareJID());
												notiDto.setToJID(message.getTo().toBareJID());
												notiDto.setFromName(fromName);
												notiDto.setGroupName(null);
												notiDto.setMessageTime(msgTime.getTime());
												notiDto.setSubject(message.getSubject());
												notiDto.setPayload(rssData);
												notiDto.setBody(message.getBody());
												notiDto.setAndroidBody(message.getBody());
												notiDto.setTitle(rssData.getRssTitle());

												controller.publishNotification(userDeviceDetail, notiDto);

											} catch (JAXBException ex) {
												log.error("Fail to parse subject 9");
											} catch (XMLStreamException e) {
												// TODO Auto-generated catch block
												log.error("Fail to parse subject 9" + e.getLocalizedMessage());
												e.printStackTrace();
											}

											break;
										case "10": // Relay
											try {
												XMLStreamReader xsr = xif.createXMLStreamReader(
														new StreamSource(new StringReader(messageTimeElement.asXML())));

												jaxbContext = JAXBContext.newInstance(MessageTime.class);
												jaxbUnMarshller = jaxbContext.createUnmarshaller();
												msgTime = (MessageTime) jaxbUnMarshller.unmarshal(xsr);

												Element relayElement = message.getChildElement("relay", "urn:xmpp:relay");

												xsr = xif.createXMLStreamReader(
														new StreamSource(new StringReader(relayElement.asXML())));
												jaxbContext = JAXBContext.newInstance(Relay.class);
												jaxbUnMarshller = jaxbContext.createUnmarshaller();
												Relay relayData = (Relay) jaxbUnMarshller.unmarshal(xsr);

												PushNotificationPayloadDto notiDto = new PushNotificationPayloadDto();
												notiDto.setMessageType("Chat");
												notiDto.setMessageId(message.getID());
												notiDto.setFromJID(message.getFrom().toBareJID());
												notiDto.setToJID(message.getTo().toBareJID());
												notiDto.setFromName(fromName);
												notiDto.setGroupName(null);
												notiDto.setMessageTime(msgTime.getTime());
												notiDto.setSubject(message.getSubject());
												notiDto.setPayload(relayData);
												notiDto.setBody(message.getBody());
												notiDto.setAndroidBody(message.getBody());
												notiDto.setTitle(relayData.getRelayTitle());

												log.info("Parsed Relay Object :: " + new Gson().toJson(relayData));
												controller.publishNotification(userDeviceDetail, notiDto);

											} catch (JAXBException ex) {
												log.error("Fail to parse subject 10");
											} catch (XMLStreamException e) {
												log.error("Fail to parse subject 10" + e.getLocalizedMessage());
												e.printStackTrace();
											}
											break;
										case "11": // Create Poll
											try {

												XMLStreamReader xsr = xif.createXMLStreamReader(
														new StreamSource(new StringReader(messageTimeElement.asXML())));

												jaxbContext = JAXBContext.newInstance(MessageTime.class);
												jaxbUnMarshller = jaxbContext.createUnmarshaller();
												msgTime = (MessageTime) jaxbUnMarshller.unmarshal(xsr);

												Element oppinionPoll = message.getChildElement("opinionPoll",
														"urn:xmpp:createpoll");
												xsr = xif.createXMLStreamReader(
														new StreamSource(new StringReader(oppinionPoll.asXML())));
												jaxbContext = JAXBContext.newInstance(OpinionPoll.class);
												jaxbUnMarshller = jaxbContext.createUnmarshaller();
												OpinionPoll oppinionPollObj = (OpinionPoll) jaxbUnMarshller.unmarshal(xsr);

												if (oppinionPollObj == null)
													log.info("Null Create Poll for one to one Chat");
												else
													log.info("Inside Create Poll for one to one Chat");

												String fromJID = message.getFrom().toBareJID();
												String toJID = message.getTo().toBareJID();

												CustomPollOptionControler custompoll = new CustomPollOptionControler();
												custompoll.addCustomOpinonPoll(oppinionPollObj, 0L, message.getID(),
														Message.Type.chat.name(), fromJID, toJID);

												Date expiredAt = new Date(oppinionPollObj.getExpireDate());
												log.info("Date :: " + expiredAt);

												formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
												String dateString = formatter.format(expiredAt);
												log.info("Formatted Date String:: " + dateString);

												String timeZone = oppinionPollObj.getTimeZone();
												formatter.setTimeZone(TimeZone.getTimeZone(timeZone));
												log.info("Parsed Date :: " + formatter.parse(dateString));

												log.info("Schedule Date :: " + formatter.parse(dateString)
														+ " based on TimeZone :: " + timeZone);

												Timer t = new Timer();
												OpinionPollExpireTimerTask timeTask = new OpinionPollExpireTimerTask(
														oppinionPollObj.getPollId(), "");
												t.schedule(timeTask, formatter.parse(dateString));

												PushNotificationPayloadDto notiDto = new PushNotificationPayloadDto();
												notiDto.setMessageType("Chat");
												notiDto.setMessageId(message.getID());
												notiDto.setFromJID(fromJID);
												notiDto.setToJID(toJID);
												notiDto.setFromName(fromName);
												notiDto.setGroupName(null);
												notiDto.setMessageTime(msgTime.getTime());
												notiDto.setSubject(message.getSubject());
												notiDto.setPayload(oppinionPollObj);
												notiDto.setBody(message.getBody());
												notiDto.setAndroidBody(message.getBody());

												controller.publishNotification(userDeviceDetail, notiDto);

												log.info(
														"Push Notification send successfully for Create Poll in one to one chat");

											} catch (JAXBException e) {
												log.error("Invalid OpinionPoll Create Request");
												e.printStackTrace();
											} catch (ParseException e) {
												log.error("Parse Exception Subject 11");
												e.printStackTrace();
											} catch (XMLStreamException e) {
												log.error("Fail to parse message Subject 11" + e.getLocalizedMessage());
												e.printStackTrace();
											}
											break;
										case "14": // Update Poll
											try {

												XMLStreamReader xsr = xif.createXMLStreamReader(
														new StreamSource(new StringReader(messageTimeElement.asXML())));

												jaxbContext = JAXBContext.newInstance(MessageTime.class);
												jaxbUnMarshller = jaxbContext.createUnmarshaller();
												msgTime = (MessageTime) jaxbUnMarshller.unmarshal(xsr);

												Element oppinionPollResponse = message.getChildElement("opinionPollUpdate",
														"urn:xmpp:opinionPollUpdate");
												xsr = xif.createXMLStreamReader(
														new StreamSource(new StringReader(oppinionPollResponse.asXML())));
												jaxbContext = JAXBContext.newInstance(OpinionPollUpdate.class);
												jaxbUnMarshller = jaxbContext.createUnmarshaller();

												OpinionPollUpdate opinionPollUpdate = (OpinionPollUpdate) jaxbUnMarshller
														.unmarshal(xsr);

												if (opinionPollUpdate == null)
													log.info("Null Update Poll for one to one Chat");
												else
													log.info("Inside Update Poll for one to one Chat");

												CustomPollOptionControler custompoll = new CustomPollOptionControler();

												if (opinionPollUpdate.getIsSelect().equals("no")) {
													custompoll.deleteOpinionPollResponse(opinionPollUpdate);
													log.info("User Response Deleted from Database.");
												} else {
													custompoll.updateOpinionPollResponse(opinionPollUpdate);
												}

												OpinionPollUpdatePayload opinionPollUpdatePayload = null;
												OptionPayload optionPayload = null;
												Connection conn = null;
												ResultSet rs = null;
												int count = 1;
												PreparedStatement pstmt = null;
												try {
													conn = DbConnectionManager.getConnection();
													pstmt = conn.prepareStatement(
															SQL_GET_SPECIFIC_OPINION_POLL_WITH_USER_DETAILS.toString());
													pstmt.setString(1, opinionPollUpdate.getPollId());
													rs = pstmt.executeQuery();

													ArrayList<Map<String, OptionPayload>> response = new ArrayList<Map<String, OptionPayload>>();

													while (rs.next()) {

														Map<String, OptionPayload> payload = new HashMap<String, OptionPayload>();

														String optionName = rs.getString("optionname");

														optionPayload = new OptionPayload();

														String[] userIds = (String[]) rs.getArray("userids").getArray();
														String noOfUsers = String.valueOf(rs.getInt("noofusers"));

														optionPayload.setCount(noOfUsers);
														optionPayload.setIds(userIds);

														payload.put(optionName, optionPayload);
														response.add(payload);

														if (count == 1) {

															opinionPollUpdatePayload = new OpinionPollUpdatePayload();
															opinionPollUpdatePayload
																	.setPollId(opinionPollUpdate.getPollId());
															opinionPollUpdatePayload
																	.setRoomId(opinionPollUpdate.getRoomId());
															opinionPollUpdatePayload.setMessageId(message.getID());
															opinionPollUpdatePayload
																	.setIsSelect(opinionPollUpdate.getIsSelect());
															opinionPollUpdatePayload
																	.setPollMessageId(opinionPollUpdate.getPollMessageId());

															count++;
														}
													}
													opinionPollUpdatePayload.setResponse(response);
												} catch (SQLException ex) {
													ex.printStackTrace();
												} catch (Exception ex) {
													ex.printStackTrace();
												} finally {
													DbConnectionManager.closeConnection(rs, pstmt, conn);
												}

												PushNotificationPayloadDto notiDto = new PushNotificationPayloadDto();
												notiDto.setMessageType("Chat");
												notiDto.setMessageId(message.getID());
												notiDto.setFromJID(message.getFrom().toBareJID());
												notiDto.setToJID(message.getTo().toBareJID());
												notiDto.setFromName(fromName);
												notiDto.setGroupName(null);
												notiDto.setMessageTime(msgTime.getTime());
												notiDto.setSubject(message.getSubject());
												notiDto.setPayload(opinionPollUpdatePayload);
												notiDto.setBody(message.getBody());
												notiDto.setAndroidBody(message.getBody());

												controller.publishNotification(userDeviceDetail, notiDto);

												log.info(
														"Push Notification send successfully for Update Poll in one to one chat");

											} catch (JAXBException e) {
												log.error("Invalid OpinionPoll Response/Update XML");
												e.printStackTrace();
											} catch (XMLStreamException e) {
												log.error("Fail to parse message Subject 14" + e.getLocalizedMessage());
												e.printStackTrace();
											}
											break;
										case "15": // Call
											try {
												XMLStreamReader xsr = xif.createXMLStreamReader(
														new StreamSource(new StringReader(messageTimeElement.asXML())));

												jaxbContext = JAXBContext.newInstance(MessageTime.class);
												jaxbUnMarshller = jaxbContext.createUnmarshaller();
												msgTime = (MessageTime) jaxbUnMarshller.unmarshal(xsr);

												Element callElement = message.getChildElement("call", "urn:xmpp:call");

												xsr = xif.createXMLStreamReader(
														new StreamSource(new StringReader(callElement.asXML())));
												jaxbContext = JAXBContext.newInstance(Call.class);
												jaxbUnMarshller = jaxbContext.createUnmarshaller();
												Call callData = (Call) jaxbUnMarshller.unmarshal(xsr);

												PushNotificationPayloadDto notiDto = new PushNotificationPayloadDto();
												notiDto.setMessageType("Call");
												notiDto.setMessageId(message.getID());
												notiDto.setFromJID(message.getFrom().toBareJID());
												notiDto.setToJID(message.getTo().toBareJID());
												notiDto.setFromName(fromName);
												notiDto.setGroupName(null);
												notiDto.setMessageTime(msgTime.getTime());
												notiDto.setSubject(message.getSubject());
												notiDto.setPayload(callData);
												notiDto.setBody(callData.getType() + " Call");
												notiDto.setAndroidBody(callData.getType() + " Call");

												/*
												* Now user will get push notification for any callData.getReason for
												* initiate, hangup, rejected, accepted
												*/
												controller.publishNotification(userDeviceDetail, notiDto);

											} catch (JAXBException ex) {
												log.error("Fail to parse messageMedia subject 15");
											} catch (XMLStreamException e) {
												log.error("Fail to parse subject 15" + e.getLocalizedMessage());
											} catch (Exception e) {
												log.error("Fail to parse subject 15" + e.getLocalizedMessage());
											}

											break;
										case "18": // Delete Poll
											try {
												log.info(" Into Opinion Poll Delete............");
												log.info("Delete Poll Message ::" + message.toXML());

												XMLStreamReader xsr = xif.createXMLStreamReader(
														new StreamSource(new StringReader(messageTimeElement.asXML())));

												jaxbContext = JAXBContext.newInstance(MessageTime.class);
												jaxbUnMarshller = jaxbContext.createUnmarshaller();
												msgTime = (MessageTime) jaxbUnMarshller.unmarshal(xsr);

												Element deletePollElement = message.getChildElement("pollDelete",
														"urn:xmpp:deletepoll");
												xsr = xif.createXMLStreamReader(
														new StreamSource(new StringReader(deletePollElement.asXML())));
												jaxbContext = JAXBContext.newInstance(PollDelete.class);
												jaxbUnMarshller = jaxbContext.createUnmarshaller();
												PollDelete pollDelete = (PollDelete) jaxbUnMarshller.unmarshal(xsr);

												if (pollDelete == null)
													log.info("Null Delete Poll for one to one Chat");
												else
													log.info("Inside Delete Poll for one to one Chat");

												PushNotificationPayloadDto notiDto = new PushNotificationPayloadDto();
												notiDto.setMessageType("Chat");
												notiDto.setMessageId(message.getID());
												notiDto.setFromJID(message.getFrom().toBareJID());
												notiDto.setToJID(message.getTo().toBareJID());
												notiDto.setFromName(fromName);
												notiDto.setGroupName(null);
												notiDto.setMessageTime(msgTime.getTime());
												notiDto.setSubject(message.getSubject());
												notiDto.setPayload(pollDelete);
												notiDto.setBody("Poll has been deleted");
												notiDto.setAndroidBody("Poll has been deleted");

												controller.publishNotification(userDeviceDetail, notiDto);

											} catch (JAXBException e) {
												log.error("Invalid OpinionPoll Delete poll XML");
												e.printStackTrace();
											} catch (XMLStreamException e) {
												log.error("Fail to parse message Subject 18" + e.getLocalizedMessage());
												e.printStackTrace();
											}
											break;
										case "19": // Poll Expire
											try {
												XMLStreamReader xsr = xif.createXMLStreamReader(
														new StreamSource(new StringReader(messageTimeElement.asXML())));

												jaxbContext = JAXBContext.newInstance(MessageTime.class);
												jaxbUnMarshller = jaxbContext.createUnmarshaller();
												msgTime = (MessageTime) jaxbUnMarshller.unmarshal(xsr);

												Element expirePoll = message.getChildElement("pollResult",
														"urn:xmpp:pollResult");
												xsr = xif.createXMLStreamReader(
														new StreamSource(new StringReader(expirePoll.asXML())));
												jaxbContext = JAXBContext.newInstance(PollExpire.class);
												jaxbUnMarshller = jaxbContext.createUnmarshaller();
												PollExpire pollExpireObj = (PollExpire) jaxbUnMarshller.unmarshal(xsr);

												if (pollExpireObj == null)
													log.info("Null Poll Expire for one to one chat");
												else
													log.info("Inside Poll Expire for one to one chat");

												PushNotificationPayloadDto notiDto = new PushNotificationPayloadDto();
												notiDto.setMessageType("Chat");
												notiDto.setMessageId(message.getID());
												notiDto.setFromJID(message.getFrom().toBareJID());
												notiDto.setToJID(message.getTo().toBareJID());
												notiDto.setFromName(fromName);
												notiDto.setGroupName(null);
												notiDto.setMessageTime(msgTime.getTime());
												notiDto.setSubject(message.getSubject());
												notiDto.setPayload(pollExpireObj);
												notiDto.setBody("Poll has been declared");
												notiDto.setAndroidBody("Poll has been declared");

												log.info("Before Push notification send for poll expire.");

												controller.publishNotification(userDeviceDetail, notiDto);

												log.info("After Push notification send for poll expire.");

											} catch (JAXBException e) {
												e.printStackTrace();
												log.error("Invalid OpinionPoll Poll Expire Request");
											} catch (XMLStreamException e) {
												log.error("Fail to parse message Subject 19" + e.getLocalizedMessage());
												e.printStackTrace();
											}
											break;
										case "21": // Moment
											try {
												XMLStreamReader xsr = xif.createXMLStreamReader(
														new StreamSource(new StringReader(messageTimeElement.asXML())));

												jaxbContext = JAXBContext.newInstance(MessageTime.class);
												jaxbUnMarshller = jaxbContext.createUnmarshaller();
												msgTime = (MessageTime) jaxbUnMarshller.unmarshal(xsr);

												Element moment = message.getChildElement("moment", "urn:xmpp:moment");
												xsr = xif.createXMLStreamReader(
														new StreamSource(new StringReader(moment.asXML())));
												jaxbContext = JAXBContext.newInstance(Moment.class);
												jaxbUnMarshller = jaxbContext.createUnmarshaller();
												Moment momentObj = (Moment) jaxbUnMarshller.unmarshal(xsr);

												if (momentObj == null)
													log.info("Null Moment for one to one chat");
												else
													log.info("Inside Moment for one to one chat");

												PushNotificationPayloadDto notiDto = new PushNotificationPayloadDto();
												notiDto.setMessageType("Chat");
												notiDto.setMessageId(message.getID());
												notiDto.setFromJID(message.getFrom().toBareJID());
												notiDto.setToJID(message.getTo().toBareJID());
												notiDto.setFromName(fromName);
												notiDto.setGroupName(null);
												notiDto.setMessageTime(msgTime.getTime());
												notiDto.setSubject(message.getSubject());
												notiDto.setPayload(momentObj);
												notiDto.setBody("Moment");
												notiDto.setAndroidBody("Moment");

												log.info("Before Push notification send for moment.");

												controller.publishNotification(userDeviceDetail, notiDto);

												log.info("After Push notification send for moment.");

											} catch (JAXBException e) {
												e.printStackTrace();
												log.error("Invalid Moment Request");
											} catch (XMLStreamException e) {
												log.error("Fail to parse message Subject 21" + e.getLocalizedMessage());
												e.printStackTrace();
											}
											break;
										case "22": // create call conference
											try {
												XMLStreamReader xsr = xif.createXMLStreamReader(
														new StreamSource(new StringReader(messageTimeElement.asXML())));

												jaxbContext = JAXBContext.newInstance(MessageTime.class);
												jaxbUnMarshller = jaxbContext.createUnmarshaller();
												msgTime = (MessageTime) jaxbUnMarshller.unmarshal(xsr);

												Element createCallConference = message.getChildElement(
														"createCallConference", "urn:xmpp:createCallConference");
												xsr = xif.createXMLStreamReader(
														new StreamSource(new StringReader(createCallConference.asXML())));
												jaxbContext = JAXBContext.newInstance(CreateCallConference.class);
												jaxbUnMarshller = jaxbContext.createUnmarshaller();
												CreateCallConference createCallConferenceObj = (CreateCallConference) jaxbUnMarshller
														.unmarshal(xsr);

												if (createCallConferenceObj == null)
													log.info("Null create call conference for one to one chat");
												else
													log.info("Inside create call conference for one to one chat");

												PushNotificationPayloadDto notiDto = new PushNotificationPayloadDto();
												notiDto.setMessageType("Chat");
												notiDto.setMessageId(message.getID());
												notiDto.setFromJID(message.getFrom().toBareJID());
												notiDto.setToJID(message.getTo().toBareJID());
												notiDto.setFromName(fromName);
												notiDto.setGroupName(null);
												notiDto.setMessageTime(msgTime.getTime());
												notiDto.setSubject(message.getSubject());
												notiDto.setPayload(createCallConferenceObj);
												notiDto.setBody(message.getBody());
												notiDto.setAndroidBody(message.getBody());

												log.info("Before Push notification send for create Call conference.");

												controller.publishNotification(userDeviceDetail, notiDto);

												log.info("After Push notification send for create Call conference..");

											} catch (JAXBException e) {
												e.printStackTrace();
												log.error("Invalid Create Conference call Request");
											} catch (XMLStreamException e) {
												log.error("Fail to parse message Subject 22" + e.getLocalizedMessage());
												e.printStackTrace();
											}
											break;
										case "23":// update call conference
											try {
												XMLStreamReader xsr = xif.createXMLStreamReader(
														new StreamSource(new StringReader(messageTimeElement.asXML())));

												jaxbContext = JAXBContext.newInstance(MessageTime.class);
												jaxbUnMarshller = jaxbContext.createUnmarshaller();
												msgTime = (MessageTime) jaxbUnMarshller.unmarshal(xsr);

												Element updateCallConference = message.getChildElement(
														"updateCallConference", "urn:xmpp:updateCallConference");
												xsr = xif.createXMLStreamReader(
														new StreamSource(new StringReader(updateCallConference.asXML())));
												jaxbContext = JAXBContext.newInstance(UpdateCallConference.class);
												jaxbUnMarshller = jaxbContext.createUnmarshaller();
												UpdateCallConference updateCallConferenceObj = (UpdateCallConference) jaxbUnMarshller
														.unmarshal(xsr);

												if (updateCallConferenceObj == null)
													log.info("Null update call conference for one to one chat");
												else
													log.info("Inside update call conference for one to one chat");

												PushNotificationPayloadDto notiDto = new PushNotificationPayloadDto();
												notiDto.setMessageType("Chat");
												notiDto.setMessageId(message.getID());
												notiDto.setFromJID(message.getFrom().toBareJID());
												notiDto.setToJID(message.getTo().toBareJID());
												notiDto.setFromName(fromName);
												notiDto.setGroupName(null);
												notiDto.setMessageTime(msgTime.getTime());
												notiDto.setSubject(message.getSubject());
												notiDto.setPayload(updateCallConferenceObj);
												notiDto.setBody(message.getBody());
												notiDto.setAndroidBody(message.getBody());

												log.info("Before update notification send for create Call conference.");

												controller.publishNotification(userDeviceDetail, notiDto);

												log.info("After Push notification send for update Call conference..");

											} catch (JAXBException e) {
												e.printStackTrace();
												log.error("Invalid Create Conference call Request");
											} catch (XMLStreamException e) {
												log.error("Fail to parse message Subject 22" + e.getLocalizedMessage());
												e.printStackTrace();
											}
											break;

										default:
											break;
									}
								}
								routingTable.routePacket(recipientJID, packet, false);
							} else {
								routingTable.routePacket(recipientJID, packet, false);
							}
						} else {
							routingTable.routePacket(recipientJID, packet, false);
						}
					} catch (JAXBException ex) {
						log.error("Fail to parse messageMedia subject 15");
					} catch (XMLStreamException e) {
						log.error("Fail to parse subject 15" + e.getLocalizedMessage());
					} catch (Exception e) {
						log.error("Failed to route packet: " + packet.toXML(), e);
						log.error("Exception cause : " + e.getLocalizedMessage());
						routingFailed(recipientJID, packet);
					}

					// Sent carbon copies to other resources of the sender:
					// When a client sends a <message/> of type "chat"
					if (packet.getType() == Message.Type.chat && !isPrivate && session != null) { // &&
																									// session.isMessageCarbonsEnabled()
																									// ??? // must the
																									// own session also
																									// be carbon
																									// enabled?
						log.info("Dhaval Message Router 143 :: " + packet.toString());
						List<JID> routes = routingTable.getRoutes(packet.getFrom().asBareJID(), null);
						for (JID route : routes) {
							// The sending server SHOULD NOT send a forwarded copy to the sending full JID
							// if it is a Carbons-enabled resource.
							if (!route.equals(session.getAddress())) {
								ClientSession clientSession = sessionManager.getSession(route);
								if (clientSession != null && clientSession.isMessageCarbonsEnabled()) {
									Message message = new Message();
									// The wrapping message SHOULD maintain the same 'type' attribute value
									message.setType(packet.getType());
									// the 'from' attribute MUST be the Carbons-enabled user's bare JID
									message.setFrom(packet.getFrom().asBareJID());
									// and the 'to' attribute SHOULD be the full JID of the resource receiving the
									// copy
									message.setTo(route);
									// The content of the wrapping message MUST contain a <sent/> element qualified
									// by the namespace "urn:xmpp:carbons:2", which itself contains a <forwarded/>
									// qualified by the namespace "urn:xmpp:forward:0" that contains the original
									// <message/> stanza.
									message.addExtension(new Sent(new Forwarded(packet)));
									clientSession.process(message);
								}
							}
						}
					}
				}
			} else {
				packet.setTo(session.getAddress());
				packet.setFrom((JID) null);
				packet.setError(PacketError.Condition.not_authorized);
				log.info("============================================================");
				session.process(packet);
			}
			// Invoke the interceptors after we have processed the read packet
			InterceptorManager.getInstance().invokeInterceptors(packet, session, true, true);
		} catch (PacketRejectedException e) {
			// An interceptor rejected this packet
			if (session != null && e.getRejectionMessage() != null && e.getRejectionMessage().trim().length() > 0) {
				// A message for the rejection will be sent to the sender of the rejected packet
				Message reply = new Message();
				reply.setID(packet.getID());
				reply.setTo(session.getAddress());
				reply.setFrom(packet.getTo());
				reply.setType(packet.getType());
				reply.setThread(packet.getThread());
				reply.setBody(e.getRejectionMessage());
				session.process(reply);
			}
		}
	}

	/**
	 * Forwards the received message to the list of users defined in the property
	 * <b>xmpp.forward.admins</b>. The property may include bare JIDs or just
	 * usernames separated by commas or white spaces. When using bare JIDs the
	 * target user may belong to a remote server.
	 * <p>
	 *
	 * If the property <b>xmpp.forward.admins</b> was not defined then the message
	 * will be sent to all the users allowed to enter the admin console.
	 *
	 * @param packet the message to forward.
	 */
	private void sendMessageToAdmins(Message packet) {
		String jids = JiveGlobals.getProperty("xmpp.forward.admins");
		if (jids != null && jids.trim().length() > 0) {
			// Forward the message to the users specified in the "xmpp.forward.admins"
			// property
			StringTokenizer tokenizer = new StringTokenizer(jids, ", ");
			while (tokenizer.hasMoreTokens()) {
				String username = tokenizer.nextToken();
				Message forward = packet.createCopy();
				if (username.contains("@")) {
					// Use the specified bare JID address as the target address
					forward.setTo(username);
				} else {
					forward.setTo(username + "@" + serverName);
				}
				route(forward);
			}
		} else {
			// Forward the message to the users allowed to log into the admin console
			for (JID jid : XMPPServer.getInstance().getAdmins()) {
				Message forward = packet.createCopy();
				forward.setTo(jid);
				route(forward);
			}
		}
	}

	@Override
	public void initialize(XMPPServer server) {
		super.initialize(server);
		messageStrategy = server.getOfflineMessageStrategy();
		routingTable = server.getRoutingTable();
		sessionManager = server.getSessionManager();
		multicastRouter = server.getMulticastRouter();
		userManager = server.getUserManager();
		serverName = server.getServerInfo().getXMPPDomain();
	}

	/**
	 * Notification message indicating that a packet has failed to be routed to the
	 * recipient.
	 *
	 * @param recipient address of the entity that failed to receive the packet.
	 * @param packet    Message packet that failed to be sent to the recipient.
	 */
	public void routingFailed(JID recipient, Packet packet) {
		log.info("Message sent to unreachable address: " + packet.toXML());
		final Message msg = (Message) packet;

		if (msg.getType().equals(Message.Type.chat) && serverName.equals(recipient.getDomain())
				&& recipient.getResource() != null) {
			// Find an existing AVAILABLE session with non-negative priority.
			log.info("Dhaval Message Router 254");
			for (JID address : routingTable.getRoutes(recipient.asBareJID(), packet.getFrom())) {
				ClientSession session = routingTable.getClientRoute(address);
				if (session != null && session.isInitialized()) {
					if (session.getPresence().getPriority() >= 0) {
						// If message was sent to an unavailable full JID of a user then retry using the
						// bare JID.
						routingTable.routePacket(recipient.asBareJID(), packet, false);
						return;
					}
				}
			}
		}

		if (serverName.equals(recipient.getDomain())) {
			// Delegate to offline message strategy, which will either bounce or ignore the
			// message depending on user settings.
			log.info("Delegating to offline message strategy.");
			messageStrategy.storeOffline((Message) packet);
		} else {
			// Recipient is not a local user. Bounce the message.
			// Note: this is similar, but not equal, to handling of message handling to
			// local users in OfflineMessageStrategy.

			// 8.5.2. localpart@domainpart
			// 8.5.2.2. No Available or Connected Resources
			if (recipient.getResource() == null) {
				if (msg.getType() == Message.Type.headline || msg.getType() == Message.Type.error) {
					// For a message stanza of type "headline" or "error", the server MUST silently
					// ignore the message.
					log.info("Dhaval Message Router 283");
					log.trace("Not bouncing a message stanza to a bare JID of non-local user, of type {}",
							msg.getType());
					return;
				}
			} else {
				// 8.5.3. localpart@domainpart/resourcepart
				// 8.5.3.2.1. Message

				// For a message stanza of type "error", the server MUST silently ignore the
				// stanza.
				if (msg.getType() == Message.Type.error) {
					log.info("Dhaval Message Router 293");
					log.trace("Not bouncing a message stanza to a full JID of non-local user, of type {}",
							msg.getType());
					return;
				}
			}

			bounce(msg);
		}
	}

	private void bounce(Message message) {
		// The bouncing behavior as implemented beyond this point was introduced as part
		// of OF-1852. This kill-switch allows it to be disabled again in case it
		// introduces unwanted side-effects.
		if (!JiveGlobals.getBooleanProperty("xmpp.message.bounce", true)) {
			log.trace("Not bouncing a message stanza, as bouncing is disabled by configuration.");
			return;
		}

		// Do nothing if the packet included an error. This intends to prevent scenarios
		// where a stanza that is bounced itself gets bounced, causing a loop.
		if (message.getError() != null) {
			log.trace(
					"Not bouncing a stanza that included an error (to prevent never-ending loops of bounces-of-bounces).");
			return;
		}

		// Do nothing if the sender was the server itself
		if (message.getFrom() == null || message.getFrom().toString().equals(serverName)) {
			log.trace("Not bouncing a stanza that was sent by the server itself.");
			return;
		}
		try {
			log.trace("Bouncing a message stanza.");

			// Generate a rejection response to the sender
			final Message errorResponse = message.createCopy();
			// return an error stanza to the sender, which SHOULD be <service-unavailable/>
			errorResponse.setError(PacketError.Condition.service_unavailable);
			errorResponse.setFrom(message.getTo());
			errorResponse.setTo(message.getFrom());
			// Send the response
			route(errorResponse);
		} catch (Exception e) {
			log.error("An exception occurred while trying to bounce a message.", e);
		}
	}
}
