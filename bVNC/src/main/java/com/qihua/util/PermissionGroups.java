package com.qihua.util;

public enum PermissionGroups {
    RECORD_AUDIO("record_audio"),
    RECORD_AND_MODIFY_AUDIO("record_and_modify_audio"),
    EXTERNAL_STORAGE_MANAGEMENT("external_storage_management");

    private final String text;

    PermissionGroups(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
