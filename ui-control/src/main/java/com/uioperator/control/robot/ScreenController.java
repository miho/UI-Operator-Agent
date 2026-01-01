package com.uioperator.control.robot;

import com.uioperator.common.model.Point;
import com.uioperator.common.model.Rectangle;
import com.uioperator.common.util.Base64Util;
import com.uioperator.control.grid.GridConfiguration;
import com.uioperator.control.grid.GridResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Controller for screenshot operations.
 * Supports full screen, region, and grid-based captures.
 */
public class ScreenController {

    private static final Logger logger = LoggerFactory.getLogger(ScreenController.class);

    private final RobotController robot;
    private final GridResolver gridResolver;

    public ScreenController() throws AWTException {
        this.robot = RobotController.getInstance();
        this.gridResolver = new GridResolver();
    }

    /**
     * Capture the entire primary screen.
     *
     * @return the screenshot as a BufferedImage
     */
    public BufferedImage captureFullScreen() {
        return robot.captureFullScreen();
    }

    /**
     * Capture a specific monitor.
     *
     * @param monitorIndex 0 for primary, 1+ for additional monitors
     * @return the screenshot as a BufferedImage
     */
    public BufferedImage captureScreen(int monitorIndex) {
        return robot.captureScreen(monitorIndex);
    }

    /**
     * Capture a region by pixel coordinates.
     *
     * @param x X coordinate of top-left corner
     * @param y Y coordinate of top-left corner
     * @param width width of the region
     * @param height height of the region
     * @return the screenshot as a BufferedImage
     */
    public BufferedImage captureRegion(int x, int y, int width, int height) {
        java.awt.Rectangle rect = new java.awt.Rectangle(x, y, width, height);
        return robot.createScreenCapture(rect);
    }

    /**
     * Capture a region by Rectangle.
     */
    public BufferedImage captureRegion(Rectangle region) {
        return captureRegion(region.getX(), region.getY(), region.getWidth(), region.getHeight());
    }

    /**
     * Capture a region around the current mouse cursor.
     *
     * @param width width of the region
     * @param height height of the region
     * @return the screenshot centered on the cursor
     */
    public BufferedImage captureAtCursor(int width, int height) {
        java.awt.Point cursor = robot.getMouseLocation();
        int x = cursor.x - width / 2;
        int y = cursor.y - height / 2;

        // Clamp to screen bounds
        java.awt.Rectangle screenBounds = robot.getPrimaryScreenBounds();
        x = Math.max(screenBounds.x, Math.min(x, screenBounds.x + screenBounds.width - width));
        y = Math.max(screenBounds.y, Math.min(y, screenBounds.y + screenBounds.height - height));

        return captureRegion(x, y, width, height);
    }

    /**
     * Capture a grid cell.
     *
     * @param gridCoordinate the grid coordinate (e.g., "A1", "A1.B3")
     * @return the screenshot of the grid cell
     */
    public BufferedImage captureGridCell(String gridCoordinate) {
        Rectangle bounds = gridResolver.resolveToBounds(gridCoordinate);
        return captureRegion(bounds);
    }

    /**
     * Capture a range of grid cells.
     *
     * @param startGrid starting grid coordinate (e.g., "A1")
     * @param endGrid ending grid coordinate (e.g., "C3")
     * @return the screenshot of the region spanning both cells
     */
    public BufferedImage captureGridRange(String startGrid, String endGrid) {
        Rectangle startBounds = gridResolver.resolveToBounds(startGrid);
        Rectangle endBounds = gridResolver.resolveToBounds(endGrid);

        // Calculate bounding box of both cells
        int x1 = Math.min(startBounds.getX(), endBounds.getX());
        int y1 = Math.min(startBounds.getY(), endBounds.getY());
        int x2 = Math.max(startBounds.getX() + startBounds.getWidth(),
                endBounds.getX() + endBounds.getWidth());
        int y2 = Math.max(startBounds.getY() + startBounds.getHeight(),
                endBounds.getY() + endBounds.getHeight());

        return captureRegion(x1, y1, x2 - x1, y2 - y1);
    }

    /**
     * Capture full screen and return as Base64.
     */
    public String captureFullScreenAsBase64() throws IOException {
        BufferedImage image = captureFullScreen();
        return Base64Util.encodeImage(image, "PNG");
    }

    /**
     * Capture screen and return as Base64.
     */
    public String captureScreenAsBase64(int monitorIndex) throws IOException {
        BufferedImage image = captureScreen(monitorIndex);
        return Base64Util.encodeImage(image, "PNG");
    }

    /**
     * Capture region and return as Base64.
     */
    public String captureRegionAsBase64(int x, int y, int width, int height) throws IOException {
        BufferedImage image = captureRegion(x, y, width, height);
        return Base64Util.encodeImage(image, "PNG");
    }

    /**
     * Capture at cursor and return as Base64.
     */
    public String captureAtCursorAsBase64(int width, int height) throws IOException {
        BufferedImage image = captureAtCursor(width, height);
        return Base64Util.encodeImage(image, "PNG");
    }

    /**
     * Capture grid cell and return as Base64.
     */
    public String captureGridCellAsBase64(String gridCoordinate) throws IOException {
        BufferedImage image = captureGridCell(gridCoordinate);
        return Base64Util.encodeImage(image, "PNG");
    }

    /**
     * Capture grid range and return as Base64.
     */
    public String captureGridRangeAsBase64(String startGrid, String endGrid) throws IOException {
        BufferedImage image = captureGridRange(startGrid, endGrid);
        return Base64Util.encodeImage(image, "PNG");
    }

    /**
     * Get the number of available monitors.
     */
    public int getMonitorCount() {
        return robot.getMonitorCount();
    }

    /**
     * Get the current mouse position.
     */
    public Point getMousePosition() {
        java.awt.Point pos = robot.getMouseLocation();
        return new Point(pos.x, pos.y);
    }

    /**
     * Get primary screen bounds.
     */
    public Rectangle getPrimaryScreenBounds() {
        java.awt.Rectangle bounds = robot.getPrimaryScreenBounds();
        return new Rectangle(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    /**
     * Get virtual screen bounds (all monitors combined).
     */
    public Rectangle getVirtualScreenBounds() {
        java.awt.Rectangle bounds = robot.getVirtualScreenBounds();
        return new Rectangle(bounds.x, bounds.y, bounds.width, bounds.height);
    }
}
