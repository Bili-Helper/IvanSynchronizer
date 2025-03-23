package Hooker;

import com.sun.jna.*;
import com.sun.jna.platform.win32.*;
import com.sun.jna.platform.win32.WinDef.*;
import com.sun.jna.ptr.PointerByReference;

// 手动定义RAWINPUT相关结构体
public interface RawInput extends Library {
    // RAWINPUTHEADER结构体
    class RAWINPUTHEADER extends Structure {
        public static class ByReference extends RAWINPUTHEADER implements Structure.ByReference {}
        public DWORD dwType;   // 输入设备类型（RIM_TYPEMOUSE等）
        public DWORD dwSize;   // 数据大小
        public WinNT.HANDLE hDevice; // 设备句柄
        public WPARAM wParam;  // 输入来源

        @Override
        protected java.util.List<String> getFieldOrder() {
            return java.util.Arrays.asList("dwType", "dwSize", "hDevice", "wParam");
        }
    }

    // RAWMOUSE结构体
    class RAWMOUSE extends Structure {
        public static class ByReference extends RAWMOUSE implements Structure.ByReference {}
        public USHORT usFlags;      // 标志（如MOUSE_MOVE_RELATIVE）
        public USHORT usButtonFlags;// 按钮状态
        public USHORT usButtonData; // 滚轮数据
        public ULONG ulRawButtons;  // 原始按钮状态
        public LONG lLastX;         // X相对移动量
        public LONG lLastY;         // Y相对移动量
        public ULONG ulExtraInformation; // 扩展信息

        @Override
        protected java.util.List<String> getFieldOrder() {
            return java.util.Arrays.asList(
                    "usFlags", "usButtonFlags", "usButtonData",
                    "ulRawButtons", "lLastX", "lLastY", "ulExtraInformation"
            );
        }
    }

    // RAWINPUT联合体（仅处理鼠标部分）
    class RAWINPUT extends Structure {
        public RAWINPUTHEADER header;
        public RAWMOUSE mouse;

        public RAWINPUT() {}
        public RAWINPUT(Pointer p) {
            super(p);
            read();
        }

        @Override
        protected java.util.List<String> getFieldOrder() {
            return java.util.Arrays.asList("header", "mouse");
        }
    }

    // RAWINPUTDEVICE结构体
    class RAWINPUTDEVICE extends Structure {
        public static class ByReference extends RAWINPUTDEVICE implements Structure.ByReference {}
        public short usUsagePage; // 设备用途页
        public short usUsage;      // 设备用途（如鼠标）
        public DWORD dwFlags;      // 标志（如RIDEV_INPUTSINK）
        public WinDef.HWND hwndTarget; // 目标窗口句柄

        @Override
        protected java.util.List<String> getFieldOrder() {
            return java.util.Arrays.asList("usUsagePage", "usUsage", "dwFlags", "hwndTarget");
        }
    }
}