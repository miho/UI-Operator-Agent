package com.uioperator.vision.analysis;

import com.uioperator.common.util.ImageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Compares screenshots to detect and visualize differences.
 */
public class ScreenshotComparator {

    private static final Logger logger = LoggerFactory.getLogger(ScreenshotComparator.class);

    private static final int DEFAULT_THRESHOLD = 30;

    /**
     * Compare two screenshots and calculate similarity.
     *
     * @param before the before screenshot
     * @param after the after screenshot
     * @return similarity percentage (0-100)
     */
    public double calculateSimilarity(BufferedImage before, BufferedImage after) {
        double difference = ImageUtil.calculateDifference(before, after);
        return (1.0 - difference) * 100.0;
    }

    /**
     * Compare two screenshots and create a diff image.
     *
     * @param before the before screenshot
     * @param after the after screenshot
     * @param highlightColor color for highlighting differences
     * @return diff image with differences highlighted
     */
    public BufferedImage createDiffImage(BufferedImage before, BufferedImage after, Color highlightColor) {
        return ImageUtil.createDiffImage(before, after, highlightColor);
    }

    /**
     * Compare screenshots and return detailed comparison result.
     */
    public ComparisonResult compare(BufferedImage before, BufferedImage after) {
        return compare(before, after, DEFAULT_THRESHOLD);
    }

    /**
     * Compare screenshots with custom threshold.
     *
     * @param before the before screenshot
     * @param after the after screenshot
     * @param threshold sensitivity threshold (0-255)
     * @return detailed comparison result
     */
    public ComparisonResult compare(BufferedImage before, BufferedImage after, int threshold) {
        if (before.getWidth() != after.getWidth() || before.getHeight() != after.getHeight()) {
            throw new IllegalArgumentException("Images must have the same dimensions");
        }

        int width = before.getWidth();
        int height = before.getHeight();

        int changedPixels = 0;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;

        List<Rectangle> changedRegions = new ArrayList<>();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb1 = before.getRGB(x, y);
                int rgb2 = after.getRGB(x, y);

                if (isPixelDifferent(rgb1, rgb2, threshold)) {
                    changedPixels++;
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }

        double changePercentage = (changedPixels * 100.0) / (width * height);
        Rectangle boundingBox = null;

        if (changedPixels > 0) {
            boundingBox = new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
        }

        logger.debug("Comparison: {}% changed, {} pixels different",
                String.format("%.2f", changePercentage), changedPixels);

        return new ComparisonResult(
                changedPixels,
                changePercentage,
                width * height,
                boundingBox,
                before.getWidth(),
                before.getHeight()
        );
    }

    private boolean isPixelDifferent(int rgb1, int rgb2, int threshold) {
        int r1 = (rgb1 >> 16) & 0xFF;
        int g1 = (rgb1 >> 8) & 0xFF;
        int b1 = rgb1 & 0xFF;

        int r2 = (rgb2 >> 16) & 0xFF;
        int g2 = (rgb2 >> 8) & 0xFF;
        int b2 = rgb2 & 0xFF;

        int diff = Math.abs(r1 - r2) + Math.abs(g1 - g2) + Math.abs(b1 - b2);
        return diff > threshold;
    }

    /**
     * Result of comparing two screenshots.
     */
    public static class ComparisonResult {
        private final int changedPixelCount;
        private final double changePercentage;
        private final int totalPixels;
        private final Rectangle boundingBox;
        private final int width;
        private final int height;

        public ComparisonResult(int changedPixelCount, double changePercentage,
                                int totalPixels, Rectangle boundingBox, int width, int height) {
            this.changedPixelCount = changedPixelCount;
            this.changePercentage = changePercentage;
            this.totalPixels = totalPixels;
            this.boundingBox = boundingBox;
            this.width = width;
            this.height = height;
        }

        public int getChangedPixelCount() {
            return changedPixelCount;
        }

        public double getChangePercentage() {
            return changePercentage;
        }

        public int getTotalPixels() {
            return totalPixels;
        }

        public Rectangle getBoundingBox() {
            return boundingBox;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public boolean hasChanges() {
            return changedPixelCount > 0;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Comparison Result:\n"));
            sb.append(String.format("  Image size: %dx%d\n", width, height));
            sb.append(String.format("  Changed pixels: %d / %d (%.2f%%)\n",
                    changedPixelCount, totalPixels, changePercentage));
            if (boundingBox != null) {
                sb.append(String.format("  Change bounds: x=%d, y=%d, w=%d, h=%d\n",
                        boundingBox.x, boundingBox.y, boundingBox.width, boundingBox.height));
            } else {
                sb.append("  No changes detected\n");
            }
            return sb.toString();
        }
    }
}
