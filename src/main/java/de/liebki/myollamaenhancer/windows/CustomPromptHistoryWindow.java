package de.liebki.myollamaenhancer.windows;

import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import de.liebki.myollamaenhancer.types.Entry;
import de.liebki.myollamaenhancer.utils.CustomPromptHistoryManager;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class CustomPromptHistoryWindow extends JDialog {

    private final DefaultListModel<Entry> listModel = new DefaultListModel<>();
    private final JList<Entry> entryList = new JBList<>(this.listModel);
    private final JTextArea detailArea = new JTextArea();
    private final JButton favButton = new JButton("Favorite");
    private final JButton copyPromptButton = new JButton("Copy Prompt");
    private final JButton copyResultButton = new JButton("Copy Result");
    private final JButton rerunButton = new JButton("Re-run");
    private final JTextArea inputArea = new JTextArea(3, 20);
    private final JButton sendButton = new JButton("Send");
    private final JButton deleteButton = new JButton("Delete");

    public CustomPromptHistoryWindow(final Project project, final ActionListener onSend) {
        super((Frame) null, "Custom Prompt History & Favorites", true);
        this.setMinimumSize(new Dimension(836, 338));
        this.setPreferredSize(new Dimension(835, 515));
        this.setSize(835, 515);
        this.setLocationRelativeTo(null);
        this.setLayout(new BorderLayout());
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        
        // Enable transparency support with undecorated window
        this.setUndecorated(true);
        this.getRootPane().setOpaque(false);

        // List setup
        this.entryList.setCellRenderer(new EntryCellRenderer());
        this.entryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.entryList.setOpaque(true);
        this.entryList.setBackground(UIManager.getColor("Panel.background"));
        final JScrollPane listScroll = new JBScrollPane(this.entryList);
        listScroll.setPreferredSize(new Dimension(340, 500));
        listScroll.setBorder(BorderFactory.createTitledBorder("History & Favorites"));

        // Load entries
        this.refreshList();

        // Right panel
        final JPanel rightPanel = new JPanel(new BorderLayout(12, 12));
        this.detailArea.setEditable(false);
        this.detailArea.setLineWrap(true);
        this.detailArea.setWrapStyleWord(true);
        this.detailArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        final JScrollPane detailScroll = new JBScrollPane(this.detailArea);
        detailScroll.setBorder(BorderFactory.createTitledBorder("Details"));
        rightPanel.add(detailScroll, BorderLayout.CENTER);

        // Button panel (actions)
        final JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        buttonPanel.setPreferredSize(new Dimension(0, 40));
        buttonPanel.add(this.favButton);
        buttonPanel.add(Box.createHorizontalStrut(12));
        buttonPanel.add(this.deleteButton);
        buttonPanel.add(Box.createHorizontalStrut(12));
        buttonPanel.add(this.copyPromptButton);
        buttonPanel.add(Box.createHorizontalStrut(12));
        buttonPanel.add(this.copyResultButton);
        buttonPanel.add(Box.createHorizontalStrut(12));
        buttonPanel.add(this.rerunButton);
        buttonPanel.add(Box.createHorizontalGlue());
        final JScrollPane buttonScroll = new JBScrollPane(buttonPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        buttonScroll.setBorder(BorderFactory.createEmptyBorder());
        buttonScroll.setPreferredSize(new Dimension(0, 48));
        rightPanel.add(buttonScroll, BorderLayout.NORTH);

        // Input panel (bottom)
        final JPanel inputPanel = new JPanel(new BorderLayout(8, 8));
        inputPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10, 0, 0, 0),
                BorderFactory.createTitledBorder("New Custom Prompt")
        ));
        this.inputArea.setLineWrap(true);
        this.inputArea.setWrapStyleWord(true);
        final JScrollPane inputScroll = new JBScrollPane(this.inputArea);
        inputScroll.setBorder(this.inputArea.getBorder());
        inputPanel.add(inputScroll, BorderLayout.CENTER);
        inputPanel.add(this.sendButton, BorderLayout.EAST);
        rightPanel.add(inputPanel, BorderLayout.SOUTH);

        // Split pane
        final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScroll, rightPanel);
        splitPane.setDividerLocation(300);
        splitPane.setBackground(UIManager.getColor("Panel.background"));
        splitPane.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"), 1));
        // Custom divider color for less contrast
        splitPane.setUI(new javax.swing.plaf.basic.BasicSplitPaneUI() {
            @Override
            public BasicSplitPaneDivider createDefaultDivider() {
                return new BasicSplitPaneDivider(this) {
                    @Override
                    public void paint(final Graphics g) {
                        g.setColor(UIManager.getColor("Panel.background"));
                        g.fillRect(0, 0, this.getWidth(), this.getHeight());
                        // Optionally, draw a subtle line in the middle
                        g.setColor(UIManager.getColor("Component.borderColor"));
                        final int mid = this.getWidth() / 2;
                        g.drawLine(mid, 0, mid, this.getHeight());
                    }
                };
            }
        });
        // Custom title bar with window dragging and close button
        final JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(UIManager.getColor("Panel.background"));
        titleBar.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        
        final JLabel titleLabel = new JLabel("Custom Prompt History & Favorites");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        titleBar.add(titleLabel, BorderLayout.WEST);
        
        final JButton closeButton = new JButton("×");
        closeButton.setFont(closeButton.getFont().deriveFont(Font.BOLD, 16.0f));
        closeButton.setPreferredSize(new Dimension(24, 24));
        closeButton.setBorder(BorderFactory.createEmptyBorder());
        closeButton.setFocusPainted(false);
        closeButton.addActionListener(e -> this.dispose());
        titleBar.add(closeButton, BorderLayout.EAST);
        
        // Add window dragging functionality
        Point[] startPoint = new Point[1];
        titleBar.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(final java.awt.event.MouseEvent e) {
                startPoint[0] = e.getPoint();
            }
        });
        titleBar.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseDragged(final java.awt.event.MouseEvent e) {
                if (null != startPoint[0]) {
                    final Point currentLocation = CustomPromptHistoryWindow.this.getLocation();
                    final Point newLocation = new Point(
                        currentLocation.x + e.getX() - startPoint[0].x,
                        currentLocation.y + e.getY() - startPoint[0].y
                    );
                    CustomPromptHistoryWindow.this.setLocation(newLocation);
                }
            }
        });

        this.add(titleBar, BorderLayout.NORTH);
        this.add(splitPane, BorderLayout.CENTER);

        // List selection
        this.entryList.addListSelectionListener(e -> this.updateDetailPanel());

        // Button actions
        this.favButton.setEnabled(false);
        this.favButton.addActionListener(e -> this.toggleFavorite());
        this.copyPromptButton.addActionListener(e -> this.copySelectedPrompt());
        this.copyResultButton.addActionListener(e -> this.copySelectedResult());
        this.rerunButton.addActionListener(e -> this.rerunSelectedPrompt());
        this.sendButton.addActionListener(onSend);
        // Ctrl+Enter (or Cmd+Enter on Mac) sends the prompt, Enter inserts newline
        final KeyStroke ctrlEnter = KeyStroke.getKeyStroke("control ENTER");
        final KeyStroke cmdEnter = KeyStroke.getKeyStroke("meta ENTER");
        this.inputArea.getInputMap().put(ctrlEnter, "sendAction");
        this.inputArea.getInputMap().put(cmdEnter, "sendAction");
        this.inputArea.getActionMap().put("sendAction", new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                CustomPromptHistoryWindow.this.sendButton.doClick();
            }
        });
        this.deleteButton.setEnabled(false);
        this.deleteButton.addActionListener(e -> this.deleteSelectedEntry());

        // Initial state
        if (!this.listModel.isEmpty()) this.entryList.setSelectedIndex(0);
        else this.updateDetailPanel();

        // ESC key closes the window
        this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "closeWindow");
        this.getRootPane().getActionMap().put("closeWindow", new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                CustomPromptHistoryWindow.this.dispose();
            }
        });

        // Make inputArea visually distinct and inviting
        this.inputArea.setBackground(UIManager.getColor("Panel.background"));
        this.inputArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"), 2),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        // Placeholder text
        this.inputArea.setForeground(JBColor.GRAY);
        this.inputArea.setText("Type your custom prompt here...");
        this.inputArea.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(final java.awt.event.FocusEvent e) {
                if ("Type your custom prompt here...".equals(inputArea.getText())) {
                    CustomPromptHistoryWindow.this.inputArea.setText("");
                    CustomPromptHistoryWindow.this.inputArea.setForeground(UIManager.getColor("TextArea.foreground"));
                }
            }

            @Override
            public void focusLost(final java.awt.event.FocusEvent e) {
                if (CustomPromptHistoryWindow.this.inputArea.getText().isEmpty()) {
                    CustomPromptHistoryWindow.this.inputArea.setForeground(JBColor.GRAY);
                    CustomPromptHistoryWindow.this.inputArea.setText("Type your custom prompt here...");
                }
            }
        });

        // Bottom panel with opacity slider and size label
        final JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        
        // Opacity slider panel (left side)
        final JPanel opacityPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        final JLabel opacityLabel = new JLabel("Opacity:");
        opacityLabel.setFont(opacityLabel.getFont().deriveFont(Font.PLAIN, 8.0f));
        final JSlider opacitySlider = new JSlider(SwingConstants.HORIZONTAL, 20, 100, 100);
        opacitySlider.setPreferredSize(new Dimension(80, 16));
        opacitySlider.setMajorTickSpacing(20);
        opacitySlider.setMinorTickSpacing(10);
        opacitySlider.setPaintTicks(true);
        opacitySlider.setPaintLabels(false);
        opacitySlider.setFont(opacitySlider.getFont().deriveFont(Font.PLAIN, 8.0f));
        
        // Add opacity change listener
        opacitySlider.addChangeListener(e -> {
            final float opacity = opacitySlider.getValue() / 100.0f;
            this.setOpacity(opacity);
        });
        
        opacityPanel.add(opacityLabel);
        opacityPanel.add(opacitySlider);
        bottomPanel.add(opacityPanel, BorderLayout.WEST);
        
        // Size label (right side)
        final JLabel sizeLabel = new JLabel();
        sizeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        sizeLabel.setFont(sizeLabel.getFont().deriveFont(Font.PLAIN, 8.0f));
        bottomPanel.add(sizeLabel, BorderLayout.EAST);

        this.add(bottomPanel, BorderLayout.SOUTH);
    }

    private void refreshList() {
        this.listModel.clear();
        final java.util.List<Entry> favs = CustomPromptHistoryManager.getFavorites();
        final java.util.List<Entry> all = CustomPromptHistoryManager.getHistory();
        for (final Entry e : favs) this.listModel.addElement(e);
        for (final Entry e : all) if (!e.favorite) this.listModel.addElement(e);
    }

    private void updateDetailPanel() {
        final Entry entry = this.entryList.getSelectedValue();
        if (null == entry) {
            this.detailArea.setText("");
            this.favButton.setText("Favorite");
            this.favButton.setEnabled(false);
            this.copyPromptButton.setEnabled(false);
            this.copyResultButton.setEnabled(false);
            this.rerunButton.setEnabled(false);
            this.deleteButton.setEnabled(false);
            return;
        }
        final String sb = "Prompt:\n" + entry.prompt + "\n\n" +
                "Result:\n" + entry.result;
        this.detailArea.setText(sb);
        this.favButton.setText(entry.favorite ? "Unfavorite" : "Favorite");
        this.favButton.setEnabled(true);
        this.copyPromptButton.setEnabled(true);
        this.copyResultButton.setEnabled(true);
        this.rerunButton.setEnabled(true);
        this.deleteButton.setEnabled(true);
    }

    private void toggleFavorite() {
        final int idx = this.entryList.getSelectedIndex();
        if (0 > idx) return;
        CustomPromptHistoryManager.toggleFavorite(idx);
        this.refreshList();
        this.entryList.setSelectedIndex(idx);
    }

    private void copySelectedPrompt() {
        final Entry entry = this.entryList.getSelectedValue();
        if (null == entry) return;
        final StringSelection sel = new StringSelection(entry.prompt);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, sel);
    }

    private void copySelectedResult() {
        final Entry entry = this.entryList.getSelectedValue();
        if (null == entry) return;
        final StringSelection sel = new StringSelection(entry.result);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, sel);
    }

    private void rerunSelectedPrompt() {
        final Entry entry = this.entryList.getSelectedValue();
        if (null == entry) return;
        // Simulate send: fill input field and trigger send
        this.inputArea.setText(entry.prompt);
        this.sendButton.doClick();
    }

    private void deleteSelectedEntry() {
        final int idx = this.entryList.getSelectedIndex();
        if (0 > idx) return;
        // Remove from file and list
        CustomPromptHistoryManager.deleteEntry(idx);
        this.refreshList();
        if (this.listModel.isEmpty()) {
            this.detailArea.setText("");
            this.favButton.setEnabled(false);
            this.copyPromptButton.setEnabled(false);
            this.copyResultButton.setEnabled(false);
            this.rerunButton.setEnabled(false);
            this.deleteButton.setEnabled(false);
        } else {
            this.entryList.setSelectedIndex(Math.min(idx, this.listModel.size() - 1));
        }
    }

    public String getInputPrompt() {
        return this.inputArea.getText();
    }

    // Custom cell renderer for modern look
    private static class EntryCellRenderer implements ListCellRenderer<Entry> {

        @Override
        public Component getListCellRendererComponent(final JList<? extends Entry> list, final Entry entry, final int index, final boolean isSelected, final boolean cellHasFocus) {
            final JTextArea area = new JTextArea();
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            area.setOpaque(true);
            area.setEditable(false);
            area.setFont(list.getFont().deriveFont(null != entry && entry.favorite ? Font.BOLD : Font.PLAIN));
            area.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            area.setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
            area.setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
            if (null != entry) {
                final String text = (entry.favorite ? "★ " : "  ") + new java.util.Date(entry.timestamp) + "\n" +
                        (60 < entry.prompt.length() ? entry.prompt.substring(0, 60) + "..." : entry.prompt.replaceAll("\n", " "));
                area.setText(text);
            }
            area.setPreferredSize(new Dimension(260, area.getPreferredSize().height));
            return area;
        }
    }
} 