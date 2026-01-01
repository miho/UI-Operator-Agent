package com.uioperator.control.robot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.AWTException;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * High-level keyboard operations using RobotController.
 * Provides key press, release, type, and combo operations.
 */
public class KeyboardController {

    private static final Logger logger = LoggerFactory.getLogger(KeyboardController.class);

    private final RobotController robot;
    private final Map<String, Integer> keyMap;

    public KeyboardController() throws AWTException {
        this.robot = RobotController.getInstance();
        this.keyMap = buildKeyMap();
    }

    /**
     * Press a key by name (without releasing).
     *
     * @param keyName the key name (e.g., "A", "Enter", "Ctrl", "F1")
     */
    public void pressKey(String keyName) {
        int keyCode = getKeyCode(keyName);
        robot.keyPress(keyCode);
    }

    /**
     * Release a key by name.
     *
     * @param keyName the key name
     */
    public void releaseKey(String keyName) {
        int keyCode = getKeyCode(keyName);
        robot.keyRelease(keyCode);
    }

    /**
     * Press and release a key.
     *
     * @param keyName the key name
     */
    public void tapKey(String keyName) {
        int keyCode = getKeyCode(keyName);
        robot.keyType(keyCode);
    }

    /**
     * Type a string of text with default delay between keystrokes.
     *
     * @param text the text to type
     */
    public void type(String text) {
        type(text, 50);
    }

    /**
     * Type a string of text with specified delay between keystrokes.
     *
     * @param text the text to type
     * @param delayMs delay between each keystroke in milliseconds
     */
    public void type(String text, int delayMs) {
        logger.debug("Typing text: '{}' with {}ms delay", text, delayMs);
        for (char c : text.toCharArray()) {
            typeChar(c);
            if (delayMs > 0) {
                robot.delay(delayMs);
            }
        }
    }

    /**
     * Execute a key combination (e.g., Ctrl+C, Alt+Tab).
     *
     * @param keys list of key names to press together
     */
    public void combo(List<String> keys) {
        logger.debug("Executing key combo: {}", keys);

        // Press all keys in order
        for (String key : keys) {
            pressKey(key);
        }

        robot.delay(50);

        // Release all keys in reverse order
        for (int i = keys.size() - 1; i >= 0; i--) {
            releaseKey(keys.get(i));
        }
    }

    /**
     * Execute a key combination (e.g., "Ctrl+C").
     *
     * @param comboString keys separated by '+' (e.g., "Ctrl+Shift+S")
     */
    public void combo(String comboString) {
        String[] parts = comboString.split("\\+");
        combo(List.of(parts));
    }

    /**
     * Type a single character.
     */
    private void typeChar(char c) {
        // Check if character requires shift
        boolean needsShift = Character.isUpperCase(c) || isShiftedChar(c);

        // Get the base key code
        int keyCode = getKeyCodeForChar(c);

        if (needsShift) {
            robot.keyPress(KeyEvent.VK_SHIFT);
        }

        try {
            robot.keyPress(keyCode);
            robot.keyRelease(keyCode);
        } finally {
            if (needsShift) {
                robot.keyRelease(KeyEvent.VK_SHIFT);
            }
        }
    }

    /**
     * Check if character requires shift key.
     */
    private boolean isShiftedChar(char c) {
        return "~!@#$%^&*()_+{}|:\"<>?".indexOf(c) >= 0;
    }

    /**
     * Get key code for a character.
     */
    private int getKeyCodeForChar(char c) {
        // Handle uppercase letters
        if (Character.isUpperCase(c)) {
            return KeyEvent.getExtendedKeyCodeForChar(Character.toLowerCase(c));
        }

        // Handle shifted characters - return the base key
        switch (c) {
            case '~': return KeyEvent.VK_BACK_QUOTE;
            case '!': return KeyEvent.VK_1;
            case '@': return KeyEvent.VK_2;
            case '#': return KeyEvent.VK_3;
            case '$': return KeyEvent.VK_4;
            case '%': return KeyEvent.VK_5;
            case '^': return KeyEvent.VK_6;
            case '&': return KeyEvent.VK_7;
            case '*': return KeyEvent.VK_8;
            case '(': return KeyEvent.VK_9;
            case ')': return KeyEvent.VK_0;
            case '_': return KeyEvent.VK_MINUS;
            case '+': return KeyEvent.VK_EQUALS;
            case '{': return KeyEvent.VK_OPEN_BRACKET;
            case '}': return KeyEvent.VK_CLOSE_BRACKET;
            case '|': return KeyEvent.VK_BACK_SLASH;
            case ':': return KeyEvent.VK_SEMICOLON;
            case '"': return KeyEvent.VK_QUOTE;
            case '<': return KeyEvent.VK_COMMA;
            case '>': return KeyEvent.VK_PERIOD;
            case '?': return KeyEvent.VK_SLASH;
            default:
                return KeyEvent.getExtendedKeyCodeForChar(c);
        }
    }

    /**
     * Get key code from key name.
     */
    public int getKeyCode(String keyName) {
        String normalized = keyName.trim().toLowerCase();
        Integer keyCode = keyMap.get(normalized);

        if (keyCode != null) {
            return keyCode;
        }

        // Try single character
        if (keyName.length() == 1) {
            return KeyEvent.getExtendedKeyCodeForChar(keyName.charAt(0));
        }

        throw new IllegalArgumentException("Unknown key: " + keyName);
    }

    /**
     * Build the key name to key code mapping.
     */
    private Map<String, Integer> buildKeyMap() {
        Map<String, Integer> map = new HashMap<>();

        // Modifier keys
        map.put("shift", KeyEvent.VK_SHIFT);
        map.put("ctrl", KeyEvent.VK_CONTROL);
        map.put("control", KeyEvent.VK_CONTROL);
        map.put("alt", KeyEvent.VK_ALT);
        map.put("meta", KeyEvent.VK_META);
        map.put("win", KeyEvent.VK_WINDOWS);
        map.put("windows", KeyEvent.VK_WINDOWS);
        map.put("cmd", KeyEvent.VK_META);
        map.put("command", KeyEvent.VK_META);

        // Navigation keys
        map.put("enter", KeyEvent.VK_ENTER);
        map.put("return", KeyEvent.VK_ENTER);
        map.put("tab", KeyEvent.VK_TAB);
        map.put("space", KeyEvent.VK_SPACE);
        map.put("backspace", KeyEvent.VK_BACK_SPACE);
        map.put("delete", KeyEvent.VK_DELETE);
        map.put("del", KeyEvent.VK_DELETE);
        map.put("insert", KeyEvent.VK_INSERT);
        map.put("ins", KeyEvent.VK_INSERT);
        map.put("escape", KeyEvent.VK_ESCAPE);
        map.put("esc", KeyEvent.VK_ESCAPE);

        // Arrow keys
        map.put("up", KeyEvent.VK_UP);
        map.put("down", KeyEvent.VK_DOWN);
        map.put("left", KeyEvent.VK_LEFT);
        map.put("right", KeyEvent.VK_RIGHT);

        // Navigation
        map.put("home", KeyEvent.VK_HOME);
        map.put("end", KeyEvent.VK_END);
        map.put("pageup", KeyEvent.VK_PAGE_UP);
        map.put("pagedown", KeyEvent.VK_PAGE_DOWN);
        map.put("pgup", KeyEvent.VK_PAGE_UP);
        map.put("pgdn", KeyEvent.VK_PAGE_DOWN);

        // Function keys
        for (int i = 1; i <= 24; i++) {
            try {
                int vk = (int) KeyEvent.class.getField("VK_F" + i).get(null);
                map.put("f" + i, vk);
            } catch (Exception e) {
                // F13-F24 may not be available on all platforms
            }
        }

        // Number keys
        for (int i = 0; i <= 9; i++) {
            map.put(String.valueOf(i), KeyEvent.VK_0 + i);
        }

        // Letter keys
        for (char c = 'a'; c <= 'z'; c++) {
            map.put(String.valueOf(c), KeyEvent.VK_A + (c - 'a'));
        }

        // Special characters
        map.put("minus", KeyEvent.VK_MINUS);
        map.put("-", KeyEvent.VK_MINUS);
        map.put("equals", KeyEvent.VK_EQUALS);
        map.put("=", KeyEvent.VK_EQUALS);
        map.put("backslash", KeyEvent.VK_BACK_SLASH);
        map.put("\\", KeyEvent.VK_BACK_SLASH);
        map.put("slash", KeyEvent.VK_SLASH);
        map.put("/", KeyEvent.VK_SLASH);
        map.put("period", KeyEvent.VK_PERIOD);
        map.put(".", KeyEvent.VK_PERIOD);
        map.put("comma", KeyEvent.VK_COMMA);
        map.put(",", KeyEvent.VK_COMMA);
        map.put("semicolon", KeyEvent.VK_SEMICOLON);
        map.put(";", KeyEvent.VK_SEMICOLON);
        map.put("quote", KeyEvent.VK_QUOTE);
        map.put("'", KeyEvent.VK_QUOTE);
        map.put("bracketleft", KeyEvent.VK_OPEN_BRACKET);
        map.put("[", KeyEvent.VK_OPEN_BRACKET);
        map.put("bracketright", KeyEvent.VK_CLOSE_BRACKET);
        map.put("]", KeyEvent.VK_CLOSE_BRACKET);
        map.put("backquote", KeyEvent.VK_BACK_QUOTE);
        map.put("`", KeyEvent.VK_BACK_QUOTE);

        // Lock keys
        map.put("capslock", KeyEvent.VK_CAPS_LOCK);
        map.put("numlock", KeyEvent.VK_NUM_LOCK);
        map.put("scrolllock", KeyEvent.VK_SCROLL_LOCK);

        // Numpad
        map.put("numpad0", KeyEvent.VK_NUMPAD0);
        map.put("numpad1", KeyEvent.VK_NUMPAD1);
        map.put("numpad2", KeyEvent.VK_NUMPAD2);
        map.put("numpad3", KeyEvent.VK_NUMPAD3);
        map.put("numpad4", KeyEvent.VK_NUMPAD4);
        map.put("numpad5", KeyEvent.VK_NUMPAD5);
        map.put("numpad6", KeyEvent.VK_NUMPAD6);
        map.put("numpad7", KeyEvent.VK_NUMPAD7);
        map.put("numpad8", KeyEvent.VK_NUMPAD8);
        map.put("numpad9", KeyEvent.VK_NUMPAD9);
        map.put("multiply", KeyEvent.VK_MULTIPLY);
        map.put("add", KeyEvent.VK_ADD);
        map.put("subtract", KeyEvent.VK_SUBTRACT);
        map.put("decimal", KeyEvent.VK_DECIMAL);
        map.put("divide", KeyEvent.VK_DIVIDE);

        // Print screen
        map.put("printscreen", KeyEvent.VK_PRINTSCREEN);
        map.put("prtsc", KeyEvent.VK_PRINTSCREEN);
        map.put("pause", KeyEvent.VK_PAUSE);

        return map;
    }

    /**
     * Check if a key name is valid.
     */
    public boolean isValidKey(String keyName) {
        try {
            getKeyCode(keyName);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
