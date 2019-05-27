package com.chat.room.api.utils;

import java.io.Closeable;

public class CloseUtils {

    public static void close(Closeable... closeables) {
        if (closeables == null) {
            return;
        }
        try {
            for (Closeable closeable : closeables) {
                closeable.close();
            }
        } catch (Exception e) {

        }
    }
}
