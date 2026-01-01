package com.uioperator.vision.mcp;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration settings for the UI Vision MCP server.
 */
public class McpConfig {

    public enum TransportMode {
        STDIO,
        HTTP
    }

    private final TransportMode transportMode;
    private final String httpHost;
    private final int httpPort;
    private final String httpEndpoint;
    private final boolean captureServerLogs;
    private final Path logDirectory;

    private McpConfig(Builder builder) {
        this.transportMode = builder.transportMode;
        this.httpHost = builder.httpHost;
        this.httpPort = builder.httpPort;
        this.httpEndpoint = builder.httpEndpoint;
        this.captureServerLogs = builder.captureServerLogs;
        this.logDirectory = builder.logDirectory;
    }

    public TransportMode getTransportMode() {
        return transportMode;
    }

    public String getHttpHost() {
        return httpHost;
    }

    public int getHttpPort() {
        return httpPort;
    }

    public String getHttpEndpoint() {
        return httpEndpoint;
    }

    public boolean isCaptureServerLogs() {
        return captureServerLogs;
    }

    public Path getLogDirectory() {
        return logDirectory;
    }

    public String getHttpUrl() {
        return String.format("http://%s:%d%s", httpHost, httpPort, httpEndpoint);
    }

    public static McpConfig defaultHttp() {
        return builder()
                .transportMode(TransportMode.HTTP)
                .httpPort(8086)
                .build();
    }

    public static McpConfig defaultStdio() {
        return builder()
                .transportMode(TransportMode.STDIO)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private TransportMode transportMode = TransportMode.HTTP;
        private String httpHost = "localhost";
        private int httpPort = 8086;
        private String httpEndpoint = "/mcp";
        private boolean captureServerLogs = true;
        private Path logDirectory = Paths.get("./logs");

        public Builder transportMode(TransportMode transportMode) {
            this.transportMode = transportMode;
            return this;
        }

        public Builder httpHost(String httpHost) {
            this.httpHost = httpHost;
            return this;
        }

        public Builder httpPort(int httpPort) {
            this.httpPort = httpPort;
            return this;
        }

        public Builder httpEndpoint(String httpEndpoint) {
            this.httpEndpoint = httpEndpoint;
            return this;
        }

        public Builder captureServerLogs(boolean captureServerLogs) {
            this.captureServerLogs = captureServerLogs;
            return this;
        }

        public Builder logDirectory(Path logDirectory) {
            this.logDirectory = logDirectory;
            return this;
        }

        public McpConfig build() {
            return new McpConfig(this);
        }
    }

    @Override
    public String toString() {
        return String.format("McpConfig{mode=%s, url=%s}",
                transportMode,
                transportMode == TransportMode.HTTP ? getHttpUrl() : "stdio");
    }
}
