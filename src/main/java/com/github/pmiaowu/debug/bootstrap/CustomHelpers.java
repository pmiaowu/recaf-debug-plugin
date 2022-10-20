package com.github.pmiaowu.debug.bootstrap;

import me.coley.recaf.Recaf;

import java.io.File;
import java.util.Random;
import java.util.regex.Pattern;

public class CustomHelpers {
    /**
     * 判断是否运行在jar环境
     *
     * @return
     */
    public static boolean isStartupFromJar() {
        String protocol = CustomHelpers.class.getResource("CustomHelpers.class").getProtocol();
        boolean runningInJar = "jar".equals(protocol);
        return runningInJar;
    }

    /**
     * 获取插件安装路径
     *
     * @return
     */
    public static String getPluginPath() {
        return Recaf.getDirectory().toFile().getAbsolutePath() + File.separator + "plugins";
    }

    /**
     * 获取资源目录路径
     * 根据运行环境的不同,获取的资源目录也会有所不同
     *
     * @return
     */
    public static String getResourcePath() {
        if (!CustomHelpers.isStartupFromJar()) {
            String path = System.getProperty("user.dir") + File.separator;
            path = path + "src" + File.separator + "main" + File.separator + "resources";
            return path;
        }
        return getPluginPath() + File.separator + "debugPluginResources";
    }

    /**
     * 获取缓存路径
     * 保存着经过插件dump的class
     *
     * @return
     */
    public static String getCachePath() {
        return getResourcePath() + File.separator + "caches";
    }

    /**
     * 判断是否base64编码
     *
     * @param str
     * @return 返回true:是base64编码,返回false:不是base64编码
     */
    public static boolean isBase64(String str) {
        String base64Pattern = "^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)$";
        return Pattern.matches(base64Pattern, str);
    }

    /**
     * 随机取若干个字符
     *
     * @param number
     * @return String
     */
    public static String randomStr(int number) {
        StringBuffer s = new StringBuffer();
        char[] stringArray = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i',
                'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u',
                'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6',
                '7', '8', '9'};
        Random random = new Random();
        for (int i = 0; i < number; i++) {
            char num = stringArray[random.nextInt(stringArray.length)];
            s.append(num);
        }
        return s.toString();
    }
}