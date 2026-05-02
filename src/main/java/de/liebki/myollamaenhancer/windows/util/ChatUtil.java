package de.liebki.myollamaenhancer.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public enum ChatUtil {
    ;
    
    private static final Gson gson = new GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create();

    public static class Message implements Serializable {
        private static final long serialVersionUID = 1L;
        
        @com.google.gson.annotations.SerializedName("sender")
        public String sender;
        
        @com.google.gson.annotations.SerializedName("message")
        public String message;
        
        // Default constructor for Gson
        public Message() {
        }
        
        public Message(final String sender, final String message) {
            this.sender = sender;
            this.message = message;
        }
    }
    

    public static void saveChatHistoryToFile(final List<Message> chatMessages, final Path chatFilePath) {
        try {
            // Create backup of existing file if it exists
            if (Files.exists(chatFilePath)) {
                final Path backupPath = chatFilePath.resolveSibling(chatFilePath.getFileName() + ".backup");
                Files.copy(chatFilePath, backupPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            
            try (final Writer writer = Files.newBufferedWriter(chatFilePath, StandardCharsets.UTF_8)) {
                ChatUtil.gson.toJson(chatMessages, writer);
            }
        } catch (final IOException e) {
            System.err.println("Error saving chat history: " + e.getMessage());
            // Try to restore from backup if save failed
            try {
                final Path backupPath = chatFilePath.resolveSibling(chatFilePath.getFileName() + ".backup");
                if (Files.exists(backupPath)) {
                    Files.copy(backupPath, chatFilePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (final IOException ignored) {}
        }
    }

    @SuppressWarnings("unchecked")
    public static void loadChatHistoryFromFile(final List<Message> chatMessages, final Path chatFilePath) {
        if (!Files.exists(chatFilePath)) {
            ChatUtil.handleMissingFile(chatFilePath);
            return;
        }

        // First try with Gson JSON deserialization
        try (final Reader reader = Files.newBufferedReader(chatFilePath, StandardCharsets.UTF_8)) {
            final List<Message> loadedMessages = ChatUtil.gson.fromJson(reader, new TypeToken<List<Message>>() {}.getType());
            if (null != loadedMessages) {
                chatMessages.clear();
                chatMessages.addAll(loadedMessages);
            }
        } catch (final Exception e) {
            System.err.println("Unexpected error loading chat history: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Handles the case when the chat history file does not exist.
     */
    private static void handleMissingFile(final Path chatFilePath) {
        System.out.println("Chat history file does not exist: " + chatFilePath);
    }
    
    
    /**
     * Handles IOException when loading chat history.
     */
    private static void handleIOException(final List<Message> chatMessages, final Path chatFilePath, final IOException e) {
        System.err.println("Error loading chat history: " + e.getMessage());
        
        // If loading fails, try to recover by reading the file as plain text
        try {
            final String content = Files.readString(chatFilePath, StandardCharsets.UTF_8);
            System.err.println("Chat history file content: " + content);
            ChatUtil.repairChatHistory(chatMessages, chatFilePath, content);
        } catch (final IOException ignored) {
            System.err.println("Failed to read chat history file for repair");
        }
    }
    
    /**
     * Attempts to repair a corrupted chat history file by extracting messages using basic string operations.
     * @param chatMessages The list to add repaired messages to
     * @param chatFilePath Path to the chat history file
     * @param content The content of the chat history file
     */
    private static void repairChatHistory(final List<Message> chatMessages, final Path chatFilePath, final String content) {
        try {
            // Try to extract messages using a more lenient approach
            final String[] lines = content.split("\n");
            for (final String line : lines) {
                if (line.contains("\"sender\"") && line.contains("\"message\"")) {
                    // Try to extract sender and message from this line
                    final int senderStart = line.indexOf("\"sender\":\"") + 10;
                    final int senderEnd = line.indexOf('"', senderStart);
                    final int messageStart = line.indexOf("\"message\":\"") + 11;
                    final int messageEnd = line.lastIndexOf('"');
                    
                    if (9 < senderStart && senderEnd > senderStart && 10 < messageStart && messageEnd > messageStart) {
                        final String sender = line.substring(senderStart, senderEnd);
                        final String message = line.substring(messageStart, messageEnd);
                        chatMessages.add(new Message(sender, message));
                    }
                }
            }
            
            // If we found some messages, save the repaired version
            if (!chatMessages.isEmpty()) {
                System.err.println("Repaired " + chatMessages.size() + " messages from corrupted chat history");
                ChatUtil.saveChatHistoryToFile(chatMessages, chatFilePath);
            }
        } catch (final Exception e) {
            System.err.println("Failed to repair chat history: " + e.getMessage());
        }
    }

    public static void exportChatAsJson(final List<Message> chatMessages, final java.awt.Component parent) {
        final javax.swing.JFileChooser fileChooser = new javax.swing.JFileChooser();
        fileChooser.setDialogTitle("Save Chat as JSON");

        final int userSelection = fileChooser.showSaveDialog(parent);
        if (javax.swing.JFileChooser.APPROVE_OPTION == userSelection) {

            final File fileToSave = fileChooser.getSelectedFile();
            try (final Writer writer = new OutputStreamWriter(new FileOutputStream(fileToSave), StandardCharsets.UTF_8)) {
                writer.write("[");

                for (int i = 0; i < chatMessages.size(); i++) {
                    final Message msg = chatMessages.get(i);
                    writer.write("{\"sender\":\"" + ChatUtil.escapeJson(msg.sender) + "\",\"message\":\"" + ChatUtil.escapeJson(msg.message) + "\"}");

                    if (i < chatMessages.size() - 1)
                        writer.write(",");
                }

                writer.write("]");
            } catch (final IOException ex) {
                // Optionally handle error
            }
        }
    }

    public static void clearChat(final List<Message> chatMessages, final Path chatFilePath) {
        chatMessages.clear();
        try {
            Files.deleteIfExists(chatFilePath);
        } catch (final IOException ignored) {
            // Optionally handle error
        }
    }

    public static String escapeHtml(final String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br/>");
    }

    public static String escapeJson(final String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public static String unescapeJson(final String text) {
        return text.replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
} 