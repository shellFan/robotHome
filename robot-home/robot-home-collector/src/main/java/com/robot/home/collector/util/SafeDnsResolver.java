package com.robot.home.collector.util;

import okhttp3.Dns;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Resolves a hostname once per new HTTP connection and returns only addresses
 * that have passed the collector's SSRF address policy. OkHttp dials one of
 * these exact InetAddress values while retaining the original URL host for
 * Host headers and TLS hostname verification.
 */
public final class SafeDnsResolver implements Dns {

    private final AddressResolver addressResolver;

    public SafeDnsResolver() {
        this(InetAddress::getAllByName);
    }

    public SafeDnsResolver(AddressResolver addressResolver) {
        if (addressResolver == null) {
            throw new IllegalArgumentException("addressResolver is required");
        }
        this.addressResolver = addressResolver;
    }

    @Override
    public List<InetAddress> lookup(String hostname) throws UnknownHostException {
        if (hostname == null || hostname.trim().isEmpty()) {
            throw new UnknownHostException("DNS hostname is empty");
        }

        InetAddress[] addresses = addressResolver.resolve(hostname);
        if (addresses == null || addresses.length == 0) {
            throw new UnknownHostException("DNS returned no addresses for " + hostname);
        }

        List<InetAddress> validated = new ArrayList<>(addresses.length);
        for (InetAddress address : addresses) {
            if (UrlSecurityUtil.isPrivateAddress(address)) {
                throw new UnknownHostException(
                        "DNS answer contains a private or non-public address for " + hostname);
            }
            validated.add(address);
        }
        return Collections.unmodifiableList(validated);
    }

    @FunctionalInterface
    public interface AddressResolver {
        InetAddress[] resolve(String hostname) throws UnknownHostException;
    }
}
