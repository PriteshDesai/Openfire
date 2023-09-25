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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Timer;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.FilenameUtils;
import org.dom4j.Element;
import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.openfire.container.BasicModule;
import org.jivesoftware.openfire.custom.dto.Call;
import org.jivesoftware.openfire.custom.dto.Contact;
import org.jivesoftware.openfire.custom.dto.CreateCallConference;
import org.jivesoftware.openfire.custom.dto.CustomPollOptionControler;
import org.jivesoftware.openfire.custom.dto.DeviceType;
import org.jivesoftware.openfire.custom.dto.Location;
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
import org.jivesoftware.openfire.disco.ServerFeaturesProvider;
import org.jivesoftware.openfire.muc.spi.OpinionPollExpireTimerTask;
import org.jivesoftware.openfire.privacy.PrivacyList;
import org.jivesoftware.openfire.privacy.PrivacyListManager;
import org.jivesoftware.openfire.pushnotifcation.PushNotificationController;
import org.jivesoftware.openfire.user.UserManager;
import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.PacketError;

import com.google.gson.Gson;

/**
 * Controls what is done with offline messages.
 *
 * @author Iain Shigeoka
 */
public class OfflineMessageStrategy extends BasicModule implements ServerFeaturesProvider {

    private static final Logger Log = LoggerFactory.getLogger(OfflineMessageStrategy.class);

    private static final SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

    private static final StringBuffer SQL_GET_SPECIFIC_OPINION_POLL_WITH_USER_DETAILS = new StringBuffer(
            "select pm.ofroomid, po.optionname, COALESCE(NULLIF(array_agg(pur.username), '{NULL}'), '{}') as userids, count(pur.username) as noofusers ")
            .append(" from ofpollmaster pm inner join ofpolloptions po on po.pollid = pm.pollid ")
            .append(" left join ofpolluserresponse pur ON pur.polloptionid = po.polloptionid where po.pollid = ? GROUP BY pm.ofroomid, po.optionname ");

    private static int quota = 100 * 1024; // Default to 100 K.
    private static Type type = Type.store_and_bounce;

    private static List<OfflineMessageListener> listeners = new CopyOnWriteArrayList<>();

    private OfflineMessageStore messageStore;
    private JID serverAddress;
    private PacketRouter router;

    public OfflineMessageStrategy() {
        super("Offline Message Strategy");
    }

    public int getQuota() {
        return quota;
    }

    public void setQuota(int quota) {
        OfflineMessageStrategy.quota = quota;
        JiveGlobals.setProperty("xmpp.offline.quota", Integer.toString(quota));
    }

    public OfflineMessageStrategy.Type getType() {
        return type;
    }

    public void setType(OfflineMessageStrategy.Type type) {
        if (type == null) {
            throw new IllegalArgumentException();
        }
        OfflineMessageStrategy.type = type;
        JiveGlobals.setProperty("xmpp.offline.type", type.toString());
    }

    public void storeOffline(Message message) {
        if (message != null) {
            // Do nothing if the message was sent to the server itself, an anonymous user or
            // a non-existent user
            // Also ignore message carbons
            JID recipientJID = message.getTo();
            if (recipientJID == null || serverAddress.equals(recipientJID) || recipientJID.getNode() == null
                    || message.getExtension("received", "urn:xmpp:carbons:2") != null
                    || !UserManager.getInstance().isRegisteredUser(recipientJID, false)) {
                return;
            }
            // Do not store messages if communication is blocked
            PrivacyList list = PrivacyListManager.getInstance().getDefaultPrivacyList(recipientJID.getNode());
            if (list != null && list.shouldBlockPacket(message)) {
                Message result = message.createCopy();
                result.setTo(message.getFrom());
                result.setFrom(message.getTo());
                result.setError(PacketError.Condition.service_unavailable);
                XMPPServer.getInstance().getRoutingTable().routePacket(message.getFrom(), result, true);
                return;
            }
            // 8.5.2. localpart@domainpart
            // 8.5.2.2. No Available or Connected Resources
            if (recipientJID.getResource() == null) {
                if (message.getType() == Message.Type.headline || message.getType() == Message.Type.error) {
                    // For a message stanza of type "headline" or "error", the server MUST silently
                    // ignore the message.
                    return;
                }
                // // For a message stanza of type "groupchat", the server MUST return an error
                // to the sender, which SHOULD be <service-unavailable/>.
                else if (message.getType() == Message.Type.groupchat) {
                    Log.info("Dhaval Offline msg stratergy 112");
                    bounce(message);
                    return;
                }
            } else {
                // 8.5.3. localpart@domainpart/resourcepart
                // 8.5.3.2.1. Message

                // For a message stanza of type "normal", "groupchat", or "headline", the server
                // MUST either (a) silently ignore the stanza
                // or (b) return an error stanza to the sender, which SHOULD be
                // <service-unavailable/>.
                if (message.getType() == Message.Type.normal || message.getType() == Message.Type.groupchat
                        || message.getType() == Message.Type.headline) {
                    // Depending on the OfflineMessageStragey, we may silently ignore or bounce
                    Log.info("Dhaval Offline msg stratergy 124");
                    if (type == Type.bounce) {
                        bounce(message);
                    }
                    // Either bounce or silently ignore, never store such messages
                    return;
                }
                // For a message stanza of type "error", the server MUST silently ignore the
                // stanza.
                else if (message.getType() == Message.Type.error) {
                    return;
                }
            }

            switch (type) {
                case bounce:
                    Log.info("Dhaval Offline msg stratergy 139");
                    bounce(message);
                    break;
                case store:
                    Log.info("Dhaval Offline msg stratergy 143");
                    store(message);
                    break;
                case store_and_bounce:
                    Log.info("Dhaval Offline msg stratergy 147" + message.toXML());
                    if (underQuota(message)) {
                        Log.info("Dhaval Offline msg stratergy 149" + message.toXML());
                        store(message);
                    } else {
                        Log.info("Dhaval Offline msg stratergy 153");
                        Log.debug("Unable to store, as user is over storage quota. Bouncing message instead: "
                                + message.toXML());
                        bounce(message);
                    }
                    break;
                case store_and_drop:
                    Log.info("Dhaval Offline msg stratergy 159");
                    if (underQuota(message)) {
                        Log.info("Dhaval Offline msg stratergy 161");
                        store(message);
                    } else {
                        Log.info("Dhaval Offline msg stratergy 164");
                        Log.debug("Unable to store, as user is over storage quota. Silently dropping message: "
                                + message.toXML());
                    }
                    break;
                case drop:
                    // Drop essentially means silently ignore/do nothing
                    Log.info("Dhaval Offline msg stratergy 170");
                    break;
            }

            if (message != null && message.getSubject() != null) {
                PushNotificationController controller = new PushNotificationController();
                String fromName = controller.getNameFromUserName(message.getFrom().toString());

                UserDeviceEntity userDeviceDetail = controller
                        .getUserDeviceDetailFromJID(message.getTo().toString().replace("/chat", ""));

                if (userDeviceDetail == null) {
                    Log.error("User Device Detail not found for JID :: " + message.getTo().toString());
                    return;
                } else if (!userDeviceDetail.isActive()) {
                    Log.info("Push Notification Disabled for user :: " + message.getTo().toString());
                    return;
                }

                Gson gson = new Gson();
                Log.info("User Device Details :: " + gson.toJson(userDeviceDetail));

                JAXBContext jaxbContext = null;
                Unmarshaller jaxbUnMarshller = null;

                MessageTime msgTime = null;
                Element messageTimeElement = message.getChildElement("messageTime", "urn:xmpp:time");
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
                            Log.info("Received Message Body :: " + message.getBody());
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
                            Log.info("After Setting into Notification Dto Payload is :: " + notiDto.getPayload());
                            Log.info("After Setting into Notification Dto Body is :: " + notiDto.getBody());
                            controller.publishNotification(userDeviceDetail, notiDto);
                        } catch (JAXBException ex) {
                            ex.printStackTrace();
                            Log.error("Fail to parse TextMessage subject 1" + ex.getLocalizedMessage());
                        } catch (XMLStreamException e) {
                            Log.error("Fail to parse TextMessage subject 1" + e.getLocalizedMessage());
                            e.printStackTrace();
                        }
                        break;

                    case "2": // Audio Message

                        try {
                            XMLStreamReader xsr = xif
                                    .createXMLStreamReader(new StreamSource(new StringReader(messageTimeElement.asXML())));

                            jaxbContext = JAXBContext.newInstance(MessageTime.class);
                            jaxbUnMarshller = jaxbContext.createUnmarshaller();
                            msgTime = (MessageTime) jaxbUnMarshller.unmarshal(xsr);

                            Element audioElement = message.getChildElement("mediaData", "urn:xmpp:media");

                            xsr = xif.createXMLStreamReader(new StreamSource(new StringReader(audioElement.asXML())));
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
                                    notiDto.setBody(
                                            "\\uD83C\\uDFB5 " + audioMessageMedia.getMessageMedia().size() + " Audios");
                                    notiDto.setAndroidBody(
                                            "\\uD83C\\uDFB5 " + audioMessageMedia.getMessageMedia().size() + " Audios");
                                } else {
                                    notiDto.setBody(
                                            "\\uD83C\\uDFB5 " + audioMessageMedia.getMessageMedia().size() + " Audio");
                                    notiDto.setAndroidBody(
                                            "\\uD83C\\uDFB5 " + audioMessageMedia.getMessageMedia().size() + " Audio");
                                }
                            } else if (DeviceType.IOS.equals(userDeviceDetail.getDeviceType())) {
                                if (audioMessageMedia.getMessageMedia().size() > 1) {
                                    notiDto.setBody("🎵 " + audioMessageMedia.getMessageMedia().size() + " Audios");
                                } else {
                                    notiDto.setBody("🎵 " + audioMessageMedia.getMessageMedia().size() + " Audio");
                                }
                            }

                            controller.publishNotification(userDeviceDetail, notiDto);
                        } catch (JAXBException ex) {
                            Log.error("Fail to parse subject 2");
                        } catch (XMLStreamException e) {
                            Log.error("Fail to parse subject 2" + e.getLocalizedMessage());
                            e.printStackTrace();
                        }
                        break;
                    case "3": // Image or Video Element
                        try {
                            XMLStreamReader xsr = xif
                                    .createXMLStreamReader(new StreamSource(new StringReader(messageTimeElement.asXML())));

                            jaxbContext = JAXBContext.newInstance(MessageTime.class);
                            jaxbUnMarshller = jaxbContext.createUnmarshaller();
                            msgTime = (MessageTime) jaxbUnMarshller.unmarshal(xsr);
                            Element audioElement = message.getChildElement("mediaData", "urn:xmpp:media");
                            xsr = xif.createXMLStreamReader(new StreamSource(new StringReader(audioElement.asXML())));
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
                                } else if (FilenameUtils.getExtension(media.getFileName()).equalsIgnoreCase("gif")) {
                                    Log.info("File type GIF : " + media);
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
                                             * This --> //ud83d//udc7e <-- is the java eclipse encodding for U+1F47E ()ALIEN
                                             * MONSTER visit : https://charbase.com/1f47e-unicode-alien-monster
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
                            Log.error("Fail to parse subject 3");
                        } catch (XMLStreamException e) {
                            Log.error("Fail to parse subject 3" + e.getLocalizedMessage());
                            e.printStackTrace();
                        } catch (Exception ex) {
                            Log.info("GSON EXCEPTION :: " + ex.getLocalizedMessage());
                        }
                        break;
                    case "4":

                        break;
                    case "5":

                        break;
                    case "6": // Document
                        try {
                            XMLStreamReader xsr = xif
                                    .createXMLStreamReader(new StreamSource(new StringReader(messageTimeElement.asXML())));

                            jaxbContext = JAXBContext.newInstance(MessageTime.class);
                            jaxbUnMarshller = jaxbContext.createUnmarshaller();
                            msgTime = (MessageTime) jaxbUnMarshller.unmarshal(xsr);

                            Element audioElement = message.getChildElement("mediaData", "urn:xmpp:media");

                            xsr = xif.createXMLStreamReader(new StreamSource(new StringReader(audioElement.asXML())));
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
                                    notiDto.setBody(
                                            "\\uD83D\\uDCC4 " + documentMessageMedia.getMessageMedia().size() + " Files");
                                    notiDto.setAndroidBody(
                                            "\\uD83D\\uDCC4 " + documentMessageMedia.getMessageMedia().size() + " Files");
                                } else {
                                    notiDto.setBody(
                                            "\\uD83D\\uDCC4 " + documentMessageMedia.getMessageMedia().size() + " File");
                                    notiDto.setAndroidBody(
                                            "\\uD83D\\uDCC4 " + documentMessageMedia.getMessageMedia().size() + " File");
                                }
                            } else if (DeviceType.IOS.equals(userDeviceDetail.getDeviceType())) {
                                if (documentMessageMedia.getMessageMedia().size() > 1) {
                                    notiDto.setBody("📄 " + documentMessageMedia.getMessageMedia().size() + " Files");
                                } else {
                                    notiDto.setBody("📄 " + documentMessageMedia.getMessageMedia().size() + " File");
                                }
                            }

                            controller.publishNotification(userDeviceDetail, notiDto);
                        } catch (JAXBException ex) {
                            Log.error("Fail to parse messageMedia subject 6");
                        } catch (XMLStreamException e) {
                            // TODO Auto-generated catch block
                            Log.error("Fail to parse subject 6" + e.getLocalizedMessage());
                            e.printStackTrace();
                        }
                        break;
                    case "7": // location
                        try {

                            XMLStreamReader xsr = xif
                                    .createXMLStreamReader(new StreamSource(new StringReader(messageTimeElement.asXML())));

                            jaxbContext = JAXBContext.newInstance(MessageTime.class);
                            jaxbUnMarshller = jaxbContext.createUnmarshaller();
                            msgTime = (MessageTime) jaxbUnMarshller.unmarshal(xsr);

                            Element locationElement = message.getChildElement("location", "urn:xmpp:location");

                            xsr = xif.createXMLStreamReader(new StreamSource(new StringReader(locationElement.asXML())));
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
                            Log.error("Fail to parse messageMedia subject 7");
                        } catch (XMLStreamException e) {
                            Log.error("Fail to parse subject 7" + e.getLocalizedMessage());
                            e.printStackTrace();
                        }
                        break;
                    case "8": // contact
                        try {
                            XMLStreamReader xsr = xif
                                    .createXMLStreamReader(new StreamSource(new StringReader(messageTimeElement.asXML())));

                            jaxbContext = JAXBContext.newInstance(MessageTime.class);
                            jaxbUnMarshller = jaxbContext.createUnmarshaller();
                            msgTime = (MessageTime) jaxbUnMarshller.unmarshal(xsr);

                            Element locationElement = message.getChildElement("contact", "urn:xmpp:contact");

                            xsr = xif.createXMLStreamReader(new StreamSource(new StringReader(locationElement.asXML())));
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
                            Log.error("Fail to parse subject 8");
                        } catch (XMLStreamException e) {
                            Log.error("Fail to parse subject 8" + e.getLocalizedMessage());
                            e.printStackTrace();
                        }
                        break;
                    case "9": // RSS
                        try {

                            XMLStreamReader xsr = xif
                                    .createXMLStreamReader(new StreamSource(new StringReader(messageTimeElement.asXML())));

                            jaxbContext = JAXBContext.newInstance(MessageTime.class);
                            jaxbUnMarshller = jaxbContext.createUnmarshaller();
                            msgTime = (MessageTime) jaxbUnMarshller.unmarshal(xsr);

                            Element locationElement = message.getChildElement("rss", "urn:xmpp:rss");

                            xsr = xif.createXMLStreamReader(new StreamSource(new StringReader(locationElement.asXML())));
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
                            Log.error("Fail to parse subject 9");
                        } catch (XMLStreamException e) {
                            // TODO Auto-generated catch block
                            Log.error("Fail to parse subject 9" + e.getLocalizedMessage());
                            e.printStackTrace();
                        }

                        break;
                    case "10": // Relay
                        try {
                            XMLStreamReader xsr = xif
                                    .createXMLStreamReader(new StreamSource(new StringReader(messageTimeElement.asXML())));

                            jaxbContext = JAXBContext.newInstance(MessageTime.class);
                            jaxbUnMarshller = jaxbContext.createUnmarshaller();
                            msgTime = (MessageTime) jaxbUnMarshller.unmarshal(xsr);

                            Element relayElement = message.getChildElement("relay", "urn:xmpp:relay");

                            xsr = xif.createXMLStreamReader(new StreamSource(new StringReader(relayElement.asXML())));
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

                            Log.info("Parsed Relay Object :: " + new Gson().toJson(relayData));
                            controller.publishNotification(userDeviceDetail, notiDto);

                        } catch (JAXBException ex) {
                            Log.error("Fail to parse subject 10");
                        } catch (XMLStreamException e) {
                            Log.error("Fail to parse subject 10" + e.getLocalizedMessage());
                            e.printStackTrace();
                        }
                        break;
                    case "11": // Create Poll
                        try {

                            XMLStreamReader xsr = xif
                                    .createXMLStreamReader(new StreamSource(new StringReader(messageTimeElement.asXML())));

                            jaxbContext = JAXBContext.newInstance(MessageTime.class);
                            jaxbUnMarshller = jaxbContext.createUnmarshaller();
                            msgTime = (MessageTime) jaxbUnMarshller.unmarshal(xsr);

                            Element oppinionPoll = message.getChildElement("opinionPoll", "urn:xmpp:createpoll");
                            xsr = xif.createXMLStreamReader(new StreamSource(new StringReader(oppinionPoll.asXML())));
                            jaxbContext = JAXBContext.newInstance(OpinionPoll.class);
                            jaxbUnMarshller = jaxbContext.createUnmarshaller();
                            OpinionPoll oppinionPollObj = (OpinionPoll) jaxbUnMarshller.unmarshal(xsr);

                            if (oppinionPollObj == null)
                                Log.info("Null Create Poll for one to one Chat");
                            else
                                Log.info("Inside Create Poll for one to one Chat");

                            String fromJID = message.getFrom().toBareJID();
                            String toJID = message.getTo().toBareJID();

                            CustomPollOptionControler custompoll = new CustomPollOptionControler();
                            custompoll.addCustomOpinonPoll(oppinionPollObj, 0L, message.getID(), Message.Type.chat.name(),
                                    fromJID, toJID);

                            Date expiredAt = new Date(oppinionPollObj.getExpireDate());
                            Log.info("Date :: " + expiredAt);

                            formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
                            String dateString = formatter.format(expiredAt);
                            Log.info("Formatted Date String:: " + dateString);

                            String timeZone = oppinionPollObj.getTimeZone();
                            formatter.setTimeZone(TimeZone.getTimeZone(timeZone));
                            Log.info("Parsed Date :: " + formatter.parse(dateString));

                            Log.info("Schedule Date :: " + formatter.parse(dateString) + " based on TimeZone :: "
                                    + timeZone);

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

                            Log.info("Push Notification send successfully for Create Poll in one to one chat");

                        } catch (JAXBException e) {
                            Log.error("Invalid OpinionPoll Create Request");
                            e.printStackTrace();
                        } catch (ParseException e) {
                            Log.error("Parse Exception Subject 11");
                            e.printStackTrace();
                        } catch (XMLStreamException e) {
                            Log.error("Fail to parse message Subject 11" + e.getLocalizedMessage());
                            e.printStackTrace();
                        }
                        break;
                    case "14": // Update Poll
                        try {

                            XMLStreamReader xsr = xif
                                    .createXMLStreamReader(new StreamSource(new StringReader(messageTimeElement.asXML())));

                            jaxbContext = JAXBContext.newInstance(MessageTime.class);
                            jaxbUnMarshller = jaxbContext.createUnmarshaller();
                            msgTime = (MessageTime) jaxbUnMarshller.unmarshal(xsr);

                            Element oppinionPollResponse = message.getChildElement("opinionPollUpdate",
                                    "urn:xmpp:opinionPollUpdate");
                            xsr = xif.createXMLStreamReader(
                                    new StreamSource(new StringReader(oppinionPollResponse.asXML())));
                            jaxbContext = JAXBContext.newInstance(OpinionPollUpdate.class);
                            jaxbUnMarshller = jaxbContext.createUnmarshaller();

                            OpinionPollUpdate opinionPollUpdate = (OpinionPollUpdate) jaxbUnMarshller.unmarshal(xsr);

                            if (opinionPollUpdate == null)
                                Log.info("Null Update Poll for one to one Chat");
                            else
                                Log.info("Inside Update Poll for one to one Chat");

                            CustomPollOptionControler custompoll = new CustomPollOptionControler();

                            if (opinionPollUpdate.getIsSelect().equals("no")) {
                                custompoll.deleteOpinionPollResponse(opinionPollUpdate);
                                Log.info("User Response Deleted from Database.");
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
                                pstmt = conn.prepareStatement(SQL_GET_SPECIFIC_OPINION_POLL_WITH_USER_DETAILS.toString());
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
                                        opinionPollUpdatePayload.setPollId(opinionPollUpdate.getPollId());
                                        opinionPollUpdatePayload.setRoomId(opinionPollUpdate.getRoomId());
                                        opinionPollUpdatePayload.setMessageId(message.getID());
                                        opinionPollUpdatePayload.setIsSelect(opinionPollUpdate.getIsSelect());
                                        opinionPollUpdatePayload.setPollMessageId(opinionPollUpdate.getPollMessageId());

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

                            Log.info("Push Notification send successfully for Update Poll in one to one chat");

                        } catch (JAXBException e) {
                            Log.error("Invalid OpinionPoll Response/Update XML");
                            e.printStackTrace();
                        } catch (XMLStreamException e) {
                            Log.error("Fail to parse message Subject 14" + e.getLocalizedMessage());
                            e.printStackTrace();
                        }
                        break;
                    case "15": // Call
                        try {
                            XMLStreamReader xsr = xif
                                    .createXMLStreamReader(new StreamSource(new StringReader(messageTimeElement.asXML())));

                            jaxbContext = JAXBContext.newInstance(MessageTime.class);
                            jaxbUnMarshller = jaxbContext.createUnmarshaller();
                            msgTime = (MessageTime) jaxbUnMarshller.unmarshal(xsr);

                            Element callElement = message.getChildElement("call", "urn:xmpp:call");

                            xsr = xif.createXMLStreamReader(new StreamSource(new StringReader(callElement.asXML())));
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
                             * Now user will get push notification for any callData.getReason for initiate,
                             * hangup, rejected, accepted
                             */
                            controller.publishNotification(userDeviceDetail, notiDto);

                        } catch (JAXBException ex) {
                            Log.error("Fail to parse messageMedia subject 15");
                        } catch (XMLStreamException e) {
                            Log.error("Fail to parse subject 15" + e.getLocalizedMessage());
                        } catch (Exception e) {
                            Log.error("Fail to parse subject 15" + e.getLocalizedMessage());
                        }

                        break;
                    case "18": // Delete Poll
                        try {
                            Log.info(" Into Opinion Poll Delete............");
                            Log.info("Delete Poll Message ::" + message.toXML());

                            XMLStreamReader xsr = xif
                                    .createXMLStreamReader(new StreamSource(new StringReader(messageTimeElement.asXML())));

                            jaxbContext = JAXBContext.newInstance(MessageTime.class);
                            jaxbUnMarshller = jaxbContext.createUnmarshaller();
                            msgTime = (MessageTime) jaxbUnMarshller.unmarshal(xsr);

                            Element deletePollElement = message.getChildElement("pollDelete", "urn:xmpp:deletepoll");
                            xsr = xif.createXMLStreamReader(new StreamSource(new StringReader(deletePollElement.asXML())));
                            jaxbContext = JAXBContext.newInstance(PollDelete.class);
                            jaxbUnMarshller = jaxbContext.createUnmarshaller();
                            PollDelete pollDelete = (PollDelete) jaxbUnMarshller.unmarshal(xsr);

                            if (pollDelete == null)
                                Log.info("Null Delete Poll for one to one Chat");
                            else
                                Log.info("Inside Delete Poll for one to one Chat");

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
                            Log.error("Invalid OpinionPoll Delete poll XML");
                            e.printStackTrace();
                        } catch (XMLStreamException e) {
                            Log.error("Fail to parse message Subject 18" + e.getLocalizedMessage());
                            e.printStackTrace();
                        }
                        break;
                    case "19": // Poll Expire
                        try {
                            XMLStreamReader xsr = xif
                                    .createXMLStreamReader(new StreamSource(new StringReader(messageTimeElement.asXML())));

                            jaxbContext = JAXBContext.newInstance(MessageTime.class);
                            jaxbUnMarshller = jaxbContext.createUnmarshaller();
                            msgTime = (MessageTime) jaxbUnMarshller.unmarshal(xsr);

                            Element expirePoll = message.getChildElement("pollResult", "urn:xmpp:pollResult");
                            xsr = xif.createXMLStreamReader(new StreamSource(new StringReader(expirePoll.asXML())));
                            jaxbContext = JAXBContext.newInstance(PollExpire.class);
                            jaxbUnMarshller = jaxbContext.createUnmarshaller();
                            PollExpire pollExpireObj = (PollExpire) jaxbUnMarshller.unmarshal(xsr);

                            if (pollExpireObj == null)
                                Log.info("Null Poll Expire for one to one chat");
                            else
                                Log.info("Inside Poll Expire for one to one chat");

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

                            Log.info("Before Push notification send for poll expire.");

                            controller.publishNotification(userDeviceDetail, notiDto);

                            Log.info("After Push notification send for poll expire.");

                        } catch (JAXBException e) {
                            e.printStackTrace();
                            Log.error("Invalid OpinionPoll Poll Expire Request");
                        } catch (XMLStreamException e) {
                            Log.error("Fail to parse message Subject 19" + e.getLocalizedMessage());
                            e.printStackTrace();
                        }
                        break;
                    case "21": // Moment
                        try {
                            XMLStreamReader xsr = xif
                                    .createXMLStreamReader(new StreamSource(new StringReader(messageTimeElement.asXML())));

                            jaxbContext = JAXBContext.newInstance(MessageTime.class);
                            jaxbUnMarshller = jaxbContext.createUnmarshaller();
                            msgTime = (MessageTime) jaxbUnMarshller.unmarshal(xsr);

                            Element moment = message.getChildElement("moment", "urn:xmpp:moment");
                            xsr = xif.createXMLStreamReader(new StreamSource(new StringReader(moment.asXML())));
                            jaxbContext = JAXBContext.newInstance(Moment.class);
                            jaxbUnMarshller = jaxbContext.createUnmarshaller();
                            Moment momentObj = (Moment) jaxbUnMarshller.unmarshal(xsr);

                            if (momentObj == null)
                                Log.info("Null Moment for one to one chat");
                            else
                                Log.info("Inside Moment for one to one chat");

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

                            Log.info("Before Push notification send for moment.");

                            controller.publishNotification(userDeviceDetail, notiDto);

                            Log.info("After Push notification send for moment.");

                        } catch (JAXBException e) {
                            e.printStackTrace();
                            Log.error("Invalid Moment Request");
                        } catch (XMLStreamException e) {
                            Log.error("Fail to parse message Subject 21" + e.getLocalizedMessage());
                            e.printStackTrace();
                        }
                        break;
                    case "22": // create call conference
                        try {
                            XMLStreamReader xsr = xif
                                    .createXMLStreamReader(new StreamSource(new StringReader(messageTimeElement.asXML())));

                            jaxbContext = JAXBContext.newInstance(MessageTime.class);
                            jaxbUnMarshller = jaxbContext.createUnmarshaller();
                            msgTime = (MessageTime) jaxbUnMarshller.unmarshal(xsr);

                            Element createCallConference = message.getChildElement("createCallConference",
                                    "urn:xmpp:createCallConference");
                            xsr = xif.createXMLStreamReader(
                                    new StreamSource(new StringReader(createCallConference.asXML())));
                            jaxbContext = JAXBContext.newInstance(CreateCallConference.class);
                            jaxbUnMarshller = jaxbContext.createUnmarshaller();
                            CreateCallConference createCallConferenceObj = (CreateCallConference) jaxbUnMarshller
                                    .unmarshal(xsr);

                            if (createCallConferenceObj == null)
                                Log.info("Null create call conference for one to one chat");
                            else
                                Log.info("Inside create call conference for one to one chat");

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

                            Log.info("Before Push notification send for create Call conference.");

                            controller.publishNotification(userDeviceDetail, notiDto);

                            Log.info("After Push notification send for create Call conference..");

                        } catch (JAXBException e) {
                            e.printStackTrace();
                            Log.error("Invalid Create Conference call Request");
                        } catch (XMLStreamException e) {
                            Log.error("Fail to parse message Subject 22" + e.getLocalizedMessage());
                            e.printStackTrace();
                        }
                        break;
                    case "23":// update call conference
                        try {
                            XMLStreamReader xsr = xif
                                    .createXMLStreamReader(new StreamSource(new StringReader(messageTimeElement.asXML())));

                            jaxbContext = JAXBContext.newInstance(MessageTime.class);
                            jaxbUnMarshller = jaxbContext.createUnmarshaller();
                            msgTime = (MessageTime) jaxbUnMarshller.unmarshal(xsr);

                            Element updateCallConference = message.getChildElement("updateCallConference",
                                    "urn:xmpp:updateCallConference");
                            xsr = xif.createXMLStreamReader(
                                    new StreamSource(new StringReader(updateCallConference.asXML())));
                            jaxbContext = JAXBContext.newInstance(UpdateCallConference.class);
                            jaxbUnMarshller = jaxbContext.createUnmarshaller();
                            UpdateCallConference updateCallConferenceObj = (UpdateCallConference) jaxbUnMarshller
                                    .unmarshal(xsr);

                            if (updateCallConferenceObj == null)
                                Log.info("Null update call conference for one to one chat");
                            else
                                Log.info("Inside update call conference for one to one chat");

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

                            Log.info("Before update notification send for create Call conference.");

                            controller.publishNotification(userDeviceDetail, notiDto);

                            Log.info("After Push notification send for update Call conference..");

                        } catch (JAXBException e) {
                            e.printStackTrace();
                            Log.error("Invalid Create Conference call Request");
                        } catch (XMLStreamException e) {
                            Log.error("Fail to parse message Subject 22" + e.getLocalizedMessage());
                            e.printStackTrace();
                        }
                        break;

                    default:
                        break;
                }
            }
        }
    }

    /**
     * Registers a listener to receive events.
     *
     * @param listener the listener.
     */
    public static void addListener(OfflineMessageListener listener) {
        if (listener == null) {
            throw new NullPointerException();
        }
        listeners.add(listener);
    }

    /**
     * Unregisters a listener to receive events.
     *
     * @param listener the listener.
     */
    public static void removeListener(OfflineMessageListener listener) {
        listeners.remove(listener);
    }

    private boolean underQuota(Message message) {
        return quota > messageStore.getSize(message.getTo().getNode()) + message.toXML().length();
    }

    private void store(Message message) {
        final boolean stored = messageStore.addMessage(message);
        // Inform listeners that an offline message was stored
        if (stored && !listeners.isEmpty()) {
            for (OfflineMessageListener listener : listeners) {
                try {
                    listener.messageStored((OfflineMessage) message);
                } catch (Exception e) {
                    Log.warn("An exception occurred while dispatching a 'messageStored' event!", e);
                }
            }
        }
    }

    private void bounce(Message message) {
        // Do nothing if the sender was the server itself
        if (message.getFrom() == null || message.getFrom().equals(serverAddress)) {
            return;
        }
        try {
            // Generate a rejection response to the sender
            Message errorResponse = message.createCopy();
            // return an error stanza to the sender, which SHOULD be <service-unavailable/>
            errorResponse.setError(PacketError.Condition.service_unavailable);
            errorResponse.setFrom(message.getTo());
            errorResponse.setTo(message.getFrom());
            // Send the response
            router.route(errorResponse);
            // Inform listeners that an offline message was bounced
            if (!listeners.isEmpty()) {
                for (OfflineMessageListener listener : listeners) {
                    try {
                        listener.messageBounced(message);
                    } catch (Exception e) {
                        Log.warn("An exception occurred while dispatching a 'messageBounced' event!", e);
                    }
                }
            }
        } catch (Exception e) {
            Log.error(e.getMessage(), e);
        }
    }

    @Override
    public void initialize(XMPPServer server) {
        super.initialize(server);
        messageStore = server.getOfflineMessageStore();
        router = server.getPacketRouter();
        serverAddress = new JID(server.getServerInfo().getXMPPDomain());

        JiveGlobals.migrateProperty("xmpp.offline.quota");
        JiveGlobals.migrateProperty("xmpp.offline.type");

        String quota = JiveGlobals.getProperty("xmpp.offline.quota");
        if (quota != null && quota.length() > 0) {
            OfflineMessageStrategy.quota = Integer.parseInt(quota);
        }
        String type = JiveGlobals.getProperty("xmpp.offline.type");
        if (type != null && type.length() > 0) {
            OfflineMessageStrategy.type = Type.valueOf(type);
        }
    }

    @Override
    public Iterator<String> getFeatures() {
        switch (type) {
            case store:
            case store_and_bounce:
            case store_and_drop:
                // http://xmpp.org/extensions/xep-0160.html#disco
                return Collections.singleton("msgoffline").iterator();
        }
        return Collections.emptyIterator();
    }

    /**
     * Strategy types.
     */
    public enum Type {

        /**
         * All messages are bounced to the sender.
         */
        bounce,

        /**
         * All messages are silently dropped.
         */
        drop,

        /**
         * All messages are stored.
         */
        store,

        /**
         * Messages are stored up to the storage limit, and then bounced.
         */
        store_and_bounce,

        /**
         * Messages are stored up to the storage limit, and then silently dropped.
         */
        store_and_drop
    }

}
