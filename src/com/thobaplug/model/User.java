/**
 * 
 */
package com.thobaplug.model;

import java.time.LocalDateTime;

/**
 * 
 */
public class User {

	private int userr_id   ;
	private String username    ;
	private String password_hash ;
	private LocalDateTime created_at ;
	
	
	/**
	 * @param username username
	 * @param password_hash password_hash
	 */
	public User(String username, String password_hash) {
		this.username = username;
		this.password_hash = password_hash;
		this.created_at = LocalDateTime.now();
	}
	
	/**
	 * @return the userr_id
	 */
	public int getUserr_id() {
		return userr_id;
	}


	/**
	 * @param userr_id the userr_id to set
	 */
	public void setUserr_id(int userr_id) {
		this.userr_id = userr_id;
	}


	/**
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}


	/**
	 * @param username the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}


	/**
	 * @return the password_hash
	 */
	public String getPassword_hash() {
		return password_hash;
	}


	/**
	 * @param password_hash the password_hash to set
	 */
	public void setPassword_hash(String password_hash) {
		this.password_hash = password_hash;
	}


	/**
	 * @return the created_at
	 */
	public LocalDateTime getCreated_at() {
		return created_at;
	}


	/**
	 * @param created_at the created_at to set
	 */
	public void setCreated_at(LocalDateTime created_at) {
		this.created_at = created_at;
	}

	 @Override
	    public String toString() {
	        return "User[" + userr_id + "] " + username;
	    }

	
}
