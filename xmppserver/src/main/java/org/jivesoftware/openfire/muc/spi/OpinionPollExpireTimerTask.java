package org.jivesoftware.openfire.muc.spi;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

import org.dom4j.Element;
import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.custom.dto.PollOpinion;
import org.jivesoftware.openfire.custom.dto.PollOptions;
import org.jivesoftware.openfire.custom.dto.PollOptionsResult;
import org.jivesoftware.openfire.custom.dto.Users;
import org.jivesoftware.openfire.muc.MUCRoom;
import org.jivesoftware.openfire.muc.MultiUserChatService;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

import com.google.gson.Gson;

public class OpinionPollExpireTimerTask extends TimerTask {

    private static final Logger Log = LoggerFactory.getLogger(OpinionPollExpireTimerTask.class);

    private static final StringBuffer SQL_EXPIRE_POLL = new StringBuffer(
            "UPDATE ofpollmaster SET isExpired = true WHERE pollid = ?");

    private static final StringBuffer SQL_GET_SPECIFIC_OPINION_POLL = new StringBuffer(
            "select o.pollid,o.createdby,o.createdat,o.expiredat,o.timezone,o.isexpired,o.question,o.ofroomid,o.messageid,o.messagetype,o.fromjid,o.tojid,array_agg(op.optionname) as polloptions, array_agg(op.polloptionid) as polloptionids ")
            .append(" from ofpollmaster o ").append(" inner join ofpolloptions op on (op.pollid = o.pollid) ")
            .append(" WHERE o.pollid = ? ").append(" group by o.pollid ");

    private static final StringBuffer SQL_GET_ALL_POLL_OPTIONS = new StringBuffer(
            "select o2.optionname,count(ops.username) as usercount,COALESCE(NULLIF(array_agg(ops.username), '{NULL}'), '{}') as users from ofpolluserresponse ops ")
            .append(" right join ofpolloptions o2 on (o2.polloptionid = ops.polloptionid) where o2.pollid = ?")
            .append(" group by ops.polloptionid, o2.optionname ");

    private static final StringBuffer SQL_GET_OPINION_POLL_USER_DETAILS = new StringBuffer(
            " select ou.name ,ou.username , ou.email from ofuser ou ")
            .append(" inner join ofpolluserresponse o on (o.username = ou.username) ")
            .append(" where o.pollid = ?");

    private String pollId;
    private String roomOriginalName;

    public OpinionPollExpireTimerTask(String pollId, String roomOriginalName) {
        this.pollId = pollId;
        this.roomOriginalName = roomOriginalName;
    }

    @Override
    public void run() {

        Log.info("Scheduler Running");
        Gson gson = new Gson();

        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DbConnectionManager.getConnection();

            PollOpinion pollOpinion = null;
            ResultSet rs = null;
            try {

                String messageType = "";
                String fromJID = "";
                String toJID = "";

                pstmt = conn.prepareStatement(SQL_GET_SPECIFIC_OPINION_POLL.toString());
                pstmt.setString(1, pollId);

                Log.info("Get Specific Poll :: " + pstmt.toString());

                rs = pstmt.executeQuery();

                while (rs.next()) {
                    pollOpinion = new PollOpinion();
                    pollOpinion.setPollId(rs.getString("pollid"));
                    pollOpinion.setCreatedBy(rs.getString("createdby"));
                    pollOpinion.setCreatedOn(rs.getLong("createdat"));
                    pollOpinion.setExpiredAt(rs.getLong("expiredat"));
                    pollOpinion.setTimeZone(rs.getString("timezone"));
                    pollOpinion.setExpired(rs.getBoolean("isexpired"));
                    pollOpinion.setQuestion(rs.getString("question"));
                    pollOpinion.setRoomId(rs.getInt("ofroomid"));
                    pollOpinion.setMessageId(rs.getString("messageid"));
                    messageType = rs.getString("messagetype");
                    fromJID = rs.getString("fromjid");
                    toJID = rs.getString("tojid");
                    pollOpinion.setPollOptionsText((String[]) rs.getArray("polloptions").getArray());
                    pollOpinion.setPollOptionIds((Integer[]) rs.getArray("polloptionids").getArray());

                    pstmt = conn.prepareStatement(SQL_GET_ALL_POLL_OPTIONS.toString());
                    pstmt.setString(1, rs.getString("pollid"));

                    Log.info("Get Poll Options :: " + pstmt.toString());

                    ResultSet oprs = pstmt.executeQuery();

                    List<PollOptions> pollOptions = new ArrayList<PollOptions>();
                    while (oprs.next()) {
                        PollOptions pollOption = new PollOptions();
                        pollOption.setCount(oprs.getInt("usercount"));
                        pollOption.setOptionName(oprs.getString("optionname"));
                        pollOption.setUsers((String[]) oprs.getArray("users").getArray());

                        pollOptions.add(pollOption);
                    }

                    pollOpinion.setPollOptions(pollOptions);
                    pstmt.close();

                    pstmt = conn.prepareStatement(SQL_GET_OPINION_POLL_USER_DETAILS.toString());
                    pstmt.setString(1, rs.getString("pollid"));

                    Log.info("Get Poll User Details :: " + pstmt.toString());

                    ResultSet oprss = pstmt.executeQuery();
                    List<Users> users = new ArrayList<Users>();
                    while (oprss.next()) {
                        Users user = new Users();
                        user.setName(oprss.getString("name"));
                        user.setUserName(oprss.getString("username"));
                        user.setEmail(oprss.getString("email"));
                        users.add(user);
                    }
                    pollOpinion.setUsers(users);

                    if (!pollOpinion.isExpired()) {

                        pstmt.close();
                        pstmt = conn.prepareStatement(SQL_EXPIRE_POLL.toString());
                        pstmt.setString(1, pollId);
                        pstmt.executeUpdate();

                        Log.info("Poll Options List :: " + gson.toJson(pollOptions));

                        PollOptionsResult winner = null;
                        if (pollOptions != null && pollOptions.size() > 0) {
                            for (PollOptions optionsRes : pollOptions) {
                                if (winner == null) {
                                    winner = new PollOptionsResult();
                                    winner.setStatus("win");
                                    winner.setPolloption(optionsRes);
                                    continue;
                                }

                                if (optionsRes != null && winner != null) {
                                    Log.info("Poll is expired :: 4");
                                    if (optionsRes.getCount() > winner.getPolloption().getCount()) {
                                        winner.setStatus("win");
                                        winner.setPolloption(optionsRes);
                                    } else if (winner.getPolloption().getCount() == optionsRes.getCount()
                                            && optionsRes.getCount() == 0) {
                                        winner.setStatus("No Response");
                                        winner.setPolloption(optionsRes);
                                    } else if (winner.getPolloption().getCount() == optionsRes.getCount()) {
                                        winner.setStatus("Tie");
                                        winner.setPolloption(optionsRes);
                                    }
                                }
                            }
                        }

                        if (winner == null) {
                            winner = new PollOptionsResult("No Response", null);
                        } else if (winner != null && winner.getPolloption() != null
                                && winner.getPolloption().getCount() == 0) {
                            winner.setStatus("No Response");
                            winner.setPolloption(null);
                        }
                        pollOpinion.setResult(winner);

                        Log.info("After poll is expired");
                        Log.info("Poll Options Details :: " + gson.toJson(pollOpinion));
                        Log.info("Message Type :: " + messageType);
                        Log.info("From JID :: " + fromJID);
                        Log.info("To JID :: " + toJID);

                        // Create new message for poll expire.
                        Message message = new Message();
                        MUCRoom chatRoom = null;

                        message.setBody("Poll has been declared");
                        message.setSubject("19");
                        message.setID(pollOpinion.getMessageId());

                        String pollStatus = pollOpinion.getResult().getStatus();
                        Log.info("Poll Status :: " + pollStatus);

                        String pollOptionName = "";

                        if (pollStatus.equals("win")) {
                            pollOptionName = pollOpinion.getResult().getPolloption().getOptionName();
                        } else if (pollStatus.equals("Tie")) {

                            for (PollOptions pollOption : pollOptions) {
                                pollOptionName += pollOption.getOptionName() + " & ";
                            }

                            pollOptionName = pollOptionName.substring(0, pollOptionName.length() - 3);
                        }

                        Log.info("Poll Option Name :: " + pollOptionName);

                        Element e = message.addChildElement("pollResult", "urn:xmpp:pollResult");
                        e.addElement("pollId").setText(pollOpinion.getPollId());
                        e.addElement("status").setText(pollOpinion.getResult().getStatus());
                        e.addElement("winnerOption").setText(pollOptionName.equals("") ? "null" : pollOptionName);

                        Element messageTime = message.addChildElement("messageTime", "urn:xmpp:time");
                        messageTime.addElement("time").setText(String.valueOf(pollOpinion.getCreatedOn()));

                        if (messageType.equals(Message.Type.chat.name())) {

                            Log.info("Poll Expire message for one to one chat.");

                            sendMessage(message, new JID(fromJID), new JID(toJID));

                            sendMessage(message, new JID(toJID), new JID(fromJID));

                        } else {

                            MultiUserChatService multiUserChatService = XMPPServer.getInstance()
                                    .getMultiUserChatManager().getMultiUserChatService("conference");
                            chatRoom = multiUserChatService.getChatRoom(roomOriginalName);

                            if (chatRoom == null) {
                                Log.info("Chat Room is null");
                                return;
                            } else {
                                Log.info("Chat Room is available");
                            }

                            Log.info("Poll Expire message for group chat.");
                            message.setFrom(chatRoom.getJID());
                            message.setTo(chatRoom.getJID());
                            message.setType(Message.Type.groupchat);

                            Log.info("Broadcast Message for group chat." + message.toXML());

                            chatRoom.serverScheduleBroadcast(message, pollOpinion.getCreatedBy());

                            Log.info("After poll expire for group chat.");
                        }
                    }
                    pstmt.close();
                    conn.close();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                DbConnectionManager.closeConnection(rs, pstmt, conn);
            }

        } catch (PSQLException ex) {
            ex.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            DbConnectionManager.closeConnection(pstmt, conn);
        }
    }

    private void sendMessage(Message message, JID fromJID, JID toJID) {

        message.setFrom(fromJID + "/chat");
        message.setTo(toJID);
        message.setType(Message.Type.chat);

        Log.info("Broadcast Message for one to one chat." + message.toXML());

        XMPPServer.getInstance().getPacketRouter().route(message);

        Log.info("After poll expire for one to one chat.");
    }

}
