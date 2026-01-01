package com.uioperator.vision.annotation;

/**
 * Types of visual annotations that can be drawn on screenshots.
 */
public enum AnnotationType {
    /** Circle annotation centered at a point */
    CIRCLE,
    /** Arrow from one point to another */
    ARROW,
    /** Text label at a position */
    TEXT,
    /** Rectangle highlight */
    RECTANGLE,
    /** Filled circle (marker) */
    MARKER,
    /** Line from one point to another */
    LINE
}
