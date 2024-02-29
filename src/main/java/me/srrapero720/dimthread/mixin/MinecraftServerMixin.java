package me.srrapero720.dimthread.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.srrapero720.dimthread.DimThread;
import me.srrapero720.dimthread.thread.ThreadPool;
import me.srrapero720.dimthread.util.CrashInfo;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

@Mixin(value = MinecraftServer.class, priority = 1010)
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
    @WrapOperation(method = "tickChildren", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/server/MinecraftServer;getWorldArray()[Lnet/minecraft/server/level/ServerLevel;", remap = false))
    public ServerLevel[] tickWorlds(MinecraftServer instance, Operation<ServerLevel[]> original) {
        return DimThread.MANAGER.isActive((MinecraftServer) (Object) this) ? new ServerLevel[]{} : original.call(instance);
    }

    /**
     * Distributes world ticking over 3 worker threads (one for each dimension) and waits until
     * they are all complete.
     */
    @Inject(method = "tickChildren", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/server/MinecraftServer;getWorldArray()[Lnet/minecraft/server/level/ServerLevel;", remap = false))
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
     * Shutdown all threadpools when the server stops.
     * Prevent server hang when stopping the server.
     */
    @Inject(method = "stopServer", at = @At("HEAD"))
    public void shutdownThreadpool(CallbackInfo ci) {
        DimThread.MANAGER.threadPools.forEach((server, pool) -> pool.shutdown());
    }
}