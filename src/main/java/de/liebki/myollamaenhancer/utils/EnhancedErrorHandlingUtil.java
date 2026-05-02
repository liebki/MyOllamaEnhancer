package de.liebki.myollamaenhancer.utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;
import java.util.concurrent.CompletionException;

/**
 * Enhanced error handling utility with better user notifications and logging.
 * Centralizes error handling logic across the plugin.
 */
public enum EnhancedErrorHandlingUtil {
    ;

    /**
     * Handles common Ollama API exceptions with user-friendly messages.
     *
     * @param project The current project (can be null)
     * @param ex The exception that occurred
     * @param operationDescription Description of the operation that failed
     * @param onUserNotification Optional callback when user is notified
     */
    public static void handleOllamaException(@Nullable final Project project, final Exception ex, final String operationDescription, @Nullable final Runnable onUserNotification) {
        final String userMessage;
        String logMessage = "[Ollama Error] " + operationDescription + ": " + ex.getClass().getSimpleName();

        if (ex instanceof IOException) {
            userMessage = "Network error during " + operationDescription + ". Check if Ollama is running and accessible.";
            logMessage += " - Network issue: " + ex.getMessage();
        } else if (ex instanceof InterruptedException) {
            userMessage = "Operation was interrupted during " + operationDescription + ".";
            logMessage += " - Interrupted";
        } else if (ex instanceof CompletionException && null != ex.getCause()) {
            // Handle wrapped exceptions
            if (ex.getCause() instanceof Exception causeException) {
                EnhancedErrorHandlingUtil.handleOllamaException(project, causeException, operationDescription, onUserNotification);
            } else {
                userMessage = "Unexpected error during " + operationDescription + ": " + ex.getCause().getMessage();
                logMessage += " - Unexpected: " + ex.getCause().getMessage();

                System.err.println(logMessage);
                ex.printStackTrace();

                if (null != onUserNotification) {
                    onUserNotification.run();
                }

                showErrorDialog(project, "Ollama Error", userMessage);
            }
            return;
        } else {
            userMessage = "Ollama API error during " + operationDescription + ": " + ex.getMessage();
            logMessage += " - " + ex.getMessage();
        }

        // Log the error
        System.err.println(logMessage);
        ex.printStackTrace();

        // Notify user
        if (null != onUserNotification) {
            onUserNotification.run();
        }

        // Show user-friendly notification
        showErrorDialog(project, "Ollama Error", userMessage);
    }

    /**
     * Shows an error dialog with proper threading handling.
     *
     * @param project The current project (can be null)
     * @param title Dialog title
     * @param message Error message
     */
    public static void showErrorDialog(@Nullable final Project project, final String title, final String message) {
        showDialogOnEDT(() ->
                Messages.showErrorDialog(project, message, title));
    }

    private static void showDialogOnEDT(final Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            ApplicationManager.getApplication().invokeLater(r);
        }
    }

    /**
     * Functional interface for suppliers that can throw exceptions.
     */
    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    /**
     * Functional interface for runnables that can throw exceptions.
     */
    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }
}
