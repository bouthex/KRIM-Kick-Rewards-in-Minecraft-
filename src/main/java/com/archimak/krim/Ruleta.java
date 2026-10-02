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

/**
 * Ruleta del destino: animación tipo tragamonedas en el centro de la pantalla y después un premio al azar.
 * Los pedidos se ponen en fila: si canjean 5 seguidas, giran una detrás de otra.
 * También tiene un mini programador de tareas ("hacé esto en N ticks") que usan los premios.
 */
public final class Ruleta {
	private static final int MAX_COLA = 20;
	private static final String[] COLORES = { "red", "gold", "yellow", "green", "aqua", "light_purple" };

	private record Tarea(long cuando, Consumer<MinecraftServer> accion) {}

	private static final ArrayDeque<String> COLA = new ArrayDeque<>();
	private static final List<Tarea> TAREAS = new ArrayList<>();
	private static long reloj;
	private static boolean girando;
	private static int t;
	private static String usuarioActual;
	private static Premios.Premio premio;
	private static boolean proteccion;

	static void iniciar() {
		ServerLifecycleEvents.SERVER_STARTED.register(s -> {
			COLA.clear(); TAREAS.clear(); girando = false;
			proteccion = proteccionActiva();
			if (proteccion) Registro.info("RULETA", "Protección de mobs activa: los hostiles de la ruleta solo dañan al jugador");
			else Registro.aviso("RULETA", "Protección de mobs NO activa en esta versión: la ruleta no va a sacar mobs hostiles");
		});
		ServerTickEvents.END_SERVER_TICK.register(Ruleta::tick);
		// Los mobs de KRIM tampoco sueltan su equipo (arcos, espadas, armaduras) al morir
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

	/** El mixin que evita que lo hostil del chat dañe a otros mobs, ¿se aplicó? */
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

	/** Premio "girá otra vez": el mismo usuario vuelve a girar apenas termine este giro. */
	static void otraVez(String usuario) { COLA.addFirst(usuario); }

	public static void despues(int ticks, Consumer<MinecraftServer> accion) {
		TAREAS.add(new Tarea(reloj + Math.max(1, ticks), accion));
	}

	static ServerPlayer jugador(MinecraftServer s) {
		var l = s.getPlayerList().getPlayers();
		return l.isEmpty() ? null : l.get(0);
	}

	private static void cmd(MinecraftServer s, String c) { Krim.cmd(s, Krim.fuente(s), c); }

	private static void tick(MinecraftServer s) {
		reloj++;
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
			usuarioActual = COLA.poll();
			boolean tregua = p.getHealth() < 8.0F;
			premio = Premios.sortear(s, p, usuarioActual, tregua, proteccion);
			girando = true;
			t = 0;
			Registro.info("RULETA", usuarioActual + " gira" + (tregua ? " (tregua: poca vida)" : "") + " -> " + premio.titulo()
					+ (premio.detalle() != null ? " | " + premio.detalle() : "") + " [" + premio.clase() + "]");
			cmd(s, "title @p times 0 12 0");
		}

		t++;
		boolean cuadro = (t < 20 && t % 2 == 0) || (t >= 20 && t < 36 && t % 3 == 0) || (t >= 36 && t < 52 && t % 5 == 0);
		if (cuadro) {
			List<String> nombres = Premios.NOMBRES_GIRO;
			String n = nombres.get(ThreadLocalRandom.current().nextInt(nombres.size()));
			String color = COLORES[(t / 2) % COLORES.length];
			cmd(s, "title @p subtitle {text:\"Ruleta de " + usuarioActual + "\",color:\"gray\"}");
			cmd(s, "title @p title {text:\"" + n + "\",color:\"" + color + "\"}");
			cmd(s, "execute at @p run playsound minecraft:block.note_block.hat master @p ~ ~ ~ 0.8 " + (0.7 + t / 50.0));
		}
		if (t == 56) revelar(s);
		if (t == 66) {
			ServerPlayer p = jugador(s);
			if (p != null) {
				try { premio.accion().hacer(s, p, usuarioActual); }
				catch (Exception e) { Registro.error("RULETA", "Falló el premio " + premio.titulo(), e); }
			}
		}
		if (t >= 72) girando = false;
	}

	private static void revelar(MinecraftServer s) {
		String sub = premio.detalle() != null ? premio.detalle() : "Ruleta de " + usuarioActual;
		cmd(s, "title @p times 0 55 15");
		cmd(s, "title @p subtitle {text:\"" + sub + "\",color:\"white\"}");
		cmd(s, "title @p title {text:\"» " + premio.titulo() + " «\",color:\"" + premio.color() + "\",bold:true}");
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
