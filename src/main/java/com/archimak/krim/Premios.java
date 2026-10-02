package com.archimak.krim;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Catálogo de la Ruleta del destino (2.3).
 *
 * Reparto (de 1000): mobs 220 · castigos 190 · efectos malos 70 · papeles/libros/mapas con puteadas 160 ·
 * mensajes 40 · frases y poesías 35 · sustos 50 · locuras neutras 65 · ítems 22 · guisito 12 · ramo 11 · galleta 10 ·
 * efectos buenos 25 · animal 20 · curación 10 · doble 22 · jackpot 5 · nada 33.  Casi dos tercios es castigo o burla.
 *
 * Reglas de siempre: lo hostil solo daña al jugador, nada rompe bloques ni prende fuego, nada suelta ítems
 * ni experiencia, nunca warden/wither/dragón, y con menos de 4 corazones hay tregua (sin mobs ni castigos).
 * Anti-repetición: no se repite ninguno de los últimos 24 premios ni sale 3 veces seguidas la misma categoría.
 */
public final class Premios {
	public enum Clase { BUENO, MALO, NEUTRO, HOSTIL }

	@FunctionalInterface
	public interface Accion { void hacer(MinecraftServer s, ServerPlayer p, String u); }

	public record Premio(String titulo, String detalle, String color, Clase clase, Accion accion, String clave, String categoria) {
		/** Texto corto para el sector de la rueda. */
		public String etiqueta() { return corta(titulo); }
	}

	private static final String SIN = "DeathLootTable:\"minecraft:empty\",CanPickUpLoot:0b";
	private static final String TAGR = "Tags:[\"" + Krim.TAG_RULETA + "\"]";
	private static final Accion NADA = (s, p, us) -> { };

	private static final ArrayDeque<String> RECIENTES = new ArrayDeque<>();
	private static final ArrayDeque<String> CATEGORIAS = new ArrayDeque<>();

	private static ThreadLocalRandom r() { return ThreadLocalRandom.current(); }
	private static int entre(int a, int b) { return a + r().nextInt(b - a + 1); }
	private static <T> T uno(List<T> l) { return l.get(r().nextInt(l.size())); }
	private static void cmd(MinecraftServer s, String c) { Krim.cmd(s, Krim.fuente(s), c); }
	private static String may(String t) { return t.toUpperCase(Locale.ROOT); }
	private static String romano(int n) { return switch (n) { case 2 -> " II"; case 3 -> " III"; case 4 -> " IV"; case 5 -> " V"; default -> ""; }; }
	private static String esc(String t) { return t.replace("\\", "").replace("\"", "'"); }

	static String corta(String t) {
		String s = t.replace("¡", "").replace("!", "").replace("»", "").replace("«", "").trim();
		return s.length() > 13 ? s.substring(0, 12).trim() + "." : s;
	}

	private static Premio pr(String cat, String clave, String titulo, String detalle, String color, Clase clase, Accion a) {
		return new Premio(titulo, detalle, color, clase, a, cat + ":" + clave, cat);
	}

	static int argb(String color) {
		return switch (color) {
			case "red" -> 0xFFFF5555; case "gold" -> 0xFFFFAA00; case "yellow" -> 0xFFFFFF55; case "green" -> 0xFF55FF55;
			case "aqua" -> 0xFF55FFFF; case "light_purple" -> 0xFFFF55FF; case "dark_purple" -> 0xFFAA00AA;
			case "gray" -> 0xFFAAAAAA; case "dark_gray" -> 0xFF777777; case "blue" -> 0xFF5555FF; case "dark_red" -> 0xFFAA0000;
			default -> 0xFFFFFFFF;
		};
	}

	// =====================================================================
	// Sorteo con anti-repetición
	// =====================================================================
	public static Premio sortear(MinecraftServer s, ServerPlayer p, String us, boolean tregua, boolean hostilesOk) {
		for (int i = 0; i < 60; i++) {
			Premio x = generar(s, p, us, tregua, hostilesOk, true);
			if (x == null || !permitido(x, tregua, hostilesOk)) continue;
			if (i < 50 && RECIENTES.contains(x.clave())) continue;
			if (i < 50 && CATEGORIAS.size() >= 2 && CATEGORIAS.stream().allMatch(c -> c.equals(x.categoria()))) continue;
			recordar(x);
			return x;
		}
		Premio m = mensaje(us, p.getName().getString());
		recordar(m);
		return m;
	}

	private static void recordar(Premio x) {
		RECIENTES.addLast(x.clave());
		while (RECIENTES.size() > 24) RECIENTES.removeFirst();
		CATEGORIAS.addLast(x.categoria());
		while (CATEGORIAS.size() > 2) CATEGORIAS.removeFirst();
	}

	private static boolean permitido(Premio x, boolean tregua, boolean hostilesOk) {
		if (x.clase() == Clase.HOSTIL && (tregua || !hostilesOk)) return false;
		return !(x.clase() == Clase.MALO && tregua);
	}

	private static Premio generar(MinecraftServer s, ServerPlayer p, String us, boolean tregua, boolean hostilesOk, boolean permitirDoble) {
		String yo = p.getName().getString();
		int x = r().nextInt(1000);
		if (x < 200) return mob(p, us);
		if (x < 375) return castigo(p, us, yo);
		if (x < 410) return hablador(us, yo);
		if (x < 450) return pr("trivia", "pregunta" + r().nextInt(4), "¡PREGUNTA!", "a ver qué tan culto sos", "aqua", Clase.NEUTRO,
				(sv, pl, u) -> Trivia.preguntar(sv, us));
		if (x < 480) return efecto(false);
		if (x < 640) return burla(us, yo);
		if (x < 670) return mensaje(us, yo);
		if (x < 705) return frase(us);
		if (x < 745) return susto();
		if (x < 830) return locura(p, us, yo);
		if (x < 852) return item(us);
		if (x < 864) return guisito(us);
		if (x < 875) return ramo(us);
		if (x < 885) return galleta(us);
		if (x < 910) return efecto(true);
		if (x < 930) return pr("animal", "random", "ANIMAL RANDOM", "regalo de " + us, "aqua", Clase.BUENO, (sv, pl, u) -> Mascota.crear(sv, pl, "aleatorio", us, us));
		if (x < 940) return pr("bueno", "curacion", "CURACIÓN TOTAL", us + " te tuvo piedad", "green", Clase.BUENO, (sv, pl, u) -> {
			cmd(sv, "effect give @p minecraft:instant_health 1 3 true");
			cmd(sv, "execute at @p run particle minecraft:heart ~ ~1.5 ~ 0.6 0.6 0.6 0 12 force");
		});
		if (x < 962) return permitirDoble ? doble(s, p, us, tregua, hostilesOk) : null;
		if (x < 967) return jackpot(us);
		return pr("nada", "nada", "NADA", uno(List.of("gracias por participar, " + us, "gracias por participar", "seguí participando",
				"gracias por participar (y por los puntos)", "la próxima será, " + us)), "gray", Clase.NEUTRO,
				(sv, pl, u) -> cmd(sv, "execute at @p run playsound minecraft:block.note_block.didgeridoo master @p ~ ~ ~ 1 0.6"));
	}

	// =====================================================================
	// MOBS (solos, hordas y combos)
	// =====================================================================
	private enum Lugar { SUELO, VUELA, AGUA, GRANDE, LEJOS }

	private record Mob(String id, String nombre, int peso, int min, int max, Lugar lugar, String extra) {}

	private static final String ZOMBIE = ",CanBreakDoors:0b";
	private static final String INMUNE = ",IsImmuneToZombification:1b";

	private static final List<Mob> MOBS = List.of(
			new Mob("zombie", "Zombie", 10, 1, 3, Lugar.SUELO, ZOMBIE),
			new Mob("zombie", "Zombie bebé", 6, 1, 3, Lugar.SUELO, ZOMBIE + ",IsBaby:1b"),
			new Mob("husk", "Momia", 6, 1, 2, Lugar.SUELO, ZOMBIE),
			new Mob("drowned", "Ahogado", 5, 1, 2, Lugar.SUELO, ZOMBIE),
			new Mob("zombie_villager", "Aldeano zombie", 4, 1, 2, Lugar.SUELO, ZOMBIE),
			new Mob("skeleton", "Esqueleto", 9, 1, 3, Lugar.LEJOS, ""),
			new Mob("stray", "Esqueleto glacial", 4, 1, 2, Lugar.LEJOS, ""),
			new Mob("bogged", "Esqueleto pantanoso", 4, 1, 2, Lugar.LEJOS, ""),
			new Mob("spider", "Araña", 8, 1, 3, Lugar.SUELO, ""),
			new Mob("cave_spider", "Araña de cueva", 5, 2, 4, Lugar.SUELO, ""),
			new Mob("creeper", "Creeper", 7, 1, 2, Lugar.SUELO, ""),
			new Mob("witch", "Bruja", 4, 1, 1, Lugar.SUELO, ""),
			new Mob("pillager", "Saqueador", 5, 1, 3, Lugar.LEJOS, ""),
			new Mob("vindicator", "Vindicador", 3, 1, 1, Lugar.SUELO, ""),
			new Mob("evoker", "Evocador", 2, 1, 1, Lugar.SUELO, ""),
			new Mob("illusioner", "Ilusionista", 1, 1, 1, Lugar.LEJOS, ""),
			new Mob("phantom", "Phantom", 5, 2, 3, Lugar.VUELA, ""),
			new Mob("slime", "Slime", 5, 2, 4, Lugar.SUELO, ",Size:0"),
			new Mob("magma_cube", "Cubo de magma", 4, 2, 4, Lugar.SUELO, ",Size:0"),
			new Mob("endermite", "Endermite", 3, 2, 4, Lugar.SUELO, ""),
			new Mob("piglin", "Piglin", 3, 1, 2, Lugar.SUELO, INMUNE),
			new Mob("piglin_brute", "Piglin bruto", 2, 1, 1, Lugar.SUELO, INMUNE),
			new Mob("hoglin", "Hoglin", 2, 1, 1, Lugar.SUELO, INMUNE),
			new Mob("zoglin", "Zoglin", 1, 1, 1, Lugar.SUELO, ""),
			new Mob("wither_skeleton", "Esqueleto wither", 2, 1, 1, Lugar.SUELO, ""),
			new Mob("guardian", "Guardián", 2, 1, 2, Lugar.AGUA, ""),
			new Mob("shulker", "Shulker", 1, 1, 1, Lugar.SUELO, ""),
			new Mob("ghast", "Ghast", 2, 1, 1, Lugar.GRANDE, ""),
			new Mob("breeze", "Breeze", 3, 1, 1, Lugar.SUELO, ""),
			new Mob("creaking", "Creaking", 2, 1, 1, Lugar.SUELO, ""),
			new Mob("vex", "Vex", 3, 2, 4, Lugar.VUELA, ""),
			new Mob("spider", "Jinete araña", 3, 1, 1, Lugar.SUELO, ",Passengers:[{id:\"minecraft:skeleton\"," + TAGR + "," + SIN + "}]"),
			new Mob("chicken", "Pollito jockey", 3, 1, 2, Lugar.SUELO, ",EggLayTime:2147483647,Passengers:[{id:\"minecraft:zombie\",IsBaby:1b,CanBreakDoors:0b," + TAGR + "," + SIN + "}]"),
			new Mob("skeleton_horse", "Jinete esqueleto", 1, 1, 1, Lugar.SUELO, ",Passengers:[{id:\"minecraft:skeleton\"," + TAGR + "," + SIN + "}]"));

	private static Mob elegirMob() {
		int total = 0;
		for (Mob m : MOBS) total += m.peso();
		int x = r().nextInt(total);
		for (Mob m : MOBS) { x -= m.peso(); if (x < 0) return m; }
		return MOBS.get(0);
	}

	private static Premio mob(ServerPlayer p, String us) {
		ServerLevel lvl = (ServerLevel) p.level();
		int t = r().nextInt(100);
		if (t < 14) return horda(us);
		if (t < 24) return emboscada(us);
		if (t < 30) return pr("mob", "creepers", "RODEADO DE CREEPERS", "3 creepers de " + us, "red", Clase.HOSTIL, (sv, pl, u) -> {
			for (int i = 0; i < 3; i++) invocar(sv, lugar((ServerLevel) pl.level(), pl, Lugar.SUELO), "creeper", "", us, "red");
		});
		Mob m = elegirMob();
		if (m.lugar() == Lugar.AGUA && buscarAgua(lvl, p.blockPosition()) == null) return null;
		if (m.lugar() == Lugar.GRANDE && lugarGrande(lvl, p) == null) return null;
		int cant = entre(m.min(), m.max());
		String titulo = may(m.nombre()) + (cant > 1 ? " x" + cant : "");
		return pr("mob", m.nombre(), titulo, "mandado por " + us, "red", Clase.HOSTIL, (sv, pl, u) -> {
			for (int i = 0; i < cant; i++) invocar(sv, lugar((ServerLevel) pl.level(), pl, m.lugar()), m.id(), m.extra(), us, "red");
		});
	}

	private static Premio horda(String us) {
		int cant = entre(4, 6);
		return pr("mob", "horda", "¡HORDA x" + cant + "!", "zombies de " + us, "dark_red", Clase.HOSTIL, (sv, pl, u) -> {
			for (int i = 0; i < cant; i++) {
				String id = uno(List.of("zombie", "zombie", "husk", "drowned", "zombie_villager"));
				String extra = ZOMBIE + (r().nextInt(4) == 0 ? ",IsBaby:1b" : "");
				invocar(sv, lugar((ServerLevel) pl.level(), pl, Lugar.SUELO), id, extra, us, "red");
			}
		});
	}

	private static Premio emboscada(String us) {
		return pr("mob", "emboscada", "¡EMBOSCADA!", "3 bichos al azar de " + us, "dark_red", Clase.HOSTIL, (sv, pl, u) -> {
			for (int i = 0; i < 3; i++) {
				Mob m;
				do { m = elegirMob(); } while (m.lugar() == Lugar.AGUA || m.lugar() == Lugar.GRANDE);
				invocar(sv, lugar((ServerLevel) pl.level(), pl, m.lugar()), m.id(), m.extra(), us, "red");
			}
		});
	}

	static void invocar(MinecraftServer s, BlockPos pos, String id, String extra, String nombre, String color) {
		String marca = "krim_r" + System.nanoTime();
		String nbt = "{Tags:[\"" + Krim.TAG_RULETA + "\",\"" + marca + "\"]," + SIN + ",CustomNameVisible:1b,"
				+ "CustomName:{text:\"" + nombre + "\",color:\"" + color + "\"}" + extra + "}";
		cmd(s, "execute as @p at @s run summon minecraft:" + id + " " + (pos.getX() + 0.5) + " " + pos.getY() + " " + (pos.getZ() + 0.5) + " " + nbt);
		String marcar = "data merge entity @s {" + TAGR + ",DeathLootTable:\"minecraft:empty\"}";
		cmd(s, "execute as @e[tag=" + marca + "] on passengers run " + marcar);
		cmd(s, "execute as @e[tag=" + marca + "] on vehicle run " + marcar);
		cmd(s, "execute at @e[tag=" + marca + "] run particle minecraft:large_smoke ~ ~0.5 ~ 0.4 0.6 0.4 0.02 20 force");
		cmd(s, "tag @e[tag=" + marca + "] remove " + marca);
	}

	// =====================================================================
	// CASTIGOS (lo que más sale)
	// =====================================================================
	private static Premio castigo(ServerPlayer p, String us, String yo) {
		ServerLevel lvl = (ServerLevel) p.level();
		return switch (r().nextInt(22)) {
			case 0, 1 -> {
				int sube = alturaLibre(lvl, p.blockPosition(), 30);
				if (sube < 14) yield null;
				yield pr("castigo", "volar", "¡A VOLAR!", sube + " bloques para arriba, cortesía de " + us, "red", Clase.MALO, (sv, pl, u) -> {
					int h = alturaLibre((ServerLevel) pl.level(), pl.blockPosition(), 30);
					cmd(sv, "execute at @p run particle minecraft:cloud ~ ~0.5 ~ 0.4 0.2 0.4 0.05 30 force");
					cmd(sv, "execute as @p at @s run tp @s ~ ~" + h + " ~");
					cmd(sv, "execute at @p run playsound minecraft:entity.breeze.jump master @p ~ ~ ~ 1 0.8");
					paracaidas();
				});
			}
			case 2 -> pr("castigo", "congelado", "¡CONGELADO!", "12 segundos sin moverte", "aqua", Clase.MALO, (sv, pl, u) -> {
				cmd(sv, "effect give @p minecraft:slowness 12 6 true");
				cmd(sv, "effect give @p minecraft:mining_fatigue 12 4 true");
				Atributos.aplicar(sv, "minecraft:jump_strength", "sin_patas", -1, "add_multiplied_total", 12);
				cmd(sv, "execute at @p run particle minecraft:snowflake ~ ~1 ~ 0.5 1 0.5 0.02 80 force");
				cmd(sv, "execute at @p run playsound minecraft:block.glass.break master @p ~ ~ ~ 1 0.8");
			});
			case 3 -> pr("castigo", "pina", "¡PIÑA DEL CHAT!", us + " te pegó (2 corazones)", "red", Clase.MALO, (sv, pl, u) -> {
				cmd(sv, "damage @p 4 minecraft:generic");
				cmd(sv, "execute at @p run particle minecraft:crit ~ ~1.2 ~ 0.3 0.3 0.3 0.3 20 force");
			});
			case 4 -> pr("castigo", "flechas", "¡LLUVIA DE FLECHAS!", "cubrite, " + yo, "red", Clase.MALO, (sv, pl, u) -> {
				for (int i = 0; i < 12; i++) Ruleta.despues(i * 3, s2 -> cmd(s2, "execute at @p run summon minecraft:arrow ~" + (r().nextDouble() * 8 - 4)
						+ " ~14 ~" + (r().nextDouble() * 8 - 4) + " {Motion:[0.0,-1.6,0.0],pickup:0b,damage:0.75d,crit:1b," + TAGR + "}"));
				cmd(sv, "execute at @p run playsound minecraft:entity.arrow.shoot master @p ~ ~ ~ 1 0.6");
			});
			case 5 -> pr("castigo", "sinpatas", "SIN PATAS", "30 segundos sin poder saltar", "red", Clase.MALO,
					(sv, pl, u) -> Atributos.aplicar(sv, "minecraft:jump_strength", "sin_patas", -1, "add_multiplied_total", 30));
			case 6 -> pr("castigo", "pesado", "GRAVEDAD x3", "30 segundos pesado como un yunque", "red", Clase.MALO,
					(sv, pl, u) -> Atributos.aplicar(sv, "minecraft:gravity", "gravedad", 2, "add_multiplied_total", 30));
			case 7 -> pr("castigo", "trex", "BRAZOS DE T-REX", "30 segundos de alcance mínimo", "red", Clase.MALO, (sv, pl, u) -> {
				Atributos.aplicar(sv, "minecraft:block_interaction_range", "brazos", -2.5, "add_value", 30);
				Atributos.aplicar(sv, "minecraft:entity_interaction_range", "brazos", -1.5, "add_value", 30);
			});
			case 8 -> pr("castigo", "borrachera", "BORRACHERA", us + " te invitó un fernet", "dark_purple", Clase.MALO, (sv, pl, u) -> {
				cmd(sv, "effect give @p minecraft:nausea 25 0 true");
				cmd(sv, "effect give @p minecraft:slowness 20 0 true");
				for (int i = 1; i <= 10; i++) Ruleta.despues(i * 25, s2 -> cmd(s2, "execute as @p at @s run tp @s ~ ~ ~ ~" + entre(-70, 70) + " ~" + entre(-20, 20)));
				cmd(sv, "execute at @p run playsound minecraft:entity.player.burp master @p ~ ~ ~ 1 0.7");
			});
			case 9 -> pr("castigo", "panico", "ATAQUE DE PÁNICO", "algo se acerca...", "dark_gray", Clase.MALO, (sv, pl, u) -> {
				cmd(sv, "effect give @p minecraft:darkness 20 0 true");
				for (int i = 0; i < 16; i++) Ruleta.despues(i * 22, s2 -> cmd(s2, "execute at @p run playsound minecraft:entity.warden.heartbeat master @p ~ ~ ~ 1 1"));
			});
			case 10 -> pr("castigo", "hambre", "¡HAMBRE VORAZ!", "la panza te ruge", "red", Clase.MALO, (sv, pl, u) -> {
				cmd(sv, "effect give @p minecraft:hunger 12 40 true");
				cmd(sv, "execute at @p run playsound minecraft:entity.player.burp master @p ~ ~ ~ 1 0.5");
			});
			case 11 -> pr("castigo", "revuelto", "INVENTARIO REVUELTO", us + " te desordenó todo", "red", Clase.MALO, (sv, pl, u) -> {
				var inv = pl.getInventory();
				List<ItemStack> l = new ArrayList<>();
				for (int i = 0; i < 36; i++) l.add(inv.getItem(i));
				Collections.shuffle(l);
				for (int i = 0; i < 36; i++) inv.setItem(i, l.get(i));
				cmd(sv, "execute at @p run playsound minecraft:item.bundle.drop_contents master @p ~ ~ ~ 1 1");
			});
			case 12 -> pr("castigo", "cielo", "¡MIRÁ AL CIELO!", "", "yellow", Clase.MALO, (sv, pl, u) -> cmd(sv, "execute as @p at @s run tp @s ~ ~ ~ ~ -90"));
			case 13 -> pr("castigo", "vuelta", "¡MIRÁ PARA ATRÁS!", "180° cortesía de " + us, "yellow", Clase.MALO, (sv, pl, u) -> {
				cmd(sv, "execute as @p at @s run tp @s ~ ~ ~ ~180 ~");
				cmd(sv, "execute at @p run playsound minecraft:entity.player.attack.sweep master @p ~ ~ ~ 1 1");
			});
			case 14 -> pr("castigo", "borde", "AL BORDE DE LA MUERTE", "te quedó medio corazón", "dark_red", Clase.MALO, (sv, pl, u) -> {
				float vida = pl.getHealth();
				if (vida > 3) cmd(sv, "damage @p " + Math.max(1, Math.round(vida - 2)) + " minecraft:generic");
				Ruleta.despues(120, s2 -> cmd(s2, "effect give @p minecraft:regeneration 6 1 true"));
			});
			case 15 -> pr("castigo", "tnt", "¡TNT DE VERDAD!", "esta sí explota... corré", "dark_red", Clase.HOSTIL, (sv, pl, u) -> {
				BlockPos pos = Premios.suelo((ServerLevel) pl.level(), pl, 2, 3);
				String donde = pos == null ? "~ ~ ~" : (pos.getX() + 0.5) + " " + pos.getY() + " " + (pos.getZ() + 0.5);
				cmd(sv, "execute as @p at @s run summon minecraft:tnt " + donde + " {fuse:70,explosion_power:2.0f," + TAGR + "}");
			});
			case 16 -> pr("castigo", "francotiradores", "FRANCOTIRADORES", "3 esqueletos a lo lejos", "red", Clase.HOSTIL, (sv, pl, u) -> {
				for (int i = 0; i < 3; i++) invocar(sv, lugar((ServerLevel) pl.level(), pl, Lugar.LEJOS), "skeleton", "", us, "red");
			});
			case 17 -> pr("castigo", "murcielagos", "¡MURCIÉLAGOS!", "30 segundos de caos", "dark_gray", Clase.MALO, (sv, pl, u) -> lluvia(sv, "bat", "", 12, 600, false));
			case 18 -> pr("castigo", "apagon", "APAGÓN", "no ves nada", "dark_gray", Clase.MALO, (sv, pl, u) -> {
				cmd(sv, "effect give @p minecraft:blindness 14 0 true");
				Ruleta.despues(30, s2 -> cmd(s2, "execute as @p at @s positioned ^ ^ ^-2 run playsound minecraft:entity.zombie.ambient master @p ~ ~ ~ 1 0.8"));
			});
			case 19 -> pr("castigo", "lento", "MODO TORTUGA", "20 segundos", "red", Clase.MALO, (sv, pl, u) -> cmd(sv, "effect give @p minecraft:slowness 20 3 false"));
			case 20 -> pr("castigo", "vex", "ENJAMBRE DE VEX", "4 vex furiosos de " + us, "red", Clase.HOSTIL, (sv, pl, u) -> {
				for (int i = 0; i < 4; i++) invocar(sv, lugar((ServerLevel) pl.level(), pl, Lugar.VUELA), "vex", "", us, "red");
			});
			default -> pr("castigo", "phantoms", "PHANTOMS DE DÍA", "3 phantoms de " + us, "red", Clase.HOSTIL, (sv, pl, u) -> {
				for (int i = 0; i < 3; i++) invocar(sv, lugar((ServerLevel) pl.level(), pl, Lugar.VUELA), "phantom", "", us, "red");
			});
		};
	}

	/** Paracaídas de último segundo: cuando el jugador está a 5 bloques del piso, caída lenta. */
	private static void paracaidas() {
		Ruleta.repetir(400, sv -> {
			ServerPlayer p = Ruleta.jugador(sv);
			if (p == null || p.isInWater()) return true;
			if (p.onGround()) return true;
			ServerLevel l = (ServerLevel) p.level();
			BlockPos b = p.blockPosition();
			for (int k = 1; k <= 6; k++) {
				if (!l.getBlockState(b.below(k)).isAir()) {
					cmd(sv, "effect give @p minecraft:slow_falling 4 0 true");
					cmd(sv, "execute at @p run particle minecraft:end_rod ~ ~2 ~ 0.4 0.1 0.4 0.02 20 force");
					cmd(sv, "title @p actionbar {text:\"¡Paracaídas de último segundo!\",color:\"gold\"}");
					return true;
				}
			}
			return false;
		});
	}

	private static int alturaLibre(ServerLevel l, BlockPos pies, int max) {
		int h = 0;
		while (h < max && l.getBlockState(pies.above(h + 2)).isAir()) h++;
		return h;
	}

	// =====================================================================
	// BURLAS: papeles, libros y mapas con puteadas y dibujos
	// =====================================================================
	private static final List<String> INSULTOS = List.of("manco", "boludo", "pelotudo", "gil", "nabo", "tarado", "bobo",
			"salame", "zapallo", "queso", "inútil", "patético", "cagón", "noob", "perdedor", "burro", "papanatas", "chanta",
			"pecho frío", "boludazo", "otario", "trompa", "lento", "fracasado", "tontito", "pavo", "gilazo", "longi");
	private static final List<String> ASCII = List.of("8====D", "8=====D~~", "B===D", "8======D", "8==D", "(_)_)::::::D",
			"8=D", "8========D", "c===3", "8===D ~ ~ ~");
	private static final List<String> FIRMAS = List.of("Con cariño", "Atte", "Besos", "Tu fan", "Sin rencor", "Con odio",
			"Saludos cordiales", "Te quiere (mentira)", "Firmado");

	private static Premio burla(String us, String yo) {
		int x = r().nextInt(100);
		if (x < 42) return pr("burla", "mapa" + r().nextInt(3), "MAPA DIBUJADO", "una obra de arte de " + us, "light_purple", Clase.NEUTRO, (sv, pl, u) -> {
			String tipo = Dibujos.dar((ServerLevel) pl.level(), pl, us);
			cmd(sv, "execute at @p run playsound minecraft:ui.cartography_table.take_result master @p ~ ~ ~ 1 1");
			Registro.info("RULETA", "Mapa dibujado: " + tipo + " de " + us);
		});
		if (x < 72) return papel(us, yo);
		return libro(us, yo);
	}

	private static Premio papel(String us, String yo) {
		String ins = uno(INSULTOS);
		String a = uno(ASCII);
		String nombre = esc(uno(List.of("Nota de %u", "Mensaje secreto de %u", "Carta de %u", "Papelito de %u",
				"Recibo de %u", "Certificado oficial", "Diploma de " + ins, "Multa de %u")).replace("%u", us));
		List<String> lineas = List.of(
				uno(List.of("Sos un " + ins + ".", "Querido " + ins + ":", "Para el más " + ins + " del server.", "Puto el que lee.",
						"Certifico que " + yo + " es " + ins + ".", "Multa por ser " + ins + ".", "Diploma a la persona más " + ins + ".")),
				a,
				uno(List.of("Puto el que lee.", "Chupala.", "Tomatela.", "Andá a cagar.", "Saludos a tu vieja.", "No lo tires.",
						"Guardalo para siempre.", "Leelo de nuevo.", "PD: " + a)),
				"- " + uno(FIRMAS) + ", " + us);
		StringBuilder lore = new StringBuilder("[");
		for (int i = 0; i < lineas.size(); i++) {
			if (i > 0) lore.append(",");
			lore.append("{text:\"").append(esc(lineas.get(i))).append("\",italic:false,color:\"").append(i == 1 ? "light_purple" : "white").append("\"}");
		}
		lore.append("]");
		String comps = "[minecraft:custom_name={text:\"" + nombre + "\",italic:false,color:\"gold\"},minecraft:lore=" + lore + "]";
		return pr("burla", "papel:" + ins, "PAPELITO", nombre, "light_purple", Clase.NEUTRO, (sv, pl, u) -> {
			cmd(sv, "give @p minecraft:paper" + comps + " 1");
			cmd(sv, "execute at @p run playsound minecraft:item.book.page_turn master @p ~ ~ ~ 1 1");
		});
	}

	private static Premio libro(String us, String yo) {
		String titulo = esc(uno(List.of("Carta de amor", "Manual del manco", "Diario de " + us, "Reglamento del noob",
				"Poemas para " + yo, "Lo que el chat piensa", "Confesiones", "Biografía de un " + uno(INSULTOS))));
		if (titulo.length() > 30) titulo = titulo.substring(0, 30);
		int paginas = entre(2, 4);
		List<String> pags = new ArrayList<>();
		for (int i = 0; i < paginas; i++) pags.add(pagina(us, yo));
		StringBuilder sb = new StringBuilder("[");
		for (int i = 0; i < pags.size(); i++) { if (i > 0) sb.append(","); sb.append("\"").append(esc(pags.get(i)).replace("\n", "\\n")).append("\""); }
		sb.append("]");
		String comps = "[minecraft:written_book_content={title:\"" + titulo + "\",author:\"" + esc(us) + "\",pages:" + sb + "}]";
		String t = titulo;
		return pr("burla", "libro:" + titulo, "LIBRO DEL CHAT", "\"" + t + "\" de " + us, "light_purple", Clase.NEUTRO, (sv, pl, u) -> {
			cmd(sv, "give @p minecraft:written_book" + comps + " 1");
			cmd(sv, "execute at @p run playsound minecraft:item.book.page_turn master @p ~ ~ ~ 1 0.8");
		});
	}

	private static String pagina(String us, String yo) {
		String i1 = uno(INSULTOS), i2 = uno(INSULTOS), a = uno(ASCII);
		return switch (r().nextInt(12)) {
			case 0 -> "Querido " + yo + ":\n\nTe escribo para decirte que sos un " + i1 + ".\n\n" + uno(FIRMAS) + ",\n" + us;
			case 1 -> "MANUAL DEL MANCO\n\nPaso 1: no morir.\nPaso 2: morir igual.\nPaso 3: llorar.\nPaso 4: ser " + i1 + ".";
			case 2 -> "\n\n   " + a + "\n\n" + may(i1) + "\n\n   " + a;
			case 3 -> "Cosas que " + yo + " hace bien:\n\n1.\n2.\n3.\n\n(nada)";
			case 4 -> "Diario de " + us + ":\n\nHoy vi jugar a " + yo + ".\nFue " + i1 + ".\nNo lo recomiendo.";
			case 5 -> "AVISO LEGAL\n\nEl portador de este libro es oficialmente " + i1 + ".\n\nFirmado: el chat";
			case 6 -> "Si leíste esto, sos puto.\n\nGracias por leer.\n\n- " + us;
			case 7 -> "Poema para " + yo + ":\n\nRosas rojas,\ncreepers verdes,\nsos tan " + i1 + "\nque siempre perdés.";
			case 8 -> "Encuesta del chat:\n\n¿" + yo + " es " + i1 + "?\n\nSí: 99%\nNo: 1% (" + yo + ")";
			case 9 -> "DIBUJO:\n\n" + a + "\n" + a + "\n" + a + "\n\nTe lo regalo.";
			case 10 -> "Receta:\n\n1 " + i1 + "\n2 " + i2 + "\nMezclar.\nResultado: " + yo + ".";
			default -> "Querido diario:\nhoy " + yo + " murió de nuevo.\nQué " + i1 + ".\nQué " + i2 + ".\n\nChupala.";
		};
	}

	// =====================================================================
	// MENSAJES en pantalla
	// =====================================================================
	private static final List<String> MENSAJES = List.of(
			"Puto el que lee", "El que lee es puto x2", "Chupala", "Tomatela", "Andá a cagar", "Sos re manco", "GG EZ",
			"Error 404: skill no encontrada", "Gastaste puntos en esto jaja", "Mirá atrás... mentira", "Manco confirmado",
			"Ni Herobrine te quiere", "Saludá a tu vieja", "Hoy no es tu día", "Estás siendo observado",
			"Diamantes a 3 bloques (mentira)", "Te quedaste sin suerte", "Felicitaciones, perdiste", "No mires el chat",
			"Alex dice que juegues mejor", "Ruleta trucha, reclamos al chat", "Tenés cara de zombie", "¿Te bañaste hoy?",
			"Que alguien le pase un mapa", "Modo pro desactivado", "Cargando talento... 1%", "Los creepers te buscan",
			"El chat vota que sos noob", "Ganaste: nada", "Si leés esto, le debés una pizza al chat", "%u te manda saludos",
			"%u te odia un poquito", "%u pagó por este mensaje", "%u dice: sos manco", "%u apostó a que morís",
			"Puto el que lee (firma: %u)", "Ojo que %u te está mirando", "Menos Minecraft, más tocar pasto",
			"Plot twist: no pasa nada", "Inserte moneda para continuar", "Che, ¿y los diamantes?", "%y es %i",
			"%y, sos %i", "Chupala %y", "%y el más %i del server", "Tomá, %i", "%u dice que sos %i",
			"Callate %i", "Bancá, %i", "%y modo %i activado", "Hola %i", "Qué hacés, %i", "Dale %i, jugá",
			"Sos %i y lo sabés", "No seas %i, %y", "Te quiero, %i");

	private static Premio mensaje(String us, String yo) {
		String m = uno(MENSAJES);
		String ins = uno(INSULTOS);
		String txt = m.replace("%u", us).replace("%y", yo).replace("%i", ins);
		Accion a = (sv, pl, u) -> cmd(sv, "execute at @p run playsound minecraft:entity.villager.ambient master @p ~ ~ ~ 1 " + (0.8 + r().nextDouble() * 0.6));
		if (txt.length() <= 22) return pr("mensaje", m, may(txt), null, "yellow", Clase.NEUTRO, a);
		return pr("mensaje", m, "MENSAJE DEL CHAT", txt, "yellow", Clase.NEUTRO, a);
	}

	// =====================================================================
	// SUSTOS (sonidos detrás tuyo)
	// =====================================================================
	private static final List<String> SONIDOS = List.of(
			"entity.creeper.primed", "entity.ghast.scream", "ambient.cave", "entity.enderman.stare", "entity.enderman.scream",
			"entity.wither.spawn", "entity.elder_guardian.curse", "entity.warden.emerge", "entity.warden.roar",
			"entity.goat.screaming.ambient", "entity.zombie.attack_wooden_door", "entity.generic.explode", "block.anvil.land",
			"entity.player.hurt", "entity.skeleton.ambient", "entity.spider.ambient", "entity.tnt.primed",
			"entity.lightning_bolt.thunder", "entity.wolf.growl", "entity.phantom.swoop", "block.chest.open",
			"block.wooden_door.open", "entity.creaking.activate", "entity.ravager.roar", "entity.evoker.prepare_summon");

	private static Premio susto() {
		String id = uno(SONIDOS);
		String t = uno(List.of("NADA... ¿O SÍ?", "SHHH...", "¿ESCUCHASTE ESO?", "NO TE DES VUELTA", "DETRÁS TUYO"));
		return pr("susto", id, t, "prestá atención...", "dark_gray", Clase.NEUTRO, (sv, pl, u) ->
				Ruleta.despues(20, s2 -> cmd(s2, "execute as @p at @s positioned ^ ^ ^-2 run playsound minecraft:" + id + " master @p ~ ~ ~ 1 " + (0.8 + r().nextDouble() * 0.3))));
	}

	// =====================================================================
	// LOCURAS NEUTRAS
	// =====================================================================
	private static Premio locura(ServerPlayer p, String us, String yo) {
		return switch (r().nextInt(12)) {
			case 0, 1 -> pr("locura", "tamano", "TAMAÑO RANDOM", "cortesía de " + us, "light_purple", Clase.NEUTRO, (sv, pl, u) -> Tamano.aplicar(sv, pl, us));
			case 2 -> pr("locura", "tntfalsa", "¡TNT!", "corré...", "red", Clase.NEUTRO, (sv, pl, u) -> {
				String m = "krim_t" + System.nanoTime();
				BlockPos pos = Premios.suelo((ServerLevel) pl.level(), pl, 2, 3);
				String donde = pos == null ? "~ ~ ~" : (pos.getX() + 0.5) + " " + pos.getY() + " " + (pos.getZ() + 0.5);
				cmd(sv, "execute as @p at @s run summon minecraft:tnt " + donde + " {fuse:80,Tags:[\"" + Krim.TAG_RULETA + "\",\"" + m + "\"]}");
				Ruleta.despues(70, s2 -> {
					cmd(s2, "execute at @e[tag=" + m + "] run particle minecraft:totem_of_undying ~ ~0.5 ~ 0.5 0.5 0.5 0.4 60 force");
					cmd(s2, "kill @e[tag=" + m + "]");
					cmd(s2, "title @p actionbar {text:\"¡ERA DE MENTIRA! jajaja\",color:\"gold\"}");
				});
			});
			case 3 -> pr("locura", "muerte", "ÚLTIMO MOMENTO", "mirá el chat del juego", "yellow", Clase.NEUTRO, (sv, pl, u) -> {
				String como = uno(List.of(" fue volado por ", " fue asesinado por ", " se cayó por culpa de ", " fue humillado por ", " murió de vergüenza por "));
				cmd(sv, "tellraw @a {text:\"" + yo + como + us + "\"}");
				Ruleta.despues(80, s2 -> cmd(s2, "tellraw @a {text:\"(mentira, sigue vivo... por ahora)\",color:\"gray\",italic:true}"));
			});
			case 4 -> pr("locura", "herobrine", "ALGUIEN ENTRÓ...", "mirá el chat del juego", "dark_gray", Clase.NEUTRO, (sv, pl, u) -> {
				cmd(sv, "tellraw @a {text:\"Herobrine se unió a la partida\",color:\"yellow\"}");
				cmd(sv, "execute at @p run playsound minecraft:ambient.cave master @p ~ ~ ~ 1 0.6");
				cmd(sv, "effect give @p minecraft:darkness 4 0 true");
				Ruleta.despues(140, s2 -> cmd(s2, "tellraw @a {text:\"Herobrine abandonó la partida\",color:\"yellow\"}"));
			});
			case 5 -> pr("locura", "gallinas", "¡LLUEVEN GALLINAS!", "cortesía de " + us, "aqua", Clase.NEUTRO, (sv, pl, u) -> lluvia(sv, "chicken", ",EggLayTime:2147483647", 10, 600, true));
			case 6 -> pr("locura", "animales", "¡LLUEVEN ANIMALES!", "cortesía de " + us, "aqua", Clase.NEUTRO,
					(sv, pl, u) -> lluvia(sv, uno(List.of("pig", "sheep", "cow", "frog", "rabbit", "armadillo")), "", 10, 600, true));
			case 7 -> pr("locura", "disco", "DISCO LOCO", us + " puso música", "light_purple", Clase.NEUTRO, (sv, pl, u) -> {
				String disco = uno(List.of("pigstep", "otherside", "cat", "chirp", "blocks", "creator", "relic", "precipice"));
				cmd(sv, "execute at @p run playsound minecraft:music_disc." + disco + " record @p ~ ~ ~ 1 1");
				for (int i = 0; i < 60; i++) Ruleta.despues(i * 10, s2 -> cmd(s2, "execute at @p run particle minecraft:note ~ ~2.2 ~ 1.5 0.5 1.5 1 3 force"));
				Ruleta.despues(600, s2 -> cmd(s2, "stopsound @p record"));
			});
			case 8 -> pr("locura", "enderman", "ENDERMAN BORRACHO", us + " te teletransportó", "dark_purple", Clase.NEUTRO, (sv, pl, u) -> {
				BlockPos pos = suelo((ServerLevel) pl.level(), pl, 5, 10);
				if (pos == null) return;
				cmd(sv, "execute at @p run particle minecraft:portal ~ ~1 ~ 0.5 1 0.5 0.5 60 force");
				cmd(sv, "execute as @p at @s run tp @s " + (pos.getX() + 0.5) + " " + pos.getY() + " " + (pos.getZ() + 0.5));
				cmd(sv, "execute at @p run playsound minecraft:entity.enderman.teleport master @p ~ ~ ~ 1 1");
			});
			case 9 -> pr("locura", "globo", "¡TE LLEVÓ UN GLOBO!", "agarrate fuerte", "light_purple", Clase.NEUTRO, (sv, pl, u) -> {
				cmd(sv, "effect give @p minecraft:levitation 3 1 true");
				cmd(sv, "effect give @p minecraft:slow_falling 15 0 true");
			});
			case 10 -> pr("locura", "lunar", "GRAVEDAD LUNAR", "30 segundos flotando", "aqua", Clase.NEUTRO,
					(sv, pl, u) -> Atributos.aplicar(sv, "minecraft:gravity", "gravedad", -0.8, "add_multiplied_total", 30));
			default -> pr("locura", "otravez", "¡GIRÁ OTRA VEZ!", "la ruleta quiere más", "gold", Clase.NEUTRO, (sv, pl, u) -> Ruleta.otraVez(us));
		};
	}

	/** Llueven animales (sin daño por caída, sin drops) y a los X ticks desaparecen en un "puf". */
	private static void lluvia(MinecraftServer s, String id, String extra, int cuantos, int duracion, boolean desdeArriba) {
		String m = "krim_ll" + System.nanoTime();
		for (int i = 0; i < cuantos; i++) {
			String nbt = "{Tags:[\"" + Krim.TAG_RULETA + "\",\"" + m + "\"]," + SIN + extra + "}";
			Ruleta.despues(i * 4, sv -> cmd(sv, "execute at @p run summon minecraft:" + id + " ~" + entre(-4, 4) + " ~" + (desdeArriba ? 7 : 2) + " ~" + entre(-4, 4) + " " + nbt));
		}
		Ruleta.despues(cuantos * 4 + 2, sv -> cmd(sv, "effect give @e[tag=" + m + "] minecraft:slow_falling 15 0 true"));
		Ruleta.despues(duracion, sv -> {
			cmd(sv, "execute at @e[tag=" + m + "] run particle minecraft:poof ~ ~0.5 ~ 0.3 0.3 0.3 0.05 10 force");
			cmd(sv, "tp @e[tag=" + m + "] ~ -1000 ~");
			cmd(sv, "kill @e[tag=" + m + "]");
		});
	}

	// =====================================================================
	// GUISITO DE LA ABUELA (efecto random adentro)
	// =====================================================================
	private static final List<String[]> GUISO = List.of(
			new String[] { "night_vision", "visión nocturna" }, new String[] { "jump_boost", "supersalto" },
			new String[] { "regeneration", "regeneración" }, new String[] { "fire_resistance", "anti fuego" },
			new String[] { "speed", "velocidad" }, new String[] { "saturation", "panza llena" },
			new String[] { "blindness", "ceguera" }, new String[] { "nausea", "mareo" }, new String[] { "poison", "veneno" },
			new String[] { "weakness", "debilidad" }, new String[] { "slowness", "lentitud" }, new String[] { "hunger", "hambre" },
			new String[] { "wither", "marchitez" }, new String[] { "levitation", "levitación" }, new String[] { "glowing", "brillo" });

	private static Premio guisito(String us) {
		String[] ef = uno(GUISO);
		int segs = ef[0].equals("levitation") ? entre(3, 5) : ef[0].equals("wither") || ef[0].equals("poison") ? entre(5, 8) : entre(15, 30);
		String abuela = uno(List.of("Guisito de la abuela de %u", "Guiso misterioso de %u", "Sopita de la abuela de %u",
				"Guiso que cocinó %u", "Guisito con amor de %u")).replace("%u", us);
		String ingrediente = uno(List.of("ingrediente secreto: ???", "tiene algo raro adentro", "la abuela no dice qué tiene",
				"huele sospechoso", "receta familiar", "no preguntes qué tiene"));
		String comps = "[minecraft:suspicious_stew_effects=[{id:\"minecraft:" + ef[0] + "\",duration:" + (segs * 20) + "}],"
				+ "minecraft:custom_name={text:\"" + esc(abuela) + "\",italic:false,color:\"gold\"},"
				+ "minecraft:lore=[{text:\"" + ingrediente + "\",italic:true,color:\"gray\"}]]";
		return pr("guiso", ef[0], "GUISITO DE LA ABUELA", "¿qué tendrá adentro?", "gold", Clase.NEUTRO, (sv, pl, u) -> {
			cmd(sv, "give @p minecraft:suspicious_stew" + comps + " 1");
			cmd(sv, "execute at @p run playsound minecraft:block.brewing_stand.brew master @p ~ ~ ~ 1 1");
			Registro.info("RULETA", "Guisito con " + ef[1] + " por " + segs + " s");
		});
	}

	// =====================================================================
	// RAMO DE FLORES (random)
	// =====================================================================
	private static final List<String> FLORES = List.of("poppy", "dandelion", "blue_orchid", "allium", "azure_bluet", "red_tulip",
			"orange_tulip", "white_tulip", "pink_tulip", "oxeye_daisy", "cornflower", "lily_of_the_valley", "sunflower", "lilac",
			"rose_bush", "peony", "torchflower", "pink_petals", "spore_blossom");

	private static Premio ramo(String us) {
		boolean marchito = r().nextInt(100) < 15;
		List<String> elegidas = new ArrayList<>(FLORES);
		Collections.shuffle(elegidas);
		int tipos = entre(3, 5);
		String nombre = esc((marchito ? "Ramo marchito de %u" : uno(List.of("Ramo de flores de %u", "Flores para vos de %u",
				"Ramito de %u", "Bouquet de %u", "Flores robadas por %u"))).replace("%u", us));
		String comps = "[minecraft:custom_name={text:\"" + nombre + "\",italic:false,color:\"" + (marchito ? "dark_gray" : "light_purple") + "\"},"
				+ "minecraft:lore=[{text:\"" + esc(uno(List.of("con amor", "te las merecés (mentira)", "no se las des a nadie",
				"recién cortadas", "las sacó de tu jardín", "para que decores tu casa de tierra"))) + "\",italic:true,color:\"gray\"}]]";
		return pr("ramo", marchito ? "marchito" : "lindo", marchito ? "RAMO MARCHITO" : "RAMO DE FLORES",
				marchito ? "lo que te merecés, de " + us : "de parte de " + us, marchito ? "dark_gray" : "light_purple", Clase.NEUTRO, (sv, pl, u) -> {
					if (marchito) {
						cmd(sv, "give @p minecraft:dead_bush" + comps + " 3");
						cmd(sv, "give @p minecraft:wither_rose" + comps + " 1");
					} else {
						for (int i = 0; i < tipos; i++) cmd(sv, "give @p minecraft:" + elegidas.get(i) + comps + " " + entre(1, 3));
						cmd(sv, "execute at @p run particle minecraft:heart ~ ~1.5 ~ 0.6 0.5 0.6 0 10 force");
					}
					cmd(sv, "execute at @p run playsound minecraft:block.azalea.place master @p ~ ~ ~ 1 1.2");
				});
	}

	// =====================================================================
	// GALLETA DE LA FORTUNA
	// =====================================================================
	private static final List<String> FORTUNAS = List.of("Hoy alguien del chat te va a traicionar", "Tu próximo cofre estará vacío",
			"Un creeper piensa en vos", "Vas a encontrar lo que buscás... en otro mundo", "Tu número de la suerte es el 0",
			"Evitá las cuevas hoy", "Un perro te va a abandonar", "La fortuna sonríe a los valientes, no a vos",
			"Tu destino está escrito: manco", "Pronto tendrás un gran botín (de tierra)", "Confiá en el aldeano equivocado",
			"El chat te ama en secreto (mucho secreto)", "Mañana encontrarás diamantes. Mentira.", "Cuidado con el próximo canje",
			"Un gran poder conlleva un gran lag", "La lava es tu amiga (no)", "Hoy es buen día para morir de una forma tonta",
			"Alguien va a girar la ruleta de nuevo", "El que busca encuentra, menos vos", "Vas a morir con el inventario lleno",
			"Tu cama extraña tu presencia", "La suerte está de tu lado... el lado equivocado", "Un enderman te está mirando",
			"Compartí esta galleta o tendrás 7 años de lag");

	private static Premio galleta(String us) {
		String f = uno(FORTUNAS);
		String comps = "[minecraft:custom_name={text:\"Galleta de la fortuna\",italic:false,color:\"gold\"},"
				+ "minecraft:lore=[{text:\"" + esc(f) + "\",italic:true,color:\"yellow\"},{text:\"- de parte de " + esc(us) + "\",italic:false,color:\"gray\"}]]";
		return pr("galleta", f, "GALLETA DE LA FORTUNA", f, "gold", Clase.NEUTRO, (sv, pl, u) -> {
			cmd(sv, "give @p minecraft:cookie" + comps + " 1");
			cmd(sv, "tellraw @a [{text:\"Galleta de la fortuna: \",color:\"gold\"},{text:\"" + esc(f) + "\",color:\"yellow\",italic:true}]");
			cmd(sv, "execute at @p run playsound minecraft:entity.generic.eat master @p ~ ~ ~ 1 1.2");
		});
	}

	// =====================================================================
	// FRASES MOTIVADORAS, DESMOTIVADORAS Y POESÍA
	// =====================================================================
	private static final List<String> MOTIVADORAS = List.of("Hoy es un gran día para no morir", "Creé en vos, aunque nadie más lo haga",
			"La constancia vence al talento", "Cada muerte es un aprendizaje", "Hoy vas a encontrar diamantes",
			"Nunca es tarde para construir una casa", "El chat cree en vos (un poquito)", "Sos más fuerte que un zombie bebé",
			"Todo gran castillo empezó con un bloque de tierra", "Si te caés, levantate. Si te caés de 30 bloques, no",
			"Los creepers explotan, vos no", "El que persevera, minea", "Hoy sí, hoy se gana");
	private static final List<String> DESMOTIVADORAS = List.of("Nada de lo que hagas importa, igual hay creepers",
			"Mañana vas a morir igual", "Hay gente que nace con talento. Vos no", "Rendirse también es una opción",
			"El fracaso es tu mejor amigo", "Tanto minar para nada", "La lava te espera", "Tus amigos juegan mejor",
			"El esfuerzo no siempre paga, mirate a vos", "Todo diamante empezó siendo carbón. El tuyo sigue siendo carbón",
			"No es mala suerte, es falta de habilidad", "Ni el aldeano te quiere tradear", "Lo intentaste. Fallaste. Clásico");
	private static final List<String> POEMAS = List.of(
			"Rosas rojas,|creepers verdes,|si no corrés,|seguro perdés.",
			"En la cueva oscura|un ruido sonó,|no era un zombie,|era tu dignidad que se fue.",
			"Picaste piedra,|picaste carbón,|pero el diamante|te dijo que no.",
			"Oh, Steve querido,|de pico de madera,|te mató una gallina|y no fue la primera.",
			"La luna en el cielo,|el phantom también,|hace tres noches|que no dormís bien.",
			"Tu casa de tierra,|tu cama sin hacer,|el chat te observa|y no deja de joder.",
			"Un creeper me dijo|con voz de cariño:|sssss...|y no quedó ni el niño.",
			"El horno prendido,|la papa en el fuego,|vos jugás tan mal|que me da miedo.",
			"Bajaste a la mina|con fe y con valor,|volviste sin nada|y con olor a dolor.");

	private static Premio frase(String us) {
		int t = r().nextInt(3);
		if (t == 2) {
			String poema = uno(POEMAS);
			return pr("frase", poema, "POESÍA DEL CHAT", "mirá el chat del juego", "light_purple", Clase.NEUTRO, (sv, pl, u) -> {
				cmd(sv, "tellraw @a {text:\"Poesía de " + us + ":\",color:\"light_purple\",bold:true}");
				for (String linea : poema.split("\\|")) cmd(sv, "tellraw @a {text:\"  " + esc(linea) + "\",color:\"white\",italic:true}");
				cmd(sv, "execute at @p run playsound minecraft:block.note_block.harp master @p ~ ~ ~ 1 1.2");
			});
		}
		boolean motiva = t == 0;
		String f = uno(motiva ? MOTIVADORAS : DESMOTIVADORAS);
		return pr("frase", f, motiva ? "FRASE MOTIVADORA" : "FRASE DESMOTIVADORA", f, motiva ? "green" : "dark_gray", Clase.NEUTRO, (sv, pl, u) -> {
			cmd(sv, "tellraw @a [{text:\"" + (motiva ? "Frase motivadora" : "Frase desmotivadora") + " de " + us + ": \",color:\""
					+ (motiva ? "green" : "gray") + "\"},{text:\"" + esc(f) + "\",color:\"white\",italic:true}]");
			cmd(sv, "execute at @p run playsound minecraft:" + (motiva ? "entity.player.levelup" : "entity.villager.no") + " master @p ~ ~ ~ 1 1");
		});
	}

	// =====================================================================
	// MOB PARLANTE: un mob con el nombre del viewer que habla en el chat
	// =====================================================================
	private static final List<String[]> PARLANTES = List.of(
			new String[] { "pig", "chancho", "" }, new String[] { "cow", "vaca", "" }, new String[] { "sheep", "oveja", "" },
			new String[] { "chicken", "gallina", ",EggLayTime:2147483647" }, new String[] { "fox", "zorro", "" },
			new String[] { "parrot", "loro", "" }, new String[] { "rabbit", "conejo", "" }, new String[] { "panda", "panda", "" },
			new String[] { "frog", "rana", "" }, new String[] { "armadillo", "armadillo", "" }, new String[] { "villager", "aldeano", "" },
			new String[] { "llama", "llama", "" },
			new String[] { "zombie", "zombie", ZOMBIE }, new String[] { "skeleton", "esqueleto", "" },
			new String[] { "spider", "araña", "" }, new String[] { "witch", "bruja", "" }, new String[] { "creeper", "creeper", "" });
	private static final List<String> ES_HOSTIL = List.of("zombie", "skeleton", "spider", "witch", "creeper");

	private static final List<String> SALUDOS = List.of("¡Hola %y! Soy %u y vine a molestarte", "Llegó %u, abran paso",
			"Buenas, ¿acá es el stream de %y?", "Hola chat, hola %y, hola manco", "Me mandaron del chat a vigilarte",
			"Tranqui %y, vengo en son de paz... creo");
	private static final List<String> CHARLA = List.of("Qué linda casa... mentira, es horrible", "No me pegues, soy tu fan",
			"Te estoy siguiendo...", "¿Me das un diamante?", "Pagué puntos para estar acá, tratame bien",
			"Si me matás, el chat se entera", "Qué mal que minás, eh", "Ojo atrás tuyo... jaja mentira",
			"Tengo hambre, ¿hay asado?", "¿Jugamos a la mancha? Vos la llevás", "Estoy re perdido", "Tu inventario es un quilombo",
			"Esta noche duermo en tu cama", "Soy el mejor viewer del canal", "Saludos a todo el chat", "Te quiero mucho, %i",
			"¿Esto es survival? No parece", "Yo lo hubiera hecho mejor", "Puto el que lee", "No sé nadar, no me lleves al agua",
			"¿Dónde queda el Nether?", "Me aburro, hacé algo", "Vengo del futuro: morís en 5 minutos", "Soy un espía del chat",
			"¿Me adoptás?", "Ese pico está re gastado", "Necesitás más antorchas, amigo", "Escuché un creeper...",
			"Uy, ¿eso era lava?", "¡Corré %y, corré!", "No mires el chat", "Che %y, ¿y los diamantes?",
			"Mi primo es un creeper, cuidado", "Me dijeron que sos %i, ¿es verdad?", "Hoy vine a ver cómo morís",
			"Dale, construí algo lindo", "¿Por qué tenés tanta tierra?", "¿Quién te enseñó a jugar?", "Mirá cómo camino, re facha",
			"Estoy tan cerca que te puedo oler", "¿Te bañaste? Olés a zombie", "Votá por mí para intendente del server",
			"Un día voy a ser enderman", "%u para presidente", "No me dejes solo de noche",
			"Te sigo hasta el fin del mundo... o del chunk", "Ay, me picó una araña", "¿Qué hora es? No tengo reloj",
			"Me encanta tu skin, re original (no)", "Si me das pan te digo dónde hay diamantes", "Era mentira lo de los diamantes",
			"Respetame que tengo nombre", "Shhh... escuchá...", "Qué noche tan linda para morir", "Cuidado que muerdo",
			"Tirame un hueso, %y", "Soy el NPC más inteligente del server", "Esto es mejor que Netflix", "Jajaja qué %i",
			"Mirá, una mariposa... ah no, era un phantom", "Hoy estás jugando como un %i", "¿Me sacás una foto?",
			"Le voy a contar a todos que sos %i", "Mi abuela mina mejor que vos", "¿Ese es tu mejor equipo? Ay...",
			"Bancame que me ato los cordones", "Quiero ser tu mascota, pero no te quiero", "Una vez le gané a un warden. Mentira",
			"¿Por qué el cielo es cuadrado?", "Dejá de mirarme así", "¿Sabías que el chat te banca? Yo no",
			"Te debo 2 diamantes desde 2019", "Eu, eu, ¿me escuchás?", "Mirá que soy importante, eh");
	private static final List<String> DESPEDIDAS = List.of("Bueno, me voy. Chau %i", "Me aburriste, me voy",
			"Me llama mi vieja, chau", "Nos vemos en el próximo canje", "Me voy a otro stream, chau",
			"Fue un placer molestarte, %y", "Adiós, %i. Te voy a extrañar (no)");

	private static Premio hablador(String us, String yo) {
		String[] m = uno(PARLANTES);
		boolean hostil = ES_HOSTIL.contains(m[0]);
		return pr("hablador", m[0], "MOB PARLANTE", "un " + m[1] + " llamado " + us, "aqua", hostil ? Clase.HOSTIL : Clase.NEUTRO, (sv, pl, u) -> {
			String marca = "krim_h" + System.nanoTime();
			BlockPos pos = lugar((ServerLevel) pl.level(), pl, Lugar.SUELO);
			String nbt = "{Tags:[\"" + Krim.TAG_RULETA + "\",\"" + marca + "\"]," + SIN + ",CustomNameVisible:1b,PersistenceRequired:1b,"
					+ "CustomName:{text:\"" + us + "\",color:\"aqua\"}" + m[2] + "}";
			cmd(sv, "execute as @p at @s run summon minecraft:" + m[0] + " " + (pos.getX() + 0.5) + " " + pos.getY() + " " + (pos.getZ() + 0.5) + " " + nbt);
			cmd(sv, "execute at @e[tag=" + marca + "] run particle minecraft:large_smoke ~ ~0.5 ~ 0.4 0.6 0.4 0.02 20 force");
			List<String> frases = new ArrayList<>(CHARLA);
			Collections.shuffle(frases);
			int n = entre(8, 12), t = 20;
			decir(sv, marca, us, m[1], llenar(uno(SALUDOS), us, yo));
			for (int i = 0; i < n; i++) {
				t += entre(100, 170);
				String f = llenar(frases.get(i), us, yo);
				Ruleta.despues(t, s2 -> decir(s2, marca, us, m[1], f));
			}
			String chau = llenar(uno(DESPEDIDAS), us, yo);
			Ruleta.despues(t + 120, s2 -> {
				decir(s2, marca, us, m[1], chau);
				if (!hostil) {
					cmd(s2, "execute at @e[tag=" + marca + "] run particle minecraft:poof ~ ~0.5 ~ 0.4 0.5 0.4 0.05 20 force");
					cmd(s2, "tp @e[tag=" + marca + "] ~ -1000 ~");
					cmd(s2, "kill @e[tag=" + marca + "]");
				}
			});
		});
	}

	private static String llenar(String f, String us, String yo) {
		return f.replace("%u", us).replace("%y", yo).replace("%i", uno(INSULTOS));
	}

	/** El mob "habla": mensaje en el chat con formato de jugador, solo si sigue vivo. */
	private static void decir(MinecraftServer s, String marca, String us, String especie, String f) {
		cmd(s, "execute as @e[tag=" + marca + ",limit=1] run tellraw @a [{text:\"<\",color:\"white\"},{text:\"" + us + "\",color:\"aqua\"},"
				+ "{text:\" (" + especie + ")\",color:\"gray\"},{text:\"> " + esc(f) + "\",color:\"white\"}]");
		cmd(s, "execute at @e[tag=" + marca + ",limit=1] run particle minecraft:note ~ ~2.2 ~ 0 0 0 1 1 force");
	}

	// =====================================================================
	// EFECTOS
	// =====================================================================


	private record Ef(String id, String nombre, int min, int max, int ampMax) {}

	private static final List<Ef> BUENOS = List.of(
			new Ef("speed", "Velocidad", 20, 30, 2), new Ef("haste", "Prisa minera", 20, 30, 2), new Ef("strength", "Fuerza", 20, 30, 1),
			new Ef("regeneration", "Regeneración", 15, 25, 1), new Ef("resistance", "Resistencia", 20, 30, 1),
			new Ef("fire_resistance", "Anti fuego", 25, 30, 0), new Ef("night_vision", "Visión nocturna", 25, 30, 0),
			new Ef("absorption", "Corazones extra", 25, 30, 1), new Ef("water_breathing", "Pulmones de pez", 25, 30, 0),
			new Ef("jump_boost", "Supersalto", 20, 30, 2));

	private static final List<Ef> MALOS = List.of(
			new Ef("slowness", "Lentitud", 18, 30, 2), new Ef("mining_fatigue", "Fiaca minera", 18, 30, 1),
			new Ef("nausea", "Mareo", 15, 25, 0), new Ef("blindness", "Ceguera", 10, 15, 0), new Ef("darkness", "Oscuridad", 15, 25, 0),
			new Ef("hunger", "Hambre", 20, 30, 2), new Ef("weakness", "Debilidad", 20, 30, 1), new Ef("poison", "Veneno", 9, 14, 0),
			new Ef("levitation", "Levitación", 4, 6, 0), new Ef("wither", "Marchitez", 7, 11, 0));

	private static Premio efecto(boolean bueno) {
		Ef e = uno(bueno ? BUENOS : MALOS);
		int dur = entre(e.min(), e.max());
		int amp = entre(0, e.ampMax());
		return pr(bueno ? "efbueno" : "efmalo", e.id(), may(e.nombre()) + romano(amp + 1), "por " + dur + " segundos",
				bueno ? "green" : "red", bueno ? Clase.BUENO : Clase.MALO, (sv, pl, u) -> {
					cmd(sv, "effect give @p minecraft:" + e.id() + " " + dur + " " + amp + " false");
					if (e.id().equals("levitation")) cmd(sv, "effect give @p minecraft:slow_falling " + (dur + 10) + " 0 true");
				});
	}

	// =====================================================================
	// ÍTEMS (nada OP; muchos de broma)
	// =====================================================================
	private record It(String id, int min, int max, String nombre, String custom, int peso) {}

	private static final List<It> ITEMS = List.of(
			new It("bread", 3, 6, "Pan", null, 4), new It("cooked_beef", 2, 4, "Bife", "Asadito de %u", 4),
			new It("cookie", 4, 10, "Galletitas", "Galletitas de %u", 4), new It("baked_potato", 3, 6, "Papas", null, 3),
			new It("torch", 8, 16, "Antorchas", null, 4), new It("arrow", 8, 16, "Flechas", null, 3), new It("iron_ingot", 1, 4, "Hierro", null, 3),
			new It("coal", 4, 10, "Carbón", null, 3), new It("string", 3, 6, "Hilo", null, 2), new It("bone_meal", 6, 12, "Polvo de hueso", null, 2),
			new It("dirt", 1, 1, "Tierra premium", "Tierra premium de %u", 6), new It("stick", 1, 1, "Varita mágica", "Varita mágica de %u", 6),
			new It("poisonous_potato", 1, 1, "Papa sospechosa", "Papa sospechosa de %u", 6), new It("rotten_flesh", 3, 6, "Asado vencido", "Asado de %u (vencido)", 6),
			new It("pufferfish", 1, 1, "Sushi", "Sushi de %u", 5),
			new It("bone", 1, 1, "Hueso", "Hueso de la suerte de %u", 4), new It("wooden_sword", 1, 1, "Excalibur", "Excalibur de %u", 5),
			new It("wooden_shovel", 1, 1, "Pala legendaria", "Pala legendaria de %u", 4), new It("feather", 1, 1, "Pluma", "Pluma de la fama de %u", 3),
			new It("paper", 1, 1, "Factura", "Factura de %u (impaga)", 4), new It("spider_eye", 1, 2, "Snack", "Snack de %u", 4),
			new It("cactus", 1, 1, "Cactus", "Abrazo de %u", 4), new It("snowball", 8, 16, "Bolas de nieve", "Bolas de nieve del chat", 3),
			new It("cod", 1, 2, "Pescado", "Pescado podrido de %u", 3), new It("pink_wool", 1, 1, "Lana rosa", "Peluche de %u", 3),
			new It("compass", 1, 1, "Brújula", "Brújula que apunta a %u", 2), new It("clock", 1, 1, "Reloj", "Reloj trucho de %u", 2),
			new It("glass_bottle", 1, 1, "Botella vacía", "Fernet de %u (vacío)", 4), new It("bowl", 1, 1, "Bowl vacío", "Guiso de %u (ya se lo comió)", 4),
			new It("leather", 1, 1, "Cuero", "Billetera de %u (vacía)", 3), new It("flint", 1, 1, "Piedrita", "Piedrita de la suerte de %u", 3),
			new It("music_disc", 1, 1, "Disco", "Disco favorito de %u", 2));

	private static final List<String> DISCOS = List.of("13", "cat", "blocks", "chirp", "far", "mall", "mellohi", "stal",
			"strad", "ward", "11", "wait", "otherside", "5", "pigstep", "relic", "creator", "precipice");

	private static Premio item(String us) {
		int total = 0;
		for (It i : ITEMS) total += i.peso();
		int x = r().nextInt(total);
		It it = ITEMS.get(0);
		for (It i : ITEMS) { x -= i.peso(); if (x < 0) { it = i; break; } }
		int cant = entre(it.min(), it.max());
		String id = it.id().equals("music_disc") ? "music_disc_" + uno(DISCOS) : it.id();
		String nombre = it.custom() == null ? null : esc(it.custom().replace("%u", us));
		String comps = "[minecraft:lore=[{text:\"Ruleta de " + us + "\",italic:false,color:\"gray\"}]"
				+ (nombre != null ? ",minecraft:custom_name={text:\"" + nombre + "\",italic:false,color:\"gold\"}" : "") + "]";
		return pr("item", it.id(), (cant > 1 ? cant + " " : "") + may(it.nombre()), nombre != null ? nombre : "regalo de " + us, "aqua", Clase.BUENO, (sv, pl, u) -> {
			cmd(sv, "give @p minecraft:" + id + comps + " " + cant);
			cmd(sv, "execute at @p run playsound minecraft:entity.item.pickup player @p ~ ~ ~ 1 1");
		});
	}

	// =====================================================================
	// Doble y jackpot
	// =====================================================================
	private static Premio doble(MinecraftServer s, ServerPlayer p, String us, boolean tregua, boolean hostilesOk) {
		Premio a = null, b = null;
		for (int i = 0; i < 25 && (a == null || b == null); i++) {
			Premio x = generar(s, p, us, tregua, hostilesOk, false);
			if (x == null || !permitido(x, tregua, hostilesOk)) continue;
			if (x.clase() == Clase.BUENO && r().nextInt(3) > 0) continue; // el doble suele ser doble castigo
			if (a == null) a = x; else if (!x.clave().equals(a.clave())) b = x;
		}
		if (a == null || b == null) return null;
		Premio pa = a, pb = b;
		Clase peor = (pa.clase() == Clase.HOSTIL || pb.clase() == Clase.HOSTIL) ? Clase.HOSTIL
				: (pa.clase() == Clase.MALO || pb.clase() == Clase.MALO) ? Clase.MALO : Clase.NEUTRO;
		return pr("doble", pa.clave() + "+" + pb.clave(), "¡DOBLE!", corta(pa.titulo()) + " + " + corta(pb.titulo()), "gold", peor, (sv, pl, u) -> {
			pa.accion().hacer(sv, pl, u);
			Ruleta.despues(40, s2 -> {
				ServerPlayer p2 = Ruleta.jugador(s2);
				if (p2 != null) pb.accion().hacer(s2, p2, u);
			});
		});
	}

	private static Premio jackpot(String us) {
		return pr("jackpot", "jackpot", "¡¡JACKPOT!!", us + " se apiadó de vos", "gold", Clase.BUENO, (sv, p, u) -> {
			String lore = "[minecraft:lore=[{text:\"JACKPOT de " + us + "\",italic:false,color:\"gold\"}]]";
			cmd(sv, "give @p minecraft:diamond" + lore + " 1");
			cmd(sv, "give @p minecraft:iron_ingot" + lore + " 6");
			cmd(sv, "give @p minecraft:emerald" + lore + " 3");
			cmd(sv, "execute at @p run playsound minecraft:ui.toast.challenge_complete master @p ~ ~ ~ 1 1");
			fuegos(8);
		});
	}

	static void fuegos(int cuantos) {
		String[] formas = { "large_ball", "star", "burst", "small_ball", "creeper" };
		for (int i = 0; i < cuantos; i++) {
			String forma = formas[r().nextInt(formas.length)];
			int c1 = r().nextInt(0xFFFFFF), c2 = r().nextInt(0xFFFFFF);
			String nbt = "{LifeTime:" + entre(18, 28) + "," + TAGR + ",FireworksItem:{id:\"minecraft:firework_rocket\",count:1,"
					+ "components:{\"minecraft:fireworks\":{flight_duration:1b,explosions:[{shape:\"" + forma + "\",colors:[I;" + c1 + "," + c2
					+ "],has_twinkle:1b,has_trail:1b}]}}}}";
			Ruleta.despues(i * 8, sv -> cmd(sv, "execute at @p run summon minecraft:firework_rocket ~" + entre(-5, 5) + " ~12 ~" + entre(-5, 5) + " " + nbt));
		}
	}

	// =====================================================================
	// Lugares seguros
	// =====================================================================
	private static boolean libre(ServerLevel l, BlockPos pos) { return l.getBlockState(pos).isAir(); }

	private static boolean piso(ServerLevel l, BlockPos pos) {
		BlockPos abajo = pos.below();
		return l.getBlockState(abajo).isFaceSturdy(l, abajo, Direction.UP)
				&& !l.getBlockState(abajo).is(Blocks.MAGMA_BLOCK) && !l.getBlockState(abajo).is(Blocks.CAMPFIRE);
	}

	static BlockPos suelo(ServerLevel l, ServerPlayer p, int dmin, int dmax) {
		int y0 = p.blockPosition().getY();
		for (int i = 0; i < 24; i++) {
			double ang = r().nextDouble() * Math.PI * 2;
			int d = entre(dmin, dmax);
			int x = (int) Math.floor(p.getX() + Math.cos(ang) * d);
			int z = (int) Math.floor(p.getZ() + Math.sin(ang) * d);
			for (int dy = 3; dy >= -4; dy--) {
				BlockPos pos = new BlockPos(x, y0 + dy, z);
				if (libre(l, pos) && libre(l, pos.above()) && piso(l, pos)) return pos;
			}
		}
		return null;
	}

	private static BlockPos lugar(ServerLevel l, ServerPlayer p, Lugar lug) {
		BlockPos pos = switch (lug) {
			case VUELA -> aire(l, p);
			case AGUA -> buscarAgua(l, p.blockPosition());
			case GRANDE -> lugarGrande(l, p);
			case LEJOS -> suelo(l, p, 9, 13);
			default -> suelo(l, p, 4, 7);
		};
		if (pos == null) pos = suelo(l, p, 2, 5);
		return pos == null ? p.blockPosition() : pos;
	}

	private static BlockPos aire(ServerLevel l, ServerPlayer p) {
		for (int i = 0; i < 12; i++) {
			BlockPos pos = p.blockPosition().offset(entre(-4, 4), entre(4, 8), entre(-4, 4));
			if (libre(l, pos) && libre(l, pos.above())) return pos;
		}
		return suelo(l, p, 3, 6);
	}

	private static BlockPos lugarGrande(ServerLevel l, ServerPlayer p) {
		for (int h = 12; h >= 8; h--) {
			BlockPos base = p.blockPosition().above(h);
			boolean ok = true;
			for (int dx = -2; dx <= 2 && ok; dx += 2)
				for (int dy = 0; dy <= 4 && ok; dy += 2)
					for (int dz = -2; dz <= 2 && ok; dz += 2)
						if (!libre(l, base.offset(dx, dy, dz))) ok = false;
			if (ok) return base;
		}
		return null;
	}

	private static BlockPos buscarAgua(ServerLevel l, BlockPos c) {
		for (int dx = -6; dx <= 6; dx++)
			for (int dy = -3; dy <= 1; dy++)
				for (int dz = -6; dz <= 6; dz++) {
					BlockPos pos = c.offset(dx, dy, dz);
					if (l.getBlockState(pos).is(Blocks.WATER) && l.getBlockState(pos.below()).is(Blocks.WATER)) return pos;
				}
		return null;
	}

	// =====================================================================
	// Etiquetas falsas para los otros sectores de la rueda
	// =====================================================================
	private static final List<String> FALSAS;
	static {
		List<String> l = new ArrayList<>();
		for (Mob m : MOBS) l.add(corta(may(m.nombre())));
		for (Ef e : MALOS) l.add(corta(may(e.nombre())));
		for (Ef e : BUENOS) l.add(corta(may(e.nombre())));
		l.addAll(List.of("A VOLAR", "CONGELADO", "PIÑA", "FLECHAS", "SIN PATAS", "GRAVEDAD x3", "T-REX", "BORRACHERA",
				"PÁNICO", "HAMBRE", "REVUELTO", "TNT", "HORDA", "EMBOSCADA", "CREEPERS", "MAPA", "PAPELITO", "LIBRO",
				"PUTO EL Q LEE", "CHUPALA", "JACKPOT", "NADA", "DOBLE", "TAMAÑO", "DISCO", "HEROBRINE", "GALLINAS",
				"GLOBO", "LUNAR", "MURCIÉLAGOS", "APAGÓN", "AL BORDE", "VEX", "PHANTOMS", "ANIMAL", "SUSTO", "MANCO",
				"GUISITO", "RAMO", "GALLETA", "POESÍA", "FRASE", "PREGUNTA", "PARLANTE", "TRIVIA"));
		FALSAS = List.copyOf(l);
	}

	static String etiquetaFalsa() { return uno(FALSAS); }
}
