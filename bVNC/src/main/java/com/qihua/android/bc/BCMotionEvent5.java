/**
 * Copyright (c) 2010 Michael A. MacDonald
 */
package com.qihua.android.bc;

import android.view.MotionEvent;

/**
 * @author Michael A. MacDonald
 */
class BCMotionEvent5 implements IBCMotionEvent {

    /* (non-Javadoc)
     * @see com.qihua.android.bc.IBCMotionEvent#getPointerCount(android.view.MotionEvent)
     */
    @Override
    public int getPointerCount(MotionEvent evt) {
        return evt.getPointerCount();
    }

}
