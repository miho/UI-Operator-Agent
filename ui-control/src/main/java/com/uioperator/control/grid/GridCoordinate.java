package com.uioperator.control.grid;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Represents a grid coordinate with support for sub-grid addressing.
 *
 * <p>Coordinates can be single-level (e.g., "A1") or multi-level with sub-grids
 * (e.g., "A1.B3.C2" - cell C2 within cell B3 within cell A1).</p>
 *
 * <p>This is the "Blade Runner enhance" feature - each level zooms deeper
 * into the previous cell.</p>
 */
public class GridCoordinate {

    private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile("^[A-Z]\\d+$");
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("^\\d+-\\d+$");
    private static final Pattern ALPHA_PATTERN = Pattern.compile("^[A-Z]{2}$");

    private final List<String> levels;
    private final String originalString;

    /**
     * Parse a grid coordinate string.
     *
     * @param coordinate the coordinate string (e.g., "A1", "A1.B3", "A1.B3.C2")
     * @return the parsed GridCoordinate
     */
    public static GridCoordinate parse(String coordinate) {
        if (coordinate == null || coordinate.trim().isEmpty()) {
            throw new IllegalArgumentException("Grid coordinate cannot be null or empty");
        }
        return new GridCoordinate(coordinate);
    }

    /**
     * Check if a string is a valid grid coordinate.
     */
    public static boolean isValid(String coordinate, GridConfiguration.LabelScheme scheme) {
        if (coordinate == null || coordinate.trim().isEmpty()) {
            return false;
        }
        try {
            GridCoordinate gc = new GridCoordinate(coordinate);
            for (String level : gc.levels) {
                if (!isValidSingleLevel(level, scheme)) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if a single level coordinate is valid for the given scheme.
     */
    private static boolean isValidSingleLevel(String label, GridConfiguration.LabelScheme scheme) {
        label = label.toUpperCase();
        switch (scheme) {
            case ALPHANUMERIC:
                return ALPHANUMERIC_PATTERN.matcher(label).matches();
            case NUMERIC:
                return NUMERIC_PATTERN.matcher(label).matches();
            case ALPHA:
                return ALPHA_PATTERN.matcher(label).matches();
            default:
                return false;
        }
    }

    private GridCoordinate(String coordinate) {
        this.originalString = coordinate.trim().toUpperCase();
        this.levels = new ArrayList<>();

        // Split by dots for sub-grid levels
        String[] parts = this.originalString.split("\\.");

        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                throw new IllegalArgumentException("Empty level in coordinate: " + coordinate);
            }
            levels.add(trimmed);
        }

        if (levels.isEmpty()) {
            throw new IllegalArgumentException("No valid levels in coordinate: " + coordinate);
        }
    }

    /**
     * Get the number of levels (1 for simple coordinate, 2+ for sub-grid).
     */
    public int getDepth() {
        return levels.size();
    }

    /**
     * Get the coordinate at a specific level (0-indexed).
     */
    public String getLevel(int index) {
        if (index < 0 || index >= levels.size()) {
            throw new IndexOutOfBoundsException("Level index: " + index);
        }
        return levels.get(index);
    }

    /**
     * Get all levels.
     */
    public List<String> getLevels() {
        return new ArrayList<>(levels);
    }

    /**
     * Get the first (top-level) coordinate.
     */
    public String getTopLevel() {
        return levels.get(0);
    }

    /**
     * Get the last (deepest) coordinate.
     */
    public String getDeepestLevel() {
        return levels.get(levels.size() - 1);
    }

    /**
     * Check if this is a sub-grid coordinate (has more than one level).
     */
    public boolean hasSubGrid() {
        return levels.size() > 1;
    }

    /**
     * Get the original coordinate string.
     */
    public String getOriginalString() {
        return originalString;
    }

    /**
     * Create a new coordinate with an additional sub-grid level.
     */
    public GridCoordinate withSubLevel(String subLevel) {
        return GridCoordinate.parse(originalString + "." + subLevel);
    }

    /**
     * Create a coordinate with the last level removed.
     */
    public GridCoordinate getParent() {
        if (levels.size() <= 1) {
            throw new IllegalStateException("Cannot get parent of top-level coordinate");
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < levels.size() - 1; i++) {
            if (i > 0) sb.append(".");
            sb.append(levels.get(i));
        }
        return GridCoordinate.parse(sb.toString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GridCoordinate that = (GridCoordinate) o;
        return Objects.equals(originalString, that.originalString);
    }

    @Override
    public int hashCode() {
        return Objects.hash(originalString);
    }

    @Override
    public String toString() {
        return originalString;
    }
}
