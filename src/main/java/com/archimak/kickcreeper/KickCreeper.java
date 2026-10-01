package com.archimak.kickcreeper;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

public class KickCreeper implements ModInitializer {
	public static final String MOD_ID = "kickcreeper";
	/** Tag que marca a los creepers del chat. El mixin solo actúa sobre estos. */
	public static final String TAG = "kick_creeper";
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
		Registro.info("INICIO", "Kick Creeper " + VERSION + " | Minecraft " + VERSION_MC
				+ " | Java " + System.getProperty("java.version") + " | puerto " + Config.puerto
				+ " | palabras bloqueadas: " + Config.bloqueadas.size());
		ServerLifecycleEvents.SERVER_STARTED.register(s -> {
			servidor = s;
			Registro.info("MUNDO", "Mundo abierto, listo para recibir canjes");
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(s -> {
			servidor = null;
			Registro.info("MUNDO", "Mundo cerrado");
		});
		ServidorHttp.iniciar(Config.puerto);
	}

	/** Llamado desde el hilo HTTP. Devuelve false si no hay un mundo abierto. */
	public static boolean spawnear(String nombre, String usuario) {
		MinecraftServer s = servidor;
		if (s == null) {
			Registro.aviso("CANJE", "Rechazado (no hay mundo abierto): nombre=\"" + nombre + "\" usuario=\"" + usuario + "\"");
			return false;
		}
		int n = PEDIDOS.incrementAndGet();
		Registro.info("CANJE", "#" + n + " recibido: nombre=\"" + nombre + "\" usuario=\"" + usuario + "\"");
		// Los comandos se ejecutan en el hilo del juego, no importa qué menú tengas abierto
		s.execute(() -> ejecutar(s, nombre, usuario));
		return true;
	}

	private static void ejecutar(MinecraftServer s, String nombre, String usuario) {
		CommandSourceStack src = s.createCommandSourceStack().withSuppressedOutput();
		String nbt = "{Tags:[\"" + TAG + "\",\"kc_nuevo\"],CustomNameVisible:1b,"
				+ "CustomName:{text:\"" + nombre + "\",color:\"green\"}}";

		// 1) Detrás del jugador si hay lugar libre
		cmd(s, src, "execute as @p at @s positioned ^ ^ ^-3 if block ~ ~ ~ #minecraft:air if block ~ ~1 ~ #minecraft:air run summon minecraft:creeper ~ ~ ~ " + nbt);
		// 2) Si no había lugar (pared, cueva chica), al lado del jugador
		cmd(s, src, "execute unless entity @e[tag=kc_nuevo] as @p at @s run summon minecraft:creeper ~ ~ ~ " + nbt);
		cmd(s, src, "tag @e[tag=kc_nuevo] remove kc_nuevo");
		Registro.info("SPAWN", "Comandos de spawn ejecutados para \"" + nombre + "\"");
		cmd(s, src, "title @p actionbar {text:\"" + usuario + " te mandó un creeper\",color:\"green\"}");
	}

	private static void cmd(MinecraftServer s, CommandSourceStack src, String comando) {
		try {
			s.getCommands().performPrefixedCommand(src, comando);
		} catch (Exception e) {
			Registro.error("COMANDO", "Falló: " + comando, e);
		}
	}
}
