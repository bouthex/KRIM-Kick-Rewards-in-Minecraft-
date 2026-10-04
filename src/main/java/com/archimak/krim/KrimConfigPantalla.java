package com.archimak.krim;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuración de KRIM (se abre desde Mod Menu): qué puede salir en la ruleta, jumpscare,
 * tiempo de los avisos, tamaño de la rueda, tregua y clics. Se guarda en config/krim.properties.
 */
public class KrimConfigPantalla extends Screen {
	private static final String[][] NOMBRES = {
			{ "mobs", "Mobs hostiles" }, { "castigos", "Castigos" }, { "jefes", "Jefes de mentira" }, { "funnys", "Funnys (rosa)" },
			{ "wachin", "WACHÍN (bardeos)" }, { "preguntas", "Preguntas" }, { "parlante", "Mob parlante" },
			{ "burlas", "Mapas, papeles y libros" }, { "mensajes", "Mensajes y frases" }, { "locuras", "Locuras" },
			{ "regalos", "Regalos" } };
	private static final String[] AVISOS = { "Instantáneo", "Corto", "1 segundo", "2 segundos" };
	private static final String[] RUEDA = { "Chica", "Normal", "Grande" };

	private final Screen anterior;
	private final List<Button> botones = new ArrayList<>();
	private int x0, y0, ancho, alto;

	public KrimConfigPantalla(Screen anterior) {
		super(Component.literal("KRIM - Configuración"));
		this.anterior = anterior;
	}

	@Override
	protected void init() {
		botones.clear();
		ancho = Math.min(420, this.width - 20);
		int col = (ancho - 30) / 2, bh = 20;
		alto = 66 + 9 * (bh + 4) + 34;
		x0 = (this.width - ancho) / 2;
		y0 = Math.max(6, (this.height - alto) / 2);
		int xa = x0 + 10, xb = x0 + 20 + col;

		// Columna izquierda: qué puede salir en la ruleta
		int y = y0 + 40;
		for (String[] n : NOMBRES) {
			if (y > y0 + 40 + 8 * (bh + 4)) break;
			String clave = n[0], texto = n[1];
			botones.add(this.addRenderableWidget(Button.builder(etiqueta(texto, Config.categoria(clave)), b -> {
				Config.categorias.put(clave, !Config.categoria(clave));
				b.setMessage(etiqueta(texto, Config.categoria(clave)));
			}).bounds(xa, y, col, bh).build()));
			y += bh + 4;
		}

		// Columna derecha: el resto de las categorías y las opciones
		y = y0 + 40;
		for (int i = 9; i < NOMBRES.length; i++) {
			String clave = NOMBRES[i][0], texto = NOMBRES[i][1];
			botones.add(this.addRenderableWidget(Button.builder(etiqueta(texto, Config.categoria(clave)), b -> {
				Config.categorias.put(clave, !Config.categoria(clave));
				b.setMessage(etiqueta(texto, Config.categoria(clave)));
			}).bounds(xb, y, col, bh).build()));
			y += bh + 4;
		}
		botones.add(this.addRenderableWidget(Button.builder(etiqueta("Jumpscare", Config.jumpscare), b -> {
			Config.jumpscare = !Config.jumpscare;
			b.setMessage(etiqueta("Jumpscare", Config.jumpscare));
		}).bounds(xb, y, col, bh).build()));
		y += bh + 4;
		botones.add(this.addRenderableWidget(Button.builder(opcion("Avisos", AVISOS[Config.avisoModo]), b -> {
			Config.avisoModo = (Config.avisoModo + 1) % AVISOS.length;
			b.setMessage(opcion("Avisos", AVISOS[Config.avisoModo]));
		}).bounds(xb, y, col, bh).build()));
		y += bh + 4;
		botones.add(this.addRenderableWidget(Button.builder(opcion("Rueda", RUEDA[Config.tamanoRueda]), b -> {
			Config.tamanoRueda = (Config.tamanoRueda + 1) % RUEDA.length;
			b.setMessage(opcion("Rueda", RUEDA[Config.tamanoRueda]));
		}).bounds(xb, y, col, bh).build()));
		y += bh + 4;
		botones.add(this.addRenderableWidget(Button.builder(opcion("Tregua", tregua()), b -> {
			Config.treguaVida = switch (Config.treguaVida) { case 0 -> 4; case 4 -> 8; case 8 -> 12; default -> 0; };
			b.setMessage(opcion("Tregua", tregua()));
		}).bounds(xb, y, col, bh).build()));
		y += bh + 4;
		botones.add(this.addRenderableWidget(Button.builder(etiqueta("Clics de la rueda", Config.clicsRueda), b -> {
			Config.clicsRueda = !Config.clicsRueda;
			b.setMessage(etiqueta("Clics de la rueda", Config.clicsRueda));
		}).bounds(xb, y, col, bh).build()));

		// Abajo: guardar
		this.addRenderableWidget(Button.builder(Component.literal("Guardar y salir"), b -> onClose())
				.bounds(this.width / 2 - 75, y0 + alto - 28, 150, 20).build());
	}

	private static String tregua() {
		return Config.treguaVida == 0 ? "Sin tregua" : "menos de " + (Config.treguaVida / 2) + " corazones";
	}

	private static Component etiqueta(String texto, boolean si) {
		return Component.literal((si ? "§a✔ " : "§c✘ ") + "§f" + texto);
	}

	private static Component opcion(String texto, String valor) {
		return Component.literal("§f" + texto + ": §e" + valor);
	}

	@Override
	public void onClose() {
		Config.guardar();
		this.minecraft.gui.setScreen(anterior);
	}

	/** Fondo normal de las pantallas y encima el panel con la estética de la ruleta (debajo de los botones). */
	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractBackground(g, mouseX, mouseY, delta);
		g.fill(x0 - 4, y0 - 4, x0 + ancho + 4, y0 + alto + 4, 0x88000000);
		g.fill(x0 - 2, y0 - 2, x0 + ancho + 2, y0 + alto + 2, 0xFFB8860B);
		g.fill(x0 - 1, y0 - 1, x0 + ancho + 1, y0 + alto + 1, 0xFFFFC107);
		g.fillGradient(x0, y0, x0 + ancho, y0 + alto, 0xF0181018, 0xF00A0A0A);
		long t = RuedaEstado.ahoraMs() / 140;
		for (int i = 0; i < 14; i++) {
			int lx = x0 + 10 + i * (ancho - 20) / 13;
			int luz = ((i + t) % 2 == 0) ? 0xFFFFFDE7 : 0xFFFF6F00;
			g.fill(lx - 1, y0 - 4, lx + 2, y0 - 1, luz);
			g.fill(lx - 1, y0 + alto + 1, lx + 2, y0 + alto + 4, luz);
		}
		// calavera chiquita + título
		Matrix3x2fStack m = g.pose();
		m.pushMatrix();
		m.translate(x0 + 22, y0 + 18);
		m.scale(0.6f, 0.6f);
		FunnyPantalla.calavera(g, 0, 0f);
		m.popMatrix();
		m.pushMatrix();
		m.translate(this.width / 2f, y0 + 8);
		m.scale(1.6f, 1.6f);
		String tit = "KRIM";
		g.text(this.font, tit, -this.font.width(tit) / 2, 0, 0xFFFFD54F, true);
		m.popMatrix();
		String sub = "Kick Rewards in Minecraft · " + Krim.VERSION;
		g.text(this.font, sub, this.width / 2 - this.font.width(sub) / 2, y0 + 25, 0xFF9E9E9E, false);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
	}
}
