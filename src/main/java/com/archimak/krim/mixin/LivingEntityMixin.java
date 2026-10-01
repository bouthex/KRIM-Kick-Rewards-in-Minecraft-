package com.archimak.krim.mixin;

import com.archimak.krim.Etiquetas;
import com.archimak.krim.Krim;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Los mobs del chat (creepers y animales) no sueltan experiencia al morir.
 * Los ítems ya los evita la tabla de botín vacía que se pone al invocarlos.
 * require = 0: si en alguna versión futura este método cambia de nombre, el juego NO crashea;
 * solo vuelven a soltar experiencia y Mixin lo avisa en latest.log.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

	@Inject(method = "shouldDropExperience", at = @At("HEAD"), cancellable = true, require = 0)
	private void krim$sinExperiencia(CallbackInfoReturnable<Boolean> cir) {
		LivingEntity yo = (LivingEntity) (Object) this;
		if (Etiquetas.tiene(yo, Krim.TAG) || Etiquetas.tiene(yo, Krim.TAG_ANIMAL)) {
			cir.setReturnValue(false);
		}
	}
}
