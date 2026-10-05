package com.mafuyu404.taczaddon.common;

import net.neoforged.neoforge.items.IItemHandler;
import java.util.function.BooleanSupplier;

/** Request-scoped source; persistence belongs to the integration that opened it. */
public record RefitSource(RefitSourceLocator locator, IItemHandler handler,
                          BooleanSupplier valid, Runnable changed) {
    public boolean isValid() { return valid.getAsBoolean(); }
    public void markChanged() { changed.run(); }
}
