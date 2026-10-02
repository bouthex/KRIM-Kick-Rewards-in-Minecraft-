package com.archimak.krim;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Pantalla de los funnys que "congelan" la partida: mientras está abierta no te podés mover ni girar la cámara,
 * y el mundo está frenado con /tick freeze.
 *  - edit: calavera que late con el phonk, barras de cine, flashes, temblor y texto de TikTok.
 *  - creditos: créditos finales que suben sobre negro.
 *  - apagon: todo negro, "Edesur te cortó la luz".
 */
public class FunnyPantalla extends Screen {
	private static final String[] CALAVERA = {
			"....KKKKKKKK....", "..KKWWWWWWWWKK..", ".KWWWWWWWWWWWWK.", "KWWWWWWWWWWWWWWK", "KWWKKKWWWWKKKWWK",
			"KWKKRKKWWKKRKKWK", "KWKKKKKWWKKKKKWK", "KWWKKKWWWWKKKWWK", "KWWWWWWKKWWWWWWK", ".KWWWWKKKKWWWWK.",
			"..KWWWWWWWWWWK..", "..KWKWKWKWKWKK..", "..KWWWWWWWWWWK..", "...KKKKKKKKKK..." };

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

	// ------------------------------------------------------------------ edit de TikTok
	private void edit(GuiGraphicsExtractor g, long el) {
		int w = this.width, h = this.height;
		float golpe = 0f;
		for (int t : e.golpesMs()) {
			long d = el - t;
			if (d >= 0 && d < 260) golpe = Math.max(golpe, 1f - d / 260f);
		}
		ThreadLocalRandom r = ThreadLocalRandom.current();
		int sx = golpe > 0.4f ? r.nextInt(-4, 5) : 0, sy = golpe > 0.4f ? r.nextInt(-3, 4) : 0;

		g.fill(0, 0, w, h, 0xB0000000);
		// grano de película
		for (int i = 0; i < 90; i++) {
			int x = r.nextInt(w), y = r.nextInt(h);
			g.fill(x, y, x + 1, y + 1, r.nextBoolean() ? 0x30FFFFFF : 0x30000000);
		}
		Matrix3x2fStack m = g.pose();
		m.pushMatrix();
		m.translate(w / 2f + sx, h / 2f + sy);

		// rayos violetas detrás de la calavera
		m.pushMatrix();
		m.rotate((float) Math.toRadians(el / 25.0));
		for (int i = 0; i < 12; i++) {
			m.pushMatrix();
			m.rotate((float) Math.toRadians(i * 30));
			g.fill(0, -6, Math.round(h * 0.6f), 6, (i % 2 == 0) ? 0x40B388FF : 0x20FF4081);
			m.popMatrix();
		}
		m.popMatrix();

		// calavera que late
		float tam = h * 0.42f / 16f * (1f + 0.14f * golpe);
		boolean ojosRojos = e.variante() == 1 || golpe > 0.6f;
		m.pushMatrix();
		m.scale(tam, tam);
		for (int fy = 0; fy < CALAVERA.length; fy++)
			for (int fx = 0; fx < 16; fx++) {
				char ch = CALAVERA[fy].charAt(fx);
				int col = switch (ch) {
					case 'K' -> 0xFF000000; case 'W' -> 0xFFF2F2F2; case 'R' -> ojosRojos ? 0xFFFF1744 : 0xFF000000; default -> 0;
				};
				if (col != 0) g.fill(fx - 8, fy - 8, fx - 7, fy - 7, col);
			}
		m.popMatrix();
		m.popMatrix();

		// barras de cine
		int barra = Math.round(h * 0.11f);
		g.fill(0, 0, w, barra, 0xFF000000);
		g.fill(0, h - barra, w, h, 0xFF000000);

		// texto de TikTok arriba (tiembla con el beat) y el tema abajo
		float esc = Math.min(2.6f, (w * 0.85f) / Math.max(1, this.font.width(e.titulo())));
		textoBorde(g, e.titulo(), w / 2f + sx, barra + 10 + sy, esc, 0xFFFFFFFF);
		textoBorde(g, e.sub(), w / 2f, h - barra + 6, 1f, 0xFFB388FF);
		g.text(this.font, e.tema(), 8, h - barra - 12, 0xFFFF80AB, true);

		// flash blanco en el arranque y en los golpes fuertes
		if (el < 200) g.fill(0, 0, w, h, ((int) (200 * (1 - el / 200f)) << 24) | 0xFFFFFF);
		else if (golpe > 0.85f) g.fill(0, 0, w, h, 0x30FFFFFF);
		// fundido al final
		long falta = e.duracionMs() - el;
		if (falta < 400) g.fill(0, 0, w, h, ((int) (255 * Math.max(0, 1 - falta / 400f)) << 24));
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
