package com.archimak.kickcreeper;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Animal pasivo con el nombre que eligió el viewer.
 * - "aleatorio" (por defecto): uno al azar de la lista de tierra; si hay agua cerca, también puede salir uno de agua
 *   (y aparece adentro del agua, nunca en seco).
 * - perro / gato / loro o cualquier id de las listas (ej: "llama", "axolotl"): ese animal.
 * Los domesticables salen domesticados: perro, gato y loro con dueño; caballo, burro, mula, llama y camello mansos.
 * Las listas se editan en config/kickcreeper.properties (animales_tierra, animales_agua).
 */
public final class Mascota {
	private static final Map<String, String> ALIAS = Map.of("perro", "wolf", "gato", "cat", "loro", "parrot");
	private static final Set<String> MANSOS = Set.of("horse", "donkey", "mule", "llama", "trader_llama", "camel");
	private static final Map<String, String> NOMBRES = Map.ofEntries(
			Map.entry("wolf", "un perro"), Map.entry("cat", "un gato"), Map.entry("parrot", "un loro"),
			Map.entry("horse", "un caballo"), Map.entry("donkey", "un burro"), Map.entry("mule", "una mula"),
			Map.entry("llama", "una llama"), Map.entry("camel", "un camello"), Map.entry("fox", "un zorro"),
			Map.entry("ocelot", "un ocelote"), Map.entry("panda", "un panda"), Map.entry("pig", "un chancho"),
			Map.entry("cow", "una vaca"), Map.entry("mooshroom", "una champivaca"), Map.entry("sheep", "una oveja"),
			Map.entry("chicken", "una gallina"), Map.entry("rabbit", "un conejo"), Map.entry("frog", "una rana"),
			Map.entry("armadillo", "un armadillo"), Map.entry("sniffer", "un sniffer"), Map.entry("turtle", "una tortuga"),
			Map.entry("allay", "un allay"), Map.entry("cod", "un bacalao"), Map.entry("salmon", "un salmón"),
			Map.entry("tropical_fish", "un pez tropical"), Map.entry("axolotl", "un ajolote"), Map.entry("squid", "un calamar"),
			Map.entry("glow_squid", "un calamar brillante"), Map.entry("dolphin", "un delfín"), Map.entry("tadpole", "un renacuajo"));

	public static void crear(MinecraftServer s, ServerPlayer p, String tipoPedido, String nombre, String usuario) {
		ServerLevel lvl = (ServerLevel) p.level();
		BlockPos agua = buscarAgua(lvl, p.blockPosition());
		String pedido = ALIAS.getOrDefault(tipoPedido, tipoPedido);

		List<String> candidatos = new ArrayList<>();
		if (Config.animalesTierra.contains(pedido) || (Config.animalesAgua.contains(pedido) && agua != null)) {
			candidatos.add(pedido);
		} else {
			if (!tipoPedido.equals("aleatorio")) {
				Registro.aviso("MASCOTA", "Tipo \"" + tipoPedido + "\" no disponible (no está en las listas o no hay agua cerca), se elige al azar");
			}
			List<String> pool = new ArrayList<>(Config.animalesTierra);
			if (agua != null) pool.addAll(Config.animalesAgua);
			java.util.Collections.shuffle(pool, ThreadLocalRandom.current());
			candidatos.addAll(pool.subList(0, Math.min(4, pool.size()))); // hasta 4 intentos si algún id falla
		}

		CommandSourceStack src = KickCreeper.fuente(s);
		for (String id : candidatos) {
			boolean deAgua = Config.animalesAgua.contains(id);
			BlockPos donde = deAgua ? agua : p.blockPosition();
			Entity e = invocar(s, src, lvl, id, donde, deAgua, nombre);
			if (e == null) {
				Registro.aviso("MASCOTA", "No se pudo crear \"" + id + "\" (¿id inválido en esta versión?), pruebo otro");
				continue;
			}
			String extra = "";
			if (e instanceof TamableAnimal animal) {
				animal.tame(p);
				extra = ", domesticado";
			} else if (MANSOS.contains(id)) {
				extra = ", manso";
			}
			String quien = NOMBRES.getOrDefault(id, id);
			Registro.info("MASCOTA", id + " \"" + nombre + "\" de " + usuario + extra);
			KickCreeper.aviso(s, src, usuario + " te regaló " + quien + ": " + nombre, "aqua");
			return;
		}
		Registro.error("MASCOTA", "No se pudo crear ningún animal para \"" + nombre + "\" de " + usuario, null);
	}

	private static Entity invocar(MinecraftServer s, CommandSourceStack src, ServerLevel lvl, String id, BlockPos pos,
			boolean deAgua, String nombre) {
		String marca = "krim_n" + System.nanoTime();
		String nbt = "{Tags:[\"krim_mascota\",\"" + marca + "\"],PersistenceRequired:1b,CustomNameVisible:1b,"
				+ "CustomName:{text:\"" + nombre + "\",color:\"aqua\"}" + (MANSOS.contains(id) ? ",Tame:1b" : "") + "}";
		String lugar = deAgua ? (pos.getX() + 0.5) + " " + pos.getY() + " " + (pos.getZ() + 0.5) : "~ ~ ~";
		KickCreeper.cmd(s, src, "execute as @p at @s run summon minecraft:" + id + " " + lugar + " " + nbt);

		List<Entity> encontradas = lvl.getEntities((Entity) null, new AABB(pos).inflate(4), e -> Etiquetas.tiene(e, marca));
		KickCreeper.cmd(s, src, "tag @e[tag=" + marca + "] remove " + marca);
		return encontradas.isEmpty() ? null : encontradas.get(0);
	}

	/** Bloque de agua a 4 bloques o menos del jugador (incluido el que pisa), o null. */
	private static BlockPos buscarAgua(ServerLevel lvl, BlockPos centro) {
		for (int r = 0; r <= 4; r++)
			for (int dx = -r; dx <= r; dx++)
				for (int dy = -2; dy <= 1; dy++)
					for (int dz = -r; dz <= r; dz++) {
						if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
						BlockPos pos = centro.offset(dx, dy, dz);
						if (lvl.getBlockState(pos).is(Blocks.WATER)) return pos;
					}
		return null;
	}
}
