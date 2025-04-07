/**
 * Copyright (C) 2012-2017 Iordan Iordanov
 * Copyright (C) 2010 Michael A. MacDonald
 * <p>
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * <p>
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this software; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
 * USA.
 */

//
// CanvasView is the Activity for showing VNC Desktop.
//
package com.qihua.bVNC;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.Prediction;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.hardware.display.DisplayManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.os.StrictMode;
import android.os.SystemClock;
import android.os.Vibrator;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.qihua.bVNC.dialogs.EnterTextDialog;
import com.qihua.bVNC.dialogs.MetaKeyDialog;
import com.qihua.bVNC.extrakeys.ExtraKeyButton;
import com.qihua.bVNC.extrakeys.ExtraKeysConstants;
import com.qihua.bVNC.extrakeys.ExtraKeysInfo;
import com.qihua.bVNC.extrakeys.ExtraKeysView;
import com.qihua.bVNC.extrakeys.SpecialButton;
import com.qihua.bVNC.extrakeys.SpecialButtonState;
import com.qihua.bVNC.gesture.GestureActionLibrary;
import com.qihua.bVNC.input.InputHandler;
import com.qihua.bVNC.input.InputHandlerTouchpad;
import com.qihua.bVNC.input.KeyBoardListenerHelper;
import com.qihua.bVNC.input.MetaKeyBean;
import com.qihua.bVNC.input.Panner;
import com.qihua.bVNC.input.RemoteCanvasHandler;
import com.qihua.bVNC.input.RemoteKeyboard;
import com.qihua.util.SamsungDexUtils;
import com.qihua.util.UriIntentParser;
import com.undatech.opaque.Connection;
import com.undatech.opaque.ConnectionSettings;
import com.undatech.opaque.MessageDialogs;
import com.undatech.opaque.RemoteClientLibConstants;
import com.undatech.opaque.util.FileUtils;
import com.undatech.opaque.util.OnTouchViewMover;
import com.undatech.opaque.util.RemoteToolbar;
import com.undatech.remoteClientUi.R;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

public class RemoteCanvasActivity extends AppCompatActivity implements OnKeyListener {

    public static final int[] inputModeIds = {R.id.itemInputTouchpad};
    public static final Map<Integer, String> inputModeMap;
    private final static String TAG = "RemoteCanvasActivity";
    private static final int[] scalingModeIds = {R.id.itemZoomable, R.id.itemFitToScreen,
            R.id.itemOneToOne};

    static {
        Map<Integer, String> temp = new HashMap<>();
        temp.put(R.id.itemInputTouchpad, InputHandlerTouchpad.ID);
        inputModeMap = Collections.unmodifiableMap(temp);
    }

    final long hideToolbarDelay = 1000;
    InputHandler inputHandler;
    Panner panner;
    Handler handler;
    RelativeLayout layoutKeys;
    ImageButton keyCtrl;
    boolean keyCtrlToggled;
    ImageButton keySuper;
    boolean keySuperToggled;
    ImageButton keyAlt;
    boolean keyAltToggled;
    ImageButton keyTab;
    ImageButton keyEsc;
    ImageButton keyShift;
    boolean keyShiftToggled;
    ImageButton keyUp;
    ImageButton keyDown;
    ImageButton keyLeft;
    ImageButton keyRight;
    ImageButton keyHome;
    ImageButton keyEnd;
    ImageButton keyKeyboard;
    boolean hardKeyboardExtended;
    boolean extraKeysHidden = true;
    volatile boolean softKeyboardUp;
    RemoteToolbar toolbar;
    View rootView;
    GestureOverlayView gestureOverlayView;
    ToolbarHiderRunnable toolbarHider = new ToolbarHiderRunnable();
    private Vibrator myVibrator;
    private RemoteCanvas canvas;
    private RemoteCanvas touchpad;
    private CanvasPresentation canvasPresentation;
    private MenuItem[] inputModeMenuItems;
    private MenuItem[] scalingModeMenuItems;
    private InputHandler inputModeHandlers[];
    private Connection connection;
    private GestureLibrary gestureLibrary;
    private GestureActionLibrary gestureActionLibrary;
    private float lastPanDist = 0f;
    private ExtraKeysView extraKeysView;

    /**
     * This runnable fixes things up after a rotation.
     */
    private Runnable rotationCorrector = () -> {
        try {
            correctAfterRotation();
        } catch (Exception e) {
        }
    };
    private MetaKeyBean lastSentKey;

    /**
     * Enables sticky immersive mode if supported.
     */
    private void enableImmersive() {
//        handler.removeCallbacks(immersiveEnabler);
//        handler.postDelayed(immersiveEnabler, 200);
    }

    /**
     * Disables sticky immersive mode.
     */
    private void disableImmersive() {
//        handler.removeCallbacks(immersiveDisabler);
//        handler.postDelayed(immersiveDisabler, 200);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            enableImmersive();
        }
    }

    public void hideToolbar() {
        handler.removeCallbacks(toolbarHider);

        ActionBar toolbar = getSupportActionBar();
        if (toolbar != null) {
            toolbar.hide();

            if (gestureOverlayView != null && canvas.connection.getEnableGesture()) {
                gestureOverlayView.setVisibility(View.GONE);
            }
        }
    }

    public boolean isToolbarShowing() {
        return toolbar.isShown();
    }

    @Override
    public void onCreate(Bundle icicle) {
        Log.d(TAG, "OnCreate called");
        super.onCreate(icicle);

        // TODO: Implement left-icon
        //requestWindowFeature(Window.FEATURE_LEFT_ICON);
        //setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.icon);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        Utils.showMenu(this);

        DisplayManager displayManager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        Display[] displays = displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION);

        if (Build.VERSION.SDK_INT >= 28) {
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(params);
        }

        if (displays.length >= 1) {
            setContentView(R.layout.control);

            // use external display in last place
            Display choosedDisplay = displays[displays.length - 1];

            try {
                canvasPresentation = new CanvasPresentation(getBaseContext(), choosedDisplay);
                canvasPresentation.show();
                canvas = canvasPresentation.getCanvas();

                touchpad = findViewById(R.id.touchpad);

                touchpad.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

                boolean isChinese = Utils.isChineseLocal(getApplicationContext());
                if (isChinese) {
                    touchpad.setImageResource(R.drawable.t_tips);
                } else {
                    touchpad.setImageResource(R.drawable.t_tips_en);
                }

                touchpad.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            } catch (Throwable ignored) {
                // fallback
                canvasPresentation = null;
                setContentView(R.layout.canvas_full);
                canvas = findViewById(R.id.canvas);
                touchpad = canvas;
            }
        } else {
            setContentView(R.layout.canvas_full);
            canvas = findViewById(R.id.canvas);
            touchpad = canvas;
        }

        canvas.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        touchpad.setOutDisplay(canvas != touchpad);
        canvas.setOutDisplay(canvas != touchpad);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        myVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        View decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener
                (new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        try {
                            correctAfterRotation();
                        } catch (Exception e) {
                            //e.printStackTrace();
                        }
                        //handler.postDelayed(rotationCorrector, 300);
                    }
                });

        Runnable setModes = new Runnable() {
            public void run() {
                try {
                    setModes();
                } catch (NullPointerException e) {
                }
            }
        };
        Runnable hideKeyboardAndExtraKeys = new Runnable() {
            public void run() {
                try {
                    hideKeyboardAndExtraKeys();
                } catch (NullPointerException e) {
                }
            }
        };

        if (Utils.isOpaque(this)) {
            initializeOpaque(setModes, hideKeyboardAndExtraKeys);
        } else {
            initialize(setModes, hideKeyboardAndExtraKeys);
        }
        if (connection != null && connection.isReadyForConnection()) {
            continueConnecting();
        }

        KeyBoardListenerHelper helper = new KeyBoardListenerHelper(this);
        helper.setOnKeyBoardChangeListener((isShow, keyBoardHeight) -> {

            if (!canvas.isOutDisplay()) {
                Rect r = new Rect();
                Rect re = new Rect();

                rootView.getWindowVisibleDisplayFrame(r);
                getWindow().getDecorView().getWindowVisibleDisplayFrame(re);
                float visibleDesktopHeight = canvas.getVisibleDesktopHeight();

                float panDistance = visibleDesktopHeight + keyBoardHeight - canvas.getHeight();
                if (isShow && panDistance > 0) {
                    canvas.absolutePan(canvas.getAbsX(), (int) (panDistance), false);
                    lastPanDist = panDistance;
                }

                if (!isShow && lastPanDist > 0) {
                    canvas.absolutePan(canvas.getAbsX(), (int) (-lastPanDist), false);
                }

                canvas.setVisibleDesktopHeight(r.bottom - re.top);
            }

            canvas.movePanToMakePointerVisible();
        });

        gestureOverlayView = findViewById(R.id.gestureOverlay);

        File gesturesDir = getDir("gestures", Context.MODE_PRIVATE);
        File gestureFile = new File(gesturesDir, canvas.connection.getId() + "_gestures.dat");
        gestureLibrary = GestureLibraries.fromFile(gestureFile);
        gestureLibrary.load();

        gestureActionLibrary = new GestureActionLibrary(canvas.connection.getId());
        gestureActionLibrary.load(getApplicationContext());

        gestureOverlayView.setOrientation(GestureOverlayView.ORIENTATION_VERTICAL);
        gestureOverlayView.setGestureStrokeWidth(20f);
        gestureOverlayView.setGestureColor(getColor(R.color.theme));
        gestureOverlayView.setEventsInterceptionEnabled(true);
        gestureOverlayView.setFadeEnabled(false);
        gestureOverlayView.setFadeOffset(0);
        gestureOverlayView.setGestureStrokeType(GestureOverlayView.GESTURE_STROKE_TYPE_SINGLE);
        gestureOverlayView.setGestureStrokeAngleThreshold(90);

        gestureOverlayView.addOnGesturePerformedListener((overlay, gesture) -> {
            ArrayList<Prediction> predictions = gestureLibrary.recognize(gesture);
            if (predictions.isEmpty()) {
                hideToolbar();

                return;
            }

            predictions.sort((o1, o2) -> (int) (o2.score - o1.score));

            // if highest score is less than 2, not recognized
            Prediction pre = predictions.get(0);
            if (pre.score < 5) {
                Toast.makeText(RemoteCanvasActivity.this, getString(R.string.gesture_not_recognized), Toast.LENGTH_SHORT).show();
                hideToolbar();

                return;
            }

            List<String> actionKeys = gestureActionLibrary.getAction(pre.name);
            if (actionKeys != null && !actionKeys.isEmpty()) {
                performShortKeys(actionKeys);
                Toast.makeText(RemoteCanvasActivity.this, getString(R.string.gesture_hint) + ":" + pre.name, Toast.LENGTH_SHORT).show();

//                Toast toast = new Toast(RemoteCanvasActivity.this);
//                View view = LayoutInflater.from(this).inflate(R.layout.gesture_tips_with_retry, null);
//                TextView textView = view.findViewById(R.id.toast_text);
//
//                textView.setText(String.format("%s:%s (%s)", getString(R.string.gesture_hint),
//                        pre.name, getString(R.string.gesture_click_to_retry)));
//                // textView.setMovementMethod(LinkMovementMethod.getInstance());
//                textView.setHighlightColor(Color.TRANSPARENT);
//                textView.setOnLongClickListener(new OnLongClickListener() {
//                    @Override
//                    public boolean onLongClick(View v) {
//                        myVibrator.vibrate(VibrationEffect.createOneShot(SHORT_VIBRATION
//                                , VibrationEffect.DEFAULT_AMPLITUDE));
//                        return true;
//                    }
//                });
//
//                toast.setView(view);
//                toast.setDuration(Toast.LENGTH_LONG);
//                toast.show();
            }

            hideToolbar();
        });

        // init the extra keys
        LayoutInflater inflater = LayoutInflater.from(this);
        extraKeysView = (ExtraKeysView) inflater.inflate(R.layout.view_extra_keys, layoutKeys, false);

        extraKeysView.setExtraKeysViewClient(new ExtraKeysView.IExtraKeysView() {
            @Override
            public void onExtraKeyButtonClick(View view, ExtraKeyButton buttonInfo, MaterialButton button) {
                performShortKeys(Arrays.asList(buttonInfo.getKey()));
            }

            @Override
            public boolean performExtraKeyButtonHapticFeedback(View view, ExtraKeyButton buttonInfo, MaterialButton button) {
                return false;
            }
        });
        extraKeysView.setButtonTextAllCaps(true);

        recreateExtraKeys();

        layoutKeys.addView(extraKeysView);

        Log.d(TAG, "OnCreate complete");
    }

    private void readExtraKeys() {
        if (Boolean.TRUE.equals(extraKeysView.readSpecialButton(SpecialButton.ALT, true))) {
            canvas.getKeyboard().onScreenAltOn();
        } else {
            canvas.getKeyboard().onScreenAltOff();
        }

        if (Boolean.TRUE.equals(extraKeysView.readSpecialButton(SpecialButton.CTRL, true))) {
            canvas.getKeyboard().onScreenCtrlOn();
        } else {
            canvas.getKeyboard().onScreenCtrlOff();
        }

        if (Boolean.TRUE.equals(extraKeysView.readSpecialButton(SpecialButton.SHIFT, true))) {
            canvas.getKeyboard().onScreenShiftOn();
        } else {
            canvas.getKeyboard().onScreenShiftOff();
        }
    }

    private void performShortKeys(List<String> keys) {
        readExtraKeys();

        if (keys.contains("ALT")) {
            canvas.getKeyboard().onScreenAltOn();
        }
        if (keys.contains("CTRL")) {
            canvas.getKeyboard().onScreenCtrlOn();
        }
        if (keys.contains("SHIFT")) {
            canvas.getKeyboard().onScreenShiftOn();
        }
        if (keys.contains("META")) {
            canvas.getKeyboard().onScreenSuperOn();
        }

        if (keys.contains("←") || keys.contains("LEFT")) {
            canvas.getKeyboard().keyEvent(KeyEvent.KEYCODE_DPAD_LEFT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT));
            canvas.getKeyboard().keyEvent(KeyEvent.KEYCODE_DPAD_LEFT, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT));
        } else if (keys.contains("↑") || keys.contains("UP")) {
            canvas.getKeyboard().keyEvent(KeyEvent.KEYCODE_DPAD_UP, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP));
            canvas.getKeyboard().keyEvent(KeyEvent.KEYCODE_DPAD_UP, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_UP));
        } else if (keys.contains("↓") || keys.contains("DOWN")) {
            canvas.getKeyboard().keyEvent(KeyEvent.KEYCODE_DPAD_DOWN, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN));
            canvas.getKeyboard().keyEvent(KeyEvent.KEYCODE_DPAD_DOWN, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_DOWN));
        } else if (keys.contains("→") || keys.contains("RIGHT")) {
            canvas.getKeyboard().keyEvent(KeyEvent.KEYCODE_DPAD_RIGHT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT));
            canvas.getKeyboard().keyEvent(KeyEvent.KEYCODE_DPAD_RIGHT, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT));
        } else if (keys.contains("TAB")) {
            canvas.getKeyboard().keyEvent(KeyEvent.KEYCODE_TAB, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_TAB));
            canvas.getKeyboard().keyEvent(KeyEvent.KEYCODE_TAB, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_TAB));
        } else if (keys.contains("ESC")) {
            canvas.getKeyboard().keyEvent(KeyEvent.KEYCODE_ESCAPE, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ESCAPE));
            canvas.getKeyboard().keyEvent(KeyEvent.KEYCODE_ESCAPE, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ESCAPE));
        } else if (keys.contains("␡") || keys.contains("BKSP")) {
            canvas.getKeyboard().keyEvent(KeyEvent.KEYCODE_DEL, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
            canvas.getKeyboard().keyEvent(KeyEvent.KEYCODE_DEL, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
        } else if (keys.contains("DEL")) {
            canvas.getKeyboard().keyEvent(KeyEvent.KEYCODE_DEL, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
            canvas.getKeyboard().keyEvent(KeyEvent.KEYCODE_DEL, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
        } else if (keys.contains("PGUP")) {
            canvas.getKeyboard().keyEvent(KeyEvent.KEYCODE_PAGE_UP, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_PAGE_UP));
            canvas.getKeyboard().keyEvent(KeyEvent.KEYCODE_PAGE_UP, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_PAGE_UP));
        } else if (keys.contains("PGDN")) {
            canvas.getKeyboard().keyEvent(KeyEvent.KEYCODE_PAGE_DOWN, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_PAGE_DOWN));
            canvas.getKeyboard().keyEvent(KeyEvent.KEYCODE_PAGE_DOWN, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_PAGE_DOWN));
        } else if (keys.contains("HOME")) {
            canvas.getKeyboard().keyEvent(KeyEvent.KEYCODE_MOVE_HOME, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MOVE_HOME));
            canvas.getKeyboard().keyEvent(KeyEvent.KEYCODE_MOVE_HOME, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MOVE_HOME));
        }  else if (keys.contains("END")) {
            canvas.getKeyboard().keyEvent(KeyEvent.KEYCODE_MOVE_END, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MOVE_END));
            canvas.getKeyboard().keyEvent(KeyEvent.KEYCODE_MOVE_END, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MOVE_END));
        } else {
            // perform the key
            if (!keys.isEmpty()) {
                canvas.getKeyboard().sendText(keys.get(keys.size() - 1));
            }
        }

        if (keys.contains("ALT")) {
            canvas.getKeyboard().onScreenAltOff();
        }
        if (keys.contains("CTRL")) {
            canvas.getKeyboard().onScreenCtrlOff();
        }
        if (keys.contains("SHIFT")) {
            canvas.getKeyboard().onScreenShiftOff();
        }
        if (keys.contains("META")) {
            canvas.getKeyboard().onScreenSuperOff();
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    void initialize(final Runnable setModes, final Runnable hideKeyboardAndExtraKeys) {
        handler = new RemoteCanvasHandler(this);

        if (Utils.querySharedPreferenceBoolean(this, Constants.keepScreenOnTag))
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (Utils.querySharedPreferenceBoolean(this, Constants.forceLandscapeTag))
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

        Intent i = getIntent();
        Uri data = i.getData();
        boolean isSupportedScheme = isSupportedScheme(data);
        if (isSupportedScheme || !Utils.isNullOrEmptry(i.getType())) {
            if (handleSupportedUri(data)) return;
        } else {
            handleSerializedConnection(i);
        }
        ((RemoteCanvasHandler) handler).setConnection(connection);
        canvas.initializeCanvas(connection, setModes, hideKeyboardAndExtraKeys);

        touchpad.setInputHandler(getInputHandlerById(R.id.itemInputTouchpad));
    }

    private void handleSerializedConnection(Intent i) {
        Log.d(TAG, "Initializing serialized connection");
        connection = new ConnectionBean(this);
        Bundle extras = i.getExtras();
        if (extras == null) {
            return;
        }

        PersistableBundle bundle = extras.getParcelable(Utils.getConnectionString(this));

        connection.populateFromPersistentBundle(bundle);
        connection.load(this);

        parsePortIfIpv4Address();
        setDefaultProtocolAndSshPorts();
    }

    private void setDefaultProtocolAndSshPorts() {
        if (connection.getPort() == 0)
            connection.setPort(Constants.DEFAULT_PROTOCOL_PORT);

        if (connection.getSshPort() == 0)
            connection.setSshPort(Constants.DEFAULT_SSH_PORT);
    }

    private void parsePortIfIpv4Address() {
        // Parse a HOST:PORT entry but only if not ipv6 address
        String host = connection.getAddress();
        if (!Utils.isValidIpv6Address(host) && host.indexOf(':') > -1) {
            String p = host.substring(host.indexOf(':') + 1);
            try {
                int parsedPort = Integer.parseInt(p);
                connection.setPort(parsedPort);
                connection.setAddress(host.substring(0, host.indexOf(':')));
            } catch (Exception e) {
                Log.i(TAG, "Could not parse port from address, will use default");
            }
        }
    }

    private boolean handleSupportedUri(Uri data) {
        Log.d(TAG, "Initializing classic connection from Intent.");
        if (isMasterPasswordEnabled()) {
            Utils.showFatalErrorMessage(this, getResources().getString(R.string.master_password_error_intents_not_supported));
            return true;
        }

        createConnectionFromUri(data);
        if (showConnectionScreenOrExitIfNotReadyForConnection()) return true;
        return false;
    }

    private void createConnectionFromUri(Uri data) {
        connection = UriIntentParser.loadFromUriOrCreateNew(data, this);
        String host = null;
        if (data != null) {
            host = data.getHost();
        }
        if (host != null && !host.startsWith(Utils.getConnectionString(this))) {
            UriIntentParser.parseFromUri(this, connection, data);
        }
    }

    private boolean isSupportedScheme(Uri data) {
        boolean isSupportedScheme = false;
        if (data != null) {
            String s = data.getScheme();
            isSupportedScheme = s.equals("rdp") || s.equals("spice") || s.equals("vnc");
        }
        return isSupportedScheme;
    }

    private boolean showConnectionScreenOrExitIfNotReadyForConnection() {
        // we need to save the connection to display the loading screen, so otherwise we should exit
        if (!connection.isReadyForConnection()) {
            Toast.makeText(this, getString(R.string.error_uri_noinfo_nosave), Toast.LENGTH_LONG).show();
            ;
            if (connection.isReadyToBeSaved()) {
                Log.i(TAG, "Exiting - Insufficent information to connect and connection was not saved.");
            } else {
                Log.i(TAG, "Insufficent information to connect, showing connection dialog.");
                // launch appropriate activity
                Class cls = bVNC.class;
                if (Utils.isRdp(this)) {
                    cls = aRDP.class;
                } else if (Utils.isSpice(this)) {
                    cls = aSPICE.class;
                }
                Intent bVncIntent = new Intent(this, cls);
                startActivity(bVncIntent);
            }
            Utils.justFinish(this);
            return true;
        }
        return false;
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private void initializeOpaque(final Runnable setModes, final Runnable hideKeyboardAndExtraKeys) {
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        Intent i = getIntent();
        String vvFileName = retrieveVvFileFromIntent(i);
        if (vvFileName == null) {
            android.util.Log.d(TAG, "Initializing session from connection settings.");
            connection = (ConnectionSettings) i.getSerializableExtra("com.undatech.opaque.ConnectionSettings");
        } else {
            android.util.Log.d(TAG, "Initializing session from vv file: " + vvFileName);
            File f = new File(vvFileName);
            if (!f.exists()) {
                // Quit with an error if the file does not exist.
                MessageDialogs.displayMessageAndFinish(this, R.string.vv_file_not_found, R.string.error_dialog_title);
                return;
            }
            connection = new ConnectionSettings(RemoteClientLibConstants.DEFAULT_SETTINGS_FILE);
            connection.load(getApplicationContext());
        }
        handler = new RemoteCanvasHandler(this, canvas, connection);
        canvas.init(connection, handler, setModes, hideKeyboardAndExtraKeys, vvFileName);
    }

    void continueConnecting() {
        android.util.Log.d(TAG, "continueConnecting");
        // Initialize and define actions for on-screen keys.
        initializeOnScreenKeys();

        touchpad.setOnKeyListener(this);
        touchpad.setFocusableInTouchMode(true);
        canvas.setDrawingCacheEnabled(true);

        // This code detects when the soft keyboard is up and sets an appropriate visibleHeight in vncCanvas.
        // When the keyboard is gone, it resets visibleHeight and pans zero distance to prevent us from being
        // below the desktop image (if we scrolled all the way down when the keyboard was up).
        // TODO: Move this into a separate thread, and post the visibility changes to the handler.
        //       to avoid occupying the UI thread with this.
        rootView = ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> relayoutViews(rootView));

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);

        if (Utils.querySharedPreferenceBoolean(this, Constants.leftHandedModeTag)) {
            params.gravity = Gravity.TOP | Gravity.LEFT;
        } else {
            params.gravity = Gravity.TOP | Gravity.RIGHT;
        }

        panner = new Panner(this, canvas.handler);

        toolbar = (RemoteToolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("");
        toolbar.getBackground().setAlpha(66);
        toolbar.setLayoutParams(params);
        setSupportActionBar(toolbar);
        showToolbar();
    }

    void relayoutViews(View rootView) {
        android.util.Log.d(TAG, "onGlobalLayout: start");
        if (canvas == null) {
            android.util.Log.d(TAG, "onGlobalLayout: canvas null, returning");
            return;
        }

        Rect r = new Rect();

        rootView.getWindowVisibleDisplayFrame(r);
        android.util.Log.d(TAG, "onGlobalLayout: getWindowVisibleDisplayFrame: " + r.toString());

        // To avoid setting the visible height to a wrong value after an screen unlock event
        // (when r.bottom holds the width of the screen rather than the height due to a rotation)
        // we make sure r.top is zero (i.e. there is no notification bar and we are in full-screen mode)
        // It's a bit of a hack.
        // One additional situation that needed handling was that devices with notches / cutouts don't
        // ever have r.top equal to zero. so a special case for them.
        Rect re = new Rect();
        getWindow().getDecorView().getWindowVisibleDisplayFrame(re);
//        if (r.top == 0 || re.top > 0) {
//            if (canvas.myDrawable != null) {
//                android.util.Log.d(TAG, "onGlobalLayout: Setting VisibleDesktopHeight to: " + (r.bottom - re.top));
//                canvas.setVisibleDesktopHeight(r.bottom - re.top);
//                canvas.relativePan(0, 0);
//            } else {
//                android.util.Log.d(TAG, "onGlobalLayout: canvas.myDrawable is null");
//            }
//        } else {
//            android.util.Log.d(TAG, "onGlobalLayout: Found r.top to be non-zero");
//        }

        // Enable/show the toolbar if the keyboard is gone, and disable/hide otherwise.
        // We detect the keyboard if more than 19% of the screen is covered.
        // Use the visible display frame of the decor view to compute notch dimensions.
        int rootViewHeight = rootView.getHeight();

        int layoutKeysBottom = layoutKeys.getBottom();
        int toolbarBottom = toolbar.getBottom();
        int rootViewBottom = layoutKeys.getRootView().getBottom();
        int diffLayoutKeysPosition = r.bottom - re.top - layoutKeysBottom;
        int diffToolbarPosition = r.bottom - re.top - toolbarBottom - r.bottom / 2;
        int diffToolbarPositionRightAbsolute = r.right - toolbar.getWidth();
        int diffToolbarPositionTopAbsolute = r.bottom - re.top - toolbar.getHeight() - r.bottom / 2;
        android.util.Log.d(TAG, "onGlobalLayout: before: r.bottom: " + r.bottom +
                " rootViewHeight: " + rootViewHeight + " re.top: " + re.top + " re.bottom: " + re.bottom +
                " layoutKeysBottom: " + layoutKeysBottom + " rootViewBottom: " + rootViewBottom + " toolbarBottom: " + toolbarBottom +
                " diffLayoutKeysPosition: " + diffLayoutKeysPosition + " diffToolbarPosition: " + diffToolbarPosition);

        boolean softKeyboardPositionChanged = false;
        if (r.bottom > rootViewHeight * 0.81) {
            android.util.Log.d(TAG, "onGlobalLayout: Less than 19% of screen is covered");
            if (softKeyboardUp) {
                softKeyboardPositionChanged = true;
            }
            softKeyboardUp = false;

            // Soft Kbd gone, shift the meta keys and arrows down.
            if (layoutKeys != null) {
                android.util.Log.d(TAG, "onGlobalLayout: shifting on-screen buttons down by: " + diffLayoutKeysPosition);
                layoutKeys.offsetTopAndBottom(diffLayoutKeysPosition);
                if (!connection.getUseLastPositionToolbar() || !connection.getUseLastPositionToolbarMoved()) {
                    toolbar.offsetTopAndBottom(diffToolbarPosition);
                } else {
                    toolbar.makeVisible(connection.getUseLastPositionToolbarX(),
                            connection.getUseLastPositionToolbarY(),
                            r.right,
                            r.bottom,
                            diffToolbarPositionRightAbsolute,
                            diffToolbarPositionTopAbsolute);
                }

                if (softKeyboardPositionChanged) {
                    android.util.Log.d(TAG, "onGlobalLayout: hiding on-screen buttons");
                    setExtraKeysVisibility(View.GONE, false);
                    canvas.invalidate();
                }
            }
        } else {
            android.util.Log.d(TAG, "onGlobalLayout: More than 19% of screen is covered");
            softKeyboardUp = true;

            //  Soft Kbd up, shift the meta keys and arrows up.
            if (layoutKeys != null) {
                Log.d(TAG, "onGlobalLayout: shifting on-screen buttons up by: " + diffLayoutKeysPosition);
                layoutKeys.offsetTopAndBottom(diffLayoutKeysPosition);
                if (!connection.getUseLastPositionToolbar() || !connection.getUseLastPositionToolbarMoved()) {
                    toolbar.offsetTopAndBottom(diffToolbarPosition);
                } else {
                    toolbar.makeVisible(connection.getUseLastPositionToolbarX(),
                            connection.getUseLastPositionToolbarY(),
                            r.right,
                            r.bottom,
                            diffToolbarPositionRightAbsolute,
                            diffToolbarPositionTopAbsolute);
                }

                if (extraKeysHidden) {
                    Log.d(TAG, "onGlobalLayout: on-screen buttons should be hidden");
                    setExtraKeysVisibility(View.GONE, false);
                } else {
                    Log.d(TAG, "onGlobalLayout: on-screen buttons should be showing");
                    setExtraKeysVisibility(View.VISIBLE, true);
                }
                canvas.invalidate();
            }
        }
        layoutKeysBottom = layoutKeys.getBottom();
        rootViewBottom = layoutKeys.getRootView().getBottom();
        android.util.Log.d(TAG, "onGlobalLayout: after: r.bottom: " + r.bottom +
                " rootViewHeight: " + rootViewHeight + " re.top: " + re.top + " re.bottom: " + re.bottom +
                " layoutKeysBottom: " + layoutKeysBottom + " rootViewBottom: " + rootViewBottom + " toolbarBottom: " + toolbarBottom +
                " diffLayoutKeysPosition: " + diffLayoutKeysPosition + " diffToolbarPosition: " + diffToolbarPosition);
    }

    /**
     * Retrieves a vv file from the intent if possible and returns the path to it.
     *
     * @param i
     * @return the vv file name or NULL if no file was discovered.
     */
    private String retrieveVvFileFromIntent(Intent i) {
        final Uri data = i.getData();
        String vvFileName = null;
        final String tempVvFile = getFilesDir() + "/tempfile.vv";
        int msgId = 0;

        android.util.Log.d(TAG, "Got intent: " + i.toString());

        if (data != null) {
            android.util.Log.d(TAG, "Got data: " + data.toString());
            final String dataString = data.toString();
            if (dataString.startsWith("http")) {
                android.util.Log.d(TAG, "Intent is with http scheme.");
                msgId = R.string.error_failed_to_download_vv_http;
                FileUtils.deleteFile(tempVvFile);

                // Spin up a thread to grab the file over the network.
                Thread t = new Thread() {
                    @Override
                    public void run() {
                        try {
                            // Download the file and write it out.
                            URL url = new URL(data.toString());
                            File file = new File(tempVvFile);

                            URLConnection ucon = url.openConnection();
                            FileUtils.outputToFile(ucon.getInputStream(), new File(tempVvFile));

                            synchronized (RemoteCanvasActivity.this) {
                                RemoteCanvasActivity.this.notify();
                            }
                        } catch (IOException e) {
                            int what = RemoteClientLibConstants.VV_OVER_HTTP_FAILURE;
                            if (dataString.startsWith("https")) {
                                what = RemoteClientLibConstants.VV_OVER_HTTPS_FAILURE;
                            }
                            // Quit with an error we could not download the .vv file.
                            handler.sendEmptyMessage(what);
                        }
                    }
                };
                t.start();

                synchronized (this) {
                    try {
                        this.wait(RemoteClientLibConstants.VV_GET_FILE_TIMEOUT);
                    } catch (InterruptedException e) {
                        vvFileName = null;
                        e.printStackTrace();
                    }
                    vvFileName = tempVvFile;
                }
            } else if (dataString.startsWith("file")) {
                android.util.Log.d(TAG, "Intent is with file scheme.");
                msgId = R.string.error_failed_to_obtain_vv_file;
                vvFileName = data.getPath();
            } else if (dataString.startsWith("content")) {
                android.util.Log.d(TAG, "Intent is with content scheme.");
                msgId = R.string.error_failed_to_obtain_vv_content;
                FileUtils.deleteFile(tempVvFile);

                try {
                    FileUtils.outputToFile(getContentResolver().openInputStream(data), new File(tempVvFile));
                    vvFileName = tempVvFile;
                } catch (IOException e) {
                    android.util.Log.e(TAG, "Could not write temp file: IOException.");
                    e.printStackTrace();
                } catch (SecurityException e) {
                    android.util.Log.e(TAG, "Could not write temp file: SecurityException.");
                    e.printStackTrace();
                }
            }

            // Check if we were successful in obtaining a file and put up an error dialog if not.
            if (dataString.startsWith("http") || dataString.startsWith("file") || dataString.startsWith("content")) {
                if (vvFileName == null)
                    MessageDialogs.displayMessageAndFinish(this, msgId, R.string.error_dialog_title);
            }
            android.util.Log.d(TAG, "Got filename: " + vvFileName);
        }

        return vvFileName;
    }

    public void extraKeysToggle(MenuItem m) {
        if (layoutKeys.getVisibility() == View.VISIBLE) {
            extraKeysHidden = true;
            setExtraKeysVisibility(View.GONE, false);
        } else {
            extraKeysHidden = false;
            setExtraKeysVisibility(View.VISIBLE, true);
        }
        setKeyStowDrawableAndVisibility(m);
        relayoutViews(rootView);
    }

    private void setKeyStowDrawableAndVisibility(MenuItem m) {
        if (m == null) {
            return;
        }
        Drawable replacer;
        if (connection.getExtraKeysToggleType() == Constants.EXTRA_KEYS_OFF) {
            m.setVisible(false);
        } else {
            m.setVisible(true);
        }
        if (layoutKeys.getVisibility() == View.GONE)
            replacer = getResources().getDrawable(R.drawable.showkeys);
        else
            replacer = getResources().getDrawable(R.drawable.hidekeys);

        m.setIcon(replacer);
    }

    public void sendShortVibration() {
        if (myVibrator != null) {
            myVibrator.vibrate(Constants.SHORT_VIBRATION);
        } else {
            Log.i(TAG, "Device cannot vibrate, not sending vibration");
        }
    }

    /**
     * Initializes the on-screen keys for meta keys and arrow keys.
     */
    private void initializeOnScreenKeys() {
        layoutKeys = (RelativeLayout) findViewById(R.id.layoutKeys);
    }

    /**
     * Resets the state and image of the on-screen keys.
     */
    private void resetOnScreenKeys(int keyCode) {
        // Do not reset on-screen keys if keycode is SHIFT.
        switch (keyCode) {
            case KeyEvent.KEYCODE_SHIFT_LEFT:
            case KeyEvent.KEYCODE_SHIFT_RIGHT:
                return;
        }
        if (!keyCtrlToggled) {
            keyCtrl.setImageResource(R.drawable.ctrloff);
            canvas.getKeyboard().onScreenCtrlOff();
        }
        if (!keyAltToggled) {
            keyAlt.setImageResource(R.drawable.altoff);
            canvas.getKeyboard().onScreenAltOff();
        }
        if (!keySuperToggled) {
            keySuper.setImageResource(R.drawable.superoff);
            canvas.getKeyboard().onScreenSuperOff();
        }
        if (!keyShiftToggled) {
            keyShift.setImageResource(R.drawable.shiftoff);
            canvas.getKeyboard().onScreenShiftOff();
        }
    }

    /**
     * Sets the visibility of the extra keys appropriately.
     */
    private void setExtraKeysVisibility(int visibility, boolean forceVisible) {
//        Configuration config = getResources().getConfiguration();
        //Log.e(TAG, "Hardware kbd hidden: " + Integer.toString(config.hardKeyboardHidden));
        //Log.e(TAG, "Any keyboard hidden: " + Integer.toString(config.keyboardHidden));
        //Log.e(TAG, "Keyboard type: " + Integer.toString(config.keyboard));

//        boolean makeVisible = forceVisible;
//        if (config.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_YES)
//            makeVisible = true;

        if (!extraKeysHidden && forceVisible &&
                connection.getExtraKeysToggleType() == Constants.EXTRA_KEYS_ON) {
            layoutKeys.setVisibility(View.VISIBLE);
            layoutKeys.invalidate();
            return;
        }

        if (visibility == View.GONE) {
            layoutKeys.setVisibility(View.GONE);
            layoutKeys.invalidate();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause called.");
//        try {
//            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//            imm.hideSoftInputFromWindow(canvas.getWindowToken(), 0);
//        } catch (NullPointerException e) {
//        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume called.");
//        try {
//            canvas.postInvalidateDelayed(2500);
//        } catch (NullPointerException e) {
//        }
    }

    /**
     * Set modes on start to match what is specified in the ConnectionBean;
     * color mode (already done) scaling, input mode
     */
    void setModes() {
        Log.d(TAG, "setModes");
        inputHandler = getInputHandlerByName(connection.getInputMode());
        AbstractScaling.getByScaleType(connection.getScaleMode()).setScaleTypeForActivity(this);
        initializeOnScreenKeys();
        try {
            COLORMODEL cm = COLORMODEL.valueOf(connection.getColorModel());
            canvas.setColorModel(cm);
        } catch (IllegalArgumentException e) {
            return;
        }
        canvas.setOnKeyListener(this);
        canvas.setFocusableInTouchMode(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            canvas.setFocusedByDefault(true);
        }
        canvas.requestFocus();
        canvas.setDrawingCacheEnabled(false);

        SamsungDexUtils.INSTANCE.dexMetaKeyCapture(this);
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onCreateDialog(int)
     */
    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == R.layout.entertext) {
            return new EnterTextDialog(this);
        } else if (id == R.id.itemHelpInputMode) {
            return createHelpDialog();
        }

        // Default to meta key dialog
        return new MetaKeyDialog(this);
    }

    /**
     * Creates the help dialog for this activity.
     */
    private Dialog createHelpDialog() {
        AlertDialog.Builder adb = new AlertDialog.Builder(this)
                .setMessage(R.string.input_mode_help_text)
                .setPositiveButton(R.string.close,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                // We don't have to do anything.
                            }
                        });
        Dialog d = adb.setView(new ListView(this)).create();
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(d.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        d.show();
        d.getWindow().setAttributes(lp);
        return d;
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onPrepareDialog(int, android.app.Dialog)
     */
    @SuppressWarnings("deprecation")
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        super.onPrepareDialog(id, dialog);
        if (dialog instanceof ConnectionSettable)
            ((ConnectionSettable) dialog).setConnection(connection);
    }

    /**
     * This function is called by the rotationCorrector runnable
     * to fix things up after a rotation.
     */
    private void correctAfterRotation() throws Exception {
        Log.d(TAG, "correctAfterRotation");
        canvas.waitUntilInflated();

        if (canvas.canvasZoomer == null) {
            return;
        }

        // Its quite common to see NullPointerExceptions here when this function is called
        // at the point of disconnection. Hence, we catch and ignore the error.
        float oldScale = canvas.canvasZoomer.getZoomFactor();
        int x = canvas.absoluteXPosition;
        int y = canvas.absoluteYPosition;
        canvas.canvasZoomer.setScaleTypeForActivity(RemoteCanvasActivity.this);
        float newScale = canvas.canvasZoomer.getZoomFactor();
        canvas.canvasZoomer.changeZoom(this, oldScale / newScale, 0, 0);
        newScale = canvas.canvasZoomer.getZoomFactor();
        if (newScale <= oldScale &&
                canvas.canvasZoomer.getScaleType() != ImageView.ScaleType.FIT_CENTER) {
            canvas.absoluteXPosition = x;
            canvas.absoluteYPosition = y;
            canvas.resetScroll();
        }
        // Automatic resolution update request handling
        if (canvas.isVnc && connection.getRdpResType() == Constants.VNC_GEOM_SELECT_AUTOMATIC) {
            canvas.rfbconn.requestResolution(canvas.getWidth(), canvas.getHeight());
        } else if (canvas.isSpice && connection.getRdpResType() == Constants.RDP_GEOM_SELECT_AUTO) {
            canvas.spicecomm.requestResolution(canvas.getWidth(), canvas.getHeight());
        } else if (canvas.isOpaque && connection.isRequestingNewDisplayResolution()) {
            canvas.spicecomm.requestResolution(canvas.getWidth(), canvas.getHeight());
        }

        // Auto change extra keys to horizontal or vertical mode
        recreateExtraKeys();
    }

    private void recreateExtraKeys() {
        if (canvas.getWidth() > canvas.getHeight()) {
            try {
                ExtraKeysInfo extraKeysInfo = new ExtraKeysInfo(ExtraKeysConstants.DEFAULT_HOR_IVALUE_EXTRA_KEYS, ExtraKeysConstants.DEFAULT_IVALUE_EXTRA_KEYS_STYLE, ExtraKeysConstants.CONTROL_CHARS_ALIASES);
                extraKeysView.reload(extraKeysInfo, 37f);
            } catch (Exception ignore) {

            }
        } else {
            try {
                ExtraKeysInfo extraKeysInfo = new ExtraKeysInfo(ExtraKeysConstants.DEFAULT_VER_IVALUE_EXTRA_KEYS, ExtraKeysConstants.DEFAULT_IVALUE_EXTRA_KEYS_STYLE, ExtraKeysConstants.CONTROL_CHARS_ALIASES);
                extraKeysView.reload(extraKeysInfo, 37f);
            } catch (Exception ignore) {

            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        enableImmersive();
        try {
            setExtraKeysVisibility(View.GONE, false);

            // Correct a few times just in case. There is no visual effect.
            handler.postDelayed(rotationCorrector, 300);
        } catch (NullPointerException e) {
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onStart called.");
        try {
            canvas.postInvalidateDelayed(2500);
        } catch (NullPointerException e) {
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "onStop called.");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.i(TAG, "onRestart called.");
        try {
            canvas.postInvalidateDelayed(2500);
        } catch (NullPointerException e) {
        }
    }

    @Override
    public void onPanelClosed(int featureId, Menu menu) {
        super.onPanelClosed(featureId, menu);
        showToolbar();
        enableImmersive();
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        if (menu != null) {
            android.util.Log.i(TAG, "Menu opened, disabling hiding action bar");
            handler.removeCallbacks(toolbarHider);
            updateScalingMenu();
            updateInputMenu();
            disableImmersive();
        }
        return super.onMenuOpened(featureId, menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Make sure extra keys stow item is gone if extra keys are disabled and vice versa.
//        setKeyStowDrawableAndVisibility(menu.findItem(R.id.extraKeysToggle));
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "OnCreateOptionsMenu called");
        try {
            getMenuInflater().inflate(R.menu.vnccanvasactivitymenu, menu);

            Menu inputMenu = menu.findItem(R.id.itemInputMode).getSubMenu();
            inputModeMenuItems = new MenuItem[inputModeIds.length];
            for (int i = 0; i < inputModeIds.length; i++) {
                inputModeMenuItems[i] = inputMenu.findItem(inputModeIds[i]);
            }
            updateInputMenu();

            Menu scalingMenu = menu.findItem(R.id.itemScaling).getSubMenu();
            scalingModeMenuItems = new MenuItem[scalingModeIds.length];
            for (int i = 0; i < scalingModeIds.length; i++) {
                scalingModeMenuItems[i] = scalingMenu.findItem(scalingModeIds[i]);
            }
            updateScalingMenu();

            OnTouchListener moveListener = new OnTouchViewMover(toolbar, handler, toolbarHider, hideToolbarDelay);
            ImageButton moveButton = new ImageButton(this);

            moveButton.setBackgroundResource(R.drawable.short_vertical_bar);
            moveButton.setMinimumWidth(36);
            MenuItem moveToolbar = menu.findItem(R.id.moveToolbar);
            moveToolbar.setActionView(moveButton);
            moveToolbar.getActionView().setOnTouchListener(moveListener);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "OnCreateOptionsMenu complete");
        return true;
    }

    /**
     * Change the scaling mode sub-menu to reflect available scaling modes.
     */
    void updateScalingMenu() {
        try {
            for (MenuItem item : scalingModeMenuItems) {
                // If the entire framebuffer is NOT contained in the bitmap, fit-to-screen is meaningless.
                if (item.getItemId() == R.id.itemFitToScreen) {
                    if (canvas != null && canvas.myDrawable != null &&
                            (canvas.myDrawable.bitmapheight != canvas.myDrawable.framebufferheight ||
                                    canvas.myDrawable.bitmapwidth != canvas.myDrawable.framebufferwidth)) {
                        item.setEnabled(false);
                    } else {
                        item.setEnabled(true);
                    }
                } else {
                    item.setEnabled(true);
                }

                AbstractScaling scaling = AbstractScaling.getById(item.getItemId());
                if (scaling.scaleType == connection.getScaleMode()) {
                    item.setChecked(true);
                }
            }
        } catch (NullPointerException e) {
        }
    }

    /**
     * Change the input mode sub-menu to reflect change in scaling
     */
    void updateInputMenu() {
        try {
            for (MenuItem item : inputModeMenuItems) {
                item.setEnabled(canvas.canvasZoomer.isValidInputMode(item.getItemId()));
                if (getInputHandlerById(item.getItemId()) == inputHandler)
                    item.setChecked(true);
            }
        } catch (NullPointerException e) {
        }
    }

    /**
     * If id represents an input handler, return that; otherwise return null
     *
     * @param id
     * @return
     */
    InputHandler getInputHandlerById(int id) {
        myVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        if (inputModeHandlers == null) {
            inputModeHandlers = new InputHandler[inputModeIds.length];
        }
        for (int i = 0; i < inputModeIds.length; ++i) {
            if (inputModeIds[i] == id) {
                if (inputModeHandlers[i] == null) {
                    if (id == R.id.itemInputTouchpad) {
                        inputModeHandlers[i] = new InputHandlerTouchpad(this, canvas, touchpad, canvas.getPointer(), App.debugLog);
                    } else {
                        throw new IllegalStateException("Unexpected value: " + id);
                    }
                }
                return inputModeHandlers[i];
            }
        }
        return null;
    }

    void clearInputHandlers() {
        if (inputModeHandlers == null)
            return;

        for (int i = 0; i < inputModeIds.length; ++i) {
            inputModeHandlers[i] = null;
        }
        inputModeHandlers = null;
    }

    InputHandler getInputHandlerByName(String name) {
        InputHandler result = null;
        for (int id : inputModeIds) {
            InputHandler handler = getInputHandlerById(id);
            if (handler.getId().equals(name)) {
                result = handler;
                break;
            }
        }
        if (result == null) {
            result = getInputHandlerById(R.id.itemInputTouchpad);
        }
        return result;
    }

    int getModeIdFromHandler(InputHandler handler) {
        for (int id : inputModeIds) {
            if (handler == getInputHandlerById(id))
                return id;
        }
        return R.id.itemInputTouchpad;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        RemoteKeyboard k = canvas.getKeyboard();
        if (k != null) {
            k.setAfterMenu(true);
        }
        int itemId = item.getItemId();
        if (itemId == R.id.itemInfo) {
            canvas.showConnectionInfo();
            return true;
//        } else if (itemId == R.id.itemSpecialKeys) {
//            showDialog(R.layout.metakey);
//            return true;
//        } else if (itemId == R.id.itemColorMode) {
//            selectColorModel();
//            return true;
//            // Following sets one of the scaling options
        } else if (itemId == R.id.itemZoomable || itemId == R.id.itemOneToOne || itemId == R.id.itemFitToScreen) {
            AbstractScaling.getById(item.getItemId()).setScaleTypeForActivity(this);
            item.setChecked(true);
            showPanningState(false);
            return true;
//        } else if (itemId == R.id.itemCenterMouse) {
//            canvas.getPointer().movePointer(canvas.absoluteXPosition + canvas.getVisibleDesktopWidth() / 2,
//                    canvas.absoluteYPosition + canvas.getVisibleDesktopHeight() / 2);
//            return true;
        } else if (itemId == R.id.itemDisconnect) {
            canvas.closeConnection();
            Utils.justFinish(this);
            return true;
//        } else if (itemId == R.id.itemEnterText) {
//            showDialog(R.layout.entertext);
//            return true;
//        } else if (itemId == R.id.itemCtrlAltDel) {
//            canvas.getKeyboard().sendMetaKey(MetaKeyBean.keyCtrlAltDel);
//            return true;
//        } else if (itemId == R.id.itemSendKeyAgain) {
//            sendSpecialKeyAgain();
//            return true;
//            // Disabling Manual/Wiki Menu item as the original does not correspond to this project anymore.
//            //case R.id.itemOpenDoc:
//            //    Utils.showDocumentation(this);
//            //    return true;
        } else if (itemId == R.id.itemHelpInputMode) {
            showDialog(R.id.itemHelpInputMode);
            return true;
        } else {
            boolean inputModeSet = setInputMode(item.getItemId());
            item.setChecked(inputModeSet);
            if (inputModeSet) {
                return inputModeSet;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean setInputMode(int id) {
        InputHandler input = getInputHandlerById(id);
        if (input != null) {
            inputHandler = input;
            connection.setInputMode(input.getId());
            if (input.getId().equals(InputHandlerTouchpad.ID)) {
                connection.setFollowMouse(true);
                connection.setFollowPan(true);
            } else {
                connection.setFollowMouse(false);
                connection.setFollowPan(false);
                canvas.getPointer().setRelativeEvents(false);
            }

            showPanningState(true);
            connection.save(this);
            return true;
        }
        return false;
    }

    private void sendSpecialKeyAgain() {
        if (lastSentKey == null
                || lastSentKey.get_Id() != connection.getLastMetaKeyId()) {
            ArrayList<MetaKeyBean> keys = new ArrayList<MetaKeyBean>();
            Database database = new Database(this);
            Cursor c = database.getReadableDatabase().rawQuery(
                    MessageFormat.format("SELECT * FROM {0} WHERE {1} = {2}",
                            MetaKeyBean.GEN_TABLE_NAME,
                            MetaKeyBean.GEN_FIELD__ID, connection
                                    .getLastMetaKeyId()),
                    MetaKeyDialog.EMPTY_ARGS);
            MetaKeyBean.Gen_populateFromCursor(c, keys, MetaKeyBean.NEW);
            c.close();
            database.close();
            if (keys.size() > 0) {
                lastSentKey = keys.get(0);
            } else {
                lastSentKey = null;
            }
        }
        if (lastSentKey != null)
            canvas.getKeyboard().sendMetaKey(lastSentKey);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy called.");
        if (canvas != null)
            canvas.closeConnection();
        System.gc();
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent evt) {
        readExtraKeys();

        boolean consumed = false;

        if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (evt.getAction() == KeyEvent.ACTION_DOWN)
                return super.onKeyDown(keyCode, evt);
            else
                return super.onKeyUp(keyCode, evt);
        }

        try {
            if (evt.getAction() == KeyEvent.ACTION_DOWN || evt.getAction() == KeyEvent.ACTION_MULTIPLE) {
                consumed = inputHandler.onKeyDown(keyCode, evt);
            } else if (evt.getAction() == KeyEvent.ACTION_UP) {
                consumed = inputHandler.onKeyUp(keyCode, evt);
            }
            resetOnScreenKeys(keyCode);
        } catch (NullPointerException e) {
        }

        return consumed;
    }

    public void showPanningState(boolean showLonger) {
        if (showLonger) {
            final Toast t = Toast.makeText(this, inputHandler.getDescription(), Toast.LENGTH_LONG);
            TimerTask tt = new TimerTask() {
                @Override
                public void run() {
                    t.show();
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                    }
                    t.show();
                }
            };
            new Timer().schedule(tt, 2000);
            t.show();
        } else {
            Toast t = Toast.makeText(this, inputHandler.getDescription(), Toast.LENGTH_SHORT);
            t.show();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onTrackballEvent(android.view.MotionEvent)
     */
    @Override
    public boolean onTrackballEvent(MotionEvent event) {
        try {
            // If we are using the Dpad as arrow keys, don't send the event to the inputHandler.
            if (connection.getUseDpadAsArrows())
                return false;
            return inputHandler.onTouchEvent(event);
        } catch (NullPointerException e) {
        }
        return super.onTrackballEvent(event);
    }

    // Send touch events or mouse events like button clicks to be handled.
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        try {
            return inputHandler.onTouchEvent(event);
        } catch (NullPointerException e) {
        }
        return super.onTouchEvent(event);
    }

    // Send e.g. mouse events like hover and scroll to be handled.
    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        // Ignore TOOL_TYPE_FINGER events that come from the touchscreen with HOVER type action
        // which cause pointer jumping trouble in simulated touchpad for some devices.
        boolean toolTypeFinger = false;
        if (Constants.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            toolTypeFinger = event.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER;
        }
        int a = event.getAction();
        if (!((a == MotionEvent.ACTION_HOVER_ENTER ||
                a == MotionEvent.ACTION_HOVER_EXIT ||
                a == MotionEvent.ACTION_HOVER_MOVE) &&
                event.getSource() == InputDevice.SOURCE_TOUCHSCREEN &&
                toolTypeFinger
        )) {
            try {
                return inputHandler.onTouchEvent(event);
            } catch (NullPointerException e) {
            }
        }
        return super.onGenericMotionEvent(event);
    }

    private void selectColorModel() {

        String[] choices = new String[COLORMODEL.values().length];
        int currentSelection = -1;
        for (int i = 0; i < choices.length; i++) {
            COLORMODEL cm = COLORMODEL.values()[i];
            choices[i] = cm.toString();
            if (canvas.isColorModel(cm))
                currentSelection = i;
        }

        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ListView list = new ListView(this);
        list.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_checked, choices));
        list.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        list.setItemChecked(currentSelection, true);
        list.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                dialog.dismiss();
                COLORMODEL cm = COLORMODEL.values()[arg2];
                canvas.setColorModel(cm);
                connection.setColorModel(cm.nameString());
                connection.save(RemoteCanvasActivity.this);
                Toast.makeText(RemoteCanvasActivity.this, getString(R.string.info_update_color_model_to) + cm.toString(), Toast.LENGTH_SHORT).show();
            }
        });
        dialog.setContentView(list);
        dialog.show();
    }

    public void showToolbar() {
        getSupportActionBar().show();
        handler.removeCallbacks(toolbarHider);
        handler.postAtTime(toolbarHider, SystemClock.uptimeMillis() + hideToolbarDelay);

        // show gesture overlay if long press action is not showing the layer
        String longPressType = Utils.querySharedPreferenceString(getApplicationContext()
                , Constants.touchpadLongPressAction, "left");

        if (gestureOverlayView != null && canvas.connection.getEnableGesture() && !longPressType.equals("gesture")) {
            gestureOverlayView.setVisibility(View.VISIBLE);
        }

    }

//    @Override
//    public void onTextSelected(String selectedString) {
//        android.util.Log.i(TAG, "onTextSelected called with selectedString: " + selectedString);
////        canvas.pd.show();
//        connection.setVmname(canvas.vmNameToId.get(selectedString));
//        connection.save(this);
//        synchronized (canvas.spicecomm) {
//            canvas.spicecomm.notify();
//        }
//    }

    public void toggleKeyboard(MenuItem menuItem) {
        handler.post(() -> {
            // show gesture overlay
            if (gestureOverlayView != null && canvas.connection.getEnableGesture()) {
                gestureOverlayView.setVisibility(View.GONE);
            }

            if (softKeyboardUp) {
                hideKeyboardAndExtraKeys();
            } else {
                showKeyboardAndExtraKeys();
            }
        });
    }

    public void fillScreen(MenuItem menuItem) {
        handler.post(() -> {
            // show gesture overlay
            if (gestureOverlayView != null && canvas.connection.getEnableGesture()) {
                gestureOverlayView.setVisibility(View.GONE);
            }

            float zoomRatio;
            float diff = (float) canvas.getWidth() / canvas.getHeight() - (float) canvas.getImageWidth() / canvas.getImageHeight();
            if (diff > 0) {
                zoomRatio = (float) canvas.getWidth() / canvas.getImageWidth();
            } else {
                zoomRatio = (float) canvas.getHeight() / canvas.getImageHeight();
            }

            // Because the zoom action is relative, we should divide the current zoom factor
            canvas.canvasZoomer.changeZoom(this, zoomRatio / canvas.canvasZoomer.getZoomFactor(), (float) canvas.getWidth() / 2, 0);
        });
    }

//    public void sendCopy(MenuItem menuItem) {
//        canvas.getKeyboard().sendUnicode('c', KeyEvent.META_CTRL_LEFT_ON);
//    }
//
//    public void sendPaste(MenuItem menuItem) {
//        canvas.getKeyboard().sendUnicode('v', KeyEvent.META_CTRL_LEFT_ON);
//    }


    public void showKeyboard() {
        android.util.Log.i(TAG, "Showing keyboard and hiding action bar");

        Utils.showKeyboard(this, touchpad);
        softKeyboardUp = true;
    }

    public void hideKeyboard() {
        android.util.Log.i(TAG, "Hiding keyboard and hiding action bar");

        Utils.hideKeyboard(this, touchpad);
        softKeyboardUp = false;
    }

    public void hideKeyboardAndExtraKeys() {
        hideKeyboard();
        if (layoutKeys.getVisibility() == View.VISIBLE) {
            extraKeysHidden = true;
            setExtraKeysVisibility(View.GONE, false);
        }
    }

    public void showKeyboardAndExtraKeys() {
        showKeyboard();
        if (layoutKeys.getVisibility() == View.GONE) {
            extraKeysHidden = false;
            setExtraKeysVisibility(View.VISIBLE, false);
        }
    }


    public void stopPanner() {
        panner.stop();
    }

    public Connection getConnection() {
        return connection;
    }

    // Returns whether we are using D-pad/Trackball to send arrow key events.
    public boolean getUseDpadAsArrows() {
        return connection.getUseDpadAsArrows();
    }

    // Returns whether the D-pad should be rotated to accommodate BT keyboards paired with phones.
    public boolean getRotateDpad() {
        return connection.getRotateDpad();
    }

    public RemoteCanvas getCanvas() {
        return canvas;
    }

    public Panner getPanner() {
        return panner;
    }

    public void setPanner(Panner panner) {
        this.panner = panner;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("WORKAROUND_FOR_BUG_19917_KEY", "WORKAROUND_FOR_BUG_19917_VALUE");
        super.onSaveInstanceState(outState);
    }

    private boolean isMasterPasswordEnabled() {
        SharedPreferences sp = getSharedPreferences(Constants.generalSettingsTag, Context.MODE_PRIVATE);
        return sp.getBoolean(Constants.masterPasswordEnabledTag, false);
    }

    @Override
    public void onBackPressed() {
        if (inputHandler != null) {
            inputHandler.onKeyDown(KeyEvent.KEYCODE_BACK, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
        }
    }

    private class ToolbarHiderRunnable implements Runnable {
        public void run() {
            hideToolbar();
        }
    }

    public void disconnectAndClose() {
        if (canvasPresentation != null) {
            canvasPresentation.dismiss();
        }

        canvas.disconnectWithoutMessage();

        finish();
    }
}
