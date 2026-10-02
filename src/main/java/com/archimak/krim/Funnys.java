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

	/**
	 * Música que te sigue: volumen mínimo 1 (se escucha igual aunque te muevas, no queda "clavada" en un lugar)
	 * y se corta en TODOS los canales al terminar (antes se cortaba en otro canal y seguía sonando: bug 2.8.2).
	 */
	static void musica(MinecraftServer s, String sonido, int ticks) {
		cmd(s, "execute at @p run playsound minecraft:" + sonido + " record @p ~ ~ ~ 1 1 1");
		Ruleta.despues(ticks, sv -> cmd(sv, "stopsound @a * minecraft:" + sonido));
	}

	private static void sonar(MinecraftServer s, String sonido, double volumen, double tono) {
		cmd(s, "execute at @p run playsound minecraft:" + sonido + " master @p ~ ~ ~ " + volumen + " " + tono);
	}

	// ------------------------------------------------------------------ edit de TikTok
	static void edit(MinecraftServer s, String us, String yo) {
		String cap = CAPTIONS.get(r().nextInt(CAPTIONS.size())).replace("%y", yo.toUpperCase());
		if (temas() > 0) { editConTema(s, us, cap); return; }
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
		int[] g = golpes.stream().mapToInt(Integer::intValue).toArray();
		FunnyEstado.actual = new FunnyEstado.Efecto(++contador, "edit", RuedaEstado.ahoraMs(), durTicks * 50, cap,
				"edit de " + us, "♪ " + t.nombre(), g, new String[0], r().nextInt(3));
		congelar(s, durTicks);
		Registro.info("FUNNY", "Edit de TikTok (" + t.nombre() + ") de " + us);
	}

	private static int temasEnElMod = -1;

	/**
	 * Cuántos phonks trae el mod adentro (los mete GitHub al compilar desde la carpeta "phonks" del repo).
	 * Si phonk_temas en la config es mayor a 0, manda la config.
	 */
	static int temas() {
		if (Config.phonkTemas > 0) return Config.phonkTemas;
		if (temasEnElMod >= 0) return temasEnElMod;
		int n = 0;
		try {
			var mod = net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer(Krim.MOD_ID);
			if (mod.isPresent()) {
				var carpeta = mod.get().findPath("assets/krim/sounds/phonk");
				if (carpeta.isPresent()) {
					try (var archivos = java.nio.file.Files.list(carpeta.get())) {
						n = (int) archivos.filter(x -> x.getFileName().toString().matches("phonk\\d+\\.ogg")).count();
					}
				}
			}
		} catch (Exception e) {
			Registro.error("FUNNY", "No se pudieron contar los phonks del mod", e);
		}
		temasEnElMod = Math.min(30, n);
		Registro.info("FUNNY", temasEnElMod > 0 ? "Phonks dentro del mod: " + temasEnElMod : "Sin phonks propios: se usa el phonk de bloques musicales");
		return temasEnElMod;
	}

	/** Edit con uno de los phonks que trae el mod adentro. */
	private static void editConTema(MinecraftServer s, String us, String cap) {
		int n = 1 + r().nextInt(temas());
		String sonido = "krim:phonk" + n;
		int durMs = Config.phonkSegundos * 1000;
		int beat = Math.round(60000f / Config.phonkBpm);
		int[] g = new int[durMs / beat];
		for (int i = 0; i < g.length; i++) g[i] = i * beat;
		cmd(s, "execute at @p run playsound " + sonido + " master @p ~ ~ ~ 1 1");
		Ruleta.despues(Config.phonkSegundos * 20, sv -> cmd(sv, "stopsound @a * " + sonido));
		FunnyEstado.actual = new FunnyEstado.Efecto(++contador, "edit", RuedaEstado.ahoraMs(), durMs, cap,
				"edit de " + us, "♪ phonk " + n, g, new String[0], r().nextInt(3));
		congelar(s, Config.phonkSegundos * 20);
		Registro.info("FUNNY", "Edit de TikTok con phonk propio " + n + " de " + us);
	}

	private static boolean contiene(int[] a, int v) { for (int x : a) if (x == v) return true; return false; }

	// ------------------------------------------------------------------ créditos finales (aleatorios)
	private static final List<String> NOMBRES = List.of("clover", "hani", "archimak", "bout", "kaki", "elio", "bruno", "amilcar",
			"tarta", "celes", "sofi", "ibai", "el chat", "un creeper", "Steve", "Alex", "Herobrine", "un aldeano", "la abuela",
			"el perro del vecino", "un zombie bebé", "el enderman", "una gallina", "el warden (de lejos)", "un piglin",
			"el phantom", "la vaca", "un esqueleto sin arco", "el loro", "un ajolote", "el gato", "la cabra que grita",
			"el tío que no vino", "el de sistemas", "tu ex", "el kiosquero", "el remisero", "un tipo random",
			"el que canjeó (%u)");
	private static final List<String> PRESENTA = List.of("BOUT PRESENTA", "KICK PRESENTA", "UNA PRODUCCIÓN DE %n",
			"%n PRESENTA", "EL CHAT PRESENTA", "BOUT FILMS PRESENTA", "KICK STUDIOS PRESENTA", "UNA PELÍCULA DE %u",
			"PRODUCCIONES %N", "%u Y EL CHAT PRESENTAN");
	private static final List<String> TITULOS = List.of("EL MANCO", "RÁPIDO Y MANCO", "LA VENGANZA DEL CREEPER",
			"MI POBRE ANGELITO 4: SOLO EN EL NETHER", "EL SEÑOR DE LOS PICOS", "BOUT: LA PELÍCULA", "MISIÓN: NO MORIR",
			"TODO TODO EN TODOS LOS CHUNKS", "LA CASA DE TIERRA", "EL PADRINO DEL SERVER", "RELATOS SALVAJES (EN MINECRAFT)",
			"VOLVER AL SPAWN", "EL ÚLTIMO DIAMANTE", "PIRATAS DEL CARIBE: EL COFRE VACÍO", "TITANIC 2: SE HUNDIÓ EN LAVA",
			"HARRY POTTER Y EL GUISITO DE LA ABUELA", "STAR WARS: EL ENDERMAN CONTRAATACA", "EL CONJURO DE LA RULETA");
	private static final List<String> ROLES = List.of("Protagonista", "Actor de reparto", "Dirección", "Guion",
			"Dirección de fotografía", "Efectos especiales", "Catering", "Vestuario", "Maquillaje", "Doble de riesgo",
			"Doble de cuerpo", "Música original", "Sonido", "Producción ejecutiva", "Coreografía", "Asesor legal",
			"Asesor de moda", "Peluquería", "Iluminación", "Montaje", "Casting", "Seguridad", "Limpieza del set",
			"Chofer", "Psicólogo del elenco", "Entrenador de creepers", "Domador de gallinas", "Responsable de las explosiones",
			"Inventor de la ruleta", "Contador de muertes", "Encargado de la lava", "Experto en manquear",
			"Proveedor de antorchas", "Testigo de todo", "Víctima principal", "Mejor amigo del villano",
			"El que no hizo nada", "El que llegó tarde", "Fan número 1", "Hater número 1");
	private static final List<String> CHISTES = List.of("Ningún animal fue lastimado (bueno, alguno sí)",
			"Filmado íntegramente en un mundo de tierra", "Ningún diamante fue encontrado durante el rodaje",
			"Presupuesto total: 3 esmeraldas y un pan", "Basado en hechos reales (lamentablemente)",
			"El protagonista murió 14 veces durante el rodaje", "Las escenas de acción NO fueron hechas por un profesional",
			"Ningún creeper fue pagado por su actuación", "El guisito de la abuela no tenía nada raro (mentira)",
			"Todos los personajes son ficticios. Menos el manco", "Rodado sin dormir ni una noche",
			"Se usaron 0 antorchas. Por eso todo estaba oscuro", "El aldeano cobró más que el protagonista",
			"La gallina pidió aparecer en los créditos", "Ninguna cama explotó... en el Overworld",
			"Gracias a mamá por bancar el stream", "Gracias al chat por los puntos", "Gracias a nadie, en realidad",
			"Dedicado a todos los que murieron por un creeper", "Esta película no tiene escena post-créditos. O sí");
	private static final List<String> FINALES = List.of("¿FIN?", "FIN", "CONTINUARÁ...", "NO HAY SEGUNDA PARTE", "FIN (POR AHORA)",
			"VOLVEMOS DESPUÉS DE ESTOS MENSAJES", "¿Y ESO FUE TODO?", "GRACIAS POR NADA", "FIN. ANDÁ A DORMIR");

	static void creditos(MinecraftServer s, String us, String yo) {
		List<String> nombres = new ArrayList<>();
		for (String n : NOMBRES) nombres.add(n.replace("%u", us));
		java.util.Collections.shuffle(nombres);
		List<String> roles = new ArrayList<>(ROLES);
		java.util.Collections.shuffle(roles);
		List<String> chistes = new ArrayList<>(CHISTES);
		java.util.Collections.shuffle(chistes);

		String quien = nombres.get(r().nextInt(nombres.size()));
		List<String> l = new ArrayList<>();
		l.add(PRESENTA.get(r().nextInt(PRESENTA.size())).replace("%n", quien).replace("%N", quien.toUpperCase()).replace("%u", us));
		l.add("");
		l.add("«" + TITULOS.get(r().nextInt(TITULOS.size())) + "»");
		l.add(""); l.add("");
		l.add("Protagonista"); l.add(yo + " (" + uno(List.of("el manco", "el de siempre", "a su pesar", "sin saberlo", "en su peor momento")) + ")");
		l.add("");
		int k = 0;
		for (int i = 0; i < 16; i++) {
			String rol = roles.get(i);
			if (rol.equals("Protagonista")) continue;
			l.add(rol);
			l.add(nombres.get(k++ % nombres.size()));
			if (r().nextInt(4) == 0) l.add(nombres.get(k++ % nombres.size()));
			l.add("");
			if (i % 5 == 4) { l.add(chistes.get(i / 5)); l.add(""); }
		}
		l.add(chistes.get(4)); l.add(""); l.add(chistes.get(5)); l.add(""); l.add("");
		l.add("Gracias por mirar"); l.add(""); l.add(""); l.add(FINALES.get(r().nextInt(FINALES.size())));
		String[] lineas = l.toArray(new String[0]);
		int durMs = 20000;
		FunnyEstado.actual = new FunnyEstado.Efecto(++contador, "creditos", RuedaEstado.ahoraMs(), durMs, "", "", "",
				new int[0], lineas, 0);
		String tema = uno(List.of("music_disc.far", "music_disc.strad", "music_disc.mellohi", "music_disc.wait", "music_disc.chirp"));
		musica(s, tema, durMs / 50);
		congelar(s, durMs / 50);
	}

	private static String uno(List<String> l) { return l.get(r().nextInt(l.size())); }

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
		int variante = r().nextInt(3); // 0 creeper, 1 sonrisa macabra, 2 grito
		FunnyEstado.actual = new FunnyEstado.Efecto(++contador, "jumpscare", RuedaEstado.ahoraMs(), 1500, "", "", "",
				new int[0], new String[0], variante);
		// grito: varios sonidos a la vez para que suene a alarido
		sonar(s, "entity.ghast.scream", 1, 0.7);
		sonar(s, "entity.enderman.scream", 1, 0.6);
		sonar(s, "entity.goat.screaming.ambient", 1, 0.8);
		if (variante == 0) sonar(s, "entity.generic.explode", 1, 0.7);
		else sonar(s, "entity.warden.roar", 1, 1.3);
	}

	// ------------------------------------------------------------------ bardeo gigante al que canjeó
	static void bardeo(MinecraftServer s, String us) {
		String b = Bardeos.generar(us);
		FunnyEstado.actual = new FunnyEstado.Efecto(++contador, "bardeo", RuedaEstado.ahoraMs(), 6500, b, "— el chat", "",
				new int[0], new String[0], r().nextInt(3));
		sonar(s, "block.anvil.land", 1, 0.6);
		sonar(s, "entity.villager.no", 1, 0.8);
		Ruleta.despues(8, sv -> sonar(sv, "entity.witch.celebrate", 1, 1.0));
		cmd(s, "tellraw @a [{text:\"BARDEO: \",color:\"light_purple\",bold:true},{text:\"" + b.replace("\"", "'") + "\",color:\"white\"}]");
		Registro.info("FUNNY", "Bardeo: " + b);
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
