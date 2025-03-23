package Hooker;

import net.java.games.input.*;

import java.io.File;

public class MouseRelativeMovementTracker {

    public static void main(String[] args) {
        // 设置JInput库路径
        // 在代码开始处添加调试信息
        String nativeLibPath = System.getProperty("user.dir") + File.separator + "target" + File.separator + "native-libs";
        System.out.println("尝试从以下路径加载原生库: " + nativeLibPath);
        File dir = new File(nativeLibPath);
        if (dir.exists() && dir.isDirectory()) {
            System.out.println("目录存在，包含以下文件:");
            for (String file : dir.list()) {
                System.out.println(" - " + file);
            }
        } else {
            System.out.println("目录不存在或不是一个目录!");
        }
        System.setProperty("net.java.games.input.librarypath", nativeLibPath);


        Controller[] controllers = null;
        try {
            // 使用DirectAndRaw环境获取控制器
            DirectAndRawInputEnvironmentPlugin directEnv = new DirectAndRawInputEnvironmentPlugin();
            controllers = directEnv.getControllers();
        } catch (Exception e) {
            System.out.println("获取控制器失败");
            return;
        }

        // 打印所有控制器信息
        System.out.println("可用的控制器:");
        for (Controller controller : controllers) {
            System.out.println(" - " + controller.getName() + " (类型: " + controller.getType() + ")");
        }

        // 查找鼠标设备
        Controller mouseController = null;
        for (Controller controller : controllers) {
            if (controller.getType() == Controller.Type.MOUSE) {
                mouseController = controller;
                System.out.println("找到鼠标设备: " + controller.getName());
                break;
            }
        }
        // 如果找到鼠标控制器，打印其组件
        if (mouseController != null) {
            System.out.println("找到鼠标控制器: " + mouseController.getName());
            System.out.println("组件列表:");
            Component[] components = mouseController.getComponents();
            for (Component component : components) {
                System.out.println(" - " + component.getName() + " (标识符: " + component.getIdentifier() + ")");
            }
        } else {
            System.out.println("未找到鼠标设备");
            return;
        }

        // 修改代码为事件监听方式
        System.out.println("开始监听鼠标移动事件...");
        Event event = new Event();
        EventQueue queue = mouseController.getEventQueue();


        // 持续获取鼠标数据
        while (true) {
            mouseController.poll();
            Component[] components = mouseController.getComponents();

            // 修改为使用组件标识符而不是名称
            for (Component component : components) {
                if (component.getIdentifier() == Component.Identifier.Axis.X) {
                    float xMovement = component.getPollData();
                    // 移除0值检查以查看是否有任何值输出
                    System.out.println("X轴相对移动量: " + xMovement);
                }
                else if (component.getIdentifier() == Component.Identifier.Axis.Y) {
                    float yMovement = component.getPollData();
                    System.out.println("Y轴相对移动量: " + yMovement);
                }
            }


            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}