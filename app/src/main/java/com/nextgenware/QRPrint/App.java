package com.nextgenware.QRPrint;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.*;
import javax.imageio.ImageIO;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.io.InputStream;
import java.io.File;

public class App extends JFrame {

    // ===== Layout Constants =====
    private static final double LEFT_PAD    = cm(0.5);
    private static final double RIGHT_PAD   = cm(0.5);
    private static final double TOP_PAD     = cm(0.8);   // header area height

    private static final double QR_SIZE     = cm(4.5);   // QR image (square)

    private static final double DIVIDER_GAP = cm(0.2);   // gap between QR and bottom section
    private static final double LOGO_SIZE   = cm(1.2);   // logo square (1:1)
    private static final double LOGO_TEXT_GAP = cm(0.15);
    private static final double BOTTOM_PAD  = cm(0.35);  // space below bottom section

    private static final double INNER_W = QR_SIZE;       // inner content width
    private static final double BOX_W   = INNER_W + LEFT_PAD + RIGHT_PAD;
    private static final double BOX_H   = TOP_PAD + QR_SIZE + DIVIDER_GAP + LOGO_SIZE + BOTTOM_PAD;

    private static final double TOP_MARGIN = cm(3.0);

    // Downloads folder path — works on Windows, Mac, Linux
    private static final String DOWNLOADS = System.getProperty("user.home") + File.separator + "Downloads";

    // Header text
    private static final String HEADER1 = "";
    private static final String HEADER2  = "National Fuel Pass";

    // Shop info lines
    private static final String SHOP1 = "AK Computers & Communication";
    private static final String SHOP2 = "School Junction, Minuwangate";
    private static final String SHOP3 = "0714080640 / 0759609023";

    // ===== State =====
    private BufferedImage qrImage;
    private BufferedImage logoImage;
    private PreviewPanel  previewPanel;
    private JComboBox<String> printerCombo;
    private PrintService[]    printServices;

    /** Loads the bundled logo from src/main/resources/logo.png */
    private static BufferedImage loadLogoFromResources() {
        try (InputStream is = App.class.getResourceAsStream("/logo.png")) {
            if (is == null) {
                System.err.println("Warning: /logo.png not found in resources.");
                return null;
            }
            return ImageIO.read(is);
        } catch (Exception e) {
            System.err.println("Warning: Could not load logo — " + e.getMessage());
            return null;
        }
    }

    public App() {
        setTitle("QR Code Printer");
        setSize(560, 650);
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JButton chooseQRButton = new JButton("QR Code");
        JButton exportButton   = new JButton("PNG");
        JButton printButton    = new JButton("Print");

        // Load logo from bundled resources (src/main/resources/logo.png)
        logoImage = loadLogoFromResources();

        // Printer selector
        printServices = PrintServiceLookup.lookupPrintServices(null, null);
        String[] printerNames = new String[printServices.length];
        for (int i = 0; i < printServices.length; i++) {
            printerNames[i] = printServices[i].getName();
        }
        printerCombo = new JComboBox<>(printerNames);

        PrintService defaultService = PrintServiceLookup.lookupDefaultPrintService();
        if (defaultService != null) {
            for (int i = 0; i < printServices.length; i++) {
                if (printServices[i].getName().equals(defaultService.getName())) {
                    printerCombo.setSelectedIndex(i);
                    break;
                }
            }
        }

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        topPanel.add(chooseQRButton);
        topPanel.add(new JLabel("Printer:"));
        topPanel.add(printerCombo);
        topPanel.add(exportButton);
        topPanel.add(printButton);

        previewPanel = new PreviewPanel();
        previewPanel.setPreferredSize(new Dimension(450, 450));
        previewPanel.setLogoImage(logoImage); // apply bundled logo immediately

        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.add(previewPanel);

        add(topPanel,      BorderLayout.NORTH);
        add(centerWrapper, BorderLayout.CENTER);

        // Choose QR — native OS file picker, default to Downloads folder
        chooseQRButton.addActionListener(e -> {
            FileDialog fd = new FileDialog(this, "Select QR Code Image", FileDialog.LOAD);
            fd.setFile("*.png;*.jpg;*.jpeg;*.bmp;*.gif");
            fd.setDirectory(DOWNLOADS);
            fd.setVisible(true);
            if (fd.getFile() != null) {
                try {
                    qrImage = ImageIO.read(new File(fd.getDirectory() + fd.getFile()));
                    previewPanel.setQRImage(qrImage);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Error loading QR image: " + ex.getMessage());
                }
            }
        });

        // Export PNG — render label to high-res BufferedImage and save
        exportButton.addActionListener(e -> {
            if (qrImage == null) {
                JOptionPane.showMessageDialog(this, "Please select a QR code image first.");
                return;
            }

            // Native save dialog, default to Downloads folder
            FileDialog fd = new FileDialog(this, "Save PNG", FileDialog.SAVE);
            fd.setDirectory(DOWNLOADS);
            fd.setFile("label.png");
            fd.setVisible(true);
            if (fd.getFile() == null) return;

            String savePath = fd.getDirectory() + fd.getFile();
            if (!savePath.toLowerCase().endsWith(".png")) savePath += ".png";

            // Render at 300 DPI — 1 pt = 1/72 inch, so scale = 300/72 ≈ 4.17x
            double dpi   = 300.0;
            double scale = dpi / 72.0;
            int imgW = (int) Math.ceil(BOX_W * scale);
            int imgH = (int) Math.ceil(BOX_H * scale);

            BufferedImage output = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = output.createGraphics();

            // White background
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, imgW, imgH);

            // Apply quality hints
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                    RenderingHints.VALUE_FRACTIONALMETRICS_ON);

            // Scale up to 300 DPI and draw
            g2d.scale(scale, scale);
            drawLabel(g2d, qrImage, logoImage);
            g2d.dispose();

            try {
                ImageIO.write(output, "PNG", new File(savePath));
                JOptionPane.showMessageDialog(this, "Saved: " + savePath);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage());
            }
        });

        // Print — no dialog
        printButton.addActionListener(e -> {
            if (qrImage == null) {
                JOptionPane.showMessageDialog(this, "Please select a QR code image first.");
                return;
            }
            if (printServices.length == 0) {
                JOptionPane.showMessageDialog(this, "No printers found.");
                return;
            }

            PrintService selectedService = printServices[printerCombo.getSelectedIndex()];
            PrinterJob job = PrinterJob.getPrinterJob();

            try {
                job.setPrintService(selectedService);
            } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(this, "Could not use selected printer: " + ex.getMessage());
                return;
            }

            PageFormat pf = job.defaultPage();
            Paper paper   = new Paper();
            double pageW  = cm(14.8);
            double pageH  = cm(21.0);
            paper.setSize(pageW, pageH);
            paper.setImageableArea(0, 0, pageW, pageH);
            pf.setPaper(paper);

            final BufferedImage logoSnap = logoImage; // capture for lambda
            job.setPrintable((g, format, pageIndex) -> {
                if (pageIndex > 0) return Printable.NO_SUCH_PAGE;
                Graphics2D g2d = (Graphics2D) g;
                applyQuality(g2d);
                double x = (format.getWidth() - BOX_W) / 2.0;
                g2d.translate(x, TOP_MARGIN);
                drawLabel(g2d, qrImage, logoSnap);
                return Printable.PAGE_EXISTS;
            }, pf);

            try {
                job.print();
                JOptionPane.showMessageDialog(this, "Sent to: " + selectedService.getName());
            } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(this, "Print failed: " + ex.getMessage());
            }
        });
    }

    // ===== Core Label Renderer =====
    /**
     * Draws the full label from origin (0, 0).
     *
     * Layout top → bottom:
     *   TOP_PAD     — "National Fuel Pass" header
     *   QR_SIZE     — QR code
     *   DIVIDER_GAP — thin separator line
     *   LOGO_SIZE   — [logo square] | [shop info 3 lines]
     *   BOTTOM_PAD
     */
    private void drawLabel(Graphics2D g2d, BufferedImage qr, BufferedImage logo) {
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Outer border
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(1f));
        g2d.draw(new Rectangle2D.Double(0, 0, BOX_W, BOX_H));

        // --- Header ---
        g2d.setFont(new Font("Arial", Font.BOLD, 10));
        FontMetrics fm    = g2d.getFontMetrics();
        int lineH         = fm.getHeight();
        double textBlockH = lineH * 2;
        double baseY      = (TOP_PAD - textBlockH) / 2.0 + fm.getAscent();

        for (String line : new String[]{HEADER1, HEADER2}) {
            double lx = (BOX_W - fm.stringWidth(line)) / 2.0;
            g2d.drawString(line, (int) lx, (int) baseY);
            baseY += lineH;
        }

        // --- QR image ---
        if (qr != null) {
            g2d.drawImage(qr, (int) LEFT_PAD, (int) TOP_PAD, (int) QR_SIZE, (int) QR_SIZE, null);
        }

        // --- Thin divider ---
        double dividerY = TOP_PAD + QR_SIZE + DIVIDER_GAP / 2.0;
        g2d.setColor(new Color(180, 180, 180));
        g2d.setStroke(new BasicStroke(0.5f));
        g2d.draw(new Rectangle2D.Double(LEFT_PAD, dividerY, INNER_W, 0));
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(1f));

        // --- Bottom section ---
        double bottomY = TOP_PAD + QR_SIZE + DIVIDER_GAP;
        double logoX   = LEFT_PAD;
        double logoY   = bottomY;

        // Logo square (1:1)
        if (logo != null) {
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(logo, (int) logoX, (int) logoY, (int) LOGO_SIZE, (int) LOGO_SIZE, null);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        } else {
            // Dashed placeholder box
            float[] dash = {3f, 3f};
            g2d.setStroke(new BasicStroke(0.5f, BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_MITER, 10f, dash, 0f));
            g2d.setColor(Color.GRAY);
            g2d.draw(new Rectangle2D.Double(logoX, logoY, LOGO_SIZE, LOGO_SIZE));
            // "LOGO" placeholder text
            g2d.setFont(new Font("Arial", Font.PLAIN, 6));
            FontMetrics pfm = g2d.getFontMetrics();
            String ph = "LOGO";
            g2d.drawString(ph,
                    (int)(logoX + (LOGO_SIZE - pfm.stringWidth(ph)) / 2.0),
                    (int)(logoY + LOGO_SIZE / 2.0 + pfm.getAscent() / 2.0));
            g2d.setStroke(new BasicStroke(1f));
            g2d.setColor(Color.BLACK);
        }

        // Shop info text — right of logo, vertically centred
        double textColX = LEFT_PAD + LOGO_SIZE + LOGO_TEXT_GAP;
        double textColW = BOX_W - textColX - RIGHT_PAD;

        g2d.setFont(new Font("Arial", Font.PLAIN, 5));
        FontMetrics sfm    = g2d.getFontMetrics();
        int sLineH         = sfm.getHeight();
        String[] shopLines = {SHOP1, SHOP2, SHOP3};

        double shopBlockH = sLineH * shopLines.length;
        double shopY      = bottomY + (LOGO_SIZE - shopBlockH) / 2.0 + sfm.getAscent();

        g2d.setColor(Color.BLACK);
        for (String line : shopLines) {
            String drawn = fitText(sfm, line, (int) textColW);
            g2d.drawString(drawn, (int) textColX, (int) shopY);
            shopY += sLineH;
        }
    }

    /** Truncates text with "…" if wider than maxWidth. */
    private String fitText(FontMetrics fm, String text, int maxWidth) {
        if (fm.stringWidth(text) <= maxWidth) return text;
        String ellipsis = "...";
        while (text.length() > 0 && fm.stringWidth(text + ellipsis) > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text + ellipsis;
    }

    // ===== Helpers =====
    private static double cm(double c) {
        return c * 72.0 / 2.54;
    }

    private void applyQuality(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
    }

    // ===== Preview Panel =====
    class PreviewPanel extends JPanel {

        private BufferedImage qr;
        private BufferedImage logo;

        public void setQRImage(BufferedImage img)   { this.qr   = img; repaint(); }
        public void setLogoImage(BufferedImage img) { this.logo = img; repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;

            if (qr == null) {
                g2d.setColor(Color.LIGHT_GRAY);
                g2d.setFont(new Font("Arial", Font.PLAIN, 13));
                String hint = "Select a QR code image to preview";
                FontMetrics fm = g2d.getFontMetrics();
                g2d.drawString(hint,
                        (getWidth()  - fm.stringWidth(hint)) / 2,
                        getHeight() / 2);
                return;
            }

            double scale = Math.min(
                    (double) getWidth()  / BOX_W,
                    (double) getHeight() / BOX_H
            );

            double tx = (getWidth()  - BOX_W * scale) / 2.0;
            double ty = (getHeight() - BOX_H * scale) / 2.0;

            g2d.translate(tx, ty);
            g2d.scale(scale, scale);

            // Shadow
            g2d.setColor(new Color(0, 0, 0, 20));
            g2d.fill(new Rectangle2D.Double(3, 3, BOX_W, BOX_H));

            // White label background
            g2d.setColor(Color.WHITE);
            g2d.fill(new Rectangle2D.Double(0, 0, BOX_W, BOX_H));

            g2d.setColor(Color.BLACK);
            drawLabel(g2d, qr, logo);
        }
    }

    // ===== Entry Point =====
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new App().setVisible(true));
    }
}