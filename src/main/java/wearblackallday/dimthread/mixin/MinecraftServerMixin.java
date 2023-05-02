/*
 * This file is part of Dimensional Threading Reforged - https://github.com/CCr4ft3r/DimensionalThreading-Reforged
 * Copyright (C) WearBlackAllDay and contributors: https://github.com/WearBlackAllDay/DimensionalThreading
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package wearblackallday.dimthread.mixin;

import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wearblackallday.dimthread.DimThread;
import wearblackallday.dimthread.util.CrashInfo;
import wearblackallday.util.ThreadPool;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {

    @Shadow
    private int tickCount;
    @Shadow
    private PlayerList playerList;

    @Shadow
    public abstract Iterable<ServerLevel> getAllLevels();

    @Shadow(remap = false)
    protected abstract ServerLevel[] getWorldArray();

    /**
     * Returns an empty iterator to stop {@code MinecraftServer#tickWorlds} from ticking
     * dimensions. This behaviour is overwritten below.
     *
     * @see MinecraftServerMixin#tickWorlds(BooleanSupplier, CallbackInfo)
     */
    @Redirect(method = "tickChildren", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/server/MinecraftServer;getWorldArray()[Lnet/minecraft/server/level/ServerLevel;"))
    public ServerLevel[] tickWorlds(MinecraftServer instance) {
        return DimThread.MANAGER.isActive((MinecraftServer) (Object) this) ? new ServerLevel[]{} : getWorldArray();
    }

    /**
     * Distributes world ticking over 3 worker threads (one for each dimension) and waits until
     * they are all complete.
     */
    @Inject(method = "tickChildren", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/server/MinecraftServer;getWorldArray()[Lnet/minecraft/server/level/ServerLevel;"))
    public void tickWorlds(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        if (!DimThread.MANAGER.isActive((MinecraftServer) (Object) this)) return;

        AtomicReference<CrashInfo> crash = new AtomicReference<>();
        ThreadPool pool = DimThread.getThreadPool((MinecraftServer) (Object) this);

        pool.execute(this.getAllLevels().iterator(), serverWorld -> {
            DimThread.attach(Thread.currentThread(), serverWorld);

            if (this.tickCount % 20 == 0) {
                ClientboundSetTimePacket timeUpdatePacket = new ClientboundSetTimePacket(
                    serverWorld.getGameTime(), serverWorld.getDayTime(),
                    serverWorld.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT));

                this.playerList.broadcastAll(timeUpdatePacket, serverWorld.dimension());
            }

            DimThread.swapThreadsAndRun(() -> {
                net.minecraftforge.event.ForgeEventFactory.onPreLevelTick(serverWorld, shouldKeepTicking);
                try {
                    serverWorld.tick(shouldKeepTicking);
                } catch (Throwable throwable) {
                    crash.set(new CrashInfo(serverWorld, throwable));
                }
                net.minecraftforge.event.ForgeEventFactory.onPostLevelTick(serverWorld, shouldKeepTicking);
            }, serverWorld, serverWorld.getChunkSource());
        });

        pool.awaitCompletion();

        if (crash.get() != null) {
            crash.get().crash("Exception ticking world");
        }
    }

    /**
     * Shutdown all threadpools when the server stop.
     * Prevent server hang when stopping the server.
     */
    @Inject(method = "stopServer", at = @At("HEAD"))
    public void shutdownThreadpool(CallbackInfo ci) {
        DimThread.MANAGER.threadPools.forEach((server, pool) -> pool.shutdown());
    }
}