package MessageConsumer;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.ptr.IntByReference;

public class GameModeDetector {
    private static final User32 user32 = User32.INSTANCE;
    
    // 上一个检测到的鼠标位置
    private static int lastMouseX = -1;
    private static int lastMouseY = -1;
    // 鼠标中心锁定计数器
    private static int centerLockCounter = 0;
    
    /**
     * 检测当前是否为游戏模式
     * 游戏模式特点：
     * 1. 全屏或边框窗口
     * 2. 鼠标频繁被重置到窗口中心
     * 3. 鼠标可能被隐藏
     */
    public static boolean isInGameMode() {
        // 检查前台窗口
        WinDef.HWND foregroundWindow = user32.GetForegroundWindow();
        if (foregroundWindow == null) {
            return false;
        }
        
        // 检查窗口样式 (游戏通常使用无边框窗口)
        int style = user32.GetWindowLong(foregroundWindow, User32.GWL_STYLE);
        boolean hasBorderStyle = (style & User32.WS_BORDER) != 0;
        
        // 检查窗口是否全屏
        boolean isFullScreen = isFullScreenWindow(foregroundWindow);
        
        // 检查鼠标位置是否被频繁重置到中心
        boolean isMouseCenterLocked = checkMouseCenterLock();
        
        // 如果满足以下条件之一，认为是游戏模式：
        // 1. 全屏无边框窗口
        // 2. 鼠标被锁定在中心位置
        return (isFullScreen && !hasBorderStyle) || isMouseCenterLocked;
    }
    
    /**
     * 检查窗口是否全屏
     */
    private static boolean isFullScreenWindow(WinDef.HWND hwnd) {
        WinDef.RECT windowRect = new WinDef.RECT();
        user32.GetWindowRect(hwnd, windowRect);
        
        int screenWidth = user32.GetSystemMetrics(User32.SM_CXSCREEN);
        int screenHeight = user32.GetSystemMetrics(User32.SM_CYSCREEN);
        
        int windowWidth = windowRect.right - windowRect.left;
        int windowHeight = windowRect.bottom - windowRect.top;
        
        // 如果窗口覆盖了整个屏幕或接近整个屏幕，认为是全屏
        return windowWidth >= screenWidth * 0.95 && windowHeight >= screenHeight * 0.95;
    }
    
    /**
     * 检查鼠标是否被锁定在窗口中心
     */
    private static boolean checkMouseCenterLock() {
        WinDef.POINT point = new WinDef.POINT();
        user32.GetCursorPos(point);
        
        int screenWidth = user32.GetSystemMetrics(User32.SM_CXSCREEN);
        int screenHeight = user32.GetSystemMetrics(User32.SM_CYSCREEN);
        
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        
        // 如果鼠标在屏幕中心附近
        boolean isNearCenter = Math.abs(point.x - centerX) < 5 && Math.abs(point.y - centerY) < 5;
        
        // 如果鼠标位置没有变化（可能被游戏锁定）
        boolean notMoved = (lastMouseX == point.x && lastMouseY == point.y);
        
        // 更新上一次位置
        lastMouseX = point.x;
        lastMouseY = point.y;
        
        // 如果鼠标在中心并且没有移动，增加计数器
        if (isNearCenter && notMoved) {
            centerLockCounter++;
        } else {
            // 否则减少计数器，但不小于0
            centerLockCounter = Math.max(0, centerLockCounter - 1);
        }
        
        // 如果连续多次检测到鼠标被锁定在中心，认为是游戏模式
        return centerLockCounter > 5;
    }
}