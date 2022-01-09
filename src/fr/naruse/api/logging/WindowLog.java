package fr.naruse.api.logging;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.rtf.RTFEditorKit;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.logging.Level;

public class WindowLog extends JFrame {

    private static final long serialVersionUID = 755062546883568428L;
    private static boolean autoScrolling = true;
    private JPanel contentPane = null;
    private JTabbedPane tabPane = null;
    private JScrollPane launcherScroll = null;
    private ConsoleArea launcherTextArea = null;
    private JScrollPane clientScroll = null;
    private ConsoleArea clientTextArea = null;

    public WindowLog(String title, BufferedImage icon) {
        this();
        this.setIconImage(icon);
        this.setTitle(title);
        this.setVisible(true);
    }

    public WindowLog(String title) {
        this();
        this.setTitle(title);
        this.setVisible(true);
    }

    public WindowLog(){
        this.setSize(new Dimension(880, 520));
        this.setLocationRelativeTo(null);
        this.setContentPane(this.iniContentPane());
        this.setBackground(Color.GRAY);
    }

    private JPanel iniContentPane() {
        if (this.contentPane == null) {
            this.contentPane = new JPanel();
            this.contentPane.setLayout(new BorderLayout());
            final JButton autoScrolling = new JButton("Pause");

            autoScrolling.addActionListener(arg0 -> {
                WindowLog.autoScrolling = !WindowLog.autoScrolling;
                autoScrolling.setText(WindowLog.autoScrolling ? "Pause" : "Play");
            });

            this.contentPane.add(autoScrolling, BorderLayout.SOUTH);
            this.contentPane.add(this.getTabPane(), BorderLayout.CENTER);
        }

        return this.contentPane;
    }

    public JTabbedPane getTabPane() {
        if (this.tabPane == null) {
            this.tabPane = new JTabbedPane();
            this.tabPane.setTabPlacement(SwingConstants.TOP);
            this.tabPane.addTab("Launcher", null, this.getLauncherScroll(), null);
            this.tabPane.addTab("Client", null, this.getClientScroll(), null);
        }

        return this.tabPane;
    }

    public JScrollPane getClientScroll() {
        if (this.clientScroll == null) {
            this.clientScroll = new JScrollPane(this.getClientTextArea());
        }

        return this.clientScroll;
    }

    private JScrollPane getLauncherScroll() {
        if (this.launcherScroll == null) {
            this.launcherScroll = new JScrollPane(this.getLauncherTextArea());
        }

        return this.launcherScroll;
    }

    private ConsoleArea getLauncherTextArea() {
        if (this.launcherTextArea == null) {
            this.launcherTextArea = new ConsoleArea();
            Font font = new Font("Courier New", Font.PLAIN, 12);
            this.launcherTextArea.setFont(font);
            this.launcherTextArea.setForeground(Color.BLACK);
            this.launcherTextArea.setEditable(false);
        }

        return this.launcherTextArea;
    }

    private ConsoleArea getClientTextArea() {
        if (this.clientTextArea == null) {
            this.clientTextArea = new ConsoleArea();
            Font font = new Font("Courier New", Font.PLAIN, 12);
            this.clientTextArea.setFont(font);
            this.clientTextArea.setForeground(Color.BLACK);
            this.clientTextArea.setEditable(false);
        }

        return this.clientTextArea;
    }

    private boolean isFirstClientLog = false;
    public void incomingOutputMessage(Level level, String s) {
        ConsoleArea area = this.getLauncherTextArea();

        if (s.contains("Client> ")) {
            area = this.getClientTextArea();
            s = s.replace("Client> ", "");
            if(isFirstClientLog){
                isFirstClientLog = false;
                this.getTabPane().setSelectedIndex(1);
            }
        }

        if (level == Level.SEVERE) {
            if (area.getLastColor() == Color.BLACK) {
                area.changeLastLine();
            }

            area.append(s, new Color(180, 0, 0));
            this.getTabPane().setSelectedComponent(area == this.getLauncherTextArea() ? this.getLauncherScroll() : this.getClientScroll());
        }else  if (level == Level.WARNING) {
            if (area.getLastColor() == Color.BLACK) {
                area.changeLastLine();
            }

            area.append(s, new Color(180, 180, 0));
            this.getTabPane().setSelectedComponent(area == this.getLauncherTextArea() ? this.getLauncherScroll() : this.getClientScroll());
        } else {
            area.append(s);
        }

        if (WindowLog.autoScrolling) {
            area.setCaretPosition(area.getDocument().getLength());
        }
    }

    public class ConsoleArea extends JTextPane {
        private static final long serialVersionUID = 4043087446338170574L;
        private final SimpleAttributeSet style;
        private String lastMsg = "";
        private Color lastColor = Color.BLACK;
        private int lastLenght = 0;

        public ConsoleArea() {
            this.style = new SimpleAttributeSet();
            this.setContentType("text/rtf");
            this.setEditorKit(new RTFEditorKit());
            this.setBackground(Color.GRAY);
        }

        public void append(String msg) {
            this.append(msg, Color.BLACK, Color.GRAY, false);
        }

        public void append(String msg, Color textColor) {
            this.append(msg, textColor, Color.WHITE, false);
        }

        public void changeLastLine() {
            try {
                this.getDocument().remove(this.lastLenght, this.lastMsg.length());
            } catch (BadLocationException e) {
                e.printStackTrace();
            }

            this.append(this.lastMsg, Color.RED);
        }

        public void append(String msg, Color color, Color bgColor, boolean isBold) {
            StyleConstants.setForeground(this.style, color);
            StyleConstants.setBackground(this.style, Color.GRAY);
            StyleConstants.setBold(this.style, isBold);
            int len = this.getDocument().getLength();

            try {
                if (!"\n".equals(msg)) {
                    this.lastMsg = msg;
                    this.lastLenght = len;
                    this.lastColor = color;
                }

                this.getDocument().insertString(len, msg, this.style);
            } catch (Exception e) {
                System.out.print("Failed to append msg [" + msg + "]");
            }
        }

        public Color getLastColor() {
            return this.lastColor;
        }
    }

}