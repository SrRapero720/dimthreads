package me.srrapero720.dimthread.mixin.impl.mi_405388;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "aztech.modern_industrialization.machines.multiblocks.world")
@Pseudo
public class ChunkEventListenerMixin {
    @Redirect(method = "onBlockStateChange", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;isSameThread()Z;"))
    @Dynamic
    private static boolean redirect$isSameThread(MinecraftServer instance) {
        return true;
    }

    /**
     * @author SrRapero720
     * @reason Redundant
     */
    @Overwrite
    private static void ensureServerThread(MinecraftServer server) {
        if (server == null) {
            throw new RuntimeException("Null server!");
        }
        // removed "isSameThread" check
    }
}