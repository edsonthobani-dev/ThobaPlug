/**
 * 
 */
package com.thobaplug.util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {

    private static final String LOG_FILE = "server.log";
    private static final DateTimeFormatter FORMAT = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static Logger instance;
    private PrintWriter fileWriter;

    private Logger() {
        try {
            fileWriter = new PrintWriter(new FileWriter(LOG_FILE, true));
            log("INFO", "****************************************");
            log("INFO", "ThobaPlug Server Started");
            log("INFO", "****************************************");
        } catch (IOException e) {
            System.out.println("Logger failed to initialise: " + e.getMessage());
        }
    }

    public static Logger getInstance() {
        if (instance == null) {
            instance = new Logger();
        }
        return instance;
    }

    public void info(String message) {
        log("INFO", message);
    }

    public void warning(String message) {
        log("WARNING", message);
    }

    public void error(String message) {
        log("ERROR", message);
    }

    public void error(String message, Exception e) {
        log("ERROR", message + " — " + e.getMessage());
    }

    private void log(String level, String message) {
        String entry = "[" + LocalDateTime.now().format(FORMAT) + "] "
                     + "[" + level + "] " + message;
        System.out.println(entry);
        if (fileWriter != null) {
            fileWriter.println(entry);
            fileWriter.flush();
        }
    }

    public void close() {
        if (fileWriter != null) {
            log("INFO", "ThobaPlug Server Stopped");
            fileWriter.close();
        }
    }
}