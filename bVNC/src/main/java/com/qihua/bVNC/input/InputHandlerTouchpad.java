/**
 * Copyright (C) 2013- Iordan Iordanov
 * <p>
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
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


package com.qihua.bVNC.input;

import android.view.MotionEvent;

import com.qihua.bVNC.RemoteCanvas;
import com.qihua.bVNC.RemoteCanvasActivity;
import com.undatech.opaque.util.GeneralUtils;
import com.undatech.remoteClientUi.R;

public class InputHandlerTouchpad extends InputHandlerGeneric {
    public static final String ID = "TOUCHPAD_MODE";
    static final String TAG = "InputHandlerTouchpad";
    private float cumulatedY = 0;
    private float cumulatedX = 0;

    public InputHandlerTouchpad(RemoteCanvasActivity activity, RemoteCanvas canvas, RemoteCanvas touchpad,
                                RemotePointer pointer, boolean debugLogging) {
        super(activity, canvas, touchpad, pointer, debugLogging);
    }

    /*
     * (non-Javadoc)
     * @see com.qihua.bVNC.input.InputHandler#getDescription()
     */
    @Override
    public String getDescription() {
        return canvas.getResources().getString(R.string.input_method_touchpad_description);
    }

    /*
     * (non-Javadoc)
     * @see com.qihua.bVNC.input.InputHandler#getId()
     */
    @Override
    public String getId() {
        return ID;
    }

    /*
     * (non-Javadoc)
     * @see android.view.GestureDetector.SimpleOnGestureListener#onScroll(android.view.MotionEvent, android.view.MotionEvent, float, float)
     */
    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        GeneralUtils.debugLog(debugLogging, TAG, "onScroll, e1: " + e1 + ", e2:" + e2);

        if (activity.isToolbarShowing()) {
            return true;
        }

        final int meta = e2.getMetaState();

        // TODO: This is a workaround for Android 4.2
        boolean twoFingers = false;
        if (e1 != null)
            twoFingers = (e1.getPointerCount() == 2);

        twoFingers = twoFingers || (e2.getPointerCount() == 2);

        if (Math.abs(distanceX) > baseSwipeDist || Math.abs(distanceY) > baseSwipeDist) {
            canSwipeToMove = true;
        }

        if (!canSwipeToMove) {
            return true;
        }

        if (inScaling) {
            return true;
        }

        if (thirdPointerWasDown) {
            return true;
        }

        if (!inScrolling && twoFingers) {
            inScrolling = true;

            distXQueue.clear();
            distYQueue.clear();

            inSwiping = true;
        }

        // If in swiping mode, indicate a swipe at regular intervals.
        if (inSwiping || immersiveSwipe) {
            scrollDown = false;
            scrollUp = false;
            scrollRight = false;
            scrollLeft = false;

            if (distanceY > 0) {
                lastScrollDirection = 2;
                scrollDown = true;
            } else if (distanceY < 0) {
                lastScrollDirection = 0;
                scrollUp = true;
            }

            if (distanceX > 0) {
                lastScrollDirection = 3;
                scrollRight = true;
            } else if (distanceX < 0) {
                lastScrollDirection = 1;
                scrollLeft = true;
            }

            if (cumulatedY * distanceY < 0) {
                cumulatedY = 0;
            }

            if (cumulatedX * distanceX < 0) {
                cumulatedX = 0;
            }

            // get the relative moving distance compared to one step
            float ratioY = distanceY / ((float) canvas.getHeight() / canvas.getVisibleDesktopHeight());
            float ratioX = distanceX / ((float) canvas.getWidth() / canvas.getVisibleDesktopWidth());

            // The direction is just up side down.
            int newY = (int)-(ratioY);
            int newX = (int)(ratioX);
            int delta = 0;

            if (Math.abs(distanceY) >= Math.abs(distanceX)) {
                scrollRight = false;
                scrollLeft = false;
            } else {
                scrollUp = false;
                scrollDown = false;
            }

            if (scrollUp || scrollDown) {
                if (distanceY < 0 && newY == 0) {
                    delta = 0;
                } else if (distanceY > 0 && newY == 0) {
                    delta = 0;
                } else {
                    delta = newY;
                }

                if (delta == 0) {
                    return true;
                }

                if (delta > 255) {
                    delta = 255;
                } else if (delta < -255) {
                    delta = -255;
                }

                if (delta < 0) {
                    // use positive number to represent the component directly for
                    // the least two bytes
                    delta = 256 + delta;
                }

                lastDelta = delta;

                // Set the coordinates to where the swipe began (i.e. where scaling started).
                sendScrollEvents(getX(e2), getY(e2), delta, meta);

                swipeSpeed = 1;
            }

            if (scrollRight || scrollLeft){
                if (distanceX < 0 && newX == 0) {
                    delta = 0;
                } else if (distanceX > 0 && newX == 0) {
                    delta = 0;
                } else {
                    delta = newX;
                }

                if (delta == 0) {
                    return true;
                }

                if (delta > 255) {
                    delta = 255;
                } else if (delta < -255) {
                    delta = -255;
                }

                if (delta < 0) {
                    // use positive number to represent the component directly for
                    // the least two bytes
                    delta = 256 + delta;
                }

                lastDelta = delta;

                // Set the coordinates to where the swipe began (i.e. where scaling started).
                sendScrollEvents(getX(e2), getY(e2), delta, meta);

                swipeSpeed = 1;
            }
        } else {
            // Make distanceX/Y display density independent.
            float sensitivity = pointer.getSensitivity();
            distanceX = sensitivity * distanceX / displayDensity;
            distanceY = sensitivity * distanceY / displayDensity;

            if (distanceX > 0 && distanceX < 1) {
                distanceX = (float) Math.ceil(distanceX);
            } else if (distanceX > -1 && distanceX < 0) {
                distanceX = (float) Math.floor(distanceX);
            }

            if (distanceY > 0 && distanceY < 1) {
                distanceY = (float) Math.ceil(distanceY);
            } else if (distanceY > -1 && distanceY < 0) {
                distanceY = (float) Math.floor(distanceY);
            }

            // Compute the absolute new mouse position.
            int newX = Math.round(pointer.getX() + -distanceX);
            int newY = Math.round(pointer.getY() + -distanceY);

            pointer.moveMouse(newX, newY, meta);

            canvas.movePanToMakePointerVisible();
        }

        return true;
    }

    /*
     * (non-Javadoc)
     * @see android.view.GestureDetector.SimpleOnGestureListener#onDown(android.view.MotionEvent)
     */
    @Override
    public boolean onDown(MotionEvent e) {
        GeneralUtils.debugLog(debugLogging, TAG, "onDown, e: " + e);
        panRepeater.stop();
        return true;
    }

    /*
     * (non-Javadoc)
     * @see com.qihua.bVNC.input.InputHandlerGeneric#getX(android.view.MotionEvent)
     */
    protected int getX(MotionEvent e) {
        RemotePointer p = canvas.getPointer();
        if (dragMode || rightDragMode || middleDragMode) {
            float distanceX = e.getX() - dragX;
            dragX = e.getX();

            totalDragX += distanceX;
            // Compute the absolute new X coordinate.
            return Math.round(p.getX() + distanceX);
        }
        dragX = e.getX();
        return p.getX();
    }

    /*
     * (non-Javadoc)
     * @see com.qihua.bVNC.input.InputHandlerGeneric#getY(android.view.MotionEvent)
     */
    protected int getY(MotionEvent e) {
        RemotePointer p = canvas.getPointer();
        if (dragMode || rightDragMode || middleDragMode) {
            float distanceY = e.getY() - dragY;
            dragY = e.getY();

            totalDragY += distanceY;
            // Compute the absolute new Y coordinate.
            return Math.round(p.getY() + distanceY);
        }
        dragY = e.getY();
        return p.getY();
    }
}
