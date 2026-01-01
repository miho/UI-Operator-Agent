package com.uioperator.vision.annotation;

import java.awt.Color;

/**
 * Represents a visual annotation to be drawn on a screenshot.
 */
public class Annotation {

    private final AnnotationType type;
    private final int x;
    private final int y;
    private final int x2;
    private final int y2;
    private final int radius;
    private final int width;
    private final int height;
    private final String text;
    private final Color color;
    private final int thickness;
    private final int fontSize;

    private Annotation(Builder builder) {
        this.type = builder.type;
        this.x = builder.x;
        this.y = builder.y;
        this.x2 = builder.x2;
        this.y2 = builder.y2;
        this.radius = builder.radius;
        this.width = builder.width;
        this.height = builder.height;
        this.text = builder.text;
        this.color = builder.color;
        this.thickness = builder.thickness;
        this.fontSize = builder.fontSize;
    }

    public AnnotationType getType() {
        return type;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getX2() {
        return x2;
    }

    public int getY2() {
        return y2;
    }

    public int getRadius() {
        return radius;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getText() {
        return text;
    }

    public Color getColor() {
        return color;
    }

    public int getThickness() {
        return thickness;
    }

    public int getFontSize() {
        return fontSize;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private AnnotationType type = AnnotationType.CIRCLE;
        private int x = 0;
        private int y = 0;
        private int x2 = 0;
        private int y2 = 0;
        private int radius = 20;
        private int width = 100;
        private int height = 100;
        private String text = "";
        private Color color = Color.RED;
        private int thickness = 2;
        private int fontSize = 14;

        public Builder type(AnnotationType type) {
            this.type = type;
            return this;
        }

        public Builder x(int x) {
            this.x = x;
            return this;
        }

        public Builder y(int y) {
            this.y = y;
            return this;
        }

        public Builder x2(int x2) {
            this.x2 = x2;
            return this;
        }

        public Builder y2(int y2) {
            this.y2 = y2;
            return this;
        }

        public Builder radius(int radius) {
            this.radius = radius;
            return this;
        }

        public Builder width(int width) {
            this.width = width;
            return this;
        }

        public Builder height(int height) {
            this.height = height;
            return this;
        }

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        public Builder color(Color color) {
            this.color = color;
            return this;
        }

        public Builder colorHex(String hex) {
            this.color = parseColor(hex);
            return this;
        }

        public Builder thickness(int thickness) {
            this.thickness = thickness;
            return this;
        }

        public Builder fontSize(int fontSize) {
            this.fontSize = fontSize;
            return this;
        }

        public Annotation build() {
            return new Annotation(this);
        }

        private static Color parseColor(String hex) {
            if (hex == null || hex.isEmpty()) {
                return Color.RED;
            }
            if (hex.startsWith("#")) {
                hex = hex.substring(1);
            }
            try {
                return new Color(Integer.parseInt(hex, 16));
            } catch (NumberFormatException e) {
                return Color.RED;
            }
        }
    }
}
