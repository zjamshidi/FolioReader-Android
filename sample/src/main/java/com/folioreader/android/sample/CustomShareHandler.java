package com.folioreader.android.sample;

import android.util.Log;

import com.folioreader.ShareHandler;

/**
 * Created by Zahra Jamshidi on 3/9/2020.
 */
public class CustomShareHandler extends ShareHandler {
    @Override
    public void share(String selectedText) {
        Log.d("CustomShareHandler", "share tapped on " + selectedText);
    }
}
