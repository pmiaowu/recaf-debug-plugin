package com.github.pmiaowu.debug.bootstrap;

import java.io.IOException;

public class ArthasClient {
    /**
     * 客户端
     */
    private TelnetUtil telnet;

    /**
     * 进程id
     */
    private String pid;

    /**
     * 回显提示符开始
     */
    private static String PROMPT_START = "[arthas@";

    /**
     * 回显提示符结束
     */
    private static String PROMPT_END = "]$ ";

    /**
     * 命令末尾添加纯文本命令
     */
    private static String COMMAND_END = " | plaintext";

    public ArthasClient(String host, int port) {
        telnet = new TelnetUtil();
        try {
            telnet.connect(host, port);
            telnet.setWindowSize(10000, 500);
            setPid(telnet.readUntil(PROMPT_END));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置pid
     *
     * @param s
     */
    private void setPid(String s) {
        int startIndex = s.lastIndexOf(PROMPT_START);
        int endIndex = s.lastIndexOf(PROMPT_END);
        this.pid = s.substring(startIndex + PROMPT_START.length(), endIndex);
    }

    /**
     * 获取pid
     *
     * @return
     */
    public String getPid() {
        return this.pid;
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
     * 发送命令,处理返回内容
     *
     * @param command 命令
     * @return
     */
    public String sendCommand(String command) {
        telnet.write(command + COMMAND_END);

        String prɒmpt = PROMPT_START + this.getPid() + PROMPT_END;

        String data = telnet.readUntil(prɒmpt);

        // 处理命令回显
        data = data.substring((command + COMMAND_END).length());

        // 处理回显提示符
        int index = data.lastIndexOf(prɒmpt);
        if (index != -1) {
            data = data.substring(0, index);
        }

        return data;
    }

    /**
     * 关闭连接
     */
    public void disconnect() {
        if (telnet.isConnected()) {
            telnet.disconnect();
        }
    }
}