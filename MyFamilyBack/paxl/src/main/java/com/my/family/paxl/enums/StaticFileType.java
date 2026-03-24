package com.my.family.paxl.enums;

/**
 * 静态协议文件类型枚举
 * 对应 resources/stastic 目录下的 .txt 文件名
 *
 * @author ai
 * @date 2026/03/24
 */
public enum StaticFileType {

    /**
     * 用户协议
     */
    USER_PROTOCOL("USER_PROTOCOL.txt", "用户协议"),

    /**
     * 隐私声明
     */
    PRIVACY_STATEMENT("PRIVACY_STATEMENT.txt", "隐私声明");

    private final String fileName;
    private final String displayName;

    StaticFileType(String fileName, String displayName) {
        this.fileName = fileName;
        this.displayName = displayName;
    }

    public String getFileName() {
        return fileName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
