package com.android.internal.policy.impl.lewa.lockscreen;

import android.content.Intent;

public interface UnlockerCallback {
    
    public abstract void pokeWakelock(int i);

    public abstract void unlocked(Intent intent);
}
