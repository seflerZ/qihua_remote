package com.qihua.bVNC.gesture;

import android.content.Context;

import androidx.annotation.Nullable;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.undatech.opaque.util.FileUtils;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GestureActionLibrary {
    private String connId;
    private Map<String, List<String>> gestureKeysMap;
    private File actionFile;

    public GestureActionLibrary(String connId) {
        this.connId = connId;
        this.gestureKeysMap = new HashMap<>();
    }

    public void addAction(String gestureName, List<String> keys) {
        gestureKeysMap.put(gestureName, keys);
    }

    public @Nullable List<String> getAction(String gestureName) {
        return gestureKeysMap.get(gestureName);
    }

    public boolean load(Context context) {
        // load actions
        actionFile = new File(context.getDir("actions", Context.MODE_PRIVATE)
                , connId + "_actions.dat");

        String json = FileUtils.readFileToString(actionFile);
        if (json.isEmpty()) {
            return false;
        }

        gestureKeysMap = JSON.parseObject(json, new TypeReference<Map<String, List<String>>>() {}.getType());

        return true;
    }

    public boolean save() {
        if (actionFile == null) {
            return false;
        }

        try {
            FileUtils.writeStringToFile(JSON.toJSONString(gestureKeysMap), actionFile);
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    public String getConnId() {
        return connId;
    }
}
