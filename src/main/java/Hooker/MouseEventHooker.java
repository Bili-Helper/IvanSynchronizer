package Hooker;

import Client.DNFMaster;
import Entity.MouseMessage;
import EventCodes.MouseEventCodes;
import com.sun.jna.platform.win32.*;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MouseEventHooker extends EventHooker implements Runnable{
    //我不明白为什么JNA没有定义鼠标输入相关的WinMessage，这些是我直接从MSDN上查的win32API里的定义：
    //https://learn.microsoft.com/en-us/windows/win32/inputdev/mouse-input-notifications
    private final static int WM_MOUSEMOVE =0x0200;
    private final static int WM_LBUTTONDOWN=0x0201;
    private final static int WM_LBUTTONUP=0x0202;
    private final static int WM_RBUTTONDOWN=0x0204;
    private final static int WM_RBUTTONUP=0x0205;
    private final static int WM_MOUSEWHEEL=0x020A;

    //上一次的鼠标位置
    private int lastMouseX = -1;
    private int lastMouseY = -1;
    //屏幕中心坐标
    private boolean gameMode = false;
    private int screenCenterX = -1;
    private int screenCenterY = -1;
    // 添加变量记录上一次的移动量
    private int previousDx = 0;
    private int previousDy = 0;
    private boolean isFirstMove = true;


    // 添加设置游戏模式的方法
    public void setGameMode(boolean enabled) {
        this.gameMode = enabled;
        if (enabled) {
            // 获取屏幕尺寸，计算中心点
            WinDef.RECT rect = new WinDef.RECT();
            User32.INSTANCE.GetWindowRect(User32.INSTANCE.GetDesktopWindow(), rect);
            screenCenterX = (rect.right - rect.left) / 2;
            screenCenterY = (rect.bottom - rect.top) / 2;
        }else {
            // 禁用游戏模式
            screenCenterX = -1;
            screenCenterY = -1;
        }

    }

    public MouseEventHooker(DefaultMQProducer producer, String topicName){
        super(producer,topicName);
    }

    @Override
    public void run() {
        Logger logger = LoggerFactory.getLogger(this.getClass());
        logger.info("Mouse Event Hooker listening");
        //鼠标Hook
        WinUser.HOOKPROC mouseHookProc = new WinUser.LowLevelMouseProc(){
            //编写回调函数，完成监听和事件packet发送
            //这是操作系统级别的中断，会直接打断/唤醒当前Hooker线程去执行回调函数
            @Override
            public WinDef.LRESULT callback(int nCode, WinDef.WPARAM wparam, WinUser.MSLLHOOKSTRUCT lparam) {
                if(DNFMaster.enableRun==false || nCode<0){
                    return user32.CallNextHookEx(null,nCode,wparam, new WinDef.LPARAM(lparam.getPointer().getLong(0)));
                }
                int WMSG = wparam.intValue();
                MouseMessage mouseMessage = new MouseMessage();
                String tag="";
                switch (WMSG){
                    case WM_LBUTTONDOWN:{
                        //System.out.println("左键按下");
                        mouseMessage.setEventCode(MouseEventCodes.MOUSEEVENTF_LEFTDOWN);
                        mouseMessage.setX(lparam.pt.x);
                        mouseMessage.setY(lparam.pt.y);
                        tag="LeftButtonDown";
                        logger.debug("{} x = {}; y = {}",tag,mouseMessage.getX(),mouseMessage.getY());
                        break;
                    }
                    case WM_LBUTTONUP:{
                        // System.out.println("左键抬起");
                        mouseMessage.setEventCode(MouseEventCodes.MOUSEEVENTF_LEFTUP);
                        mouseMessage.setX(lparam.pt.x);
                        mouseMessage.setY(lparam.pt.y);
                        tag="LeftButtonUp";
                        logger.debug("{} x = {}; y = {}",tag,mouseMessage.getX(),mouseMessage.getY());
                        break;
                    }
                    case WM_RBUTTONDOWN:{
                        //System.out.println("右键按下");
                        mouseMessage.setEventCode(MouseEventCodes.MOUSEEVENTF_RIGHTDOWN);
                        mouseMessage.setX(lparam.pt.x);
                        mouseMessage.setY(lparam.pt.y);
                        tag="RightButtonDown";
                        logger.debug("{} x = {}; y = {}",tag,mouseMessage.getX(),mouseMessage.getY());
                        break;
                    }
                    case WM_RBUTTONUP:{
                        //System.out.println("右键抬起");
                        mouseMessage.setEventCode(MouseEventCodes.MOUSEEVENTF_RIGHTUP);
                        mouseMessage.setX(lparam.pt.x);
                        mouseMessage.setY(lparam.pt.y);
                        tag="RightButtonUp";
                        logger.debug("{} x = {}; y = {}",tag,mouseMessage.getX(),mouseMessage.getY());
                        break;
                    }
                    //鼠标移动事件可以忽略，减少峰值流量

                    case WM_MOUSEMOVE:{
                        // System.out.println("鼠标移动"+ "x="+lparam.pt.x+" y="+lparam.pt.y);
                        mouseMessage.setEventCode(MouseEventCodes.MOUSEEVENTF_MOVE);
                        mouseMessage.setX(lparam.pt.x);
                        mouseMessage.setY(lparam.pt.y);
                        tag="MouseMove";

                        // 发送相对移动量
                        if (lastMouseX != -1 && lastMouseY != -1) {
                            int dx = lparam.pt.x - lastMouseX;
                            int dy = lparam.pt.y - lastMouseY;

                            // 游戏模式下检测连续相反方向的移动（游戏补偿机制）
                            if (gameMode && !isFirstMove) {
                                // 判断当前移动是否与上一次移动是相反的（抵消操作）
                                boolean isCompensationMove = (dx != 0 && dx == -previousDx) ||  // x方向互为相反数且不为0
                                        (dy != 0 && dy == -previousDy) || (Math.abs(dx) > 0 && Math.abs(previousDx) > 0 && Math.abs(dx + previousDx) <= 1) ||  // x方向近似互为相反数
                                        (Math.abs(dy) > 0 && Math.abs(previousDy) > 0 && Math.abs(dy + previousDy) <= 1);
                                if (isCompensationMove) {
                                    logger.debug("检测到游戏补偿操作，忽略此次移动");

                                    // 忽略这次移动，不更新鼠标相对移动量
                                    mouseMessage.setDeltaX(0);
                                    mouseMessage.setDeltaY(0);

                                    // 重置上一次的移动记录，避免连续多次补偿的情况
                                    previousDx = 0;
                                    previousDy = 0;
                                } else {
                                    // 正常的移动，记录并发送
                                    logger.debug("{} x = {}; y = {} dx = {}; dy = {}", tag, mouseMessage.getX(), mouseMessage.getY(), dx, dy);
                                    mouseMessage.setDeltaX(dx);
                                    mouseMessage.setDeltaY(dy);

                                    // 记录本次移动量以供下次比较
                                    previousDx = dx;
                                    previousDy = dy;
                                }
                            } else {
                                // 第一次移动或非游戏模式
                                logger.debug("{} x = {}; y = {} dx = {}; dy = {}", tag, mouseMessage.getX(), mouseMessage.getY(), dx, dy);
                                mouseMessage.setDeltaX(dx);
                                mouseMessage.setDeltaY(dy);

                                // 记录本次移动量
                                previousDx = dx;
                                previousDy = dy;
                                isFirstMove = false;
                            }
                        }

                        lastMouseX = lparam.pt.x;
                        lastMouseY = lparam.pt.y;
                        break;
                    }

                    case WM_MOUSEWHEEL:{
                        //System.out.println("滚轮");
                        /*
                        If the message is WM_MOUSEWHEEL, the high-order word of this member is the wheel delta.
                        The low-order word is reserved. A positive value indicates that the wheel was rotated forward,
                        away from the user; a negative value indicates that the wheel was rotated backward, toward the user.
                         One wheel click is defined as WHEEL_DELTA, which is 120.
                         https://learn.microsoft.com/en-us/windows/win32/api/winuser/ns-winuser-msllhookstruct
                         */
                        //获取滚轮滚动值delta，一个整数，以120位单位进行滚动，正数表示向上滚，负数表示向下滚
                        int delta = lparam.mouseData >> 16;
                        // System.out.println(delta);
                        mouseMessage.setEventCode(MouseEventCodes.MOUSEEVENTF_WHEEL);
                        mouseMessage.setX(lparam.pt.x);
                        mouseMessage.setY(lparam.pt.y);
                        mouseMessage.setDelta(delta);
                        tag="WheelMove";
                        logger.debug("{} x = {}; y = {} delta={}",tag,mouseMessage.getX(),mouseMessage.getY(),delta);
                        break;
                    }
                    default:
                        tag="unknown mouse action";
                }
                if(tag.equals("unknown mouse action")){
                    logger.error("{} WM={}",tag,WMSG);
                    return user32.CallNextHookEx(null,nCode, wparam,new WinDef.LPARAM(lparam.getPointer().getLong(0)));
                }
                if( producer!=null)
                    sendRocketMQMessage(mouseMessage,tag);
                return user32.CallNextHookEx(null,nCode, wparam,new WinDef.LPARAM(lparam.getPointer().getLong(0)));
            }
        };
        //添加鼠标钩子，返回钩子句柄hhook
        WinUser.HHOOK hhook = user32.SetWindowsHookEx(User32.WH_MOUSE_LL, mouseHookProc, hmodule, 0);
        WinUser.MSG msg = new WinUser.MSG();
        int result=-1;
        //为当前线程开启windows消息循环,阻塞住当前线程并等待回调函数执行
        while ((result = user32.GetMessage(msg, null, 0, 0)) != 0) {
            if (result == -1) {
                System.err.println("error in get message");
                break;
            } else {
                System.out.println("got message");
                user32.TranslateMessage(msg);
                user32.DispatchMessage(msg);
            }
        }
        user32.UnhookWindowsHookEx(hhook);
    }


}
