package com.archimak.krim.mixin;

import com.archimak.krim.Etiquetas;
import com.archimak.krim.Krim;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Los mobs de la ruleta solo eligen como objetivo a jugadores (no persiguen aldeanos, tortugas ni mascotas). */
@Mixin(Mob.class)
public abstract class MobMixin {

	@Inject(method = "setTarget", at = @At("HEAD"), cancellable = true, require = 0)
	private void krim$soloJugadores(LivingEntity objetivo, CallbackInfo ci) {
		if (objetivo == null || objetivo instanceof Player) return;
		if (Etiquetas.tiene((Mob) (Object) this, Krim.TAG_RULETA)) ci.cancel();
	}
}
