package pt.unl.fct.di.adc.firstwebapp.util;

import java.util.UUID;

public class AuthToken {

	private String username;
	private String tokenID;
	private long creationData;
	private long expirationData;
	private String role;

	public static final long EXPIRATION_TIME = 1000 * 60 * 60 * 2; // 2h

	public AuthToken() {
	} // Nothing to be done here

	public AuthToken(String username, String role) {
		this.username = username;
		this.tokenID = UUID.randomUUID().toString();
		this.creationData = System.currentTimeMillis();
		this.expirationData = this.creationData + AuthToken.EXPIRATION_TIME;
		this.role = role;
	}
	
	public AuthToken(String username, String tokenID, long creationData, long expirationData, String role) {
		this.username = username;
		this.tokenID = tokenID;
		this.creationData = creationData;
		this.expirationData = expirationData;
		this.role = role;
	}
	
	public String getUsername() {
		return username;
	}
	
	public String getTokenID() {
		return tokenID;
	}
	
	public long getCreationData() {
		return creationData;
	}
	
	public long getExpirationData() {
		return expirationData;
	}
	
	public String getRole() {
		return role;
	}
}
