package com.thd.passwordmanager;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.mindrot.jbcrypt.BCrypt;

import at.favre.lib.bytes.Bytes;
import at.favre.lib.crypto.bkdf.KeyDerivationFunction;
import at.favre.lib.crypto.bkdf.Version;

public class DBController {
	private static final String host = "localhost";
	private static final String port = "3306";
	private static final String database = "pwmanagerinsecure";
	private static String username = "root";
	private static final String password = "";
	private static boolean loggedin = false;
	public static Connection con;
	public static int currentUserID;
	private static String currentUsername;

	private static String lastErrorMessage;

	public static boolean isConnected() {
		return (con == null ? false : true);
	}

	// connect to database
	public static void connect() throws ClassNotFoundException {
		if (!isConnected()) {
			try {
				Class.forName("com.mysql.cj.jdbc.Driver");
				con = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database, username,
						password);
				System.out.println("[MySQL] verbunden");
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	// disconnect from database
	public static void disconnect() {
		if (isConnected()) {
			try {
				con.close();
				System.out.println("[MySQL] Verbindung geschlossen");
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	// creates an account
	public static boolean createAccount(String username, String password) throws UnsupportedEncodingException {
		
			getNextID();
			PreparedStatement createAccount;
			try {
				createAccount = con
						.prepareStatement("INSERT INTO users (userid, username, password)\r\n" + "VALUES ('" + getNextID() + "','" + username + "','" + password + "')");
			
			createAccount.execute();
			}catch (SQLException e) {
				e.printStackTrace();
			}
		return loggedin;
	}

	// login function
	public static boolean login(String username, String password) {
		try {
			PreparedStatement getPassword = con.prepareStatement("SELECT password FROM users WHERE username='"+ username + "'");
			ResultSet result = getPassword.executeQuery();
			if (result.next() == false) {
				return false;
			}
			
			String userpassword = result.getString("password");
			if (userpassword.equals(password)) {
				PreparedStatement getCurrentID = con.prepareStatement("SELECT userid FROM users WHERE username='" + username + "'");
				ResultSet id = getCurrentID.executeQuery();
				id.next();
				currentUserID = id.getInt("userid");
				currentUsername = username;
				setState(true);
				return true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(0);
		}
		return false;
	}

	// adds entry for logged in user
	public static boolean addEntry(String platform, int userid, String username, String password) {
		try {
			PreparedStatement ps2 = con.prepareStatement("INSERT INTO entrys (pwid, userid, platform, "
					+ "username, password)\r\n" + "VALUES ('" + getNextPasswordID() + "', '" + currentUserID + "', '" + platform + "', '" + username + "', '" + password + "');");
			ps2.execute();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public static void deleteEntry(int pwid) {
		try {
			PreparedStatement ps = con.prepareStatement("DELETE FROM entrys WHERE pwid='" + pwid +"'");
			ps.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static List<EntryDTO> getEntryset() {
		try {
			PreparedStatement ps = con
					.prepareStatement("SELECT pwid, platform, username, " + "password FROM entrys where userid='" + currentUserID + "'");
			ResultSet rs = ps.executeQuery();
			List<EntryDTO> list = new ArrayList<EntryDTO>();
			while (rs.next() == true) {
				String password = rs.getString("password");
				String plaintext = String.valueOf(password);
				list.add(
						new EntryDTO(rs.getInt("pwid"), rs.getString("platform"), rs.getString("username"), plaintext));
			}
			return list;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	// returns logged in/logged out
	public static boolean getState() {
		return loggedin;
	}

	// sets logged in state
	public static void setState(boolean newState) {
		loggedin = newState;
	}

	// return username of logged in user
	public static String getUsername() {
		return currentUsername;
	}

	// gets next ID for account creation
	public static int getNextID() {
		PreparedStatement getID;
		try {
			getID = con.prepareStatement("SELECT max(userid) as max_id FROM users");
			ResultSet result = getID.executeQuery();
			result.next();
			int id = result.getInt("max_id");
			return ++id;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}

	public static int getNextPasswordID() {
		PreparedStatement ps;
		try {
			ps = con.prepareStatement("SELECT max(pwid) as max_id FROM entrys");
			ResultSet rs = ps.executeQuery();
			rs.next();
			int id = rs.getInt("max_id");
			id++;
			return id;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}

	public static String getErrorMessage() {
		return lastErrorMessage;
	}

	public static void setErrorMessage(String errorMessage) {
		lastErrorMessage = errorMessage;
	}

	public static boolean checkDuplicate(String username) {
		PreparedStatement ps;
		try {

			ps = con.prepareStatement("SELECT * FROM users where username=?");
			ps.setString(1, username);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				return true;
			}
			return false;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return true;
	}
}