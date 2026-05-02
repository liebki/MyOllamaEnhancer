package de.liebki.myollamaenhancer.windows;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.ide.ui.LafManager;
import com.intellij.ide.ui.LafManagerListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import de.liebki.myollamaenhancer.models.CodeFile;
import de.liebki.myollamaenhancer.models.CodeSection;
import de.liebki.myollamaenhancer.util.LatexUnicodeUtility;
import de.liebki.myollamaenhancer.utils.DialogUtil;
import de.liebki.myollamaenhancer.utils.DuckDbService;
import de.liebki.myollamaenhancer.utils.NotificationUtil;
import de.liebki.myollamaenhancer.utils.OllamaAPIUtil;
import de.liebki.myollamaenhancer.util.ChatUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.beans.PropertyChangeListener;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import de.liebki.myollamaenhancer.events.KnowledgeBaseChangedTopic;


public class ChatToolWindow implements ToolWindowFactory, DumbAware {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Project project;
    private JEditorPane chatHistoryPane;
    private JTextField inputField;
    private JPanel mainPanel;
    private JPanel inputPanel;
    private JButton sendButton;
    private JButton trashButton;
    // Context selection UI
    private JPanel contextPanel;
    private JButton classesButton;
    private JButton methodsButton;
    private JButton refreshContextButton;
    private JPopupMenu classesMenu;
    private JPopupMenu methodsMenu;
    private JBPopup classesPopup;
    private JBPopup methodsPopup;
    private List<CodeFile> availableFiles = new ArrayList<>();
    private List<CodeSection> availableSections = new ArrayList<>();
    private final Set<Long> selectedClassIds = new HashSet<>();
    private final Set<Long> selectedMethodIds = new HashSet<>();
    private boolean isProcessing;
    private final StringBuilder currentAIResponse = new StringBuilder();
    private final List<ChatUtil.Message> chatMessages = new ArrayList<>();
    private Path chatFilePath;
    private PropertyChangeListener lafChangeListener;
    private Disposable lafBusDisposable;
    private JBScrollPane scrollPane;

    

    // Context size and chunking configuration
    private static final int CONTEXT_INLINE_BUDGET = 8000; // characters
    private static final int CHUNK_TARGET_CHARS = 3500;    // larger chunks for less lossiness
    private static final int CHUNK_SOFT_WINDOW = 300;      // extra window to find a soft break
    private static final int CHUNK_OVERLAP = 300;          // overlapping context to cover boundaries

    // Popup menu configuration (scroll when many items)
    private static final int MENU_MAX_VISIBLE_ITEMS = 12;
    private static final int MENU_PREFERRED_WIDTH = 600;
    private static final int MENU_PREFERRED_HEIGHT = 420;

        @Override
        public final void createToolWindowContent(Project project, ToolWindow toolWindow) {
        this.project = project;
        String basePath = project.getBasePath();
        if (null == basePath)
            basePath = System.getProperty("user.home");

        final Path chatDir = Paths.get(basePath, ".MyOllamaEnhancer");
        try { Files.createDirectories(chatDir); } catch (final IOException ignored) {}

        this.chatFilePath = chatDir.resolve("chat_history.json");
        this.mainPanel = new JPanel(new BorderLayout());

        this.chatHistoryPane = new JEditorPane();
        this.chatHistoryPane.setContentType("text/html");
        // Make the pane blend into the IDE theme background
        this.chatHistoryPane.setOpaque(true);
        this.chatHistoryPane.setBackground(UIUtil.getPanelBackground());

        this.chatHistoryPane.setEditable(false);
        this.chatHistoryPane.setBorder(JBUI.Borders.empty(10));

        this.scrollPane = new JBScrollPane(this.chatHistoryPane);
        this.scrollPane.setPreferredSize(new Dimension(400, 300));

        // Context selection panel (top)
        this.contextPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        this.classesButton = new JButton("Classes (0)");
        this.methodsButton = new JButton("Methods (0)");
        this.refreshContextButton = new JButton("Refresh");
        this.classesMenu = new JPopupMenu();
        this.methodsMenu = new JPopupMenu();
        this.classesButton.addActionListener(ev -> this.showClassesPopup());
        this.methodsButton.addActionListener(ev -> this.showMethodsPopup());
        this.refreshContextButton.addActionListener(ev -> this.refreshContextData());
        this.contextPanel.add(new JLabel("Context:"));
        this.contextPanel.add(this.classesButton);
        this.contextPanel.add(this.methodsButton);
        this.contextPanel.add(this.refreshContextButton);
        this.mainPanel.add(this.contextPanel, BorderLayout.NORTH);

        this.inputPanel = new JPanel(new BorderLayout(5, 5));
        this.inputField = new JTextField();

        this.sendButton = new JButton("Send");
        this.trashButton = new JButton("\uD83D\uDDD1");

        this.trashButton.setToolTipText("Clear or Save Chat");
        this.inputPanel.add(this.trashButton, BorderLayout.WEST);

        this.inputPanel.add(this.inputField, BorderLayout.CENTER);
        this.inputPanel.add(this.sendButton, BorderLayout.EAST);

        this.inputPanel.setBorder(JBUI.Borders.empty(5));
        this.mainPanel.add(this.scrollPane, BorderLayout.CENTER);

        this.mainPanel.add(this.inputPanel, BorderLayout.SOUTH);
        this.sendButton.addActionListener(e -> this.sendMessage(project));

        this.inputField.addActionListener(e -> this.sendMessage(project));
        this.trashButton.addActionListener(e -> this.handleTrashAction());

        toolWindow.getComponent().add(this.mainPanel);
        ChatUtil.loadChatHistoryFromFile(this.chatMessages, this.chatFilePath);

        this.updateChatHistoryPane();

        // Load available context and build menus
        this.refreshContextData();

        // Register theme listeners initially
        this.registerThemeListeners();

        // Re-register listeners when tool window becomes displayable again; unregister when hidden
        this.mainPanel.addHierarchyListener(e -> {
            if (0 != (e.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED)) {
                if (this.mainPanel.isDisplayable()) {
                    this.registerThemeListeners();
                    // Make sure we repaint in case theme changed while hidden
                    SwingUtilities.invokeLater(this::refreshForThemeChange);
                } else {
                    this.unregisterThemeListeners();
                }
            }
        });
    }

    private void registerThemeListeners() {
        try {
            if (null == lafChangeListener) {
                this.lafChangeListener = evt -> {
                    if ("lookAndFeel".equals(evt.getPropertyName())) {
                        SwingUtilities.invokeLater(this::refreshForThemeChange);
                    }
                };
                UIManager.addPropertyChangeListener(this.lafChangeListener);
            }
        } catch (final Throwable ignored) { }

        try {
            if (null == lafBusDisposable) {
                this.lafBusDisposable = Disposer.newDisposable("ChatToolWindowLafBus");
                ApplicationManager.getApplication().getMessageBus()
                    .connect(this.lafBusDisposable)
                    .subscribe(LafManagerListener.TOPIC, new LafManagerListener() {
                        @Override
                        public void lookAndFeelChanged(final LafManager source) {
                            SwingUtilities.invokeLater(ChatToolWindow.this::refreshForThemeChange);
                        }
                    });

                // Subscribe to knowledge base changes to auto-refresh context
                ApplicationManager.getApplication().getMessageBus()
                    .connect(this.lafBusDisposable)
                    .subscribe(KnowledgeBaseChangedTopic.TOPIC, project -> {
                        // Refresh only if the event is for this project (or project is null)
                        if (null == this.project || null == project || this.project.equals(project)) {
                            SwingUtilities.invokeLater(() -> {
                                this.refreshContextData();
                                // If a popup is open, keep counts correct (menus rebuilt above)
                                this.updateSelectionButtonLabels();
                            });
                        }
                    });
            }
        } catch (final Throwable ignored) { }
    }

    private void unregisterThemeListeners() {
        try {
            if (null != lafChangeListener) {
                UIManager.removePropertyChangeListener(this.lafChangeListener);
                this.lafChangeListener = null;
            }
        } catch (final Throwable ignored) { }
        try {
            if (null != lafBusDisposable) {
                Disposer.dispose(this.lafBusDisposable);
                this.lafBusDisposable = null;
            }
        } catch (final Throwable ignored) { }
    }

    // Ensures the UI tree updates to the new LAF and reapplies theme-aware colors/content
    private void refreshForThemeChange() {
        try {
            // Refresh UI defaults for all components in the toolwindow
            SwingUtilities.updateComponentTreeUI(this.mainPanel);

            // Apply background from current IDE LAF to relevant components
            final Color bg = UIUtil.getPanelBackground();
            if (null != mainPanel) {
                this.mainPanel.setOpaque(true);
                this.mainPanel.setBackground(bg);
            }
            if (null != inputPanel) {
                this.inputPanel.setOpaque(true);
                this.inputPanel.setBackground(bg);
            }
            if (null != contextPanel) {
                this.contextPanel.setOpaque(true);
                this.contextPanel.setBackground(bg);
            }
            if (null != scrollPane) {
                this.scrollPane.setOpaque(true);
                this.scrollPane.setBackground(bg);
                if (null != scrollPane.getViewport()) {
                    this.scrollPane.getViewport().setOpaque(true);
                    this.scrollPane.getViewport().setBackground(bg);
                }
            }
            if (null != chatHistoryPane) {
                this.chatHistoryPane.setOpaque(true);
                this.chatHistoryPane.setBackground(bg);
                // Force HTML kit/document reset to ensure CSS is reapplied when switching to dark
                try {
                    this.chatHistoryPane.setContentType("text/plain");
                    this.chatHistoryPane.setText("");
                    this.chatHistoryPane.setEditorKit(new javax.swing.text.html.HTMLEditorKit());
                    this.chatHistoryPane.setContentType("text/html");
                } catch (final Exception ignored) { }
            }

            // Rebuild themed HTML and repaint
            this.updateChatHistoryPane();
            this.mainPanel.revalidate();
            this.mainPanel.repaint();
        } catch (final Exception ignored) { }
    }

    // Context UI and data
    private void refreshContextData() {
        try {
            this.availableFiles = DuckDbService.getAllCodeFiles(this.project);
            this.availableSections = DuckDbService.getAllCodeSections(this.project);
        } catch (final Exception ex) {
            System.err.println("[ChatToolWindow] Failed to load context: " + ex.getMessage());
            this.availableFiles = new ArrayList<>();
            this.availableSections = new ArrayList<>();
        }
        this.rebuildClassesMenu();
        this.rebuildMethodsMenu();
        this.updateSelectionButtonLabels();
    }

    private void rebuildClassesMenu() {
        this.classesMenu.removeAll();
        final List<JCheckBoxMenuItem> items = new ArrayList<>();
        for (final CodeFile cf : this.availableFiles) {
            if (!cf.enabled) continue;
            final boolean sel = this.selectedClassIds.contains(cf.id);
            final String label = cf.fileName + (null != cf.folder && !cf.folder.isBlank() ? "  —  " + cf.folder : "");
            final JCheckBoxMenuItem item = new JCheckBoxMenuItem(label, sel);
            item.addActionListener(e -> {
                if (this.selectedClassIds.contains(cf.id)) this.selectedClassIds.remove(cf.id); else this.selectedClassIds.add(cf.id);
                this.updateSelectionButtonLabels();
            });
            items.add(item);
        }

        if (items.isEmpty()) {
            final JMenuItem none = new JMenuItem("No classes available");
            none.setEnabled(false);
            this.classesMenu.add(none);
            return;
        }

        if (ChatToolWindow.MENU_MAX_VISIBLE_ITEMS < items.size()) {
            final JPanel container = new JPanel();
            container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
            for (final JCheckBoxMenuItem it : items) {
                container.add(it);
            }
            final JBScrollPane sp = new JBScrollPane(container);
            sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            sp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
            sp.setPreferredSize(new Dimension(ChatToolWindow.MENU_PREFERRED_WIDTH, ChatToolWindow.MENU_PREFERRED_HEIGHT));
            final JMenuItem scrollItem = new JMenuItem();
            scrollItem.setLayout(new BorderLayout());
            scrollItem.add(sp, BorderLayout.CENTER);
            scrollItem.setEnabled(false);
            this.classesMenu.add(scrollItem);
        } else {
            for (final JCheckBoxMenuItem it : items) this.classesMenu.add(it);
        }

        this.classesMenu.addSeparator();
        final JMenuItem clear = new JMenuItem("Clear selection");
        clear.addActionListener(e -> {
            this.selectedClassIds.clear();
            this.updateSelectionButtonLabels(); });
        this.classesMenu.add(clear);
    }

    private void rebuildMethodsMenu() {
        this.methodsMenu.removeAll();
        final List<JCheckBoxMenuItem> items = new ArrayList<>();
        for (final CodeSection cs : this.availableSections) {
            if (!cs.enabled) continue;
            final String shortCode = null != cs.code && 60 < cs.code.length() ? cs.code.substring(0, 60) + "..." : cs.code;
            final String label = (null != cs.fileName ? cs.fileName : "") + (null != cs.folder && !cs.folder.isBlank() ? "  —  " + cs.folder : "") + (null != shortCode && !shortCode.isBlank() ? "  |  " + shortCode.replaceAll("\n", " ") : "");
            final boolean sel = this.selectedMethodIds.contains(cs.id);
            final JCheckBoxMenuItem item = new JCheckBoxMenuItem(label, sel);
            item.addActionListener(e -> {
                if (this.selectedMethodIds.contains(cs.id)) this.selectedMethodIds.remove(cs.id); else this.selectedMethodIds.add(cs.id);
                this.updateSelectionButtonLabels();
            });
            items.add(item);
        }

        if (items.isEmpty()) {
            final JMenuItem none = new JMenuItem("No methods available");
            none.setEnabled(false);
            this.methodsMenu.add(none);
            return;
        }

        if (ChatToolWindow.MENU_MAX_VISIBLE_ITEMS < items.size()) {
            final JPanel container = new JPanel();
            container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
            for (final JCheckBoxMenuItem it : items) {
                container.add(it);
            }
            final JBScrollPane sp = new JBScrollPane(container);
            sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            sp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
            sp.setPreferredSize(new Dimension(ChatToolWindow.MENU_PREFERRED_WIDTH, ChatToolWindow.MENU_PREFERRED_HEIGHT));
            final JMenuItem scrollItem = new JMenuItem();
            scrollItem.setLayout(new BorderLayout());
            scrollItem.add(sp, BorderLayout.CENTER);
            scrollItem.setEnabled(false);
            this.methodsMenu.add(scrollItem);
        } else {
            for (final JCheckBoxMenuItem it : items) this.methodsMenu.add(it);
        }

        this.methodsMenu.addSeparator();
        final JMenuItem clear = new JMenuItem("Clear selection");
        clear.addActionListener(e -> {
            this.selectedMethodIds.clear();
            this.updateSelectionButtonLabels(); });
        this.methodsMenu.add(clear);
    }

    // JBPopup-based selectors (do not close on hover)
    private void showClassesPopup() {
        try { if (null != classesPopup && !this.classesPopup.isDisposed()) this.classesPopup.cancel(); } catch (final Exception ignored) {}

        final JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        final List<JCheckBox> checkBoxes = new ArrayList<>();
        for (final CodeFile cf : this.availableFiles) {
            if (!cf.enabled) continue;
            final boolean sel = this.selectedClassIds.contains(cf.id);
            final String label = cf.fileName + (null != cf.folder && !cf.folder.isBlank() ? "  —  " + cf.folder : "");
            final JCheckBox cb = new JCheckBox(label, sel);
            cb.addActionListener(e -> {
                if (cb.isSelected()) this.selectedClassIds.add(cf.id); else this.selectedClassIds.remove(cf.id);
                this.updateSelectionButtonLabels();
            });
            checkBoxes.add(cb);
            listPanel.add(cb);
        }

        if (0 == listPanel.getComponentCount()) {
            listPanel.add(new JLabel("No classes available"));
        }

        final JBScrollPane sp = new JBScrollPane(listPanel);
        sp.setPreferredSize(new Dimension(ChatToolWindow.MENU_PREFERRED_WIDTH, ChatToolWindow.MENU_PREFERRED_HEIGHT));

        final JPanel root = new JPanel(new BorderLayout(6,6));
        root.setBorder(JBUI.Borders.empty(6));

        // Filter field
        final JTextField filterField = new JTextField();
        filterField.putClientProperty("JTextField.placeholderText", "Filter classes...");
        final JPanel filterPanel = new JPanel(new BorderLayout(4, 4));
        filterPanel.add(new JLabel("Filter:"), BorderLayout.WEST);
        filterPanel.add(filterField, BorderLayout.CENTER);
        root.add(filterPanel, BorderLayout.NORTH);

        root.add(sp, BorderLayout.CENTER);

        final JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        final JButton clearBtn = new JButton("Clear");
        final JButton closeBtn = new JButton("Close");
        clearBtn.addActionListener(e -> {
            this.selectedClassIds.clear();
            // uncheck all
            for (final JCheckBox cbx : checkBoxes) cbx.setSelected(false);
            this.updateSelectionButtonLabels();
        });
        closeBtn.addActionListener(e -> { try { if (null != classesPopup) this.classesPopup.cancel(); } catch (final
        Exception ignored) {} });
        actions.add(clearBtn);
        actions.add(closeBtn);
        root.add(actions, BorderLayout.SOUTH);

        // Filtering logic
        final Runnable applyFilter = () -> {
            final String q = filterField.getText().toLowerCase().trim();
            for (final JCheckBox cbx : checkBoxes) {
                final boolean visible = q.isEmpty() || cbx.getText().toLowerCase().contains(q);
                cbx.setVisible(visible);
            }
            listPanel.revalidate();
            listPanel.repaint();
        };
        filterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(final DocumentEvent e) { applyFilter.run(); }
            @Override public void removeUpdate(final DocumentEvent e) { applyFilter.run(); }
            @Override public void changedUpdate(final DocumentEvent e) { applyFilter.run(); }
        });

        this.classesPopup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(root, filterField)
                .setResizable(true)
                .setMovable(true)
                .setRequestFocus(true)
                .setCancelOnClickOutside(true)
                .setCancelOnOtherWindowOpen(true)
                .setDimensionServiceKey(this.project, "MOEP.ClassesPopup", false)
                .createPopup();
        this.classesPopup.showUnderneathOf(this.classesButton);
    }

    private void showMethodsPopup() {
        try { if (null != methodsPopup && !this.methodsPopup.isDisposed()) this.methodsPopup.cancel(); } catch (final Exception ignored) {}

        final JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        final List<JCheckBox> checkBoxes = new ArrayList<>();
        for (final CodeSection cs : this.availableSections) {
            if (!cs.enabled) continue;
            final String shortCode = null != cs.code && 60 < cs.code.length() ? cs.code.substring(0, 60) + "..." : cs.code;
            final String label = (null != cs.fileName ? cs.fileName : "") + (null != cs.folder && !cs.folder.isBlank() ? "  —  " + cs.folder : "") + (null != shortCode && !shortCode.isBlank() ? "  |  " + shortCode.replaceAll("\n", " ") : "");
            final boolean sel = this.selectedMethodIds.contains(cs.id);
            final JCheckBox cb = new JCheckBox(label, sel);
            cb.addActionListener(e -> {
                if (cb.isSelected()) this.selectedMethodIds.add(cs.id); else this.selectedMethodIds.remove(cs.id);
                this.updateSelectionButtonLabels();
            });
            checkBoxes.add(cb);
            listPanel.add(cb);
        }

        if (0 == listPanel.getComponentCount()) {
            listPanel.add(new JLabel("No methods available"));
        }

        final JBScrollPane sp = new JBScrollPane(listPanel);
        sp.setPreferredSize(new Dimension(ChatToolWindow.MENU_PREFERRED_WIDTH, ChatToolWindow.MENU_PREFERRED_HEIGHT));

        final JPanel root = new JPanel(new BorderLayout(6,6));
        root.setBorder(JBUI.Borders.empty(6));

        // Filter field
        final JTextField filterField = new JTextField();
        filterField.putClientProperty("JTextField.placeholderText", "Filter methods...");
        final JPanel filterPanel = new JPanel(new BorderLayout(4, 4));
        filterPanel.add(new JLabel("Filter:"), BorderLayout.WEST);
        filterPanel.add(filterField, BorderLayout.CENTER);
        root.add(filterPanel, BorderLayout.NORTH);

        root.add(sp, BorderLayout.CENTER);

        final JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        final JButton clearBtn = new JButton("Clear");
        final JButton closeBtn = new JButton("Close");
        clearBtn.addActionListener(e -> {
            this.selectedMethodIds.clear();
            for (final JCheckBox cbx : checkBoxes) cbx.setSelected(false);
            this.updateSelectionButtonLabels();
        });
        closeBtn.addActionListener(e -> { try { if (null != methodsPopup) this.methodsPopup.cancel(); } catch (final
        Exception ignored) {} });
        actions.add(clearBtn);
        actions.add(closeBtn);
        root.add(actions, BorderLayout.SOUTH);

        // Filtering logic
        final Runnable applyFilter = () -> {
            final String q = filterField.getText().toLowerCase().trim();
            for (final JCheckBox cbx : checkBoxes) {
                final boolean visible = q.isEmpty() || cbx.getText().toLowerCase().contains(q);
                cbx.setVisible(visible);
            }
            listPanel.revalidate();
            listPanel.repaint();
        };
        filterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(final DocumentEvent e) { applyFilter.run(); }
            @Override public void removeUpdate(final DocumentEvent e) { applyFilter.run(); }
            @Override public void changedUpdate(final DocumentEvent e) { applyFilter.run(); }
        });

        this.methodsPopup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(root, filterField)
                .setResizable(true)
                .setMovable(true)
                .setRequestFocus(true)
                .setCancelOnClickOutside(true)
                .setCancelOnOtherWindowOpen(true)
                .setDimensionServiceKey(this.project, "MOEP.MethodsPopup", false)
                .createPopup();
        this.methodsPopup.showUnderneathOf(this.methodsButton);
    }

    private void updateSelectionButtonLabels() {
        this.classesButton.setText("Classes (" + this.selectedClassIds.size() + ")");
        this.methodsButton.setText("Methods (" + this.selectedMethodIds.size() + ")");
    }

    private String buildSelectedContextText() {
        final StringBuilder sb = new StringBuilder();
        final int maxChars = 8000; // budget for context
        int remaining = maxChars;

        if (!this.selectedClassIds.isEmpty()) {
            sb.append("Context - Selected Classes:\n\n");
            for (final Long id : this.selectedClassIds) {
                final CodeFile cf = this.findFileById(id);
                if (null == cf) continue;
                final String header = "File: " + this.safe(cf.fileName) + (null != cf.folder && !cf.folder.isBlank() ? "\nPath: " + cf.folder : "") + "\n\n";
                final String codeBlockStart = "```\n";
                String code = null != cf.code ? cf.code : "";
                final String codeBlockEnd = "\n```\n\n";
                final int need = header.length() + codeBlockStart.length() + code.length() + codeBlockEnd.length();
                if (need > remaining) {
                    final int allowed = Math.max(0, remaining - header.length() - codeBlockStart.length() - codeBlockEnd.length());
                    if (0 >= allowed) break;
                    code = code.substring(0, Math.min(code.length(), allowed));
                }
                sb.append(header).append(codeBlockStart).append(code).append(codeBlockEnd);
                remaining = maxChars - sb.length();
                if (0 >= remaining) break;
            }
        }

        if (0 < remaining && !this.selectedMethodIds.isEmpty()) {
            sb.append("Context - Selected Methods:\n\n");
            for (final Long id : this.selectedMethodIds) {
                final CodeSection cs = this.findSectionById(id);
                if (null == cs) continue;
                final String header = (null != cs.fileName ? ("File: " + this.safe(cs.fileName) + "\n") : "") + (null != cs.folder && !cs.folder.isBlank() ? ("Path: " + cs.folder + "\n") : "") + "\n";
                final String codeBlockStart = "```\n";
                String code = null != cs.code ? cs.code : "";
                final String codeBlockEnd = "\n```\n\n";
                final int need = header.length() + codeBlockStart.length() + code.length() + codeBlockEnd.length();
                if (need > remaining) {
                    final int allowed = Math.max(0, remaining - header.length() - codeBlockStart.length() - codeBlockEnd.length());
                    if (0 >= allowed) break;
                    code = code.substring(0, Math.min(code.length(), allowed));
                }
                sb.append(header).append(codeBlockStart).append(code).append(codeBlockEnd);
                remaining = maxChars - sb.length();
                if (0 >= remaining) break;
            }
        }

        return sb.toString();
    }

    private String safe(final String s) { return null == s ? "" : s; }

    private CodeFile findFileById(final long id) {
        for (final CodeFile cf : this.availableFiles) {
            if (cf.id == id) return cf;
        }
        return null;
    }

    private CodeSection findSectionById(final long id) {
        for (final CodeSection cs : this.availableSections) {
            if (cs.id == id) return cs;
        }
        return null;
    }

    private void sendMessage(final Project project) {
        if (this.isProcessing) {
            return; // Prevent multiple submissions
        }
        
        final String userMessage = this.inputField.getText().trim();
        if (userMessage.isEmpty()) {
            return;
        }

        this.setProcessingState(true);
        this.handleUserInput(userMessage);
        this.processMessageAsync(project, userMessage);
    }

    private void handleUserInput(final String userMessage) {
        this.appendMessage("You", userMessage, "#0078d4");
        this.inputField.setText("");
        this.chatHistoryPane.setCaretPosition(this.chatHistoryPane.getDocument().getLength());
    }
    
    private void setProcessingState(final boolean processing) {
        this.isProcessing = processing;
        this.inputField.setEnabled(!processing);
        this.sendButton.setEnabled(!processing);

        this.trashButton.setEnabled(!processing);
        this.trashButton.setText(processing ? "\uD83D\uDEAB" : "\uD83D\uDDD1");
        
        // Update button text to show processing state
        if (processing) {
            this.sendButton.setText("Processing...");
        } else {
            this.sendButton.setText("Send");
        }
    }

    private void processMessageAsync(final Project project, final String userMessage) {
        this.executor.submit(() -> {
            try {
                final String sysPrompt = this.buildSystemPrompt();
                System.out.println("[processMessageAsync] sysPrompt: " + sysPrompt);

                final String inlineContext = this.buildSelectedContextText();
                final String inv = this.buildInventoryForSelection();
                System.out.println("[processMessageAsync] selected classes=" + this.selectedClassIds.size() + ", methods=" + this.selectedMethodIds.size());
                final int inlineLen = null == inlineContext ? 0 : inlineContext.length();
                final int invLen = null == inv ? 0 : inv.length();
                System.out.println("[processMessageAsync] inlineContext length=" + inlineLen + " + inventory " + invLen + " / " + ChatToolWindow.CONTEXT_INLINE_BUDGET);
                if (ChatToolWindow.CONTEXT_INLINE_BUDGET < inlineLen + invLen) {
                    System.out.println("[processMessageAsync] context too large (" + inlineContext.length() + ") -> running chunked summarization flow");
                    this.processLargeContextFlow(project, userMessage);
                } else {
                    final String prompt = this.buildUserPrompt(userMessage);
                    System.out.println("[processMessageAsync] prompt: " + prompt);
                    this.generateAIResponse(project, prompt, sysPrompt);
                    System.out.println("[processMessageAsync] generateAIResponse done");
                }

            } catch (final Exception ex) {
                this.appendSystemMessage("Error: " + ex.getMessage());
                SwingUtilities.invokeLater(() -> this.setProcessingState(false));
            }
        });
    }

    

    private String buildSystemPrompt() {
        String basePrompt = "You are a friendly AI assistant, you help the user answer their question or simply talk to them. If they dont have a question and simply want to have a conversation, you simply talk to them. You do not need to use formatting for a normal conversation, if you want you can.";
        basePrompt += "\nPlease use only markdown for formatting never latex because it cant be used, therefore you NEED to use markdown if you want to format anything.";
        return basePrompt;
    }

    // Large context handling: chunk, per-chunk QA with original question, then aggregate
    private static final String CHUNK_SUMMARY_SYSTEM_PROMPT =
            "Summarize the code CHUNK. State its purpose and behavior, and list key elements (classes/structs/functions/methods/variables). " +
            "List the method and class names you see. Use clear Markdown. Keep it concise and avoid long code.";

    // New: QA prompt for per-chunk question answering (do NOT mention chunking explicitly)
    private static final String CHUNK_QA_SYSTEM_PROMPT =
            "Answer the user's question based ONLY on the provided code and metadata. " +
            "If the answer is not present, reply with 'not present' and list any related identifiers. " +
            "Be concise. Use Markdown. Do not speculate.";

    private static final String FINAL_AGGREGATION_SYSTEM_PROMPT =
            "You are given multiple parts of answers and a deterministic inventory (filenames, paths, method signatures). " +
            "Use ALL information exactly as presented to answer the user's question. Do not discard details. " +
            "If there are conflicts, note them and choose the best-supported interpretation. " +
            "Explain the files and relationships as needed. Use only these answers/metadata; avoid guessing. Use Markdown.";

    private void processLargeContextFlow(final Project project, final String userMessage) {
        try {
            // Collect selected items
            final List<CodeFile> files = new ArrayList<>();
            for (final Long id : this.selectedClassIds) {
                final CodeFile cf = this.findFileById(id);
                if (null != cf && cf.enabled) files.add(cf);
            }
            final List<CodeSection> sections = new ArrayList<>();
            for (final Long id : this.selectedMethodIds) {
                final CodeSection cs = this.findSectionById(id);
                if (null != cs && cs.enabled) sections.add(cs);
            }
            System.out.println("[chunk] Large-context flow: resolved files=" + files.size() + ", sections=" + sections.size());

            final StringBuilder aggregated = new StringBuilder();
            aggregated.append("Aggregated per-chunk answers (generated automatically):\n\n");

            // Summarize files (prefer method-based chunks)
            for (final CodeFile cf : files) {
                final String fileCode = null != cf.code ? cf.code : "";
                System.out.println("[chunk] File '" + this.safe(cf.fileName) + "' length=" + fileCode.length());
                final List<CodeSection> fileSections = this.getSectionsForFile(cf);
                List<String> chunks = new ArrayList<>();
                if (!fileSections.isEmpty()) {
                    for (final CodeSection cs : fileSections) {
                        final String sectionCode = null != cs.code ? cs.code : "";
                        if (CHUNK_TARGET_CHARS * 2 < sectionCode.length()) {
                            final List<String> sub = this.softSplitText(sectionCode);
                            chunks.addAll(sub);
                        } else {
                            chunks.add(sectionCode);
                        }
                    }
                    System.out.println("[chunk] -> using method-based chunks from KB: " + chunks.size() + " for file '" + this.safe(cf.fileName) + "'");
                } else {
                    chunks = this.softSplitText(fileCode);
                    System.out.println("[chunk] -> produced " + chunks.size() + " char-based chunks for file '" + this.safe(cf.fileName) + "'");
                }
                aggregated.append("# File: ").append(this.safe(cf.fileName));
                if (null != cf.folder && !cf.folder.isBlank()) aggregated.append("\nPath: ").append(cf.folder);
                aggregated.append("\nChunks: ").append(chunks.size()).append("\n\n");
                for (int i = 0; i < chunks.size(); i++) {
                    final String piece = chunks.get(i);
                    System.out.println("[qa] File '" + this.safe(cf.fileName) + "' chunk " + (i+1) + "/" + chunks.size() + " length=" + piece.length());
                    final String meta = "Context: file '" + this.safe(cf.fileName) + "'" + (null != cf.folder && !cf.folder.isBlank() ? (" at path '" + cf.folder + "'") : "") + ", segment " + (i+1) + "/" + chunks.size() + ".";
                    final String answer = this.answerChunkSync(piece, userMessage, meta);
                    System.out.println("[qa] -> answer length=" + (null == answer ? 0 : answer.length()));
                    aggregated.append("- Chunk ").append(i + 1).append(" answer:\n").append(answer).append("\n\n");
                }
            }

            // Per-chunk QA for selected methods/sections
            for (final CodeSection cs : sections) {
                final String sectionCode = null != cs.code ? cs.code : "";
                System.out.println("[chunk] Section from file '" + this.safe(cs.fileName) + "' length=" + sectionCode.length());
                final List<String> chunks = this.softSplitText(sectionCode);
                System.out.println("[chunk] -> produced " + chunks.size() + " chunks for section of file '" + this.safe(cs.fileName) + "'");
                aggregated.append("# Method section from file: ").append(this.safe(cs.fileName));
                if (null != cs.folder && !cs.folder.isBlank()) aggregated.append("\nPath: ").append(cs.folder);
                aggregated.append("\nChunks: ").append(chunks.size()).append("\n\n");
                for (int i = 0; i < chunks.size(); i++) {
                    final String piece = chunks.get(i);
                    System.out.println("[qa] Section '" + this.safe(cs.fileName) + "' chunk " + (i+1) + "/" + chunks.size() + " length=" + piece.length());
                    final String meta = "Context: method/section in file '" + this.safe(cs.fileName) + "'" + (null != cs.folder && !cs.folder.isBlank() ? (" at path '" + cs.folder + "'") : "") + ", segment " + (i+1) + "/" + chunks.size() + ".";
                    final String answer = this.answerChunkSync(piece, userMessage, meta);
                    System.out.println("[qa] -> answer length=" + (null == answer ? 0 : answer.length()));
                    aggregated.append("- Chunk ").append(i + 1).append(" answer:\n").append(answer).append("\n\n");
                }
            }

            // Build final prompt with aggregated summaries and user's question
            final StringBuilder finalPrompt = new StringBuilder();
            finalPrompt.append("You are given aggregated summaries of code from the project's knowledge base. " +
                    "Use these summaries (not the original raw code) to answer the user's question with as much detail as necessary. " +
                    "If there are gaps, reason cautiously and state assumptions.\n\n");

            // Add metadata overview
            if (!files.isEmpty()) {
                finalPrompt.append("Included files:\n");
                for (final CodeFile cf : files) {
                    finalPrompt.append("- ").append(this.safe(cf.fileName));
                    if (null != cf.folder && !cf.folder.isBlank()) finalPrompt.append(" (" + cf.folder + ")");
                    finalPrompt.append("\n");
                }
                finalPrompt.append("\n");
            }
            if (!sections.isEmpty()) {
                finalPrompt.append("Included methods/sections:\n");
                for (final CodeSection cs : sections) {
                    finalPrompt.append("- ").append(this.safe(cs.fileName));
                    if (null != cs.folder && !cs.folder.isBlank()) finalPrompt.append(" (" + cs.folder + ")");
                    finalPrompt.append("\n");
                }
                finalPrompt.append("\n");
            }

            // Deterministic inventory always included
            final String inventory = this.buildInventoryForSelection();
            if (null != inventory && !inventory.isBlank()) {
                finalPrompt.append("Inventory (deterministic from KB):\n");
                finalPrompt.append(inventory).append("\n");
            }

            finalPrompt.append(aggregated);
            finalPrompt.append("\n\nUser question:\n").append(userMessage);

            // Stream the final answer with an aggregation-focused system prompt
            this.generateAIResponse(project, finalPrompt.toString(), ChatToolWindow.FINAL_AGGREGATION_SYSTEM_PROMPT);
        } catch (final Exception e) {
            this.appendSystemMessage("Error during large context processing: " + e.getMessage());
        }
    }

    private String summarizeChunkSync(final String codeChunk, final String meta) {
        try {
            System.out.println("[summarize] Summarizing chunk, code length=" + (null == codeChunk ? 0 : codeChunk.length()) + "; meta=" + meta);
            final String userPrompt = meta + "\n\nHere is the code CHUNK:\n```\n" + (null == codeChunk ? "" : codeChunk) + "\n```\n\nPlease provide the explanation now.";
            final var tc = OllamaAPIUtil.generateOllamaResponseSync(userPrompt, ChatToolWindow.CHUNK_SUMMARY_SYSTEM_PROMPT);
            return null != tc ? (null != tc.visibleContent() ? tc.visibleContent() : "") : "";
        } catch (final Exception e) {
            return "[Chunk summarization failed: " + e.getMessage() + "]";
        }
    }

    private String answerChunkSync(final String codeChunk, final String userQuestion, final String meta) {
        try {
            System.out.println("[qa] Answering question for chunk, code length=" + (null == codeChunk ? 0 : codeChunk.length()) + "; meta=" + meta);
            final String userPrompt = meta + "\n\nCode:\n```\n" + (null == codeChunk ? "" : codeChunk) + "\n```\n\nQuestion: " + userQuestion + "\n\nAnswer:";
            final var tc = OllamaAPIUtil.generateOllamaResponseSync(userPrompt, ChatToolWindow.CHUNK_QA_SYSTEM_PROMPT);
            return null != tc ? (null != tc.visibleContent() ? tc.visibleContent() : "") : "";
        } catch (final Exception e) {
            return "[Chunk QA failed: " + e.getMessage() + "]";
        }
    }

    private List<String> softSplitText(final String text) {
        final List<String> chunks = new ArrayList<>();
        if (null == text) return chunks;
        final int n = text.length();
        int i = 0;
        System.out.println("[split] begin length=" + n + ", target=" + ChatToolWindow.CHUNK_TARGET_CHARS + ", window=" + ChatToolWindow.CHUNK_SOFT_WINDOW);
        while (i < n) {
            final int start = i;
            final int targetEnd = Math.min(start + ChatToolWindow.CHUNK_TARGET_CHARS, n);
            int end = this.findSoftBoundary(text, start, targetEnd);
            if (end <= start) end = Math.min(targetEnd, n); // safety
            chunks.add(text.substring(start, end));
            // advance with overlap
            final int nextStart = Math.max(end - ChatToolWindow.CHUNK_OVERLAP, start + 1);
            i = nextStart;
        }
        System.out.println("[split] produced chunks=" + chunks.size());
        return chunks;
    }

    private int findSoftBoundary(final String text, final int start, final int targetEnd) {
        final int n = text.length();
        final int forwardLimit = Math.min(targetEnd + ChatToolWindow.CHUNK_SOFT_WINDOW, n);
        // Prefer forward break after target within window
        for (int j = targetEnd; j < forwardLimit; j++) {
            final char c = text.charAt(j - 1);
            if ('\n' == c || '\r' == c || ';' == c || '}' == c || ')' == c) {
                return j;
            }
        }
        // Otherwise, look backward within window
        final int backwardLimit = Math.max(start, targetEnd - ChatToolWindow.CHUNK_SOFT_WINDOW);
        for (int j = targetEnd; j > backwardLimit; j--) {
            final char c = text.charAt(j - 1);
            if ('\n' == c || '\r' == c || ';' == c || '}' == c || ')' == c || ' ' == c) {
                return j;
            }
        }
        // Hard split at target
        System.out.println("[split] hard boundary at targetEnd=" + targetEnd);
        return targetEnd;
    }

    private String buildUserPrompt(final String userMessage) {
        final String inventory = this.buildInventoryForSelection();
        final String context = this.buildSelectedContextText();
        final StringBuilder sb = new StringBuilder();
        if (null != inventory && !inventory.isBlank()) {
            sb.append("Inventory (deterministic from KB):\n");
            sb.append(inventory).append("\n");
        }
        if (null != context && !context.isBlank()) {
            sb.append(context).append("\n\n");
        }
        sb.append("User question:\n").append(userMessage);
        return sb.toString();
    }

    

    private void generateAIResponse(final Project project, final String prompt, final String sysPrompt) {
        System.out.println("Generating:");

        this.currentAIResponse.setLength(0);
        ChatUtil.Message[] aiMessage = new ChatUtil.Message[1];

        // Create the AI message first
        try {
            aiMessage[0] = this.initializeAiMessage();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            this.appendSystemMessage("Error: Operation was interrupted");
            this.setProcessingState(false);
            return;
        } catch (final InvocationTargetException e) {
            this.appendSystemMessage("Error: Failed to update chat interface - " + e.getCause().getMessage());
            this.setProcessingState(false);
            return;
        }

        OllamaAPIUtil.generateStreamingOllamaResponse(
                project,
                prompt,
                sysPrompt,
                this.createTokenConsumer(aiMessage[0]),
                this.createCompletionHandler(project, aiMessage[0]),
                this.createErrorHandler()
        );
    }

    // Deterministic inventory from KB for selected files and methods
    private String buildInventoryForSelection() {
        final StringBuilder sb = new StringBuilder();
        if (!this.selectedClassIds.isEmpty()) {
            sb.append("Files and their methods:\n");
            for (final Long id : this.selectedClassIds) {
                final CodeFile cf = this.findFileById(id);
                if (null == cf) continue;
                sb.append("- File: ").append(this.safe(cf.fileName));
                if (null != cf.folder && !cf.folder.isBlank()) sb.append(" (" + cf.folder + ")");
                sb.append("\n");

                final List<CodeSection> secs = this.getSectionsForFile(cf);
                if (secs.isEmpty()) {
                    sb.append("  (no methods indexed)\n");
                } else {
                    for (final CodeSection cs : secs) {
                        final String sig = this.extractSignature(cs.code);
                        sb.append("  • ").append(sig).append("\n");
                    }
                }
                sb.append("\n");
            }
        }

        if (!this.selectedMethodIds.isEmpty()) {
            sb.append("Individually selected methods:\n");
            for (final Long id : this.selectedMethodIds) {
                final CodeSection cs = this.findSectionById(id);
                if (null == cs) continue;
                sb.append("- ");
                if (null != cs.fileName) sb.append(this.safe(cs.fileName)).append(": ");
                sb.append(this.extractSignature(cs.code));
                if (null != cs.folder && !cs.folder.isBlank()) sb.append(" (" + cs.folder + ")");
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    private List<CodeSection> getSectionsForFile(final CodeFile cf) {
        final List<CodeSection> result = new ArrayList<>();
        for (final CodeSection cs : this.availableSections) {
            if (!cs.enabled) continue;
            final boolean nameMatch = null != cs.fileName && cs.fileName.equals(cf.fileName);
            final boolean folderMatch = (null == cs.folder ? (null == cf.folder || cf.folder.isBlank()) : cs.folder.equals(cf.folder));
            if (nameMatch && folderMatch) {
                result.add(cs);
            }
        }
        return result;
    }

    // Quick signature extraction: first non-empty line, trimmed to 120 chars
    private String extractSignature(final String code) {
        if (null == code) return "(unknown signature)";
        final String[] lines = code.split("\r?\n");
        for (final String line : lines) {
            String t = line.trim();
            if (!t.isEmpty()) {
                if (120 < t.length()) t = t.substring(0, 117) + "...";
                return t;
            }
        }
        return "(empty section)";
    }

    // Initializes and inserts an empty AI message into the chat and refreshes the UI.
    private ChatUtil.Message initializeAiMessage() throws InterruptedException, InvocationTargetException {
        ChatUtil.Message[] holder = new ChatUtil.Message[1];
        SwingUtilities.invokeAndWait(() -> {
            holder[0] = new ChatUtil.Message("AI", "");
            this.chatMessages.add(holder[0]);
            this.updateChatHistoryPane();
        });
        return holder[0];
    }

    

    // Streams tokens into the AI message with live UI updates.
    private java.util.function.Consumer<String> createTokenConsumer(final ChatUtil.Message aiMsg) {
        return token -> {
            this.currentAIResponse.append(token);
            SwingUtilities.invokeLater(() -> {
                aiMsg.message = this.currentAIResponse.toString();
                this.updateChatHistoryPane();
            });
        };
    }

    // Handles stream completion: finalizes message, persists, notifies, and resets state.
    private Runnable createCompletionHandler(final Project project, final ChatUtil.Message aiMsg) {
        return () -> {
            try {
                final String aiMsgText = this.currentAIResponse.toString();
                aiMsg.message = aiMsgText;

                System.out.println("[generateAIResponse] " + aiMsgText);

                ChatUtil.saveChatHistoryToFile(this.chatMessages, this.chatFilePath);
                this.updateChatHistoryPane();
                try {
                    final String preview = 120 < aiMsgText.length() ? aiMsgText.substring(0, 117) + "..." : aiMsgText;
                    NotificationUtil.info(project, "New message from LLM: " + preview);
                } catch (final Exception notifyEx) {
                    System.err.println("Notification failed: " + notifyEx.getMessage());
                }
            } finally {
                SwingUtilities.invokeLater(() -> this.setProcessingState(false));
            }
        };
    }

    // Handles stream error: posts system message, persists, and resets state.
    private java.util.function.Consumer<String> createErrorHandler() {
        return error -> {
            try {
                this.appendSystemMessage(error);
                ChatUtil.saveChatHistoryToFile(this.chatMessages, this.chatFilePath);
            } finally {
                SwingUtilities.invokeLater(() -> this.setProcessingState(false));
            }
        };
    }

    private void handleTrashAction() {
        final String[] options = {"Save Chat", "Discard Chat", "Cancel"};
        final int result = DialogUtil.showOptionDialog(
                "Do you want to save the chat before clearing?",
                "Clear Chat",
                options,
                0
        );

        if (0 == result) { // Save Chat
            ChatUtil.exportChatAsJson(this.chatMessages, this.mainPanel);
            ChatUtil.clearChat(this.chatMessages, this.chatFilePath);

            this.updateChatHistoryPane();
        } else if (1 == result) { // Discard Chat
            ChatUtil.clearChat(this.chatMessages, this.chatFilePath);
            this.updateChatHistoryPane();
        }
    }

    private void updateChatHistoryPane() {
        SwingUtilities.invokeLater(() -> {
            try {
                prepareChatHistoryPane();
                final StringBuilder html = buildHtmlContent();

                System.out.println("[updateChatHistoryPane] html: " + html);
                setChatHistoryContent(html.toString());
            } catch (final Exception e) {
                handleChatHistoryError(e);
            }
        });
    }
    
    /**
     * Prepares the chat history pane for new content.
     */
    private void prepareChatHistoryPane() throws Exception {
        // Clear the document
        this.chatHistoryPane.getDocument().remove(0, this.chatHistoryPane.getDocument().getLength());
        
        // Set content type to HTML
        this.chatHistoryPane.setContentType("text/html");
    }
    
    /**
     * Builds the HTML content for the chat history.
     */
    private StringBuilder buildHtmlContent() {
        final StringBuilder html = createHtmlHeader();
        
        for (final ChatUtil.Message msg : this.chatMessages) {
            appendMessageToHtml(html, msg);
        }
        
        html.append("</body></html>");
        return html;
    }
    
    /**
     * Creates the HTML header with CSS styling.
     */
    private StringBuilder createHtmlHeader() {
        final boolean dark = this.isDarkTheme();

        // Define very simple, Swing-HTML-friendly CSS with theme-aware hex colors
        final String bodyBg = dark ? "#232323" : "#ffffff";
        final String msgBg = dark ? "#2b2b2b" : "#f5f5f5";
        final String msgColor = dark ? "#bbbbbb" : "#222222";
        final String userBg = dark ? "#283593" : "#e3f2fd";
        final String userColor = dark ? "#ffffff" : "#0d47a1";
        final String aiBg = dark ? "#263238" : "#eceff1";
        final String aiColor = dark ? "#ffffff" : "#263238";
        final String senderColor = dark ? "#ffffff" : "#000000";
        final String msgTextBg = dark ? "#232323" : "#ffffff";
        final String codeBg = dark ? "#181818" : "#f0f0f0";
        final String codeColor = dark ? "#e0e0e0" : "#111111";

        return new StringBuilder(
            "<html><head>" +
            "<style>" +
            "body { font-family: Arial, sans-serif; background-color: " + bodyBg + "; margin: 0; padding: 2px; }" +
            ".message { margin: 8px 0 16px 0; padding: 8px; background-color: " + msgBg + "; color: " + msgColor + "; }" +
            ".user { background-color: " + userBg + "; color: " + userColor + "; margin-left: 20%; }" +
            ".ai { background-color: " + aiBg + "; color: " + aiColor + "; margin-right: 20%; }" +
            ".sender { font-weight: bold; color: " + senderColor + "; }" +
            ".message-text { background-color: " + msgTextBg + "; color: " + msgColor + "; padding: 6px; font-size: 1em; }" +
            "pre { background-color: " + codeBg + "; color: " + codeColor + "; padding: 6px; font-family: monospace; }" +
            "code { background-color: " + codeBg + "; color: " + codeColor + "; padding: 2px 4px; font-family: monospace; }" +
            "</style>" +
            "</head><body>"
        );
    }

    private boolean isDarkTheme() {
        // Infer darkness from the current panel background luminance; this follows IDE LAF (not editor scheme)
        try {
            final Color bg = UIUtil.getPanelBackground();
            final int r = bg.getRed();
            final int g = bg.getGreen();
            final int b = bg.getBlue();
            // Perceptual luminance
            final double y = 0.2126 * r + 0.7152 * g + 0.0722 * b;
            return 128.0 > y;
        } catch (final Throwable t) {
            // Fallback to Laf name heuristics
            try {
                String lafName = null != UIManager.getLookAndFeel() ? UIManager.getLookAndFeel().getName() : "";
                lafName = null != lafName ? lafName.toLowerCase() : "";
                return lafName.contains("darcula") || lafName.contains("dark");
            } catch (final Throwable ignored) {
                return false;
            }
        }
    }
    
    /**
     * Appends a message to the HTML content.
     */
    private void appendMessageToHtml(final StringBuilder html, final ChatUtil.Message msg) {
        // Skip empty messages
        if (this.isEmptyMessage(msg)) {
            return;
        }
        
        // Process message content and generate HTML
        final String processedMessage = this.format(msg.message);
        this.appendMessageHtml(html, msg, processedMessage);
        
        // Close message container
        html.append("</div>");
    }
    
    /**
     * Checks if a message is empty or null.
     */
    private boolean isEmptyMessage(final ChatUtil.Message msg) {
        return null == msg.message || msg.message.trim().isEmpty();
    }
    
    /**
     * Appends the main message HTML structure.
     */
    private void appendMessageHtml(final StringBuilder html, final ChatUtil.Message msg, final String processedMessage) {
        final boolean isUser = "You".equals(msg.sender);
        final String messageClass = isUser ? "message user" : "message ai";
        
        html.append(String.format(
            "<div class='%s'>" +
            "<span class='sender'>%s</span>" +
            "<div class='message-text'>%s</div>",
            messageClass,
            msg.sender,
            processedMessage
        ));
    }

    
    
    /**
     * Sets the HTML content to the chat history pane.
     */
    private void setChatHistoryContent(String htmlContent) {
        System.out.println("[setChatHistoryContent] htmlContent: " + htmlContent);
        System.out.println("[setChatHistoryContent] htmlContent length: " + htmlContent.length());
        try {
            // Always reset to plain text first to avoid Swing CSS parser bugs
            this.chatHistoryPane.setContentType("text/plain");
            this.chatHistoryPane.setText(""); // clear content

            // Now set as HTML
            this.chatHistoryPane.setContentType("text/html");
            if (null == htmlContent || htmlContent.trim().isEmpty()) {
                htmlContent = "<html><body><i>No chat history.</i></body></html>";
            }
            this.chatHistoryPane.setText(htmlContent);
            this.chatHistoryPane.setCaretPosition(this.chatHistoryPane.getDocument().getLength());
        } catch (final Exception e) {
            // Handle CSS parsing errors by falling back to plain text
            System.err.println("Error setting HTML content: " + e.getMessage());
            e.printStackTrace();
            
            // Fallback to plain text
            try {
                final String plainText = htmlContent.replaceAll("<[^>]*>", "");
                this.chatHistoryPane.setContentType("text/plain");
                this.chatHistoryPane.setText(plainText);
                this.chatHistoryPane.setCaretPosition(this.chatHistoryPane.getDocument().getLength());
                
                // Reset to HTML for next update
                this.chatHistoryPane.setContentType("text/html");
            } catch (final Exception fallbackException) {
                System.err.println("Error in fallback handling: " + fallbackException.getMessage());
                this.chatHistoryPane.setText("Error displaying chat history.");
            }
        }
    }
    
    /**
     * Handles errors when updating the chat history pane.
     */
    private void handleChatHistoryError(final Exception e) {
        e.printStackTrace();
        try {
            this.chatHistoryPane.setContentType("text/plain");
            this.chatHistoryPane.getDocument().insertString(0, "Error loading chat history: " + e.getMessage(), null);
        } catch (final Exception ex) {
            // Last resort if even this fails
            this.chatHistoryPane.setText("Error loading chat history.");
        }
    }

    private void appendMessage(final String sender, final String message, final String color) {
        // Create a new message
        final ChatUtil.Message chatMessage = new ChatUtil.Message(sender, message);
        
        // Add the message to our history
        this.chatMessages.add(chatMessage);
        
        // Save the chat history
        ChatUtil.saveChatHistoryToFile(this.chatMessages, this.chatFilePath);
        
        // Update the chat display
        this.updateChatHistoryPane();
        
        // Clear the current AI response buffer
        this.currentAIResponse.setLength(0);
    }

    private void appendSystemMessage(final String message) {
        // Add the system message to our history
        final ChatUtil.Message chatMessage = new ChatUtil.Message("System", message);
        this.chatMessages.add(chatMessage);
        
        // Save the chat history
        ChatUtil.saveChatHistoryToFile(this.chatMessages, this.chatFilePath);
        
        // Update the chat display
        this.updateChatHistoryPane();
    }
    
    /**
     * Simple detection for common LaTeX delimiters and standalone macros.
     */
    private boolean containsLatex(final String text) {
        if (null == text || text.isEmpty()) return false;
        final String[] patterns = {
            "\\$\\$[\\s\\S]*?\\$\\$",                               // $$ ... $$
            "(?<!\\\\)\\$[^$\\n]+(?<!\\\\)\\$",                    // $ ... $ (not escaped)
            "\\\\\\([\\s\\S]*?\\\\\\)",                             // \\( ... \\)
            "\\\\\\[[\\s\\S]*?\\\\\\]",                             // \\\\[ ... \\\\]
            "\\\\begin\\{(equation\\*?|align\\*?|gather\\*?|multline\\*?|cases)\\}[\\s\\S]*?\\\\end\\{\\1\\}", // envs
            // standalone common LaTeX macros (no delimiters)
            "\\\\(boxed|frac|sqrt|sum|prod|int|lim|log|sin|cos|tan|alpha|beta|gamma|delta|epsilon|theta|lambda|pi|phi|psi|omega|ldots|cdot|pm|leq|geq|neq|infty)(\\s*\\{[^}]*\\}){0,3}"
        };
        for (final String p : patterns) {
            if (text.matches("(?s).*" + p + ".*")) return true;
        }
        return false;
    }
    private String format(String message) {
        // First, run markdown/basic HTML
        final String md = this.formatMarkdown(message);

        // Then convert LaTeX to Swing-safe HTML/Unicode
        final String outputString = this.formatLatex(md);

        return outputString;
    }

    private String formatMarkdown(String message) {
        if (null == message) {
            return message;
        }
        final String original = message;
        
        // First, escape HTML special characters
        message = message
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll("\"", "&quot;")
            .replaceAll("'", "&#39;");
        
        final boolean dark = this.isDarkTheme();
        final String codeBg = dark ? "#1e1e1e" : "#f0f0f0";
        final String codeColor = dark ? "#e0e0e0" : "#111111";

        // Format code blocks with triple backticks
        message = message.replaceAll(
            "```(\\w*)\\s*([\\s\\S]*?)\\s*```",
            "<div style='background-color:" + codeBg + "; padding:12px; margin:8px 0; overflow-x:auto;'><pre style='margin:0; white-space:pre-wrap; font-family:monospace; font-size:0.9em; color:" + codeColor + ";'>$2</pre></div>"
        );
        
        // Format bold text
        message = message.replaceAll("\\*\\*([^*]+)\\*\\*", "<strong>$1</strong>");
        
        // Format inline code - needs to be after HTML escaping
        message = message.replaceAll("`([^`]+)`", "<code style='background-color:" + codeBg + "; color:" + codeColor + "; padding:2px 4px; font-family:monospace;'>$1</code>");
        
        // Format bulleted lists
        message = message.replaceAll("\\*\\s+([^\n]*)", "<li>$1</li>");
        message = message.replaceAll("(<li>.*?</li>\\s*)+", "<ul style='margin:8px 0; padding-left:20px;'>$0</ul>");
        
        // Format numbered lists
        message = message.replaceAll("(\\d+)\\.\\s+([^\n]*)", "<li>$2</li>");
        message = message.replaceAll("(<li>.*?</li>\\s*)+", "<ol style='margin:8px 0; padding-left:20px;'>$0</ol>");

        // Convert newlines to <br> tags (except inside code blocks)
        message = message.replaceAll("(?<!</?pre[^>]*>|</?code[^>]*>|</?li[^>]*>|</?ul[^>]*>|</?ol[^>]*>)\\n", "<br>");
        
        return message;
    }

    private String formatLatex(String html) {
        if (null == html) return null;
        // Avoid touching code blocks to preserve code formatting
        final boolean hasCodeBlocks = html.contains("<pre") || html.contains("<code");
        if (hasCodeBlocks) {
            return html;
        }
        // Convert LaTeX to Unicode/HTML (sup/sub) safely for Swing
        final String converted = LatexUnicodeUtility.convert(html);
        if (this.containsLatex(html)) {
            System.out.println("[debug] LaTeX converted in message");
        }
        return converted;
    }
}