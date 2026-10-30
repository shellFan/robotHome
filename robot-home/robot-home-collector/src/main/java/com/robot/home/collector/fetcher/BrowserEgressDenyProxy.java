package com.robot.home.collector.fetcher;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * A loopback-only deny proxy for Chromium requests that escape Playwright
 * routing. It never forwards bytes or resolves destination hosts.
 */
final class BrowserEgressDenyProxy implements Closeable {

    private final ServerSocket listener;
    private final Thread worker;
    private volatile boolean running = true;

    BrowserEgressDenyProxy() throws IOException {
        listener = new ServerSocket();
        listener.bind(new InetSocketAddress(
                InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 0));
        listener.setSoTimeout(250);
        worker = new Thread(this::acceptAndDrop, "robot-home-browser-egress-deny");
        worker.setDaemon(true);
        worker.start();
    }

    int port() {
        return listener.getLocalPort();
    }

    String serverAddress() {
        return "http://127.0.0.1:" + port();
    }

    private void acceptAndDrop() {
        while (running) {
            try (Socket ignored = listener.accept()) {
                // Close immediately. This listener is intentionally not a proxy.
            } catch (SocketTimeoutException ignored) {
                // Re-check the close flag periodically.
            } catch (IOException e) {
                if (running) {
                    running = false;
                }
            }
        }
    }

    @Override
    public void close() throws IOException {
        running = false;
        listener.close();
        worker.interrupt();
        if (Thread.currentThread() != worker) {
            try {
                worker.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
