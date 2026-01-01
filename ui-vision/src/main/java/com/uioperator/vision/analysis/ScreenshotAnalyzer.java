package com.uioperator.vision.analysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Analyzes screenshots to describe content and find elements.
 *
 * <p>Note: This is a placeholder implementation. In a production system,
 * this would integrate with an LLM or computer vision service for
 * actual image analysis.</p>
 */
public class ScreenshotAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(ScreenshotAnalyzer.class);

    /**
     * Analyze a screenshot and describe its contents.
     *
     * @param screenshot the screenshot to analyze
     * @param prompt optional prompt to guide analysis
     * @return description of the screenshot contents
     */
    public String analyze(BufferedImage screenshot, String prompt) {
        logger.debug("Analyzing screenshot {}x{} with prompt: {}",
                screenshot.getWidth(), screenshot.getHeight(), prompt);

        // Basic image statistics
        StringBuilder analysis = new StringBuilder();
        analysis.append("Screenshot Analysis:\n");
        analysis.append(String.format("  Dimensions: %d x %d pixels\n",
                screenshot.getWidth(), screenshot.getHeight()));

        // Analyze color distribution
        ColorStats stats = analyzeColors(screenshot);
        analysis.append(String.format("  Dominant color: RGB(%d, %d, %d)\n",
                stats.dominantR, stats.dominantG, stats.dominantB));
        analysis.append(String.format("  Average brightness: %.1f%%\n", stats.brightness * 100));

        // Detect if mostly dark or light
        if (stats.brightness < 0.3) {
            analysis.append("  Theme: Appears to be dark mode / dark background\n");
        } else if (stats.brightness > 0.7) {
            analysis.append("  Theme: Appears to be light mode / light background\n");
        }

        // Note about full analysis
        analysis.append("\n[Note: Full semantic analysis requires LLM integration.\n");
        analysis.append(" This provides basic image statistics only.]\n");

        if (prompt != null && !prompt.isEmpty()) {
            analysis.append(String.format("\nPrompt provided: %s\n", prompt));
            analysis.append("(LLM integration required to respond to specific prompts)\n");
        }

        return analysis.toString();
    }

    /**
     * Find an element in the screenshot by description.
     *
     * @param screenshot the screenshot to search
     * @param elementDescription description of the element to find
     * @return location information or null if not found
     */
    public ElementLocation findElement(BufferedImage screenshot, String elementDescription) {
        logger.debug("Searching for element: '{}' in {}x{} screenshot",
                elementDescription, screenshot.getWidth(), screenshot.getHeight());

        // Placeholder - would use computer vision or LLM in production
        return new ElementLocation(
                "Element search requires LLM or computer vision integration.\n" +
                "Description: " + elementDescription + "\n" +
                "Image size: " + screenshot.getWidth() + "x" + screenshot.getHeight(),
                -1, -1, 0, 0, 0.0
        );
    }

    /**
     * Analyze the result of an action by comparing before/after screenshots.
     *
     * @param before screenshot before action
     * @param after screenshot after action
     * @param actionDescription what action was performed
     * @return analysis of what changed
     */
    public String analyzeActionResult(BufferedImage before, BufferedImage after,
                                       String actionDescription) {
        logger.debug("Analyzing action result: '{}'", actionDescription);

        ScreenshotComparator comparator = new ScreenshotComparator();
        ScreenshotComparator.ComparisonResult result = comparator.compare(before, after);

        StringBuilder analysis = new StringBuilder();
        analysis.append("Action Result Analysis:\n");
        analysis.append(String.format("  Action: %s\n", actionDescription != null ? actionDescription : "Unknown"));
        analysis.append("\n");
        analysis.append(result.toString());

        if (result.hasChanges()) {
            analysis.append("\nConclusion: Visual changes detected after action.\n");
            if (result.getChangePercentage() < 5) {
                analysis.append("The changes are minimal (< 5% of screen).\n");
            } else if (result.getChangePercentage() < 25) {
                analysis.append("Moderate changes detected.\n");
            } else {
                analysis.append("Significant changes detected (> 25% of screen).\n");
            }
        } else {
            analysis.append("\nConclusion: No visual changes detected after action.\n");
            analysis.append("This could indicate:\n");
            analysis.append("  - The action had no visible effect\n");
            analysis.append("  - The action failed\n");
            analysis.append("  - Changes occurred outside the captured region\n");
        }

        return analysis.toString();
    }

    /**
     * Analyze color distribution in an image.
     */
    private ColorStats analyzeColors(BufferedImage image) {
        long totalR = 0, totalG = 0, totalB = 0;
        int pixelCount = 0;

        // Sample pixels (every 4th pixel for performance)
        for (int y = 0; y < image.getHeight(); y += 4) {
            for (int x = 0; x < image.getWidth(); x += 4) {
                int rgb = image.getRGB(x, y);
                totalR += (rgb >> 16) & 0xFF;
                totalG += (rgb >> 8) & 0xFF;
                totalB += rgb & 0xFF;
                pixelCount++;
            }
        }

        int avgR = (int) (totalR / pixelCount);
        int avgG = (int) (totalG / pixelCount);
        int avgB = (int) (totalB / pixelCount);
        double brightness = (avgR + avgG + avgB) / (3.0 * 255.0);

        return new ColorStats(avgR, avgG, avgB, brightness);
    }

    private static class ColorStats {
        final int dominantR, dominantG, dominantB;
        final double brightness;

        ColorStats(int r, int g, int b, double brightness) {
            this.dominantR = r;
            this.dominantG = g;
            this.dominantB = b;
            this.brightness = brightness;
        }
    }

    /**
     * Information about a located UI element.
     */
    public static class ElementLocation {
        private final String description;
        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private final double confidence;

        public ElementLocation(String description, int x, int y, int width, int height, double confidence) {
            this.description = description;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.confidence = confidence;
        }

        public String getDescription() {
            return description;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public double getConfidence() {
            return confidence;
        }

        public boolean isFound() {
            return x >= 0 && y >= 0;
        }

        public Point getCenter() {
            return new Point(x + width / 2, y + height / 2);
        }

        @Override
        public String toString() {
            if (!isFound()) {
                return "Element not found: " + description;
            }
            return String.format("Element at (%d, %d), size %dx%d, confidence %.1f%%",
                    x, y, width, height, confidence * 100);
        }
    }
}
