package com.archimak.krim;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TraceableEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

/** KRIM - Kick Rewards in Minecraft. */
public class Krim implements ModInitializer {
	public static final String MOD_ID = "krim";
	/** Tag que marca a los creepers del chat. NO cambiar: los creepers del chat que ya existen en el mundo tienen este tag. */
	public static final String TAG = "kick_creeper";
	/** Tag de los animales del chat (tampoco cambiar). */
	public static final String TAG_ANIMAL = "krim_mascota";
	/** Tag de todo lo que invoca la ruleta (tampoco cambiar). */
	public static final String TAG_RULETA = "krim_ruleta";
	/** Botín vacío: los mobs del chat no sueltan ítems al morir. */
	public static final String SIN_BOTIN = "DeathLootTable:\"minecraft:empty\"";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final String VERSION = FabricLoader.getInstance().getModContainer(MOD_ID)
			.map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("?");
	public static final String VERSION_MC = FabricLoader.getInstance().getModContainer("minecraft")
			.map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("?");

	private static volatile MinecraftServer servidor;
	public static final AtomicInteger PEDIDOS = new AtomicInteger();
	public static final AtomicInteger EXPLOSIONES = new AtomicInteger();

	public static boolean hayMundo() { return servidor != null; }

	@Override
	public void onInitialize() {
		Registro.iniciar();
		Config.cargar();
		Registro.info("INICIO", "KRIM " + VERSION + " | Minecraft " + VERSION_MC
				+ " | Java " + System.getProperty("java.version") + " | puerto " + Config.puerto
				+ " | palabras bloqueadas: " + Config.bloqueadas.size());
		ServerLifecycleEvents.SERVER_STARTED.register(s -> {
			servidor = s;
			Registro.info("MUNDO", "Mundo abierto, listo para recibir canjes");
			Etiquetas.metodo(); // deja registrado en el log qué método de tags usa esta versión
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(s -> {
			servidor = null;
			Registro.info("MUNDO", "Mundo cerrado");
		});
		Tamano.iniciar();
		Ruleta.iniciar();
		Trivia.iniciar();
		ServidorHttp.iniciar(Config.puerto);
	}

	/**
	 * Llamado desde el hilo HTTP. Encola la acción en el hilo del juego (funciona con cualquier menú abierto).
	 * Devuelve false si no hay un mundo abierto.
	 */
	public static boolean encolar(String tipo, String detalle, BiConsumer<MinecraftServer, ServerPlayer> accion) {
		MinecraftServer s = servidor;
		if (s == null) {
			Registro.aviso("CANJE", "Rechazado (no hay mundo abierto): " + tipo + " " + detalle);
			return false;
		}
		int n = PEDIDOS.incrementAndGet();
		Registro.info("CANJE", "#" + n + " " + tipo + ": " + detalle);
		s.execute(() -> {
			ServerPlayer p = jugador(s);
			if (p == null) {
				Registro.aviso("CANJE", "#" + n + " sin jugador en el mundo, se descarta");
				return;
			}
			try {
				accion.accept(s, p);
			} catch (Exception e) {
				Registro.error("CANJE", "#" + n + " " + tipo + " falló", e);
			}
		});
		return true;
	}

	private static ServerPlayer jugador(MinecraftServer s) {
		List<ServerPlayer> l = s.getPlayerList().getPlayers();
		return l.isEmpty() ? null : l.get(0);
	}

	// ---------- Creeper ----------
	public static void creeper(MinecraftServer s, ServerPlayer p, String nombre, String usuario) {
		CommandSourceStack src = fuente(s);
		String nbt = "{Tags:[\"" + TAG + "\",\"kc_nuevo\"],CustomNameVisible:1b," + SIN_BOTIN + ","
				+ "CustomName:{text:\"" + nombre + "\",color:\"green\"}}";
		// 1) Detrás del jugador si hay lugar libre; 2) si no, al lado
		cmd(s, src, "execute as @p at @s positioned ^ ^ ^-3 if block ~ ~ ~ #minecraft:air if block ~ ~1 ~ #minecraft:air run summon minecraft:creeper ~ ~ ~ " + nbt);
		cmd(s, src, "execute unless entity @e[tag=kc_nuevo] as @p at @s run summon minecraft:creeper ~ ~ ~ " + nbt);
		cmd(s, src, "tag @e[tag=kc_nuevo] remove kc_nuevo");
		Registro.info("SPAWN", "Creeper \"" + nombre + "\" creado");
		aviso(s, src, usuario + " te mandó un creeper", "green");
	}

	// ---------- Quién es "del chat" ----------
	/** Cualquier entidad creada por KRIM (creeper, animales, ruleta). */
	public static boolean esDelChat(Entity e) {
		return e != null && (Etiquetas.tiene(e, TAG) || Etiquetas.tiene(e, TAG_ANIMAL) || Etiquetas.tiene(e, TAG_RULETA));
	}

	/** Creepers y cosas de la ruleta, o sus proyectiles (flechas, bolas de fuego, TNT): solo pueden dañar al jugador. */
	public static boolean esHostilDelChat(Entity e) {
		if (e == null) return false;
		if (Etiquetas.tiene(e, TAG) || Etiquetas.tiene(e, TAG_RULETA)) return true;
		if (e instanceof TraceableEntity t) {
			Entity dueno = t.getOwner();
			return dueno != null && (Etiquetas.tiene(dueno, TAG) || Etiquetas.tiene(dueno, TAG_RULETA));
		}
		return false;
	}

	// ---------- Aviso previo + acción al segundo ----------
	static final int AVISO_MS = 1000;

	/** Muestra la tarjeta de aviso y, 1 segundo después, hace la acción. */
	static void conAviso(MinecraftServer s, String usuario, String texto, int tipo, BiConsumer<MinecraftServer, ServerPlayer> accion) {
		conAviso(s, usuario, texto, tipo, AVISO_MS, accion);
	}

	static void conAviso(MinecraftServer s, String usuario, String texto, int tipo, int msPedido, BiConsumer<MinecraftServer, ServerPlayer> accion) {
		int ms = switch (Config.avisoModo) { case 0 -> 50; case 2 -> 1000; case 3 -> 2000; default -> msPedido; };
		AvisoEstado.agregar(usuario, texto, tipo, ms);
		cmd(s, fuente(s), "execute at @p run playsound minecraft:block.note_block.bell master @p ~ ~ ~ 0.8 " + (tipo == 2 ? "0.8" : "1.3"));
		Ruleta.despues(Math.max(1, ms / 50), sv -> {
			ServerPlayer p = Ruleta.jugador(sv);
			if (p == null) return;
			try { accion.accept(sv, p); } catch (Exception e) { Registro.error("CANJE", "Falló después del aviso: " + texto, e); }
		});
	}

	// ---------- Utilidades compartidas ----------
	static CommandSourceStack fuente(MinecraftServer s) {
		return s.createCommandSourceStack().withSuppressedOutput();
	}

	static void aviso(MinecraftServer s, CommandSourceStack src, String texto, String color) {
		cmd(s, src, "title @p actionbar {text:\"" + texto + "\",color:\"" + color + "\"}");
	}

	/** Aviso más grande (subtítulo en el centro de la pantalla, unos 3 segundos). */
	static void avisoGrande(MinecraftServer s, CommandSourceStack src, String texto, String color) {
		cmd(s, src, "title @p times 10 60 20");
		cmd(s, src, "title @p subtitle {text:\"" + texto + "\",color:\"" + color + "\"}");
		cmd(s, src, "title @p title {text:\"\"}");
	}

	static void cmd(MinecraftServer s, CommandSourceStack src, String comando) {
		try {
			s.getCommands().performPrefixedCommand(src, comando);
		} catch (Exception e) {
			Registro.error("COMANDO", "Falló: " + comando, e);
		}
	}
}
