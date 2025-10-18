package com.example.myapplication6;

import android.view.MotionEvent;
import android.view.View;

public class OverlayTouchListener implements View.OnTouchListener {
    private float dX, dY;

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                dX = view.getX() - event.getRawX();
                dY = view.getY() - event.getRawY();
                return true;
            case MotionEvent.ACTION_MOVE:
                view.setX(event.getRawX() + dX);
                view.setY(event.getRawY() + dY);
                return true;
        }
        return false;
    }
}

