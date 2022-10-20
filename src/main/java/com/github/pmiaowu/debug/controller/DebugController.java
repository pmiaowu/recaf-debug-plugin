package com.github.pmiaowu.debug.controller;

import com.github.pmiaowu.debug.bootstrap.ArthasClient;
import com.github.pmiaowu.debug.bootstrap.CustomHelpers;
import com.github.pmiaowu.debug.bootstrap.YamlReader;

import me.coley.recaf.control.Controller;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.util.struct.ListeningMap;
import me.coley.recaf.workspace.ClassResource;
import me.coley.recaf.workspace.Workspace;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

public class DebugController {
    private static YamlReader yamlReader = YamlReader.getInstance();

    public static void dumpClass(Controller controller, String randomSeed, String className) {
        System.out.println("准备开始dump类");

        // 连接Arthas
        ArthasClient client = new ArthasClient(yamlReader.getString("arthas.host"), yamlReader.getInteger("arthas.port"));
        if (!client.isConnected()) {
            System.out.println("连接arthas失败,请检查arthas是否启动");
            return;
        }

        // 执行dump命令,将该类保存到远端服务器中
        String command1 = "dump -l 1000 -d '" + yamlReader.getString("arthas.remotePath") + "' " + "'" + className + "'";
        String dumpResult = client.sendCommand(command1);
        System.out.println("远端服务器,执行dump命令: " + command1);

        // 获取该类在远端服务器的dump位置
        String detailRemoteClassPath = getDetailRemoteClassPath(dumpResult, className);
        if (detailRemoteClassPath == null) {
            client.disconnect();
            System.out.println("远端服务器,执行dump命令失败,请检查类名是否输入正确");
            return;
        }
        System.out.println("该类在远端服务器的dump位置: " + detailRemoteClassPath);

        // 获取该类的详情并进行base64编码
        String command2 = "base64 " + "'" + detailRemoteClassPath + "'";
        String base64EncodeResult = client.sendCommand(command2);
        base64EncodeResult = base64EncodeResult.replaceAll("\\s+|\r|\n|\t", "");
        System.out.println("远端服务器,执行base64编码命令: " + command2);

        if (!CustomHelpers.isBase64(base64EncodeResult)) {
            client.disconnect();
            System.out.println("远端服务器,返回的结果貌似不是base64编码: " + base64EncodeResult);
            return;
        }

        // 关闭连接
        client.disconnect();

        // 创建caches目录
        cacheDirectoryCreation(randomSeed, className);

        // 该类在本地的详情地址
        String detailLocalClassPath = CustomHelpers.getCachePath() + File.separator + randomSeed + File.separator + className.replace(".", File.separator) + ".class";
        System.out.println("该类在本地的详情地址: " + detailLocalClassPath);

        // 将该类写入到本地caches目录
        // 用于给插件读取,然后将类加载到工作空间
        try {
            Files.write(
                    Paths.get(detailLocalClassPath),
                    Base64.getDecoder().decode(base64EncodeResult)
            );
            System.out.println("将该类保存在本地成功");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 将类加载到工作空间
        ClassResource resource = null;
        try {
            resource = new ClassResource(Paths.get(detailLocalClassPath));
        } catch (IOException e) {
            e.printStackTrace();
        }

        controller.setWorkspace(new Workspace(resource));
        ((GuiController) controller).windows().getMainWindow().openClass(
                resource,
                className.replace(".", "/")
        );
    }

    public static void redefineClass(Controller controller, String randomSeed, String className) {
        String className2 = className.replace("/", ".");

        // 连接Arthas
        ArthasClient client = new ArthasClient(yamlReader.getString("arthas.host"), yamlReader.getInteger("arthas.port"));
        if (!client.isConnected()) {
            System.out.println("连接arthas失败,请检查arthas是否启动");
            return;
        }

        // 执行dump命令,将该类保存到远端服务器中
        String command1 = "dump -l 1000 -d '" + yamlReader.getString("arthas.remotePath") + "' " + "'" + className2 + "'";
        String dumpResult = client.sendCommand(command1);
        System.out.println("远端服务器,执行dump命令: " + command1);

        // 获取该类在远端服务器的dump位置
        String detailRemoteClassPath = getDetailRemoteClassPath(dumpResult, className2);
        if (detailRemoteClassPath == null) {
            client.disconnect();
            System.out.println("远端服务器,执行dump命令失败,请检查类名是否输入正确");
            return;
        }
        System.out.println("该类在远端服务器的dump位置: " + detailRemoteClassPath);

        // 获取该类的byte[]数据
        ListeningMap<String, byte[]> classes = controller.getWorkspace().getPrimary().getClasses();
        byte[] byteData = classes.get(className);

        // 创建caches目录
        cacheDirectoryCreation(randomSeed, className2);

        // 该类在本地的详情地址
        String detailLocalClassPath = CustomHelpers.getCachePath() + File.separator + randomSeed + File.separator + className2.replace(".", File.separator) + ".class";
        System.out.println("该类在本地的详情地址: " + detailLocalClassPath);

        // 修改该类在本地的详情
        try {
            Files.write(Paths.get(detailLocalClassPath), byteData);
            System.out.println("成功修改该类在本地的详情");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 远端服务器debug文件保存路径
        String detailRemoteDebugTxtPath = detailRemoteClassPath.replace(".class", ".debug.txt");
        String detailRemoteDebugClassPath = detailRemoteClassPath.replace(".class", ".debug.class");

        // 在远端服务器将本地修改的类base64编码保存成txt文件
        String command2 = "echo " + "'" + Base64.getEncoder().encodeToString(byteData) + "'" + " > " + "'" + detailRemoteDebugTxtPath + "'";
        client.sendCommand(command2);
        System.out.println("将该类base64编码保存成txt文件,上传到远端服务器");
        System.out.println("该类在远端服务器txt文件路径: " + detailRemoteDebugTxtPath);

        // 在远端服务器将本地上传的txt文件base64解码为class文件
        String command3 = "base64 -d " + "'" + detailRemoteDebugTxtPath + "'" + " --output " + "'" + detailRemoteDebugClassPath + "'";
        client.sendCommand(command3);
        System.out.println("将上传在远端服务器的txt文件解码保存为class文件");
        System.out.println("该类在远端服务器class文件路径: " + detailRemoteDebugClassPath);

        // 将该类更新到远端服务器,修改运行中的类字节码并让它实时生效
        String command4 = "retransform " + "'" + detailRemoteDebugClassPath + "'";
        String result = client.sendCommand(command4);
        System.out.println("远端服务器,执行retransform命令: " + command4);
        if (result.contains("retransform success")) {
            System.out.println("远端服务器,热更新成功");
        } else {
            System.out.println("远端服务器,热更新失败: " + result);
        }

        // 关闭连接
        client.disconnect();
    }

    /**
     * 从杂乱的数据中过滤出详细的远程class路径
     *
     * @param data      要进行过滤的数据
     * @param className 要进行匹配的类名
     * @return
     */
    private static String getDetailRemoteClassPath(String data, String className) {
        String detailClassPath = null;

        data = data.trim().replaceAll("\\s+", " ");

        if (!data.contains("LOCATION")) {
            return detailClassPath;
        }

        for (String str : data.split(" ")) {
            if ("".equals(str) || " ".equals(str)) {
                continue;
            }

            String remoteClassPath = null;
            if (str.startsWith("/")) {
                remoteClassPath = className.replace(".", "/");
            } else {
                remoteClassPath = className.replace(".", "\\");
            }

            if (str.contains(remoteClassPath)) {
                detailClassPath = str;
                break;
            }
        }

        return detailClassPath;
    }

    /**
     * 本地缓存目录创建
     *
     * @param className
     */
    private static void cacheDirectoryCreation(String randomSeed, String className) {
        File file;

        if (className.contains(".")) {
            String classDir = className.substring(0, className.lastIndexOf(".")).replace(".", File.separator);
            file = new File(CustomHelpers.getCachePath() + File.separator + randomSeed + File.separator + classDir);
        } else if (className.contains(File.separator)) {
            String classDir = className.substring(0, className.lastIndexOf(File.separator));
            file = new File(CustomHelpers.getCachePath() + File.separator + randomSeed + File.separator + classDir);
        } else {
            file = new File(CustomHelpers.getCachePath() + File.separator + randomSeed);
        }

        if (!file.exists()) {
            file.mkdirs();
        }
    }
}