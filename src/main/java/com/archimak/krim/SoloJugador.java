package com.archimak.krim;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;

/** Explosión que solo daña y empuja a jugadores: mobs, mascotas, aldeanos e items quedan intactos. */
public class SoloJugador extends ExplosionDamageCalculator {
	public static final SoloJugador INSTANCIA = new SoloJugador();

	@Override
	public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
		return entity instanceof Player;
	}

	@Override
	public float getKnockbackMultiplier(Entity entity) {
		return entity instanceof Player ? 1.0F : 0.0F;
	}
}
