package Hooker;

import com.sun.jna.*;
import com.sun.jna.platform.win32.*;
import com.sun.jna.ptr.IntByReference;
import Entity.MouseMessage;
import EventCodes.MouseEventCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RawMouseInput {
    private static final Logger logger = LoggerFactory.getLogger(RawMouseInput.class);
    private static final User32 user32 = User32.INSTANCE;

    // 注册原始输入设备
    public void registerRawInput(WinDef.HWND hWnd) {
        RawInput.RAWINPUTDEVICE[] rid = new RawInput.RAWINPUTDEVICE[1];
        rid[0] = new RawInput.RAWINPUTDEVICE();
        rid[0].usUsagePage = (short) 0x01;      // HID_USAGE_PAGE_GENERIC
        rid[0].usUsage = (short) 0x02;          // HID_USAGE_GENERIC_MOUSE
        rid[0].dwFlags = new WinDef.DWORD(User32.RIDEV_INPUTSINK); // 接收后台输入
        rid[0].hwndTarget = hWnd;

        if (!user32.RegisterRawInputDevices(rid[0].getPointer(), 1, rid[0].size())) {
            logger.error("注册原始输入设备失败: {}", Native.getLastError());
        }
    }

    // 处理WM_INPUT消息
    public void handleRawInput(WinUser.MSG msg) {
        if (msg.message != WinUser.WM_INPUT) return;

        // 获取原始输入数据
        IntByReference pcbSize = new IntByReference();
        user32.GetRawInputData(msg.wParam, User32.RID_INPUT, null, pcbSize, WinDef.DWORD.SIZE);

        Pointer buffer = new Memory(pcbSize.getValue());
        user32.GetRawInputData(msg.wParam, User32.RID_INPUT, buffer, pcbSize, WinDef.DWORD.SIZE);

        // 解析RAWINPUT结构体
        RawInput.RAWINPUT rawInput = new RawInput.RAWINPUT(buffer);
        if (rawInput.header.dwType.intValue() == User32.RIM_TYPEMOUSE) {
            processRawMouseData(rawInput.mouse);
        }
    }

    // 处理原始鼠标数据
    private void processRawMouseData(RawInput.RAWMOUSE mouse) {
        // 检查是否为相对移动模式
        if ((mouse.usFlags & User32.MOUSE_MOVE_RELATIVE) != 0) {
            int dx = mouse.lLastX.intValue();
            int dy = mouse.lLastY.intValue();

            // 构建并发送消息
            MouseMessage msg = new MouseMessage();
            msg.setEventCode(MouseEventCodes.MOUSEEVENTF_MOVE);
            msg.setDeltaX(dx);
            msg.setDeltaY(dy);
            msg.setIsRelativeMode(true);

            sendMouseEvent(msg);
        }
    }

    // 发送鼠标事件到系统
    private void sendMouseEvent(MouseMessage msg) {
        WinUser.INPUT input = new WinUser.INPUT();
        input.type = new WinDef.DWORD(WinUser.INPUT.INPUT_MOUSE);
        input.input.setType("mi");

        WinUser.MOUSEINPUT mi = input.input.mi;
        mi.dx = new WinDef.LONG(msg.getDeltaX());
        mi.dy = new WinDef.LONG(msg.getDeltaY());
        mi.dwFlags = new WinDef.DWORD(msg.getEventCode());

        // 发送输入
        user32.SendInput(new WinDef.DWORD(1), new WinUser.INPUT[]{input}, input.size());
    }

    // 创建隐藏窗口（可选）
    public static WinDef.HWND createHiddenWindow() {
        WinDef.HWND hWnd = user32.CreateWindowEx(
                0, "STATIC", "RawInputWindow",
                WinUser.WS_POPUP, 0, 0, 0, 0,
                null, null, null, null
        );
        user32.ShowWindow(hWnd, User32.SW_HIDE);
        return hWnd;
    }

    // 示例用法
    public static void main(String[] args) {
        RawMouseInput rawMouse = new RawMouseInput();
        WinDef.HWND hWnd = createHiddenWindow();
        rawMouse.registerRawInput(hWnd);

        WinUser.MSG msg = new WinUser.MSG();
        while (user32.GetMessage(msg, null, 0, 0) != 0) {
            rawMouse.handleRawInput(msg);
            user32.TranslateMessage(msg);
            user32.DispatchMessage(msg);
        }
    }
}