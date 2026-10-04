package com.archimak.krim;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Avisos tipo "tarjeta" arriba de la pantalla antes de que aparezca algo de un canje:
 * quién lo mandó, qué es y una cuenta regresiva. Lo escribe el servidor y lo dibuja KrimCliente.
 * tipo: 0 bueno (verde), 1 interactivo (amarillo), 2 malo (rojo), 3 jefe (negro), 4 funny (rosa).
 */
public final class AvisoEstado {
	public record Aviso(String usuario, String texto, int tipo, long inicioMs, long llegaMs, long finMs) {}

	public static final CopyOnWriteArrayList<Aviso> AVISOS = new CopyOnWriteArrayList<>();

	static void agregar(String usuario, String texto, int tipo, int msHastaLlegar) {
		long ahora = RuedaEstado.ahoraMs();
		AVISOS.removeIf(a -> ahora > a.finMs());
		AVISOS.add(new Aviso(usuario, texto, tipo, ahora, ahora + msHastaLlegar, ahora + msHastaLlegar + 250));
	}

	private AvisoEstado() { }
}
