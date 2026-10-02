package com.archimak.krim;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Ruleta del destino: rueda gráfica en pantalla (ver KrimCliente) que gira ~5 s y frena en un premio.
 * Fila de giros, sonidos sincronizados con la rueda, tareas programadas para los premios
 * y limpieza de equipo de los mobs de KRIM al morir.
 */
public final class Ruleta {
	private static final int MAX_COLA = 20;
	private static final int GIRO_MS = 5200, ACCION_MS = 700, TOTAL_MS = 9000;

	private record Tarea(long cuando, Consumer<MinecraftServer> accion) {}

	private static final ArrayDeque<String> COLA = new ArrayDeque<>();
	private static final List<Tarea> TAREAS = new ArrayList<>();
	private static long reloj;
	private static boolean girando, revelado, hecho, limpiado;
	private static int ultimoSector = -1;
	private static String usuarioActual;
	private static Premios.Premio premio;
	private static RuedaEstado.Datos rueda;
	private static boolean proteccion;

	static void iniciar() {
		ServerLifecycleEvents.SERVER_STARTED.register(s -> {
			COLA.clear(); TAREAS.clear(); girando = false; limpiado = false; RuedaEstado.actual = null;
			proteccion = proteccionActiva();
			if (proteccion) Registro.info("RULETA", "Protección de mobs activa: los hostiles de la ruleta solo dañan al jugador");
			else Registro.aviso("RULETA", "Protección de mobs NO activa en esta versión: la ruleta no va a sacar mobs hostiles");
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(s -> RuedaEstado.actual = null);
		ServerTickEvents.END_SERVER_TICK.register(Ruleta::tick);
		ServerLivingEntityEvents.ALLOW_DEATH.register((entidad, fuente, dano) -> {
			if (Krim.esDelChat(entidad)) vaciarEquipo(entidad);
			return true;
		});
	}

	private static void vaciarEquipo(LivingEntity e) {
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			try { e.setItemSlot(slot, ItemStack.EMPTY); } catch (Exception ignored) { }
		}
	}

	private static boolean proteccionActiva() {
		try {
			for (var m : LivingEntity.class.getDeclaredMethods()) if (m.getName().contains("krim$sinDanoAOtros")) return true;
		} catch (Exception ignored) { }
		return false;
	}

	public static void pedir(MinecraftServer s, ServerPlayer p, String usuario) {
		if (COLA.size() >= MAX_COLA) {
			Registro.aviso("RULETA", "Fila llena, se descarta el giro de " + usuario);
			return;
		}
		COLA.add(usuario);
		Registro.info("RULETA", "Giro de " + usuario + " en fila (" + COLA.size() + ")");
	}

	static void otraVez(String usuario) { COLA.addFirst(usuario); }

	public static void despues(int ticks, Consumer<MinecraftServer> accion) {
		TAREAS.add(new Tarea(reloj + Math.max(1, ticks), accion));
	}

	/** Repite "paso" cada tick hasta que devuelva true o pasen maxTicks. */
	static void repetir(int maxTicks, Predicate<MinecraftServer> paso) {
		long limite = reloj + maxTicks;
		Consumer<MinecraftServer>[] uno = new Consumer[1];
		uno[0] = sv -> {
			boolean listo;
			try { listo = paso.test(sv); } catch (Exception e) { Registro.error("RULETA", "Falló una tarea repetida", e); listo = true; }
			if (!listo && reloj < limite) despues(1, uno[0]);
		};
		despues(1, uno[0]);
	}

	static ServerPlayer jugador(MinecraftServer s) {
		var l = s.getPlayerList().getPlayers();
		return l.isEmpty() ? null : l.get(0);
	}

	private static void cmd(MinecraftServer s, String c) { Krim.cmd(s, Krim.fuente(s), c); }

	private static void tick(MinecraftServer s) {
		reloj++;
		if (!limpiado && jugador(s) != null) {
			limpiado = true;
			Atributos.limpiar(s); // por si el juego se cerró con un castigo puesto
		}
		if (!TAREAS.isEmpty()) {
			List<Tarea> listas = new ArrayList<>();
			TAREAS.removeIf(ta -> { if (ta.cuando() <= reloj) { listas.add(ta); return true; } return false; });
			for (Tarea ta : listas) {
				try { ta.accion().accept(s); } catch (Exception e) { Registro.error("RULETA", "Falló una tarea programada", e); }
			}
		}

		if (!girando) {
			if (COLA.isEmpty()) return;
			ServerPlayer p = jugador(s);
			if (p == null) return;
			empezar(s, p);
			return;
		}

		long el = RuedaEstado.ahoraMs() - rueda.inicioMs();
		if (el < GIRO_MS) {
			int sector = RuedaEstado.Datos.sectorBajoPuntero(rueda.rotacion(el));
			if (sector != ultimoSector) {
				ultimoSector = sector;
				double tono = 0.8 + 0.8 * Math.min(1.0, el / (double) GIRO_MS);
				cmd(s, "execute at @p run playsound minecraft:block.note_block.hat master @p ~ ~ ~ 0.7 " + tono);
			}
		}
		if (el >= GIRO_MS && !revelado) { revelado = true; revelar(s); }
		if (el >= GIRO_MS + ACCION_MS && !hecho) {
			hecho = true;
			ServerPlayer p = jugador(s);
			if (p != null) {
				try { premio.accion().hacer(s, p, usuarioActual); }
				catch (Exception e) { Registro.error("RULETA", "Falló el premio " + premio.titulo(), e); }
			}
		}
		if (el >= TOTAL_MS) {
			girando = false;
			if (RuedaEstado.actual == rueda) RuedaEstado.actual = null;
		}
	}

	private static void empezar(MinecraftServer s, ServerPlayer p) {
		usuarioActual = COLA.poll();
		boolean tregua = p.getHealth() < 8.0F;
		premio = Premios.sortear(s, p, usuarioActual, tregua, proteccion);
		Registro.info("RULETA", usuarioActual + " gira" + (tregua ? " (tregua: poca vida)" : "") + " -> " + premio.titulo()
				+ (premio.detalle() != null ? " | " + premio.detalle() : "") + " [" + premio.clase() + "]");

		ThreadLocalRandom r = ThreadLocalRandom.current();
		int ganador = r.nextInt(RuedaEstado.SECTORES);
		String[] etiquetas = new String[RuedaEstado.SECTORES];
		List<String> usadas = new ArrayList<>();
		usadas.add(premio.etiqueta());
		for (int i = 0; i < RuedaEstado.SECTORES; i++) {
			if (i == ganador) { etiquetas[i] = premio.etiqueta(); continue; }
			String e;
			int intentos = 0;
			do { e = Premios.etiquetaFalsa(); } while (usadas.contains(e) && ++intentos < 20);
			usadas.add(e);
			etiquetas[i] = e;
		}
		float ancho = 360f / RuedaEstado.SECTORES;
		float base = ((255f - ganador * ancho) % 360f + 360f) % 360f;
		float jitter = (float) r.nextDouble(-ancho * 0.35, ancho * 0.35);
		float anguloFinal = 360f * r.nextInt(5, 8) + base + jitter;

		rueda = new RuedaEstado.Datos(usuarioActual, etiquetas, ganador, RuedaEstado.ahoraMs(), GIRO_MS, TOTAL_MS,
				anguloFinal, premio.titulo(), premio.detalle(), Premios.argb(premio.color()), COLA.size());
		RuedaEstado.actual = rueda;
		girando = true; revelado = false; hecho = false; ultimoSector = -1;
		cmd(s, "execute at @p run playsound minecraft:block.beacon.activate master @p ~ ~ ~ 0.8 1.4");
	}

	private static void revelar(MinecraftServer s) {
		String sonido = switch (premio.clase()) {
			case BUENO -> "minecraft:ui.toast.challenge_complete";
			case MALO -> "minecraft:entity.villager.no";
			case HOSTIL -> "minecraft:entity.wither.shoot";
			default -> "minecraft:entity.player.levelup";
		};
		cmd(s, "execute at @p run playsound " + sonido + " master @p ~ ~ ~ 1 1");
		cmd(s, "execute at @p run particle minecraft:dust{color:[1.0,0.82,0.25],scale:1.3} ~ ~1.2 ~ 0.7 0.9 0.7 0 40 force");
		cmd(s, "tellraw @a [{text:\"[Ruleta de " + usuarioActual + "] \",color:\"gold\"},{text:\"" + premio.titulo() + "\",color:\""
				+ premio.color() + "\",bold:true}" + (premio.detalle() != null ? ",{text:\" — " + premio.detalle() + "\",color:\"gray\"}" : "") + "]");
	}
}
