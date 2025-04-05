package com.example.voca.ui.home;

public class FunctionItem {
    public static final int TYPE_FRAGMENT = 0; // Điều hướng đến Fragment
    public static final int TYPE_ACTIVITY = 1; // Mở Activity

    private String functionName;
    private int backgroundImageResId; // Resource ID của hình nền
    private int icon;
    private int type; // Loại điều hướng
    private int destinationId; // ID của Fragment đích (dùng cho Navigation)
    private Class<?> activityClass; // Class của Activity (dùng cho Intent)

    // Constructor cho điều hướng đến Fragment
    public FunctionItem(String functionName, int icon, int backgroundImageResId, int destinationId) {
        this.functionName = functionName;
        this.icon = icon;
        this.backgroundImageResId = backgroundImageResId;
        this.type = TYPE_FRAGMENT;
        this.destinationId = destinationId;
        this.activityClass = null;
    }

    // Constructor cho mở Activity
    public FunctionItem(String functionName, int icon, int backgroundImageResId, Class<?> activityClass) {
        this.functionName = functionName;
        this.icon = icon;
        this.backgroundImageResId = backgroundImageResId;
        this.type = TYPE_ACTIVITY;
        this.destinationId = -1;
        this.activityClass = activityClass;
    }

    public String getFunctionName() {
        return functionName;
    }

    public int getBackgroundImageResId() {
        return backgroundImageResId;
    }

    public int getType() {
        return type;
    }

    public int getDestinationId() {
        return destinationId;
    }

    public int getIcon() {
        return icon;
    }

    public Class<?> getActivityClass() {
        return activityClass;
    }
}