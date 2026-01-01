# UI-Operator-Agent

MCP-based agent system for UI automation and verification. Provides fine-grained mouse and keyboard control with a grid-based coordinate system (Blade Runner "enhance" style) and screenshot analysis capabilities.

## Architecture

Two MCP servers working together:

1. **ui-control** (port 8085) - Fine-grained mouse, keyboard, grid, and screenshot control
2. **ui-vision** (port 8086) - Screenshot analysis, comparison, and annotation

## Features

### UI Control Server (22 tools)

**Mouse Operations:**
- `mouse_move` - Move to pixel coordinates or grid cell
- `mouse_left_click` / `mouse_right_click` / `mouse_double_click` - Clicks with modifier support (Shift, Ctrl, Alt)
- `mouse_middle_click` - Middle button click
- `mouse_press_left` / `mouse_release_left` - Fine-grained left button control
- `mouse_press_right` / `mouse_release_right` - Fine-grained right button control
- `mouse_scroll` - Scroll wheel control
- `mouse_drag` - Drag from point A to B

**Keyboard Operations:**
- `key_press` / `key_release` - Individual key control
- `key_type` - Type text strings
- `key_combo` - Execute key combinations (e.g., Ctrl+C)

**Grid System (Blade Runner "Enhance"):**
- `grid_configure` - Set NxM grid dimensions (up to 26x26)
- `grid_get_config` - Get current grid configuration
- `grid_to_pixel` - Convert grid coordinate to pixels

Grid coordinates support recursive sub-grids:
- `A1` - Top-level cell
- `A1.B3` - Cell B3 within cell A1
- `A1.B3.C2` - Cell C2 within B3 within A1 (unlimited depth)

**Screenshot Operations:**
- `screenshot_full` - Capture entire screen
- `screenshot_region` - Capture by pixels or grid cells
- `screenshot_at_cursor` - Capture around cursor

**Command Batching:**
- `execute_sequence` - Execute multiple commands with delays

### UI Vision Server (5 tools)

- `analyze_screenshot` - Describe screenshot contents
- `analyze_action_result` - Compare before/after screenshots
- `find_element` - Locate UI elements by description
- `compare_screenshots` - Diff two screenshots
- `annotate_screenshot` - Add circles, arrows, text, rectangles

## Building

```bash
# Build all modules
./gradlew build

# Build server JARs
./gradlew :ui-control:serverJar :ui-vision:serverJar
```

## Running

### STDIO Mode (for Claude Desktop)

```bash
# UI Control Server
java -jar ui-control/build/libs/ui-control-mcp-server-all.jar --stdio

# UI Vision Server
java -jar ui-vision/build/libs/ui-vision-mcp-server-all.jar --stdio
```

### HTTP Mode

```bash
# UI Control Server (port 8085)
java -jar ui-control/build/libs/ui-control-mcp-server-all.jar --http 8085

# UI Vision Server (port 8086)
java -jar ui-vision/build/libs/ui-vision-mcp-server-all.jar --http 8086
```

## Claude Desktop Configuration

Add to `claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "ui-control": {
      "command": "java",
      "args": ["-jar", "C:/path/to/ui-control-mcp-server-all.jar", "--stdio"]
    },
    "ui-vision": {
      "command": "java",
      "args": ["-jar", "C:/path/to/ui-vision-mcp-server-all.jar", "--stdio"]
    }
  }
}
```

## Example Usage

### Grid-Based Click
```json
{"tool": "grid_configure", "params": {"rows": 10, "columns": 10}}
{"tool": "mouse_left_click", "params": {"grid": "E5"}}
```

### Sub-Grid Precision (Blade Runner Enhance)
```json
{"tool": "mouse_move", "params": {"grid": "B2.C3.A1"}}
```

### Screenshot and Analysis Workflow
```json
{"tool": "screenshot_full", "params": {}}
{"tool": "mouse_left_click", "params": {"x": 500, "y": 300}}
{"tool": "screenshot_full", "params": {}}
{"tool": "analyze_action_result", "params": {"before_image": "...", "after_image": "...", "action_description": "Clicked button"}}
```

## Requirements

- Java 17+
- Cross-platform (Windows, macOS, Linux)

## Project Structure

```
ui-operator-agent/
├── common/              # Shared utilities
├── ui-control/          # UI Control MCP Server
│   ├── robot/           # Mouse, keyboard, screen controllers
│   ├── grid/            # Grid coordinate system
│   └── mcp/             # MCP server infrastructure
└── ui-vision/           # UI Vision MCP Server
    ├── analysis/        # Screenshot analysis
    └── annotation/      # Visual annotations
```
