package com.uioperator.control.robot;

import com.uioperator.common.model.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.AWTException;
import java.awt.event.InputEvent;

/**
 * High-level mouse operations using RobotController.
 * Provides click, drag, scroll, and movement operations with modifier support.
 */
public class MouseController {

    private static final Logger logger = LoggerFactory.getLogger(MouseController.class);

    // Mouse button constants
    public static final int BUTTON_LEFT = InputEvent.BUTTON1_DOWN_MASK;
    public static final int BUTTON_MIDDLE = InputEvent.BUTTON2_DOWN_MASK;
    public static final int BUTTON_RIGHT = InputEvent.BUTTON3_DOWN_MASK;

    private final RobotController robot;
    private final KeyboardController keyboard;

    public MouseController() throws AWTException {
        this.robot = RobotController.getInstance();
        this.keyboard = new KeyboardController();
    }

    /**
     * Move mouse to the specified coordinates.
     */
    public void moveTo(int x, int y) {
        robot.mouseMove(x, y);
    }

    /**
     * Move mouse to the specified point.
     */
    public void moveTo(Point point) {
        moveTo(point.getX(), point.getY());
    }

    /**
     * Get current mouse position.
     */
    public Point getPosition() {
        java.awt.Point pos = robot.getMouseLocation();
        return new Point(pos.x, pos.y);
    }

    /**
     * Perform a left click at the current position.
     */
    public void leftClick() {
        robot.mouseClick(BUTTON_LEFT);
    }

    /**
     * Perform a left click at the specified coordinates.
     */
    public void leftClick(int x, int y) {
        moveTo(x, y);
        robot.mouseClick(BUTTON_LEFT);
    }

    /**
     * Perform a left click with modifiers.
     */
    public void leftClick(int x, int y, boolean shift, boolean ctrl, boolean alt) {
        moveTo(x, y);
        clickWithModifiers(BUTTON_LEFT, shift, ctrl, alt);
    }

    /**
     * Perform a right click at the current position.
     */
    public void rightClick() {
        robot.mouseClick(BUTTON_RIGHT);
    }

    /**
     * Perform a right click at the specified coordinates.
     */
    public void rightClick(int x, int y) {
        moveTo(x, y);
        robot.mouseClick(BUTTON_RIGHT);
    }

    /**
     * Perform a right click with modifiers.
     */
    public void rightClick(int x, int y, boolean shift, boolean ctrl, boolean alt) {
        moveTo(x, y);
        clickWithModifiers(BUTTON_RIGHT, shift, ctrl, alt);
    }

    /**
     * Perform a middle click at the current position.
     */
    public void middleClick() {
        robot.mouseClick(BUTTON_MIDDLE);
    }

    /**
     * Perform a middle click at the specified coordinates.
     */
    public void middleClick(int x, int y) {
        moveTo(x, y);
        robot.mouseClick(BUTTON_MIDDLE);
    }

    /**
     * Perform a double click at the current position.
     */
    public void doubleClick() {
        robot.mouseClick(BUTTON_LEFT);
        robot.delay(50);
        robot.mouseClick(BUTTON_LEFT);
    }

    /**
     * Perform a double click at the specified coordinates.
     */
    public void doubleClick(int x, int y) {
        moveTo(x, y);
        doubleClick();
    }

    /**
     * Perform a double click with modifiers.
     */
    public void doubleClick(int x, int y, boolean shift, boolean ctrl, boolean alt) {
        moveTo(x, y);
        pressModifiers(shift, ctrl, alt);
        try {
            robot.mouseClick(BUTTON_LEFT);
            robot.delay(50);
            robot.mouseClick(BUTTON_LEFT);
        } finally {
            releaseModifiers(shift, ctrl, alt);
        }
    }

    /**
     * Press the left mouse button (without releasing).
     */
    public void pressLeft() {
        robot.mousePress(BUTTON_LEFT);
    }

    /**
     * Release the left mouse button.
     */
    public void releaseLeft() {
        robot.mouseRelease(BUTTON_LEFT);
    }

    /**
     * Press the right mouse button (without releasing).
     */
    public void pressRight() {
        robot.mousePress(BUTTON_RIGHT);
    }

    /**
     * Release the right mouse button.
     */
    public void releaseRight() {
        robot.mouseRelease(BUTTON_RIGHT);
    }

    /**
     * Press the middle mouse button (without releasing).
     */
    public void pressMiddle() {
        robot.mousePress(BUTTON_MIDDLE);
    }

    /**
     * Release the middle mouse button.
     */
    public void releaseMiddle() {
        robot.mouseRelease(BUTTON_MIDDLE);
    }

    /**
     * Scroll the mouse wheel.
     *
     * @param amount positive = scroll down, negative = scroll up
     */
    public void scroll(int amount) {
        robot.mouseWheel(amount);
    }

    /**
     * Scroll the mouse wheel at a specific position.
     */
    public void scroll(int x, int y, int amount) {
        moveTo(x, y);
        robot.mouseWheel(amount);
    }

    /**
     * Drag from the current position to the target position.
     *
     * @param endX target X coordinate
     * @param endY target Y coordinate
     * @param button mouse button to use (BUTTON_LEFT, BUTTON_RIGHT, BUTTON_MIDDLE)
     * @param durationMs how long the drag should take in milliseconds
     */
    public void drag(int endX, int endY, int button, int durationMs) {
        Point start = getPosition();
        drag(start.getX(), start.getY(), endX, endY, button, durationMs);
    }

    /**
     * Drag from start position to end position.
     *
     * @param startX starting X coordinate
     * @param startY starting Y coordinate
     * @param endX ending X coordinate
     * @param endY ending Y coordinate
     * @param button mouse button to use
     * @param durationMs how long the drag should take
     */
    public void drag(int startX, int startY, int endX, int endY, int button, int durationMs) {
        logger.debug("Dragging from ({}, {}) to ({}, {}) over {}ms",
                startX, startY, endX, endY, durationMs);

        // Move to start position
        moveTo(startX, startY);
        robot.delay(50);

        // Press button
        robot.mousePress(button);
        robot.delay(50);

        // Calculate steps for smooth movement
        int steps = Math.max(10, durationMs / 10);
        int delayPerStep = durationMs / steps;

        double dx = (double) (endX - startX) / steps;
        double dy = (double) (endY - startY) / steps;

        // Smooth movement
        for (int i = 1; i <= steps; i++) {
            int x = startX + (int) (dx * i);
            int y = startY + (int) (dy * i);
            moveTo(x, y);
            robot.delay(delayPerStep);
        }

        // Ensure we end at exact position
        moveTo(endX, endY);
        robot.delay(50);

        // Release button
        robot.mouseRelease(button);
        logger.debug("Drag complete");
    }

    /**
     * Perform a click with keyboard modifiers.
     */
    private void clickWithModifiers(int button, boolean shift, boolean ctrl, boolean alt) {
        pressModifiers(shift, ctrl, alt);
        try {
            robot.mouseClick(button);
        } finally {
            releaseModifiers(shift, ctrl, alt);
        }
    }

    /**
     * Press modifier keys.
     */
    private void pressModifiers(boolean shift, boolean ctrl, boolean alt) {
        if (shift) keyboard.pressKey("Shift");
        if (ctrl) keyboard.pressKey("Ctrl");
        if (alt) keyboard.pressKey("Alt");
    }

    /**
     * Release modifier keys.
     */
    private void releaseModifiers(boolean shift, boolean ctrl, boolean alt) {
        if (alt) keyboard.releaseKey("Alt");
        if (ctrl) keyboard.releaseKey("Ctrl");
        if (shift) keyboard.releaseKey("Shift");
    }

    /**
     * Parse a button name to a button constant.
     */
    public static int parseButton(String buttonName) {
        if (buttonName == null) return BUTTON_LEFT;
        switch (buttonName.toLowerCase()) {
            case "left":
                return BUTTON_LEFT;
            case "middle":
                return BUTTON_MIDDLE;
            case "right":
                return BUTTON_RIGHT;
            default:
                return BUTTON_LEFT;
        }
    }
}
