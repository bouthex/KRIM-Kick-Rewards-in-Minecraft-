package com.archimak.kickcreeper;

import net.fabricmc.loader.api.FabricLoader;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Log propio: .minecraft/logs/kickcreeper.log (se agrega, no se borra).
 * Cada línea: fecha hora | NIVEL | CATEGORIA | mensaje. Si supera 2 MB se renombra a kickcreeper.old.log.
 * Además todo sale en latest.log con el prefijo [KickCreeper].
 */
public final class Registro {
	private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final long MAX = 2L * 1024 * 1024;
	private static Path archivo;

	static synchronized void iniciar() {
		try {
			Path dir = FabricLoader.getInstance().getGameDir().resolve("logs");
			Files.createDirectories(dir);
			archivo = dir.resolve("kickcreeper.log");
			if (Files.exists(archivo) && Files.size(archivo) > MAX) {
				Files.move(archivo, dir.resolve("kickcreeper.old.log"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (Exception e) {
			KickCreeper.LOGGER.warn("[KickCreeper] No se pudo preparar kickcreeper.log", e);
		}
	}

	public static void info(String cat, String msg) { escribir("INFO ", cat, msg, null); }
	public static void aviso(String cat, String msg) { escribir("AVISO", cat, msg, null); }
	public static void error(String cat, String msg, Throwable t) { escribir("ERROR", cat, msg, t); }

	private static synchronized void escribir(String nivel, String cat, String msg, Throwable t) {
		String linea = LocalDateTime.now().format(F) + " | " + nivel + " | " + String.format("%-8s", cat) + " | " + msg;
		if (t != null) {
			StringWriter sw = new StringWriter();
			t.printStackTrace(new PrintWriter(sw));
			linea += System.lineSeparator() + sw;
			KickCreeper.LOGGER.error("[KickCreeper] {} | {}", cat, msg, t);
		} else if (nivel.startsWith("AVISO")) {
			KickCreeper.LOGGER.warn("[KickCreeper] {} | {}", cat, msg);
		} else {
			KickCreeper.LOGGER.info("[KickCreeper] {} | {}", cat, msg);
		}
		if (archivo == null) return;
		try {
			Files.writeString(archivo, linea + System.lineSeparator(), StandardCharsets.UTF_8,
					StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (Exception ignored) { }
	}
}
