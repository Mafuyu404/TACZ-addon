package com.mafuyu404.taczaddon.common;

import com.mafuyu404.taczaddon.init.Config;

public final class FastSwapService {
    private FastSwapService() {
    }

    public static boolean enabled() {
        return Config.FAST_SWAP_GUN.get();
    }
}
