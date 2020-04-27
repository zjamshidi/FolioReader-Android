package com.folioreader;

import android.content.Context;

import androidx.annotation.Nullable;

import java.io.Serializable;

/**
 * Created by Zahra Jamshidi on 3/9/2020.
 */
public abstract class ShareHandler implements Serializable {
    public abstract void share(Context context, @Nullable String selectedText);
}
