package com.uioperator.common.model;

import java.util.Objects;

/**
 * Immutable rectangle representing a screen region.
 */
public final class Rectangle {
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    public Rectangle(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
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

    public Point getCenter() {
        return new Point(x + width / 2, y + height / 2);
    }

    public Point getTopLeft() {
        return new Point(x, y);
    }

    public Point getBottomRight() {
        return new Point(x + width, y + height);
    }

    public boolean contains(Point point) {
        return point.getX() >= x && point.getX() < x + width
                && point.getY() >= y && point.getY() < y + height;
    }

    public boolean contains(int px, int py) {
        return px >= x && px < x + width && py >= y && py < y + height;
    }

    public java.awt.Rectangle toAwtRectangle() {
        return new java.awt.Rectangle(x, y, width, height);
    }

    public static Rectangle fromAwtRectangle(java.awt.Rectangle awtRect) {
        return new Rectangle(awtRect.x, awtRect.y, awtRect.width, awtRect.height);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rectangle rectangle = (Rectangle) o;
        return x == rectangle.x && y == rectangle.y
                && width == rectangle.width && height == rectangle.height;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, width, height);
    }

    @Override
    public String toString() {
        return String.format("Rectangle(x=%d, y=%d, w=%d, h=%d)", x, y, width, height);
    }
}
