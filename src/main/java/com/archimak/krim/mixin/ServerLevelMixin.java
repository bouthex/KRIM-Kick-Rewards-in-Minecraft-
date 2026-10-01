package com.archimak.krim.mixin;

import com.archimak.krim.Etiquetas;
import com.archimak.krim.Krim;
import com.archimak.krim.Registro;
import com.archimak.krim.SoloJugador;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

	/**
	 * Cuando explota un creeper con el tag del chat: no rompe bloques (KEEP),
	 * no prende fuego y solo daña al jugador. Cualquier otra explosión queda igual que en vanilla.
	 */
	@WrapOperation(
		method = "explode",
		at = @At(
			value = "NEW",
			target = "(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;Lnet/minecraft/world/phys/Vec3;FZLnet/minecraft/world/level/Explosion$BlockInteraction;)Lnet/minecraft/world/level/ServerExplosion;"
		)
	)
	private ServerExplosion krim$soloJugador(
			ServerLevel level, Entity source, DamageSource damageSource, ExplosionDamageCalculator calculator,
			Vec3 pos, float radius, boolean fire, Explosion.BlockInteraction interaction,
			Operation<ServerExplosion> original) {
		if (source != null && Etiquetas.tiene(source, Krim.TAG)) {
			int n = Krim.EXPLOSIONES.incrementAndGet();
			Registro.info("EXPLOSION", "#" + n + " creeper del chat \"" + source.getName().getString() + "\" en "
					+ Math.round(pos.x) + " " + Math.round(pos.y) + " " + Math.round(pos.z) + " radio " + radius
					+ " -> sin bloques, solo jugador");
			return original.call(level, source, damageSource, SoloJugador.INSTANCIA, pos, radius, false, Explosion.BlockInteraction.KEEP);
		}
		return original.call(level, source, damageSource, calculator, pos, radius, fire, interaction);
	}
}
