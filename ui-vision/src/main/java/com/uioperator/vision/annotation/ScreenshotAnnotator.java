package com.uioperator.vision.annotation;

import com.uioperator.common.util.ImageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Adds visual annotations to screenshots.
 * Supports circles, arrows, text, and rectangles.
 */
public class ScreenshotAnnotator {

    private static final Logger logger = LoggerFactory.getLogger(ScreenshotAnnotator.class);

    /**
     * Apply multiple annotations to an image.
     *
     * @param source the source image (will not be modified)
     * @param annotations list of annotations to apply
     * @return a new image with annotations drawn
     */
    public BufferedImage annotate(BufferedImage source, List<Annotation> annotations) {
        BufferedImage result = ImageUtil.copy(source);

        for (Annotation annotation : annotations) {
            applyAnnotation(result, annotation);
        }

        logger.debug("Applied {} annotations to image", annotations.size());
        return result;
    }

    /**
     * Apply a single annotation to an image (modifies in place).
     */
    private void applyAnnotation(BufferedImage image, Annotation annotation) {
        switch (annotation.getType()) {
            case CIRCLE:
                drawCircle(image, annotation);
                break;
            case ARROW:
                drawArrow(image, annotation);
                break;
            case TEXT:
                drawText(image, annotation);
                break;
            case RECTANGLE:
                drawRectangle(image, annotation);
                break;
            case MARKER:
                drawMarker(image, annotation);
                break;
            case LINE:
                drawLine(image, annotation);
                break;
        }
    }

    /**
     * Draw a circle annotation.
     */
    public BufferedImage addCircle(BufferedImage image, int centerX, int centerY,
                                   int radius, Color color, int thickness) {
        BufferedImage result = ImageUtil.copy(image);
        Graphics2D g = result.createGraphics();
        setupGraphics(g);
        g.setColor(color);
        g.setStroke(new BasicStroke(thickness));
        g.drawOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
        g.dispose();
        return result;
    }

    private void drawCircle(BufferedImage image, Annotation a) {
        Graphics2D g = image.createGraphics();
        setupGraphics(g);
        g.setColor(a.getColor());
        g.setStroke(new BasicStroke(a.getThickness()));
        int r = a.getRadius();
        g.drawOval(a.getX() - r, a.getY() - r, r * 2, r * 2);
        g.dispose();
    }

    /**
     * Draw an arrow annotation.
     */
    public BufferedImage addArrow(BufferedImage image, int x1, int y1, int x2, int y2,
                                  Color color, int thickness) {
        BufferedImage result = ImageUtil.copy(image);
        ImageUtil.drawArrow(result, x1, y1, x2, y2, color, thickness);
        return result;
    }

    private void drawArrow(BufferedImage image, Annotation a) {
        ImageUtil.drawArrow(image, a.getX(), a.getY(), a.getX2(), a.getY2(),
                a.getColor(), a.getThickness());
    }

    /**
     * Draw a text annotation.
     */
    public BufferedImage addText(BufferedImage image, String text, int x, int y,
                                 Color color, int fontSize) {
        BufferedImage result = ImageUtil.copy(image);
        ImageUtil.drawText(result, text, x, y, color, fontSize);
        return result;
    }

    private void drawText(BufferedImage image, Annotation a) {
        ImageUtil.drawText(image, a.getText(), a.getX(), a.getY(),
                a.getColor(), a.getFontSize());
    }

    /**
     * Draw a rectangle annotation.
     */
    public BufferedImage addRectangle(BufferedImage image, int x, int y, int width, int height,
                                      Color color, int thickness) {
        BufferedImage result = ImageUtil.copy(image);
        Graphics2D g = result.createGraphics();
        setupGraphics(g);
        g.setColor(color);
        g.setStroke(new BasicStroke(thickness));
        g.drawRect(x, y, width, height);
        g.dispose();
        return result;
    }

    private void drawRectangle(BufferedImage image, Annotation a) {
        Graphics2D g = image.createGraphics();
        setupGraphics(g);
        g.setColor(a.getColor());
        g.setStroke(new BasicStroke(a.getThickness()));
        g.drawRect(a.getX(), a.getY(), a.getWidth(), a.getHeight());
        g.dispose();
    }

    /**
     * Draw a filled marker (small filled circle).
     */
    public BufferedImage addMarker(BufferedImage image, int x, int y, int radius, Color color) {
        BufferedImage result = ImageUtil.copy(image);
        Graphics2D g = result.createGraphics();
        setupGraphics(g);
        g.setColor(color);
        g.fillOval(x - radius, y - radius, radius * 2, radius * 2);
        g.dispose();
        return result;
    }

    private void drawMarker(BufferedImage image, Annotation a) {
        Graphics2D g = image.createGraphics();
        setupGraphics(g);
        g.setColor(a.getColor());
        int r = a.getRadius();
        g.fillOval(a.getX() - r, a.getY() - r, r * 2, r * 2);
        g.dispose();
    }

    /**
     * Draw a line between two points.
     */
    public BufferedImage addLine(BufferedImage image, int x1, int y1, int x2, int y2,
                                 Color color, int thickness) {
        BufferedImage result = ImageUtil.copy(image);
        Graphics2D g = result.createGraphics();
        setupGraphics(g);
        g.setColor(color);
        g.setStroke(new BasicStroke(thickness));
        g.drawLine(x1, y1, x2, y2);
        g.dispose();
        return result;
    }

    private void drawLine(BufferedImage image, Annotation a) {
        Graphics2D g = image.createGraphics();
        setupGraphics(g);
        g.setColor(a.getColor());
        g.setStroke(new BasicStroke(a.getThickness()));
        g.drawLine(a.getX(), a.getY(), a.getX2(), a.getY2());
        g.dispose();
    }

    private void setupGraphics(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }
}
