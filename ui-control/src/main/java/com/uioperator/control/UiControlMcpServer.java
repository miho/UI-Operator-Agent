package com.uioperator.control;

import com.uioperator.common.model.Point;
import com.uioperator.common.model.Rectangle;
import com.uioperator.control.grid.GridConfiguration;
import com.uioperator.control.grid.GridResolver;
import com.uioperator.control.mcp.McpConfig;
import com.uioperator.control.mcp.ServerLauncher;
import com.uioperator.control.robot.KeyboardController;
import com.uioperator.control.robot.MouseController;
import com.uioperator.control.robot.ScreenController;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * MCP Server for UI control operations.
 * Provides fine-grained mouse, keyboard, grid, and screenshot tools.
 */
public class UiControlMcpServer {

    private static MouseController mouseController;
    private static KeyboardController keyboardController;
    private static ScreenController screenController;
    private static GridResolver gridResolver;

    public static void main(String[] args) {
        try {
            // Parse command line arguments
            McpConfig config = parseArgs(args);

            // Initialize controllers
            initControllers();

            if (config.getTransportMode() == McpConfig.TransportMode.STDIO) {
                startStdioServer();
            } else {
                ServerLauncher launcher = new ServerLauncher(config);
                launcher.startAsync().join();

                System.err.println("UI Control MCP Server started on " + config.getHttpUrl());
                System.err.println("Press Ctrl+C to stop");

                // Keep running until interrupted
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

    private static void initControllers() throws Exception {
        mouseController = new MouseController();
        keyboardController = new KeyboardController();
        screenController = new ScreenController();
        gridResolver = new GridResolver();
    }

    /**
     * Create a stdio server for standalone or embedded use.
     */
    public static McpAsyncServer createStdioServer() {
        try {
            initControllers();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize controllers", e);
        }

        StdioServerTransportProvider transportProvider =
                new StdioServerTransportProvider(McpJsonMapper.createDefault());

        var tools = ToolFactory.createAllAsyncTools();

        return McpServer.async(transportProvider)
                .serverInfo("ui-control-server", "1.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(true)
                        .build())
                .tools(tools.toArray(new McpServerFeatures.AsyncToolSpecification[0]))
                .build();
    }

    private static void startStdioServer() throws InterruptedException {
        McpAsyncServer server = createStdioServer();

        System.err.println("UI Control MCP Server started (stdio mode)");
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

    /**
     * Factory for creating MCP tool specifications.
     */
    public static class ToolFactory {

        public static List<McpServerFeatures.AsyncToolSpecification> createAllAsyncTools() {
            List<McpServerFeatures.AsyncToolSpecification> tools = new ArrayList<>();

            // Mouse operations
            tools.add(createMouseMoveTool());
            tools.add(createMouseLeftClickTool());
            tools.add(createMouseRightClickTool());
            tools.add(createMouseDoubleClickTool());
            tools.add(createMouseMiddleClickTool());
            tools.add(createMousePressLeftTool());
            tools.add(createMouseReleaseLeftTool());
            tools.add(createMousePressRightTool());
            tools.add(createMouseReleaseRightTool());
            tools.add(createMouseScrollTool());
            tools.add(createMouseDragTool());

            // Keyboard operations
            tools.add(createKeyPressTool());
            tools.add(createKeyReleaseTool());
            tools.add(createKeyTypeTool());
            tools.add(createKeyComboTool());

            // Grid operations
            tools.add(createGridConfigureTool());
            tools.add(createGridGetConfigTool());
            tools.add(createGridToPixelTool());

            // Screenshot operations
            tools.add(createScreenshotFullTool());
            tools.add(createScreenshotRegionTool());
            tools.add(createScreenshotAtCursorTool());

            // Command batching
            tools.add(createExecuteSequenceTool());

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

        // ==================== MOUSE TOOLS ====================

        private static McpServerFeatures.AsyncToolSpecification createMouseMoveTool() {
            String schema = """
                {
                  "type": "object",
                  "properties": {
                    "x": {"type": "integer", "description": "X pixel coordinate"},
                    "y": {"type": "integer", "description": "Y pixel coordinate"},
                    "grid": {"type": "string", "description": "Grid coordinate (e.g., 'A1' or 'A1.B3')"}
                  }
                }
                """;

            return createTool("mouse_move",
                    "Move mouse to pixel coordinates or grid cell",
                    schema,
                    args -> {
                        Point target = resolvePosition(args);
                        mouseController.moveTo(target);
                        return String.format("Mouse moved to (%d, %d)", target.getX(), target.getY());
                    });
        }

        private static McpServerFeatures.AsyncToolSpecification createMouseLeftClickTool() {
            String schema = """
                {
                  "type": "object",
                  "properties": {
                    "x": {"type": "integer", "description": "X pixel coordinate"},
                    "y": {"type": "integer", "description": "Y pixel coordinate"},
                    "grid": {"type": "string", "description": "Grid coordinate"},
                    "shift": {"type": "boolean", "default": false},
                    "ctrl": {"type": "boolean", "default": false},
                    "alt": {"type": "boolean", "default": false}
                  }
                }
                """;

            return createTool("mouse_left_click",
                    "Perform left mouse click with optional modifiers",
                    schema,
                    args -> {
                        boolean shift = getBoolArg(args, "shift", false);
                        boolean ctrl = getBoolArg(args, "ctrl", false);
                        boolean alt = getBoolArg(args, "alt", false);

                        if (hasPosition(args)) {
                            Point target = resolvePosition(args);
                            mouseController.leftClick(target.getX(), target.getY(), shift, ctrl, alt);
                            return String.format("Left clicked at (%d, %d)", target.getX(), target.getY());
                        } else {
                            Point pos = mouseController.getPosition();
                            mouseController.leftClick(pos.getX(), pos.getY(), shift, ctrl, alt);
                            return "Left clicked at current position";
                        }
                    });
        }

        private static McpServerFeatures.AsyncToolSpecification createMouseRightClickTool() {
            String schema = """
                {
                  "type": "object",
                  "properties": {
                    "x": {"type": "integer"},
                    "y": {"type": "integer"},
                    "grid": {"type": "string"},
                    "shift": {"type": "boolean", "default": false},
                    "ctrl": {"type": "boolean", "default": false},
                    "alt": {"type": "boolean", "default": false}
                  }
                }
                """;

            return createTool("mouse_right_click",
                    "Perform right mouse click with optional modifiers",
                    schema,
                    args -> {
                        boolean shift = getBoolArg(args, "shift", false);
                        boolean ctrl = getBoolArg(args, "ctrl", false);
                        boolean alt = getBoolArg(args, "alt", false);

                        if (hasPosition(args)) {
                            Point target = resolvePosition(args);
                            mouseController.rightClick(target.getX(), target.getY(), shift, ctrl, alt);
                            return String.format("Right clicked at (%d, %d)", target.getX(), target.getY());
                        } else {
                            mouseController.rightClick();
                            return "Right clicked at current position";
                        }
                    });
        }

        private static McpServerFeatures.AsyncToolSpecification createMouseDoubleClickTool() {
            String schema = """
                {
                  "type": "object",
                  "properties": {
                    "x": {"type": "integer"},
                    "y": {"type": "integer"},
                    "grid": {"type": "string"},
                    "shift": {"type": "boolean", "default": false},
                    "ctrl": {"type": "boolean", "default": false},
                    "alt": {"type": "boolean", "default": false}
                  }
                }
                """;

            return createTool("mouse_double_click",
                    "Perform double left click with optional modifiers",
                    schema,
                    args -> {
                        boolean shift = getBoolArg(args, "shift", false);
                        boolean ctrl = getBoolArg(args, "ctrl", false);
                        boolean alt = getBoolArg(args, "alt", false);

                        if (hasPosition(args)) {
                            Point target = resolvePosition(args);
                            mouseController.doubleClick(target.getX(), target.getY(), shift, ctrl, alt);
                            return String.format("Double clicked at (%d, %d)", target.getX(), target.getY());
                        } else {
                            mouseController.doubleClick();
                            return "Double clicked at current position";
                        }
                    });
        }

        private static McpServerFeatures.AsyncToolSpecification createMouseMiddleClickTool() {
            String schema = """
                {
                  "type": "object",
                  "properties": {
                    "x": {"type": "integer"},
                    "y": {"type": "integer"},
                    "grid": {"type": "string"}
                  }
                }
                """;

            return createTool("mouse_middle_click",
                    "Perform middle button click",
                    schema,
                    args -> {
                        if (hasPosition(args)) {
                            Point target = resolvePosition(args);
                            mouseController.middleClick(target.getX(), target.getY());
                            return String.format("Middle clicked at (%d, %d)", target.getX(), target.getY());
                        } else {
                            mouseController.middleClick();
                            return "Middle clicked at current position";
                        }
                    });
        }

        private static McpServerFeatures.AsyncToolSpecification createMousePressLeftTool() {
            String schema = """
                {"type": "object", "properties": {}}
                """;

            return createTool("mouse_press_left",
                    "Press and hold left mouse button",
                    schema,
                    args -> {
                        mouseController.pressLeft();
                        return "Left mouse button pressed";
                    });
        }

        private static McpServerFeatures.AsyncToolSpecification createMouseReleaseLeftTool() {
            String schema = """
                {"type": "object", "properties": {}}
                """;

            return createTool("mouse_release_left",
                    "Release left mouse button",
                    schema,
                    args -> {
                        mouseController.releaseLeft();
                        return "Left mouse button released";
                    });
        }

        private static McpServerFeatures.AsyncToolSpecification createMousePressRightTool() {
            String schema = """
                {"type": "object", "properties": {}}
                """;

            return createTool("mouse_press_right",
                    "Press and hold right mouse button",
                    schema,
                    args -> {
                        mouseController.pressRight();
                        return "Right mouse button pressed";
                    });
        }

        private static McpServerFeatures.AsyncToolSpecification createMouseReleaseRightTool() {
            String schema = """
                {"type": "object", "properties": {}}
                """;

            return createTool("mouse_release_right",
                    "Release right mouse button",
                    schema,
                    args -> {
                        mouseController.releaseRight();
                        return "Right mouse button released";
                    });
        }

        private static McpServerFeatures.AsyncToolSpecification createMouseScrollTool() {
            String schema = """
                {
                  "type": "object",
                  "properties": {
                    "amount": {"type": "integer", "description": "Scroll amount (positive = down, negative = up)"},
                    "x": {"type": "integer"},
                    "y": {"type": "integer"},
                    "grid": {"type": "string"}
                  },
                  "required": ["amount"]
                }
                """;

            return createTool("mouse_scroll",
                    "Scroll mouse wheel",
                    schema,
                    args -> {
                        int amount = getIntArg(args, "amount");
                        if (hasPosition(args)) {
                            Point target = resolvePosition(args);
                            mouseController.scroll(target.getX(), target.getY(), amount);
                            return String.format("Scrolled %d at (%d, %d)", amount, target.getX(), target.getY());
                        } else {
                            mouseController.scroll(amount);
                            return String.format("Scrolled %d at current position", amount);
                        }
                    });
        }

        private static McpServerFeatures.AsyncToolSpecification createMouseDragTool() {
            String schema = """
                {
                  "type": "object",
                  "properties": {
                    "start_x": {"type": "integer"},
                    "start_y": {"type": "integer"},
                    "start_grid": {"type": "string"},
                    "end_x": {"type": "integer"},
                    "end_y": {"type": "integer"},
                    "end_grid": {"type": "string"},
                    "button": {"type": "string", "enum": ["left", "right", "middle"], "default": "left"},
                    "duration_ms": {"type": "integer", "default": 500}
                  }
                }
                """;

            return createTool("mouse_drag",
                    "Drag from point A to point B",
                    schema,
                    args -> {
                        Point start = resolveStartPosition(args);
                        Point end = resolveEndPosition(args);
                        String buttonName = getStringArg(args, "button", "left");
                        int button = MouseController.parseButton(buttonName);
                        int duration = getIntArg(args, "duration_ms", 500);

                        mouseController.drag(start.getX(), start.getY(), end.getX(), end.getY(), button, duration);
                        return String.format("Dragged from (%d, %d) to (%d, %d)",
                                start.getX(), start.getY(), end.getX(), end.getY());
                    });
        }

        // ==================== KEYBOARD TOOLS ====================

        private static McpServerFeatures.AsyncToolSpecification createKeyPressTool() {
            String schema = """
                {
                  "type": "object",
                  "properties": {
                    "key": {"type": "string", "description": "Key name (e.g., 'A', 'Enter', 'Ctrl', 'F1')"}
                  },
                  "required": ["key"]
                }
                """;

            return createTool("key_press",
                    "Press a key (without releasing)",
                    schema,
                    args -> {
                        String key = getStringArg(args, "key");
                        keyboardController.pressKey(key);
                        return String.format("Key pressed: %s", key);
                    });
        }

        private static McpServerFeatures.AsyncToolSpecification createKeyReleaseTool() {
            String schema = """
                {
                  "type": "object",
                  "properties": {
                    "key": {"type": "string"}
                  },
                  "required": ["key"]
                }
                """;

            return createTool("key_release",
                    "Release a pressed key",
                    schema,
                    args -> {
                        String key = getStringArg(args, "key");
                        keyboardController.releaseKey(key);
                        return String.format("Key released: %s", key);
                    });
        }

        private static McpServerFeatures.AsyncToolSpecification createKeyTypeTool() {
            String schema = """
                {
                  "type": "object",
                  "properties": {
                    "text": {"type": "string", "description": "Text to type"},
                    "delay_ms": {"type": "integer", "default": 50, "description": "Delay between keystrokes"}
                  },
                  "required": ["text"]
                }
                """;

            return createTool("key_type",
                    "Type a string of text",
                    schema,
                    args -> {
                        String text = getStringArg(args, "text");
                        int delay = getIntArg(args, "delay_ms", 50);
                        keyboardController.type(text, delay);
                        return String.format("Typed %d characters", text.length());
                    });
        }

        private static McpServerFeatures.AsyncToolSpecification createKeyComboTool() {
            String schema = """
                {
                  "type": "object",
                  "properties": {
                    "keys": {
                      "type": "array",
                      "items": {"type": "string"},
                      "description": "Keys to press together (e.g., ['Ctrl', 'C'])"
                    }
                  },
                  "required": ["keys"]
                }
                """;

            return createTool("key_combo",
                    "Execute a key combination (e.g., Ctrl+C)",
                    schema,
                    args -> {
                        @SuppressWarnings("unchecked")
                        List<String> keys = (List<String>) args.get("keys");
                        keyboardController.combo(keys);
                        return String.format("Key combo executed: %s", String.join("+", keys));
                    });
        }

        // ==================== GRID TOOLS ====================

        private static McpServerFeatures.AsyncToolSpecification createGridConfigureTool() {
            String schema = """
                {
                  "type": "object",
                  "properties": {
                    "rows": {"type": "integer", "minimum": 1, "maximum": 26, "default": 3},
                    "columns": {"type": "integer", "minimum": 1, "maximum": 26, "default": 3},
                    "label_scheme": {
                      "type": "string",
                      "enum": ["ALPHANUMERIC", "NUMERIC", "ALPHA"],
                      "default": "ALPHANUMERIC"
                    }
                  }
                }
                """;

            return createTool("grid_configure",
                    "Configure grid dimensions and labeling scheme",
                    schema,
                    args -> {
                        int rows = getIntArg(args, "rows", 3);
                        int columns = getIntArg(args, "columns", 3);
                        String schemeStr = getStringArg(args, "label_scheme", "ALPHANUMERIC");
                        GridConfiguration.LabelScheme scheme =
                                GridConfiguration.LabelScheme.valueOf(schemeStr.toUpperCase());

                        GridConfiguration.getInstance().configure(rows, columns, scheme);
                        return GridConfiguration.getInstance().describe();
                    });
        }

        private static McpServerFeatures.AsyncToolSpecification createGridGetConfigTool() {
            String schema = """
                {"type": "object", "properties": {}}
                """;

            return createTool("grid_get_config",
                    "Get current grid configuration",
                    schema,
                    args -> GridConfiguration.getInstance().describe());
        }

        private static McpServerFeatures.AsyncToolSpecification createGridToPixelTool() {
            String schema = """
                {
                  "type": "object",
                  "properties": {
                    "grid": {"type": "string", "description": "Grid coordinate (e.g., 'A1' or 'A1.B3')"}
                  },
                  "required": ["grid"]
                }
                """;

            return createTool("grid_to_pixel",
                    "Convert grid coordinate to pixel coordinate",
                    schema,
                    args -> {
                        String grid = getStringArg(args, "grid");
                        Point pixel = gridResolver.resolve(grid);
                        Rectangle bounds = gridResolver.resolveToBounds(grid);
                        return String.format("Grid '%s' -> Center: (%d, %d), Bounds: %s",
                                grid, pixel.getX(), pixel.getY(), bounds);
                    });
        }

        // ==================== SCREENSHOT TOOLS ====================

        private static McpServerFeatures.AsyncToolSpecification createScreenshotFullTool() {
            String schema = """
                {
                  "type": "object",
                  "properties": {
                    "monitor": {"type": "integer", "default": 0, "description": "Monitor index (0 for primary)"}
                  }
                }
                """;

            return createTool("screenshot_full",
                    "Capture entire screen as base64 PNG",
                    schema,
                    args -> {
                        int monitor = getIntArg(args, "monitor", 0);
                        String base64 = screenController.captureScreenAsBase64(monitor);
                        Rectangle bounds = screenController.getPrimaryScreenBounds();
                        return String.format("Screenshot captured (%dx%d). Base64 data:\n%s",
                                bounds.getWidth(), bounds.getHeight(), base64);
                    });
        }

        private static McpServerFeatures.AsyncToolSpecification createScreenshotRegionTool() {
            String schema = """
                {
                  "type": "object",
                  "properties": {
                    "x": {"type": "integer"},
                    "y": {"type": "integer"},
                    "width": {"type": "integer"},
                    "height": {"type": "integer"},
                    "grid_start": {"type": "string", "description": "Starting grid cell"},
                    "grid_end": {"type": "string", "description": "Ending grid cell"}
                  }
                }
                """;

            return createTool("screenshot_region",
                    "Capture region by pixel bounds or grid cells",
                    schema,
                    args -> {
                        String base64;
                        String description;

                        if (args.containsKey("grid_start") && args.containsKey("grid_end")) {
                            String start = getStringArg(args, "grid_start");
                            String end = getStringArg(args, "grid_end");
                            base64 = screenController.captureGridRangeAsBase64(start, end);
                            description = String.format("Grid range %s to %s", start, end);
                        } else if (args.containsKey("grid_start")) {
                            String grid = getStringArg(args, "grid_start");
                            base64 = screenController.captureGridCellAsBase64(grid);
                            description = String.format("Grid cell %s", grid);
                        } else {
                            int x = getIntArg(args, "x");
                            int y = getIntArg(args, "y");
                            int width = getIntArg(args, "width");
                            int height = getIntArg(args, "height");
                            base64 = screenController.captureRegionAsBase64(x, y, width, height);
                            description = String.format("Region (%d, %d) %dx%d", x, y, width, height);
                        }

                        return String.format("Screenshot captured: %s. Base64 data:\n%s", description, base64);
                    });
        }

        private static McpServerFeatures.AsyncToolSpecification createScreenshotAtCursorTool() {
            String schema = """
                {
                  "type": "object",
                  "properties": {
                    "width": {"type": "integer", "default": 200},
                    "height": {"type": "integer", "default": 200}
                  }
                }
                """;

            return createTool("screenshot_at_cursor",
                    "Capture region around current cursor position",
                    schema,
                    args -> {
                        int width = getIntArg(args, "width", 200);
                        int height = getIntArg(args, "height", 200);
                        Point cursor = screenController.getMousePosition();
                        String base64 = screenController.captureAtCursorAsBase64(width, height);
                        return String.format("Screenshot captured around cursor (%d, %d), %dx%d. Base64 data:\n%s",
                                cursor.getX(), cursor.getY(), width, height, base64);
                    });
        }

        // ==================== COMMAND SEQUENCE TOOL ====================

        private static McpServerFeatures.AsyncToolSpecification createExecuteSequenceTool() {
            String schema = """
                {
                  "type": "object",
                  "properties": {
                    "commands": {
                      "type": "array",
                      "items": {
                        "type": "object",
                        "properties": {
                          "tool": {"type": "string", "description": "Tool name to execute"},
                          "params": {"type": "object", "description": "Tool parameters"},
                          "delay_after_ms": {"type": "integer", "default": 0}
                        },
                        "required": ["tool", "params"]
                      }
                    }
                  },
                  "required": ["commands"]
                }
                """;

            return createTool("execute_sequence",
                    "Execute a list of commands in order with optional delays",
                    schema,
                    args -> {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> commands = (List<Map<String, Object>>) args.get("commands");
                        StringBuilder results = new StringBuilder();
                        results.append(String.format("Executing %d commands:\n", commands.size()));

                        for (int i = 0; i < commands.size(); i++) {
                            Map<String, Object> cmd = commands.get(i);
                            String toolName = (String) cmd.get("tool");
                            @SuppressWarnings("unchecked")
                            Map<String, Object> params = (Map<String, Object>) cmd.get("params");
                            int delay = cmd.containsKey("delay_after_ms")
                                    ? ((Number) cmd.get("delay_after_ms")).intValue() : 0;

                            results.append(String.format("%d. %s: ", i + 1, toolName));
                            try {
                                String result = executeToolByName(toolName, params);
                                results.append(result).append("\n");
                            } catch (Exception e) {
                                results.append("ERROR: ").append(e.getMessage()).append("\n");
                            }

                            if (delay > 0 && i < commands.size() - 1) {
                                Thread.sleep(delay);
                            }
                        }

                        return results.toString();
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

        private static String executeToolByName(String toolName, Map<String, Object> params) throws Exception {
            switch (toolName) {
                case "mouse_move": return handleMouseMove(params);
                case "mouse_left_click": return handleMouseLeftClick(params);
                case "mouse_right_click": return handleMouseRightClick(params);
                case "mouse_double_click": return handleMouseDoubleClick(params);
                case "mouse_middle_click": return handleMouseMiddleClick(params);
                case "mouse_scroll": return handleMouseScroll(params);
                case "key_press": return handleKeyPress(params);
                case "key_release": return handleKeyRelease(params);
                case "key_type": return handleKeyType(params);
                case "key_combo": return handleKeyCombo(params);
                default: throw new IllegalArgumentException("Unknown tool: " + toolName);
            }
        }

        private static String handleMouseMove(Map<String, Object> args) {
            Point target = resolvePosition(args);
            mouseController.moveTo(target);
            return String.format("Moved to (%d, %d)", target.getX(), target.getY());
        }

        private static String handleMouseLeftClick(Map<String, Object> args) {
            if (hasPosition(args)) {
                Point target = resolvePosition(args);
                mouseController.leftClick(target.getX(), target.getY());
            } else {
                mouseController.leftClick();
            }
            return "Left clicked";
        }

        private static String handleMouseRightClick(Map<String, Object> args) {
            if (hasPosition(args)) {
                Point target = resolvePosition(args);
                mouseController.rightClick(target.getX(), target.getY());
            } else {
                mouseController.rightClick();
            }
            return "Right clicked";
        }

        private static String handleMouseDoubleClick(Map<String, Object> args) {
            if (hasPosition(args)) {
                Point target = resolvePosition(args);
                mouseController.doubleClick(target.getX(), target.getY());
            } else {
                mouseController.doubleClick();
            }
            return "Double clicked";
        }

        private static String handleMouseMiddleClick(Map<String, Object> args) {
            if (hasPosition(args)) {
                Point target = resolvePosition(args);
                mouseController.middleClick(target.getX(), target.getY());
            } else {
                mouseController.middleClick();
            }
            return "Middle clicked";
        }

        private static String handleMouseScroll(Map<String, Object> args) {
            int amount = getIntArg(args, "amount");
            mouseController.scroll(amount);
            return String.format("Scrolled %d", amount);
        }

        private static String handleKeyPress(Map<String, Object> args) {
            String key = getStringArg(args, "key");
            keyboardController.pressKey(key);
            return "Key pressed: " + key;
        }

        private static String handleKeyRelease(Map<String, Object> args) {
            String key = getStringArg(args, "key");
            keyboardController.releaseKey(key);
            return "Key released: " + key;
        }

        private static String handleKeyType(Map<String, Object> args) {
            String text = getStringArg(args, "text");
            keyboardController.type(text);
            return "Typed: " + text;
        }

        private static String handleKeyCombo(Map<String, Object> args) {
            @SuppressWarnings("unchecked")
            List<String> keys = (List<String>) args.get("keys");
            keyboardController.combo(keys);
            return "Combo: " + String.join("+", keys);
        }

        // ==================== ARGUMENT HELPERS ====================

        private static boolean hasPosition(Map<String, Object> args) {
            return args.containsKey("x") && args.containsKey("y") || args.containsKey("grid");
        }

        private static Point resolvePosition(Map<String, Object> args) {
            if (args.containsKey("grid")) {
                String grid = getStringArg(args, "grid");
                return gridResolver.resolve(grid);
            }
            int x = getIntArg(args, "x");
            int y = getIntArg(args, "y");
            return new Point(x, y);
        }

        private static Point resolveStartPosition(Map<String, Object> args) {
            if (args.containsKey("start_grid")) {
                String grid = getStringArg(args, "start_grid");
                return gridResolver.resolve(grid);
            }
            if (args.containsKey("start_x") && args.containsKey("start_y")) {
                return new Point(getIntArg(args, "start_x"), getIntArg(args, "start_y"));
            }
            return mouseController.getPosition();
        }

        private static Point resolveEndPosition(Map<String, Object> args) {
            if (args.containsKey("end_grid")) {
                String grid = getStringArg(args, "end_grid");
                return gridResolver.resolve(grid);
            }
            return new Point(getIntArg(args, "end_x"), getIntArg(args, "end_y"));
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

        private static int getIntArg(Map<String, Object> args, String key) {
            Object value = args.get(key);
            if (value == null) {
                throw new IllegalArgumentException("Required parameter missing: " + key);
            }
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return Integer.parseInt(value.toString());
        }

        private static int getIntArg(Map<String, Object> args, String key, int defaultValue) {
            Object value = args.get(key);
            if (value == null) return defaultValue;
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return Integer.parseInt(value.toString());
        }

        private static boolean getBoolArg(Map<String, Object> args, String key, boolean defaultValue) {
            Object value = args.get(key);
            if (value == null) return defaultValue;
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
            return Boolean.parseBoolean(value.toString());
        }
    }
}
