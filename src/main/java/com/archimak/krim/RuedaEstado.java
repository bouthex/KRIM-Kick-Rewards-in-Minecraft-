package com.archimak.krim;

/**
 * Estado de la rueda que se ve en pantalla. Lo escribe el servidor (Ruleta) y lo lee el cliente (KrimCliente).
 * En un mundo de un jugador los dos corren en el mismo programa, así que se comparte directo, sin red.
 * Los tiempos son milisegundos de System.nanoTime() / 1e6 (mismo reloj en los dos lados).
 */
public final class RuedaEstado {
	public static final int SECTORES = 12;

	/** tipos[i]: 0 bueno (verde), 1 interactivo/neutro (amarillo), 2 malo (rojo). */
	public record Datos(String usuario, String[] etiquetas, int[] tipos, int ganador, long inicioMs, int giroMs, int totalMs,
			float anguloFinal, String titulo, String detalle, int colorResultado, int enFila) {

		/** Rotación de la rueda (grados) a los ms indicados desde el inicio. */
		public float rotacion(long transcurrido) {
			if (transcurrido >= giroMs) return anguloFinal;
			double t = Math.max(0, transcurrido) / (double) giroMs;
			return (float) (anguloFinal * (1 - Math.pow(1 - t, 4)));
		}

		/** Sector que está bajo el puntero (arriba) con esta rotación. */
		public static int sectorBajoPuntero(float rot) {
			double a = ((270.0 - rot) % 360.0 + 360.0) % 360.0;
			return (int) (a / (360.0 / SECTORES)) % SECTORES;
		}
	}

	public static volatile Datos actual;

	public static long ahoraMs() { return System.nanoTime() / 1_000_000L; }

	private RuedaEstado() { }
}
