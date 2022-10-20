package com.github.pmiaowu.debug;

import com.github.pmiaowu.debug.bootstrap.CustomHelpers;
import com.github.pmiaowu.debug.controller.DebugController;

import javafx.scene.Parent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import me.coley.recaf.control.Controller;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.plugin.api.MenuProviderPlugin;
import me.coley.recaf.plugin.api.ContextMenuInjectorPlugin;
import me.coley.recaf.plugin.api.StartupPlugin;
import me.coley.recaf.ui.ContextBuilder;
import me.coley.recaf.ui.controls.ActionButton;
import me.coley.recaf.ui.controls.ActionMenuItem;

import org.plugface.core.annotations.Plugin;

import java.util.Iterator;
import java.util.Set;

@Plugin(name = "Debug")
public class Application implements StartupPlugin, MenuProviderPlugin, ContextMenuInjectorPlugin {
    private Controller controller;

    private String randomSeed;

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String getDescription() {
        String s = "";
        s = s + "这是一个Debug调试插件\r\n";
        s = s + "配合阿里巴巴开源的arthas\n";
        s = s + "可以修改运行中的类字节码并让它实时生效";
        return s;
    }

    /**
     * 插件启动时运行
     *
     * @param controller
     */
    @Override
    public void onStart(Controller controller) {
        this.controller = controller;

        // 创建随机数种子
        this.randomSeed = CustomHelpers.randomStr(15);
    }

    /**
     * 插件菜单创建子菜单
     *
     * @return
     */
    @Override
    public Menu createMenu() {
        Menu menu = new Menu(this.getName());

        // debug菜单创建
        ActionMenuItem debugMenu = new ActionMenuItem("调试", () -> {
            ((GuiController) this.controller).windows().window(this.getName(), createDebugPanel()).show();
        });

        // 添加菜单
        menu.getItems().add(debugMenu);

        return menu;
    }

    /**
     * 创建debug面板
     *
     * @return
     */
    private Parent createDebugPanel() {
        TextField textField = new TextField();
        GridPane grid = new GridPane();
        grid.addRow(0, new Label("调试的类名:"), textField);
        grid.addRow(1, new ActionButton("加载类", () -> {
            DebugController.dumpClass(this.controller, this.randomSeed, textField.getText());

            // 移除所有创建的窗口
            Set<Stage> windows = ((GuiController) this.controller).windows().getWindows();
            Iterator<Stage> iterator = windows.iterator();
            while (iterator.hasNext()) {
                Stage win = iterator.next();
                win.close();
                windows.remove(win);
            }
        }));
        return grid;
    }

    /**
     * 右键菜单-为每个class类都添加一个新按钮
     *
     * @param builder
     * @param menu
     * @param name
     */
    @Override
    public void forClass(ContextBuilder builder, ContextMenu menu, String name) {
        menu.getItems().add(new ActionMenuItem("热更新", () -> DebugController.redefineClass(this.controller, this.randomSeed, name)));
    }
}