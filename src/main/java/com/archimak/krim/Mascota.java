package com.archimak.krim;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
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
 * Las listas se editan en config/krim.properties (animales_tierra, animales_agua).
 */
public final class Mascota {
	private static final Map<String, String> ALIAS = Map.of("perro", "wolf", "gato", "cat", "loro", "parrot");
	private static final Set<String> MANSOS = Set.of("horse", "donkey", "mule", "llama", "trader_llama", "camel");
	/** Los que se manejan con montura: salen ensillados para poder andar (sin montura te subís pero no se mueven). */
	private static final Set<String> CON_MONTURA = Set.of("horse", "donkey", "mule", "camel");
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
			Map.entry("glow_squid", "un calamar brillante"), Map.entry("dolphin", "un delfín"), Map.entry("tadpole", "un renacuajo"),
			Map.entry("pufferfish", "un pez globo"));

	// ---------- Entrada animada: el animal cae del cielo despacio con brillos dorados ----------
	private static final int ALTURA_MAX = 4;      // bloques por encima del suelo
	private static final int DURACION_MAX = 200;  // ticks (10 s) como máximo de estela
	private static final String BRILLO = "minecraft:dust{color:[1.0,0.82,0.25],scale:1.3}";
	private static final List<Cayendo> CAYENDO = new ArrayList<>();
	private static boolean tickRegistrado;

	private static final class Cayendo {
		final Entity e; int ticks;
		Cayendo(Entity e) { this.e = e; }
	}

	private static void registrarTick() {
		if (tickRegistrado) return;
		tickRegistrado = true;
		ServerTickEvents.END_SERVER_TICK.register(Mascota::tick);
	}

	private static void tick(MinecraftServer s) {
		if (CAYENDO.isEmpty()) return;
		CommandSourceStack src = Krim.fuente(s);
		for (int i = CAYENDO.size() - 1; i >= 0; i--) {
			Cayendo c = CAYENDO.get(i);
			c.ticks++;
			String en = "execute at " + c.e.getUUID() + " run ";
			if (c.e.isRemoved() || c.ticks > DURACION_MAX) { CAYENDO.remove(i); continue; }
			boolean llego = c.ticks > 5 && (c.e.onGround() || c.e.isInWater());
			if (llego) {
				Krim.cmd(s, src, en + "particle minecraft:totem_of_undying ~ ~0.5 ~ 0.5 0.4 0.5 0.25 35 force");
				Krim.cmd(s, src, en + "particle " + BRILLO + " ~ ~0.3 ~ 0.8 0.2 0.8 0 25 force");
				Krim.cmd(s, src, en + "playsound minecraft:entity.player.levelup neutral @a ~ ~ ~ 0.6 1.6");
				CAYENDO.remove(i);
			} else if (c.ticks % 2 == 0) {
				Krim.cmd(s, src, en + "particle " + BRILLO + " ~ ~0.5 ~ 0.35 0.35 0.35 0 5 force");
				if (c.ticks % 6 == 0) Krim.cmd(s, src, en + "particle minecraft:wax_on ~ ~0.5 ~ 0.3 0.3 0.3 0 2 force");
			}
		}
	}

	/** Lugar de aparición: 2 bloques delante del jugador si hay aire, y lo más alto posible (hasta 4) sin techo. */
	private static BlockPos lugarDeCaida(ServerLevel lvl, ServerPlayer p) {
		double rad = Math.toRadians(p.getYRot());
		BlockPos pies = p.blockPosition();
		BlockPos frente = BlockPos.containing(p.getX() - Math.sin(rad) * 2, p.getY(), p.getZ() + Math.cos(rad) * 2);
		BlockPos base = lvl.getBlockState(frente).isAir() && lvl.getBlockState(frente.above()).isAir() ? frente : pies;
		int h = 0;
		while (h < ALTURA_MAX && lvl.getBlockState(base.above(h + 2)).isAir()) h++;
		return base.above(h);
	}

	public static void crear(MinecraftServer s, ServerPlayer p, String tipoPedido, String nombre, String usuario) {
		registrarTick();
		ServerLevel lvl = (ServerLevel) p.level();
		BlockPos agua = buscarAgua(lvl, p.blockPosition());
		String pedido = ALIAS.getOrDefault(tipoPedido, tipoPedido);

		List<String> candidatos = new ArrayList<>();
		if (Config.animalesTierra.contains(pedido) || Config.animalesAgua.contains(pedido)) {
			candidatos.add(pedido);
		} else {
			if (!tipoPedido.equals("aleatorio")) {
				Registro.aviso("MASCOTA", "Tipo \"" + tipoPedido + "\" no disponible (no está en las listas o no hay agua cerca), se elige al azar");
			}
			// Todos con la misma chance, también los de agua: caen volando como los demás, haya agua o no
			List<String> pool = new ArrayList<>(Config.animalesTierra);
			pool.addAll(Config.animalesAgua);
			if (!pool.contains("pufferfish")) pool.add("pufferfish");
			java.util.Collections.shuffle(pool, ThreadLocalRandom.current());
			candidatos.addAll(pool.subList(0, Math.min(6, pool.size()))); // varios intentos por si alguno no se puede
		}

		CommandSourceStack src = Krim.fuente(s);
		for (String id : candidatos) {
			boolean deAgua = Config.animalesAgua.contains(id) || id.equals("pufferfish");
			BlockPos donde = (deAgua && agua != null) ? caidaAlAgua(lvl, agua) : lugarDeCaida(lvl, p);
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
				domarConDueno(e, p);
				extra = ", manso";
			}
			String quien = NOMBRES.getOrDefault(id, id);
			Registro.info("MASCOTA", id + " \"" + nombre + "\" de " + usuario + extra);
			Krim.cmd(s, src, "tellraw @a [{text:\"" + usuario + " te regaló " + quien + ": \",color:\"aqua\"},{text:\"" + nombre + "\",color:\"white\"}]");
			return;
		}
		Registro.error("MASCOTA", "No se pudo crear ningún animal para \"" + nombre + "\" de " + usuario, null);
	}

	private static Entity invocar(MinecraftServer s, CommandSourceStack src, ServerLevel lvl, String id, BlockPos pos,
			boolean deAgua, String nombre) {
		if (CON_MONTURA.contains(id)) return invocarMontable(s, src, lvl, id, pos, nombre);
		String marca = "krim_n" + System.nanoTime();
		String nbt = "{Tags:[\"" + Krim.TAG_ANIMAL + "\",\"" + marca + "\"],PersistenceRequired:1b,CustomNameVisible:1b," + Krim.SIN_BOTIN + ","
				+ (id.equals("chicken") ? "EggLayTime:2147483647," : "") // la gallina del chat no pone huevos
				+ "CustomName:{text:\"" + nombre + "\",color:\"aqua\"}" + (MANSOS.contains(id) ? ",Tame:1b" : "")
				+ (CON_MONTURA.contains(id) ? ",equipment:{saddle:{id:\"minecraft:saddle\",count:1}}" : "") + "}";
		String lugar = (pos.getX() + 0.5) + " " + pos.getY() + " " + (pos.getZ() + 0.5);
		Krim.cmd(s, src, "execute as @p at @s run summon minecraft:" + id + " " + lugar + " " + nbt);

		// Caída lenta (sin daño) mientras dura la entrada; los de agua además son inmunes mientras caen
		Krim.cmd(s, src, "effect give @e[tag=" + marca + "] minecraft:slow_falling 10 0 true");
		if (deAgua) Krim.cmd(s, src, "effect give @e[tag=" + marca + "] minecraft:resistance 12 4 true");
		Krim.cmd(s, src, "execute as @p at @s run playsound minecraft:block.amethyst_block.chime neutral @a "
				+ (pos.getX() + 0.5) + " " + pos.getY() + " " + (pos.getZ() + 0.5) + " 1 1.2");

		List<Entity> encontradas = lvl.getEntities((Entity) null, new AABB(pos).inflate(4), e -> Etiquetas.tiene(e, marca));
		if (!encontradas.isEmpty()) CAYENDO.add(new Cayendo(encontradas.get(0)));
		Krim.cmd(s, src, "tag @e[tag=" + marca + "] remove " + marca);
		return encontradas.isEmpty() ? null : encontradas.get(0);
	}

	/**
	 * Montables: se invocan SIN datos extra, igual que un caballo normal del juego (Minecraft les arma solo
	 * la raza, la velocidad y el salto), y recién después se les pone nombre, tag, montura y dueño.
	 */
	private static Entity invocarMontable(MinecraftServer s, CommandSourceStack src, ServerLevel lvl, String id, BlockPos pos, String nombre) {
		AABB zona = new AABB(pos).inflate(6);
		java.util.Set<java.util.UUID> antes = new java.util.HashSet<>();
		for (Entity e : lvl.getEntities((Entity) null, zona, e -> true)) antes.add(e.getUUID());
		String lugar = (pos.getX() + 0.5) + " " + pos.getY() + " " + (pos.getZ() + 0.5);
		Krim.cmd(s, src, "execute as @p at @s run summon minecraft:" + id + " " + lugar);
		List<Entity> nuevas = lvl.getEntities((Entity) null, zona,
				e -> !antes.contains(e.getUUID())
						&& ("minecraft:" + id).equals(String.valueOf(net.minecraft.world.entity.EntityType.getKey(e.getType()))));
		if (nuevas.isEmpty()) return null;
		Entity e = nuevas.get(0);
		String u = e.getUUID().toString();
		Krim.cmd(s, src, "tag " + u + " add " + Krim.TAG_ANIMAL);
		Krim.cmd(s, src, "data merge entity " + u + " {CustomName:{text:\"" + nombre + "\",color:\"aqua\"},CustomNameVisible:1b,"
				+ "PersistenceRequired:1b," + Krim.SIN_BOTIN + "}");
		Krim.cmd(s, src, "item replace entity " + u + " saddle with minecraft:saddle");
		Krim.cmd(s, src, "effect give " + u + " minecraft:slow_falling 10 0 true");
		Krim.cmd(s, src, "execute as @p at @s run playsound minecraft:block.amethyst_block.chime neutral @a " + lugar + " 1 1.2");
		CAYENDO.add(new Cayendo(e));
		return e;
	}

	/**
	 * Caballos y compañía no son "TamableAnimal": se doman con tameWithName(jugador), que además los deja con vos como dueño.
	 * Se busca por nombre para no depender del paquete de la clase (cambió en las versiones nuevas).
	 */
	private static void domarConDueno(Entity e, ServerPlayer p) {
		boolean dueno = llamar(e, "tameWithName", p);
		boolean domado = llamar(e, "setTamed", true);
		if (!dueno) llamar(e, "setOwner", p);
		// Diagnóstico: si el caballo no se deja manejar, esta línea dice qué falló
		Object esDomado = leer(e, "isTamed"), tieneMontura = leer(e, "isSaddled");
		Registro.info("MASCOTA", e.getClass().getSimpleName() + ": domado=" + esDomado + " montura=" + tieneMontura
				+ " (tameWithName " + (dueno ? "ok" : "no") + ", setTamed " + (domado ? "ok" : "no") + ")");
	}

	/** Llama a un método público de 1 parámetro por nombre (compatible con el argumento). */
	private static boolean llamar(Object o, String metodo, Object arg) {
		for (java.lang.reflect.Method mt : o.getClass().getMethods()) {
			if (!mt.getName().equals(metodo) || mt.getParameterCount() != 1) continue;
			Class<?> t = mt.getParameterTypes()[0];
			boolean encaja = t.isInstance(arg) || (t == boolean.class && arg instanceof Boolean);
			if (!encaja) continue;
			try { mt.invoke(o, arg); return true; } catch (Exception ex) { Registro.error("MASCOTA", "Falló " + metodo, ex); }
		}
		return false;
	}

	private static Object leer(Object o, String metodo) {
		try { return o.getClass().getMethod(metodo).invoke(o); } catch (Exception ex) { return "?"; }
	}

	/** Para los de agua: aparecen hasta 3 bloques arriba del agua (si hay aire) y caen adentro. */
	private static BlockPos caidaAlAgua(ServerLevel lvl, BlockPos agua) {
		BlockPos sup = agua;
		while (lvl.getBlockState(sup.above()).is(Blocks.WATER)) sup = sup.above(); // superficie
		int h = 0;
		while (h < 3 && lvl.getBlockState(sup.above(h + 1)).isAir()) h++;
		return sup.above(h);
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
