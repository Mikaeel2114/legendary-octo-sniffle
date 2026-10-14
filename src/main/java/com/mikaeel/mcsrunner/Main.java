package com.mikaeel.mcsrunner;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.prefs.Preferences;
import java.util.ArrayList;
import java.util.List;

public final class Main extends JFrame {
    private final JTextField scriptField = new JTextField();
    private final DefaultListModel<String> serverModel = new DefaultListModel<>();
    private final JList<String> serverList = new JList<>(serverModel);
    private final List<Process> processes = new ArrayList<>();
    private final JTextArea log = new JTextArea();
    private final JLabel status = new JLabel(" آماده برای اجرا");
    private final JButton runButton = new JButton("▶  Run server");
    private final JButton stopButton = new JButton("■  Stop");
    private final Preferences prefs = Preferences.userNodeForPackage(Main.class);

    public Main() {
        super("Minecraft Server Runner");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(720, 500));
        setSize(820, 570);
        setLocationRelativeTo(null);
        buildUi();
        loadSavedServers();
        updateButtons();
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBorder(new EmptyBorder(18, 20, 18, 20));
        root.setBackground(new Color(19, 24, 31));
        setContentPane(root);

        JLabel title = new JLabel("Minecraft Server Runner");
        title.setForeground(new Color(235, 244, 238));
        title.setFont(new Font("Dialog", Font.BOLD, 25));
        JLabel subtitle = new JLabel("فایل start.bat را یک‌بار انتخاب کن، بعد با یک کلیک سرور را اجرا کن.");
        subtitle.setForeground(new Color(158, 174, 166));
        subtitle.setFont(new Font("Dialog", Font.PLAIN, 14));
        JPanel heading = new JPanel();
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.setOpaque(false);
        heading.add(title);
        heading.add(Box.createVerticalStrut(5));
        heading.add(subtitle);
        root.add(heading, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setOpaque(false);
        scriptField.setToolTipText("مسیر start.bat یا پوشه‌ی سرور");
        scriptField.setFont(new Font("Dialog", Font.PLAIN, 14));
        scriptField.setBackground(new Color(31, 39, 48));
        scriptField.setForeground(new Color(235, 244, 238));
        scriptField.setCaretColor(Color.WHITE);
        scriptField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(65, 82, 75)), new EmptyBorder(9, 10, 9, 10)));
        JButton browse = new JButton("+ Add server");
        browse.addActionListener(e -> chooseScript());
        top.add(scriptField, BorderLayout.CENTER);
        top.add(browse, BorderLayout.EAST);
        center.add(top, BorderLayout.NORTH);

        serverList.setVisibleRowCount(4);
        serverList.setFont(new Font("Dialog", Font.PLAIN, 14));
        serverList.setBackground(new Color(31, 39, 48));
        serverList.setForeground(new Color(235, 244, 238));
        serverList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        serverList.setBorder(new EmptyBorder(6, 8, 6, 8));
        serverList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && serverList.getSelectedIndex() >= 0)
                scriptField.setText(serverList.getSelectedValue());
        });
        JPanel serversPanel = new JPanel(new BorderLayout(0, 5));
        serversPanel.setOpaque(false);
        JLabel serversTitle = new JLabel("سرورها (چندتا را با Ctrl انتخاب کن)");
        serversTitle.setForeground(new Color(158, 174, 166));
        serversPanel.add(serversTitle, BorderLayout.NORTH);
        serversPanel.add(new JScrollPane(serverList), BorderLayout.CENTER);
        center.add(serversPanel, BorderLayout.CENTER);

        log.setEditable(false);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        log.setLineWrap(true);
        log.setWrapStyleWord(true);
        log.setBackground(new Color(12, 16, 21));
        log.setForeground(new Color(191, 214, 199));
        log.setBorder(new EmptyBorder(12, 12, 12, 12));
        center.add(new JScrollPane(log), BorderLayout.CENTER);
        root.add(center, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(12, 0));
        bottom.setOpaque(false);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        actions.setOpaque(false);
        JButton runAllButton = new JButton("▶  Run all");
        runAllButton.setBackground(new Color(70, 171, 105));
        runAllButton.setForeground(Color.WHITE);
        runAllButton.setFocusPainted(false);
        runAllButton.addActionListener(e -> runAllServers());
        runButton.setText("▶  Run selected");
        runButton.setBackground(new Color(70, 171, 105));
        runButton.setForeground(Color.WHITE);
        runButton.setFocusPainted(false);
        runButton.addActionListener(e -> runServer());
        stopButton.setBackground(new Color(172, 74, 74));
        stopButton.setForeground(Color.WHITE);
        stopButton.setFocusPainted(false);
        stopButton.addActionListener(e -> stopServer());
        actions.add(runButton);
        actions.add(Box.createHorizontalStrut(8));
        actions.add(runAllButton);
        actions.add(Box.createHorizontalStrut(8));
        actions.add(stopButton);
        bottom.add(actions, BorderLayout.WEST);
        status.setForeground(new Color(158, 174, 166));
        bottom.add(status, BorderLayout.CENTER);
        JButton copy = new JButton("Copy log");
        copy.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(log.getText()), null);
            status.setText(" log کپی شد");
        });
        bottom.add(copy, BorderLayout.EAST);
        root.add(bottom, BorderLayout.SOUTH);
        append("Minecraft Server Runner آماده است.");
    }

    private void chooseScript() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("انتخاب start.bat سرور");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            if (!selected.getName().toLowerCase().endsWith(".bat")) {
                JOptionPane.showMessageDialog(this, "لطفاً یک فایل .bat انتخاب کن.", "فایل نامعتبر", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String path = selected.getAbsolutePath();
            if (!serverModel.contains(path)) {
                serverModel.addElement(path);
                saveServers();
                append("سرور اضافه شد: " + selected.getName());
            }
            serverList.setSelectedIndex(serverModel.indexOf(path));
            scriptField.setText(path);
        }
    }

    private void loadSavedServers() {
        int count = prefs.getInt("serverCount", 0);
        for (int i = 0; i < count; i++) {
            String path = prefs.get("server" + i, "");
            if (!path.isBlank() && new File(path).isFile()) serverModel.addElement(path);
        }
        if (!serverModel.isEmpty()) {
            serverList.setSelectedIndex(0);
            scriptField.setText(serverModel.get(0));
        }
    }

    private void saveServers() {
        prefs.putInt("serverCount", serverModel.size());
        for (int i = 0; i < serverModel.size(); i++) prefs.put("server" + i, serverModel.get(i));
    }

    private void runServer() {
        int[] selected = serverList.getSelectedIndices();
        if (selected.length == 0 && !scriptField.getText().trim().isEmpty()) {
            String path = scriptField.getText().trim();
            if (!serverModel.contains(path)) { serverModel.addElement(path); saveServers(); }
            selected = new int[]{serverModel.indexOf(path)};
        }
        for (int index : selected) startServer(serverModel.get(index));
    }

    private void runAllServers() {
        for (int i = 0; i < serverModel.size(); i++) startServer(serverModel.get(i));
    }

    private void startServer(String path) {
        File script = new File(path);
        if (!script.isFile()) { append("پیدا نشد: " + path); return; }
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "call", script.getAbsolutePath());
            pb.directory(script.getParentFile());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            synchronized (processes) { processes.add(process); }
            updateButtons();
            status.setText(" " + processes.size() + " سرور در حال اجراست");
            append("شروع شد: " + script.getName());
            streamOutput(process.getInputStream(), script.getName());
            new Thread(() -> {
                try {
                    int exit = process.waitFor();
                    synchronized (processes) { processes.remove(process); }
                    SwingUtilities.invokeLater(() -> { append(script.getName() + " متوقف شد. exit code: " + exit); updateButtons(); });
                } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            }, "server-waiter").start();
        } catch (IOException ex) { append("خطا در " + script.getName() + ": " + ex.getMessage()); }
    }

    private void streamOutput(InputStream input, String name) {
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String message = "[" + name + "] " + line;
                    SwingUtilities.invokeLater(() -> append(message));
                }
            } catch (IOException ex) { SwingUtilities.invokeLater(() -> append("خطا در log " + name + ": " + ex.getMessage())); }
        }, "server-output").start();
    }

    private void stopServer() {
        synchronized (processes) {
            for (Process process : processes) if (process.isAlive()) process.destroy();
            processes.clear();
        }
        append("درخواست توقف همه‌ی سرورها ارسال شد.");
        updateButtons();
    }

    private void updateButtons() {
        boolean running;
        synchronized (processes) { running = !processes.isEmpty(); }
        runButton.setEnabled(!running);
        stopButton.setEnabled(running);
        scriptField.setEnabled(!running);
        serverList.setEnabled(!running);
    }

    private void append(String text) {
        log.append(text + System.lineSeparator());
        log.setCaretPosition(log.getDocument().getLength());
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }
}
