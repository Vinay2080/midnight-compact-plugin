package dev.verloren.midnight.ui;

import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Utility for presenting notifications to the user using the configured Midnight notification group.
 */
public final class CompactNotificationUtil {

  private CompactNotificationUtil() {}

  /**
   * Posts an IDE notification under the Midnight Notifications group.
   *
   * @param project the target project
   * @param content the notification body
   * @param type    the notification severity type
   */
  public static void notifyUser(@NotNull Project project, @NotNull String content, @NotNull NotificationType type) {
    NotificationGroup group = NotificationGroupManager.getInstance().getNotificationGroup("Midnight Notifications");
    if (group != null) {
      group.createNotification(content, type).notify(project);
    }
  }
}
