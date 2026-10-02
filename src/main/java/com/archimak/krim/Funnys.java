package com.archimak.krim;

import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Sección rosa de la ruleta: edits de TikTok con phonk, créditos finales, apagón, cámara lenta, jumpscare y risas.
 * El phonk está armado nota por nota con los sonidos de bloque musical de Minecraft (cencerro, bombo, platillo,
 * caja y bajo), así que no usa audio de nadie y suena igual en cualquier PC.
 * Durante el edit, los créditos y el apagón el mundo se congela con /tick freeze y se descongela al final.
 */
public final class Funnys {
	private static int contador;

	/** Un tema de phonk: 16 pasos que se repiten 2 veces. Las alturas del cencerro son la melodía (0 = silencio). */
	private record Tema(String nombre, int paso, int[] bombo, int[] caja, double[] cencerro, double bajo) {}

	private static final List<Tema> TEMAS = List.of(
			new Tema("Phonk del Creeper", 4, new int[] { 0, 3, 6, 8, 11, 14 }, new int[] { 4, 12 },
					new double[] { 1.0, 0, 1.19, 0, 1.0, 1.33, 0, 1.19, 1.0, 0, 0.89, 0, 1.0, 0, 0.75, 0 }, 0.6),
			new Tema("Montagem Bout", 4, new int[] { 0, 3, 7, 10, 12 }, new int[] { 4, 8, 13 },
					new double[] { 1.5, 1.5, 0, 1.68, 0, 1.5, 1.33, 0, 1.5, 0, 1.68, 1.78, 0, 1.5, 0, 1.33 }, 0.7),
			new Tema("Sigma Phonk", 5, new int[] { 0, 8, 10 }, new int[] { 4, 12 },
					new double[] { 1.0, 1.19, 1.33, 1.19, 1.0, 0, 0.89, 1.0, 1.19, 1.0, 0.89, 0.75, 0.89, 0, 1.0, 0 }, 0.5),
			new Tema("Drift Phonk Argento", 4, new int[] { 0, 4, 7, 8, 12, 15 }, new int[] { 4, 12 },
					new double[] { 2.0, 0, 1.78, 0, 1.5, 0, 1.33, 1.5, 1.19, 0, 1.33, 0, 1.0, 1.19, 0.89, 0 }, 0.55));

	private static final List<String> CAPTIONS = List.of("MOMENTO SIGMA", "EL CHAT CUANDO MORÍS:", "NO WAY", "ESTO ES CINE",
			"AURA +1000", "AURA -1000", "BRO PENSÓ QUE PODÍA", "POV: SOS %y", "%y MODO DIOS", "%y DESPUÉS DE MORIR:",
			"CUANDO EL CREEPER TE SALUDA", "LA CARA DEL CHAT", "NADIE:  %y:", "FINAL ÉPICO", "EL MANCO MÁS FACHERO");

	private static ThreadLocalRandom r() { return ThreadLocalRandom.current(); }
	private static void cmd(MinecraftServer s, String c) { Krim.cmd(s, Krim.fuente(s), c); }

	/** La ruleta espera mientras hay un funny en pantalla. */
	static boolean activo() {
		FunnyEstado.Efecto e = FunnyEstado.actual;
		return e != null && RuedaEstado.ahoraMs() - e.inicioMs() < e.duracionMs() + 500;
	}

	/** Por si el juego se cerró en el medio de un funny. */
	static void limpiar(MinecraftServer s) {
		FunnyEstado.actual = null;
		cmd(s, "tick unfreeze");
		cmd(s, "tick rate 20");
	}

	private static void congelar(MinecraftServer s, int ticks) {
		cmd(s, "tick freeze");
		Ruleta.despues(ticks, sv -> cmd(sv, "tick unfreeze"));
	}

	private static void sonar(MinecraftServer s, String sonido, double volumen, double tono) {
		cmd(s, "execute at @p run playsound minecraft:" + sonido + " master @p ~ ~ ~ " + volumen + " " + tono);
	}

	// ------------------------------------------------------------------ edit de TikTok
	static void edit(MinecraftServer s, String us, String yo) {
		Tema t = TEMAS.get(r().nextInt(TEMAS.size()));
		int pasos = 32, durTicks = pasos * t.paso() + 10;
		List<Integer> golpes = new ArrayList<>();
		for (int i = 0; i < pasos; i++) {
			int paso = i % 16;
			final int cuando = i * t.paso() + 4;
			boolean bombo = contiene(t.bombo(), paso), caja = contiene(t.caja(), paso);
			double cen = t.cencerro()[paso];
			if (bombo) golpes.add(cuando * 50);
			Ruleta.despues(cuando, sv -> {
				if (bombo) { sonar(sv, "block.note_block.basedrum", 1, 0.8); sonar(sv, "block.note_block.bass", 1, t.bajo()); }
				if (caja) sonar(sv, "block.note_block.snare", 1, 1.0);
				sonar(sv, "block.note_block.hat", 0.5, 1.6);
				if (cen > 0) sonar(sv, "block.note_block.cow_bell", 1, cen);
			});
		}
		Ruleta.despues(4, sv -> sonar(sv, "entity.warden.sonic_boom", 0.6, 1.4));
		String cap = CAPTIONS.get(r().nextInt(CAPTIONS.size())).replace("%y", yo.toUpperCase());
		int[] g = golpes.stream().mapToInt(Integer::intValue).toArray();
		FunnyEstado.actual = new FunnyEstado.Efecto(++contador, "edit", RuedaEstado.ahoraMs(), durTicks * 50, cap,
				"edit de " + us, "♪ " + t.nombre(), g, new String[0], r().nextInt(3));
		congelar(s, durTicks);
		Registro.info("FUNNY", "Edit de TikTok (" + t.nombre() + ") de " + us);
	}

	private static boolean contiene(int[] a, int v) { for (int x : a) if (x == v) return true; return false; }

	// ------------------------------------------------------------------ créditos finales
	static void creditos(MinecraftServer s, String us, String yo) {
		String[] lineas = {
				"KRIM PRESENTA", "", "Una película de " + us, "", "Protagonista", yo + " (el manco)", "",
				"Actor de reparto", "Un creeper con nombre", "", "Dirección de fotografía", "El chat", "",
				"Efectos especiales", "La ruleta", "", "Catering", "El guisito de la abuela", "",
				"Ningún animal fue lastimado", "(bueno, alguno sí)", "", "Gracias por mirar", "", "", "¿FIN?" };
		FunnyEstado.actual = new FunnyEstado.Efecto(++contador, "creditos", RuedaEstado.ahoraMs(), 11000, "", "", "",
				new int[0], lineas, 0);
		sonar(s, "music_disc.far", 1, 1);
		Ruleta.despues(220, sv -> cmd(sv, "stopsound @p record"));
		congelar(s, 220);
	}

	// ------------------------------------------------------------------ se cortó la luz
	static void apagon(MinecraftServer s, String us) {
		FunnyEstado.actual = new FunnyEstado.Efecto(++contador, "apagon", RuedaEstado.ahoraMs(), 6000, "", "", "",
				new int[0], new String[] { "Edesur te cortó la luz...", "Volvió la luz. Por ahora." }, 0);
		sonar(s, "block.lever.click", 1, 0.6);
		sonar(s, "entity.generic.extinguish_fire", 1, 0.8);
		Ruleta.despues(84, sv -> sonar(sv, "block.lever.click", 1, 1.2));
		congelar(s, 120);
	}

	// ------------------------------------------------------------------ jumpscare (sin congelar)
	static void jumpscare(MinecraftServer s) {
		FunnyEstado.actual = new FunnyEstado.Efecto(++contador, "jumpscare", RuedaEstado.ahoraMs(), 1300, "", "", "",
				new int[0], new String[0], r().nextInt(2));
		sonar(s, "entity.creeper.primed", 1, 1.0);
		sonar(s, "entity.generic.explode", 0.9, 0.7);
		sonar(s, "entity.ghast.scream", 1, 0.8);
	}

	// ------------------------------------------------------------------ cámara lenta
	static void lenta(MinecraftServer s) {
		cmd(s, "tick rate 6");
		cmd(s, "title @p times 5 40 10");
		cmd(s, "title @p subtitle {text:\"modo película\",color:\"light_purple\"}");
		cmd(s, "title @p title {text:\"CÁMARA LENTA\",color:\"light_purple\",bold:true}");
		sonar(s, "entity.ender_dragon.growl", 0.8, 0.5);
		Ruleta.despues(36, sv -> { cmd(sv, "tick rate 20"); sonar(sv, "entity.player.levelup", 1, 0.6); });
	}

	// ------------------------------------------------------------------ risas enlatadas
	static void risas(MinecraftServer s) {
		cmd(s, "title @p actionbar {text:\"*risas del público*\",color:\"light_purple\",italic:true}");
		for (int i = 0; i < 10; i++) {
			double tono = 0.9 + r().nextDouble() * 0.6;
			Ruleta.despues(i * 3 + r().nextInt(3), sv -> sonar(sv, "entity.witch.celebrate", 0.8, tono));
		}
		for (int i = 0; i < 4; i++) Ruleta.despues(10 + i * 6, sv -> sonar(sv, "entity.villager.celebrate", 0.7, 1.2));
	}

	private Funnys() { }
}
