package com.archimak.krim;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Mapas dibujados por el chat. Cada mapa es un lienzo de 128x128 pintado por código
 * (formas, colores, rotación, adornos y textos al azar), así que no hay dos iguales.
 * El mapa se crea "lejos" del mundo y sin seguimiento, así nunca se pinta con el terreno.
 */
public final class Dibujos {
	private static final int N = 128;
	// Ids de color de mapa de Minecraft (el byte final es id*4 + brillo)
	static final int NEGRO = 29, BLANCO = 8, ROSA = 20, ROJO = 28, AMARILLO = 18, MARRON = 26, VERDE = 27, CELESTE = 17,
			ORO = 30, PIEL = 36, GRIS = 22, AZUL = 25, VIOLETA = 24, NARANJA = 15, ARENA = 2, VERDE_CLARO = 19, MAGENTA = 16;
	private static final int[] FONDOS = { ARENA, BLANCO, CELESTE, AMARILLO, VERDE_CLARO, ROSA, GRIS, 14 };

	private static ThreadLocalRandom r() { return ThreadLocalRandom.current(); }
	private static int uno(int... v) { return v[r().nextInt(v.length)]; }
	private static <T> T uno(List<T> l) { return l.get(r().nextInt(l.size())); }
	private static byte c(int id) { return (byte) (id * 4 + 2); }

	static final List<String> LEYENDAS = List.of("PUTO EL QUE LEE", "PARA VOS", "TOMA", "TE QUIERO", "CHUPALA", "MANCO",
			"REGALO", "ANDA A CAGAR", "TOMATELA", "SALUDOS", "DE NADA", "OBRA MAESTRA", "SOS VOS", "MIRALO BIEN",
			"AUTORRETRATO", "PARA TU VIEJA", "GUARDALO", "NO LO TIRES", "100% REAL", "NOOB", "BOLUDO", "GIL",
			"QUE MIRAS", "ESTE SOS VOS", "PRESTALO", "FIRMADO: EL CHAT");

	/** Crea el mapa dibujado y se lo da al jugador. Devuelve qué dibujo salió. */
	public static String dar(ServerLevel lvl, ServerPlayer p, String usuario) {
		ItemStack mapa = MapItem.create(lvl, 25_000_000 + r().nextInt(2000) * 128, 25_000_000, (byte) 0, false, false);
		MapItemSavedData datos = MapItem.getSavedData(mapa.get(DataComponents.MAP_ID), lvl);
		if (datos == null) {
			Registro.aviso("MAPA", "No se pudo crear el mapa dibujado");
			return null;
		}
		byte[] px = new byte[N * N];
		int fondo = uno(FONDOS);
		java.util.Arrays.fill(px, c(fondo));
		String tipo;
		int x = r().nextInt(100);
		if (x < 38) tipo = pene(px, fondo);
		else if (x < 52) tipo = dedo(px);
		else if (x < 64) tipo = caca(px);
		else if (x < 80) tipo = textoGrande(px, fondo, usuario);
		else if (x < 88) tipo = ele(px);
		else if (x < 95) tipo = creeper(px);
		else tipo = corazon(px);
		adornos(px, fondo);
		if (r().nextInt(100) < 55) texto(px, "DE: " + usuario, 64, 3, 1, NEGRO, true);

		for (int y = 0; y < N; y++)
			for (int xx = 0; xx < N; xx++) datos.setColor(xx, y, px[xx + y * N]);

		String nombre = uno(List.of("Mapa del tesoro de %u", "Obra de arte de %u", "Retrato hecho por %u",
				"Dibujito de %u", "Regalo artístico de %u", "Cuadro original de %u", "Mapa secreto de %u",
				"El Picasso del chat: %u", "Arte de %u (no robar)")).replace("%u", usuario);
		mapa.set(DataComponents.CUSTOM_NAME, Component.literal(nombre));
		mapa.set(DataComponents.LORE, new ItemLore(List.of(Component.literal("Dibujado por " + usuario + " en la ruleta"))));
		if (!p.getInventory().add(mapa)) p.drop(mapa, false);
		return tipo;
	}

	// ------------------------------------------------------------------ dibujos
	private static String pene(byte[] px, int fondo) {
		int piel = uno(ROSA, ROSA, PIEL, MARRON, ORO, VERDE, CELESTE, VIOLETA);
		int punta = piel == ROSA ? uno(ROJO, MAGENTA) : uno(ROSA, ROJO);
		double ang = Math.toRadians(r().nextInt(-40, 41));
		boolean alas = r().nextInt(100) < 22, corona = r().nextInt(100) < 18, anteojos = r().nextInt(100) < 15;
		double ca = Math.cos(ang), sa = Math.sin(ang);
		int cx = 64, cy = 62;
		for (int y = 0; y < N; y++)
			for (int x = 0; x < N; x++) {
				double dx = x - cx, dy = y - cy;
				double u = dx * ca + dy * sa, v = -dx * sa + dy * ca;
				int col = -1;
				for (int capa = 0; capa < 2; capa++) {
					double o = capa == 0 ? 3 : 0;
					boolean bolas = Math.hypot(u + 13, v - 26) <= 14 + o || Math.hypot(u - 13, v - 26) <= 14 + o;
					boolean tronco = Math.abs(u) <= 10 + o && v >= -24 && v <= 24;
					boolean cabeza = elipse(u, v + 27, 13 + o, 11 + o);
					boolean ala = alas && (elipse(u + 28, v + 2, 15 + o, 6 + o) || elipse(u - 28, v + 2, 15 + o, 6 + o));
					boolean coro = corona && v <= -36 + o && v >= -48 - o && Math.abs(u) <= 10 + o
							&& (v >= -40 || (Math.abs(u) % 7) < 3 + o);
					if (capa == 0) {
						if (bolas || tronco || cabeza || ala || coro) col = NEGRO;
					} else {
						if (ala) col = BLANCO;
						if (bolas || tronco) col = piel;
						if (cabeza) col = punta;
						if (coro) col = ORO;
						if (cabeza && Math.abs(u) <= 1 && v >= -33 && v <= -28) col = NEGRO;
						if (anteojos && cabeza && (Math.hypot(u + 5, v + 26) <= 3.5 || Math.hypot(u - 5, v + 26) <= 3.5)) col = NEGRO;
					}
				}
				if (col >= 0) px[x + y * N] = c(col);
			}
		leyenda(px, fondo);
		return "pene" + (alas ? " con alas" : "") + (corona ? " con corona" : "") + (anteojos ? " con anteojos" : "");
	}

	private static final String[] DEDO = {
			"......OO........", ".....OXXO.......", ".....OXXO.......", ".....OXXO.......", ".....OXXO.......",
			"..OO.OXXOOO.....", ".OXXOOXXOXXO....", ".OXXOOXXOXXOO...", ".OXXXXXXXXXXXO..", ".OXXXXXXXXXXXO..",
			".OXXXXXXXXXXXO..", "..OXXXXXXXXXO...", "..OXXXXXXXXXO...", "...OXXXXXXXO....", "...OXXXXXXXO....",
			"...OOOOOOOOO...." };

	private static String dedo(byte[] px) {
		int piel = uno(PIEL, ROSA, MARRON, ORO, VERDE);
		int esc = 6, ox = 20 + r().nextInt(-6, 7), oy = 4;
		for (int fy = 0; fy < 16; fy++)
			for (int fx = 0; fx < 16; fx++) {
				char ch = DEDO[fy].charAt(fx);
				if (ch == '.') continue;
				rect(px, ox + fx * esc, oy + fy * esc, esc, esc, ch == 'O' ? NEGRO : piel);
			}
		texto(px, uno(List.of("PARA VOS", "TOMA", "SALUDOS", "CON AMOR", "PUTO EL QUE LEE")), 64, 110, 2, NEGRO, true);
		return "dedo";
	}

	private static String caca(byte[] px) {
		int marron = uno(MARRON, MARRON, ORO, VERDE);
		int[][] capas = { { 64, 92, 44, 15 }, { 64, 72, 33, 14 }, { 64, 54, 22, 12 }, { 66, 40, 9, 8 } };
		for (int[] e : capas) elipse(px, e[0], e[1], e[2] + 3, e[3] + 3, NEGRO);
		for (int[] e : capas) elipse(px, e[0], e[1], e[2], e[3], marron);
		circulo(px, 54, 70, 7, BLANCO); circulo(px, 74, 70, 7, BLANCO);
		circulo(px, 55 + r().nextInt(-2, 3), 71, 3, NEGRO); circulo(px, 75 + r().nextInt(-2, 3), 71, 3, NEGRO);
		boolean triste = r().nextBoolean();
		for (int x = 52; x <= 76; x++) {
			int y = triste ? 92 - (int) (Math.pow(x - 64, 2) / 30) : 84 + (int) (Math.pow(x - 64, 2) / 30);
			rect(px, x, y, 1, 2, NEGRO);
		}
		texto(px, uno(List.of("SOS VOS", "TU JUEGO", "TU CASA", "TU BASE", "AUTORRETRATO", "ESTE SOS VOS")), 64, 113, 2, NEGRO, true);
		return "caca";
	}

	private static String textoGrande(byte[] px, int fondo, String usuario) {
		List<String> frases = List.of("PUTO EL QUE LEE", "MANCO", "NOOB", "GIL", "BOLUDO", "CHUPALA", "SOS MALO",
				"TOMATELA", "ANDA A CAGAR", "QUE MIRAS", "PELOTUDO", "SALAME", "TROMPA", "CAGON", "ZAPALLO");
		String f = uno(frases);
		int borde = uno(NEGRO, ROJO, AZUL, VIOLETA, ORO);
		rect(px, 0, 0, N, 5, borde); rect(px, 0, N - 5, N, 5, borde); rect(px, 0, 0, 5, N, borde); rect(px, N - 5, 0, 5, N, borde);
		String[] palabras = f.split(" ");
		int esc = f.length() <= 6 ? 3 : 2;
		int y = 64 - palabras.length * (7 * esc + 4) / 2;
		int col = uno(NEGRO, ROJO, AZUL, VIOLETA);
		for (String p : palabras) { texto(px, p, 64, y, esc, col, true); y += 7 * esc + 4; }
		texto(px, "ATTE: " + usuario, 64, 112, 1, NEGRO, true);
		return "texto";
	}

	private static String ele(byte[] px) {
		int col = uno(ROJO, NEGRO, AZUL, VERDE);
		rect(px, 34, 12, 22, 80, NEGRO); rect(px, 34, 70, 64, 22, NEGRO);
		rect(px, 37, 15, 16, 74, col); rect(px, 37, 73, 58, 16, col);
		texto(px, uno(List.of("PERDEDOR", "MANCO", "TOMA TU L", "GG EZ")), 64, 104, 2, NEGRO, true);
		return "L";
	}

	private static String creeper(byte[] px) {
		rect(px, 14, 6, 100, 100, VERDE);
		int[][] negro = { { 30, 26, 20, 20 }, { 78, 26, 20, 20 }, { 54, 46, 20, 30 }, { 42, 56, 12, 30 }, { 74, 56, 12, 30 } };
		for (int[] q : negro) rect(px, q[0], q[1], q[2], q[3], NEGRO);
		texto(px, uno(List.of("SSSSS...", "BOOM", "TU AMIGO", "PUTO EL QUE LEE")), 64, 112, 2, NEGRO, true);
		return "creeper";
	}

	private static String corazon(byte[] px) {
		int col = uno(ROJO, ROSA, MAGENTA);
		for (int y = 0; y < N; y++)
			for (int x = 0; x < N; x++) {
				double u = (x - 64) / 40.0, v = -(y - 56) / 40.0;
				double f = Math.pow(u * u + v * v - 1, 3) - u * u * v * v * v;
				double fo = Math.pow(u * u / 1.12 + v * v / 1.12 - 1, 3) - u * u / 1.12 * Math.pow(v / Math.sqrt(1.12), 3);
				if (fo <= 0) px[x + y * N] = c(NEGRO);
				if (f <= 0) px[x + y * N] = c(col);
			}
		texto(px, "TE AMO", 64, 100, 2, NEGRO, true);
		texto(px, uno(List.of("(MENTIRA)", "(NAAA)", "(JODA)", "(BAH...)")), 64, 117, 1, NEGRO, true);
		return "corazon";
	}

	private static void leyenda(byte[] px, int fondo) {
		texto(px, uno(LEYENDAS), 64, 113, 1, NEGRO, true);
	}

	private static void adornos(byte[] px, int fondo) {
		int n = r().nextInt(0, 18);
		int col = uno(AMARILLO, BLANCO, ROJO, CELESTE, ORO);
		for (int i = 0; i < n; i++) {
			int x = r().nextInt(4, 124), y = r().nextInt(4, 124);
			if (px[x + y * N] != c(fondo)) continue;
			px[x + y * N] = c(col);
			if (x > 0) px[x - 1 + y * N] = c(col);
			if (x < N - 1) px[x + 1 + y * N] = c(col);
			if (y > 0) px[x + (y - 1) * N] = c(col);
			if (y < N - 1) px[x + (y + 1) * N] = c(col);
		}
	}

	// ------------------------------------------------------------------ primitivas
	private static boolean elipse(double u, double v, double rx, double ry) { return (u * u) / (rx * rx) + (v * v) / (ry * ry) <= 1; }

	private static void elipse(byte[] px, int cx, int cy, int rx, int ry, int col) {
		for (int y = cy - ry; y <= cy + ry; y++)
			for (int x = cx - rx; x <= cx + rx; x++)
				if (x >= 0 && y >= 0 && x < N && y < N && elipse(x - cx, y - cy, rx, ry)) px[x + y * N] = c(col);
	}

	private static void circulo(byte[] px, int cx, int cy, int rr, int col) { elipse(px, cx, cy, rr, rr, col); }

	private static void rect(byte[] px, int x0, int y0, int w, int h, int col) {
		for (int y = Math.max(0, y0); y < Math.min(N, y0 + h); y++)
			for (int x = Math.max(0, x0); x < Math.min(N, x0 + w); x++) px[x + y * N] = c(col);
	}

	// ------------------------------------------------------------------ letra de 5x7
	private static final Map<Character, String> FUENTE = new HashMap<>();
	static {
		String[] g = {
				"A01110100011000111111100011000110001", "B11110100011000111110100011000111110", "C01110100011000010000100001000101110",
				"D11110100011000110001100011000111110", "E11111100001000011110100001000011111", "F11111100001000011110100001000010000",
				"G01110100011000010111100011000101111", "H10001100011000111111100011000110001", "I01110001000010000100001000010001110",
				"J00111000100001000010000101001001100", "K10001100101010011000101001001010001", "L10000100001000010000100001000011111",
				"M10001110111010110101100011000110001", "N10001100011100110101100111000110001", "O01110100011000110001100011000101110",
				"P11110100011000111110100001000010000", "Q01110100011000110001101011001001101", "R11110100011000111110101001001010001",
				"S01111100001000001110000010000111110", "T11111001000010000100001000010000100", "U10001100011000110001100011000101110",
				"V10001100011000110001100010101000100", "W10001100011000110101101011010101010", "X10001100010101000100010101000110001",
				"Y10001100010101000100001000010000100", "Z11111000010001000100010001000011111", "001110100011001110101110011000101110",
				"100100011000010000100001000010001110", "201110100010000100110010001000011111", "311110000010000101110000010000111110",
				"400010001100101010010111110001000010", "511111100001111000001000011000101110", "600110010001000011110100011000101110",
				"711111000010001000100010000100001000", "801110100011000101110100011000101110", "901110100011000101111000010001001100",
				"!00100001000010000100001000000000100", "?01110100010000100010001000000000100", ":00000001000010000000001000010000000",
				".00000000000000000000000000011000110", "-00000000000000011111000000000000000", " 00000000000000000000000000000000000",
				"(00010001000100001000010000010000010", ")01000001000001000010000100010001000", ",00000000000000000000001100010001000" };
		for (String s : g) FUENTE.put(s.charAt(0), s.substring(1));
	}

	static String normalizar(String t) {
		String s = Normalizer.normalize(t, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
		return s.toUpperCase(Locale.ROOT);
	}

	/** Escribe texto centrado en x (si centrado) con la fuente de 5x7 a la escala indicada, con borde blanco. */
	static void texto(byte[] px, String t, int x, int y, int esc, int col, boolean centrado) {
		String s = normalizar(t);
		int ancho = s.length() * 6 * esc - esc;
		if (ancho > N - 4 && esc > 1) { texto(px, t, x, y, esc - 1, col, centrado); return; }
		int x0 = centrado ? x - ancho / 2 : x;
		for (int pasada = 0; pasada < 2; pasada++) {
			int cx = x0;
			for (char ch : s.toCharArray()) {
				String gl = FUENTE.getOrDefault(ch, FUENTE.get('?'));
				for (int fy = 0; fy < 7; fy++)
					for (int fx = 0; fx < 5; fx++) {
						if (gl.charAt(fy * 5 + fx) != '1') continue;
						if (pasada == 0) rect(px, cx + fx * esc - 1, y + fy * esc - 1, esc + 2, esc + 2, BLANCO);
						else rect(px, cx + fx * esc, y + fy * esc, esc, esc, col);
					}
				cx += 6 * esc;
			}
		}
	}

	private Dibujos() { }
}
