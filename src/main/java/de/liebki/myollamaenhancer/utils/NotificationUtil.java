package de.liebki.myollamaenhancer.utils;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;

public enum NotificationUtil {
    ;
    public static final String GROUP_ID = "MyOllamaEnhancer Notifications";

    public static void info(Project project, String content) {
        notify(project, content, NotificationType.INFORMATION);
    }

    public static void warn(Project project, String content) {
        notify(project, content, NotificationType.WARNING);
    }

    public static void error(Project project, String content) {
        notify(project, content, NotificationType.ERROR);
    }

    public static void notify(Project project, String content, NotificationType type) {
        Notification notification = NotificationGroupManager.getInstance()
                .getNotificationGroup(GROUP_ID)
                .createNotification(content, type);
        notification.notify(project);
    }
}
