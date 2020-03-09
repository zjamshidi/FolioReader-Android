package com.folioreader;

import java.io.Serializable;

/**
 * Created by Zahra Jamshidi on 3/9/2020.
 */
public abstract class ShareHandler implements Serializable {
    public abstract void share(String selectedText);
}
