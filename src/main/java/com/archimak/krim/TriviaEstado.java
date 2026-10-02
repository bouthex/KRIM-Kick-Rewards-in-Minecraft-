package com.archimak.krim;

/** Estado de la pregunta en pantalla, compartido entre el servidor (Trivia) y la pantalla (PreguntaPantalla). */
public final class TriviaEstado {
	public record Pregunta(int id, String categoria, String texto, String[] opciones, boolean[] correctas,
			long inicioMs, int duracionMs, String usuario) {}

	public record Resultado(boolean correcto, String respuestaCorrecta, long inicioMs) {}

	public static volatile Pregunta actual;
	/** -2 sin responder, -1 se cerró la pantalla, 0..3 opción elegida. */
	public static volatile int respuesta = -2;
	public static volatile Resultado resultado;

	private TriviaEstado() { }
}
