package com.robot.home.collector.fetcher;

import com.robot.home.collector.util.SafeDnsResolver;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SafeDnsResolverTest {

    @Test
    void rejectsTheWholeDnsAnswerIfAnyAddressIsPrivate() throws Exception {
        InetAddress publicAddress = address("203.0.113.10");
        InetAddress loopbackAddress = address("127.0.0.1");
        SafeDnsResolver resolver = new SafeDnsResolver(host ->
                new InetAddress[]{publicAddress, loopbackAddress});

        UnknownHostException error = assertThrows(
                UnknownHostException.class, () -> resolver.lookup("rebind.example"));

        assertTrue(error.getMessage().contains("private"));
    }

    @Test
    void returnsTheSameValidatedAddressesForTheConnectorToUse() throws Exception {
        InetAddress first = address("8.8.8.8");
        InetAddress second = address("1.1.1.1");
        SafeDnsResolver resolver = new SafeDnsResolver(host ->
                new InetAddress[]{first, second});

        List<InetAddress> result = resolver.lookup("public.example");

        assertEquals(2, result.size());
        assertSame(first, result.get(0));
        assertSame(second, result.get(1));
    }

    @Test
    void rejectsEmptyDnsAnswers() {
        SafeDnsResolver resolver = new SafeDnsResolver(host -> new InetAddress[0]);

        assertThrows(UnknownHostException.class, () -> resolver.lookup("empty.example"));
    }

    private static InetAddress address(String value) throws Exception {
        return InetAddress.getByName(value);
    }
}
