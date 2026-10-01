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
			if (agregar) {
				try (Writer w = Files.newBufferedWriter(archivo, StandardCharsets.UTF_8)) {
					p.store(w, "KRIM - palabras_bloqueadas y listas de animales separadas por coma. Animales: ids de Minecraft (ej: cow,pig)");
				}
			}
			puerto = Integer.parseInt(p.getProperty("puerto", "47110").trim());
			animalesTierra = lista(p.getProperty("animales_tierra", TIERRA_DEF));
			animalesAgua = lista(p.getProperty("animales_agua", AGUA_DEF));
			for (String s : p.getProperty("palabras_bloqueadas", "").split(",")) {
				if (!s.isBlank()) bloqueadas.add(s.trim().toLowerCase());
			}
		} catch (Exception e) {
			Registro.error("CONFIG", "No se pudo leer la config, uso valores por defecto", e);
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
