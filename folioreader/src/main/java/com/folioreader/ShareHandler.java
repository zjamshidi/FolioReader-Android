package com.folioreader;

import android.content.Context;

import androidx.annotation.Nullable;

/**
 * Created by Zahra Jamshidi on 3/9/2020.
 */
public interface ShareHandler {
    void share(Context context, @Nullable String selectedText);
}
