import java.util.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class TypeRacerMongolian extends JFrame {

    // Default fallback text: excerpt from "Minii nutag" by D. Natsagdorj.
    private static final String DEFAULT_TEXT =
        "Хэнтий Хангай Саяны өндөр сайхан нуруунууд Хойд зүгийн чимэг болсон ой хөвч уулнууд " +
        "Мэнэн Шарга Номины өргөн их говиуд Өмнө зүгийн манлай болсон элсэн манхан далайнууд " +
        "Энэ бол миний төрсөн нутаг Монголын сайхан орон";

    private String targetText = DEFAULT_TEXT;
    private long startTime = 0;
    private boolean isRacing = false;
    private Timer wpmTimer;

    private JTextPane textDisplayPane;
    private JTextField inputField;
    private JTextField urlInputField;
    private JLabel timeValueLabel;
    private JLabel wpmLabel;
    private JLabel accuracyValueLabel;
    private JButton loadPdfButton;
    private RacePanel racePanel;
    private JTextArea leaderboardArea;

    public TypeRacerMongolian() {
        setTitle("Монгол TypeRacer");
        setSize(850, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        initUI();
        setupListeners();
        loadLeaderboard();
    }

    private void initUI() {
        getContentPane().setBackground(new Color(13, 17, 23));
        setLayout(new BorderLayout(16, 16));
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel rootPanel = new JPanel(new BorderLayout(16, 16));
        rootPanel.setOpaque(false);
        add(rootPanel, BorderLayout.CENTER);

        JPanel leftPanel = new JPanel();
        leftPanel.setOpaque(false);
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(13, 17, 23));
        headerPanel.setBorder(new EmptyBorder(0, 0, 4, 0));

        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        JLabel titleLabel = new JLabel("TypeRace");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 30));
        titleLabel.setForeground(new Color(96, 165, 250));
        JLabel subtitleLabel = new JLabel("Horse Edition");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(new Color(148, 163, 184));
        titlePanel.add(titleLabel);
        titlePanel.add(subtitleLabel);

        JPanel statsPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        statsPanel.setOpaque(false);
        timeValueLabel = new JLabel("0.0с", SwingConstants.CENTER);
        wpmLabel = new JLabel("0", SwingConstants.CENTER);
        accuracyValueLabel = new JLabel("100%", SwingConstants.CENTER);
        statsPanel.add(buildStatCard(timeValueLabel, "Хугацаа"));
        statsPanel.add(buildStatCard(wpmLabel, "WPM"));
        statsPanel.add(buildStatCard(accuracyValueLabel, "Нарийвчлал"));

        headerPanel.add(titlePanel, BorderLayout.WEST);
        headerPanel.add(statsPanel, BorderLayout.EAST);
        leftPanel.add(headerPanel);

        JPanel pdfPanel = new JPanel(new BorderLayout(10, 10));
        pdfPanel.setBackground(new Color(20, 25, 33));
        pdfPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(45, 55, 72)),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        JLabel pdfLabel = new JLabel("PDF");
        pdfLabel.setForeground(new Color(148, 163, 184));
        pdfLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        urlInputField = new JTextField("http://scribd.com/document/782507403/Harry-Potter-and-the-Philosophers-Stone");
        styleTextField(urlInputField);
        loadPdfButton = new JButton("Шинэ үг");
        styleButton(loadPdfButton);
        pdfPanel.add(pdfLabel, BorderLayout.WEST);
        pdfPanel.add(urlInputField, BorderLayout.CENTER);
        pdfPanel.add(loadPdfButton, BorderLayout.EAST);
        leftPanel.add(Box.createVerticalStrut(14));
        leftPanel.add(pdfPanel);
        leftPanel.add(Box.createVerticalStrut(14));

        JPanel mainPanel = new JPanel(new BorderLayout(14, 14));
        mainPanel.setOpaque(false);

        racePanel = new RacePanel();
        racePanel.setPreferredSize(new Dimension(0, 145));
        racePanel.setBackground(new Color(20, 25, 33));
        racePanel.setBorder(BorderFactory.createLineBorder(new Color(45, 55, 72)));

        textDisplayPane = new JTextPane();
        textDisplayPane.setEditable(false);
        textDisplayPane.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        textDisplayPane.setForeground(new Color(148, 163, 184));
        textDisplayPane.setBackground(new Color(24, 29, 37));
        textDisplayPane.setMargin(new Insets(18, 18, 18, 18));
        updateTextHighlighting(0);

        JScrollPane textScrollPane = new JScrollPane(textDisplayPane);
        textScrollPane.setBorder(BorderFactory.createLineBorder(new Color(45, 55, 72)));

        JPanel inputPanel = new JPanel(new BorderLayout(0, 10));
        inputPanel.setBackground(new Color(20, 25, 33));
        inputPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(45, 55, 72)),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        JLabel inputTitle = new JLabel("Энд бичнэ үү");
        inputTitle.setForeground(new Color(148, 163, 184));
        inputTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        inputField = new JTextField();
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 22));
        styleTextField(inputField);
        inputPanel.add(inputTitle, BorderLayout.NORTH);
        inputPanel.add(inputField, BorderLayout.CENTER);

        mainPanel.add(racePanel, BorderLayout.NORTH);
        mainPanel.add(textScrollPane, BorderLayout.CENTER);
        mainPanel.add(inputPanel, BorderLayout.SOUTH);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(new Color(20, 25, 33));
        rightPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(45, 55, 72)),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        rightPanel.setPreferredSize(new Dimension(220, 0));
        JLabel leaderboardTitle = new JLabel("Шилдэг оролцогчид");
        leaderboardTitle.setForeground(new Color(148, 163, 184));
        leaderboardTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        leaderboardArea = new JTextArea();
        leaderboardArea.setEditable(false);
        leaderboardArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        leaderboardArea.setForeground(new Color(203, 213, 225));
        leaderboardArea.setBackground(new Color(20, 25, 33));
        leaderboardArea.setBorder(new EmptyBorder(8, 0, 0, 0));
        JScrollPane leaderboardScrollPane = new JScrollPane(leaderboardArea);
        leaderboardScrollPane.setBorder(BorderFactory.createLineBorder(new Color(45, 55, 72)));
        rightPanel.add(leaderboardTitle, BorderLayout.NORTH);
        rightPanel.add(leaderboardScrollPane, BorderLayout.CENTER);

        rootPanel.add(leftPanel, BorderLayout.NORTH);
        rootPanel.add(mainPanel, BorderLayout.CENTER);
        rootPanel.add(rightPanel, BorderLayout.EAST);
    }

    private JPanel buildStatCard(JLabel valueLabel, String caption) {
        JPanel card = new JPanel();
        card.setLayout(new BorderLayout());
        card.setBackground(new Color(20, 25, 33));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(45, 55, 72)),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        valueLabel.setForeground(Color.WHITE);
        valueLabel.setBorder(new EmptyBorder(0, 0, 2, 0));

        JLabel captionLabel = new JLabel(caption, SwingConstants.CENTER);
        captionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        captionLabel.setForeground(new Color(148, 163, 184));

        JPanel inner = new JPanel();
        inner.setOpaque(false);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        valueLabel.setAlignmentX(CENTER_ALIGNMENT);
        captionLabel.setAlignmentX(CENTER_ALIGNMENT);
        inner.add(valueLabel);
        inner.add(captionLabel);

        card.add(inner, BorderLayout.CENTER);
        return card;
    }

    private void styleTextField(JTextField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        field.setForeground(new Color(226, 232, 240));
        field.setBackground(new Color(15, 20, 27));
        field.setCaretColor(new Color(96, 165, 250));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(45, 55, 72)),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
    }

    private void styleButton(JButton button) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(59, 130, 246));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
    }
    
    private void setupListeners() {
        inputField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                verifyTyping();
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                verifyTyping();
            }
            @Override
            public void changedUpdate(DocumentEvent e) {
                verifyTyping();
            }
        });

        loadPdfButton.addActionListener(e -> {
            String urlStr = urlInputField.getText().trim();
            loadPdfButton.setEnabled(false);    
            loadPdfButton.setText("Уншиж байна...");

            new Thread(() -> {
                try {
                    String rawText = loadPdfTextFromUrl(urlStr);
                    String cleanText = rawText.replaceAll("\\s+", " ").trim();
                    if (cleanText.isEmpty()) {
                        throw new Exception("PDF-ээс бичвэр олдсонгүй.");
                    }

                    if (cleanText.length() > 250) {
                        cleanText = cleanText.substring(0, 250);
                    }

                    final String finalizedText = cleanText;
                    SwingUtilities.invokeLater(() -> {
                        targetText = finalizedText;
                        resetRace();
                        JOptionPane.showMessageDialog(this, "PDF амжилттай ачаалагдлаа!");
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "Алдаа гарлаа: " + ex.getMessage(), "Алдаа", JOptionPane.ERROR_MESSAGE);
                        targetText = DEFAULT_TEXT;
                        resetRace();
                    });
                } finally {
                    SwingUtilities.invokeLater(() -> {
                        loadPdfButton.setEnabled(true);
                        loadPdfButton.setText("Шинэ үг");
                    });
                }
            }).start();
        });

        wpmTimer = new Timer(200, e -> updateWpmDisplay());
    }

    private void verifyTyping() {
        String typed = inputField.getText();

        if (!isRacing && !typed.isEmpty()) {
            isRacing = true;
            startTime = System.currentTimeMillis();
            wpmTimer.start();
        }

        if (targetText.startsWith(typed)) {
            inputField.setBackground(new Color(15, 20, 27));
            int matchedChars = typed.length();
            double progress = (double) matchedChars / targetText.length();
            racePanel.updateProgress(progress);
            updateTextHighlighting(matchedChars);
            updateStats();

            if (typed.equals(targetText)) {
                endRace();
            }
        } else {
            inputField.setBackground(new Color(60, 18, 27));
            accuracyValueLabel.setText("0%");
        }
    }

    private void updateWpmDisplay() {
        if (startTime == 0) {
            return;
        }

        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed == 0) {
            return;
        }

        double minutes = elapsed / 60000.0;
        int matchedChars = inputField.getText().length();
        int wpm = (int) ((matchedChars / 5.0) / minutes);
        wpmLabel.setText(String.valueOf(wpm));
        timeValueLabel.setText(String.format("%.1fс", elapsed / 1000.0));
    }

    private void updateTextHighlighting(int completedLength) {
        textDisplayPane.setText(targetText);
        StyledDocument doc = textDisplayPane.getStyledDocument();

        SimpleAttributeSet completedStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(completedStyle, new Color(245, 158, 11));
        StyleConstants.setBackground(completedStyle, new Color(75, 50, 10));

        SimpleAttributeSet remainingStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(remainingStyle, new Color(148, 163, 184));
        StyleConstants.setBackground(remainingStyle, new Color(24, 29, 37));

        doc.setCharacterAttributes(0, completedLength, completedStyle, true);
        doc.setCharacterAttributes(completedLength, targetText.length() - completedLength, remainingStyle, true);
    }

    private void endRace() {
        wpmTimer.stop();
        isRacing = false;
        long elapsed = System.currentTimeMillis() - startTime;
        double minutes = elapsed / 60000.0;
        int finalWpm = (int) ((targetText.length() / 5.0) / minutes);

        wpmLabel.setText(String.valueOf(finalWpm));
        timeValueLabel.setText(String.format("%.1fс", elapsed / 1000.0));
        accuracyValueLabel.setText("100%");
        JOptionPane.showMessageDialog(this, "Баяр хүргэе! Та уралдааныг дуусгалаа.\nХурд: " + finalWpm + " WPM");

        String name = JOptionPane.showInputDialog(this, "Нэрээ оруулна уу:", "Амжилтын самбар", JOptionPane.PLAIN_MESSAGE);
        if (name != null && !name.trim().isEmpty()) {
            LeaderboardManager.save(name.trim(), finalWpm);
            loadLeaderboard();
        }
        resetRace();
    }

    private void resetRace() {
        isRacing = false;
        startTime = 0;
        if (wpmTimer.isRunning()) {
            wpmTimer.stop();
        }
        inputField.setText("");
        inputField.setBackground(new Color(15, 20, 27));
        wpmLabel.setText("0");
        timeValueLabel.setText("0.0с");
        accuracyValueLabel.setText("100%");
        racePanel.updateProgress(0.0);
        updateTextHighlighting(0);
    }

    private void updateStats() {
        String typed = inputField.getText();
        if (typed.isEmpty()) {
            accuracyValueLabel.setText("100%");
        } else if (targetText.startsWith(typed)) {
            accuracyValueLabel.setText("100%");
        } else {
            accuracyValueLabel.setText("0%");
        }
    }

    private void loadLeaderboard() {
        leaderboardArea.setText(LeaderboardManager.getDisplayString());
    }

    private String loadPdfTextFromUrl(String urlStr) throws Exception {
        try {
            Class<?> pdDocumentClass = Class.forName("org.apache.pdfbox.pdmodel.PDDocument");
            Class<?> pdfTextStripperClass = Class.forName("org.apache.pdfbox.text.PDFTextStripper");

            Object document;
            try (InputStream is = new URI(urlStr).toURL().openStream()) {
                document = pdDocumentClass.getMethod("load", InputStream.class).invoke(null, is);
            }

            try {
                Object stripper = pdfTextStripperClass.getConstructor().newInstance();
                return (String) pdfTextStripperClass.getMethod("getText", pdDocumentClass).invoke(stripper, document);
            } finally {
                pdDocumentClass.getMethod("close").invoke(document);
            }
        } catch (ClassNotFoundException ex) {
            throw new Exception("PDF унших PDFBox library олдсонгүй. Maven ашиглаж ажиллуулна уу эсвэл PDFBox jar-уудыг classpath-д нэмнэ үү.");
        }
    }

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        SwingUtilities.invokeLater(() -> {
            TypeRacerMongolian frame = new TypeRacerMongolian();
            frame.setVisible(true);
        });
    }

    class RacePanel extends JPanel {
        private double progress = 0.0;

        public void updateProgress(double progress) {
            this.progress = progress;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            g2.setColor(new Color(230, 210, 180));
            g2.fillRect(10, h / 2 - 15, w - 20, 30);

            g2.setColor(new Color(160, 130, 90));
            g2.drawRect(10, h / 2 - 15, w - 20, 30);

            int finishX = w - 60;
            g2.setColor(Color.RED);
            g2.fillRect(finishX, h / 2 - 35, 4, 50);
            g2.fillPolygon(new int[] {finishX, finishX - 20, finishX}, new int[] {h / 2 - 35, h / 2 - 25, h / 2 - 15}, 3);

            int startX = 20;
            int endX = w - 110;
            int currentX = startX + (int) (progress * (endX - startX));
            int baseLineY = h / 2 + 5;

            g2.setColor(new Color(139, 69, 19));
            g2.fillRect(currentX, baseLineY - 22, 35, 16);

            g2.setColor(Color.DARK_GRAY);
            g2.fillOval(currentX + 3, baseLineY - 8, 12, 12);
            g2.fillOval(currentX + 20, baseLineY - 8, 12, 12);
            g2.setColor(Color.WHITE);
            g2.fillOval(currentX + 7, baseLineY - 4, 4, 4);
            g2.fillOval(currentX + 24, baseLineY - 4, 4, 4);

            g2.setColor(Color.BLACK);
            g2.drawLine(currentX + 35, baseLineY - 10, currentX + 50, baseLineY - 10);

            g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
            g2.drawString("\uD83D\uDC0E", currentX + 45, baseLineY - 6);
        }
    }

    static class LeaderboardManager {
        private static final String FILE_PATH = "leaderboard.txt";

        public static void save(String name, int wpm) {
            try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(FILE_PATH, true), "UTF-8"))) {
                out.println(name + "," + wpm);
            } catch (IOException e) {
                System.err.println("Leaderboard save failed: " + e.getMessage());
            }
        }

        public static String getDisplayString() {
            File file = new File(FILE_PATH);
            if (!file.exists()) {
                return " Мэдээлэл байхгүй байна.";
            }

            List<String[]> entries = new ArrayList<>();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
                String line;
                while ((line = in.readLine()) != null) {
                    String[] tokens = line.split(",");
                    if (tokens.length == 2) {
                        entries.add(tokens);
                    }
                }
            } catch (IOException e) {
                return " Алдаа: Уншиж чадсангүй.";
            }

            entries.sort((a, b) -> Integer.compare(Integer.parseInt(b[1].trim()), Integer.parseInt(a[1].trim())));

            StringBuilder sb = new StringBuilder("  Байр | Нэр | Хурд (WPM)\n");
            sb.append("  -------------------------\n");
            int rank = 1;
            for (String[] entry : entries) {
                sb.append(String.format("   #%d  | %-6s | %s WPM\n", rank++, entry[0], entry[1]));
                if (rank > 10) {
                    break;
                }
            }
            return sb.toString();
        }
    }
}
