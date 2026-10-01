package com.archimak.krim;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

/**
 * Cartel con el mensaje del chat delante del jugador, mirándolo.
 * Nunca reemplaza bloques: solo se pone en aire, sobre un bloque donde un cartel se sostiene,
 * y además el setblock usa "keep" (no hace nada si el lugar no está vacío). Queda para siempre y encerado.
 */
public final class Cartel {
	private static final int ANCHO = 15;

	public static void colocar(MinecraftServer s, ServerPlayer p, String texto, String usuario) {
		ServerLevel lvl = (ServerLevel) p.level();
		float yaw = p.getYRot();
		double rad = Math.toRadians(yaw);
		double dx = -Math.sin(rad), dz = Math.cos(rad);

		BlockPos pos = buscarLugar(lvl, p, dx, dz);
		CommandSourceStack src = Krim.fuente(s);
		if (pos == null) {
			Registro.aviso("CARTEL", "Sin lugar libre delante del jugador para \"" + texto + "\" de " + usuario);
			Krim.aviso(s, src, "No había lugar para el cartel de " + usuario, "red");
			return;
		}

		int rotacion = Math.floorMod(Math.round((yaw + 180f) / 22.5f), 16); // el frente mira al jugador
		List<String> l = lineas(texto);
		while (l.size() < 3) l.add("");
		String firma = ("- " + usuario);
		if (firma.length() > 13) firma = firma.substring(0, 13); // en negrita entra un poco menos

		// Texto negro iluminado (tinta brillante) y la firma en rojo y negrita
		String nbt = "{is_waxed:1b,front_text:{has_glowing_text:1b,color:\"black\",messages:[\"" + l.get(0) + "\",\"" + l.get(1) + "\",\"" + l.get(2)
				+ "\",{text:\"" + firma + "\",color:\"red\",bold:1b}]}}";
		Krim.cmd(s, src, "execute as @p at @s run setblock "
				+ pos.getX() + " " + pos.getY() + " " + pos.getZ()
				+ " minecraft:oak_sign[rotation=" + rotacion + "]" + nbt + " keep");

		if (lvl.getBlockState(pos).is(Blocks.OAK_SIGN)) {
			Registro.info("CARTEL", "Colocado en " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + ": \"" + texto + "\" de " + usuario);
			Krim.aviso(s, src, usuario + " te dejó un cartel", "yellow");
		} else {
			Registro.aviso("CARTEL", "El setblock no colocó el cartel en " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
		}
	}

	/** Prueba delante (2, 3 y 1 bloques), al centro y a los costados, a la misma altura, uno arriba y uno abajo. */
	private static BlockPos buscarLugar(ServerLevel lvl, ServerPlayer p, double dx, double dz) {
		BlockPos pies = p.blockPosition();
		int[] distancias = { 2, 3, 1 };
		int[] costados = { 0, 1, -1 };
		int[] alturas = { 0, 1, -1 };
		for (int d : distancias) for (int c : costados) for (int h : alturas) {
			double x = p.getX() + dx * d + dz * c;
			double z = p.getZ() + dz * d - dx * c;
			BlockPos pos = BlockPos.containing(x, p.getY() + h, z);
			if (pos.equals(pies) || pos.equals(pies.above())) continue;
			if (!lvl.getBlockState(pos).isAir()) continue;
			if (!Blocks.OAK_SIGN.defaultBlockState().canSurvive(lvl, pos)) continue;
			return pos;
		}
		return null;
	}

	/** Reparte el texto en hasta 3 líneas de 15 caracteres, cortando por palabras. */
	static List<String> lineas(String texto) {
		List<String> res = new ArrayList<>();
		StringBuilder actual = new StringBuilder();
		for (String palabra : texto.split(" ")) {
			while (palabra.length() > ANCHO) {
				if (actual.length() > 0) { res.add(actual.toString()); actual.setLength(0); }
				res.add(palabra.substring(0, ANCHO));
				palabra = palabra.substring(ANCHO);
			}
			if (actual.length() == 0) actual.append(palabra);
			else if (actual.length() + 1 + palabra.length() <= ANCHO) actual.append(' ').append(palabra);
			else { res.add(actual.toString()); actual.setLength(0); actual.append(palabra); }
		}
		if (actual.length() > 0) res.add(actual.toString());
		return res.size() > 3 ? new ArrayList<>(res.subList(0, 3)) : res;
	}
}
