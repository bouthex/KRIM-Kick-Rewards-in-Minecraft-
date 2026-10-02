package com.archimak.krim;

import net.minecraft.server.MinecraftServer;

/**
 * Cambios temporales de atributos del jugador (saltar, gravedad, alcance) para la ruleta.
 * Cada uno usa un modificador con id propio "krim:ruleta_*", así siempre se puede quitar,
 * y al entrar al mundo se limpian todos por si el juego se cerró con alguno puesto.
 */
public final class Atributos {
	private static final String[][] CONOCIDOS = {
			{ "minecraft:jump_strength", "sin_patas" }, { "minecraft:gravity", "gravedad" },
			{ "minecraft:block_interaction_range", "brazos" }, { "minecraft:entity_interaction_range", "brazos" },
			{ "minecraft:movement_speed", "velocidad" } };

	static void aplicar(MinecraftServer s, String atributo, String nombre, double valor, String op, int segundos) {
		String id = "krim:ruleta_" + nombre;
		Krim.cmd(s, Krim.fuente(s), "attribute @p " + atributo + " modifier remove " + id);
		Krim.cmd(s, Krim.fuente(s), "attribute @p " + atributo + " modifier add " + id + " " + valor + " " + op);
		Ruleta.despues(segundos * 20, sv -> Krim.cmd(sv, Krim.fuente(sv), "attribute @p " + atributo + " modifier remove " + id));
	}

	static void limpiar(MinecraftServer s) {
		for (String[] a : CONOCIDOS) Krim.cmd(s, Krim.fuente(s), "attribute @a " + a[0] + " modifier remove krim:ruleta_" + a[1]);
	}

	private Atributos() { }
}
