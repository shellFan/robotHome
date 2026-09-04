package com.robot.home.collector.util;

import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * 哈希工具类
 * 提供SHA-256内容哈希和SimHash相似度去重
 */
public class HashUtils {

    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    /**
     * 计算SHA-256哈希
     */
    public static String sha256(String input) {
        if (input == null) {
            return "";
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * 计算SHA-256哈希（带前缀）
     */
    public static String sha256WithPrefix(String input) {
        String hash = sha256(input);
        return "sha256:" + hash;
    }

    /**
     * SimHash计算（64位）
     * 用于内容相似度检测，海明距离<=3认为相似
     */
    public static long simHash(String content) {
        return simHash(content, 64);
    }

    /**
     * SimHash计算
     * @param content 文本内容
     * @param hashBits 哈希位数（通常64）
     */
    public static long simHash(String content, int hashBits) {
        if (StringUtils.isBlank(content)) {
            return 0;
        }

        // 分词：简单按标点和空格分割，取2-gram
        List<String> tokens = tokenize(content);
        if (tokens.isEmpty()) {
            return 0;
        }

        int[] vector = new int[hashBits];

        for (String token : tokens) {
            long tokenHash = murmurHash64(token);
            for (int i = 0; i < hashBits; i++) {
                if ((tokenHash & (1L << i)) != 0) {
                    vector[i] += 1;
                } else {
                    vector[i] -= 1;
                }
            }
        }

        long fingerprint = 0;
        for (int i = 0; i < hashBits; i++) {
            if (vector[i] > 0) {
                fingerprint |= (1L << i);
            }
        }
        return fingerprint;
    }

    /**
     * 计算海明距离
     */
    public static int hammingDistance(long hash1, long hash2) {
        long xor = hash1 ^ hash2;
        int distance = 0;
        while (xor != 0) {
            distance++;
            xor &= xor - 1;
        }
        return distance;
    }

    /**
     * 判断两个SimHash是否相似（海明距离<=3）
     */
    public static boolean isSimilar(long hash1, long hash2) {
        return isSimilar(hash1, hash2, 3);
    }

    /**
     * 判断两个SimHash是否相似
     */
    public static boolean isSimilar(long hash1, long hash2, int threshold) {
        return hammingDistance(hash1, hash2) <= threshold;
    }

    /**
     * SimHash分段，用于Redis存储和快速检索
     * 将64位SimHash分为4段，每段16位
     */
    public static int[] simHashSegments(long simHash) {
        int[] segments = new int[4];
        for (int i = 0; i < 4; i++) {
            segments[i] = (int) ((simHash >>> (i * 16)) & 0xFFFF);
        }
        return segments;
    }

    /**
     * 生成SimHash段的Redis key
     */
    public static String simHashSegmentKey(int segmentIndex, int segmentValue) {
        return "simhash:seg:" + segmentIndex + ":" + segmentValue;
    }

    /**
     * 简单分词：2-gram + 单词
     */
    private static List<String> tokenize(String content) {
        List<String> tokens = new ArrayList<>();

        // 清理HTML标签
        String text = content.replaceAll("<[^>]+>", " ")
                .replaceAll("[\\s\\p{Punct}]+", " ")
                .trim();

        if (text.isEmpty()) {
            return tokens;
        }

        // 按空格分词
        String[] words = text.split("\\s+");

        // 单词token
        for (String word : words) {
            if (word.length() >= 2) {
                tokens.add(word.toLowerCase());
            }
        }

        // 2-gram token（增加语义信息）
        for (int i = 0; i < words.length - 1; i++) {
            if (words[i].length() >= 2 && words[i + 1].length() >= 2) {
                tokens.add(words[i].toLowerCase() + "_" + words[i + 1].toLowerCase());
            }
        }

        return tokens;
    }

    /**
     * MurmurHash3 64位变体（简化版）
     */
    private static long murmurHash64(String key) {
        byte[] data = key.getBytes(StandardCharsets.UTF_8);
        int length = data.length;
        long h = 0x87c37b91114253d5L;
        int n = length / 8;

        for (int i = 0; i < n; i++) {
            long k = getLongLittleEndian(data, i * 8);
            k *= 0x87c37b91114253d5L;
            k = Long.rotateLeft(k, 31);
            k *= 0x4cf5ad432745937fL;
            h ^= k;
            h = Long.rotateLeft(h, 27);
            h = h * 5 + 0x52dce729;
        }

        // 处理剩余字节
        int remaining = length & 7;
        if (remaining > 0) {
            long k = 0;
            for (int i = remaining; i > 0; i--) {
                k <<= 8;
                k |= (data[length - i] & 0xFF);
            }
            k *= 0x87c37b91114253d5L;
            k = Long.rotateLeft(k, 31);
            k *= 0x4cf5ad432745937fL;
            h ^= k;
        }

        h ^= length;
        h ^= (h >>> 33);
        h *= 0xff51afd7ed558ccdL;
        h ^= (h >>> 33);
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= (h >>> 33);

        return h;
    }

    private static long getLongLittleEndian(byte[] data, int offset) {
        return ((long) data[offset] & 0xFF)
                | (((long) data[offset + 1] & 0xFF) << 8)
                | (((long) data[offset + 2] & 0xFF) << 16)
                | (((long) data[offset + 3] & 0xFF) << 24)
                | (((long) data[offset + 4] & 0xFF) << 32)
                | (((long) data[offset + 5] & 0xFF) << 40)
                | (((long) data[offset + 6] & 0xFF) << 48)
                | (((long) data[offset + 7] & 0xFF) << 56);
    }

    /**
     * 字节数组转十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(HEX_CHARS[(b >> 4) & 0x0F]);
            sb.append(HEX_CHARS[b & 0x0F]);
        }
        return sb.toString();
    }
}