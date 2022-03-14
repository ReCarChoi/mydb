package xyz.recarchoi.mydb.backend.util;

import java.nio.ByteBuffer;

/**
 * @author recarchoi
 * @since 2022/3/8 9:01
 */
public class Parser {
    public static long parseLong(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        return buffer.getLong();
    }

    public static byte[] long2Byte(long value) {
        return ByteBuffer.allocate(Long.SIZE / Byte.SIZE).putLong(value).array();
    }
}
