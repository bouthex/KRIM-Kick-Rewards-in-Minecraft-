package com.archimak.krim;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Canje de tamaño: el jugador cambia a un tamaño al azar durante un rato y después vuelve a la normalidad.
 * Usa un modificador del atributo minecraft:scale con id krim:tamano (no toca el valor base),
 * así siempre se puede quitar. Si el juego se cierra con el efecto activo, se quita al volver a entrar.
 * Nunca elige un tamaño que no entre en el lugar donde estás parado (para no asfixiarte en una cueva).
 */
public final class Tamano {
	private static final int DURACION = 30 * 20; // 30 segundos
	private static final double ALTO_JUGADOR = 1.8;
	private static final double[] TAMANOS = { 0.25, 0.35, 0.5, 0.65, 1.5, 2.0, 2.5, 3.0 };
	private static final String MOD = "krim:tamano";

	private static int restante; // ticks que faltan; 0 = sin efecto
	private static boolean limpiado;

	/** Se llama una vez al iniciar el mod. */
	static void iniciar() {
		ServerLifecycleEvents.SERVER_STARTED.register(s -> { restante = 0; limpiado = false; });
		ServerTickEvents.END_SERVER_TICK.register(Tamano::tick);
	}

	private static void tick(MinecraftServer s) {
		if (!limpiado && !s.getPlayerList().getPlayers().isEmpty()) {
			// Por si el juego se cerró con el efecto puesto: volver al tamaño normal al entrar
			limpiado = true;
			if (restante == 0) Krim.cmd(s, Krim.fuente(s), "attribute @a minecraft:scale modifier remove " + MOD);
		}
		if (restante <= 0) return;
		restante--;
		CommandSourceStack src = Krim.fuente(s);
		if (restante == 100) Krim.aviso(s, src, "Volvés a tu tamaño en 5 segundos...", "gold");
		if (restante == 0) {
			Krim.cmd(s, src, "attribute @p minecraft:scale modifier remove " + MOD);
			Krim.cmd(s, src, "execute at @p run particle minecraft:poof ~ ~1 ~ 0.6 0.8 0.6 0.05 40 force");
			Krim.cmd(s, src, "execute at @p run playsound minecraft:entity.puffer_fish.blow_out player @a ~ ~ ~ 1 1");
			Krim.aviso(s, src, "Volviste a tu tamaño normal", "gold");
			Registro.info("TAMANO", "Efecto terminado, tamaño normal");
		}
	}

	public static void aplicar(MinecraftServer s, ServerPlayer p, String usuario) {
		ServerLevel lvl = (ServerLevel) p.level();
		double altoLibre = altoLibre(lvl, p.blockPosition());

		List<Double> posibles = new ArrayList<>();
		for (double t : TAMANOS) if (t * ALTO_JUGADOR <= altoLibre - 0.1) posibles.add(t);
		if (posibles.isEmpty()) posibles.add(TAMANOS[0]);
		double t = posibles.get(ThreadLocalRandom.current().nextInt(posibles.size()));

		CommandSourceStack src = Krim.fuente(s);
		Krim.cmd(s, src, "attribute @p minecraft:scale modifier remove " + MOD); // si ya había uno, se reemplaza
		Krim.cmd(s, src, "attribute @p minecraft:scale modifier add " + MOD + " " + (t - 1.0) + " add_value");
		restante = DURACION;

		boolean grande = t > 1;
		String sonido = grande ? "minecraft:entity.puffer_fish.blow_up" : "minecraft:entity.puffer_fish.blow_out";
		String particula = grande ? "minecraft:explosion" : "minecraft:poof";
		Krim.cmd(s, src, "execute at @p run particle " + particula + " ~ ~1 ~ 0.6 0.8 0.6 0.05 " + (grande ? 3 : 40) + " force");
		Krim.cmd(s, src, "execute at @p run particle minecraft:dust{color:[1.0,0.82,0.25],scale:1.3} ~ ~1 ~ 0.6 1.0 0.6 0 40 force");
		Krim.cmd(s, src, "execute at @p run playsound " + sonido + " player @a ~ ~ ~ 1 " + (grande ? "0.6" : "1.6"));

		String apodo = t <= 0.3 ? "HORMIGA" : t < 1 ? "ENANO" : t <= 2 ? "GRANDOTE" : "GIGANTE";
		String veces = (t == Math.floor(t)) ? String.valueOf((int) t) : String.valueOf(t);
		Krim.avisoGrande(s, src, usuario + " te convirtió en " + apodo + " (x" + veces + ") por 30 s", grande ? "red" : "aqua");
		Registro.info("TAMANO", "Tamaño x" + veces + " (" + apodo + ") por " + usuario + " | alto libre " + altoLibre);
	}

	/** Bloques de aire seguidos desde los pies hacia arriba (máximo 7). */
	private static double altoLibre(ServerLevel lvl, BlockPos pies) {
		int n = 0;
		while (n < 7 && !lvl.getBlockState(pies.above(n)).isSuffocating(lvl, pies.above(n))) n++;
		return n;
	}
}
