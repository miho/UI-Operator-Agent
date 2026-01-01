package com.uioperator.control.grid;

import com.uioperator.common.model.Rectangle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Configuration for the screen grid system.
 * Supports configurable NxM grids with multiple labeling schemes.
 *
 * <p>This implements the "Blade Runner enhance" feature - dividing the screen
 * into a grid that can be recursively subdivided for precise targeting.</p>
 */
public class GridConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(GridConfiguration.class);

    /**
     * Label scheme for grid cells.
     */
    public enum LabelScheme {
        /** Column letters (A-Z), Row numbers (1-26). Example: A1, B3, Z10 */
        ALPHANUMERIC,
        /** Row-Column numbers. Example: 1-1, 2-3, 10-5 */
        NUMERIC,
        /** Both letters. Example: AA, BC, ZZ */
        ALPHA
    }

    private static final AtomicReference<GridConfiguration> instance = new AtomicReference<>();

    private volatile int rows;
    private volatile int columns;
    private volatile LabelScheme labelScheme;
    private volatile Rectangle screenBounds;

    private GridConfiguration() {
        this.rows = 3;
        this.columns = 3;
        this.labelScheme = LabelScheme.ALPHANUMERIC;
        updateScreenBounds();
    }

    /**
     * Get the singleton instance.
     */
    public static GridConfiguration getInstance() {
        GridConfiguration config = instance.get();
        if (config == null) {
            config = new GridConfiguration();
            if (!instance.compareAndSet(null, config)) {
                config = instance.get();
            }
        }
        return config;
    }

    /**
     * Configure the grid dimensions and label scheme.
     *
     * @param rows number of rows (1-26)
     * @param columns number of columns (1-26)
     * @param labelScheme the labeling scheme to use
     */
    public synchronized void configure(int rows, int columns, LabelScheme labelScheme) {
        if (rows < 1 || rows > 26) {
            throw new IllegalArgumentException("Rows must be between 1 and 26");
        }
        if (columns < 1 || columns > 26) {
            throw new IllegalArgumentException("Columns must be between 1 and 26");
        }
        this.rows = rows;
        this.columns = columns;
        this.labelScheme = labelScheme;
        updateScreenBounds();
        logger.info("Grid configured: {}x{} with {} scheme", rows, columns, labelScheme);
    }

    /**
     * Update the screen bounds from the current display configuration.
     */
    public void updateScreenBounds() {
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        java.awt.Rectangle bounds = gd.getDefaultConfiguration().getBounds();
        this.screenBounds = new Rectangle(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    /**
     * Get the current number of rows.
     */
    public int getRows() {
        return rows;
    }

    /**
     * Get the current number of columns.
     */
    public int getColumns() {
        return columns;
    }

    /**
     * Get the current label scheme.
     */
    public LabelScheme getLabelScheme() {
        return labelScheme;
    }

    /**
     * Get the screen bounds being used for the grid.
     */
    public Rectangle getScreenBounds() {
        return screenBounds;
    }

    /**
     * Get the width of a single cell in pixels.
     */
    public int getCellWidth() {
        return screenBounds.getWidth() / columns;
    }

    /**
     * Get the height of a single cell in pixels.
     */
    public int getCellHeight() {
        return screenBounds.getHeight() / rows;
    }

    /**
     * Get the bounds of a cell by row and column indices (0-based).
     */
    public Rectangle getCellBounds(int row, int col) {
        return getCellBounds(row, col, screenBounds);
    }

    /**
     * Get the bounds of a cell within a specific parent rectangle.
     */
    public Rectangle getCellBounds(int row, int col, Rectangle parentBounds) {
        if (row < 0 || row >= rows || col < 0 || col >= columns) {
            throw new IllegalArgumentException(
                    String.format("Invalid cell indices: row=%d, col=%d (grid is %dx%d)", row, col, rows, columns));
        }

        int cellWidth = parentBounds.getWidth() / columns;
        int cellHeight = parentBounds.getHeight() / rows;

        int x = parentBounds.getX() + col * cellWidth;
        int y = parentBounds.getY() + row * cellHeight;

        return new Rectangle(x, y, cellWidth, cellHeight);
    }

    /**
     * Get the label for a cell at the given row and column (0-based indices).
     */
    public String getCellLabel(int row, int col) {
        switch (labelScheme) {
            case ALPHANUMERIC:
                // Column letter (A-Z), Row number (1-26)
                char colChar = (char) ('A' + col);
                int rowNum = row + 1;
                return String.valueOf(colChar) + rowNum;

            case NUMERIC:
                // Row-Column (1-based)
                return (row + 1) + "-" + (col + 1);

            case ALPHA:
                // Both letters (AA, AB, BA, etc.)
                char rowChar = (char) ('A' + row);
                char colChar2 = (char) ('A' + col);
                return String.valueOf(colChar2) + rowChar;

            default:
                return (row + 1) + "-" + (col + 1);
        }
    }

    /**
     * Parse a cell label and return [row, column] indices (0-based).
     */
    public int[] parseLabel(String label) {
        label = label.trim().toUpperCase();

        switch (labelScheme) {
            case ALPHANUMERIC:
                return parseAlphanumericLabel(label);
            case NUMERIC:
                return parseNumericLabel(label);
            case ALPHA:
                return parseAlphaLabel(label);
            default:
                return parseAlphanumericLabel(label);
        }
    }

    /**
     * Parse ALPHANUMERIC label (e.g., "A1", "B3", "Z10").
     */
    private int[] parseAlphanumericLabel(String label) {
        if (label.length() < 2) {
            throw new IllegalArgumentException("Invalid grid label: " + label);
        }

        char colChar = label.charAt(0);
        if (colChar < 'A' || colChar > 'Z') {
            throw new IllegalArgumentException("Invalid column letter: " + colChar);
        }

        int col = colChar - 'A';
        int row;
        try {
            row = Integer.parseInt(label.substring(1)) - 1;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid row number in: " + label);
        }

        validateIndices(row, col, label);
        return new int[]{row, col};
    }

    /**
     * Parse NUMERIC label (e.g., "1-1", "2-3").
     */
    private int[] parseNumericLabel(String label) {
        String[] parts = label.split("-");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid numeric grid label: " + label);
        }

        try {
            int row = Integer.parseInt(parts[0]) - 1;
            int col = Integer.parseInt(parts[1]) - 1;
            validateIndices(row, col, label);
            return new int[]{row, col};
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid numeric grid label: " + label);
        }
    }

    /**
     * Parse ALPHA label (e.g., "AA", "BC").
     */
    private int[] parseAlphaLabel(String label) {
        if (label.length() != 2) {
            throw new IllegalArgumentException("Invalid alpha grid label: " + label);
        }

        char colChar = label.charAt(0);
        char rowChar = label.charAt(1);

        if (colChar < 'A' || colChar > 'Z' || rowChar < 'A' || rowChar > 'Z') {
            throw new IllegalArgumentException("Invalid alpha grid label: " + label);
        }

        int col = colChar - 'A';
        int row = rowChar - 'A';
        validateIndices(row, col, label);
        return new int[]{row, col};
    }

    /**
     * Validate that row and column are within bounds.
     */
    private void validateIndices(int row, int col, String label) {
        if (row < 0 || row >= rows) {
            throw new IllegalArgumentException(
                    String.format("Row index %d out of bounds for '%s' (grid has %d rows)", row + 1, label, rows));
        }
        if (col < 0 || col >= columns) {
            throw new IllegalArgumentException(
                    String.format("Column index %d out of bounds for '%s' (grid has %d columns)", col + 1, label, columns));
        }
    }

    /**
     * Reset to default configuration.
     */
    public synchronized void reset() {
        this.rows = 3;
        this.columns = 3;
        this.labelScheme = LabelScheme.ALPHANUMERIC;
        updateScreenBounds();
        logger.info("Grid reset to defaults: 3x3 ALPHANUMERIC");
    }

    /**
     * Get a string representation of the current configuration.
     */
    @Override
    public String toString() {
        return String.format("GridConfiguration{%dx%d, scheme=%s, screen=%s}",
                rows, columns, labelScheme, screenBounds);
    }

    /**
     * Get a detailed description of the grid for users.
     */
    public String describe() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Grid: %d rows x %d columns (%s scheme)\n", rows, columns, labelScheme));
        sb.append(String.format("Screen: %dx%d at (%d, %d)\n",
                screenBounds.getWidth(), screenBounds.getHeight(),
                screenBounds.getX(), screenBounds.getY()));
        sb.append(String.format("Cell size: %dx%d pixels\n", getCellWidth(), getCellHeight()));
        sb.append(String.format("Cell labels: %s to %s",
                getCellLabel(0, 0), getCellLabel(rows - 1, columns - 1)));
        return sb.toString();
    }
}
