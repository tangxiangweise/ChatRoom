package com.chat.room.api.utils.plugin;

import java.io.IOException;

public class BufferOverflowException extends IOException {

    /**
     * Serial version ID
     */
    private static final long serialVersionUID = -322401823167626048L;

    /**
     * Create a new Exception
     *
     * @since ostermillerutils 1.00.00
     */
    public BufferOverflowException() {
        super();
    }

    /**
     * Create a new Exception with the given message.
     *
     * @param msg Error message.
     * @since ostermillerutils 1.00.00
     */
    public BufferOverflowException(String msg) {
        super(msg);
    }
}
