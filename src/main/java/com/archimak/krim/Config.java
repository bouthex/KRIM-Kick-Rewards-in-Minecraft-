package com.archimak.krim;

import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** config/krim.properties (se crea solo la primera vez; si existe el viejo kickcreeper.properties, se copia). */
public final class Config {
	static final String TIERRA_DEF = "wolf,cat,parrot,horse,donkey,mule,llama,camel,fox,ocelot,panda,pig,cow,mooshroom,sheep,chicken,rabbit,frog,armadillo,sniffer,turtle,allay";
	static final String AGUA_DEF = "cod,salmon,tropical_fish,axolotl,squid,glow_squid,dolphin,tadpole";

	public static int puerto = 47110;

	// ---- Opciones de la pantalla de configuración (Mod Menu) ----
	/** Categorías de la ruleta que pueden salir. */
	public static final String[] CATEGORIAS = { "mobs", "castigos", "jefes", "funnys", "wachin", "preguntas", "parlante",
			"burlas", "mensajes", "locuras", "regalos" };
	public static final java.util.Map<String, Boolean> categorias = new java.util.LinkedHashMap<>();
	static { for (String k : CATEGORIAS) categorias.put(k, true); }
	public static boolean jumpscare = true;
	/** 0 instantáneo, 1 corto (como está), 2 un segundo, 3 dos segundos. */
	public static int avisoModo = 1;
	/** Tamaño de la rueda: 0 chica, 1 normal, 2 grande. */
	public static int tamanoRueda = 1;
	/** Vida mínima para que la ruleta saque mobs y castigos (en medios corazones; 0 = sin tregua). */
	public static int treguaVida = 8;
	public static boolean clicsRueda = true;

	public static boolean categoria(String k) { return categorias.getOrDefault(k, true); }
	/** 0 = automático (usa los phonks que trae el mod; si no hay, el de bloques musicales). */
	public static int phonkTemas = 0;
	public static int phonkSegundos = 10;
	public static int phonkBpm = 130;
	public static List<String> bloqueadas = new ArrayList<>();
	public static List<String> animalesTierra = lista(TIERRA_DEF);
	public static List<String> animalesAgua = lista(AGUA_DEF);

	static void cargar() {
		Path archivo = FabricLoader.getInstance().getConfigDir().resolve("krim.properties");
		Path viejo = FabricLoader.getInstance().getConfigDir().resolve("kickcreeper.properties");
		Properties p = new Properties();
		try {
			if (!Files.exists(archivo) && Files.exists(viejo)) {
				Files.copy(viejo, archivo);
				Registro.info("CONFIG", "Config copiada de kickcreeper.properties a krim.properties (podés borrar la vieja)");
			}
			if (Files.exists(archivo)) {
				try (Reader r = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) { p.load(r); }
			} else {
				p.setProperty("puerto", "47110");
				p.setProperty("palabras_bloqueadas", "");
				try (Writer w = Files.newBufferedWriter(archivo, StandardCharsets.UTF_8)) {
					p.store(w, "KRIM - palabras_bloqueadas separadas por coma (ej: palabra1,palabra2)");
				}
			}
			boolean agregar = false;
			if (p.getProperty("animales_tierra") == null) { p.setProperty("animales_tierra", TIERRA_DEF); agregar = true; }
			if (p.getProperty("animales_agua") == null) { p.setProperty("animales_agua", AGUA_DEF); agregar = true; }
			if (p.getProperty("phonk_temas") == null) { p.setProperty("phonk_temas", "0"); agregar = true; }
			if (p.getProperty("phonk_segundos") == null) { p.setProperty("phonk_segundos", "10"); agregar = true; }
			if (p.getProperty("phonk_bpm") == null) { p.setProperty("phonk_bpm", "130"); agregar = true; }
			if (agregar) {
				try (Writer w = Files.newBufferedWriter(archivo, StandardCharsets.UTF_8)) {
					p.store(w, "KRIM - palabras_bloqueadas y listas de animales separadas por coma. Animales: ids de Minecraft (ej: cow,pig)");
				}
			}
			puerto = Integer.parseInt(p.getProperty("puerto", "47110").trim());
			animalesTierra = lista(p.getProperty("animales_tierra", TIERRA_DEF));
			phonkTemas = Math.max(0, Math.min(30, Integer.parseInt(p.getProperty("phonk_temas", "0").trim())));
			phonkSegundos = Math.max(4, Math.min(20, Integer.parseInt(p.getProperty("phonk_segundos", "10").trim())));
			phonkBpm = Math.max(60, Math.min(200, Integer.parseInt(p.getProperty("phonk_bpm", "130").trim())));
			animalesAgua = lista(p.getProperty("animales_agua", AGUA_DEF));
			for (String s : p.getProperty("palabras_bloqueadas", "").split(",")) {
				if (!s.isBlank()) bloqueadas.add(s.trim().toLowerCase());
			}
			for (String k : CATEGORIAS) categorias.put(k, !"no".equals(p.getProperty("ruleta_" + k, "si").trim()));
			jumpscare = !"no".equals(p.getProperty("jumpscare", "si").trim());
			avisoModo = Math.max(0, Math.min(3, Integer.parseInt(p.getProperty("aviso_modo", "1").trim())));
			tamanoRueda = Math.max(0, Math.min(2, Integer.parseInt(p.getProperty("tamano_rueda", "1").trim())));
			treguaVida = Math.max(0, Math.min(12, Integer.parseInt(p.getProperty("tregua_vida", "8").trim())));
			clicsRueda = !"no".equals(p.getProperty("clics_rueda", "si").trim());
		} catch (Exception e) {
			Registro.error("CONFIG", "No se pudo leer la config, uso valores por defecto", e);
		}
	}

	/** Guarda las opciones de la pantalla sin pisar el resto del archivo. */
	public static void guardar() {
		Path archivo = FabricLoader.getInstance().getConfigDir().resolve("krim.properties");
		Properties p = new Properties();
		try {
			if (Files.exists(archivo)) try (Reader r = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) { p.load(r); }
			for (String k : CATEGORIAS) p.setProperty("ruleta_" + k, categoria(k) ? "si" : "no");
			p.setProperty("jumpscare", jumpscare ? "si" : "no");
			p.setProperty("aviso_modo", String.valueOf(avisoModo));
			p.setProperty("tamano_rueda", String.valueOf(tamanoRueda));
			p.setProperty("tregua_vida", String.valueOf(treguaVida));
			p.setProperty("clics_rueda", clicsRueda ? "si" : "no");
			try (Writer w = Files.newBufferedWriter(archivo, StandardCharsets.UTF_8)) {
				p.store(w, "KRIM - se puede editar desde Mod Menu");
			}
			Registro.info("CONFIG", "Configuración guardada desde Mod Menu");
		} catch (Exception e) {
			Registro.error("CONFIG", "No se pudo guardar la configuración", e);
		}
	}

	private static List<String> lista(String v) {
		List<String> l = new ArrayList<>();
		for (String s : v.split(",")) {
			String t = s.trim().toLowerCase().replace("minecraft:", "");
			if (t.matches("[a-z0-9_]+")) l.add(t);
		}
		return l;
	}

	/** Deja solo letras, números y signos básicos (sin comillas ni barras), máximo 32 caracteres (o el máximo indicado). */
	public static String limpiar(String t) { return limpiar(t, 32); }

	public static String limpiar(String t, int max) {
		if (t == null) return "";
		t = t.replaceAll("[^\\p{L}\\p{N} _\\-.,!¡?¿:()]", "").replaceAll("\\s+", " ").trim();
		if (t.length() > max) t = t.substring(0, max).trim();
		String bajo = t.toLowerCase();
		for (String b : bloqueadas) if (bajo.contains(b)) {
			Registro.aviso("FILTRO", "Texto bloqueado por palabra prohibida: \"" + t + "\"");
			return "";
		}
		return t;
	}
}
