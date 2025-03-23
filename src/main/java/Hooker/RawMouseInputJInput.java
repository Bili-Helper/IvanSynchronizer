package Hooker;

import net.java.games.input.*;
import net.java.games.input.DirectAndRawInputEnvironmentPlugin;
import Entity.MouseMessage;
import EventCodes.MouseEventCodes;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import com.alibaba.fastjson.JSON;

public class RawMouseInputJInput implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(RawMouseInputJInput.class);

    // 添加静态代码块用于加载本地库
    static {
        try {
            // 尝试从构建目录加载
            String nativeLibPath = System.getProperty("user.dir") + File.separator + "target" + File.separator + "native-libs";
            System.setProperty("net.java.games.input.librarypath", nativeLibPath);
            logger.info("设置JInput本地库路径为：" + nativeLibPath);
        } catch (Exception e) {
            logger.error("设置本地库路径失败", e);
        }
    }


    private final DefaultMQProducer producer;
    private final String topicName;
    private volatile boolean running = true;

    public RawMouseInputJInput(DefaultMQProducer producer, String topicName) {
        this.producer = producer;
        this.topicName = topicName;
    }

    @Override
    public void run() {
        logger.info("Starting Raw Mouse Input Listener");

        Controller[] controllers = null;
        try {
            // 使用DirectAndRaw环境获取控制器
            DirectAndRawInputEnvironmentPlugin directEnv = new DirectAndRawInputEnvironmentPlugin();
            controllers = directEnv.getControllers();
        } catch (Exception e) {
            logger.error("获取控制器失败", e);
            return;
        }

        // 查找鼠标设备
        Controller mouseController = null;
        for (Controller controller : controllers) {
            if (controller.getType() == Controller.Type.MOUSE) {
                mouseController = controller;
                logger.info("找到鼠标设备: " + controller.getName());
                break;
            }
        }

        if (mouseController == null) {
            logger.error("未找到鼠标设备");
            return;
        }

        // 持续轮询鼠标事件
        Event event = new Event();
        while (running) {
            // 确保控制器轮询正常工作
            mouseController.poll();

            // 获取队列中的所有事件
            EventQueue queue = mouseController.getEventQueue();
            while (queue.getNextEvent(event)) {
                logger.debug("收到事件: {}", event.toString());
                processEvent(event);
            }

            // 添加小延迟避免CPU过载
            try {
                Thread.sleep(0);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }


    private void processEvent(Event event) {
        Component component = event.getComponent();
        float value = event.getValue();

        MouseMessage mouseMessage = new MouseMessage();
        String tag = "";

        // 只保留相对模式处理逻辑
        if (component.isRelative()) {
            // 处理X/Y轴移动
            if (component.getIdentifier() == Component.Identifier.Axis.X) {
                int deltaX = (int) value; // 直接转换，避免过滤微小移动
                mouseMessage.setDeltaX(deltaX);
                mouseMessage.setEventCode(MouseEventCodes.MOUSEEVENTF_MOVE);
                tag = "RawMouseMove";
                logger.debug("X轴移动: {}", deltaX);
            }
            else if (component.getIdentifier() == Component.Identifier.Axis.Y) {
                int deltaY = (int) value;
                mouseMessage.setDeltaY(deltaY);
                mouseMessage.setEventCode(MouseEventCodes.MOUSEEVENTF_MOVE);
                tag = "RawMouseMove";
                logger.debug("Y轴移动: {}", deltaY);
            }
            // 处理滚轮
            else if (component.getIdentifier() == Component.Identifier.Axis.Z) {
                int wheelDelta = (int) value;
                mouseMessage.setDeltaWheel(wheelDelta);
                mouseMessage.setEventCode(MouseEventCodes.MOUSEEVENTF_WHEEL);
                tag = "MouseWheel";
                logger.debug("滚轮: {}", wheelDelta);
            }
        }
        // 处理按钮事件
        else if (component.getIdentifier() instanceof Component.Identifier.Button) {
            if (component.getIdentifier() == Component.Identifier.Button.LEFT) {
                mouseMessage.setEventCode(value > 0.5f ? MouseEventCodes.MOUSEEVENTF_LEFTDOWN : MouseEventCodes.MOUSEEVENTF_LEFTUP);
                tag = value > 0.5f ? "MouseLeftDown" : "MouseLeftUp";
                logger.debug("鼠标左键: " + (value > 0.5f ? "按下" : "释放"));
            } else if (component.getIdentifier() == Component.Identifier.Button.RIGHT) {
                mouseMessage.setEventCode(value > 0.5f ? MouseEventCodes.MOUSEEVENTF_RIGHTDOWN : MouseEventCodes.MOUSEEVENTF_RIGHTUP);
                tag = value > 0.5f ? "MouseRightDown" : "MouseRightUp";
                logger.debug("鼠标右键: " + (value > 0.5f ? "按下" : "释放"));
            } else if (component.getIdentifier() == Component.Identifier.Button.MIDDLE) {
                mouseMessage.setEventCode(value > 0.5f ? MouseEventCodes.MOUSEEVENTF_MIDDLEDOWN : MouseEventCodes.MOUSEEVENTF_MIDDLEUP);
                tag = value > 0.5f ? "MouseMiddleDown" : "MouseMiddleUp";
                logger.debug("鼠标中键: " + (value > 0.5f ? "按下" : "释放"));
            }
        }

        // 发送消息
        if (!tag.isEmpty() && producer != null && mouseMessage.getEventCode() != 0) {
            sendRocketMQMessage(mouseMessage, tag);
        }
    }

    private void sendRocketMQMessage(MouseMessage message, String tag) {
        try {
            String jsonMessage = JSON.toJSONString(message);
            Message mqMessage = new Message(topicName, tag, jsonMessage.getBytes(StandardCharsets.UTF_8));

            // 异步发送消息，避免阻塞鼠标事件处理线程
            producer.sendOneway(mqMessage);
            logger.debug("发送消息成功: tag=" + tag + ", content=" + jsonMessage);
        } catch (Exception e) {
            logger.error("发送消息失败: " + e.getMessage());
        }
    }

    public void stop() {
        running = false;
    }
}