package Entity;
//消息实体对象
public class MouseMessage extends EventMessage{
    private Integer x; //cursor的屏幕像素空间xy坐标
    private Integer y;
    private Integer delta; //如果是滚轮 则是滚动数值
    private Integer deltaX; // 相对X移动量
    private Integer deltaY; // 相对Y移动量
    private Integer wheelDelta; // 滚轮移动量
    private Boolean isRelativeMode;// 是否为相对模式（游戏模式）


    public MouseMessage() {
        super();
        this.isRelativeMode = false;
    }

    public Integer getX() {
        return x;
    }

    public void setX(Integer x) {
        this.x = x;
    }

    public Integer getY() {
        return y;
    }

    public void setY(Integer y) {
        this.y = y;
    }

    public Integer getDelta() {
        return delta;
    }

    public void setDelta(Integer delta) {
        this.delta = delta;
    }

    // 为新字段添加getter和setter
    public Integer getDeltaX() {
        return deltaX;
    }

    public void setDeltaX(Integer deltaX) {
        this.deltaX = deltaX;
    }

    public Integer getDeltaY() {
        return deltaY;
    }

    public void setDeltaY(Integer deltaY) {
        this.deltaY = deltaY;
    }

    public Integer getWheelDelta() {
        return wheelDelta;
    }

    public void setDeltaWheel(Integer wheelDelta) {
        this.wheelDelta = wheelDelta;
    }

    public Boolean getIsRelativeMode() {
        return isRelativeMode;
    }

    public void setIsRelativeMode(Boolean relativeMode) {
        isRelativeMode = relativeMode;
    }

    public void setTimeStamp(long l) {
        
    }

    public void setCode(int mouseeventfMove) {
    }

    public void setXAxis(int value) {
    }

    public void setYAxis(int i) {
    }
}
