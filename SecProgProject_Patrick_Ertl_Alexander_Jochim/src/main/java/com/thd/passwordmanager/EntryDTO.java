package com.thd.passwordmanager;

public class EntryDTO {
	private String platform;
	private String username;
	private String password;
	private int pwid;

	/**
	 * @param pwid
	 * @param platform
	 * @param username
	 * @param password
	 */
	public EntryDTO(int pwid, String platform, String username, String password) {
		this.pwid = pwid;
		this.platform = platform;
		this.username = username;
		this.password = password;
	}

	public record Person(int pwid, String platform, String username, String password) {
	}

	/**
	 * @return int
	 */
	public int getPwid() {
		return pwid;
	}

	/**
	 * @return String
	 */
	public String getPlatform() {
		return platform;
	}

	/**
	 * @return String
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @return String
	 */
	public String getPassword() {
		return password;
	}

}
