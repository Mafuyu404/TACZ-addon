package com.mafuyu404.taczaddon.common;

import com.mafuyu404.taczaddon.init.CommonConfig;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ReloadState;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import com.tacz.guns.entity.sync.ModSyncedEntityData;
import net.minecraft.world.entity.LivingEntity;

public final class ShootWhileReloadService {
    private ShootWhileReloadService() {
    }

    public static boolean canInterruptForImmediateServerShot(
            LivingEntity shooter
    ) {
        if (!CommonConfig.enableShootWhileReloading()
                || shooter == null) {
            return false;
        }
        IGunOperator operator =
                IGunOperator.fromLivingEntity(shooter);
        if (operator == null) {
            return false;
        }
        ShooterDataHolder data = operator.getDataHolder();
        return data != null
                && data.reloadStateType.isReloading();
    }

    public static void commitReloadCancellation(
            LivingEntity shooter
    ) {
        if (shooter == null) {
            return;
        }

        IGunOperator operator =
                IGunOperator.fromLivingEntity(shooter);
        if (operator == null) {
            return;
        }

        ShooterDataHolder data = operator.getDataHolder();
        operator.cancelReload();
        if (data != null) {
            data.reloadStateType =
                    ReloadState.StateType.NOT_RELOADING;
            data.reloadTimestamp = -1L;
        }
        ModSyncedEntityData.RELOAD_STATE_KEY.setValue(
                shooter,
                new ReloadState()
        );
    }
}
