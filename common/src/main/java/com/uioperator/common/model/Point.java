package com.uioperator.common.model;

import java.util.Objects;

/**
 * Immutable 2D point representing pixel coordinates.
 */
public final class Point {
    private final int x;
    private final int y;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Point offset(int dx, int dy) {
        return new Point(x + dx, y + dy);
    }

    public java.awt.Point toAwtPoint() {
        return new java.awt.Point(x, y);
    }

    public static Point fromAwtPoint(java.awt.Point awtPoint) {
        return new Point(awtPoint.x, awtPoint.y);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Point point = (Point) o;
        return x == point.x && y == point.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return String.format("Point(%d, %d)", x, y);
    }
}
