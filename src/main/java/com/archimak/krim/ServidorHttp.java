package com.archimak.krim;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Mini servidor HTTP solo en 127.0.0.1 (nadie de afuera de tu PC puede usarlo).
 * GET /creeper?nombre=...&usuario=...
 */
public final class ServidorHttp {

	static void iniciar(int puerto) {
		Thread hilo = new Thread(() -> {
			try (ServerSocket ss = new ServerSocket(puerto, 50, InetAddress.getLoopbackAddress())) {
				Registro.info("HTTP", "Escuchando en http://127.0.0.1:" + puerto);
				while (true) {
					try (Socket c = ss.accept()) {
						atender(c);
					} catch (Exception e) {
						Registro.error("HTTP", "Error atendiendo pedido", e);
					}
				}
			} catch (Exception e) {
				Registro.error("HTTP", "No se pudo abrir el puerto " + puerto + " (¿otro programa lo usa? cambialo en krim.properties)", e);
			}
		}, "Krim-HTTP");
		hilo.setDaemon(true);
		hilo.start();
	}

	private static void atender(Socket c) throws Exception {
		c.setSoTimeout(3000);
		BufferedReader in = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8));
		String linea = in.readLine();
		if (linea == null) return;
		String h;
		while ((h = in.readLine()) != null && !h.isEmpty()) { /* descartar headers */ }

		String[] partes = linea.split(" ");
		if (partes.length < 2) {
			Registro.aviso("HTTP", "Pedido invalido: " + linea);
			responder(c, 400, "pedido invalido");
			return;
		}
		String destino = partes[1];
		int q = destino.indexOf('?');
		String ruta = q >= 0 ? destino.substring(0, q) : destino;
		try { ruta = URLDecoder.decode(ruta, StandardCharsets.UTF_8); } catch (IllegalArgumentException ignored) { }
		Map<String, String> params = q >= 0 ? parsear(destino.substring(q + 1)) : new HashMap<>();

		switch (ruta) {
			case "/creeper" -> {
				String usuario = usuario(params);
				String nombre = Config.limpiar(primero(params, "nombre", "texto"));
				if (nombre.isEmpty()) nombre = usuario.equals("El chat") ? "Creeper del chat" : usuario;
				String n = nombre;
				boolean ok = Krim.encolar("creeper", "nombre=\"" + n + "\" usuario=\"" + usuario + "\"",
						(s, p) -> Krim.conAviso(s, usuario, "te manda un CREEPER", 2, 700, (sv, pl) -> Krim.creeper(sv, pl, n, usuario)));
				responder(c, ok ? 200 : 503, ok ? "ok" : "no hay ningun mundo abierto");
			}
			case "/cartel" -> {
				String usuario = usuario(params);
				String texto = Config.limpiar(primero(params, "texto", "nombre"), 45);
				if (texto.isEmpty()) texto = "Saludos!";
				String t = texto;
				boolean ok = Krim.encolar("cartel", "texto=\"" + t + "\" usuario=\"" + usuario + "\"",
						(s, p) -> Krim.conAviso(s, usuario, "te deja un CARTEL", 1, (sv, pl) -> Cartel.colocar(sv, pl, t, usuario)));
				responder(c, ok ? 200 : 503, ok ? "ok" : "no hay ningun mundo abierto");
			}
			case "/mascota" -> {
				String usuario = usuario(params);
				String tipo = params.getOrDefault("tipo", "aleatorio").trim().toLowerCase();
				String nombre = Config.limpiar(primero(params, "nombre", "texto"));
				if (nombre.isEmpty()) nombre = usuario.equals("El chat") ? "Mascota del chat" : usuario;
				String n = nombre;
				boolean ok = Krim.encolar("mascota", "tipo=" + tipo + " nombre=\"" + n + "\" usuario=\"" + usuario + "\"",
						(s, p) -> Krim.conAviso(s, usuario, "te regala una MASCOTA", 0, (sv, pl) -> Mascota.crear(sv, pl, tipo, n, usuario)));
				responder(c, ok ? 200 : 503, ok ? "ok" : "no hay ningun mundo abierto");
			}
			case "/tamano", "/tamaño" -> {
				String usuario = usuario(params);
				boolean ok = Krim.encolar("tamano", "usuario=\"" + usuario + "\"", (s, p) -> Tamano.aplicar(s, p, usuario));
				responder(c, ok ? 200 : 503, ok ? "ok" : "no hay ningun mundo abierto");
			}
			case "/hostil" -> {
				String usuario = usuario(params);
				boolean ok = Krim.encolar("hostil", "usuario=\"" + usuario + "\"", (s, p) -> Premios.hostilCanje(s, p, usuario));
				responder(c, ok ? 200 : 503, ok ? "ok" : "no hay ningun mundo abierto");
			}
			case "/ruleta" -> {
				String usuario = usuario(params);
				String forzar = params.get("forzar") == null ? null : params.get("forzar").trim().toLowerCase();
				boolean ok = Krim.encolar("ruleta", "usuario=\"" + usuario + "\"" + (forzar != null ? " forzar=" + forzar : ""),
						(s, p) -> Ruleta.pedir(s, p, usuario, forzar));
				responder(c, ok ? 200 : 503, ok ? "ok" : "no hay ningun mundo abierto");
			}
			case "/estado" -> responder(c, 200,
					"KRIM " + Krim.VERSION + "\n"
					+ "Minecraft " + Krim.VERSION_MC + "\n"
					+ "Mundo abierto: " + (Krim.hayMundo() ? "si" : "no") + "\n"
					+ "Canjes recibidos en esta sesion: " + Krim.PEDIDOS.get() + "\n"
					+ "Explosiones de creepers del chat: " + Krim.EXPLOSIONES.get() + "\n"
					+ "Puerto: " + Config.puerto + "\n");
			case "/" -> responder(c, 200, "KRIM activo. Rutas: /creeper /cartel /mascota /tamano /ruleta /hostil /estado");
			default -> {
				Registro.aviso("HTTP", "Ruta desconocida: " + ruta);
				responder(c, 404, "no existe");
			}
		}
	}

	private static String usuario(Map<String, String> params) {
		String u = Config.limpiar(params.get("usuario"));
		return u.isEmpty() ? "El chat" : u;
	}

	private static String primero(Map<String, String> params, String a, String b) {
		String v = params.get(a);
		return v == null || v.isBlank() ? params.get(b) : v;
	}

	private static Map<String, String> parsear(String query) {
		Map<String, String> m = new HashMap<>();
		for (String par : query.split("&")) {
			int i = par.indexOf('=');
			if (i <= 0) continue;
			try {
				m.put(URLDecoder.decode(par.substring(0, i), StandardCharsets.UTF_8),
						URLDecoder.decode(par.substring(i + 1), StandardCharsets.UTF_8));
			} catch (IllegalArgumentException ignored) { }
		}
		return m;
	}

	private static void responder(Socket c, int codigo, String cuerpo) throws Exception {
		byte[] b = cuerpo.getBytes(StandardCharsets.UTF_8);
		String estado = codigo == 200 ? "OK" : codigo == 404 ? "Not Found" : codigo == 503 ? "Service Unavailable" : "Bad Request";
		String cab = "HTTP/1.1 " + codigo + " " + estado + "\r\n"
				+ "Content-Type: text/plain; charset=utf-8\r\n"
				+ "Content-Length: " + b.length + "\r\n"
				+ "Connection: close\r\n\r\n";
		OutputStream out = c.getOutputStream();
		out.write(cab.getBytes(StandardCharsets.US_ASCII));
		out.write(b);
		out.flush();
	}
}
