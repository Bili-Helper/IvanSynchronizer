package Client;

import Hooker.KeyboardEventHooker;
import Hooker.RawMouseInputJInput;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.client.consumer.rebalance.AllocateMessageQueueAveragely;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

/**
 * 主控机
 */
public class DNFMaster {
    public static boolean enableRun = false;
    private static RawMouseInputJInput rawMouseInputJInput; // 添加类成员变量以便在回调中访问

    public static void main(String[] args) throws MQClientException {
        Logger logger = LoggerFactory.getLogger(DNFMaster.class);

        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream("config.properties"));
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("配置文件错误");
            return;
        }
        String nameServerAddr = properties.getProperty("NameServerAddr");
        if(nameServerAddr == null){
            logger.error("配置文件错误");
            return;
        }

        String mouseTopicName = "MouseActions";
        String keyboardTopicName = "KeyboardActions";
        String controlTopicName = "ControlMessage";

        // 初始化鼠标生产者
        DefaultMQProducer mouseProducer = new DefaultMQProducer("MasterMouse");
        mouseProducer.setNamesrvAddr(nameServerAddr);
        try {
            mouseProducer.start();
            logger.info("鼠标生产者已启动");
        } catch (MQClientException e) {
            logger.error("鼠标生产者启动失败", e);
            return;
        }
        // 使用 RawMouseInputJInput 替代 MouseEventHooker
        rawMouseInputJInput = new RawMouseInputJInput(mouseProducer, mouseTopicName);
        Thread rawMouseThread = new Thread(rawMouseInputJInput);
        rawMouseThread.start();
        logger.info("RawMouseInputJInput 开始监听原始鼠标输入");

        DefaultMQProducer keyboardProducer = new DefaultMQProducer("MasterKeyboard");
        keyboardProducer.setNamesrvAddr(nameServerAddr);
        keyboardProducer.start();
        KeyboardEventHooker keyboardEventHooker = new KeyboardEventHooker(keyboardProducer, keyboardTopicName);
        Thread keyboardHookThread = new Thread(keyboardEventHooker);
        keyboardHookThread.start();

        // 监听开启、关闭同步
        DefaultMQPushConsumer mouseConsumer = new DefaultMQPushConsumer("MasterControl");
        mouseConsumer.setNamesrvAddr(nameServerAddr);
        try {
            mouseConsumer.subscribe(controlTopicName, "*");
        } catch (MQClientException e) {
            e.printStackTrace();
            return;
        }
        mouseConsumer.setAllocateMessageQueueStrategy(new AllocateMessageQueueAveragely());

        // 修改回调逻辑，移除 mouseEventHooker.setGameMode 调用
        mouseConsumer.registerMessageListener(new MessageListenerOrderly() {
            @Override
            public ConsumeOrderlyStatus consumeMessage(List<MessageExt> msgs, ConsumeOrderlyContext context) {
                for (MessageExt msg : msgs) {
                    String tags = msg.getTags();
                    if(tags.equals("enable")){
                        enableRun = true;
                        // 注意：RawMouseInputJInput 不需要设置游戏模式，因为它总是捕获原始输入
                        logger.info("开启游戏模式同步");
                    } else {
                        enableRun = false;
                        logger.info("关闭同步");
                    }
                }
                return ConsumeOrderlyStatus.SUCCESS;
            }
        });

        // 启动Consumer
        try {
            mouseConsumer.start();
            logger.info("Control Consumer Started");
        } catch (MQClientException e) {
            e.printStackTrace();
        }

        // 阻塞主线程
        try {
            rawMouseThread.join();
        } catch (InterruptedException e) {
            logger.error("主线程被中断", e);
        }

        // 添加钩子以确保程序退出时关闭资源
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (rawMouseInputJInput != null) {
                rawMouseInputJInput.stop();
                logger.info("RawMouseInputJInput 已停止");
            }

            if (mouseProducer != null) {
                mouseProducer.shutdown();
            }

            if (keyboardProducer != null) {
                keyboardProducer.shutdown();
            }

            if (mouseConsumer != null) {
                mouseConsumer.shutdown();
            }

            logger.info("所有资源已关闭");
        }));
    }

}