/**
 * Copyright (C) 2009 Michael A. MacDonald
 */
package com.qihua.android.bc;

import android.app.ActivityManager;

/**
 * @author Michael A. MacDonald
 */
class BCActivityManagerDefault implements IBCActivityManager {

    /* (non-Javadoc)
     * @see com.qihua.android.bc.IBCActivityManager#getMemoryClass(android.app.ActivityManager)
     */
    @Override
    public int getMemoryClass(ActivityManager am) {
        return 16;
    }

}
