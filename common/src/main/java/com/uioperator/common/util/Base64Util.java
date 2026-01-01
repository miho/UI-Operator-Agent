package com.uioperator.common.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * Utility class for Base64 encoding/decoding of images.
 */
public final class Base64Util {

    private Base64Util() {
        // Utility class
    }

    /**
     * Encode a BufferedImage to a Base64 string (PNG format).
     */
    public static String encodeImage(BufferedImage image) throws IOException {
        return encodeImage(image, "PNG");
    }

    /**
     * Encode a BufferedImage to a Base64 string with specified format.
     */
    public static String encodeImage(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        byte[] bytes = baos.toByteArray();
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * Decode a Base64 string to a BufferedImage.
     */
    public static BufferedImage decodeImage(String base64) throws IOException {
        byte[] bytes = Base64.getDecoder().decode(base64);
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        return ImageIO.read(bais);
    }

    /**
     * Encode raw bytes to Base64.
     */
    public static String encode(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    /**
     * Decode Base64 string to raw bytes.
     */
    public static byte[] decode(String base64) {
        return Base64.getDecoder().decode(base64);
    }

    /**
     * Create a data URI for an image (e.g., "data:image/png;base64,...").
     */
    public static String toDataUri(BufferedImage image, String format) throws IOException {
        String base64 = encodeImage(image, format);
        String mimeType = "image/" + format.toLowerCase();
        return "data:" + mimeType + ";base64," + base64;
    }

    /**
     * Extract Base64 data from a data URI.
     */
    public static String fromDataUri(String dataUri) {
        if (dataUri.startsWith("data:")) {
            int commaIndex = dataUri.indexOf(',');
            if (commaIndex > 0) {
                return dataUri.substring(commaIndex + 1);
            }
        }
        return dataUri;
    }
}
