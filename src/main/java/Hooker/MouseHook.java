package Hooker;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.LRESULT;
import com.sun.jna.platform.win32.WinDef.WPARAM;
import com.sun.jna.platform.win32.WinDef.LPARAM;
import com.sun.jna.platform.win32.WinUser.HOOKPROC;
import com.sun.jna.platform.win32.WinUser.HHOOK;
import com.sun.jna.platform.win32.WinUser.MSG;
import com.sun.jna.platform.win32.WinUser;

import java.util.Arrays;
import java.util.List;

public class MouseHook {
    private static User32 user32 = User32.INSTANCE;
    private static HHOOK mouseHook;
    private static HOOKPROC mouseProc;

    // 定义MSLLHOOKSTRUCT结构体
    public static class MSLLHOOKSTRUCT extends Structure {
        public static class ByReference extends MSLLHOOKSTRUCT implements Structure.ByReference {}

        public POINT pt;
        public int mouseData;
        public int flags;
        public int time;
        public Pointer dwExtraInfo;

        public MSLLHOOKSTRUCT() {
            super();
            pt = new POINT();
        }

        public MSLLHOOKSTRUCT(Pointer p) {
            super(p);
            pt = new POINT();
            read();
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("pt", "mouseData", "flags", "time", "dwExtraInfo");
        }

        public static class POINT extends Structure {
            public int x;
            public int y;

            @Override
            protected List<String> getFieldOrder() {
                return Arrays.asList("x", "y");
            }
        }
    }

    public static void main(String[] args) {
        // 设置鼠标钩子
        mouseProc = new HOOKPROC() {
            public LRESULT callback(int nCode, WPARAM wParam, LPARAM lParam) {
                if (nCode >= 0) {
                    // 正确地处理MSLLHOOKSTRUCT结构体 - 使用结构体的构造函数
                    MSLLHOOKSTRUCT mouseInfo = new MSLLHOOKSTRUCT(lParam.toPointer());

                    System.out.println("鼠标移动: X=" + mouseInfo.pt.x +
                            ", Y=" + mouseInfo.pt.y +
                            ", data=" + mouseInfo.mouseData);
                }
                return user32.CallNextHookEx(mouseHook, nCode, wParam, lParam);
            }
        };

        mouseHook = user32.SetWindowsHookEx(WinUser.WH_MOUSE_LL, mouseProc, null, 0);

        // 消息循环，保持程序运行
        MSG msg = new MSG();
        while (user32.GetMessage(msg, null, 0, 0) > 0) {
            user32.TranslateMessage(msg);
            user32.DispatchMessage(msg);
        }

        // 卸载钩子
        user32.UnhookWindowsHookEx(mouseHook);
    }
}