/**
 * Copyright (C) 2012 Iordan Iordanov
 * Copyright (C) 2010 Michael A. MacDonald
 * Copyright (C) 2004 Horizon Wimba.  All Rights Reserved.
 * Copyright (C) 2001-2003 HorizonLive.com, Inc.  All Rights Reserved.
 * Copyright (C) 2001,2002 Constantin Kaplinsky.  All Rights Reserved.
 * Copyright (C) 2000 Tridia Corporation.  All Rights Reserved.
 * Copyright (C) 1999 AT&T Laboratories Cambridge.  All Rights Reserved.
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
// RemoteCanvas is a subclass of android.view.SurfaceView which draws a VNC
// desktop on it.
//

package com.qihua.bVNC;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.ClipboardManager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatImageView;

import com.qihua.android.bc.BCFactory;
import com.qihua.bVNC.dialogs.GetTextFragment;
import com.qihua.bVNC.exceptions.AnonCipherUnsupportedException;
import com.qihua.bVNC.input.InputHandler;
import com.qihua.bVNC.input.InputHandlerTouchpad;
import com.qihua.bVNC.input.RemoteCanvasHandler;
import com.qihua.bVNC.input.RemoteKeyboard;
import com.qihua.bVNC.input.RemotePointer;
import com.qihua.bVNC.input.RemoteRdpKeyboard;
import com.qihua.bVNC.input.RemoteRdpPointer;
import com.qihua.bVNC.input.RemoteSpiceKeyboard;
import com.qihua.bVNC.input.RemoteSpicePointer;
import com.qihua.bVNC.input.RemoteVncKeyboard;
import com.qihua.bVNC.input.RemoteVncPointer;
import com.tigervnc.rfb.AuthFailureException;
import com.undatech.opaque.Connection;
import com.undatech.opaque.MessageDialogs;
import com.undatech.opaque.RdpCommunicator;
import com.undatech.opaque.RemoteClientLibConstants;
import com.undatech.opaque.RfbConnectable;
import com.undatech.opaque.SpiceCommunicator;
import com.undatech.opaque.Viewable;
import com.undatech.opaque.proxmox.ProxmoxClient;
import com.undatech.opaque.proxmox.pojo.PveRealm;
import com.undatech.opaque.proxmox.pojo.PveResource;
import com.undatech.opaque.proxmox.pojo.SpiceDisplay;
import com.undatech.opaque.proxmox.pojo.VmStatus;
import com.undatech.opaque.util.FileUtils;
import com.undatech.remoteClientUi.R;

import org.apache.http.HttpException;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

import javax.security.auth.login.LoginException;

public class RemoteCanvas extends AppCompatImageView
        implements Viewable, GetTextFragment.OnFragmentDismissedListener {
    private final static String TAG = "RemoteCanvas";

    public AbstractScaling canvasZoomer;

    // Variable indicating that we are currently scrolling in simulated touchpad mode.
    public boolean cursorBeingMoved = false;

    // Connection parameters
    public Connection connection;
    public SSHConnection sshConnection = null;
    // VNC protocol connection
    public RfbConnectable rfbconn = null;
    public RfbProto rfb = null;
    public SpiceCommunicator spicecomm = null;
    public boolean maintainConnection = true;
    public AbstractBitmapData myDrawable;
    // Progress dialog shown at connection time.
    public AlertDialog progressDialog;
    public boolean serverJustCutText = false;
    public Runnable setModes;
    public Runnable hideKeyboardAndExtraKeys;
    public boolean spiceUpdateReceived = false;
    /**
     * Handler for the dialogs that display the x509/RDP/SSH key signatures to the user.
     * Also shows the dialogs which show various connection failures.
     */
    public Handler handler;

    private InputHandler inputHandler;

    private boolean outDisplay = false;

    Database database;
    Map<String, String> vmNameToId = new HashMap<String, String>();
    // RFB Decoder
    Decoder decoder = null;
    // The remote pointer and keyboard
    RemotePointer pointer;
    RemoteKeyboard keyboard;
    boolean useFull = false;
    boolean compact = false;
    // Used to set the contents of the clipboard.
    ClipboardManager clipboard;
    Timer clipboardMonitorTimer;
    ClipboardMonitor clipboardMonitor;
    /*
     * Position of the top left portion of the <i>visible</i> part of the screen, in
     * full-frame coordinates
     */
    int absoluteXPosition = 0, absoluteYPosition = 0;

    /*
     * How much to shift coordinates over when converting from full to view coordinates.
     */
    float shiftX = 0, shiftY = 0;

    /*
     * This variable holds the height of the visible rectangle of the screen. It is used to keep track
     * of how much of the screen is hidden by the soft keyboard if any.
     */
    int visibleHeight = -1;

    /*
     * These variables contain the width and height of the display in pixels
     */
    int displayWidth = 0;
    int displayHeight = 0;
    float displayDensity = 0;

    /*
     * This flag indicates whether this is the VNC client.
     */
    boolean isVnc = false;

    /*
     * This flag indicates whether this is the RDP client.
     */
    boolean isRdp = false;

    /*
     * This flag indicates whether this is the SPICE client.
     */
    boolean isSpice = false;

    /*
     * This flag indicates whether this is the NvStream(Sunshine) client.
     */
    boolean isNvStream = false;

    /*
     * This flag indicates whether this is the Opaque client.
     */
    boolean isOpaque = false;
    boolean sshTunneled = false;
    long lastDraw;
    boolean userPanned = false;
    String vvFileName;
    /**
     * This runnable displays a message on the screen.
     */
    CharSequence screenMessage;
    /**
     * Shows a non-fatal error message.
     *
     * @param error
     */
    Runnable showDialogMessage = new Runnable() {
        public void run() {
            Utils.showErrorMessage(getContext(), String.valueOf(screenMessage));
        }
    };

    private RdpCommunicator rdpcomm = null;
    // Internal bitmap data
    private int capacity;
    /**
     * This runnable sets the drawable (contained in myDrawable) for the VncCanvas (ImageView).
     */
    private Runnable drawableSetter = new Runnable() {
        public void run() {
            android.util.Log.d(TAG, "drawableSetter.run");
            if (myDrawable != null) {
                android.util.Log.d(TAG, "drawableSetter myDrawable not null");
                myDrawable.setImageDrawable(RemoteCanvas.this);
            } else {
                android.util.Log.e(TAG, "drawableSetter myDrawable is null");
            }
        }
    };
    private Runnable showMessage = new Runnable() {
        public void run() {
            Toast.makeText(getContext(), screenMessage, Toast.LENGTH_SHORT).show();
        }
    };
    /**
     * This runnable causes a toast with information about the current connection to be shown.
     */
    private Runnable desktopInfo = new Runnable() {
        public void run() {
            showConnectionInfo();
        }
    };

    /**
     * Constructor used by the inflation apparatus
     *
     * @param context
     */
    public RemoteCanvas(final Context context, AttributeSet attrs) {
        super(context, attrs);

        clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
//        isVnc = Utils.isVnc(getContext());
//        isRdp = Utils.isRdp(getContext());
//        isSpice = Utils.isSpice(getContext());
//        isOpaque = Utils.isOpaque(getContext());

        Display display;
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) {
            display = ((Activity) context).getWindow().getWindowManager().getDefaultDisplay();
        } else {
            if (context instanceof Activity) {
                display = ((Activity) context).getWindow().getWindowManager().getDefaultDisplay();
            } else {
                display = context.getDisplay();
            }
        }

        displayWidth = display.getWidth();
        displayHeight = display.getHeight();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        displayDensity = metrics.density;

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.connection_progress, null);

        // 配置ProgressBar和TextView
        ProgressBar progressBar = dialogView.findViewById(R.id.progressBar);
        TextView messageView = dialogView.findViewById(R.id.message);
        messageView.setText(R.string.info_progress_dialog_establishing);

        progressDialog = new AlertDialog.Builder(getContext())
                .setTitle(R.string.info_progress_dialog_connecting)
                .setView(dialogView)  // 使用自定义视图
                .setCancelable(false)
                .setOnCancelListener(dialog -> {
                    closeConnection();
                    handler.post(() ->
                            Utils.showFatalErrorMessage(getContext(),
                                    getContext().getString(R.string.info_progress_dialog_aborted)));
                })
                .create();
    }

    public void startPointerCapture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requestPointerCapture();
        }

        requestFocus();
    }

    @Override
    public boolean onCapturedPointerEvent(MotionEvent event) {
        return inputHandler.onPointerEvent(event);
    }

    void init(final Connection settings, final Handler handler, final Runnable setModes, final Runnable hideKeyboardAndExtraKeys, final String vvFileName) {
        this.connection = settings;
        this.handler = handler;
        this.setModes = setModes;
        this.hideKeyboardAndExtraKeys = hideKeyboardAndExtraKeys;
        this.vvFileName = vvFileName;

        checkNetworkConnectivity();
        initializeClipboardMonitor();
        spicecomm = new SpiceCommunicator(getContext(), handler, this,
                settings.isRequestingNewDisplayResolution() || settings.getRdpResType() == Constants.RDP_GEOM_SELECT_CUSTOM,
                !Utils.isFree(getContext()) && settings.isUsbEnabled(), App.debugLog);
        rfbconn = spicecomm;
        pointer = new RemoteSpicePointer(spicecomm, this, handler, App.debugLog);
        try {
            keyboard = new RemoteSpiceKeyboard(getResources(), spicecomm, this, handler,
                    settings.getLayoutMap(), App.debugLog);
        } catch (Throwable e) {
            handleUncaughtException(e);
        }
        maintainConnection = true;
        if (vvFileName == null) {
            if (connection.getConnectionTypeString().equals(getResources().getString(R.string.connection_type_pve))) {
                startPve();
            } else {
                connection.setAddress(Utils.getHostFromUriString(connection.getAddress()));
                startOvirt();
            }
        } else {
            startFromVvFile(vvFileName);
        }
    }

    /**
     * Checks whether the device has networking and quits with an error if it doesn't.
     */
    private void checkNetworkConnectivity() {
        ConnectivityManager cm = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork == null || !activeNetwork.isAvailable() || !activeNetwork.isConnected()) {
            disconnectAndShowMessage(R.string.error_not_connected_to_network, R.string.error_dialog_title);
        }
    }

    public void disconnectAndShowMessage(final int messageId, final int titleId) {
        closeConnection();
        handler.post(new Runnable() {
            public void run() {
                MessageDialogs.displayMessageAndFinish(getContext(), messageId, titleId);
            }
        });
    }

    public void disconnectAndShowMessage(final int messageId, final int titleId, final String textToAppend) {
        closeConnection();
        handler.post(new Runnable() {
            public void run() {
                MessageDialogs.displayMessageAndFinish(getContext(), messageId, titleId, textToAppend);
            }
        });
    }

    public void disconnectWithoutMessage() {
        closeConnection();
//        Utils.justFinish(getContext());
    }

    public void saveZoomFactor(float zoomFactor) {
        connection.setLastZoomFactor(zoomFactor);
        connection.saveAndWriteRecent(false, getContext());
    }

    /**
     * Initializes the clipboard monitor.
     */
    private void initializeClipboardMonitor() {
        clipboardMonitor = new ClipboardMonitor(getContext(), this);
        if (clipboardMonitor != null) {
            clipboardMonitorTimer = new Timer();
            if (clipboardMonitorTimer != null) {
                clipboardMonitorTimer.schedule(clipboardMonitor, 0, 500);
            }
        }
    }

    /**
     * Reinitialize Canvas
     */
    public void reinitializeCanvas() {
        Log.i(TAG, "Reinitializing remote canvas");
        initializeCanvas(this.connection, this.setModes, this.hideKeyboardAndExtraKeys);
        handler.post(this.hideKeyboardAndExtraKeys);
    }

    /**
     * Reinitialize Opaque
     */
    public void reinitializeOpaque() {
        Log.i(TAG, "Reinitializing remote canvas opaque");
        init(this.connection, this.handler, this.setModes, this.hideKeyboardAndExtraKeys, this.vvFileName);
        handler.post(this.hideKeyboardAndExtraKeys);
    }

    /**
     * Create a view showing a remote desktop connection
     *
     * @param conn     Connection settings
     * @param setModes Callback to run on UI thread after connection is set up
     */
    public RemotePointer initializeCanvas(Connection conn, final Runnable setModes, final Runnable hideKeyboardAndExtraKeys) {
        progressDialog.show();

        maintainConnection = true;
        this.setModes = setModes;
        this.hideKeyboardAndExtraKeys = hideKeyboardAndExtraKeys;
        connection = conn;
        sshTunneled = (connection.getConnectionType() == Constants.CONN_TYPE_SSH);
        handler = new RemoteCanvasHandler(getContext(), this, connection);

        isVnc = conn.getConnectionType() == Constants.CONN_TYPE_VNC;
        isRdp = conn.getConnectionType() == Constants.CONN_TYPE_RDP;

        try {
            if (isSpice) {
                initializeSpiceConnection();
            } else if (isRdp) {
                initializeRdpConnection();
            } else if (isVnc) {
                initializeVncConnection();
            } else {
                throw new Exception("unknown connection type");
            }
        } catch (Throwable e) {
            handleUncaughtException(e);
        }

        Thread t = new Thread() {
            public void run() {
                try {
//                    sshConnection = new SSHConnection(connection, getContext(), handler);
//                    sshConnection.changeOrInitializeSshHostKey(false);
                    if (isSpice) {
                        startSpiceConnection();
                    } else if (isRdp) {
                        startRdpConnection();
                    } else if (isVnc) {
                        startVncConnection();
                    } else {
                        throw new Exception("unknown connection type");
                    }

                } catch (Throwable e) {
                    handleUncaughtException(e);
                }
            }
        };
        t.start();

        clipboardMonitor = new ClipboardMonitor(getContext(), this);
        clipboardMonitorTimer = new Timer();
        try {
            clipboardMonitorTimer.schedule(clipboardMonitor, 0, 500);
        } catch (NullPointerException ignored) {
        }

        return pointer;
    }

    private void handleUncaughtException(Throwable e) {
        if (maintainConnection) {
            Log.e(TAG, e.toString());
//            e.printStackTrace();
            // Ensure we dismiss the progress dialog before we finish
            if (progressDialog.isShowing())
                progressDialog.dismiss();

            if (e instanceof OutOfMemoryError) {
                disposeDrawable();
                showFatalMessageAndQuit(getContext().getString(R.string.error_out_of_memory));
            } else {
                String error = getContext().getString(R.string.error_connection_failed);
                if (e.getMessage() != null) {
                    if (e.getMessage().indexOf("SSH") < 0 &&
                            (e.getMessage().indexOf("authentication") > -1 ||
                                    e.getMessage().indexOf("Unknown security result") > -1 ||
                                    e.getMessage().indexOf("password check failed") > -1)
                    ) {
                        error = getContext().getString(R.string.error_vnc_authentication);
                    }
                    error = error + "<br>" + e.getLocalizedMessage();
                }
                showFatalMessageAndQuit(error);
            }
        }
    }

    /**
     * Retreives the requested remote width.
     */
    @Override
    public int getDesiredWidth() {
        int w = getRemoteWidth(getWidth(), getHeight());
        if (!connection.isRequestingNewDisplayResolution() &&
                connection.getRdpResType() == Constants.RDP_GEOM_SELECT_CUSTOM) {
            w = connection.getRdpWidth();
        }
        android.util.Log.d(TAG, "Width requested: " + w);
        return w;
    }

    /**
     * Retreives the requested remote height.
     */
    @Override
    public int getDesiredHeight() {
        int h = getRemoteHeight(getWidth(), getHeight());
        if (!connection.isRequestingNewDisplayResolution() &&
                connection.getRdpResType() == Constants.RDP_GEOM_SELECT_CUSTOM) {
            h = connection.getRdpHeight();
        }
        android.util.Log.d(TAG, "Height requested: " + h);
        return h;
    }

    /**
     * Initializes a SPICE connection.
     */
    private void initializeSpiceConnection() throws Exception {
        spicecomm = new SpiceCommunicator(getContext(), handler, this, true,
                !Utils.isFree(getContext()) && connection.isUsbEnabled(),
                App.debugLog);
        rfbconn = spicecomm;
        pointer = new RemoteSpicePointer(rfbconn, RemoteCanvas.this, handler, App.debugLog);
        keyboard = new RemoteSpiceKeyboard(getResources(), spicecomm, RemoteCanvas.this,
                handler, connection.getLayoutMap(), App.debugLog);
        //spicecomm.setUIEventListener(RemoteCanvas.this);
        spicecomm.setHandler(handler);
    }

    /**
     * Starts a SPICE connection using libspice.
     */
    private void startSpiceConnection() throws Exception {
        // Get the address and port (based on whether an SSH tunnel is being established or not).
        String address = getAddress();
        // To prevent an SSH tunnel being created when port or TLS port is not set, we only
        // getPort when port/tport are positive.
        int port = connection.getPort();
        if (port > 0) {
            port = getRemoteProtocolPort(port);
        }

        int tport = connection.getTlsPort();
        if (tport > 0) {
            tport = getRemoteProtocolPort(tport);
        }

        spicecomm.connectSpice(address, Integer.toString(port), Integer.toString(tport), connection.getPassword(),
                connection.getCaCertPath(), null, // TODO: Can send connection.getCaCert() here instead
                connection.getCertSubject(), connection.getEnableSound());
    }

    /**
     * Initializes an RDP connection.
     */
    private void initializeRdpConnection() throws Exception {
        android.util.Log.i(TAG, "initializeRdpConnection: Initializing RDP connection.");

        rdpcomm = new RdpCommunicator(getContext(), handler, this,
                connection.getUserName(), connection.getRdpDomain(), connection.getPassword(),
                App.debugLog);
        rfbconn = rdpcomm;
        pointer = new RemoteRdpPointer(rfbconn, this, handler, App.debugLog);
        keyboard = new RemoteRdpKeyboard(rdpcomm, this, handler, App.debugLog,
                connection.getPreferSendingUnicode());
    }

    /**
     * Starts an RDP connection using the FreeRDP library.
     */
    private void startRdpConnection() throws Exception {
        android.util.Log.i(TAG, "startRdpConnection: Starting RDP connection.");

        // Get the address and port (based on whether an SSH tunnel is being established or not).
        String address = getAddress();
        int rdpPort = getRemoteProtocolPort(connection.getPort());
        waitUntilInflated();
        int remoteWidth = getRemoteWidth(getWidth(), getHeight());
        int remoteHeight = getRemoteHeight(getWidth(), getHeight());

        rdpcomm.setConnectionParameters(address, rdpPort, connection.getNickname(), remoteWidth,
                remoteHeight, connection.getDesktopBackground(), connection.getFontSmoothing(),
                connection.getDesktopComposition(), connection.getWindowContents(),
                connection.getMenuAnimation(), connection.getVisualStyles(),
                connection.getRedirectSdCard(), connection.getConsoleMode(),
                connection.getRemoteSoundType(), connection.getEnableRecording(),
                connection.getRemoteFx(), connection.getEnableGfx(), connection.getEnableGfxH264(),
                connection.getRdpColor());
        rdpcomm.connect();
    }

    /**
     * Initializes a VNC connection.
     */
    private void initializeVncConnection() throws Exception {
        Log.i(TAG, "Initializing connection to: " + connection.getAddress() + ", port: " + connection.getPort());
        boolean sslTunneled = connection.getConnectionType() == Constants.CONN_TYPE_STUNNEL;
        decoder = new Decoder(this, connection.getUseLocalCursor() == Constants.CURSOR_FORCE_LOCAL);
        rfb = new RfbProto(decoder, this, connection.getPrefEncoding(), connection.getViewOnly(),
                sslTunneled, connection.getIdHashAlgorithm(), connection.getIdHash(), connection.getX509KeySignature(),
                App.debugLog);

        rfbconn = rfb;
        pointer = new RemoteVncPointer(rfbconn, RemoteCanvas.this, handler, App.debugLog);
        boolean rAltAsIsoL3Shift = Utils.querySharedPreferenceBoolean(this.getContext(),
                Constants.rAltAsIsoL3ShiftTag);
        keyboard = new RemoteVncKeyboard(rfbconn, RemoteCanvas.this, handler,
                rAltAsIsoL3Shift, App.debugLog);
    }

    /**
     * Starts a VNC connection using the TightVNC backend.
     */
    private void startVncConnection() throws Exception {

        try {
            String address = getAddress();
            int vncPort = getRemoteProtocolPort(connection.getPort());
            Log.i(TAG, "Establishing VNC session to: " + address + ", port: " + vncPort);
            // TODO: VNC Server cert is not set when the connection is SSH tunneled because there at
            // TODO: present it is assumed the connection is either SSH tunneled or x509 encrypted,
            // TODO: and when both are the case, there is no way to save the x509 cert.
            String sslCert = connection.getX509KeySignature();
            rfb.initializeAndAuthenticate(address, vncPort, connection.getUserName(),
                    connection.getPassword(), connection.getUseRepeater(),
                    connection.getRepeaterId(), connection.getConnectionType(),
                    sslCert);
        } catch (AnonCipherUnsupportedException e) {
            showFatalMessageAndQuit(getContext().getString(R.string.error_anon_dh_unsupported));
        } catch (RfbProto.RfbPasswordAuthenticationException e) {
            Log.e(TAG, "Authentication failed, will prompt user for password");
            handler.sendEmptyMessage(RemoteClientLibConstants.GET_VNC_PASSWORD);
            return;
        } catch (RfbProto.RfbUsernameRequiredException e) {
            Log.e(TAG, "Username required, will prompt user for username and password");
            handler.sendEmptyMessage(RemoteClientLibConstants.GET_VNC_CREDENTIALS);
            return;
        } catch (AuthFailureException e) {
            Log.e(TAG, "TigerVNC AuthFailureException: " + e.getLocalizedMessage());
            handler.sendEmptyMessage(RemoteClientLibConstants.GET_VNC_CREDENTIALS);
            return;
        } catch (Exception e) {
            throw new Exception(getContext().getString(R.string.error_vnc_unable_to_connect) +
                    Utils.messageAndStackTraceAsString(e));
        }

        rfb.writeClientInit();
        rfb.readServerInit();

        // Is custom resolution enabled?
        if (connection.getRdpResType() != Constants.VNC_GEOM_SELECT_DISABLED) {
            waitUntilInflated();
            rfb.setPreferredFramebufferSize(getVncRemoteWidth(getWidth(), getHeight()),
                    getVncRemoteHeight(getWidth(), getHeight()));
        }

        reallocateDrawable(displayWidth, displayHeight);
        decoder.setPixelFormat(rfb);

        handler.post(() ->
                progressDialog.setMessage(getContext().getString(R.string.info_progress_dialog_downloading)));

        sendUnixAuth();
        handler.post(drawableSetter);

        // Hide progress dialog
        if (progressDialog.isShowing())
            progressDialog.dismiss();

        try {
            rfb.processProtocol();
        } catch (RfbProto.RfbUltraVncColorMapException e) {
            Log.e(TAG, "UltraVnc supports only 24bpp. Switching color mode and reconnecting.");
            connection.setColorModel(COLORMODEL.C24bit.nameString());
            connection.save(getContext());
            handler.sendEmptyMessage(RemoteClientLibConstants.REINIT_SESSION);
        }
    }

    /**
     * Initialize the canvas to show the remote desktop
     */
    void startOvirt() {
//        if (!pd.isShowing())
//            pd.show();

        Thread cThread = new Thread() {
            @Override
            public void run() {
                try {
                    // Obtain user's password if necessary.
                    if (connection.getPassword().equals("")) {
                        android.util.Log.i(TAG, "Displaying a dialog to obtain user's password.");
                        handler.sendEmptyMessage(RemoteClientLibConstants.GET_PASSWORD);
                        synchronized (spicecomm) {
                            spicecomm.wait();
                        }
                    }

                    String ovirtCaFile = null;
                    if (connection.isUsingCustomOvirtCa()) {
                        ovirtCaFile = connection.getOvirtCaFile();
                    } else {
                        ovirtCaFile = new File(getContext().getFilesDir(), "ssl/certs/ca-certificates.crt").getPath();
                    }

                    // If not VM name is specified, then get a list of VMs and let the user pick one.
                    if (connection.getVmname().equals("")) {
                        int success = spicecomm.fetchOvirtVmNames(connection.getHostname(), connection.getUserName(),
                                connection.getPassword(), ovirtCaFile,
                                connection.isSslStrict());
                        // VM retrieval was unsuccessful we do not continue.
                        ArrayList<String> vmNames = spicecomm.getVmNames();
                        if (success != 0 || vmNames.isEmpty()) {
                            return;
                        } else {
                            // If there is just one VM, pick it and skip the dialog.
                            if (vmNames.size() == 1) {
                                connection.setVmname(vmNames.get(0));
                                connection.save(getContext());
                            } else {
                                while (connection.getVmname().equals("")) {
                                    android.util.Log.i(TAG, "Displaying a dialog with VMs to the user.");
                                    // Populate the data structure that is used to convert VM names to IDs.
                                    for (String s : vmNames) {
                                        vmNameToId.put(s, s);
                                    }
                                    handler.sendMessage(RemoteCanvasHandler.getMessageStringList(RemoteClientLibConstants.DIALOG_DISPLAY_VMS,
                                            "vms", vmNames));
                                    synchronized (spicecomm) {
                                        spicecomm.wait();
                                    }
                                }
                            }
                        }
                    }
                    spicecomm.setHandler(handler);
                    spicecomm.connectOvirt(connection.getHostname(),
                            connection.getVmname(),
                            connection.getUserName(),
                            connection.getPassword(),
                            ovirtCaFile,
                            connection.isAudioPlaybackEnabled(), connection.isSslStrict());

                    try {
                        synchronized (spicecomm) {
                            spicecomm.wait(35000);
                        }
                    } catch (InterruptedException e) {
                    }

                    if (!spiceUpdateReceived && maintainConnection) {
                        handler.sendEmptyMessage(RemoteClientLibConstants.OVIRT_TIMEOUT);
                    }

                } catch (Throwable e) {
                    handleUncaughtException(e);
                }
            }
        };
        cThread.start();
    }

    /**
     * Initialize the canvas to show the remote desktop
     *
     * @return
     */
    // TODO: Switch away from writing out a file to initiating a connection directly.
    String retrieveVvFileFromPve(final String hostname, final ProxmoxClient api, final String vmId,
                                 final String node, final String virt) {
        android.util.Log.i(TAG, String.format("Trying to connect to PVE host: " + hostname));
        final String tempVvFile = getContext().getFilesDir() + "/tempfile.vv";
        FileUtils.deleteFile(tempVvFile);

        Thread cThread = new Thread() {
            @Override
            public void run() {
                try {
                    VmStatus status = api.getCurrentStatus(node, virt, Integer.parseInt(vmId));
                    if (status.getStatus().equals(VmStatus.STOPPED)) {
                        api.startVm(node, virt, Integer.parseInt(vmId));
                        while (!status.getStatus().equals(VmStatus.RUNNING)) {
                            status = api.getCurrentStatus(node, virt, Integer.parseInt(vmId));
                            SystemClock.sleep(500);
                        }
                    }
                    SpiceDisplay spiceData = api.spiceVm(node, virt, Integer.parseInt(vmId));
                    if (spiceData != null) {
                        spiceData.outputToFile(tempVvFile, hostname);
                    } else {
                        android.util.Log.e(TAG, "PVE returned null data for display.");
                        handler.sendEmptyMessage(RemoteClientLibConstants.PVE_NULL_DATA);
                    }
                } catch (LoginException e) {
                    android.util.Log.e(TAG, "Failed to login to PVE.");
                    handler.sendEmptyMessage(RemoteClientLibConstants.PVE_FAILED_TO_AUTHENTICATE);
                } catch (JSONException e) {
                    android.util.Log.e(TAG, "Failed to parse json from PVE.");
                    handler.sendEmptyMessage(RemoteClientLibConstants.PVE_FAILED_TO_PARSE_JSON);
                } catch (NumberFormatException e) {
                    android.util.Log.e(TAG, "Error converting PVE ID to integer.");
                    handler.sendEmptyMessage(RemoteClientLibConstants.PVE_VMID_NOT_NUMERIC);
                } catch (IOException e) {
                    android.util.Log.e(TAG, "IO Error communicating with PVE API: " + e.getMessage());
                    handler.sendMessage(RemoteCanvasHandler.getMessageString(RemoteClientLibConstants.PVE_API_IO_ERROR,
                            "error", e.getMessage()));
                    e.printStackTrace();
                } catch (HttpException e) {
                    android.util.Log.e(TAG, "PVE API returned error code: " + e.getMessage());
                    handler.sendMessage(RemoteCanvasHandler.getMessageString(RemoteClientLibConstants.PVE_API_UNEXPECTED_CODE,
                            "error", e.getMessage()));
                }
                // At this stage we have either retrieved display data or failed, so permit the UI thread to continue.
                synchronized (tempVvFile) {
                    tempVvFile.notify();
                }
            }
        };
        cThread.start();

        // Wait until a timeout or until we are notified the worker thread trying to retrieve display data is done.
        synchronized (tempVvFile) {
            try {
                tempVvFile.wait();
            } catch (InterruptedException e) {
                handler.sendEmptyMessage(RemoteClientLibConstants.PVE_TIMEOUT_COMMUNICATING);
                e.printStackTrace();
            }
        }

        File checkFile = new File(tempVvFile);
        if (!checkFile.exists() || checkFile.length() == 0) {
            return null;
        }
        return tempVvFile;
    }

    /**
     * Initialize the canvas to show the remote desktop
     */
    void startFromVvFile(final String vvFileName) {
        Thread cThread = new Thread() {
            @Override
            public void run() {
                try {
                    spicecomm.startSessionFromVvFile(vvFileName, connection.isAudioPlaybackEnabled());
                } catch (Throwable e) {
                    handleUncaughtException(e);
                }
            }
        };
        cThread.start();
    }

    /**
     * Initialize the canvas to show the remote desktop
     */
    void startPve() {
//        if (!pd.isShowing())
//            pd.show();

        Thread cThread = new Thread() {
            @Override
            public void run() {
                try {
                    // Obtain user's password if necessary.
                    if (connection.getPassword().equals("")) {
                        android.util.Log.i(TAG, "Displaying a dialog to obtain user's password.");
                        handler.sendEmptyMessage(RemoteClientLibConstants.GET_PASSWORD);
                        synchronized (spicecomm) {
                            spicecomm.wait();
                        }
                    }

                    String user = connection.getUserName();
                    String realm = RemoteClientLibConstants.PVE_DEFAULT_REALM;

                    // Try to parse realm from user entered
                    int indexOfAt = connection.getUserName().indexOf('@');
                    if (indexOfAt != -1) {
                        realm = user.substring(indexOfAt + 1);
                        user = user.substring(0, indexOfAt);
                    }

                    // Connect to the API and obtain available realms
                    String uriToParse = connection.getHostname();
                    if (!uriToParse.startsWith("http://") && !uriToParse.startsWith("https://")) {
                        uriToParse = String.format("%s%s", "https://", uriToParse);
                    }
                    Uri uri = Uri.parse(uriToParse);
                    String protocol = uri.getScheme();
                    String host = uri.getHost();
                    int port = uri.getPort();
                    if (port < 0) {
                        port = 8006;
                    }
                    String pveUri = String.format("%s://%s:%d", protocol, host, port);

                    ProxmoxClient api = new ProxmoxClient(pveUri, connection, handler);
                    HashMap<String, PveRealm> realms = api.getAvailableRealms();

                    // If selected realm has TFA enabled, then ask for the code
                    if (realms.get(realm).getTfa() != null) {
                        android.util.Log.i(TAG, "Displaying a dialog to obtain OTP/TFA.");
                        handler.sendEmptyMessage(RemoteClientLibConstants.GET_OTP_CODE);
                        synchronized (spicecomm) {
                            spicecomm.wait();
                        }
                    }

                    // Login with provided credentials
                    api.login(user, realm, connection.getPassword(), connection.getOtpCode());

                    // Get map of user parseable names to resources
                    Map<String, PveResource> nameToResources = api.getResources();

                    if (nameToResources.isEmpty()) {
                        android.util.Log.e(TAG, "No available VMs found for user in PVE cluster");
                        disconnectAndShowMessage(R.string.error_no_vm_found_for_user, R.string.error_dialog_title);
                        return;
                    }

                    String vmId = connection.getVmname();
                    if (vmId.matches("/")) {
                        vmId = connection.getVmname().replaceAll(".*/", "");
                        connection.setVmname(vmId);
                        connection.save(getContext());
                    }

                    String node = null;
                    String virt = null;

                    // If there is just one VM, pick it and ignore what is saved in settings.
                    if (nameToResources.size() == 1) {
                        android.util.Log.e(TAG, "A single VM was found, so picking it.");
                        String key = (String) nameToResources.keySet().toArray()[0];
                        PveResource a = nameToResources.get(key);
                        node = a.getNode();
                        virt = a.getType();
                        connection.setVmname(a.getVmid());
                        connection.save(getContext());
                    } else {
                        while (connection.getVmname().isEmpty()) {
                            android.util.Log.i(TAG, "PVE: Displaying a dialog with VMs to the user.");
                            // Populate the data structure that is used to convert VM names to IDs.
                            for (String s : nameToResources.keySet()) {
                                vmNameToId.put(nameToResources.get(s).getName() + " (" + s + ")", s);
                            }
                            // Get the user parseable names and display them
                            ArrayList<String> vms = new ArrayList<String>(vmNameToId.keySet());
                            handler.sendMessage(RemoteCanvasHandler.getMessageStringList(
                                    RemoteClientLibConstants.DIALOG_DISPLAY_VMS, "vms", vms));
                            synchronized (spicecomm) {
                                spicecomm.wait();
                            }
                        }

                        // At this point, either the user selected a VM or there was an ID saved.
                        if (nameToResources.get(connection.getVmname()) != null) {
                            node = nameToResources.get(connection.getVmname()).getNode();
                            virt = nameToResources.get(connection.getVmname()).getType();
                        } else {
                            android.util.Log.e(TAG, "No VM with the following ID was found: " + connection.getVmname());
                            disconnectAndShowMessage(R.string.error_no_such_vm_found_for_user, R.string.error_dialog_title);
                            return;
                        }
                    }

                    vmId = connection.getVmname();
                    // Only if we managed to obtain a VM name we try to get a .vv file for the display.
                    if (!vmId.isEmpty()) {
                        String vvFileName = retrieveVvFileFromPve(host, api, vmId, node, virt);
                        if (vvFileName != null) {
                            startFromVvFile(vvFileName);
                        }
                    }
                } catch (LoginException e) {
                    android.util.Log.e(TAG, "Failed to login to PVE.");
                    handler.sendEmptyMessage(RemoteClientLibConstants.PVE_FAILED_TO_AUTHENTICATE);
                } catch (JSONException e) {
                    android.util.Log.e(TAG, "Failed to parse json from PVE.");
                    handler.sendEmptyMessage(RemoteClientLibConstants.PVE_FAILED_TO_PARSE_JSON);
                } catch (IOException e) {
                    android.util.Log.e(TAG, "IO Error communicating with PVE API: " + e.getMessage());
                    handler.sendMessage(RemoteCanvasHandler.getMessageString(RemoteClientLibConstants.PVE_API_IO_ERROR,
                            "error", e.getMessage()));
                    e.printStackTrace();
                } catch (HttpException e) {
                    android.util.Log.e(TAG, "PVE API returned error code: " + e.getMessage());
                    handler.sendMessage(RemoteCanvasHandler.getMessageString(RemoteClientLibConstants.PVE_API_UNEXPECTED_CODE,
                            "error", e.getMessage()));
                } catch (Throwable e) {
                    handleUncaughtException(e);
                }
            }
        };
        cThread.start();
    }

    /**
     * Sends over the unix username and password if this is VNC over SSH connectio and automatic sending of
     * UNIX credentials is enabled for AutoX (for x11vnc's "-unixpw" option).
     */
    void sendUnixAuth() {
        // If the type of connection is ssh-tunneled and we are told to send the unix credentials, then do so.
        if (sshTunneled && connection.getAutoXUnixAuth()) {
            keyboard.keyEvent(KeyEvent.KEYCODE_UNKNOWN, new KeyEvent(SystemClock.uptimeMillis(),
                    connection.getSshUser(), 0, 0));
            keyboard.keyEvent(KeyEvent.KEYCODE_ENTER, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
            keyboard.keyEvent(KeyEvent.KEYCODE_ENTER, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));

            keyboard.keyEvent(KeyEvent.KEYCODE_UNKNOWN, new KeyEvent(SystemClock.uptimeMillis(),
                    connection.getSshPassword(), 0, 0));
            keyboard.keyEvent(KeyEvent.KEYCODE_ENTER, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
            keyboard.keyEvent(KeyEvent.KEYCODE_ENTER, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
        }
    }

    /**
     * Retreives the requested remote width.
     */
    private int getRemoteWidth(int viewWidth, int viewHeight) {
        int remoteWidth = 0;
        int reqWidth = connection.getRdpWidth();
        int reqHeight = connection.getRdpHeight();
        if (connection.getRdpResType() == Constants.RDP_GEOM_SELECT_CUSTOM &&
                reqWidth >= 2 && reqHeight >= 2) {
            remoteWidth = reqWidth;
        } else if (connection.getRdpResType() == Constants.RDP_GEOM_SELECT_NATIVE_PORTRAIT) {
            remoteWidth = Math.min(viewWidth, viewHeight);
        } else if (connection.getRdpResType() == Constants.RDP_GEOM_SELECT_NATIVE_LANDSCAPE) {
            remoteWidth = Math.max(viewWidth, viewHeight);
        } else if (connection.getRdpResType() == Constants.RDP_GEOM_FULL_HD) {
            remoteWidth = 1920;
        } else {
            remoteWidth = viewWidth;
        }
        // We make the resolution even if it is odd.
        if (remoteWidth % 2 == 1) remoteWidth--;
        return remoteWidth;
    }

    /**
     * Retreives the requested remote height.
     */
    private int getRemoteHeight(int viewWidth, int viewHeight) {
        int remoteHeight = 0;
        int reqWidth = connection.getRdpWidth();
        int reqHeight = connection.getRdpHeight();
        if (connection.getRdpResType() == Constants.RDP_GEOM_SELECT_CUSTOM &&
                reqWidth >= 2 && reqHeight >= 2) {
            remoteHeight = reqHeight;
        } else if (connection.getRdpResType() == Constants.RDP_GEOM_SELECT_NATIVE_PORTRAIT) {
            remoteHeight = Math.max(viewWidth, viewHeight);
        } else if (connection.getRdpResType() == Constants.RDP_GEOM_SELECT_NATIVE_LANDSCAPE) {
            remoteHeight = Math.min(viewWidth, viewHeight);
        } else if (connection.getRdpResType() == Constants.RDP_GEOM_FULL_HD) {
            remoteHeight = 1080;
        } else {
            remoteHeight = viewHeight;
        }
        // We make the resolution even if it is odd.
        if (remoteHeight % 2 == 1) remoteHeight--;
        return remoteHeight;
    }

    /**
     * Determines the preferred remote width for VNC conncetions.
     */
    private int getVncRemoteWidth(int viewWidth, int viewHeight) {
        int remoteWidth = 0;
        int reqWidth = connection.getRdpWidth();
        int reqHeight = connection.getRdpHeight();
        if (connection.getRdpResType() == Constants.VNC_GEOM_SELECT_CUSTOM &&
                reqWidth >= 2 && reqHeight >= 2) {
            remoteWidth = reqWidth;
        } else if (connection.getRdpResType() == Constants.VNC_GEOM_SELECT_AUTOMATIC) {
            remoteWidth = viewWidth;
        } else if (connection.getRdpResType() == Constants.VNC_GEOM_SELECT_NATIVE_PORTRAIT) {
            remoteWidth = Math.min(viewWidth, viewHeight);
        } else if (connection.getRdpResType() == Constants.VNC_GEOM_SELECT_NATIVE_LANDSCAPE) {
            remoteWidth = Math.max(viewWidth, viewHeight);
        }
        // We make the resolution even if it is odd.
        if (remoteWidth % 2 == 1) remoteWidth--;
        return remoteWidth;
    }

    /**
     * Determines the preferred remote height for VNC conncetions.
     */
    private int getVncRemoteHeight(int viewWidth, int viewHeight) {
        int remoteHeight = 0;
        int reqWidth = connection.getRdpWidth();
        int reqHeight = connection.getRdpHeight();
        if (connection.getRdpResType() == Constants.VNC_GEOM_SELECT_CUSTOM &&
                reqWidth >= 2 && reqHeight >= 2) {
            remoteHeight = reqHeight;
        } else if (connection.getRdpResType() == Constants.VNC_GEOM_SELECT_AUTOMATIC) {
            remoteHeight = viewHeight;
        } else if (connection.getRdpResType() == Constants.VNC_GEOM_SELECT_NATIVE_PORTRAIT) {
            remoteHeight = Math.max(viewWidth, viewHeight);
        } else if (connection.getRdpResType() == Constants.VNC_GEOM_SELECT_NATIVE_LANDSCAPE) {
            remoteHeight = Math.min(viewWidth, viewHeight);
        }
        // We make the resolution even if it is odd.
        if (remoteHeight % 2 == 1) remoteHeight--;
        return remoteHeight;
    }

    void showMessage(final String error) {
        android.util.Log.d(TAG, "showMessage");
        screenMessage = error;
        handler.removeCallbacks(showDialogMessage);
        handler.post(showDialogMessage);
    }

    /**
     * Closes the connection and shows a fatal message which ends the activity.
     *
     * @param error
     */
    public void showFatalMessageAndQuit(final String error) {
        closeConnection();
        handler.post(new Runnable() {
            public void run() {
                Utils.showFatalErrorMessage(getContext(), error);
            }
        });
    }

    public void showToastAndReconnect(final String warn) {
        reConnect();
        handler.post(new Runnable() {
            public void run() {
                Toast.makeText(getContext(), warn, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * If necessary, initializes an SSH tunnel and returns local forwarded port, or
     * if SSH tunneling is not needed, returns the given port.
     *
     * @return
     */
    int getRemoteProtocolPort(int port) throws Exception {
        int result = 0;

        if (sshTunneled) {
            sshConnection = new SSHConnection(connection, getContext(), handler);
            int newPort = sshConnection.initializeSSHTunnel();
            if (newPort > 0)
                port = newPort;
            result = sshConnection.createLocalPortForward(port);
        } else {
            if (isVnc && port <= 20) {
                result = Constants.DEFAULT_VNC_PORT + port;
            } else {
                result = port;
            }
        }
        return result;
    }

    /**
     * Returns localhost if using SSH tunnel or saved VNC address.
     *
     * @return
     */
    String getAddress() {
        if (sshTunneled) {
            return new String("127.0.0.1");
        } else
            return connection.getAddress();
    }

    /**
     * Initializes the drawable and bitmap into which the remote desktop is drawn.
     *
     * @param dx
     * @param dy
     * @throws IOException
     */
    @Override
    public void reallocateDrawable(int dx, int dy) {
        Log.i(TAG, "Desktop name is " + rfbconn.desktopName());
        Log.i(TAG, "Desktop size is " + rfbconn.framebufferWidth() + " x " + rfbconn.framebufferHeight());

        int fbsize = rfbconn.framebufferWidth() * rfbconn.framebufferHeight();

        capacity = BCFactory.getInstance().getBCActivityManager().getMemoryClass(Utils.getActivityManager(getContext()));

        if (connection.getForceFull() == BitmapImplHint.AUTO) {
            if (fbsize * CompactBitmapData.CAPACITY_MULTIPLIER <= capacity * 1024 * 1024) {
                useFull = true;
                compact = true;
            } else if (fbsize * FullBufferBitmapData.CAPACITY_MULTIPLIER <= capacity * 1024 * 1024) {
                useFull = true;
            } else {
                useFull = false;
            }
        } else {
            useFull = (connection.getForceFull() == BitmapImplHint.FULL);
        }

        if (isRdp) {
            myDrawable = new UltraCompactBitmapData(rfbconn, this, isSpice | isOpaque | isRdp);
            android.util.Log.i(TAG, "Using UltraCompactBufferBitmapData.");
        } else if (!useFull) {
            myDrawable = new LargeBitmapData(rfbconn, this, dx, dy, capacity);
            android.util.Log.i(TAG, "Using LargeBitmapData.");
        } else {
            try {
                // TODO: Remove this if Android 4.2 receives a fix for a bug which causes it to stop drawing
                // the bitmap in CompactBitmapData when under load (say playing a video over VNC).
                if (!compact) {
                    myDrawable = new FullBufferBitmapData(rfbconn, this, capacity);
                    android.util.Log.i(TAG, "Using FullBufferBitmapData.");
                } else {
                    myDrawable = new CompactBitmapData(rfbconn, this, isSpice | isOpaque);
                    android.util.Log.i(TAG, "Using CompactBufferBitmapData.");
                }
            } catch (Throwable e) { // If despite our efforts we fail to allocate memory, use LBBM.
                disposeDrawable();

                useFull = false;
                myDrawable = new LargeBitmapData(rfbconn, this, dx, dy, capacity);
                android.util.Log.i(TAG, "Using LargeBitmapData.");
            }
        }

        try {
            if (needsLocalCursor()) {
                initializeSoftCursor();
            }
            handler.post(drawableSetter);
            handler.post(setModes);
//            myDrawable.syncScroll();
            if (decoder != null) {
                decoder.setBitmapData(myDrawable);
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    /**
     * Determines if the app should show a local cursor or not
     */
    private boolean needsLocalCursor() {
        boolean isRdpSpiceOrOpaque = isRdp || isSpice || isOpaque;
        boolean localCursorNotForceDisabled =
                connection.getUseLocalCursor() != Constants.CURSOR_FORCE_DISABLE;
        boolean localCursorForceEnabled =
                connection.getUseLocalCursor() == Constants.CURSOR_FORCE_LOCAL;
        return (isRdpSpiceOrOpaque && localCursorNotForceDisabled) || localCursorForceEnabled;
    }

    /**
     * Disposes of the old drawable which holds the remote desktop data.
     */
    private void disposeDrawable() {
        if (myDrawable != null)
            myDrawable.dispose();
        myDrawable = null;
//        System.gc();
    }

    /**
     * The remote desktop's size has changed and this method
     * reinitializes local data structures to match.
     */
    public void updateFBSize() {
        try {
            myDrawable.frameBufferSizeChanged();
        } catch (Throwable e) {
            boolean useLBBM = false;

            // If we've run out of memory, try using another bitmapdata type.
            if (e instanceof OutOfMemoryError) {
                disposeDrawable();

                // If we were using CompactBitmapData, try FullBufferBitmapData.
                if (compact == true) {
                    compact = false;
                    try {
                        myDrawable = new FullBufferBitmapData(rfbconn, this, capacity);
                    } catch (Throwable e2) {
                        useLBBM = true;
                    }
                } else
                    useLBBM = true;

                // Failing FullBufferBitmapData or if we weren't using CompactBitmapData, try LBBM.
                if (useLBBM) {
                    disposeDrawable();

                    useFull = false;
                    myDrawable = new LargeBitmapData(rfbconn, this, getWidth(), getHeight(), capacity);
                }
                if (decoder != null) {
                    decoder.setBitmapData(myDrawable);
                }
            }
        }
        handler.post(drawableSetter);
        handler.post(setModes);
        myDrawable.syncScroll();
    }

    /**
     * Displays a short toast message on the screen.
     *
     * @param message
     */
    public void displayShortToastMessage(final CharSequence message) {
        screenMessage = message;
        handler.removeCallbacks(showMessage);
        handler.post(showMessage);
    }

    /**
     * Displays a short toast message on the screen.
     *
     * @param messageID
     */
    public void displayShortToastMessage(final int messageID) {
        screenMessage = getResources().getText(messageID);
        handler.removeCallbacks(showMessage);
        handler.post(showMessage);
    }

    /**
     * Lets the drawable know that an update from the remote server has arrived.
     */
    public void doneWaiting() {
        myDrawable.doneWaiting();
    }

    /**
     * Indicates that RemoteCanvas's scroll position should be synchronized with the
     * drawable's scroll position (used only in LargeBitmapData)
     */
    public void syncScroll() {
        myDrawable.syncScroll();
    }

    /*
     * f(x,s) is a function that returns the coordinate in screen/scroll space corresponding
     * to the coordinate x in full-frame space with scaling s.
     *
     * This function returns the difference between f(x,s1) and f(x,s2)
     *
     * f(x,s) = (x - i/2) * s + ((i - w)/2)) * s
     *        = s (x - i/2 + i/2 + w/2)
     *        = s (x + w/2)
     *
     *
     * f(x,s) = (x - ((i - w)/2)) * s
     * @param oldscaling
     * @param scaling
     * @param imageDim
     * @param windowDim
     * @param offset
     * @return
     */

    /**
     * Requests a remote desktop update at the specified rectangle.
     */
    public void writeFramebufferUpdateRequest(int x, int y, int w, int h, boolean incremental) throws IOException {
        myDrawable.prepareFullUpdateRequest(incremental);
        rfbconn.writeFramebufferUpdateRequest(x, y, w, h, incremental);
    }

    /**
     * Requests an update of the entire remote desktop.
     */
    public void writeFullUpdateRequest(boolean incremental) {
        myDrawable.prepareFullUpdateRequest(incremental);
        rfbconn.writeFramebufferUpdateRequest(myDrawable.getXoffset(), myDrawable.getYoffset(),
                myDrawable.bmWidth(), myDrawable.bmHeight(), incremental);
    }

    /**
     * Set the device clipboard text with the string parameter.
     */
    public void setClipboardText(String s) {
        if (s != null && s.length() > 0) {
            clipboard.setText(s);
        }
    }

    /**
     * Method that disconnects from the remote server.
     */
    public void closeConnection() {
        maintainConnection = false;

        if (keyboard != null) {
            // Tell the server to release any meta keys.
            keyboard.clearMetaState();
            keyboard.keyEvent(0, new KeyEvent(KeyEvent.ACTION_UP, 0));
        }
        // Close the rfb connection.
        if (rfbconn != null) {
            rfbconn.close();
        }

        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }

        // Close the SSH tunnel.
        if (sshConnection != null) {
            sshConnection.terminateSSHTunnel();
            sshConnection = null;
        }

        if (connection != null) {
            Log.d(TAG, "Saving screenshot to " + getContext().getFilesDir() + "/" + connection.getScreenshotFilename());
            Utils.writeScreenshotToFile(myDrawable, getContext().getFilesDir() + "/" + connection.getScreenshotFilename(), 720);
        }

//        disposeDrawable();
        onDestroy();
    }

    public void reConnect() {
        rfbconn.reconnect();
    }

    /**
     * Cleans up resources after a disconnection.
     */
    public void onDestroy() {
        Log.v(TAG, "Cleaning up resources");

        removeCallbacksAndMessages();
        if (clipboardMonitorTimer != null) {
            clipboardMonitorTimer.cancel();
            // Occasionally causes a NullPointerException
            //clipboardMonitorTimer.purge();
            clipboardMonitorTimer = null;
        }
        clipboardMonitor = null;
        clipboard = null;
        setModes = null;
        decoder = null;
        canvasZoomer = null;
        drawableSetter = null;
        screenMessage = null;
        desktopInfo = null;

        disposeDrawable();
    }

    public void removeCallbacksAndMessages() {
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }

    /**
     * Computes the X and Y offset for converting coordinates from full-frame coordinates to view coordinates.
     */
    public void computeShiftFromFullToView() {
        shiftX = (rfbconn.framebufferWidth() - getWidth()) / 2;
        shiftY = (rfbconn.framebufferHeight() - getHeight()) / 2;
    }

    /**
     * Change to Canvas's scroll position to match the absoluteXPosition
     */
    void resetScroll() {
        float scale = getZoomFactor();
        //android.util.Log.d(TAG, "resetScroll: " + (absoluteXPosition - shiftX) * scale + ", "
        //                                        + (absoluteYPosition - shiftY) * scale);
        scrollTo((int) ((absoluteXPosition) * scale),
                (int) ((absoluteYPosition) * scale));
    }

    /**
     * Make sure mouse is visible on displayable part of screen
     */
    public void movePanToMakePointerVisible() {
        //Log.d(TAG, "movePanToMakePointerVisible");
        if (rfbconn == null)
            return;

        // Do not pan when not scaled.
//        if (getZoomFactor() == getMinimumScale()) {
//            return;
//        }

        boolean panX = true;
        boolean panY = true;

        boolean panned = false;

        // We only pan if the current scaling is able to pan.
        if (canvasZoomer != null && !canvasZoomer.isAbleToPan())
            return;

        // Coordinates in screen's resolution
        int x = (int) (pointer.getX());
        int y = (int) (pointer.getY());
        int wthresh = (int) (Constants.H_THRESH / getZoomFactor());
        int hthresh = (int) (Constants.W_THRESH / getZoomFactor());

        // Coordinates in picture's resolution
        int w = getVisibleDesktopWidth();
        int h = getVisibleDesktopHeight();
        int iw = getImageWidth();
        int ih = getImageHeight();

        // bWidth = black border width
        int bWidth = (int) ((getWidth() - iw * getZoomFactor()) / 2);
        if (bWidth <= 0) {
            bWidth = 0;
        }

        // This is definitely 0 because we have the image top aligned
        int bHeight = 0;

        // newX and newY are in screen's resolution
        int newX = absoluteXPosition;
        int newY = absoluteYPosition;

        if ((x + bWidth > w + absoluteXPosition - wthresh) && (absoluteXPosition + w < bWidth + iw)) {
            newX += (x - (w + absoluteXPosition - wthresh - bWidth));

            // left padding x + image_width visible is larger than left black border + image_width
            // which means the right side has been reached, no more space to do right panning now
            if (newX + w > iw + bWidth) {
                newX = iw - w + bWidth;
            }
        } else if ((x + bWidth < absoluteXPosition + wthresh) && (x > 0)) {
            newX += (x - (absoluteXPosition + wthresh - bWidth));
            if (newX < bWidth)
                newX = bWidth;
        }

        if (panX && newX != absoluteXPosition) {
            absoluteXPosition = newX;
            panned = true;
        }

        // do not pan when there is space left on the screen
        if ((y + bHeight > h + absoluteYPosition - hthresh) && (absoluteYPosition + h < bHeight + ih)) {
            newY += ((y) - (h + absoluteYPosition - hthresh) - bHeight);
            if (newY + h > ih + bHeight)
                newY = ih + bHeight - h;
        } else if ((y + bHeight < absoluteYPosition + hthresh) && y > 0) {
            newY += (y - (absoluteYPosition + hthresh - bHeight));
            if (newY < bHeight)
                newY = bHeight;
        }

        if (panY && newY != absoluteYPosition) {
            absoluteYPosition = newY;
            panned = true;
        }

        if (panned) {
            //scrollBy(newX - absoluteXPosition, newY - absoluteYPosition);
            resetScroll();
        }
    }

    public int getTopMargin(double scale) {
        return (int) (Constants.TOP_MARGIN / scale);
    }

    public int getBottomMargin(double scale) {
        return (int) (Constants.BOTTOM_MARGIN / scale);
    }

    /**
     * Pan by a number of pixels (relative pan)
     *
     * @param dX
     * @param dY
     * @return True if the pan changed the view (did not move view out of bounds); false otherwise
     */
    public boolean relativePan(float dX, float dY) {
        android.util.Log.d(TAG, "relativePan: " + dX + ", " + dY);

        // We only pan if the current scaling is able to pan.
        if (canvasZoomer != null && !canvasZoomer.isAbleToPan())
            return false;

        double sX = dX;
        double sY = dY;

        // Prevent panning right or below desktop image except for provision for on-screen
        // buttons and curved screens
//        if (absoluteXPosition + getVisibleDesktopWidth() + sX > getImageWidth())
//            sX = getImageWidth() - getVisibleDesktopWidth() - absoluteXPosition;
//        if (absoluteYPosition + getVisibleDesktopHeight() + sY > getImageHeight() + buttonAndCurveOffset)
//            sY = getImageHeight() - getVisibleDesktopHeight() - absoluteYPosition + buttonAndCurveOffset;

        absoluteXPosition += sX;
        absoluteYPosition += sY;
        if (sX != 0.0 || sY != 0.0) {
            //scrollBy((int)sX, (int)sY);
            resetScroll();
            return true;
        }
        return false;
    }

    /**
     * Absolute pan.
     *
     * @param x
     * @param y
     */
    public void absolutePan(int x, int y, boolean force) {
        //android.util.Log.d(TAG, "absolutePan: " + x + ", " + y);

        if (canvasZoomer != null) {
            int vW = getVisibleDesktopWidth();
            int vH = getVisibleDesktopHeight();
            int w = getImageWidth();
            int h = getImageHeight();

            if (!force) {
                if (x + vW > w) x = w - vW;
                if (y + vH > h) y = h - vH;
                if (x < 0) x = 0;
                if (y < 0) y = 0;
            }

            absoluteXPosition = x;
            absoluteYPosition = y;
            resetScroll();
        }
    }

    /* (non-Javadoc)
     * @see android.view.View#onScrollChanged(int, int, int, int)
     */
    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (myDrawable != null) {
            myDrawable.scrollChanged(absoluteXPosition, absoluteYPosition);
        }
    }

    @Override
    public Bitmap getBitmap() {
        Bitmap bitmap = null;
        if (myDrawable != null) {
            bitmap = myDrawable.mbitmap;
        }
        return bitmap;
    }

    /**
     * Causes a redraw of the myDrawable to happen at the indicated coordinates.
     */
    public void reDraw(int x, int y, int w, int h) {
        if (System.currentTimeMillis() - lastDraw < 8) {
            return;
        }

        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
            progressDialog = null; // no use any more
        }

        //android.util.Log.i(TAG, "reDraw called: " + x +", " + y + " + " + w + "x" + h);
        float scale = getZoomFactor();
        float shiftedX = x - shiftX;
        float shiftedY = y - shiftY;

        postInvalidate((int) ((shiftedX) * scale), (int) ((shiftedY) * scale),
                (int) ((shiftedX + w) * scale), (int) ((shiftedY + h) * scale));

        lastDraw = System.currentTimeMillis();
    }

    /**
     * This is a float-accepting version of reDraw().
     * Causes a redraw of the myDrawable to happen at the indicated coordinates.
     */
    public void reDraw(float x, float y, float w, float h) {
        reDraw((int)x, (int)y, (int)w, (int)h);
    }

    /**
     * Displays connection info in a toast message.
     */
    public void showConnectionInfo() {
        if (rfbconn == null)
            return;

        String msg = null;
        int idx = rfbconn.desktopName().indexOf("(");
        if (idx > 0) {
            // Breakup actual desktop name from IP addresses for improved
            // readability
            String dn = rfbconn.desktopName().substring(0, idx).trim();
            String ip = rfbconn.desktopName().substring(idx).trim();
            msg = dn + "\n" + ip;
        } else
            msg = rfbconn.desktopName();
        msg += "\n" + rfbconn.framebufferWidth() + "x" + rfbconn.framebufferHeight();
        String enc = rfbconn.getEncoding();
        // Encoding might not be set when we display this message
        if (decoder != null && decoder.getColorModel() != null) {
            if (enc != null && !enc.equals("")) {
                msg += ", " + rfbconn.getEncoding() + getContext().getString(R.string.info_encoding) + decoder.getColorModel().toString();
            }
            msg += ", " + decoder.getColorModel().toString();
        }
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }

    /**
     * Invalidates (to redraw) the location of the remote pointer.
     */
    public void invalidateMousePosition() {
        if (myDrawable != null) {
            // add little offset for the cursor image
            myDrawable.moveCursorRect(pointer.getX() - pointer.getHotspotX(), pointer.getY() - pointer.getHotspotY());
            RectF r = myDrawable.getCursorRect();
            reDraw(r.left , r.top , r.width(), r.height());
        }
    }

    @Override
    public void setMousePointerPosition(int x, int y) {
        softCursorMove(x, y);
    }

    @Override
    public void mouseMode(boolean relative) {
        if (relative && !connection.getInputMode().equals(InputHandlerTouchpad.ID)) {
            showMessage(getContext().getString(R.string.info_set_touchpad_input_mode));
        } else {
            this.pointer.setRelativeEvents(relative);
        }
    }

    @Override
    public boolean isAbleToPan() {
        return canvasZoomer.isAbleToPan();
    }

    @Override
    public void onConnectionSuccess() {
        handler.post(() -> {
            TextView messageView = progressDialog.findViewById(R.id.message);
            messageView.setText(R.string.info_continue_connected);
        });
    }

    /**
     * Moves soft cursor into a particular location.
     *
     * @param x
     * @param y
     */
    synchronized void softCursorMove(int x, int y) {
        if (myDrawable.isNotInitSoftCursor() && connection.getUseLocalCursor() != Constants.CURSOR_FORCE_DISABLE) {
            initializeSoftCursor();
        }

        if (!cursorBeingMoved || pointer.isRelativeEvents()) {
            pointer.setX(x);
            pointer.setY(y);
            RectF prevR = new RectF(myDrawable.getCursorRect());
            // Move the cursor.
            myDrawable.moveCursorRect(x, y);
            // Show the cursor.
//            RectF r = myDrawable.getCursorRect();
//            reDraw(r.left, r.top, r.width(), r.height());
//            reDraw(prevR.left, prevR.top, prevR.width(), prevR.height());
        }
    }

    /**
     * Initializes the data structure which holds the remote pointer data.
     */
    void initializeSoftCursor() {
    }

    public void setSoftCursorPixels(int[] pixels, int width, int height, int xPos, int yPos) {
        myDrawable.setCursorRect(pointer.getX() - xPos, pointer.getY() - yPos, width, height, 0, 0);
        myDrawable.setSoftCursor(pixels);

        pointer.setHotspotX(xPos);
        pointer.setHotspotY(yPos);
    }

    public void setSoftCursorBitmap(Bitmap bitmap, int width, int height, int xPos, int yPos) {
        myDrawable.setCursorRect(pointer.getX() - xPos, pointer.getY() - yPos, width, height, 0, 0);
        myDrawable.setSoftCursor(bitmap);

        pointer.setHotspotX(xPos);
        pointer.setHotspotY(yPos);
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        android.util.Log.d(TAG, "onCreateInputConnection called");
        BaseInputConnection bic = new BaseInputConnection(this, false);
        outAttrs.actionLabel = null;
        outAttrs.inputType = getKeyboardVariation();
        String currentIme = Settings.Secure.getString(getContext().getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
        android.util.Log.d(TAG, "currentIme: " + currentIme);
        outAttrs.imeOptions |= EditorInfo.IME_FLAG_NO_FULLSCREEN;
        return bic;
    }

    private int getKeyboardVariation() {
        String keyboardVariationStr = Utils.querySharedPreferenceString(
                getContext(),
                Constants.softwareKeyboardType,
                getContext().getString(R.string.pref_keyboard_type_TYPE_NULL_value)
        );
        int keyboardVariation = 0;
        try {
            keyboardVariation = Integer.parseInt(keyboardVariationStr);
        } catch (NumberFormatException e) {
            Log.e(TAG, e.toString());
        }
        return keyboardVariation;
    }

    public RemotePointer getPointer() {
        return pointer;
    }

    public RemoteKeyboard getKeyboard() {
        return keyboard;
    }

    public float getZoomFactor() {
        if (canvasZoomer == null)
            return 1;
        return canvasZoomer.getZoomFactor();
    }

    public int getVisibleDesktopWidth() {
        return (int) ((double) getWidth() / getZoomFactor());
    }

    public int getVisibleDesktopHeight() {
        if (visibleHeight > 0)
            return (int) ((double) visibleHeight / getZoomFactor());
        else
            return (int) ((double) getHeight() / getZoomFactor());
    }

    public int getImageVisibleInScreenHeight() {
        return (int) ((getImageHeight() - getAbsY()) * getZoomFactor());
    }

    public void setVisibleDesktopHeight(int newHeight) {
        visibleHeight = newHeight;
    }

    public int getImageWidth() {
        return rfbconn.framebufferWidth();
    }

    public int getImageHeight() {
        return rfbconn.framebufferHeight();
    }

    public int getCenteredXOffset() {
        return (int)(rfbconn.framebufferWidth() * getMinimumScale() - getWidth()) / 2;
    }

    public int getCenteredYOffset() {
        return (int)(rfbconn.framebufferHeight() * getMinimumScale() - getHeight()) / 2;
    }

    public float getMinimumScale() {
        if (myDrawable != null) {
            return myDrawable.getMinimumScale();
        } else
            return 1.f;
    }

    public float getDisplayDensity() {
        return displayDensity;
    }

    public boolean isColorModel(COLORMODEL cm) {
        if (isVnc && decoder != null) {
            return (decoder.getColorModel() != null) && decoder.getColorModel().equals(cm);
        } else {
            return false;
        }
    }

    public void setColorModel(COLORMODEL cm) {
        if (isVnc && decoder != null) {
            decoder.setColorModel(cm);
        }
    }

    public boolean getMouseFollowPan() {
        return connection.getFollowPan();
    }

    public int getAbsX() {
        return absoluteXPosition;
    }

    public int getAbsY() {
        return absoluteYPosition;
    }

    /**
     * Used to wait until getWidth and getHeight return sane values.
     */
    public void waitUntilInflated() {
        synchronized (this) {
            while (getWidth() == 0 || getHeight() == 0) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public PointerIcon onResolvePointerIcon(MotionEvent event, int pointerIndex) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !outDisplay) {
            return PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_NULL);
        }

        return super.onResolvePointerIcon(event, pointerIndex);
    }

    /**
     * Used to detect when the view is inflated to a sane size other than 0x0.
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (w > 0 && h > 0) {
            synchronized (this) {
                this.notify();
            }
        }
    }

    /**
     * Function used to initialize an empty SSH HostKey for a new VNC over SSH connection.
     */
    public String retrievevvFileName() {
        return this.vvFileName;
    }

    @Override
    public void onTextObtained(String dialogId, String[] obtainedString, boolean dialogCancelled, boolean save) {
        if (dialogCancelled) {
            handler.sendEmptyMessage(RemoteClientLibConstants.DISCONNECT_NO_MESSAGE);
            return;
        }

        switch (dialogId) {
            case GetTextFragment.DIALOG_ID_GET_VNC_CREDENTIALS:
                android.util.Log.i(TAG, "Text obtained from DIALOG_ID_GET_VNC_USERNAME.");
                connection.setUserName(obtainedString[0]);
                connection.setPassword(obtainedString[1]);
                connection.setKeepPassword(save);
                connection.save(getContext());
                handler.sendEmptyMessage(RemoteClientLibConstants.REINIT_SESSION);
                break;
            case GetTextFragment.DIALOG_ID_GET_VNC_PASSWORD:
                android.util.Log.i(TAG, "Text obtained from DIALOG_ID_GET_VNC_PASSWORD.");
                connection.setPassword(obtainedString[0]);
                connection.setKeepPassword(save);
                connection.save(getContext());
                handler.sendEmptyMessage(RemoteClientLibConstants.REINIT_SESSION);
                break;
            case GetTextFragment.DIALOG_ID_GET_RDP_CREDENTIALS:
                android.util.Log.i(TAG, "Text obtained from DIALOG_ID_GET_VNC_PASSWORD.");
                connection.setUserName(obtainedString[0]);
                connection.setRdpDomain(obtainedString[1]);
                connection.setPassword(obtainedString[2]);
                connection.setKeepPassword(save);
                connection.save(getContext());
                handler.sendEmptyMessage(RemoteClientLibConstants.REINIT_SESSION);
                break;
            case GetTextFragment.DIALOG_ID_GET_SPICE_PASSWORD:
                android.util.Log.i(TAG, "Text obtained from DIALOG_ID_GET_SPICE_PASSWORD.");
                connection.setPassword(obtainedString[0]);
                connection.setKeepPassword(save);
                connection.save(getContext());
                handler.sendEmptyMessage(RemoteClientLibConstants.REINIT_SESSION);
                break;
            case GetTextFragment.DIALOG_ID_GET_OPAQUE_CREDENTIALS:
                android.util.Log.i(TAG, "Text obtained from DIALOG_ID_GET_OPAQUE_CREDENTIALS");
                connection.setUserName(obtainedString[0]);
                connection.setPassword(obtainedString[1]);
                connection.setKeepPassword(save);
                connection.save(getContext());
                handler.sendEmptyMessage(RemoteClientLibConstants.REINIT_SESSION);
                break;
            case GetTextFragment.DIALOG_ID_GET_OPAQUE_PASSWORD:
                android.util.Log.i(TAG, "Text obtained from DIALOG_ID_GET_OPAQUE_PASSWORD");
                connection.setPassword(obtainedString[0]);
                connection.setKeepPassword(save);
                connection.save(getContext());
                synchronized (spicecomm) {
                    spicecomm.notify();
                }
                break;
            case GetTextFragment.DIALOG_ID_GET_OPAQUE_OTP_CODE:
                android.util.Log.i(TAG, "Text obtained from DIALOG_ID_GET_OPAQUE_OTP_CODE");
                connection.setOtpCode(obtainedString[0]);
                synchronized (spicecomm) {
                    spicecomm.notify();
                }
                break;
            default:
                android.util.Log.e(TAG, "Unknown dialog type.");
                break;
        }
    }

    public void setInputHandler(InputHandler inputHandler) {
        this.inputHandler = inputHandler;
    }

    public void setOutDisplay(boolean outDisplay) {
        this.outDisplay = outDisplay;
    }

    public boolean isOutDisplay() {
        return this.outDisplay;
    }
}
