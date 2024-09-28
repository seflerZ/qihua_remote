/**
 * Copyright (C) 2012 Iordan Iordanov
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

package com.qihua.bVNC.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.qihua.bVNC.ConnectionBean;
import com.qihua.bVNC.Database;
import com.qihua.bVNC.MostRecentBean;
import com.qihua.bVNC.Utils;
import com.undatech.remoteClientUi.R;

import net.sqlcipher.database.SQLiteDatabase;

/**
 * @author Michael A. MacDonald
 *
 */
public class IntroTextDialog extends Dialog {

    static IntroTextDialog dialog = null;
    private PackageInfo packageInfo;
    private Database database;
    private boolean donate = false;

    /**
     * @param context -- Containing dialog
     */
    private IntroTextDialog(Activity context, PackageInfo pi, Database database) {
        super(context);
        setOwnerActivity(context);
        packageInfo = pi;
        this.database = database;
    }

    public static void showIntroTextIfNecessary(Activity context, Database database, boolean show) {
//        PackageInfo pi;
//        try {
//            String packageName = Utils.pName(context);
//            pi = context.getPackageManager().getPackageInfo(packageName, 0);
//        } catch (PackageManager.NameNotFoundException nnfe) {
//            return;
//        }
//        MostRecentBean mr = ConnectionBean.getMostRecent(database.getReadableDatabase());
//        database.close();
//
//        if (dialog == null && show && (mr == null || mr.getShowSplashVersion() != pi.versionCode)) {
//            dialog = new IntroTextDialog(context, pi, database);
//            dialog.show();
//        }
    }

    /* (non-Javadoc)
     * @see android.app.Dialog#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String pkgName = Utils.pName(this.getContext());

        // do not use original's donate text
        donate = false;

        setContentView(R.layout.intro_dialog);
        getWindow().setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

        Context context = this.getContext();
        String title = getContext().getResources().getString(R.string.intro_title);
        if (Utils.isRdp(context)) {
            title = getContext().getResources().getString(R.string.rdp_intro_title);
        } else if (Utils.isSpice(context)) {
            title = getContext().getResources().getString(R.string.spice_intro_title);
        }
        StringBuilder sb = new StringBuilder(title);
        setTitle(sb);
        sb.delete(0, sb.length());
        if (pkgName.contains("SPICE")) {
            sb.append(getContext().getResources().getString(R.string.ad_donate_text_spice));
            sb.append("<br>");
            sb.append("<br>");
        } else if (pkgName.contains("RDP")) {
            sb.append(getContext().getResources().getString(R.string.ad_donate_text_rdp));
            sb.append("<br>");
            sb.append("<br>");
        }
        sb.append(getContext().getResources().getString(R.string.ad_donate_text0));
        sb.append("<br>");
        sb.append("<br>");

        if (donate) {
            sb.append("<a href=\"");
            sb.append(Utils.getDonationPackageLink(getContext()));
            sb.append("\">");
            sb.append(getContext().getResources().getString(R.string.ad_donate_text1));
            sb.append("</a>");
            sb.append("<br>");
            sb.append("<br>");
            sb.append(getContext().getResources().getString(R.string.ad_donate_text2));
            sb.append("<br>");
            sb.append("<br>");
            sb.append(getContext().getResources().getString(R.string.ad_donate_text3));
            sb.append(" <a href=\"market://details?id=com.qihua.bVNC\">VNC</a>");
            sb.append(", ");
            sb.append("<a href=\"market://details?id=com.qihua.rmt\">RDP</a>");
            sb.append(", ");
            sb.append("<a href=\"market://details?id=com.qihua.aSPICE\">SPICE</a>");
            sb.append(", ");
            sb.append("<a href=\"market://details?id=com.undatech.opaque\">oVirt/RHEV/Proxmox</a>");
            sb.append("<br>");
            sb.append("<br>");
        }

        sb.append(getContext().getResources().getString(R.string.intro_header));
        if (Utils.isVnc(context)) {
            sb.append(getContext().getResources().getString(R.string.intro_text));
        } else if (Utils.isRdp(context)) {
            sb.append(getContext().getResources().getString(R.string.rdp_intro_text));
        } else if (Utils.isSpice(context)) {
            sb.append(getContext().getResources().getString(R.string.spice_intro_text));
        }
        sb.append("\n");
        sb.append(getContext().getResources().getString(R.string.intro_version_text));
        TextView introTextView = (TextView) findViewById(R.id.textIntroText);
        introTextView.setText(Html.fromHtml(sb.toString()));
        introTextView.setMovementMethod(LinkMovementMethod.getInstance());
        ((Button) findViewById(R.id.buttonCloseIntro)).setOnClickListener(new View.OnClickListener() {

            /* (non-Javadoc)
             * @see android.view.View.OnClickListener#onClick(android.view.View)
             */
            @Override
            public void onClick(View v) {
                showAgain(true);
            }

        });

        Button buttonCloseIntroDontShow = (Button) findViewById(R.id.buttonCloseIntroDontShow);
        if (donate) {
            buttonCloseIntroDontShow.setVisibility(View.GONE);
        } else {
            buttonCloseIntroDontShow.setOnClickListener(new View.OnClickListener() {

                /* (non-Javadoc)
                 * @see android.view.View.OnClickListener#onClick(android.view.View)
                 */
                @Override
                public void onClick(View v) {
                    showAgain(false);
                }

            });
        }
    }

    /* (non-Javadoc)
     * @see android.app.Dialog#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getOwnerActivity().getMenuInflater().inflate(R.menu.intro_dialog_menu, menu);
        // Disabling Manual/Wiki Menu item as the original does not correspond to this project anymore.
        /*
        menu.findItem(R.id.itemOpenDoc).setOnMenuItemClickListener(new OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Utils.showDocumentation(getOwnerActivity());
                dismiss();
                return true;
            }
        });
        */
        menu.findItem(R.id.itemClose).setOnMenuItemClickListener(new OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                showAgain(true);
                return true;
            }
        });
        menu.findItem(R.id.itemDontShowAgain).setOnMenuItemClickListener(new OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                showAgain(false);
                return true;
            }
        });
        return true;
    }

    /*
     * (non-Javadoc)
     * @see android.app.Dialog#onBackPressed()
     */
    @Override
    public void onBackPressed() {
        showAgain(true);
    }

    private void showAgain(boolean show) {
        SQLiteDatabase db = database.getWritableDatabase();
        MostRecentBean mostRecent = ConnectionBean.getMostRecent(db);
        if (mostRecent != null) {
            int value = -1;
            if (!show) {
                value = packageInfo.versionCode;
            }
            mostRecent.setShowSplashVersion(value);
            mostRecent.Gen_update(db);
        }
        database.close();
        dismiss();
        dialog = null;
    }
}
