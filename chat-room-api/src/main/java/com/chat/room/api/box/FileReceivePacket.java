package com.chat.room.api.box;

import com.chat.room.api.box.abs.ReceivePacket;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * 文件接受包
 */
public class FileReceivePacket extends ReceivePacket<FileOutputStream, File> {

    private final File file;

    public FileReceivePacket(long len, File file) {
        super(len);
        this.file = file;
    }

    /**
     * 从流转变为对应尸体时直接返回创建实传入的file文件
     *
     * @param stream 文件传输流
     * @return
     */
    @Override
    protected File buildEntity(FileOutputStream stream) {
        return file;
    }

    @Override
    public byte type() {
        return TYPE_STREAM_FILE;
    }

    @Override
    protected FileOutputStream createStream() {
        try {
            return new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            return null;
        }
    }
}
