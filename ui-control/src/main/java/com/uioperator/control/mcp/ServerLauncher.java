package com.uioperator.control.mcp;

import com.uioperator.control.UiControlMcpServer;
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
 * Manages the UI Control MCP server lifecycle.
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

    /**
     * Constructs a new ServerLauncher with the given configuration.
     *
     * @param config the MCP server configuration
     */
    public ServerLauncher(McpConfig config) {
        this.config = config;
    }

    /**
     * Start MCP server in background thread.
     * Returns CompletableFuture that completes when server is ready.
     *
     * @return future that completes with true if server started successfully
     */
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
                logger.info("UI Control MCP Server started successfully in {} mode", config.getTransportMode());
                return true;
            } catch (Exception e) {
                logger.error("Failed to start MCP server", e);
                running = false;
                return false;
            }
        });
    }

    /**
     * Start HTTP server.
     */
    private void startHttpServer() throws Exception {
        logger.info("Starting HTTP server on port {}", config.getHttpPort());

        HttpServletStatelessServerTransport transport = HttpServletStatelessServerTransport.builder()
                .jsonMapper(McpJsonMapper.createDefault())
                .messageEndpoint(config.getHttpEndpoint())
                .build();

        // Create stateless sync tools
        var tools = UiControlMcpServer.ToolFactory.createAllStatelessTools();

        syncServer = McpServer.sync(transport)
                .serverInfo("ui-control-server", getVersion())
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

    /**
     * Start stdio server.
     */
    private void startStdioServer() throws InterruptedException {
        logger.info("Starting stdio server");

        asyncServer = UiControlMcpServer.createStdioServer();

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
        }, "UI-Control-MCP-Server-Thread");

        serverThread.setDaemon(true);
        serverThread.start();

        Thread.sleep(500);
        logger.info("Stdio server started");
    }

    /**
     * Shutdown the server cleanly.
     */
    public void shutdown() {
        if (!running) {
            return;
        }

        logger.info("Shutting down UI Control MCP server...");
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

            logger.info("UI Control MCP server shutdown complete");
        } catch (Exception e) {
            logger.error("Error during server shutdown", e);
        }
    }

    /**
     * Check if server is currently running.
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Get the endpoint URL or description.
     */
    public String getEndpointUrl() {
        if (config.getTransportMode() == McpConfig.TransportMode.HTTP) {
            return config.getHttpUrl();
        } else {
            return "stdio (standard input/output)";
        }
    }

    /**
     * Get server version.
     */
    private String getVersion() {
        return "1.0.0";
    }
}
