package com.github.pmiaowu.debug.bootstrap;

import org.apache.commons.net.telnet.TelnetClient;
import org.apache.commons.net.telnet.WindowSizeOptionHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

public class TelnetUtil {
    private TelnetClient telnet;
    private InputStream in;
    private PrintStream out;

    public synchronized void connect(String host, int port) throws IOException {
        this.connect(host, port, "VT100");
    }

    public synchronized void connect(String host, int port, String termtype) throws IOException {
        if (telnet == null) {
            telnet = new TelnetClient(termtype);
            telnet.connect(host, port);

            in = telnet.getInputStream();
            out = new PrintStream(telnet.getOutputStream());
        }
    }

    /**
     * 内容发送
     *
     * @param value
     */
    public void write(String value) {
        out.println(value);
        out.flush();
    }

    /**
     * 内容读取
     *
     * @param pattern
     * @return
     */
    public String readUntil(String pattern) {
        try {
            char lastChar = pattern.charAt(pattern.length() - 1);
            StringBuffer sb = new StringBuffer();
            char ch = (char) in.read();
            while (true) {
                sb.append(ch);
                if (ch == lastChar) {
                    if (sb.toString().endsWith(pattern)) {
                        return sb.toString();
                    }
                }
                ch = (char) in.read();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 发送命令,并回显
     *
     * @param command
     * @param pattern
     * @return
     */
    public String sendCommand(String command, String pattern) {
        write(command);
        return readUntil(pattern);
    }

    /**
     * 是否连接正常
     * true=连接正常, false=连接失败
     *
     * @return
     */
    public boolean isConnected() {
        return telnet.isConnected();
    }

    /**
     * 关闭连接
     */
    public void disconnect() {
        try {
            telnet.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置客户端窗口大小
     *
     * @param width  命令行宽度
     * @param height 命令行高度
     */
    public void setWindowSize(int width, int height) {
        WindowSizeOptionHandler sizeOptionHandler = new WindowSizeOptionHandler(width, height);
        try {
            telnet.sendSubnegotiation(sizeOptionHandler.startSubnegotiationLocal());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}