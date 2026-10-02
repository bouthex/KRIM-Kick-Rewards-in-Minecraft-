package com.archimak.krim;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Pantalla de los funnys que "congelan" la partida: mientras está abierta no te podés mover ni girar la cámara,
 * y el mundo está frenado con /tick freeze.
 *  - edit: calavera estilo emoji del meme que late con el phonk, temblor y texto de TikTok, sobre el juego congelado.
 *  - creditos: créditos finales que suben sobre negro.
 *  - apagon: todo negro, "Edesur te cortó la luz".
 */
public class FunnyPantalla extends Screen {
	private final FunnyEstado.Efecto e;

	public FunnyPantalla(FunnyEstado.Efecto e) {
		super(Component.literal("KRIM"));
		this.e = e;
	}

	public FunnyEstado.Efecto efecto() { return e; }

	@Override
	public boolean isPauseScreen() { return false; }

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		long el = RuedaEstado.ahoraMs() - e.inicioMs();
		switch (e.tipo()) {
			case "edit" -> edit(g, el);
			case "creditos" -> creditos(g, el);
			default -> apagon(g, el);
		}
	}

	/** Sin fondo borroso ni oscuro: el juego se ve congelado detrás. */
	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) { }

	// ------------------------------------------------------------------ edit de TikTok
	private void edit(GuiGraphicsExtractor g, long el) {
		int w = this.width, h = this.height;
		float golpe = 0f;
		for (int t : e.golpesMs()) {
			long d = el - t;
			if (d >= 0 && d < 240) golpe = Math.max(golpe, 1f - d / 240f);
		}
		ThreadLocalRandom r = ThreadLocalRandom.current();
		int sx = golpe > 0.5f ? r.nextInt(-3, 4) : 0, sy = golpe > 0.5f ? r.nextInt(-3, 4) : 0;

		// La calavera aparece de golpe (con un rebotecito) y late con el beat
		float entrada = el < 180 ? 0.6f + 0.4f * (el / 180f) : 1f;
		float tam = h * 0.46f / 32f * entrada * (1f + 0.10f * golpe);
		Matrix3x2fStack m = g.pose();
		m.pushMatrix();
		m.translate(w / 2f + sx, h / 2f + sy);
		m.scale(tam, tam);
		calavera(g, e.variante(), golpe);
		m.popMatrix();

		float esc = Math.min(2.4f, (w * 0.85f) / Math.max(1, this.font.width(e.titulo())));
		textoBorde(g, e.titulo(), w / 2f + sx, h * 0.12f + sy, esc, 0xFFFFFFFF);
		textoBorde(g, e.sub(), w / 2f, h * 0.88f, 1f, 0xFFE0E0E0);
		g.text(this.font, e.tema(), 8, h - 14, 0xFFFFFFFF, true);

		if (el < 120) g.fill(0, 0, w, h, ((int) (160 * (1 - el / 120f)) << 24) | 0xFFFFFF);
	}

	/**
	 * Calavera estilo emoji del meme, dibujada en una grilla de 32x32 con sombreado.
	 * variante 0: normal · 1: ojos rojos que brillan con el beat · 2: con anteojos negros (modo sigma)
	 */
	private static void calavera(GuiGraphicsExtractor g, int variante, float golpe) {
		for (int y = 0; y < 32; y++)
			for (int x = 0; x < 32; x++) {
				double u = (x + 0.5 - 16) / 16.0, v = (y + 0.5 - 16) / 16.0;
				boolean craneo = Math.hypot(u, (v + 0.12) * 1.05) <= 0.80;
				boolean mandibula = Math.abs(u) <= 0.46 && v >= 0.28 && v <= 0.84 && !(Math.abs(u) > 0.30 && v > 0.70);
				boolean borde = (Math.hypot(u, (v + 0.12) * 1.05) <= 0.86 || (Math.abs(u) <= 0.52 && v >= 0.26 && v <= 0.90 && !(Math.abs(u) > 0.36 && v > 0.76)))
						&& !(craneo || mandibula);
				int col = 0;
				if (borde) col = 0xFF4A4A4A;
				if (craneo || mandibula) {
					double luz = 0.5 - 0.35 * u - 0.30 * v;
					col = luz > 0.75 ? 0xFFFFFFFF : luz > 0.45 ? 0xFFEDEDED : luz > 0.2 ? 0xFFD9D9D9 : 0xFFC4C4C4;
					boolean ojo = (Math.pow((u + 0.32) / 0.24, 2) + Math.pow((v - 0.02) / 0.27, 2) <= 1)
							|| (Math.pow((u - 0.32) / 0.24, 2) + Math.pow((v - 0.02) / 0.27, 2) <= 1);
					boolean nariz = v >= 0.28 && v <= 0.44 && Math.abs(u) <= (0.44 - v) * 0.9;
					boolean dientes = mandibula && v >= 0.56 && (Math.abs(u) % 0.20 < 0.035 || Math.abs(v - 0.66) < 0.03);
					if (ojo) col = 0xFF262626;
					if (nariz || dientes) col = 0xFF3A3A3A;
					if (ojo && variante == 1) {
						boolean pupila = Math.hypot(Math.abs(u) - 0.32, v - 0.04) < 0.09 + 0.05 * golpe;
						if (pupila) col = 0xFFFF1744;
					}
					if (variante == 2 && v >= -0.16 && v <= 0.18 && Math.abs(u) <= 0.64) col = (v < -0.10) ? 0xFF000000 : 0xFF151515;
					if (variante == 2 && v >= -0.12 && v <= -0.06 && Math.abs(u + 0.30) < 0.10) col = 0xFF5E5E5E;
				}
				if (col != 0) g.fill(x - 16, y - 16, x - 15, y - 15, col);
			}
	}

	// ------------------------------------------------------------------ créditos finales
	private void creditos(GuiGraphicsExtractor g, long el) {
		int w = this.width, h = this.height;
		g.fill(0, 0, w, h, 0xFF000000);
		float vel = (h + e.lineas().length * 14f) / (e.duracionMs() - 1500f);
		float y0 = h - el * vel;
		for (int i = 0; i < e.lineas().length; i++) {
			String l = e.lineas()[i];
			if (l.isEmpty()) continue;
			float y = y0 + i * 14;
			if (y < -20 || y > h + 20) continue;
			boolean titulo = i == 0 || l.equals("¿FIN?");
			boolean rol = i + 1 < e.lineas().length && !e.lineas()[i + 1].isEmpty() && (i == 0 || e.lineas()[i - 1].isEmpty());
			int col = titulo ? 0xFFFFD54F : rol ? 0xFF9E9E9E : 0xFFFFFFFF;
			textoBorde(g, l, w / 2f, y, titulo ? 2f : 1.1f, col);
		}
	}

	// ------------------------------------------------------------------ apagón
	private void apagon(GuiGraphicsExtractor g, long el) {
		int w = this.width, h = this.height;
		if (el < 4200) {
			g.fill(0, 0, w, h, 0xFF000000);
			if (el > 1500) textoBorde(g, e.lineas()[0], w / 2f, h / 2f, 1f, 0xFF616161);
		} else {
			long d = el - 4200;
			int alfa = (int) Math.max(0, 255 - d * 255 / 900);
			g.fill(0, 0, w, h, alfa << 24);
			textoBorde(g, e.lineas()[1], w / 2f, h / 2f, 1.2f, 0xFFFFEB3B);
		}
	}

	private void textoBorde(GuiGraphicsExtractor g, String t, float x, float y, float esc, int color) {
		Matrix3x2fStack m = g.pose();
		m.pushMatrix();
		m.translate(x, y);
		m.scale(esc, esc);
		int x0 = -this.font.width(t) / 2;
		g.text(this.font, t, x0 - 1, 0, 0xFF000000, false);
		g.text(this.font, t, x0 + 1, 0, 0xFF000000, false);
		g.text(this.font, t, x0, -1, 0xFF000000, false);
		g.text(this.font, t, x0, 1, 0xFF000000, false);
		g.text(this.font, t, x0, 0, color, false);
		m.popMatrix();
	}
}
