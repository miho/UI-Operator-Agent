package com.uioperator.vision;

import com.uioperator.common.util.Base64Util;
import com.uioperator.vision.analysis.ScreenshotAnalyzer;
import com.uioperator.vision.analysis.ScreenshotComparator;
import com.uioperator.vision.annotation.Annotation;
import com.uioperator.vision.annotation.AnnotationType;
import com.uioperator.vision.annotation.ScreenshotAnnotator;
import com.uioperator.vision.mcp.McpConfig;
import com.uioperator.vision.mcp.ServerLauncher;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.Mono;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * MCP Server for UI vision and analysis operations.
 * Provides screenshot analysis, comparison, and annotation tools.
 */
public class UiVisionMcpServer {

    private static ScreenshotAnalyzer analyzer;
    private static ScreenshotComparator comparator;
    private static ScreenshotAnnotator annotator;

    public static void main(String[] args) {
        try {
            McpConfig config = parseArgs(args);
            initComponents();

            if (config.getTransportMode() == McpConfig.TransportMode.STDIO) {
                startStdioServerMain();
            } else {
                ServerLauncher launcher = new ServerLauncher(config);
                launcher.startAsync().join();

                System.err.println("UI Vision MCP Server started on " + config.getHttpUrl());
                System.err.println("Press Ctrl+C to stop");

                CountDownLatch latch = new CountDownLatch(1);
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    launcher.shutdown();
                    latch.countDown();
                }));
                latch.await();
            }
        } catch (Exception e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static McpConfig parseArgs(String[] args) {
        McpConfig.Builder builder = McpConfig.builder();

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--stdio":
                    builder.transportMode(McpConfig.TransportMode.STDIO);
                    break;
                case "--http":
                    builder.transportMode(McpConfig.TransportMode.HTTP);
                    if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                        builder.httpPort(Integer.parseInt(args[++i]));
                    }
                    break;
                case "--port":
                    if (i + 1 < args.length) {
                        builder.httpPort(Integer.parseInt(args[++i]));
                    }
                    break;
            }
        }

        return builder.build();
    }

    private static void initComponents() {
        analyzer = new ScreenshotAnalyzer();
        comparator = new ScreenshotComparator();
        annotator = new ScreenshotAnnotator();
    }

    public static McpAsyncServer createStdioServer() {
        initComponents();

        StdioServerTransportProvider transportProvider =
                new StdioServerTransportProvider(McpJsonMapper.createDefault());

        var tools = ToolFactory.createAllAsyncTools();

        return McpServer.async(transportProvider)
                .serverInfo("ui-vision-server", "1.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(true)
                        .build())
                .tools(tools.toArray(new McpServerFeatures.AsyncToolSpecification[0]))
                .build();
    }

    private static void startStdioServerMain() throws InterruptedException {
        McpAsyncServer server = createStdioServer();

        System.err.println("UI Vision MCP Server started (stdio mode)");
        System.err.println("Version: 1.0.0");
        System.err.println("Ready for connections...");

        CountDownLatch latch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("Shutting down server...");
            server.close();
            latch.countDown();
        }));

        latch.await();
    }

    // ==================== TOOL FACTORY ====================

    public static class ToolFactory {

        public static List<McpServerFeatures.AsyncToolSpecification> createAllAsyncTools() {
            List<McpServerFeatures.AsyncToolSpecification> tools = new ArrayList<>();

            tools.add(createAnalyzeScreenshotTool());
            tools.add(createAnalyzeActionResultTool());
            tools.add(createFindElementTool());
            tools.add(createCompareScreenshotsTool());
            tools.add(createAnnotateScreenshotTool());

            return tools;
        }

        public static List<McpStatelessServerFeatures.SyncToolSpecification> createAllStatelessTools() {
            List<McpStatelessServerFeatures.SyncToolSpecification> tools = new ArrayList<>();

            for (McpServerFeatures.AsyncToolSpecification asyncTool : createAllAsyncTools()) {
                tools.add(convertToStateless(asyncTool));
            }

            return tools;
        }

        private static McpStatelessServerFeatures.SyncToolSpecification convertToStateless(
                McpServerFeatures.AsyncToolSpecification asyncTool) {
            return new McpStatelessServerFeatures.SyncToolSpecification.Builder()
                    .tool(asyncTool.tool())
                    .callHandler((transportContext, request) ->
                            asyncTool.callHandler().apply(null, request).block())
                    .build();
        }

        // ==================== ANALYSIS TOOLS ====================

        private static McpServerFeatures.AsyncToolSpecification createAnalyzeScreenshotTool() {
            String schema = """
                {
                  "type": "object",
                  "properties": {
                    "image_data": {"type": "string", "description": "Base64 encoded screenshot"},
                    "prompt": {"type": "string", "description": "What to look for or describe"}
                  },
                  "required": ["image_data"]
                }
                """;

            return createTool("analyze_screenshot",
                    "Analyze a screenshot and describe what's visible",
                    schema,
                    args -> {
                        String imageData = getStringArg(args, "image_data");
                        String prompt = getStringArg(args, "prompt", null);

                        BufferedImage image = Base64Util.decodeImage(Base64Util.fromDataUri(imageData));
                        return analyzer.analyze(image, prompt);
                    });
        }

        private static McpServerFeatures.AsyncToolSpecification createAnalyzeActionResultTool() {
            String schema = """
                {
                  "type": "object",
                  "properties": {
                    "before_image": {"type": "string", "description": "Base64 screenshot before action"},
                    "after_image": {"type": "string", "description": "Base64 screenshot after action"},
                    "action_description": {"type": "string", "description": "What action was performed"}
                  },
                  "required": ["before_image", "after_image"]
                }
                """;

            return createTool("analyze_action_result",
                    "Analyze before/after screenshots to determine action result",
                    schema,
                    args -> {
                        String beforeData = getStringArg(args, "before_image");
                        String afterData = getStringArg(args, "after_image");
                        String actionDesc = getStringArg(args, "action_description", null);

                        BufferedImage before = Base64Util.decodeImage(Base64Util.fromDataUri(beforeData));
                        BufferedImage after = Base64Util.decodeImage(Base64Util.fromDataUri(afterData));

                        return analyzer.analyzeActionResult(before, after, actionDesc);
                    });
        }

        private static McpServerFeatures.AsyncToolSpecification createFindElementTool() {
            String schema = """
                {
                  "type": "object",
                  "properties": {
                    "image_data": {"type": "string", "description": "Base64 encoded screenshot"},
                    "element_description": {"type": "string", "description": "Description of element to find"}
                  },
                  "required": ["image_data", "element_description"]
                }
                """;

            return createTool("find_element",
                    "Find a specific UI element in the screenshot",
                    schema,
                    args -> {
                        String imageData = getStringArg(args, "image_data");
                        String description = getStringArg(args, "element_description");

                        BufferedImage image = Base64Util.decodeImage(Base64Util.fromDataUri(imageData));
                        ScreenshotAnalyzer.ElementLocation location = analyzer.findElement(image, description);

                        return location.toString();
                    });
        }

        private static McpServerFeatures.AsyncToolSpecification createCompareScreenshotsTool() {
            String schema = """
                {
                  "type": "object",
                  "properties": {
                    "image1": {"type": "string", "description": "First base64 screenshot"},
                    "image2": {"type": "string", "description": "Second base64 screenshot"},
                    "highlight_color": {"type": "string", "default": "#FF0000", "description": "Color for highlighting"}
                  },
                  "required": ["image1", "image2"]
                }
                """;

            return createTool("compare_screenshots",
                    "Compare two screenshots and highlight differences",
                    schema,
                    args -> {
                        String data1 = getStringArg(args, "image1");
                        String data2 = getStringArg(args, "image2");
                        String colorHex = getStringArg(args, "highlight_color", "#FF0000");

                        BufferedImage img1 = Base64Util.decodeImage(Base64Util.fromDataUri(data1));
                        BufferedImage img2 = Base64Util.decodeImage(Base64Util.fromDataUri(data2));
                        Color color = parseColor(colorHex);

                        // Get comparison statistics
                        ScreenshotComparator.ComparisonResult result = comparator.compare(img1, img2);

                        // Create diff image
                        BufferedImage diffImage = comparator.createDiffImage(img1, img2, color);
                        String diffBase64 = Base64Util.encodeImage(diffImage);

                        return result.toString() + "\nDiff image (base64):\n" + diffBase64;
                    });
        }

        private static McpServerFeatures.AsyncToolSpecification createAnnotateScreenshotTool() {
            String schema = """
                {
                  "type": "object",
                  "properties": {
                    "image_data": {"type": "string", "description": "Base64 encoded screenshot"},
                    "annotations": {
                      "type": "array",
                      "items": {
                        "type": "object",
                        "properties": {
                          "type": {"type": "string", "enum": ["circle", "arrow", "text", "rectangle", "marker", "line"]},
                          "x": {"type": "integer"},
                          "y": {"type": "integer"},
                          "x2": {"type": "integer", "description": "End x for arrow/line"},
                          "y2": {"type": "integer", "description": "End y for arrow/line"},
                          "radius": {"type": "integer", "description": "For circle/marker"},
                          "width": {"type": "integer", "description": "For rectangle"},
                          "height": {"type": "integer", "description": "For rectangle"},
                          "text": {"type": "string", "description": "For text annotation"},
                          "color": {"type": "string", "default": "#FF0000"},
                          "thickness": {"type": "integer", "default": 2},
                          "font_size": {"type": "integer", "default": 14}
                        },
                        "required": ["type", "x", "y"]
                      }
                    }
                  },
                  "required": ["image_data", "annotations"]
                }
                """;

            return createTool("annotate_screenshot",
                    "Add visual annotations to a screenshot",
                    schema,
                    args -> {
                        String imageData = getStringArg(args, "image_data");
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> annotationMaps =
                                (List<Map<String, Object>>) args.get("annotations");

                        BufferedImage image = Base64Util.decodeImage(Base64Util.fromDataUri(imageData));

                        // Parse annotations
                        List<Annotation> annotations = new ArrayList<>();
                        for (Map<String, Object> ann : annotationMaps) {
                            annotations.add(parseAnnotation(ann));
                        }

                        // Apply annotations
                        BufferedImage annotated = annotator.annotate(image, annotations);
                        String resultBase64 = Base64Util.encodeImage(annotated);

                        return String.format("Applied %d annotations. Annotated image (base64):\n%s",
                                annotations.size(), resultBase64);
                    });
        }

        // ==================== HELPER METHODS ====================

        private static McpServerFeatures.AsyncToolSpecification createTool(
                String name, String description, String schema, ToolHandler handler) {
            return new McpServerFeatures.AsyncToolSpecification.Builder()
                    .tool(McpSchema.Tool.builder()
                            .name(name)
                            .description(description)
                            .inputSchema(McpJsonMapper.createDefault(), schema)
                            .build())
                    .callHandler((exchange, request) -> {
                        try {
                            Map<String, Object> args = request.arguments() != null
                                    ? request.arguments() : Map.of();
                            String result = handler.handle(args);
                            return Mono.just(new McpSchema.CallToolResult.Builder()
                                    .content(List.of(new McpSchema.TextContent(result)))
                                    .isError(false)
                                    .build());
                        } catch (Exception e) {
                            return Mono.just(new McpSchema.CallToolResult.Builder()
                                    .content(List.of(new McpSchema.TextContent("Error: " + e.getMessage())))
                                    .isError(true)
                                    .build());
                        }
                    })
                    .build();
        }

        @FunctionalInterface
        interface ToolHandler {
            String handle(Map<String, Object> args) throws Exception;
        }

        private static Annotation parseAnnotation(Map<String, Object> ann) {
            String typeStr = ann.get("type").toString().toUpperCase();
            AnnotationType type = AnnotationType.valueOf(typeStr);

            Annotation.Builder builder = Annotation.builder()
                    .type(type)
                    .x(getIntArg(ann, "x", 0))
                    .y(getIntArg(ann, "y", 0));

            if (ann.containsKey("x2")) builder.x2(getIntArg(ann, "x2", 0));
            if (ann.containsKey("y2")) builder.y2(getIntArg(ann, "y2", 0));
            if (ann.containsKey("radius")) builder.radius(getIntArg(ann, "radius", 20));
            if (ann.containsKey("width")) builder.width(getIntArg(ann, "width", 100));
            if (ann.containsKey("height")) builder.height(getIntArg(ann, "height", 100));
            if (ann.containsKey("text")) builder.text(ann.get("text").toString());
            if (ann.containsKey("color")) builder.colorHex(ann.get("color").toString());
            if (ann.containsKey("thickness")) builder.thickness(getIntArg(ann, "thickness", 2));
            if (ann.containsKey("font_size")) builder.fontSize(getIntArg(ann, "font_size", 14));

            return builder.build();
        }

        private static Color parseColor(String hex) {
            if (hex == null || hex.isEmpty()) return Color.RED;
            if (hex.startsWith("#")) hex = hex.substring(1);
            try {
                return new Color(Integer.parseInt(hex, 16));
            } catch (NumberFormatException e) {
                return Color.RED;
            }
        }

        private static String getStringArg(Map<String, Object> args, String key) {
            Object value = args.get(key);
            if (value == null) {
                throw new IllegalArgumentException("Required parameter missing: " + key);
            }
            return value.toString();
        }

        private static String getStringArg(Map<String, Object> args, String key, String defaultValue) {
            Object value = args.get(key);
            return value != null ? value.toString() : defaultValue;
        }

        private static int getIntArg(Map<String, Object> args, String key, int defaultValue) {
            Object value = args.get(key);
            if (value == null) return defaultValue;
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return Integer.parseInt(value.toString());
        }
    }
}
