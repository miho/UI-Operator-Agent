package com.uioperator.common.util;

import com.uioperator.common.model.Rectangle;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Utility class for image manipulation operations.
 */
public final class ImageUtil {

    private ImageUtil() {
        // Utility class
    }

    /**
     * Create a copy of a BufferedImage.
     */
    public static BufferedImage copy(BufferedImage source) {
        BufferedImage copy = new BufferedImage(
                source.getWidth(),
                source.getHeight(),
                source.getType()
        );
        Graphics2D g = copy.createGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return copy;
    }

    /**
     * Extract a region from an image.
     */
    public static BufferedImage extractRegion(BufferedImage source, Rectangle region) {
        return source.getSubimage(
                region.getX(),
                region.getY(),
                region.getWidth(),
                region.getHeight()
        );
    }

    /**
     * Extract a region from an image by coordinates.
     */
    public static BufferedImage extractRegion(BufferedImage source, int x, int y, int width, int height) {
        return source.getSubimage(x, y, width, height);
    }

    /**
     * Resize an image to the specified dimensions.
     */
    public static BufferedImage resize(BufferedImage source, int width, int height) {
        BufferedImage resized = new BufferedImage(width, height, source.getType());
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source, 0, 0, width, height, null);
        g.dispose();
        return resized;
    }

    /**
     * Draw a circle on an image.
     */
    public static void drawCircle(BufferedImage image, int centerX, int centerY, int radius, Color color, int thickness) {
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(color);
        g.setStroke(new BasicStroke(thickness));
        g.drawOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
        g.dispose();
    }

    /**
     * Draw a rectangle on an image.
     */
    public static void drawRectangle(BufferedImage image, Rectangle rect, Color color, int thickness) {
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(color);
        g.setStroke(new BasicStroke(thickness));
        g.drawRect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
        g.dispose();
    }

    /**
     * Draw an arrow on an image.
     */
    public static void drawArrow(BufferedImage image, int x1, int y1, int x2, int y2, Color color, int thickness) {
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(color);
        g.setStroke(new BasicStroke(thickness));

        // Draw line
        g.drawLine(x1, y1, x2, y2);

        // Draw arrowhead
        double angle = Math.atan2(y2 - y1, x2 - x1);
        int arrowSize = Math.max(10, thickness * 3);

        int ax1 = (int) (x2 - arrowSize * Math.cos(angle - Math.PI / 6));
        int ay1 = (int) (y2 - arrowSize * Math.sin(angle - Math.PI / 6));
        int ax2 = (int) (x2 - arrowSize * Math.cos(angle + Math.PI / 6));
        int ay2 = (int) (y2 - arrowSize * Math.sin(angle + Math.PI / 6));

        g.fillPolygon(new int[]{x2, ax1, ax2}, new int[]{y2, ay1, ay2}, 3);
        g.dispose();
    }

    /**
     * Draw text on an image.
     */
    public static void drawText(BufferedImage image, String text, int x, int y, Color color, int fontSize) {
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(color);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, fontSize));
        g.drawString(text, x, y);
        g.dispose();
    }

    /**
     * Calculate pixel difference between two images.
     * Returns a value between 0 (identical) and 1 (completely different).
     */
    public static double calculateDifference(BufferedImage img1, BufferedImage img2) {
        if (img1.getWidth() != img2.getWidth() || img1.getHeight() != img2.getHeight()) {
            throw new IllegalArgumentException("Images must have the same dimensions");
        }

        long totalDiff = 0;
        int width = img1.getWidth();
        int height = img1.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb1 = img1.getRGB(x, y);
                int rgb2 = img2.getRGB(x, y);

                int r1 = (rgb1 >> 16) & 0xFF;
                int g1 = (rgb1 >> 8) & 0xFF;
                int b1 = rgb1 & 0xFF;

                int r2 = (rgb2 >> 16) & 0xFF;
                int g2 = (rgb2 >> 8) & 0xFF;
                int b2 = rgb2 & 0xFF;

                totalDiff += Math.abs(r1 - r2) + Math.abs(g1 - g2) + Math.abs(b1 - b2);
            }
        }

        // Max possible difference per pixel is 255*3 = 765
        double maxDiff = (double) width * height * 765;
        return totalDiff / maxDiff;
    }

    /**
     * Create a diff image highlighting differences between two images.
     */
    public static BufferedImage createDiffImage(BufferedImage img1, BufferedImage img2, Color highlightColor) {
        if (img1.getWidth() != img2.getWidth() || img1.getHeight() != img2.getHeight()) {
            throw new IllegalArgumentException("Images must have the same dimensions");
        }

        int width = img1.getWidth();
        int height = img1.getHeight();
        BufferedImage diff = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        int highlightRgb = highlightColor.getRGB();
        int threshold = 30; // Sensitivity threshold

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb1 = img1.getRGB(x, y);
                int rgb2 = img2.getRGB(x, y);

                int r1 = (rgb1 >> 16) & 0xFF;
                int g1 = (rgb1 >> 8) & 0xFF;
                int b1 = rgb1 & 0xFF;

                int r2 = (rgb2 >> 16) & 0xFF;
                int g2 = (rgb2 >> 8) & 0xFF;
                int b2 = rgb2 & 0xFF;

                int pixelDiff = Math.abs(r1 - r2) + Math.abs(g1 - g2) + Math.abs(b1 - b2);

                if (pixelDiff > threshold) {
                    diff.setRGB(x, y, highlightRgb);
                } else {
                    // Grayscale version of original
                    int gray = (r2 + g2 + b2) / 3;
                    diff.setRGB(x, y, 0xFF000000 | (gray << 16) | (gray << 8) | gray);
                }
            }
        }

        return diff;
    }
}
