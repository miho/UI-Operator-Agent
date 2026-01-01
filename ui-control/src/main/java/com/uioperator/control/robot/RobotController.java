package com.uioperator.control.robot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe wrapper around java.awt.Robot for UI automation.
 * All robot operations are synchronized to prevent race conditions.
 */
public class RobotController {

    private static final Logger logger = LoggerFactory.getLogger(RobotController.class);

    private static volatile RobotController instance;
    private static final Object instanceLock = new Object();

    private final Robot robot;
    private final ReentrantLock operationLock = new ReentrantLock();
    private int autoDelay = 50; // Default delay between operations in ms

    private RobotController() throws AWTException {
        this.robot = new Robot();
        this.robot.setAutoDelay(autoDelay);
        this.robot.setAutoWaitForIdle(true);
        logger.info("RobotController initialized");
    }

    /**
     * Get the singleton instance of RobotController.
     */
    public static RobotController getInstance() throws AWTException {
        if (instance == null) {
            synchronized (instanceLock) {
                if (instance == null) {
                    instance = new RobotController();
                }
            }
        }
        return instance;
    }

    /**
     * Set the delay between robot operations.
     */
    public void setAutoDelay(int ms) {
        operationLock.lock();
        try {
            this.autoDelay = ms;
            robot.setAutoDelay(ms);
        } finally {
            operationLock.unlock();
        }
    }

    /**
     * Get the current auto delay.
     */
    public int getAutoDelay() {
        return autoDelay;
    }

    /**
     * Move the mouse to the specified screen coordinates.
     */
    public void mouseMove(int x, int y) {
        operationLock.lock();
        try {
            robot.mouseMove(x, y);
            logger.debug("Mouse moved to ({}, {})", x, y);
        } finally {
            operationLock.unlock();
        }
    }

    /**
     * Press a mouse button.
     *
     * @param buttons InputEvent button mask (e.g., InputEvent.BUTTON1_DOWN_MASK)
     */
    public void mousePress(int buttons) {
        operationLock.lock();
        try {
            robot.mousePress(buttons);
            logger.debug("Mouse button pressed: {}", buttons);
        } finally {
            operationLock.unlock();
        }
    }

    /**
     * Release a mouse button.
     *
     * @param buttons InputEvent button mask
     */
    public void mouseRelease(int buttons) {
        operationLock.lock();
        try {
            robot.mouseRelease(buttons);
            logger.debug("Mouse button released: {}", buttons);
        } finally {
            operationLock.unlock();
        }
    }

    /**
     * Perform a complete mouse click (press and release).
     */
    public void mouseClick(int buttons) {
        operationLock.lock();
        try {
            robot.mousePress(buttons);
            robot.mouseRelease(buttons);
            logger.debug("Mouse clicked: {}", buttons);
        } finally {
            operationLock.unlock();
        }
    }

    /**
     * Rotate the mouse scroll wheel.
     *
     * @param wheelAmt positive for scroll down, negative for scroll up
     */
    public void mouseWheel(int wheelAmt) {
        operationLock.lock();
        try {
            robot.mouseWheel(wheelAmt);
            logger.debug("Mouse wheel rotated: {}", wheelAmt);
        } finally {
            operationLock.unlock();
        }
    }

    /**
     * Press a keyboard key.
     *
     * @param keycode KeyEvent virtual key code (e.g., KeyEvent.VK_A)
     */
    public void keyPress(int keycode) {
        operationLock.lock();
        try {
            robot.keyPress(keycode);
            logger.debug("Key pressed: {}", keycode);
        } finally {
            operationLock.unlock();
        }
    }

    /**
     * Release a keyboard key.
     *
     * @param keycode KeyEvent virtual key code
     */
    public void keyRelease(int keycode) {
        operationLock.lock();
        try {
            robot.keyRelease(keycode);
            logger.debug("Key released: {}", keycode);
        } finally {
            operationLock.unlock();
        }
    }

    /**
     * Press and release a key.
     */
    public void keyType(int keycode) {
        operationLock.lock();
        try {
            robot.keyPress(keycode);
            robot.keyRelease(keycode);
            logger.debug("Key typed: {}", keycode);
        } finally {
            operationLock.unlock();
        }
    }

    /**
     * Capture a screenshot of the specified screen region.
     */
    public BufferedImage createScreenCapture(Rectangle screenRect) {
        operationLock.lock();
        try {
            BufferedImage capture = robot.createScreenCapture(screenRect);
            logger.debug("Screenshot captured: {}x{} at ({}, {})",
                    screenRect.width, screenRect.height, screenRect.x, screenRect.y);
            return capture;
        } finally {
            operationLock.unlock();
        }
    }

    /**
     * Capture a screenshot of the entire primary screen.
     */
    public BufferedImage captureFullScreen() {
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        Rectangle bounds = gd.getDefaultConfiguration().getBounds();
        return createScreenCapture(bounds);
    }

    /**
     * Capture a screenshot of a specific monitor.
     *
     * @param monitorIndex 0 for primary, 1+ for additional monitors
     */
    public BufferedImage captureScreen(int monitorIndex) {
        GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        if (monitorIndex < 0 || monitorIndex >= devices.length) {
            throw new IllegalArgumentException("Invalid monitor index: " + monitorIndex);
        }
        Rectangle bounds = devices[monitorIndex].getDefaultConfiguration().getBounds();
        return createScreenCapture(bounds);
    }

    /**
     * Get the current mouse location.
     */
    public Point getMouseLocation() {
        return MouseInfo.getPointerInfo().getLocation();
    }

    /**
     * Get screen bounds for the primary monitor.
     */
    public Rectangle getPrimaryScreenBounds() {
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        return gd.getDefaultConfiguration().getBounds();
    }

    /**
     * Get screen bounds for all monitors combined (virtual screen).
     */
    public Rectangle getVirtualScreenBounds() {
        Rectangle virtualBounds = new Rectangle();
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        for (GraphicsDevice gd : ge.getScreenDevices()) {
            virtualBounds = virtualBounds.union(gd.getDefaultConfiguration().getBounds());
        }
        return virtualBounds;
    }

    /**
     * Get the number of available monitors.
     */
    public int getMonitorCount() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices().length;
    }

    /**
     * Wait for all events to be processed.
     */
    public void waitForIdle() {
        operationLock.lock();
        try {
            robot.waitForIdle();
        } finally {
            operationLock.unlock();
        }
    }

    /**
     * Delay execution for the specified time.
     */
    public void delay(int ms) {
        operationLock.lock();
        try {
            robot.delay(ms);
        } finally {
            operationLock.unlock();
        }
    }
}
