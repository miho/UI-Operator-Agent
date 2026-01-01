package com.uioperator.control.grid;

import com.uioperator.common.model.Point;
import com.uioperator.common.model.Rectangle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves grid coordinates to pixel coordinates.
 *
 * <p>Supports recursive sub-grid resolution - each level of the coordinate
 * (separated by dots) zooms into the previous cell, like Blade Runner's
 * "enhance" feature.</p>
 *
 * <p>Example: "A1.B3.C2" resolves as:
 * <ol>
 *   <li>A1 - Find cell A1 in the full screen grid</li>
 *   <li>B3 - Divide A1 into a sub-grid, find cell B3 within it</li>
 *   <li>C2 - Divide B3 into a sub-grid, find cell C2 within it</li>
 *   <li>Return the center of C2</li>
 * </ol>
 * </p>
 */
public class GridResolver {

    private static final Logger logger = LoggerFactory.getLogger(GridResolver.class);

    private final GridConfiguration config;

    public GridResolver() {
        this.config = GridConfiguration.getInstance();
    }

    public GridResolver(GridConfiguration config) {
        this.config = config;
    }

    /**
     * Resolve a grid coordinate string to a pixel point (center of cell).
     *
     * @param coordinate the grid coordinate (e.g., "A1", "A1.B3")
     * @return the center point of the resolved cell
     */
    public Point resolve(String coordinate) {
        GridCoordinate gc = GridCoordinate.parse(coordinate);
        return resolve(gc);
    }

    /**
     * Resolve a GridCoordinate to a pixel point.
     */
    public Point resolve(GridCoordinate coordinate) {
        Rectangle bounds = resolveToBounds(coordinate);
        return bounds.getCenter();
    }

    /**
     * Resolve a grid coordinate to its bounding rectangle.
     *
     * @param coordinate the grid coordinate
     * @return the bounding rectangle of the resolved cell
     */
    public Rectangle resolveToBounds(String coordinate) {
        GridCoordinate gc = GridCoordinate.parse(coordinate);
        return resolveToBounds(gc);
    }

    /**
     * Resolve a GridCoordinate to its bounding rectangle.
     */
    public Rectangle resolveToBounds(GridCoordinate coordinate) {
        Rectangle currentBounds = config.getScreenBounds();

        logger.debug("Resolving coordinate '{}' starting from bounds {}",
                coordinate.getOriginalString(), currentBounds);

        for (String level : coordinate.getLevels()) {
            int[] indices = config.parseLabel(level);
            int row = indices[0];
            int col = indices[1];

            currentBounds = getCellBoundsWithin(row, col, currentBounds);
            logger.debug("Level '{}' resolved to bounds {}", level, currentBounds);
        }

        logger.debug("Final bounds for '{}': {}", coordinate.getOriginalString(), currentBounds);
        return currentBounds;
    }

    /**
     * Get the bounds of a cell within a parent rectangle.
     * Uses the current grid configuration (rows/columns).
     */
    private Rectangle getCellBoundsWithin(int row, int col, Rectangle parentBounds) {
        int cellWidth = parentBounds.getWidth() / config.getColumns();
        int cellHeight = parentBounds.getHeight() / config.getRows();

        int x = parentBounds.getX() + col * cellWidth;
        int y = parentBounds.getY() + row * cellHeight;

        return new Rectangle(x, y, cellWidth, cellHeight);
    }

    /**
     * Convert a pixel point to its grid coordinate at the current level.
     *
     * @param point the pixel coordinate
     * @return the grid label (e.g., "A1")
     */
    public String pointToGrid(Point point) {
        return pointToGrid(point.getX(), point.getY());
    }

    /**
     * Convert pixel coordinates to a grid label.
     */
    public String pointToGrid(int x, int y) {
        Rectangle bounds = config.getScreenBounds();

        int cellWidth = bounds.getWidth() / config.getColumns();
        int cellHeight = bounds.getHeight() / config.getRows();

        int col = (x - bounds.getX()) / cellWidth;
        int row = (y - bounds.getY()) / cellHeight;

        // Clamp to valid range
        col = Math.max(0, Math.min(col, config.getColumns() - 1));
        row = Math.max(0, Math.min(row, config.getRows() - 1));

        return config.getCellLabel(row, col);
    }

    /**
     * Get all cell labels in the current grid.
     */
    public String[][] getAllLabels() {
        String[][] labels = new String[config.getRows()][config.getColumns()];
        for (int row = 0; row < config.getRows(); row++) {
            for (int col = 0; col < config.getColumns(); col++) {
                labels[row][col] = config.getCellLabel(row, col);
            }
        }
        return labels;
    }

    /**
     * Get the center point for each cell in the grid.
     */
    public Point[][] getAllCenters() {
        Point[][] centers = new Point[config.getRows()][config.getColumns()];
        for (int row = 0; row < config.getRows(); row++) {
            for (int col = 0; col < config.getColumns(); col++) {
                Rectangle bounds = config.getCellBounds(row, col);
                centers[row][col] = bounds.getCenter();
            }
        }
        return centers;
    }

    /**
     * Validate that a coordinate string is valid for the current configuration.
     */
    public boolean isValidCoordinate(String coordinate) {
        try {
            GridCoordinate gc = GridCoordinate.parse(coordinate);
            for (String level : gc.getLevels()) {
                config.parseLabel(level); // Will throw if invalid
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get the GridConfiguration being used.
     */
    public GridConfiguration getConfiguration() {
        return config;
    }
}
