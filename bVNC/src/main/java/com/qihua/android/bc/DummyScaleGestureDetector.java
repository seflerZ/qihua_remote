/* Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * MODIFIED FOR ANTLERSOFT
 *
 * Changes for antlersoft/ vnc viewer for android
 *
 * Copyright (C) 2010 Michael A. MacDonald
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
 * USA.
 */
package com.qihua.android.bc;

import android.view.MotionEvent;

/**
 * Implementation of scale gesture detector interface for devices without multi-touch support; does nothing
 *
 * @author Michael A. MacDonald
 */
class DummyScaleGestureDetector implements IBCScaleGestureDetector {

    /* (non-Javadoc)
     * @see com.qihua.android.bc.IBCScaleGestureDetector#getCurrentSpan()
     */
    @Override
    public float getCurrentSpan() {
        return 0;
    }

    /* (non-Javadoc)
     * @see com.qihua.android.bc.IBCScaleGestureDetector#getEventTime()
     */
    @Override
    public long getEventTime() {
        return 0;
    }

    /* (non-Javadoc)
     * @see com.qihua.android.bc.IBCScaleGestureDetector#getFocusX()
     */
    @Override
    public float getFocusX() {
        return 0;
    }

    /* (non-Javadoc)
     * @see com.qihua.android.bc.IBCScaleGestureDetector#getFocusY()
     */
    @Override
    public float getFocusY() {
        return 0;
    }

    /* (non-Javadoc)
     * @see com.qihua.android.bc.IBCScaleGestureDetector#getPreviousSpan()
     */
    @Override
    public float getPreviousSpan() {
        return 0;
    }

    /* (non-Javadoc)
     * @see com.qihua.android.bc.IBCScaleGestureDetector#getScaleFactor()
     */
    @Override
    public float getScaleFactor() {
        return 0;
    }

    /* (non-Javadoc)
     * @see com.qihua.android.bc.IBCScaleGestureDetector#getTimeDelta()
     */
    @Override
    public long getTimeDelta() {
        return 0;
    }

    /* (non-Javadoc)
     * @see com.qihua.android.bc.IBCScaleGestureDetector#isInProgress()
     */
    @Override
    public boolean isInProgress() {
        return false;
    }

    /* (non-Javadoc)
     * @see com.qihua.android.bc.IBCScaleGestureDetector#onTouchEvent(android.view.MotionEvent)
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }

}
