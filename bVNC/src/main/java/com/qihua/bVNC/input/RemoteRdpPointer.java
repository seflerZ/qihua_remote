package com.qihua.bVNC.input;

import android.os.Handler;

import com.qihua.bVNC.RemoteCanvas;
import com.undatech.opaque.RfbConnectable;
import com.undatech.opaque.util.GeneralUtils;

public class RemoteRdpPointer extends RemotePointer {
    private static final String TAG = "RemoteRdpPointer";

    private final static int PTRFLAGS_HWHEEL = 0x0400;
    private final static int PTRFLAGS_WHEEL = 0x0200;
    private final static int PTRFLAGS_WHEEL_NEGATIVE = 0x0100;
    //private final static int PTRFLAGS_DOWN           = 0x8000;

    private final static int MOUSE_BUTTON_NONE = 0x0000;
    private final static int MOUSE_BUTTON_MOVE = 0x0800;
    private final static int MOUSE_BUTTON_LEFT = 0x1000;
    private final static int MOUSE_BUTTON_RIGHT = 0x2000;

    private static final int MOUSE_BUTTON_MIDDLE = 0x4000;
    private static final int MOUSE_BUTTON_SCROLL_UP = PTRFLAGS_WHEEL | 0x0058;
    private static final int MOUSE_BUTTON_SCROLL_DOWN = PTRFLAGS_WHEEL | PTRFLAGS_WHEEL_NEGATIVE | 0x00a8;



    private static final int MOUSE_BUTTON_SCROLL_LEFT = PTRFLAGS_HWHEEL | 0x0058;
    private static final int MOUSE_BUTTON_SCROLL_RIGHT = PTRFLAGS_HWHEEL | PTRFLAGS_WHEEL_NEGATIVE | 0x00a8;

    public RemoteRdpPointer(RfbConnectable spicecomm, RemoteCanvas canvas, Handler handler,
                            boolean debugLogging) {
        super(spicecomm, canvas, handler, debugLogging);
    }

    private void sendButtonDownOrMoveButtonDown(int x, int y, int metaState) {
        if (prevPointerMask == pointerMask) {
            moveMouseButtonDown(x, y, metaState);
        } else {
            sendPointerEvent(x, y, metaState, false);
        }
    }

    @Override
    public void leftButtonDown(int x, int y, int metaState) {
        pointerMask = MOUSE_BUTTON_LEFT | POINTER_DOWN_MASK;
        sendButtonDownOrMoveButtonDown(x, y, metaState);
    }

    @Override
    public void middleButtonDown(int x, int y, int metaState) {
        pointerMask = MOUSE_BUTTON_MIDDLE | POINTER_DOWN_MASK;
        sendButtonDownOrMoveButtonDown(x, y, metaState);
    }

    @Override
    public void rightButtonDown(int x, int y, int metaState) {
        pointerMask = MOUSE_BUTTON_RIGHT | POINTER_DOWN_MASK;
        sendButtonDownOrMoveButtonDown(x, y, metaState);
    }

    @Override
    public void scrollUp(int x, int y, int speed, int metaState) {
        if (speed < 0) {
            pointerMask = MOUSE_BUTTON_SCROLL_UP;
        } else {
            pointerMask = PTRFLAGS_WHEEL | (speed & 0x00ff);
        }

        sendPointerEvent(x, y, metaState, false);
    }

    @Override
    public void scrollDown(int x, int y, int speed, int metaState) {
        if (speed < 0) {
            pointerMask = MOUSE_BUTTON_SCROLL_DOWN;
        } else {
            pointerMask = PTRFLAGS_WHEEL | PTRFLAGS_WHEEL_NEGATIVE | (speed & 0x00ff);
        }
        sendPointerEvent(x, y, metaState, false);
    }

    @Override
    public void scrollLeft(int x, int y, int speed, int metaState) {
        if (speed < 0) {
            pointerMask = MOUSE_BUTTON_SCROLL_LEFT;
        } else {
            pointerMask = PTRFLAGS_HWHEEL | (speed & 0x00ff);
        }
        sendPointerEvent(x, y, metaState, false);
    }

    @Override
    public void scrollRight(int x, int y, int speed, int metaState) {
        if (speed < 0) {
            pointerMask = MOUSE_BUTTON_SCROLL_RIGHT;
        } else {
            pointerMask = PTRFLAGS_HWHEEL | PTRFLAGS_WHEEL_NEGATIVE | (speed & 0x00ff);
        }
        sendPointerEvent(x, y, metaState, false);
    }

    @Override
    public void moveMouse(int x, int y, int metaState) {
        pointerMask = MOUSE_BUTTON_MOVE | prevPointerMask;
        sendPointerEvent(x, y, metaState, true);
    }

    @Override
    public void moveMouseButtonDown(int x, int y, int metaState) {
        pointerMask = MOUSE_BUTTON_MOVE | POINTER_DOWN_MASK;
        sendPointerEvent(x, y, metaState, true);
    }

    @Override
    public void moveMouseButtonUp(int x, int y, int metaState) {
        pointerMask = MOUSE_BUTTON_MOVE;
        sendPointerEvent(x, y, metaState, true);
    }

    @Override
    public void releaseButton(int x, int y, int metaState) {
        pointerMask = MOUSE_BUTTON_MOVE;
        sendPointerEvent(x, y, metaState, false);
        prevPointerMask = 0;
    }

    /**
     * Sends a pointer event to the server.
     *
     * @param x
     * @param y
     * @param metaState
     * @param isMoving
     */
    private void sendPointerEvent(int x, int y, int metaState, boolean isMoving) {

        int combinedMetaState = metaState | canvas.getKeyboard().getMetaState();

        // Save the previous pointer mask other than action_move, so we can
        // send it with the pointer flag "not down" to clear the action.
        if (!isMoving) {
            // If this is a new mouse down event, release previous button pressed to avoid confusing the remote OS.
            if (prevPointerMask != 0 && prevPointerMask != pointerMask) {
                protocomm.writePointerEvent(pointerX, pointerY,
                        combinedMetaState,
                        prevPointerMask & ~POINTER_DOWN_MASK, false);
            }
            prevPointerMask = pointerMask;
        }

        canvas.invalidateMousePosition();
        pointerX = x;
        pointerY = y;

        // Do not let mouse pointer leave the bounds of the desktop.
        if (pointerX < 0) {
            pointerX = 0;
        } else if (pointerX >= canvas.getImageWidth()) {
            pointerX = canvas.getImageWidth() - 1;
        }
        if (pointerY < 0) {
            pointerY = 0;
        } else if (pointerY >= canvas.getImageHeight()) {
            pointerY = canvas.getImageHeight() - 1;
        }
//        canvas.invalidateMousePosition();
        GeneralUtils.debugLog(this.debugLogging, TAG, "Sending absolute mouse event at: " + pointerX +
                ", " + pointerY + ", pointerMask: " + pointerMask);
        protocomm.writePointerEvent(pointerX, pointerY, combinedMetaState, pointerMask, false);
    }

}
