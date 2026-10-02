package com.archimak.krim;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla de pregunta estilo Kahoot: panel oscuro con borde dorado, la pregunta arriba,
 * 4 respuestas en cuadros de colores (A rojo, B azul, C amarillo, D verde) y una barra de tiempo.
 * Debajo de cada cuadro hay un botón real (invisible) que recibe el clic.
 * No pausa el juego. Cerrarla con Esc cuenta como "me rindo".
 */
public class PreguntaPantalla extends Screen {
	private static final int[] COLORES = { 0xFFE21B3C, 0xFF1368CE, 0xFFD89E00, 0xFF26890C };
	private static final int[] COLORES_HOVER = { 0xFFFF4D6A, 0xFF3D8BF2, 0xFFFFBF1F, 0xFF3FB521 };
	private static final String[] LETRAS = { "A", "B", "C", "D" };

	private final TriviaEstado.Pregunta p;
	private final Button[] botones = new Button[4];
	private boolean respondida;
	private int x0, y0, pw, ph;

	public PreguntaPantalla(TriviaEstado.Pregunta p) {
		super(Component.literal("Pregunta"));
		this.p = p;
	}

	public TriviaEstado.Pregunta pregunta() { return p; }

	public boolean respondida() { return respondida; }

	@Override
	protected void init() {
		pw = Math.min(380, this.width - 20);
		ph = 196;
		x0 = (this.width - pw) / 2;
		y0 = (this.height - ph) / 2;
		int bw = (pw - 30) / 2, bh = 32;
		for (int i = 0; i < 4; i++) {
			int bx = x0 + 10 + (i % 2) * (bw + 10);
			int by = y0 + 98 + (i / 2) * (bh + 8);
			final int idx = i;
			botones[i] = Button.builder(Component.empty(), b -> elegir(idx)).bounds(bx, by, bw, bh).build();
			this.addRenderableWidget(botones[i]);
		}
	}

	private void elegir(int i) {
		if (respondida) return;
		respondida = true;
		TriviaEstado.respuesta = i;
		this.minecraft.gui.setScreen(null);
	}

	@Override
	public boolean isPauseScreen() { return false; }

	@Override
	public void onClose() {
		if (!respondida) { respondida = true; TriviaEstado.respuesta = -1; }
		this.minecraft.gui.setScreen(null);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		long el = RuedaEstado.ahoraMs() - p.inicioMs();
		boolean especial = p.categoria().equals("PREGUNTA ESPECIAL");

		// Panel con sombra y borde dorado
		g.fill(x0 - 4, y0 - 4, x0 + pw + 4, y0 + ph + 4, 0x88000000);
		g.fill(x0 - 2, y0 - 2, x0 + pw + 2, y0 + ph + 2, 0xFFB8860B);
		g.fill(x0 - 1, y0 - 1, x0 + pw + 1, y0 + ph + 1, 0xFFFFC107);
		g.fillGradient(x0, y0, x0 + pw, y0 + ph, 0xF01A1030, 0xF00D0A1A);

		// Cabecera
		int cab1 = especial ? 0xFFE91E63 : 0xFF6A1B9A, cab2 = especial ? 0xFFFF9800 : 0xFF283593;
		g.fillGradient(x0, y0, x0 + pw, y0 + 22, cab1, cab2);
		centrado(g, p.categoria() + "  ·  de " + p.usuario(), this.width / 2f, y0 + 7, 1f, 0xFFFFFFFF);

		// Luces del borde (como la rueda)
		int fase = (int) (el / 120);
		for (int k = 0; k < 12; k++) {
			int lx = x0 + 8 + k * (pw - 16) / 11;
			int luz = ((k + fase) % 2 == 0) ? 0xFFFFFDE7 : 0xFFFF6F00;
			g.fill(lx - 1, y0 + ph + 1, lx + 2, y0 + ph + 4, luz);
			g.fill(lx - 1, y0 - 4, lx + 2, y0 - 1, luz);
		}

		// Pregunta (hasta 3 líneas)
		List<String> lineas = partir(p.texto(), (int) ((pw - 24) / 1.25f));
		float y = y0 + 32 + (3 - Math.min(3, lineas.size())) * 7;
		for (int i = 0; i < Math.min(3, lineas.size()); i++) {
			centrado(g, lineas.get(i), this.width / 2f, y, 1.25f, 0xFFFFFFFF);
			y += 14;
		}

		// Respuestas
		for (int i = 0; i < 4; i++) {
			Button b = botones[i];
			if (b == null) continue;
			boolean hover = b.isHovered();
			int bx = b.getX(), by = b.getY(), bw = b.getWidth(), bh = b.getHeight();
			g.fill(bx + 2, by + 2, bx + bw + 2, by + bh + 2, 0xAA000000);
			g.fill(bx, by, bx + bw, by + bh, hover ? COLORES_HOVER[i] : COLORES[i]);
			g.fill(bx, by + bh - 3, bx + bw, by + bh, 0x40000000);
			if (hover) g.outline(bx - 1, by - 1, bw + 2, bh + 2, 0xFFFFFFFF);
			g.fill(bx + 4, by + 4, bx + 24, by + bh - 4, 0x55000000);
			centrado(g, LETRAS[i], bx + 14, by + bh / 2f - 4, 1f, 0xFFFFFFFF);
			String op = p.opciones()[i];
			float esc = Math.min(1f, (bw - 34f) / Math.max(1, this.font.width(op)));
			centrado(g, op, bx + 28 + (bw - 32) / 2f, by + bh / 2f - 4 * esc, esc, 0xFFFFFFFF);
		}

		// Barra de tiempo
		float resto = Math.max(0f, 1f - el / (float) p.duracionMs());
		int seg = (int) Math.ceil(resto * p.duracionMs() / 1000f);
		int barra = Math.round((pw - 20) * resto);
		int colBarra = resto > 0.5f ? 0xFF43A047 : resto > 0.25f ? 0xFFFDD835 : 0xFFE53935;
		g.fill(x0 + 10, y0 + ph - 14, x0 + pw - 10, y0 + ph - 8, 0xFF2A2A2A);
		g.fill(x0 + 10, y0 + ph - 14, x0 + 10 + barra, y0 + ph - 8, colBarra);
		centrado(g, seg + " s", x0 + pw - 22, y0 + ph - 25, 0.8f, 0xFFDDDDDD);
		centrado(g, "Esc = me rindo", x0 + 40, y0 + ph - 25, 0.7f, 0xFF888888);
	}

	private void centrado(GuiGraphicsExtractor g, String t, float x, float y, float esc, int color) {
		Matrix3x2fStack m = g.pose();
		m.pushMatrix();
		m.translate(x, y);
		m.scale(esc, esc);
		g.text(this.font, t, -this.font.width(t) / 2, 0, color, true);
		m.popMatrix();
	}

	private List<String> partir(String t, int ancho) {
		List<String> res = new ArrayList<>();
		StringBuilder linea = new StringBuilder();
		for (String palabra : t.split(" ")) {
			String prueba = linea.length() == 0 ? palabra : linea + " " + palabra;
			if (this.font.width(prueba) > ancho && linea.length() > 0) {
				res.add(linea.toString());
				linea = new StringBuilder(palabra);
			} else {
				linea = new StringBuilder(prueba);
			}
		}
		if (linea.length() > 0) res.add(linea.toString());
		return res;
	}
}
