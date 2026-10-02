package com.archimak.krim.mixin;

import com.archimak.krim.Etiquetas;
import com.archimak.krim.Krim;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1) Los mobs del chat (creepers, animales y ruleta) no sueltan experiencia al morir.
 * 2) Lo hostil del chat (creepers, mobs de la ruleta y sus flechas/bolas de fuego) solo puede dañar a jugadores:
 *    aldeanos, mascotas, animales y otros mobs no reciben daño.
 * require = 0: si un método cambia de nombre en otra versión, el juego no crashea; la ruleta detecta
 * que la protección 2 no está activa y deja de sacar mobs hostiles (queda anotado en krim.log).
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

	@Inject(method = "shouldDropExperience", at = @At("HEAD"), cancellable = true, require = 0)
	private void krim$sinExperiencia(CallbackInfoReturnable<Boolean> cir) {
		LivingEntity yo = (LivingEntity) (Object) this;
		if (Etiquetas.tiene(yo, Krim.TAG) || Etiquetas.tiene(yo, Krim.TAG_ANIMAL) || Etiquetas.tiene(yo, Krim.TAG_RULETA)) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true, require = 0)
	private void krim$sinDanoAOtros(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof Player) return;
		if (Krim.esHostilDelChat(source.getEntity()) || Krim.esHostilDelChat(source.getDirectEntity())) {
			cir.setReturnValue(false);
		}
	}
}
