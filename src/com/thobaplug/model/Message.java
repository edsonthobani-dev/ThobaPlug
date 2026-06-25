/**
 * 
 */
package com.thobaplug.model;

import java.time.LocalDateTime;

/**
 * 
 */
public class Message {

	private int  message_id ;
	private int sender_id  ;
	private int recipient_id;
	private String content    ;
	private LocalDateTime sent_at  ;
	private boolean  is_private  ;
	private String senderUsername;
	/**
	 * @param message_id
	 * @param sender_id
	 * @param recipient_id
	 * @param content
	 * @param sent_at
	 * @param is_private
	 * @param senderUsername
	 */
	public Message( int sender_id, int recipient_id, String content,
			boolean is_private) {
		
		this.sender_id = sender_id;
		this.recipient_id = recipient_id;
		this.content = content;
		this.sent_at = LocalDateTime.now();
		this.is_private = is_private;
		
	}

	
	/**
	 * @return the message_id
	 */
	public int getMessage_id() {
		return message_id;
	}
	/**
	 * @param message_id the message_id to set
	 */
	public void setMessage_id(int message_id) {
		this.message_id = message_id;
	}
	/**
	 * @return the sender_id
	 */
	public int getSender_id() {
		return sender_id;
	}
	/**
	 * @param sender_id the sender_id to set
	 */
	public void setSender_id(int sender_id) {
		this.sender_id = sender_id;
	}
	/**
	 * @return the recipient_id
	 */
	public int getRecipient_id() {
		return recipient_id;
	}
	/**
	 * @param recipient_id the recipient_id to set
	 */
	public void setRecipient_id(int recipient_id) {
		this.recipient_id = recipient_id;
	}
	/**
	 * @return the content
	 */
	public String getContent() {
		return content;
	}
	/**
	 * @param content the content to set
	 */
	public void setContent(String content) {
		this.content = content;
	}
	/**
	 * @return the sent_at
	 */
	public LocalDateTime getSent_at() {
		return sent_at;
	}
	/**
	 * @param sent_at the sent_at to set
	 */
	public void setSent_at(LocalDateTime sent_at) {
		this.sent_at = sent_at;
	}
	/**
	 * @return the is_private
	 */
	public boolean isIs_private() {
		return is_private;
	}
	/**
	 * @param is_private the is_private to set
	 */
	public void setIs_private(boolean is_private) {
		this.is_private = is_private;
	}
	/**
	 * @return the senderUsername
	 */
	public String getSenderUsername() {
		return senderUsername;
	}
	/**
	 * @param senderUsername the senderUsername to set
	 */
	public void setSenderUsername(String senderUsername) {
		this.senderUsername = senderUsername;
	}
	
	@Override
    public String toString() {
        return "[" + sent_at + "] " + senderUsername + ": " + content;
    }
	
}
