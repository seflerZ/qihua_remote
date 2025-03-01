package com.qihua.bVNC.gesture;

import android.content.Context;
import android.content.Intent;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.undatech.remoteClientUi.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class GestureEditorActivity extends AppCompatActivity implements GestureEditorActions {
    private GestureLibrary gestureLibrary;
    private GestureListAdapter gestureListAdapter;
    private Gesture gesture = null;
    private RecyclerView gestureList = null;
    private GestureActionLibrary actionLibrary = null;
    private AlertDialog alertDialog;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.configure_gesture);
        setContentView(R.layout.gesture_list);

        Intent intent = getIntent();
        String connId = intent.getStringExtra("connId");

        actionLibrary = new GestureActionLibrary(connId);

        File gestureFile = new File(getDir("gestures", Context.MODE_PRIVATE)
                , connId + "_gestures.dat");

        gestureLibrary = GestureLibraries.fromFile(gestureFile);
        gestureLibrary.load();

        gestureListAdapter = new GestureListAdapter(getGestureEntries());

        gestureList = findViewById(R.id.gestureList);
        gestureList.setAdapter(gestureListAdapter);
        gestureList.setLayoutManager(new LinearLayoutManager(this));
    }

    private List<GestureHolder> getGestureEntries()  {
        List<GestureHolder> gestureList = new ArrayList<>();

        actionLibrary.load(getApplicationContext());

        try {
            Set<String> gestureSet = gestureLibrary.getGestureEntries();
            for (String name : gestureSet) {
                List<Gesture> gestures = gestureLibrary.getGestures(name);
                for (Gesture gesture : gestures) {
                    GestureHolder gestureHolder = new GestureHolder(gesture, name);
                    List<String> keys = actionLibrary.getAction(name);

                    if (keys == null || keys.isEmpty()) {
                        continue;
                    }

                    gestureHolder.setKeys(keys);
                    gestureList.add(gestureHolder);
                }
            }
        } catch (Exception ignore) {}

        return gestureList;
    }

    @Override
    public void onDeleteClicked(GestureHolder gestureHolder, int pos) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.delete_gesture));
        builder.setPositiveButton(getString(R.string.delete), (dialog, which) -> {
            GestureListAdapter adapter = (GestureListAdapter) gestureList.getAdapter();
            adapter.removeGesture(pos);

            gestureLibrary.removeEntry(gestureHolder.getName());
            gestureLibrary.save();

            adapter.notifyDataSetChanged();
        });

        builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> {
            dialog.dismiss();
        });

        builder.show();
    }

    public void triggerAddGestureDialog(MenuItem item) {
        alertDialog = new AlertDialog.Builder(this)
                .setPositiveButton(getString(R.string.save_gesture), null)
                .setNegativeButton(getString(R.string.cancel), (d, which) -> d.dismiss())
                .setTitle(getString(R.string.create_new_gesture))
                .create();

        LayoutInflater inflater = LayoutInflater.from(this);

        View gestureView = inflater.inflate(R.layout.gesture_add_gesture, null);
        GestureOverlayView gestureOverlayView = gestureView.findViewById(R.id.popupGestureAdd);
        gestureOverlayView.setOrientation(GestureOverlayView.ORIENTATION_VERTICAL);
        gestureOverlayView.setGestureStrokeWidth(20f);

        EditText gestureNameView = gestureView.findViewById(R.id.popupGestureName);

        // 添加手势监听器
        gestureOverlayView.addOnGestureListener(gestureListener);

        // 手动绑定 PositiveButton 的点击事件
        alertDialog.setOnShowListener(d -> {
            Button positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String gestureName = gestureNameView.getText().toString();

                if (gesture == null) {
                    Toast.makeText(this, R.string.add_some_gesture, Toast.LENGTH_LONG).show();
                } else if (gestureName.isEmpty()) {
                    gestureNameView.setError(getString(R.string.gesture_name_required));
                    Toast.makeText(this, R.string.gesture_name_required, Toast.LENGTH_LONG).show();
                } else {
                    // 输入有效，关闭对话框
                    gestureLibrary.addGesture(gestureName, gesture);
                    GestureHolder gestureHolder = saveGestureAction(gestureName, alertDialog);
                    if (gestureHolder == null || !gestureLibrary.save()) {
                        Toast.makeText(this, R.string.save_fail, Toast.LENGTH_LONG).show();
                    } else {
                        GestureListAdapter adapter = (GestureListAdapter) gestureList.getAdapter();
                        adapter.addGesture(gestureHolder);

                        adapter.notifyDataSetChanged();
                        Toast.makeText(this, R.string.saved, Toast.LENGTH_LONG).show();

                        gesture = null;
                    }

                    alertDialog.dismiss();
                }
            });
        });

        EditText editText = gestureView.findViewById(R.id.gesture_keycode);
        editText.setOnKeyListener((v, keyCode, event) -> {
            // 检查是否是按键按下事件
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_DPAD_LEFT:
                        editText.append("\u2190"); // 插入左箭头（←）
                        return true; // 表示已经处理该事件
                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                        editText.append("\u2192"); // 插入右箭头（→）
                        return true; // 表示已经处理该事件
                    case KeyEvent.KEYCODE_DPAD_UP:
                        editText.append("\u2191"); // 插入上箭头（↑）
                        return true; // 表示已经处理该事件
                    case KeyEvent.KEYCODE_DPAD_DOWN:
                        editText.append("\u2193"); // 插入下箭头（↓）
                        return true; // 表示已经处理该事件
                    default:
                        // 对于其他按键，返回 false，让系统正常处理
                        return false;
                }
            }
            return false;
        });


        alertDialog.setView(gestureView);
        alertDialog.show();
    }

    private GestureOverlayView.OnGestureListener gestureListener = new GestureOverlayView.OnGestureListener() {
        @Override
        public void onGestureStarted(GestureOverlayView overlay, MotionEvent event) {

        }

        @Override
        public void onGesture(GestureOverlayView overlay, MotionEvent event) {
            gesture = overlay.getGesture();
        }

        @Override
        public void onGestureEnded(GestureOverlayView overlay, MotionEvent event) {

        }

        @Override
        public void onGestureCancelled(GestureOverlayView overlay, MotionEvent event) {

        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gesture_menu, menu);
        return true;
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();
    }

    private GestureHolder saveGestureAction(String gestureName, AlertDialog dialog) {
        EditText gestureKeyEdit = dialog.findViewById(R.id.gesture_keycode);
        String key = gestureKeyEdit.getText().toString();

        LinearLayout layout = dialog.findViewById(R.id.meta_key_group);
        List<String> keys = new ArrayList<>();

        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            if (child instanceof Button) {
                Button btn = (Button) child;
                if (btn.isSelected()) {
                    keys.add(btn.getText().toString());
                }
            }
        }

        if (!key.isEmpty()) {
            keys.add(key.toLowerCase());
        }

        actionLibrary.addAction(gestureName, keys);

        if (!actionLibrary.save()) {
            return null;
        }

        GestureHolder gestureHolder = new GestureHolder(gesture, gestureName);
        gestureHolder.setKeys(keys);

        return gestureHolder;
    }

    public void changeBtnBackground(View view) {
        Button btn = (Button) view;

        if (!btn.isSelected()) {
            btn.setSelected(true);
            btn.setBackgroundResource(R.drawable.btn_pressed);
        } else {
            btn.setSelected(false);
            btn.setBackgroundResource(R.drawable.btn_normal);
        }
    }

    public void inputDpadKey(View view) {
        Button btn = (Button) view;

        EditText editText = alertDialog.findViewById(R.id.gesture_keycode);
        editText.setText(btn.getText());
    }
}
