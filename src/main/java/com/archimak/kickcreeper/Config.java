package com.archimak.kickcreeper;

import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** config/kickcreeper.properties (se crea solo la primera vez). */
public final class Config {
	public static int puerto = 47110;
	public static List<String> bloqueadas = new ArrayList<>();

	static void cargar() {
		Path archivo = FabricLoader.getInstance().getConfigDir().resolve("kickcreeper.properties");
		Properties p = new Properties();
		try {
			if (Files.exists(archivo)) {
				try (Reader r = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) { p.load(r); }
			} else {
				p.setProperty("puerto", "47110");
				p.setProperty("palabras_bloqueadas", "");
				try (Writer w = Files.newBufferedWriter(archivo, StandardCharsets.UTF_8)) {
					p.store(w, "Kick Creeper - palabras_bloqueadas separadas por coma (ej: palabra1,palabra2)");
				}
			}
			puerto = Integer.parseInt(p.getProperty("puerto", "47110").trim());
			for (String s : p.getProperty("palabras_bloqueadas", "").split(",")) {
				if (!s.isBlank()) bloqueadas.add(s.trim().toLowerCase());
			}
		} catch (Exception e) {
			Registro.error("CONFIG", "No se pudo leer la config, uso valores por defecto", e);
		}
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
