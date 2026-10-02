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
	private static final int[] COLORES = { 0xFFE53935, 0xFFFB8C00, 0xFFFDD835, 0xFF43A047, 0xFF00ACC1, 0xFF8E24AA };
	private static final int[] COLORES_CLAROS = { 0xFFFF8A80, 0xFFFFCC80, 0xFFFFF59D, 0xFFA5D6A7, 0xFF80DEEA, 0xFFCE93D8 };

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
		});
	}

	private static int preguntaAbierta = -1;
	private static PreguntaPantalla pantalla;

	private static void dibujar(GuiGraphicsExtractor g, DeltaTracker dt) {
		dibujarRueda(g);
		dibujarResultadoPregunta(g);
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
			int color = (parpadeo && i == d.ganador()) ? 0xFFFFFFFF
					: (frenada && i == d.ganador()) ? COLORES_CLAROS[i % COLORES_CLAROS.length] : COLORES[i % COLORES.length];
			for (float a = i * ancho; a < (i + 1) * ancho; a += 1.4f) rayo(g, m, a, 0, radio, color, 4);
		}
		for (int i = 0; i < RuedaEstado.SECTORES; i++) rayo(g, m, i * ancho, 10, radio, 0xFF3E2723, 1);
		for (int i = 0; i < RuedaEstado.SECTORES; i++) {
			String et = d.etiquetas()[i];
			m.pushMatrix();
			m.rotate((float) Math.toRadians(i * ancho + ancho / 2));
			m.translate(radio * 0.60f, 0);
			float lugar = radio * 0.62f; // largo disponible del sector para el texto
			float s = Math.min(0.95f, lugar / Math.max(1, font.width(et)));
			m.scale(s, s);
			int col = (frenada && i == d.ganador()) ? 0xFF000000 : 0xFFFFFFFF;
			g.text(font, et, -font.width(et) / 2, -4, col, !(frenada && i == d.ganador()));
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

		// Resultado
		if (frenada) {
			float ys = radio + 16;
			int anchoTit = Math.round(font.width(d.titulo()) * 1.3f);
			int anchoDet = d.detalle() == null ? 0 : font.width(d.detalle());
			int mitad = Math.max(anchoTit, anchoDet) / 2 + 10;
			int alto = d.detalle() == null ? 20 : 30;
			g.fill(-mitad - 1, Math.round(ys) - 1, mitad + 1, Math.round(ys) + alto + 1, 0xFF000000 | (d.colorResultado() & 0x00FFFFFF));
			g.fill(-mitad, Math.round(ys), mitad, Math.round(ys) + alto, 0xE0101010);
			textoCentrado(g, m, font, d.titulo(), 0, ys + 5, 1.3f, d.colorResultado());
			if (d.detalle() != null) textoCentrado(g, m, font, d.detalle(), 0, ys + 19, 1.0f, 0xFFDDDDDD);
		}
		m.popMatrix();
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
