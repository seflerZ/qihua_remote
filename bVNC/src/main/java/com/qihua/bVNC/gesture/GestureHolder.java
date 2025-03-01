package com.qihua.bVNC.gesture;

import android.gesture.Gesture;

import com.google.android.gms.common.util.CollectionUtils;

import java.util.List;
import java.util.Set;

public class GestureHolder {
    private Gesture gesture;
    private String name;
    private List<String> keys;

    public GestureHolder(Gesture gesture, String name) {
        this.gesture = gesture;
        this.name = name;
    }

    public Gesture getGesture() {
        return gesture;
    }

    public String getName() {
        return name;
    }

    public List<String> getKeys() {
        return keys;
    }

    public String getJoinedKeys() {
        return String.join("+", keys);
    }

    public void setKeys(List<String> keys) {
        this.keys = keys;
    }
}
