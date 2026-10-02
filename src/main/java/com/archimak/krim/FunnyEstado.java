package com.archimak.krim;

/** Efecto "funny" en pantalla (edit de TikTok, créditos, apagón, jumpscare), compartido entre servidor y pantalla. */
public final class FunnyEstado {
	public record Efecto(int id, String tipo, long inicioMs, int duracionMs, String titulo, String sub, String tema,
			int[] golpesMs, String[] lineas, int variante) {}

	public static volatile Efecto actual;

	private FunnyEstado() { }
}
