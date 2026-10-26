package dev.verloren.midnight.toolwindow;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.ui.JBPopupMenu;
import dev.verloren.midnight.version.CompactVersionManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseEvent;

/**
 * Context menu handler for {@link CompactVersionCard}.
 */
public final class CompactVersionCardContextMenu {

  private CompactVersionCardContextMenu() {}

  /**
   * Constructs and displays the contextual popup menu at the mouse trigger coordinates.
   */
  public static void showContextMenu(
      @NotNull JComponent parent,
      @NotNull MouseEvent e,
      @NotNull String version,
      boolean isInstalled,
      @Nullable String installedPath,
      @Nullable Runnable onSelect,
      @Nullable Runnable onRemove
  ) {
    JBPopupMenu menu = new JBPopupMenu();
    if (isInstalled) {
      JMenuItem removeItem = new JMenuItem("Remove Version v" + version, AllIcons.Actions.GC);
      removeItem.addActionListener(_ -> {
        if (onRemove != null) {
          onRemove.run();
        }
      });
      menu.add(removeItem);

      if (installedPath != null && !installedPath.isEmpty()) {
        JMenuItem copyItem = new JMenuItem("Copy Executable Path", AllIcons.Actions.Copy);
        copyItem.addActionListener(_ -> CopyPasteManager.getInstance().setContents(new StringSelection(installedPath)));
        menu.add(copyItem);
      }
    } else {
      JMenuItem downloadItem = new JMenuItem("Download & Switch to v" + version, AllIcons.Actions.Download);
      downloadItem.addActionListener(_ -> {
        if (onSelect != null) {
          onSelect.run();
        }
      });
      menu.add(downloadItem);
    }

    JMenuItem copyUrlItem = new JMenuItem("Copy Asset Download URL", AllIcons.Actions.Copy);
    copyUrlItem.addActionListener(_ -> {
      String url = CompactVersionManager.getReleaseDownloadUrl(version);
      CopyPasteManager.getInstance().setContents(new StringSelection(url));
    });
    menu.add(copyUrlItem);

    menu.show(parent, e.getX(), e.getY());
  }
}
