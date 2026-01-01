package com.uioperator.vision.mcp;

import com.uioperator.vision.UiVisionMcpServer;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpStatelessSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletStatelessServerTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

/**
 * Manages the UI Vision MCP server lifecycle.
 * Supports both HTTP and STDIO transport modes.
 */
public class ServerLauncher {

    private static final Logger logger = LoggerFactory.getLogger(ServerLauncher.class);

    private final McpConfig config;

    private McpAsyncServer asyncServer;
    private McpStatelessSyncServer syncServer;
    private Server jettyServer;
    private Thread serverThread;
    private CountDownLatch stdioLatch;
    private volatile boolean running = false;

    public ServerLauncher(McpConfig config) {
        this.config = config;
    }

    public CompletableFuture<Boolean> startAsync() {
        if (running) {
            logger.warn("Server is already running");
            return CompletableFuture.completedFuture(true);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                if (config.getTransportMode() == McpConfig.TransportMode.HTTP) {
                    startHttpServer();
                } else {
                    startStdioServer();
                }
                running = true;
                logger.info("UI Vision MCP Server started successfully in {} mode", config.getTransportMode());
                return true;
            } catch (Exception e) {
                logger.error("Failed to start MCP server", e);
                running = false;
                return false;
            }
        });
    }

    private void startHttpServer() throws Exception {
        logger.info("Starting HTTP server on port {}", config.getHttpPort());

        HttpServletStatelessServerTransport transport = HttpServletStatelessServerTransport.builder()
                .jsonMapper(McpJsonMapper.createDefault())
                .messageEndpoint(config.getHttpEndpoint())
                .build();

        var tools = UiVisionMcpServer.ToolFactory.createAllStatelessTools();

        syncServer = McpServer.sync(transport)
                .serverInfo("ui-vision-server", "1.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(true)
                        .build())
                .tools(tools.toArray(new io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncToolSpecification[0]))
                .build();

        jettyServer = new Server(config.getHttpPort());
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        jettyServer.setHandler(context);

        ServletHolder servletHolder = new ServletHolder(transport);
        context.addServlet(servletHolder, config.getHttpEndpoint());

        jettyServer.start();
        logger.info("HTTP server started: {}", getEndpointUrl());
    }

    private void startStdioServer() throws InterruptedException {
        logger.info("Starting stdio server");

        asyncServer = UiVisionMcpServer.createStdioServer();

        stdioLatch = new CountDownLatch(1);
        serverThread = new Thread(() -> {
            try {
                logger.info("Stdio server thread running...");
                stdioLatch.await();
            } catch (InterruptedException e) {
                logger.info("Stdio server thread interrupted");
            } catch (Exception e) {
                logger.error("Server thread error", e);
                running = false;
            }
        }, "UI-Vision-MCP-Server-Thread");

        serverThread.setDaemon(true);
        serverThread.start();

        Thread.sleep(500);
        logger.info("Stdio server started");
    }

    public void shutdown() {
        if (!running) {
            return;
        }

        logger.info("Shutting down UI Vision MCP server...");
        running = false;

        try {
            if (asyncServer != null) {
                asyncServer.close();
                asyncServer = null;
            }

            if (syncServer != null) {
                syncServer.close();
                syncServer = null;
            }

            if (jettyServer != null) {
                jettyServer.stop();
                jettyServer = null;
            }

            if (stdioLatch != null) {
                stdioLatch.countDown();
            }

            if (serverThread != null && serverThread.isAlive()) {
                serverThread.interrupt();
                serverThread.join(2000);
            }

            logger.info("UI Vision MCP server shutdown complete");
        } catch (Exception e) {
            logger.error("Error during server shutdown", e);
        }
    }

    public boolean isRunning() {
        return running;
    }

    public String getEndpointUrl() {
        if (config.getTransportMode() == McpConfig.TransportMode.HTTP) {
            return config.getHttpUrl();
        } else {
            return "stdio (standard input/output)";
        }
    }
}
