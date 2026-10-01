package com.archimak.kickcreeper;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Mascota ya domesticada con el nombre que eligió el viewer.
 * Se crea con /summon (así sale con variante y color al azar como en vanilla) y después se domestica desde código.
 */
public final class Mascota {

	public static void crear(MinecraftServer s, ServerPlayer p, String tipo, String nombre, String usuario) {
		String entidad = switch (tipo) {
			case "gato" -> "minecraft:cat";
			case "loro" -> "minecraft:parrot";
			default -> "minecraft:wolf";
		};
		String marca = "krim_n" + System.nanoTime();
		CommandSourceStack src = KickCreeper.fuente(s);
		KickCreeper.cmd(s, src, "execute as @p at @s run summon " + entidad + " ~ ~ ~ {Tags:[\"krim_mascota\",\"" + marca + "\"],"
				+ "CustomNameVisible:1b,CustomName:{text:\"" + nombre + "\",color:\"aqua\"},PersistenceRequired:1b}");

		ServerLevel lvl = (ServerLevel) p.level();
		List<Entity> encontradas = lvl.getEntities((Entity) null, new AABB(p.blockPosition()).inflate(4), e -> Etiquetas.tiene(e, marca));
		KickCreeper.cmd(s, src, "tag @e[tag=" + marca + "] remove " + marca);

		if (encontradas.isEmpty()) {
			Registro.aviso("MASCOTA", "No se encontró la mascota recién creada (" + tipo + " \"" + nombre + "\")");
			return;
		}
		if (encontradas.get(0) instanceof TamableAnimal animal) {
			animal.tame(p);
			Registro.info("MASCOTA", tipo + " \"" + nombre + "\" de " + usuario + " creado y domesticado");
		} else {
			Registro.aviso("MASCOTA", "La entidad creada no se puede domesticar: " + entidad);
		}
		KickCreeper.aviso(s, src, usuario + " te regaló una mascota: " + nombre, "aqua");
	}
}
