/**
 * Copyright (C) 2012 Iordan Iordanov
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

package com.qihua.bVNC;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.qihua.bVNC.gesture.GestureEditorActivity;
import com.qihua.util.PermissionGroups;
import com.qihua.util.PermissionsManager;
import com.morpheusly.common.Utilities;
import com.undatech.remoteClientUi.R;

import java.util.List;

/**
 * aRDP is the Activity for setting up RDP connections.
 */
public class aRDP extends MainConfiguration {
    private final static String TAG = "aRDP";
    private LinearLayout layoutAdvancedSettings;
    private EditText sshServer;
    private EditText sshPort;
    private EditText sshUser;
    private EditText portText;
    private EditText passwordText;
    private ToggleButton toggleAdvancedSettings;
    //private Spinner colorSpinner;
    private Spinner spinnerRdpGeometry;
    private EditText textUsername;
    private EditText rdpDomain;
    private EditText rdpWidth;
    private EditText rdpHeight;
    private CheckBox checkboxKeepPassword;
    private CheckBox checkboxUseDpadAsArrows;
    private RadioGroup groupRemoteSoundType;
    private CheckBox checkboxEnableRecording;
    private CheckBox checkboxEnableGesture;
    private CheckBox checkboxConsoleMode;
//    private CheckBox checkboxRedirectSdCard;
    private CheckBox checkboxRemoteFx;
    private CheckBox checkboxDesktopBackground;
    private CheckBox checkboxFontSmoothing;
    private CheckBox checkboxDesktopComposition;
    private CheckBox checkboxWindowContents;
    private CheckBox checkboxMenuAnimation;
    private CheckBox checkboxVisualStyles;
    private CheckBox checkboxRotateDpad;
    private CheckBox checkboxUseLastPositionToolbar;
    private CheckBox checkboxUseSshPubkey;
    private CheckBox checkboxEnableGfx;
    private CheckBox checkboxEnableGfxH264;
    private CheckBox checkboxPreferSendingUnicode;
    private Spinner spinnerRdpColor;
    private List<String> rdpColorArray;

    @Override
    public void onCreate(Bundle icicle) {
        layoutID = R.layout.main_rdp;

        super.onCreate(icicle);

        sshServer = (EditText) findViewById(R.id.sshServer);
        sshPort = (EditText) findViewById(R.id.sshPort);
        sshUser = (EditText) findViewById(R.id.sshUser);
        portText = (EditText) findViewById(R.id.textPORT);
        passwordText = (EditText) findViewById(R.id.textPASSWORD);
        textUsername = (EditText) findViewById(R.id.textUsername);
        rdpDomain = (EditText) findViewById(R.id.rdpDomain);

        // Here we say what happens when the Pubkey Checkbox is checked/unchecked.
        checkboxUseSshPubkey = (CheckBox) findViewById(R.id.checkboxUseSshPubkey);

        checkboxKeepPassword = (CheckBox) findViewById(R.id.checkboxKeepPassword);
        checkboxUseDpadAsArrows = (CheckBox) findViewById(R.id.checkboxUseDpadAsArrows);
        checkboxRotateDpad = (CheckBox) findViewById(R.id.checkboxRotateDpad);
        checkboxUseLastPositionToolbar = (CheckBox) findViewById(R.id.checkboxUseLastPositionToolbar);
        // The advanced settings button.
        toggleAdvancedSettings = (ToggleButton) findViewById(R.id.toggleAdvancedSettings);
        layoutAdvancedSettings = (LinearLayout) findViewById(R.id.layoutAdvancedSettings);
        toggleAdvancedSettings.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton arg0, boolean checked) {
                if (checked)
                    layoutAdvancedSettings.setVisibility(View.VISIBLE);
                else
                    layoutAdvancedSettings.setVisibility(View.GONE);
            }
        });

        rdpColorArray = Utilities.Companion.toList(getResources().getStringArray(R.array.rdp_colors));
        spinnerRdpColor = (Spinner) findViewById(R.id.spinnerRdpColor);
        spinnerRdpColor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View view, int itemIndex, long id) {
                selected.setRdpColor(Integer.parseInt(rdpColorArray.get(itemIndex)));
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        // The geometry type and dimensions boxes.
        spinnerRdpGeometry = (Spinner) findViewById(R.id.spinnerRdpGeometry);
        rdpWidth = (EditText) findViewById(R.id.rdpWidth);
        rdpHeight = (EditText) findViewById(R.id.rdpHeight);
        spinnerRdpGeometry.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View view, int itemIndex, long id) {
                selected.setRdpResType(itemIndex);
                setRemoteWidthAndHeight();
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        connectionType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> ad, View view, int itemIndex, long id) {
                selectedConnType = itemIndex;
                selected.setConnectionType(selectedConnType);
//                selected.save(MainConfiguration.this);
//                updateViewFromSelected();

                if (selectedConnType == Constants.CONN_TYPE_RDP) {
                    setVisibilityOfSshWidgets(View.GONE);
                    rdpDomain.setVisibility(View.VISIBLE);
                    findViewById(R.id.geometryGroup).setVisibility(View.VISIBLE);
                    findViewById(R.id.checkboxEnableRecording).setVisibility(View.VISIBLE);
                    findViewById(R.id.textDescriptGeom).setVisibility(View.VISIBLE);
                } else if (selectedConnType == Constants.CONN_TYPE_VNC) {
                    rdpDomain.setVisibility(View.GONE);
                    findViewById(R.id.geometryGroup).setVisibility(View.GONE);
                    findViewById(R.id.checkboxEnableRecording).setVisibility(View.GONE);
                    findViewById(R.id.textDescriptGeom).setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> ad) {
            }
        });

        groupRemoteSoundType = (RadioGroup) findViewById(R.id.groupRemoteSoundType);
        checkboxEnableRecording = (CheckBox) findViewById(R.id.checkboxEnableRecording);
        checkboxConsoleMode = (CheckBox) findViewById(R.id.checkboxConsoleMode);
//        checkboxRedirectSdCard = (CheckBox) findViewById(R.id.checkboxRedirectSdCard);
        checkboxEnableGesture = (CheckBox) findViewById(R.id.checkboxEnableGesture);
        checkboxRemoteFx = (CheckBox) findViewById(R.id.checkboxRemoteFx);
        checkboxDesktopBackground = (CheckBox) findViewById(R.id.checkboxDesktopBackground);
        checkboxFontSmoothing = (CheckBox) findViewById(R.id.checkboxFontSmoothing);
        checkboxDesktopComposition = (CheckBox) findViewById(R.id.checkboxDesktopComposition);
        checkboxWindowContents = (CheckBox) findViewById(R.id.checkboxWindowContents);
        checkboxMenuAnimation = (CheckBox) findViewById(R.id.checkboxMenuAnimation);
        checkboxVisualStyles = (CheckBox) findViewById(R.id.checkboxVisualStyles);
        checkboxEnableGfx = (CheckBox) findViewById(R.id.checkboxEnableGfx);
        checkboxEnableGfxH264 = (CheckBox) findViewById(R.id.checkboxEnableGfxH264);
        checkboxPreferSendingUnicode = (CheckBox) findViewById(R.id.checkboxPreferSendingUnicode);
        setConnectionTypeSpinnerAdapter(R.array.rdp_connection_type);
    }

    /**
     * Enables and disables the EditText boxes for width and height of remote desktop.
     */
    private void setRemoteWidthAndHeight() {
        if (selected.getRdpResType() != Constants.RDP_GEOM_SELECT_CUSTOM) {
            rdpWidth.setEnabled(false);
            rdpHeight.setEnabled(false);
        } else {
            rdpWidth.setEnabled(true);
            rdpHeight.setEnabled(true);
        }
    }

    protected void updateViewFromSelected() {
        if (selected == null)
            return;
        super.commonUpdateViewFromSelected();

        sshServer.setText(selected.getSshServer());
        sshPort.setText(Integer.toString(selected.getSshPort()));
        sshUser.setText(selected.getSshUser());

        checkboxUseSshPubkey.setChecked(selected.getUseSshPubKey());

        portText.setText(Integer.toString(selected.getPort()));

        if (selected.getKeepPassword() || selected.getPassword().length() > 0) {
            passwordText.setText(selected.getPassword());
        }

        checkboxKeepPassword.setChecked(selected.getKeepPassword());
        checkboxUseDpadAsArrows.setChecked(selected.getUseDpadAsArrows());
        checkboxRotateDpad.setChecked(selected.getRotateDpad());
        checkboxUseLastPositionToolbar.setChecked((!isNewConnection) ? selected.getUseLastPositionToolbar() : this.useLastPositionToolbarDefault());
        textNickname.setText(selected.getNickname());
        textUsername.setText(selected.getUserName());
        rdpDomain.setText(selected.getRdpDomain());
        spinnerRdpColor.setSelection(rdpColorArray.indexOf(String.valueOf(selected.getRdpColor())));
        spinnerRdpGeometry.setSelection(selected.getRdpResType());
        rdpWidth.setText(Integer.toString(selected.getRdpWidth()));
        rdpHeight.setText(Integer.toString(selected.getRdpHeight()));
        setRemoteWidthAndHeight();
        setRemoteSoundTypeFromSettings(selected.getRemoteSoundType());
        checkboxEnableRecording.setChecked(selected.getEnableRecording());
        checkboxEnableGesture.setChecked(selected.getEnableGesture());
        checkboxConsoleMode.setChecked(selected.getConsoleMode());
//        checkboxRedirectSdCard.setChecked(selected.getRedirectSdCard());
        checkboxRemoteFx.setChecked(selected.getRemoteFx());
        checkboxDesktopBackground.setChecked(selected.getDesktopBackground());
        checkboxFontSmoothing.setChecked(selected.getFontSmoothing());
        checkboxDesktopComposition.setChecked(selected.getDesktopComposition());
        checkboxWindowContents.setChecked(selected.getWindowContents());
        checkboxMenuAnimation.setChecked(selected.getMenuAnimation());
        checkboxVisualStyles.setChecked(selected.getVisualStyles());
        checkboxEnableGfx.setChecked(selected.getEnableGfx());
        checkboxEnableGfxH264.setChecked(selected.getEnableGfxH264());
        checkboxPreferSendingUnicode.setChecked(selected.getPreferSendingUnicode());

        /* TODO: Reinstate color spinner but for RDP settings.
        colorSpinner = (Spinner)findViewById(R.id.colorformat);
        COLORMODEL[] models=COLORMODEL.values();
        ArrayAdapter<COLORMODEL> colorSpinnerAdapter = new ArrayAdapter<COLORMODEL>(this, R.layout.connection_list_entry, models);
        colorSpinner.setAdapter(colorSpinnerAdapter);
        colorSpinner.setSelection(0);
        COLORMODEL cm = COLORMODEL.valueOf(selected.getColorModel());
        COLORMODEL[] colors=COLORMODEL.values();
        for (int i=0; i<colors.length; ++i)
        {
            if (colors[i] == cm) {
                colorSpinner.setSelection(i);
                break;
            }
        }*/
    }

    protected void updateSelectedFromView() {
        commonUpdateSelectedFromView();

        if (selected == null) {
            return;
        }
        try {
            selected.setPort(Integer.parseInt(portText.getText().toString()));
            selected.setSshPort(Integer.parseInt(sshPort.getText().toString()));
        } catch (NumberFormatException nfe) {
        }

        selected.setNickname(textNickname.getText().toString());
        selected.setSshServer(sshServer.getText().toString());
        selected.setSshUser(sshUser.getText().toString());

        // If we are using an SSH key, then the ssh password box is used
        // for the key pass-phrase instead.
        selected.setUseSshPubKey(checkboxUseSshPubkey.isChecked());
        selected.setUserName(textUsername.getText().toString());
        selected.setRdpDomain(rdpDomain.getText().toString());
        selected.setRdpResType(spinnerRdpGeometry.getSelectedItemPosition());
        try {
            selected.setRdpWidth(Integer.parseInt(rdpWidth.getText().toString()));
            selected.setRdpHeight(Integer.parseInt(rdpHeight.getText().toString()));
        } catch (NumberFormatException nfe) {
        }
        setRemoteSoundTypeFromView(groupRemoteSoundType);
        selected.setEnableRecording(checkboxEnableRecording.isChecked());
        selected.setEnableGesture(checkboxEnableGesture.isChecked());
        selected.setConsoleMode(checkboxConsoleMode.isChecked());
//        selected.setRedirectSdCard(checkboxRedirectSdCard.isChecked());
        selected.setRemoteFx(checkboxRemoteFx.isChecked());
        selected.setDesktopBackground(checkboxDesktopBackground.isChecked());
        selected.setFontSmoothing(checkboxFontSmoothing.isChecked());
        selected.setDesktopComposition(checkboxDesktopComposition.isChecked());
        selected.setWindowContents(checkboxWindowContents.isChecked());
        selected.setMenuAnimation(checkboxMenuAnimation.isChecked());
        selected.setVisualStyles(checkboxVisualStyles.isChecked());
        selected.setEnableGfx(checkboxEnableGfx.isChecked());
        selected.setEnableGfxH264(checkboxEnableGfxH264.isChecked());

        selected.setPassword(passwordText.getText().toString());
        selected.setKeepPassword(checkboxKeepPassword.isChecked());
        selected.setUseDpadAsArrows(checkboxUseDpadAsArrows.isChecked());
        selected.setRotateDpad(checkboxRotateDpad.isChecked());
        selected.setUseLastPositionToolbar(checkboxUseLastPositionToolbar.isChecked());
        selected.setPreferSendingUnicode(checkboxPreferSendingUnicode.isChecked());
        // TODO: Reinstate Color model spinner but for RDP settings.
        //selected.setColorModel(((COLORMODEL)colorSpinner.getSelectedItem()).nameString());
    }

    /**
     * Automatically linked with android:onClick in the layout.
     * @param view
     */
    public void toggleEnableRecording(View view) {
        CheckBox b = (CheckBox) view;
        Activity a = this;

        if (PermissionsManager.hasPermission(a, PermissionGroups.RECORD_AND_MODIFY_AUDIO)) {
            selected.setEnableRecording(b.isChecked());
            return;
        }

        // reqest for recoding permission
        android.app.AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(getString(R.string.request_recording_parameter_title));
        alertDialogBuilder.setMessage(getString(R.string.request_recording_parameter_description));

        alertDialogBuilder.setPositiveButton(getText(R.string.ok), (dialog, which) -> {
            PermissionsManager.requestPermissions(a, PermissionGroups.RECORD_AND_MODIFY_AUDIO, true);
        });

        alertDialogBuilder.create().show();
    }

    public void toggleEnableGesture(View view) {
        CheckBox b = (CheckBox) view;
        selected.setEnableGesture(b.isChecked());
    }

    public void editGesture(View view) {
        Intent intent = new Intent(this, GestureEditorActivity.class);
        intent.putExtra("connId", getCurrentConnection().getId());
        startActivity(intent);
    }

    public void toggleEnableStorageRedirect(View view) {
        CheckBox b = (CheckBox) view;
        PermissionsManager.requestPermissions(this, PermissionGroups.EXTERNAL_STORAGE_MANAGEMENT, true);

        selected.setRedirectSdCard(b.isChecked());
    }

    /**
     * Automatically linked with android:onClick in the layout.
     * @param view
     */
    public void remoteSoundTypeToggled(View view) {
//        if (Utils.isFree(this)) {
//            IntroTextDialog.showIntroTextIfNecessary(this, database, true);
//        }
    }

    /**
     * Sets the remote sound type in the settings from the specified parameter.
     * @param view
     */
    public void setRemoteSoundTypeFromView(View view) {
        RadioGroup g = (RadioGroup) view;
//        if (Utils.isFree(this)) {
//            IntroTextDialog.showIntroTextIfNecessary(this, database, true);
//            g.check(R.id.radioRemoteSoundDisabled);
//        }

        int id = g.getCheckedRadioButtonId();
        int soundType = Constants.REMOTE_SOUND_DISABLED;
        if (id == R.id.radioRemoteSoundOnServer) {
            soundType = Constants.REMOTE_SOUND_ON_SERVER;
        } else if (id == R.id.radioRemoteSoundOnDevice) {
            soundType = Constants.REMOTE_SOUND_ON_DEVICE;
        }
        selected.setRemoteSoundType(soundType);
    }

    public void setRemoteSoundTypeFromSettings(int type) {
        type = Constants.REMOTE_SOUND_DISABLED;

        int id = 0;
        switch (type) {
            case Constants.REMOTE_SOUND_DISABLED:
                id = R.id.radioRemoteSoundDisabled;
                break;
            case Constants.REMOTE_SOUND_ON_DEVICE:
                id = R.id.radioRemoteSoundOnDevice;
                break;
            case Constants.REMOTE_SOUND_ON_SERVER:
                id = R.id.radioRemoteSoundOnServer;
                break;
        }
        groupRemoteSoundType.check(id);
    }

    public void save(MenuItem item) {
        if (ipText.getText().length() != 0 && portText.getText().length() != 0) {
            saveConnectionAndCloseLayout();
        } else {
            Toast.makeText(this, R.string.rdp_server_empty, Toast.LENGTH_LONG).show();
        }

    }
}
