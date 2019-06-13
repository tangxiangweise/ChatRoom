package com.chat.room.client;

import com.chat.room.api.bean.ServerInfo;
import com.chat.room.api.box.FileSendPacket;
import com.chat.room.api.constants.Foo;
import com.chat.room.api.core.Connector;
import com.chat.room.api.core.IoContext;
import com.chat.room.api.core.impl.IoSelectorProvider;
import com.chat.room.api.core.impl.SchedulerImpl;
import com.chat.room.api.core.schedule.IdleTimeoutScheduleJob;
import com.chat.room.api.core.schedule.ScheduleJob;
import com.chat.room.api.handler.ConnectorCloseChain;
import com.chat.room.api.handler.ConnectorHandler;
import com.chat.room.api.utils.CloseUtils;

import java.io.*;
import java.util.concurrent.TimeUnit;

public class Client {

    public static void main(String[] args) throws IOException {

        File cachePath = Foo.getCacheDir("client");
        IoContext.setup().ioProvider(new IoSelectorProvider()).scheduler(new SchedulerImpl(1)).start();
        ServerInfo serverInfo = ClientSearcher.searchServer(10000);
        System.out.println("Server : " + serverInfo);
        if (serverInfo != null) {
            TCPClient tcpClient = null;
            try {
                tcpClient = TCPClient.startWith(serverInfo, cachePath);
                if (tcpClient == null) {
                    return;
                }
                tcpClient.getCloseChain().appendLast(new ConnectorCloseChain() {
                    @Override
                    protected boolean consume(ConnectorHandler handler, Connector connector) {
                        CloseUtils.close(System.in);
                        return true;
                    }
                });
                ScheduleJob scheduleJob = new IdleTimeoutScheduleJob(50, TimeUnit.SECONDS, tcpClient);
                tcpClient.schedule(scheduleJob);
                write(tcpClient);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (tcpClient != null) {
                    tcpClient.exit();
                }
            }
        }
        IoContext.close();
    }

    private static void write(TCPClient tcpClient) throws IOException {
        //构建键盘输入流
        InputStream in = System.in;
        BufferedReader input = new BufferedReader(new InputStreamReader(in));
        do {
            //键盘读取一行
            String msg = input.readLine();
            if (msg == null || Foo.COMMAND_EXIT.equalsIgnoreCase(msg)) {
                break;
            } else if (msg.length() == 0) {
                continue;
            }
            if (msg.startsWith("--f")) {
                String[] array = msg.split(" ");
                if (array.length >= 2) {
                    String filePath = array[1];
                    File file = new File(filePath);
                    if (file.exists() && file.isFile()) {
                        FileSendPacket packet = new FileSendPacket(file);
                        tcpClient.send(packet);
                        continue;
                    }
                }
            }
            //发送到服务器
            tcpClient.send(msg);
        } while (true);
    }
}
