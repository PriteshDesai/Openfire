package org.jivesoftware.openfire.custom.dto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.jivesoftware.database.DbConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomPollOptionControler {

	private static final Logger Log = LoggerFactory.getLogger(CustomPollOptionControler.class);

	private static final StringBuffer SQL_INSERT_OPINION_POLL = new StringBuffer(
			"INSERT INTO ofpollmaster(pollid,createdby,createdat,expiredat,timezone,question,ofroomid,messageid,messagetype,fromjid,tojid)")
					.append(" values(?,?,?,?,?,?,?,?,?,?,?)");

	private static final StringBuffer SQL_INSERT_OPINION_OPTION = new StringBuffer(
			"INSERT INTO ofpolloptions(pollid,optionname)").append(" values(?,?)");

	private static final StringBuffer SQL_INSERT_OPINION_UPDATE_RESPONSE = new StringBuffer(
			"INSERT INTO ofpolluserresponse(username,polloptionid,pollid) values(?,?,?)")
					.append(" ON CONFLICT (username,pollid) DO UPDATE")
					.append(" SET polloptionid=excluded.polloptionid");

	private static final StringBuffer SQL_GET_POLL_OPINION_OPTION_ID = new StringBuffer(
			"SELECT polloptionid from ofpolloptions WHERE pollid = ? and optionname = ?");

	private static final StringBuffer SQL_DELETE_OPINION_POLL_RESPONSE = new StringBuffer(
			"DELETE FROM ofpolluserresponse WHERE username = ? AND pollid = ?");

	public boolean addCustomOpinonPoll(OpinionPoll opinionPoll, Long roomId, String messageId, String messageType,
			String fromJID, String toJID) {

		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = DbConnectionManager.getConnection();
			conn.setAutoCommit(false);
			pstmt = conn.prepareStatement(SQL_INSERT_OPINION_POLL.toString());
			pstmt.setString(1, opinionPoll.getPollId());
			pstmt.setString(2, opinionPoll.getPollCreatedBy());
			pstmt.setLong(3, System.currentTimeMillis());
			pstmt.setLong(4, opinionPoll.getExpireDate());
			pstmt.setString(5, opinionPoll.getTimeZone());
			pstmt.setString(6, opinionPoll.getQuestion());
			pstmt.setLong(7, roomId);
			pstmt.setString(8, messageId);
			pstmt.setString(9, messageType);
			pstmt.setString(10, fromJID);
			pstmt.setString(11, toJID);

			pstmt.execute();
			conn.setAutoCommit(false);
			pstmt = conn.prepareStatement(SQL_INSERT_OPINION_OPTION.toString());
			for (String option : opinionPoll.getOptions().getOptionName()) {
				pstmt.setString(1, opinionPoll.getPollId());
				pstmt.setString(2, option);
				pstmt.addBatch();
			}
			pstmt.executeBatch();
			conn.commit();
			pstmt.close();
			conn.close();
		} catch (SQLException e) {
			Log.error("Fail to insert opinion poll :" + e.getLocalizedMessage());
			e.printStackTrace();
		} finally {
			DbConnectionManager.closeConnection(pstmt, conn);
		}

		return true;
	}

	public boolean updateOpinionPollResponse(OpinionPollUpdate opinionPollUpdate) {

		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;

		try {
			conn = DbConnectionManager.getConnection();
			stmt = conn.prepareStatement(SQL_GET_POLL_OPINION_OPTION_ID.toString());
			stmt.setString(1, opinionPollUpdate.getPollId());
			stmt.setString(2, opinionPollUpdate.getOpinionResponse().getOptionName());

			rs = stmt.executeQuery();

			if (rs != null && rs.next()) {
				int polloptionId = rs.getInt(1);

				stmt = conn.prepareStatement(SQL_INSERT_OPINION_UPDATE_RESPONSE.toString());
				stmt.setString(1, opinionPollUpdate.getOpinionResponse().getOptionAttendee());
				stmt.setInt(2, polloptionId);
				stmt.setString(3, opinionPollUpdate.getPollId());

				stmt.execute();
			}
			stmt.close();
			conn.close();
		} catch (SQLException ex) {
			ex.printStackTrace();
			Log.error("Fail to save opinion poll response.");
		} finally {
			DbConnectionManager.closeConnection(rs, stmt, conn);
		}

		return true;
	}

	public boolean deleteOpinionPollResponse(OpinionPollUpdate opinionPollUpdate) {

		Connection conn = null;
		PreparedStatement pstmt = null;

		try {

			conn = DbConnectionManager.getConnection();
			pstmt = conn.prepareStatement(SQL_DELETE_OPINION_POLL_RESPONSE.toString());
			pstmt.setString(1, opinionPollUpdate.getOpinionResponse().getOptionAttendee());
			pstmt.setString(2, opinionPollUpdate.getPollId());
			pstmt.executeUpdate();

			pstmt.close();
			conn.close();

		} catch (SQLException ex) {
			ex.printStackTrace();
			Log.error("Fail to Delete opinion poll response.");
		} finally {
			DbConnectionManager.closeConnection(pstmt, conn);
		}
		return true;
	}
}