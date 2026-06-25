/**
 * 
 */
package com.thobaplug.client;

import com.google.gson.JsonObject;

/**
 * 
 */
public interface IMessageListener {
	void onMessageReceived(JsonObject message);
    void onDisconnected();
}
