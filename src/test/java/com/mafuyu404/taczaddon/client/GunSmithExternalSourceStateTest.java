package com.mafuyu404.taczaddon.client;

import com.mafuyu404.taczaddon.init.crafting.GunSmithSourceScreenAccess;
import com.mafuyu404.taczaddon.testutil.MinecraftTestBootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GunSmithExternalSourceStateTest {
    @BeforeAll
    static void bootstrap() throws Exception {
        MinecraftTestBootstrap.prepare();
    }

    @Test
    void unchangedSourceRevisionReturnsUnchanged() {
        GunSmithExternalSourceState state = state();

        state.requestSourceRefresh(7);
        assertEquals(
                GunSmithSourceScreenAccess.AcceptResult.UPDATED,
                state.acceptSourceSnapshot(
                        7,
                        1L,
                        1L,
                        List.of()
                )
        );

        state.requestSourceRefresh(7);
        assertEquals(
                GunSmithSourceScreenAccess.AcceptResult.UNCHANGED,
                state.acceptSourceSnapshot(
                        7,
                        2L,
                        1L,
                        List.of()
                )
        );
    }

    @Test
    void changedSourceRevisionReturnsUpdated() {
        GunSmithExternalSourceState state = state();

        state.requestSourceRefresh(7);
        state.acceptSourceSnapshot(
                7,
                1L,
                1L,
                List.of()
        );

        state.requestSourceRefresh(7);
        assertEquals(
                GunSmithSourceScreenAccess.AcceptResult.UPDATED,
                state.acceptSourceSnapshot(
                        7,
                        2L,
                        2L,
                        List.of()
                )
        );
    }

    @Test
    void contentChangeWithSameRevisionReturnsUpdated() {
        GunSmithExternalSourceState state = state();

        state.requestSourceRefresh(7);
        state.acceptSourceSnapshot(
                7,
                1L,
                1L,
                List.of()
        );

        state.requestSourceRefresh(7);
        assertEquals(
                GunSmithSourceScreenAccess.AcceptResult.UPDATED,
                state.acceptSourceSnapshot(
                        7,
                        2L,
                        1L,
                        List.of(new ItemStack(Items.DIRT, 2))
                )
        );

        state.requestSourceRefresh(7);
        assertEquals(
                GunSmithSourceScreenAccess.AcceptResult.UNCHANGED,
                state.acceptSourceSnapshot(
                        7,
                        3L,
                        1L,
                        List.of(new ItemStack(Items.DIRT, 2))
                )
        );
    }

    @Test
    void staleRequestIdIsRejected() {
        GunSmithExternalSourceState state = state();

        state.requestSourceRefresh(7);
        state.acceptSourceSnapshot(
                7,
                1L,
                1L,
                List.of()
        );

        state.requestSourceRefresh(7);
        assertEquals(
                GunSmithSourceScreenAccess.AcceptResult.REJECTED,
                state.acceptSourceSnapshot(
                        7,
                        1L,
                        1L,
                        List.of()
                )
        );
    }

    @Test
    void wrongContainerIdIsRejected() {
        GunSmithExternalSourceState state = state();

        state.requestSourceRefresh(7);
        assertEquals(
                GunSmithSourceScreenAccess.AcceptResult.REJECTED,
                state.acceptSourceSnapshot(
                        8,
                        1L,
                        1L,
                        List.of()
                )
        );
        assertEquals(
                GunSmithSourceScreenAccess.AcceptResult.UPDATED,
                state.acceptSourceSnapshot(
                        7,
                        1L,
                        1L,
                        List.of()
                )
        );
    }

    @Test
    void staleResponseDoesNotClearNewerPendingRequest() {
        GunSmithExternalSourceState state = state();

        state.requestSourceRefresh(7);
        state.requestSourceRefresh(7);

        assertEquals(
                GunSmithSourceScreenAccess.AcceptResult.REJECTED,
                state.acceptSourceSnapshot(
                        7,
                        1L,
                        1L,
                        List.of()
                )
        );
        assertEquals(
                GunSmithSourceScreenAccess.AcceptResult.UPDATED,
                state.acceptSourceSnapshot(
                        7,
                        2L,
                        1L,
                        List.of()
                )
        );
    }

    @Test
    void duplicateAlreadyAcceptedRequestIsRejected() {
        GunSmithExternalSourceState state = state();

        state.requestSourceRefresh(7);
        assertEquals(
                GunSmithSourceScreenAccess.AcceptResult.UPDATED,
                state.acceptSourceSnapshot(
                        7,
                        1L,
                        1L,
                        List.of()
                )
        );
        assertEquals(
                GunSmithSourceScreenAccess.AcceptResult.REJECTED,
                state.acceptSourceSnapshot(
                        7,
                        1L,
                        1L,
                        List.of()
                )
        );
    }

    @Test
    void refreshTimeoutReissuesRequest() {
        AtomicInteger sends = new AtomicInteger();
        GunSmithExternalSourceState state = new GunSmithExternalSourceState(
                (containerId, requestId) -> sends.incrementAndGet()
        );

        state.requestSourceRefresh(7);
        for (int tick = 0; tick < 100; tick++) {
            state.tickSourceRefresh(7);
        }

        assertEquals(2, sends.get());
        assertEquals(
                GunSmithSourceScreenAccess.AcceptResult.UPDATED,
                state.acceptSourceSnapshot(
                        7,
                        2L,
                        1L,
                        List.of()
                )
        );
    }

    private static GunSmithExternalSourceState state() {
        return new GunSmithExternalSourceState(
                (containerId, requestId) -> {
                }
        );
    }
}
