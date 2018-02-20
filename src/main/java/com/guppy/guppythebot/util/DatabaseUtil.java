package com.guppy.guppythebot.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

public class DatabaseUtil
{
	private static Connection conn;
	
	public DatabaseUtil()
	{
		try {
			conn = connect();
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	private static Connection connect() throws SQLException {
		return DriverManager.getConnection("jdbc:sqlite:data/guppy.db");
	}
	
	public void insertNewUser(Long discordId, String displayName, Boolean isAdmin, String friendlyName) throws SQLException {
		String sql = "INSERT INTO BOT_USER(DISCORD_ID,DISPLAY_NAME,FRIENDLY_NAME,IS_ADMIN) VALUES(?,?,?,?)";
		PreparedStatement pstmt = conn.prepareStatement(sql);
		
		pstmt.setFloat(1, discordId);
		pstmt.setString(2, displayName);
		pstmt.setString(3, friendlyName);
		pstmt.setString(4, isAdmin.toString());
		
		pstmt.executeUpdate();
	}
	
	public boolean userExists(Long discordId) {
		try {
			String sql = "SELECT * FROM BOT_USER WHERE DISCORD_ID = ?";
			PreparedStatement pstmt = conn.prepareStatement(sql);
			pstmt.setFloat(1, discordId);
			ResultSet rs = pstmt.executeQuery();
			
			return rs.next();
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean isAdmin(Long discordId) {
		try {
			String sql = "SELECT IS_ADMIN FROM BOT_USER WHERE DISCORD_ID = ?";
			PreparedStatement pstmt = conn.prepareStatement(sql);
			pstmt.setFloat(1, discordId);
			ResultSet rs = pstmt.executeQuery();
			
			return Boolean.parseBoolean(rs.getString("IS_ADMIN"));
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public int getUserReputaionById(Long discordId) {
		String sql = "SELECT REPUTATION FROM BOT_USER WHERE DISCORD_ID = ?";
		try {
			PreparedStatement pstmt = conn.prepareStatement(sql);
			pstmt.setFloat(1, discordId);
			
			ResultSet rs = pstmt.executeQuery();
			
			if (rs.next()) {
				return rs.getInt("REPUTATION");
			}
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
		
		return 0;
	}
	
	public Map<Integer, String> getAllRep() {
		String sql = "SELECT DISPLAY_NAME, REPUTATION FROM BOT_USER";
		Map<Integer, String> repMap = new HashMap<Integer, String>();
		try {
			PreparedStatement pstmt = conn.prepareStatement(sql);
			
			ResultSet rs = pstmt.executeQuery();
			
			while (rs.next()) {
				String dispName = rs.getString("DISPLAY_NAME");
				Integer reputation = rs.getInt("REPUTATION");
				repMap.put(reputation, dispName);
			}
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
		Map<Integer, String> treeMap = new TreeMap<Integer, String>(repMap);
		return treeMap;
	}
	
	public void userPlusRep(Long discordId) {
		String sql = "UPDATE BOT_USER SET REPUTATION = REPUTATION + 1 WHERE DISCORD_ID = ?";
		try {
			PreparedStatement pstmt = conn.prepareStatement(sql);
			
			pstmt.setFloat(1, discordId);
			
			pstmt.executeUpdate();
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void userMinusRep(Long discordId) {
		String sql = "UPDATE BOT_USER SET REPUTATION = REPUTATION - 1 WHERE DISCORD_ID = ?";
		try {
			PreparedStatement pstmt = conn.prepareStatement(sql);
			
			pstmt.setFloat(1, discordId);
			
			pstmt.executeUpdate();
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void updateUserDisplayName(Long discordId, String userName) {
		String sql = "UPDATE BOT_USER SET DISPLAY_NAME = ? WHERE DISCORD_ID = ?";
		try {
			PreparedStatement pstmt = conn.prepareStatement(sql);
			
			pstmt.setString(1, userName);
			pstmt.setFloat(2, discordId);
			
			pstmt.executeUpdate();
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public String getUserDisplayName(Long discordId) {
		String sql = "SELECT DISCORD_ID, DISPLAY_NAME FROM BOT_USER ORDER BY REPUTATION";
		try {
			PreparedStatement pstmt = conn.prepareStatement(sql);
			ResultSet rs = pstmt.executeQuery();
			
			while (rs.next()) {
				return rs.getString("DISPLAY_NAME");
			}
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
		
		return "";
	}
	
	public void tryUpdateDisplayName(Long discordId, String userName) {
		try {
			if (userExists(discordId)) {
				String name = getUserDisplayName(discordId);
				if (!name.equals(userName)) {
					updateUserDisplayName(discordId, userName);
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public Properties lookupProperties() {
		Properties prop = new Properties();
		String sql = "SELECT * FROM BOT_PROPERTIES";
		
		try {
			PreparedStatement pstmt = conn.prepareStatement(sql);
			ResultSet rs = pstmt.executeQuery();
			
			while (rs.next()) {
				String key = rs.getString("KEY");
				String value = rs.getString("VALUE");
				prop.setProperty(key, value);
			}
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
		
		return prop;
	}
}
