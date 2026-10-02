package com.archimak.krim;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;

/**
 * Dibuja la Ruleta del destino en el centro de la pantalla: rueda de 12 sectores que gira y frena,
 * aro dorado con luces que titilan, puntero rojo arriba, centro con el logo y cartel con el resultado.
 * Todo se dibuja con rectángulos rotados (rayos), sin texturas.
 */
public class KrimCliente implements ClientModInitializer {
	// Colores por tipo de premio, con dos tonos para que los sectores vecinos se distingan
	// [tipo][0 = tono A, 1 = tono B, 2 = claro (ganador)]
	// tipo: 0 bueno (verde), 1 interactivo (amarillo), 2 malo (rojo), 3 jefe/carnada (negro), 4 funny (rosa)
	private static final int[][] COLORES = {
			{ 0xFF2E9E46, 0xFF1F7A34, 0xFF9CFF9C },
			{ 0xFFF2C21B, 0xFFD39B00, 0xFFFFF59D },
			{ 0xFFE53935, 0xFFAF1F1F, 0xFFFF8A80 },
			{ 0xFF1C1C1C, 0xFF050505, 0xFF757575 },
			{ 0xFFEC407A, 0xFFC2185B, 0xFFF8BBD0 } };

	@Override
	public void onInitializeClient() {
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("krim", "ruleta"), KrimCliente::dibujar);
		// Abre la pantalla de la pregunta cuando el servidor la manda, y la cierra si se terminó el tiempo
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			TriviaEstado.Pregunta p = TriviaEstado.actual;
			if (p != null && p.id() != preguntaAbierta && mc.player != null) {
				preguntaAbierta = p.id();
				pantalla = new PreguntaPantalla(p);
				mc.gui.setScreen(pantalla);
			}
			// Se terminó el tiempo y la pantalla sigue abierta: se cierra sola
			if (p == null && pantalla != null) {
				if (!pantalla.respondida()) mc.gui.setScreen(null);
				pantalla = null;
			}
			// Funnys que congelan la pantalla (edit, créditos, apagón)
			FunnyEstado.Efecto f = FunnyEstado.actual;
			// Solo estos tres usan pantalla; jumpscare y wachín se dibujan en el HUD (E-006)
			boolean conPantalla = f != null && (f.tipo().equals("edit") || f.tipo().equals("creditos") || f.tipo().equals("apagon"));
			if (conPantalla && f.id() != funnyAbierto && mc.player != null) {
				funnyAbierto = f.id();
				funny = new FunnyPantalla(f);
				mc.gui.setScreen(funny);
			}
			if (funny != null && RuedaEstado.ahoraMs() - funny.efecto().inicioMs() > funny.efecto().duracionMs()) {
				mc.gui.setScreen(null);
				funny = null;
			}
		});
	}

	private static int funnyAbierto = -1;
	private static FunnyPantalla funny;

	private static int preguntaAbierta = -1;
	private static PreguntaPantalla pantalla;

	private static void dibujar(GuiGraphicsExtractor g, DeltaTracker dt) {
		dibujarRueda(g);
		dibujarResultadoPregunta(g);
		dibujarJumpscare(g);
		dibujarWachin(g);
	}

	// ------------------------------------------------------------------ wachín: bardeo gigante
	private static void dibujarWachin(GuiGraphicsExtractor g) {
		FunnyEstado.Efecto f = FunnyEstado.actual;
		if (f == null || !f.tipo().equals("wachin")) return;
		long el = RuedaEstado.ahoraMs() - f.inicioMs();
		if (el < 0 || el > f.duracionMs()) return;
		Minecraft mc = Minecraft.getInstance();
		Font font = mc.font;
		int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
		float alfa = el < 200 ? el / 200f : el > f.duracionMs() - 400 ? (f.duracionMs() - el) / 400f : 1f;
		g.fill(0, 0, w, h, ((int) (150 * alfa) << 24));
		// palabras repartidas en renglones que entren en el 90% del ancho, con la escala más grande posible
		String[] palabras = f.titulo().toUpperCase().split(" ");
		float esc = 3.2f;
		java.util.List<String> lineas = new java.util.ArrayList<>();
		while (esc > 1.2f) {
			lineas.clear();
			StringBuilder l = new StringBuilder();
			for (String p : palabras) {
				String prueba = l.length() == 0 ? p : l + " " + p;
				if (font.width(prueba) * esc > w * 0.9f && l.length() > 0) { lineas.add(l.toString()); l = new StringBuilder(p); }
				else l = new StringBuilder(prueba);
			}
			lineas.add(l.toString());
			if (lineas.size() * 11 * esc < h * 0.6f) break;
			esc -= 0.2f;
		}
		java.util.concurrent.ThreadLocalRandom r = java.util.concurrent.ThreadLocalRandom.current();
		Matrix3x2fStack m = g.pose();
		float entrada = el < 250 ? rebote(el / 250f) : 1f;
		int[] colores = { 0xFFFF5252, 0xFFFFEB3B, 0xFFFF80AB };
		float y = h / 2f - lineas.size() * 11 * esc / 2f;
		for (int i = 0; i < lineas.size(); i++) {
			String l = lineas.get(i);
			int sx = el < 1200 ? r.nextInt(-2, 3) : 0;
			m.pushMatrix();
			m.translate(w / 2f + sx, y + i * 11 * esc);
			m.scale(esc * entrada, esc * entrada);
			int x0 = -font.width(l) / 2;
			g.text(font, l, x0 - 1, 0, 0xFF000000, false);
			g.text(font, l, x0 + 1, 0, 0xFF000000, false);
			g.text(font, l, x0, -1, 0xFF000000, false);
			g.text(font, l, x0, 1, 0xFF000000, false);
			g.text(font, l, x0, 0, colores[(f.variante() + i) % colores.length], false);
			m.popMatrix();
		}
		m.pushMatrix();
		m.translate(w / 2f, y + lineas.size() * 11 * esc + 6);
		m.scale(1.3f, 1.3f);
		g.text(font, f.sub(), -font.width(f.sub()) / 2, 0, 0xFFE0E0E0, true);
		m.popMatrix();
	}

	// ------------------------------------------------------------------ jumpscare: cara de creeper a pantalla completa
	private static final String[] CREEPER = { "........", ".KK..KK.", ".KK..KK.", "...KK...", "..KKKK..", "..KKKK..", "..K..K..", "........" };

	/**
	 * Cara de terror propia (no es ninguna foto): piel pálida, ojos negros enormes y una boca
	 * con sonrisa macabra (sonrisa = true) o abierta gritando. Parpadea en negativo para asustar más.
	 */
	private static void caraTerror(GuiGraphicsExtractor g, int w, int h, int sx, int sy, boolean sonrisa, long el) {
		boolean negativo = (el / 90) % 4 == 1;
		g.fill(0, 0, w, h, negativo ? 0xFFF0F0F0 : 0xFF050505);
		int celdas = 48;
		int t = Math.max(2, Math.round(Math.max(w, h) * 1.05f / celdas));
		int ox = (w - celdas * t) / 2 + sx, oy = (h - celdas * t) / 2 + sy;
		for (int y = 0; y < celdas; y++)
			for (int x = 0; x < celdas; x++) {
				double u = (x + 0.5 - 24) / 24.0, v = (y + 0.5 - 24) / 24.0;
				if (u * u / 0.62 + v * v / 0.92 > 1) continue;
				int col = 0xFFC9C3B8;
				double luz = 0.15 - 0.25 * u - 0.2 * v;
				if (luz < -0.1) col = 0xFFA39C90;
				if (luz < -0.3) col = 0xFF7E776C;
				boolean ojo = Math.pow((Math.abs(u) - 0.30) / 0.17, 2) + Math.pow((v + 0.18) / 0.20, 2) <= 1;
				if (ojo) col = 0xFF000000;
				if (ojo && Math.hypot(Math.abs(u) - 0.30, v + 0.18) < 0.035) col = 0xFFFFFFFF;
				if (!ojo && Math.pow((Math.abs(u) - 0.30) / 0.24, 2) + Math.pow((v + 0.18) / 0.28, 2) <= 1) col = 0xFF5A5148;
				if (sonrisa) {
					double curva = 0.30 + 0.25 * u * u;
					if (Math.abs(u) < 0.62 && v > curva && v < curva + 0.10) col = 0xFF000000;
					if (Math.abs(u) < 0.58 && v > curva + 0.02 && v < curva + 0.06 && Math.abs(u) % 0.09 < 0.05) col = 0xFFE8E2D0;
				} else {
					boolean boca = u * u / 0.06 + (v - 0.45) * (v - 0.45) / 0.07 <= 1;
					if (boca) col = 0xFF000000;
					if (boca && v > 0.62) col = 0xFF3A0000;
				}
				if (Math.abs(u) < 0.03 && v > -0.02 && v < 0.12) col = 0xFF5A5148;
				if (negativo) col = 0xFF000000 | (~col & 0x00FFFFFF);
				g.fill(ox + x * t, oy + y * t, ox + x * t + t, oy + y * t + t, col);
			}
	}

	private static void dibujarJumpscare(GuiGraphicsExtractor g) {
		FunnyEstado.Efecto f = FunnyEstado.actual;
		if (f == null || !f.tipo().equals("jumpscare")) return;
		long el = RuedaEstado.ahoraMs() - f.inicioMs();
		if (el < 0 || el > f.duracionMs()) return;
		Minecraft mc = Minecraft.getInstance();
		int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
		java.util.concurrent.ThreadLocalRandom r = java.util.concurrent.ThreadLocalRandom.current();
		int sx = r.nextInt(-8, 9), sy = r.nextInt(-8, 9);
		int lado = Math.round(Math.max(w, h) * 1.15f), t = lado / 8;
		int ox = (w - lado) / 2 + sx, oy = (h - lado) / 2 + sy;
		if (f.variante() == 0) {
			g.fill(0, 0, w, h, 0xFF4CAF50);
			for (int y = 0; y < 8; y++)
				for (int x = 0; x < 8; x++) {
					int c = CREEPER[y].charAt(x) == 'K' ? 0xFF0B0B0B : (((x * 7 + y * 3) % 5 == 0) ? 0xFF66BB6A : 0);
					if (c != 0) g.fill(ox + x * t, oy + y * t, ox + x * t + t, oy + y * t + t, c);
				}
		} else {
			caraTerror(g, w, h, sx, sy, f.variante() == 1, el);
		}
		if (el > 900) g.fill(0, 0, w, h, ((int) (255 * Math.min(1, (el - 900) / 400f)) << 24) | 0xFFFFFF);
	}

	// ------------------------------------------------------------------ resultado de la pregunta
	private static final String[] CACA = {
			".......KK.......", "......KLBK......", ".....KBLBBK.....", ".....KBBBBK.....", "....KKBBBBKK....",
			"...KBBLBBBBBK...", "...KBWWBBWWBK...", "..KBBWKBBWKBBK..", "..KBBBBBBBBBBK..", ".KBLBKBBBBKBBBK.",
			".KBBBBKKKKBBBBK.", "KBBLBBBBBBBBBBBK", "KBBBBBBBBBBBBBBK", ".KKKKKKKKKKKKKK." };

	private static void dibujarResultadoPregunta(GuiGraphicsExtractor g) {
		TriviaEstado.Resultado r = TriviaEstado.resultado;
		if (r == null) return;
		long el = RuedaEstado.ahoraMs() - r.inicioMs();
		if (el < 0 || el > 3200) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;
		Font font = mc.font;
		int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
		Matrix3x2fStack m = g.pose();
		float esc = el < 250 ? rebote(el / 250f) : el > 2900 ? Math.max(0f, (3200 - el) / 300f) : 1f;
		if (esc <= 0.01f) return;
		m.pushMatrix();
		m.translate(w / 2f, h / 2f - 10);
		m.scale(esc, esc);
		if (r.correcto()) {
			// Rayos dorados girando detrás
			m.pushMatrix();
			m.rotate((float) Math.toRadians(el / 12.0));
			for (int i = 0; i < 16; i++) rayo(g, m, i * 22.5f, 0, 70, (i % 2 == 0) ? 0x99FFC107 : 0x55FFF59D, 10);
			m.popMatrix();
			textoCentrado(g, m, font, "¡CORRECTO!", 2, -10, 3.2f, 0xFF1B5E20);
			textoCentrado(g, m, font, "¡CORRECTO!", 0, -12, 3.2f, 0xFF76FF03);
			textoCentrado(g, m, font, "el chat no lo puede creer", 0, 22, 1f, 0xFFFFFFFF);
		} else {
			int t = 7;
			int ox = -8 * t, oy = -7 * t - 10;
			for (int fy = 0; fy < CACA.length; fy++)
				for (int fx = 0; fx < 16; fx++) {
					char ch = CACA[fy].charAt(fx);
					int col = switch (ch) {
						case 'K' -> 0xFF2B1A0E; case 'B' -> 0xFF7B4A1E; case 'L' -> 0xFFA0672E; case 'W' -> 0xFFFFFFFF; default -> 0;
					};
					if (col != 0) g.fill(ox + fx * t, oy + fy * t, ox + fx * t + t, oy + fy * t + t, col);
				}
			textoCentrado(g, m, font, "INCORRECTO", 0, 50, 2.2f, 0xFFFF5252);
			textoCentrado(g, m, font, "Era: " + r.respuestaCorrecta(), 0, 72, 1f, 0xFFDDDDDD);
		}
		m.popMatrix();
	}

	// ------------------------------------------------------------------ rueda
	private static void dibujarRueda(GuiGraphicsExtractor g) {
		RuedaEstado.Datos d = RuedaEstado.actual;
		if (d == null) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;
		long el = RuedaEstado.ahoraMs() - d.inicioMs();
		if (el < 0 || el > d.totalMs()) return;

		Font font = mc.font;
		int w = mc.getWindow().getGuiScaledWidth();
		int h = mc.getWindow().getGuiScaledHeight();
		float radio = Math.max(50f, Math.min(100f, h * 0.38f));
		float cx = w / 2f, cy = h / 2f - 8f;

		// Escala: entra con rebote y sale achicándose
		float esc;
		if (el < 350) esc = rebote(el / 350f);
		else if (el > d.totalMs() - 350) esc = Math.max(0f, (d.totalMs() - el) / 350f);
		else esc = 1f;
		if (esc <= 0.01f) return;

		boolean frenada = el >= d.giroMs();
		float rot = d.rotacion(el);
		boolean parpadeo = frenada && ((el / 150) % 2 == 0);

		Matrix3x2fStack m = g.pose();
		m.pushMatrix();
		m.translate(cx, cy);
		m.scale(esc, esc);

		// Título arriba
		String arriba = "RULETA DE " + d.usuario().toUpperCase();
		textoCentrado(g, m, font, arriba, 0, -radio - 30, 1.0f, 0xFFFFD54F);
		if (d.enFila() > 0) textoCentrado(g, m, font, "(+" + d.enFila() + " en fila)", 0, -radio - 20, 0.7f, 0xFFBDBDBD);

		// Sombra, aro dorado y borde
		disco(g, m, radio + 11, 0x66000000, 3.0f, 6);
		disco(g, m, radio + 8, 0xFFB8860B, 1.4f, 4);
		disco(g, m, radio + 6, 0xFFFFC107, 1.4f, 4);
		disco(g, m, radio + 1, 0xFF3E2723, 1.4f, 4);

		// Sectores (giran)
		m.pushMatrix();
		m.rotate((float) Math.toRadians(rot));
		float ancho = 360f / RuedaEstado.SECTORES;
		for (int i = 0; i < RuedaEstado.SECTORES; i++) {
			int[] paleta = COLORES[Math.max(0, Math.min(4, d.tipos()[i]))];
			int color = (parpadeo && i == d.ganador()) ? 0xFFFFFFFF : (frenada && i == d.ganador()) ? paleta[2] : paleta[i % 2];
			for (float a = i * ancho; a < (i + 1) * ancho; a += 1.4f) rayo(g, m, a, 0, radio, color, 4);
		}
		for (int i = 0; i < RuedaEstado.SECTORES; i++) rayo(g, m, i * ancho, 10, radio, 0xFF3E2723, 1);
		for (int i = 0; i < RuedaEstado.SECTORES; i++) {
			String et = d.etiquetas()[i];
			float angSector = i * ancho + ancho / 2;
			m.pushMatrix();
			m.rotate((float) Math.toRadians(angSector));
			m.translate(radio * 0.60f, 0);
			// Si el sector quedó en la mitad izquierda, se da vuelta el texto para que nunca se lea de cabeza
			float angPantalla = (((rot + angSector) % 360f) + 360f) % 360f;
			if (angPantalla > 90f && angPantalla < 270f) m.rotate((float) Math.PI);
			float lugar = radio * 0.66f;
			float s = Math.min(1.0f, lugar / Math.max(1, font.width(et)));
			m.scale(s, s);
			int col = (frenada && i == d.ganador()) ? 0xFF000000 : 0xFFFFFFFF;
			int borde = (frenada && i == d.ganador()) ? 0xFFFFFFFF : 0xFF000000;
			conBorde(g, font, et, -font.width(et) / 2, -4, col, borde);
			m.popMatrix();
		}
		m.popMatrix();

		// Luces del aro (no giran, titilan)
		int fase = (int) (el / (frenada ? 120 : 70));
		for (int k = 0; k < 24; k++) {
			double a = Math.toRadians(k * 15);
			int lx = Math.round((float) Math.cos(a) * (radio + 7));
			int ly = Math.round((float) Math.sin(a) * (radio + 7));
			int luz = ((k + fase) % 2 == 0) ? 0xFFFFFDE7 : 0xFFFF6F00;
			g.fill(lx - 1, ly - 1, lx + 2, ly + 2, luz);
		}

		// Centro
		float hub = radio / 72f;
		disco(g, m, 15 * hub, 0xFF3E2723, 6f, 4);
		disco(g, m, 13 * hub, 0xFFFFC107, 6f, 4);
		disco(g, m, 10 * hub, 0xFFB71C1C, 6f, 4);
		textoCentrado(g, m, font, "KRIM", 0, -3 * hub, 0.6f * hub, 0xFFFFFFFF);

		// Puntero rojo arriba, apuntando hacia abajo
		int top = Math.round(-radio - 13);
		for (int y = 0; y < 13; y++) {
			int mitad = Math.round((13 - y) * 0.75f);
			g.fill(-mitad - 1, top + y, mitad + 2, top + y + 1, 0xFF212121);
		}
		for (int y = 1; y < 11; y++) {
			int mitad = Math.round((11 - y) * 0.7f);
			g.fill(-mitad, top + y, mitad + 1, top + y + 1, 0xFFE53935);
		}

		// Mientras gira: cartel grande y horizontal con lo que pasa por el puntero
		if (!frenada) {
			int sec = RuedaEstado.Datos.sectorBajoPuntero(rot);
			String actual = d.etiquetas()[sec];
			int[] paleta = COLORES[Math.max(0, Math.min(4, d.tipos()[sec]))];
			float ys = radio + 16;
			int mitad = Math.round(font.width(actual) * 1.6f) / 2 + 12;
			g.fill(-mitad - 1, Math.round(ys) - 1, mitad + 1, Math.round(ys) + 23, 0xFF000000 | (paleta[0] & 0x00FFFFFF));
			g.fill(-mitad, Math.round(ys), mitad, Math.round(ys) + 22, 0xE0101010);
			m.pushMatrix();
			m.translate(0, ys + 5);
			m.scale(1.6f, 1.6f);
			conBorde(g, font, actual, -font.width(actual) / 2, 0, paleta[2], 0xFF000000);
			m.popMatrix();
		}

		// Resultado
		if (frenada) {
			float ys = radio + 16;
			int anchoTit = Math.round(font.width(d.titulo()) * 1.3f);
			int anchoDet = d.detalle() == null ? 0 : font.width(d.detalle());
			int mitad = Math.max(anchoTit, anchoDet) / 2 + 10;
			int alto = d.detalle() == null ? 20 : 30;
			g.fill(-mitad - 1, Math.round(ys) - 1, mitad + 1, Math.round(ys) + alto + 1, 0xFF000000 | (d.colorResultado() & 0x00FFFFFF));
			g.fill(-mitad, Math.round(ys), mitad, Math.round(ys) + alto, 0xE0101010);
			m.pushMatrix();
			m.translate(0, ys + 5);
			m.scale(1.3f, 1.3f);
			conBorde(g, font, d.titulo(), -font.width(d.titulo()) / 2, 0, d.colorResultado(), 0xFF000000);
			m.popMatrix();
			if (d.detalle() != null) textoCentrado(g, m, font, d.detalle(), 0, ys + 19, 1.0f, 0xFFDDDDDD);
		}
		m.popMatrix();
	}

	/** Texto con borde de 1 px alrededor: se lee bien sobre cualquier color. */
	private static void conBorde(GuiGraphicsExtractor g, Font font, String t, int x, int y, int color, int borde) {
		g.text(font, t, x - 1, y, borde, false);
		g.text(font, t, x + 1, y, borde, false);
		g.text(font, t, x, y - 1, borde, false);
		g.text(font, t, x, y + 1, borde, false);
		g.text(font, t, x, y, color, false);
	}

	private static void rayo(GuiGraphicsExtractor g, Matrix3x2fStack m, float grados, float desde, float hasta, int color, int grosor) {
		m.pushMatrix();
		m.rotate((float) Math.toRadians(grados));
		int med = grosor / 2;
		g.fill(Math.round(desde), -med, Math.round(hasta), grosor - med, color);
		m.popMatrix();
	}

	private static void disco(GuiGraphicsExtractor g, Matrix3x2fStack m, float radio, int color, float paso, int grosor) {
		for (float a = 0; a < 360f; a += paso) rayo(g, m, a, 0, radio, color, grosor);
	}

	private static void textoCentrado(GuiGraphicsExtractor g, Matrix3x2fStack m, Font font, String t, float x, float y, float escala, int color) {
		m.pushMatrix();
		m.translate(x, y);
		m.scale(escala, escala);
		g.text(font, t, -font.width(t) / 2, 0, color, true);
		m.popMatrix();
	}

	private static float rebote(float t) {
		float c1 = 1.70158f, c3 = c1 + 1;
		return 1 + c3 * (float) Math.pow(t - 1, 3) + c1 * (float) Math.pow(t - 1, 2);
	}
}
