package com.robot.home.collector.fetcher;

import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BrowserEgressDenyProxyTest {

    @Test
    void acceptsLocalProxyConnectionsAndClosesThemWithoutForwarding() throws Exception {
        try (BrowserEgressDenyProxy proxy = new BrowserEgressDenyProxy();
             Socket client = new Socket()) {
            client.connect(new InetSocketAddress("127.0.0.1", proxy.port()), 1000);
            client.setSoTimeout(2000);

            assertEquals(-1, client.getInputStream().read());
        }
    }
}
