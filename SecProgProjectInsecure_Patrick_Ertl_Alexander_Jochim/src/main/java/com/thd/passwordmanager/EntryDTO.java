package com.thd.passwordmanager;

public class EntryDTO {
	private String platform;
	private String username;
	private String password;
	private int pwid;

	public EntryDTO(int pwid, String platform, String username, String password) {
		this.pwid = pwid;
		this.platform = platform;
		this.username = username;
		this.password = password;
	}

	public record Person(int pwid, String platform, String username, String password) {
	}

	public int getPwid() {
		return pwid;
	}

	public String getPlatform() {
		return platform;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

}
