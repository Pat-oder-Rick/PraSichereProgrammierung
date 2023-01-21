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
	private static final String database = "pwmanager";
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
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	// disconnect from database
	/**
	 * 
	 */
	public static void disconnect() {
		if (isConnected()) {
			try {
				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	// creates an account
	/**
	 * @param username
	 * @param password
	 * @return true/false
	 * @throws UnsupportedEncodingException
	 */
	public static boolean createAccount(String username, char[] password) throws UnsupportedEncodingException {
		try {
			getNextID();
			PreparedStatement createAccount = con
					.prepareStatement("INSERT INTO users (userid, username, password, iv)\r\n" + "VALUES (?, ?, ?, ?)");
			int nextID = getNextID();
			createAccount.setInt(1, nextID);
			createAccount.setString(2, username);
			createAccount.setString(3,
					BCrypt.hashpw(String.valueOf(password), BCrypt.gensalt(12) + DBController.getPepper()));
			byte[] iv = getRandomIV();
			createAccount.setBytes(4, iv);
			createAccount.execute();
			PreparedStatement generateSalt = con.prepareStatement("INSERT INTO salts (userid, salt) VALUES (?, ?)");
			generateSalt.setInt(1, nextID);
			generateSalt.setBytes(2, Bytes.random(16).array());
			generateSalt.execute();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(0);
		}
		return false;
	}

	// login function
	/**
	 * @param username
	 * @param password
	 * @return true/false
	 */
	public static boolean login(String username, char[] password) {
		try {
			PreparedStatement getPassword = con.prepareStatement("SELECT password FROM users WHERE username=?");
			getPassword.setString(1, username);
			ResultSet result = getPassword.executeQuery();
			if (result.next() == false) {
				return false;
			}
			if (BCrypt.checkpw(String.valueOf(password), result.getString("password"))) {
				PreparedStatement getCurrentID = con.prepareStatement("SELECT userid FROM users WHERE username=?");
				getCurrentID.setString(1, username);
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
	/**
	 * @param platform
	 * @param userid
	 * @param username
	 * @param password
	 * @return true/false
	 */
	public static boolean addEntry(String platform, int userid, String username, char[] password) {
		try {
			PreparedStatement ps2 = con.prepareStatement("INSERT INTO entrys (pwid, userid, platform, "
					+ "username, password)\r\n" + "VALUES (?, ?, ?, ?, ?);");
			ps2.setInt(1, getNextPasswordID());
			ps2.setInt(2, userid);
			ps2.setString(3, platform);
			ps2.setString(4, username);
			ps2.setBytes(5, encryptPassword(password));
			ps2.execute();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * @param pwid
	 */
	public static void deleteEntry(int pwid) {
		try {
			PreparedStatement ps = con.prepareStatement("DELETE FROM entrys WHERE pwid=?");
			ps.setInt(1, pwid);
			ps.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return List of EntryDTOs
	 */
	public static List<EntryDTO> getEntryset() {
		try {
			PreparedStatement ps = con
					.prepareStatement("SELECT pwid, platform, username, " + "password FROM entrys where userid=?");
			ps.setInt(1, currentUserID);
			ResultSet rs = ps.executeQuery();
			List<EntryDTO> list = new ArrayList<EntryDTO>();
			while (rs.next() == true) {
				byte[] password2 = rs.getBytes("password");
				String plaintext = String.valueOf(decryptPassword(password2));
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
	/**
	 * @return true/false
	 */
	public static boolean getState() {
		return loggedin;
	}

	// sets logged in state
	/**
	 * @param newState
	 */
	public static void setState(boolean newState) {
		loggedin = newState;
	}

	// return username of logged in user
	/**
	 * @return String
	 */
	public static String getUsername() {
		return currentUsername;
	}

	// encrypt String
	/**
	 * @param password
	 * @return Byte Array
	 */
	public static byte[] encryptPassword(char[] password) {
		try {
			PreparedStatement getPassword = con.prepareStatement("SELECT password " + "FROM users WHERE username=?");
			getPassword.setString(1, currentUsername);
			ResultSet result = getPassword.executeQuery();
			result.next();
			SecretKey AESKey = keyDerivation(result.getString("password").toCharArray());
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			IvParameterSpec iv = new IvParameterSpec(getIV());
			cipher.init(Cipher.ENCRYPT_MODE, AESKey, iv);
			byte[] encrypted = cipher.doFinal(String.valueOf(password).getBytes(StandardCharsets.UTF_8));
			return encrypted;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	// decrypt String
	/**
	 * @param password
	 * @return char Array
	 */
	public static char[] decryptPassword(byte[] password) {
		try {
			PreparedStatement getPassword = con.prepareStatement("SELECT password FROM users WHERE username=?");
			getPassword.setString(1, currentUsername);
			ResultSet result = getPassword.executeQuery();
			result.next();
			char[] pw = result.getString("password").toCharArray();
			SecretKey AESKey = keyDerivation(pw);
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
			cipher.init(Cipher.DECRYPT_MODE, AESKey, new IvParameterSpec(getIV()));
			byte[] original = cipher.doFinal(password);
			return new String(original).toCharArray();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	// generates random Initialisation Vector for AES Encryption in CBC mode
	/**
	 * @return Byte Array
	 */
	public static byte[] getRandomIV() {
		byte[] nonce = new byte[16];
		new SecureRandom().nextBytes(nonce);
		return nonce;
	}

	/**
	 * @return Byte Array
	 */
	public static byte[] getIV() {
		PreparedStatement getIV;
		try {
			getIV = con.prepareStatement("SELECT iv FROM users WHERE username=?");
			getIV.setString(1, currentUsername);
			ResultSet result = getIV.executeQuery();
			result.next();
			byte[] iv = result.getBytes(1);
			return iv;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	// generates secret Key
	/**
	 * @param secret
	 * @return SecretKey
	 */
	public static SecretKey keyDerivation(char[] secret) {
		char[] pw = "secret".toCharArray();
		byte[] pepper = Bytes.random(16).array();
		int costFactor = 5;
		KeyDerivationFunction kdf = new KeyDerivationFunction.Default(Version.HKDF_HMAC512);
		byte[] aesKey = kdf.derive(getUserSalt(), pw, costFactor, Bytes.from("aes-key").array(), 16);
		String s = Base64.getEncoder().encodeToString(aesKey);
		SecretKey aesSecretKey = new SecretKeySpec(aesKey, "AES");
		return aesSecretKey;
	}

	// generates Salt
	/**
	 * @return Byte Array
	 */
	public static byte[] getUserSalt() {
		try {
			PreparedStatement getSalt = con.prepareStatement("SELECT" + " salt FROM salts WHERE userid=?");
			getSalt.setInt(1, currentUserID);
			ResultSet rs = getSalt.executeQuery();
			rs.next();
			return rs.getBytes("salt");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	// gets next ID for account creation
	/**
	 * @return int
	 */
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

	/**
	 * @return int
	 */
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

	/**
	 * @return String
	 */
	public static String getPepper() {
		try {
			PreparedStatement ps = con.prepareStatement("SELECT pepper FROM pepper");
			ResultSet rs = ps.executeQuery();
			rs.next();
			String pepper = rs.getString("pepper");
			return pepper;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @return String
	 */
	public static String getErrorMessage() {
		return lastErrorMessage;
	}

	/**
	 * @param errorMessage
	 */
	public static void setErrorMessage(String errorMessage) {
		lastErrorMessage = errorMessage;
	}

	/**
	 * @param username
	 * @return true/false
	 */
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