package com.chat.room.api.box.impl;

import com.chat.room.api.box.SendPacket;

import java.io.File;
import java.io.FileInputStream;

public class FileSendPacket extends SendPacket<FileInputStream> {

    public FileSendPacket(File file) {
        this.length = file.length();
    }

    @Override
    protected FileInputStream createStream() {
        return null;
    }
}
