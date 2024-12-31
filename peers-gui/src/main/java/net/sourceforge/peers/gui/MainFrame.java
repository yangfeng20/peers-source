/*
    This file is part of Peers, a java SIP softphone.

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
    
    Copyright 2010-2013 Yohann Martineau 
 */

package net.sourceforge.peers.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.*;
import javax.swing.border.Border;

import net.sourceforge.peers.FileLogger;
import net.sourceforge.peers.Logger;
import net.sourceforge.peers.javaxsound.JavaxSoundManager;
import net.sourceforge.peers.media.AbstractSoundManager;
import net.sourceforge.peers.sip.Utils;
import net.sourceforge.peers.sip.transport.SipRequest;
import net.sourceforge.peers.sip.transport.SipResponse;

public class MainFrame implements WindowListener, ActionListener {

    public static void main(final String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI(args);
            }
        });
    }

    private static void createAndShowGUI(String[] args) {
        JFrame.setDefaultLookAndFeelDecorated(true);
        new MainFrame(args);
    }

    private JFrame mainFrame;
    private JPanel mainPanel;
    private JPanel dialerPanel;
    private JTextField uri;
    private JButton actionButton;
    private JLabel statusLabel;

    private EventManager eventManager;
    private Registration registration;
    private Logger logger;

    private JPanel headersPanel;
    private JTextField headerKeyField;
    private JTextField headerValueField;
    private JButton addHeaderButton;
    private JPanel addedHeadersPanel;  // 用于显示已添加的 headers
    private JTextArea headersTextArea; // 可选，用于文本格式输入
    private Map<String, String> headersMap = new HashMap<>(); // 存储 SIP headers


    public MainFrame(final String[] args) {
        String peersHome = Utils.DEFAULT_PEERS_HOME;
        if (args.length > 0) {
            peersHome = args[0];
        }
        logger = new FileLogger(peersHome);
        String lookAndFeelClassName = UIManager.getSystemLookAndFeelClassName();
        try {
            UIManager.setLookAndFeel(lookAndFeelClassName);
        } catch (Exception e) {
            logger.error("cannot change look and feel", e);
        }
        final AbstractSoundManager soundManager = new JavaxSoundManager(
                false, //TODO config.isMediaDebug(),
                logger, peersHome);
        String title = "";
        if (!Utils.DEFAULT_PEERS_HOME.equals(peersHome)) {
            title = peersHome;
        }
        title += "/Peers: SIP User-Agent";
        mainFrame = new JFrame(title);
        mainFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        mainFrame.addWindowListener(this);

        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        dialerPanel = new JPanel();

        uri = new JTextField("sip:", 15);
        uri.addActionListener(this);

        actionButton = new JButton("Call");
        actionButton.addActionListener(this);

        dialerPanel.add(uri);
        dialerPanel.add(actionButton);
        dialerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        statusLabel = new JLabel(title);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        Border border = BorderFactory.createEmptyBorder(0, 2, 2, 2);
        statusLabel.setBorder(border);

        mainPanel.add(dialerPanel);
        mainPanel.add(statusLabel);

        addHeadersUi();

        Container contentPane = mainFrame.getContentPane();
        contentPane.add(mainPanel);

        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("File");
        menu.setMnemonic('F');
        JMenuItem menuItem = new JMenuItem("Exit");
        menuItem.setMnemonic('x');
        menuItem.setActionCommand(EventManager.ACTION_EXIT);

        registration = new Registration(statusLabel, logger);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String peersHome = Utils.DEFAULT_PEERS_HOME;
                if (args.length > 0) {
                    peersHome = args[0];
                }
                eventManager = new EventManager(MainFrame.this,
                        peersHome, logger, soundManager);
                eventManager.register();
            }
        }, "gui-event-manager");
        thread.start();

        try {
            while (eventManager == null) {
                Thread.sleep(50);
            }
        } catch (InterruptedException e) {
            return;
        }
        menuItem.addActionListener(eventManager);
        menu.add(menuItem);
        menuBar.add(menu);

        menu = new JMenu("Edit");
        menu.setMnemonic('E');
        menuItem = new JMenuItem("Account");
        menuItem.setMnemonic('A');
        menuItem.setActionCommand(EventManager.ACTION_ACCOUNT);
        menuItem.addActionListener(eventManager);
        menu.add(menuItem);
        menuItem = new JMenuItem("Preferences");
        menuItem.setMnemonic('P');
        menuItem.setActionCommand(EventManager.ACTION_PREFERENCES);
        menuItem.addActionListener(eventManager);
        menu.add(menuItem);
        menuBar.add(menu);

        menu = new JMenu("Help");
        menu.setMnemonic('H');
        menuItem = new JMenuItem("User manual");
        menuItem.setMnemonic('D');
        menuItem.setActionCommand(EventManager.ACTION_DOCUMENTATION);
        menuItem.addActionListener(eventManager);
        menu.add(menuItem);
        menuItem = new JMenuItem("About");
        menuItem.setMnemonic('A');
        menuItem.setActionCommand(EventManager.ACTION_ABOUT);
        menuItem.addActionListener(eventManager);
        menu.add(menuItem);
        menuBar.add(menu);

        mainFrame.setJMenuBar(menuBar);

        mainFrame.pack();
        mainFrame.setVisible(true);
    }

    private void addHeadersUi() {
        headersPanel = new JPanel();
        headersPanel.setLayout(new BoxLayout(headersPanel, BoxLayout.Y_AXIS));

        // SIP Header key/value 输入框
        headerKeyField = new JTextField(15);
        headerValueField = new JTextField(15);
        addHeaderButton = new JButton("Add Header");

        // 设置输入框的固定高度
        headerKeyField.setPreferredSize(new java.awt.Dimension(150, 30));
        headerValueField.setPreferredSize(new java.awt.Dimension(150, 30));

        addHeaderButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String key = headerKeyField.getText().trim();
                String value = headerValueField.getText().trim();
                if (!key.isEmpty() && !value.isEmpty()) {
                    headersMap.put(key, value); // 将 SIP header 加入 Map
                    headerKeyField.setText("");  // 清空输入框
                    headerValueField.setText(""); // 清空输入框
                    updateAddedHeadersUI(); // 更新UI
                }
            }
        });

        // 用于显示已添加的 headers
        addedHeadersPanel = new JPanel();
        addedHeadersPanel.setLayout(new BoxLayout(addedHeadersPanel, BoxLayout.Y_AXIS));

        // SIP Header 文本框（可选）
        headersTextArea = new JTextArea(5, 20);  // 你可以根据需要调整大小
        headersTextArea.setLineWrap(true);
        headersTextArea.setWrapStyleWord(true);

        // 将组件加入到面板
        //headersPanel.add(new JLabel("SIP Header Key:"));
        //headersPanel.add(headerKeyField);
        //headersPanel.add(new JLabel("SIP Header Value:"));
        //headersPanel.add(headerValueField);
        //headersPanel.add(addHeaderButton);
        //headersPanel.add(new JLabel("Or enter headers in text format:"));
        //headersPanel.add(new JScrollPane(addedHeadersPanel));
        headersPanel.add(new JScrollPane(headersTextArea));

        // 将 headersPanel 加入到主面板
        mainPanel.add(headersPanel);
    }

    // 更新已添加的Headers UI
    private void updateAddedHeadersUI() {
        addedHeadersPanel.removeAll(); // 清空当前显示

        for (Map.Entry<String, String> entry : headersMap.entrySet()) {
            JPanel headerPanel = new JPanel();
            headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.X_AXIS));

            // 显示key和value
            headerPanel.add(new JLabel(entry.getKey() + ": " + entry.getValue()));

            // 删除按钮
            JButton deleteButton = new JButton("Delete");
            deleteButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    headersMap.remove(entry.getKey()); // 从Map中删除该header
                    updateAddedHeadersUI(); // 更新UI
                }
            });
            headerPanel.add(deleteButton);

            addedHeadersPanel.add(headerPanel);
        }

        // 刷新UI
        addedHeadersPanel.revalidate();
        addedHeadersPanel.repaint();
    }

    // window events

    @Override
    public void windowActivated(WindowEvent e) {
    }

    @Override
    public void windowClosed(WindowEvent e) {
        eventManager.windowClosed();
    }

    @Override
    public void windowClosing(WindowEvent e) {
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
    }

    @Override
    public void windowIconified(WindowEvent e) {
    }

    @Override
    public void windowOpened(WindowEvent e) {
    }

    // action event

    @Override
    public void actionPerformed(ActionEvent e) {
        headersMap.clear();
        // 获取 URI
        String uriText = uri.getText();

        // 处理文本框格式输入（如果用户输入了文本格式）
        String textHeaders = headersTextArea.getText().trim();
        if (!textHeaders.isEmpty()) {
            parseTextHeaders(textHeaders);
        }

        // 将 headersMap 传递给 callClicked 方法
        eventManager.callClicked(uriText, headersMap);
    }

    private void parseTextHeaders(String textHeaders) {
        // 按行分割输入的文本
        String[] lines = textHeaders.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.contains(":")) {
                String[] keyValue = line.split(":", 2);
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();
                headersMap.put(key, value); // 更新 Map
            }
        }
    }

    // misc.
    public void setLabelText(String text) {
        statusLabel.setText(text);
        mainFrame.pack();
    }

    public void registerFailed(SipResponse sipResponse) {
        registration.registerFailed();
    }

    public void registerSuccessful(SipResponse sipResponse) {
        registration.registerSuccessful();
    }

    public void registering(SipRequest sipRequest) {
        registration.registerSent();
    }

    public void socketExceptionOnStartup() {
        JOptionPane.showMessageDialog(mainFrame, "peers SIP port " +
                "unavailable, exiting");
        System.exit(1);
    }

}
