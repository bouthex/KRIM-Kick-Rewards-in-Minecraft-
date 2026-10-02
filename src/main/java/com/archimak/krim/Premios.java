package com.archimak.krim;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Catálogo de la ruleta. Cada tiro arma un premio nuevo combinando categoría, elemento, cantidad,
 * duración, potencia y nombre, así que las combinaciones son prácticamente infinitas.
 *
 * Reglas (las mismas de todo KRIM):
 * - Nada rompe bloques ni daña a otros mobs: lo hostil lleva el tag krim_ruleta y los mixins lo limitan al jugador.
 * - Nada suelta ítems ni experiencia al morir.
 * - Nunca: warden, wither, ender dragon (ni elder guardian, gigante, devastador, enderman, blaze, lepisma,
 *   que rompen o queman el terreno o son injustos).
 * - Tregua: con poca vida no salen hostiles ni castigos.
 */
public final class Premios {
	public enum Clase { BUENO, MALO, NEUTRO, HOSTIL }

	@FunctionalInterface
	public interface Accion { void hacer(MinecraftServer s, ServerPlayer p, String u); }

	public record Premio(String titulo, String detalle, String color, Clase clase, Accion accion) {}

	private static final String SIN = "DeathLootTable:\"minecraft:empty\",CanPickUpLoot:0b";
	private static final String TAGR = "Tags:[\"" + Krim.TAG_RULETA + "\"]";

	private static ThreadLocalRandom r() { return ThreadLocalRandom.current(); }
	private static int entre(int a, int b) { return a + r().nextInt(b - a + 1); }
	private static <T> T uno(List<T> l) { return l.get(r().nextInt(l.size())); }
	private static void cmd(MinecraftServer s, String c) { Krim.cmd(s, Krim.fuente(s), c); }
	private static String u(String t, String usuario) { return t.replace("%u", usuario); }
	private static String may(String t) { return t.toUpperCase(Locale.ROOT); }
	private static String romano(int n) { return switch (n) { case 2 -> " II"; case 3 -> " III"; case 4 -> " IV"; case 5 -> " V"; default -> ""; }; }
	private static final Accion NADA = (s, p, us) -> { };

	// =====================================================================
	// Sorteo
	// =====================================================================
	public static Premio sortear(MinecraftServer s, ServerPlayer p, String usuario, boolean tregua, boolean hostilesOk) {
		for (int i = 0; i < 30; i++) {
			Premio pr = generar(s, p, usuario, tregua, hostilesOk, true);
			if (pr != null && permitido(pr, tregua, hostilesOk)) return pr;
		}
		return mensaje(usuario);
	}

	private static boolean permitido(Premio pr, boolean tregua, boolean hostilesOk) {
		if (pr.clase() == Clase.HOSTIL && (tregua || !hostilesOk)) return false;
		return !(pr.clase() == Clase.MALO && tregua);
	}

	private static Premio generar(MinecraftServer s, ServerPlayer p, String us, boolean tregua, boolean hostilesOk, boolean permitirDoble) {
		int x = r().nextInt(112);
		if (x < 16) return mensaje(us);
		if (x < 24) return sonido();
		if (x < 34) return efecto(true);
		if (x < 44) return efecto(false);
		if (x < 59) return item(us);
		if (x < 72) return hostil(p, us);
		if (x < 77) return animal(us);
		if (x < 104) return especial(p, us);
		if (x < 107) return permitirDoble ? doble(s, p, us, tregua, hostilesOk) : null;
		if (x < 108) return jackpot(us);
		return nada(us);
	}

	// =====================================================================
	// Mensajes
	// =====================================================================
	private static final List<String> MENSAJES = List.of(
			"Puto el que lee", "El que lee es puto x2", "Tomá agua", "Andá a dormir", "Comé algo, flaco",
			"El chat te quiere (mentira)", "Error 404: skill no encontrada", "Nada. Literalmente nada.",
			"Gastaste puntos en esto jaja", "Mirá atrás... mentira", "Sos re manco", "GG EZ",
			"El creeper era tu amigo", "Ni Herobrine te quiere", "Saludá a tu vieja", "Cuidado con la lava",
			"Ese pico no va a picar solo", "Hoy no es tu día", "Estás siendo observado",
			"¿Y si construís una casa?", "Ese ruido no fue nada...", "Diamantes a 3 bloques (mentira)",
			"Manco confirmado", "Like si leíste esto", "Sos el elegido", "Respirá hondo",
			"Te quedaste sin suerte", "Felicitaciones, perdiste", "Premio: un abrazo del chat",
			"Este mensaje se autodestruye", "Mensaje oficial del gobierno: jugá mejor", "No mires el chat",
			"Steve te manda saludos", "Alex dice que juegues mejor", "Estás a un bloque de la gloria",
			"La próxima sale algo bueno", "Ruleta trucha, reclamos al chat", "Tenés cara de zombie",
			"Sos más lento que una tortuga", "¿Te bañaste hoy?", "Que alguien le pase un mapa",
			"Eso fue sospechoso", "Modo pro desactivado", "Cargando talento... 1%", "Ojo con las arañas",
			"Los creepers te buscan", "Dormí en una cama, por favor", "Este era el premio bueno",
			"El mensaje más caro del mundo", "El chat vota que sos noob", "Hola, soy un mensaje",
			"Ganaste: nada", "Si leés esto, le debés una pizza al chat", "%u te manda saludos",
			"%u te odia un poquito", "%u pagó por este mensaje", "%u es el MVP del chat",
			"%u dice: sos manco", "%u dice que te quiere", "%u quiere ver una casa linda",
			"%u se gastó los puntos en vos", "Abrazo de %u", "%u: dale que va", "%u apostó a que morís",
			"Tranqui, %u te cuida", "Puto el que lee (firma: %u)", "Ojo que %u te está mirando",
			"Menos Minecraft, más tocar pasto", "Achievement: aburrir al chat", "Tu cama te extraña",
			"El horno sigue prendido", "Te olvidaste algo en el cofre", "¿Ese era tu perro?",
			"Plot twist: no pasa nada", "Inserte moneda para continuar", "Ruleta en mantenimiento",
			"Che, ¿y los diamantes?", "Mañana es lunes", "El Nether te extraña", "Hasta la vista, baby");

	private static Premio mensaje(String us) {
		String m = u(uno(MENSAJES), us);
		if (m.length() <= 22) return new Premio(may(m), null, "yellow", Clase.NEUTRO, NADA);
		return new Premio("MENSAJE DEL DESTINO", m, "yellow", Clase.NEUTRO, NADA);
	}

	// =====================================================================
	// Sonidos sospechosos (suenan detrás tuyo)
	// =====================================================================
	private static final List<String> SONIDOS = List.of(
			"entity.creeper.primed", "entity.ghast.scream", "ambient.cave", "entity.enderman.stare",
			"entity.enderman.scream", "entity.wither.spawn", "entity.elder_guardian.curse", "entity.warden.emerge",
			"entity.warden.heartbeat", "entity.goat.screaming.ambient", "entity.villager.ambient",
			"entity.evoker.prepare_wololo", "entity.cat.hiss", "entity.zombie.attack_wooden_door",
			"entity.generic.explode", "block.anvil.land", "entity.player.hurt", "entity.skeleton.ambient",
			"entity.spider.ambient", "entity.zombie.ambient", "entity.tnt.primed", "item.totem.use",
			"entity.player.burp", "entity.witch.celebrate", "block.bell.use", "entity.lightning_bolt.thunder",
			"entity.pig.ambient", "entity.wolf.growl", "entity.phantom.swoop", "block.chest.open",
			"block.wooden_door.open", "entity.creaking.activate", "entity.breeze.idle_ground");

	private static Premio sonido() {
		String id = uno(SONIDOS);
		String t = uno(List.of("NADA... ¿O SÍ?", "SHHH...", "¿ESCUCHASTE ESO?", "NO TE DES VUELTA"));
		return new Premio(t, "Prestá atención...", "dark_gray", Clase.NEUTRO, (s, p, us) ->
				Ruleta.despues(15, sv -> cmd(sv, "execute as @p at @s positioned ^ ^ ^-2 run playsound minecraft:" + id + " master @p ~ ~ ~ 1 " + (0.8 + r().nextDouble() * 0.4))));
	}

	// =====================================================================
	// Efectos
	// =====================================================================
	private record Ef(String id, String nombre, int min, int max, int ampMax) {}

	private static final List<Ef> BUENOS = List.of(
			new Ef("speed", "Velocidad", 15, 40, 2), new Ef("haste", "Prisa minera", 20, 60, 2),
			new Ef("strength", "Fuerza", 10, 30, 1), new Ef("jump_boost", "Salto", 15, 40, 2),
			new Ef("regeneration", "Regeneración", 8, 20, 1), new Ef("resistance", "Resistencia", 15, 40, 1),
			new Ef("fire_resistance", "Anti fuego", 30, 90, 0), new Ef("water_breathing", "Pulmones de pez", 30, 120, 0),
			new Ef("invisibility", "Invisibilidad", 15, 45, 0), new Ef("night_vision", "Visión nocturna", 30, 120, 0),
			new Ef("absorption", "Corazones extra", 30, 60, 2), new Ef("luck", "Suerte", 60, 180, 1),
			new Ef("slow_falling", "Caída de pluma", 20, 60, 0), new Ef("dolphins_grace", "Delfín", 20, 60, 0),
			new Ef("conduit_power", "Poder de conducto", 20, 60, 0), new Ef("hero_of_the_village", "Héroe de la aldea", 60, 180, 1),
			new Ef("saturation", "Panza llena", 1, 3, 0));

	private static final List<Ef> MALOS = List.of(
			new Ef("slowness", "Lentitud", 8, 20, 2), new Ef("mining_fatigue", "Fiaca minera", 8, 15, 0),
			new Ef("nausea", "Mareo", 6, 12, 0), new Ef("blindness", "Ceguera", 3, 6, 0),
			new Ef("darkness", "Oscuridad", 5, 10, 0), new Ef("hunger", "Hambre", 10, 25, 1),
			new Ef("weakness", "Debilidad", 10, 25, 0), new Ef("poison", "Veneno", 4, 7, 0),
			new Ef("glowing", "Brillo de neón", 20, 60, 0), new Ef("unluck", "Mala suerte", 60, 180, 0),
			new Ef("levitation", "Levitación", 2, 4, 0));

	private static Premio efecto(boolean bueno) {
		Ef e = uno(bueno ? BUENOS : MALOS);
		int dur = entre(e.min(), e.max());
		int amp = entre(0, e.ampMax());
		String titulo = may(e.nombre()) + romano(amp + 1);
		return new Premio(titulo, "por " + dur + " segundos", bueno ? "green" : "red", bueno ? Clase.BUENO : Clase.MALO, (s, p, us) -> {
			cmd(s, "effect give @p minecraft:" + e.id() + " " + dur + " " + amp + " false");
			if (e.id().equals("levitation")) cmd(s, "effect give @p minecraft:slow_falling " + (dur + 10) + " 0 true");
		});
	}

	// =====================================================================
	// Ítems (regalos y regalos "de broma")
	// =====================================================================
	private record It(String id, int min, int max, String nombre, String custom, int peso) {}

	private static final List<It> ITEMS = List.of(
			new It("bread", 3, 8, "Pan", null, 6), new It("cooked_beef", 2, 6, "Bife", "Asadito de %u", 6),
			new It("golden_carrot", 2, 5, "Zanahoria dorada", null, 4), new It("cake", 1, 1, "Torta", "Torta de cumple de %u", 3),
			new It("cookie", 4, 12, "Galletitas", "Galletitas de %u", 5), new It("pumpkin_pie", 1, 3, "Tarta de calabaza", null, 4),
			new It("golden_apple", 1, 1, "Manzana dorada", "Manzana bendecida por %u", 2), new It("honey_bottle", 1, 2, "Miel", null, 3),
			new It("apple", 2, 5, "Manzanas", null, 5), new It("baked_potato", 3, 8, "Papas al horno", null, 5),
			new It("sweet_berries", 4, 12, "Bayas", null, 4), new It("glow_berries", 3, 8, "Bayas brillantes", null, 3),
			new It("mushroom_stew", 1, 1, "Guiso de hongos", "Guisito de la abuela de %u", 3), new It("rabbit_stew", 1, 1, "Guiso de conejo", null, 2),
			new It("torch", 8, 24, "Antorchas", null, 6), new It("arrow", 8, 24, "Flechas", null, 4),
			new It("iron_ingot", 2, 6, "Hierro", null, 5), new It("gold_ingot", 1, 4, "Oro", null, 4),
			new It("diamond", 1, 1, "Diamante", "Diamante de %u", 1), new It("emerald", 1, 3, "Esmeraldas", null, 3),
			new It("ender_pearl", 1, 3, "Perlas de ender", null, 3), new It("experience_bottle", 3, 8, "Botellas de XP", null, 3),
			new It("firework_rocket", 3, 8, "Cohetes", null, 3), new It("name_tag", 1, 1, "Etiqueta", "Etiqueta firmada por %u", 2),
			new It("saddle", 1, 1, "Montura", null, 2), new It("lead", 1, 2, "Correa", null, 2),
			new It("coal", 4, 12, "Carbón", null, 5), new It("redstone", 4, 12, "Redstone", null, 3),
			new It("lapis_lazuli", 4, 12, "Lapislázuli", null, 3), new It("bone_meal", 6, 16, "Polvo de hueso", null, 3),
			new It("slime_ball", 2, 4, "Bolas de slime", null, 2), new It("string", 3, 8, "Hilo", null, 3),
			new It("book", 1, 3, "Libros", null, 2), new It("bucket", 1, 1, "Balde", null, 2),
			new It("shield", 1, 1, "Escudo", "Escudo de %u", 2), new It("water_bucket", 1, 1, "Balde de agua", "Agua bendita de %u", 2),
			new It("totem_of_undying", 1, 1, "TÓTEM", "Tótem de %u", 1), new It("spyglass", 1, 1, "Catalejo", "Catalejo de chusma de %u", 2),
			new It("dirt", 1, 1, "Tierra premium", "Tierra premium de %u", 4), new It("stick", 1, 1, "Varita mágica", "Varita mágica de %u", 4),
			new It("poisonous_potato", 1, 1, "Papa sospechosa", "Papa sospechosa de %u", 4), new It("rotten_flesh", 3, 6, "Carne podrida", "Asado de %u (vencido)", 4),
			new It("dead_bush", 1, 1, "Ramo de flores", "Ramo de flores de %u", 3), new It("pufferfish", 1, 1, "Sushi", "Sushi de %u", 3),
			new It("bone", 1, 1, "Hueso", "Hueso de la suerte de %u", 3), new It("egg", 4, 16, "Huevos", "Huevos sorpresa de %u", 3),
			new It("wooden_sword", 1, 1, "Excalibur", "Excalibur de %u", 3), new It("wooden_shovel", 1, 1, "Pala legendaria", "Pala legendaria de %u", 3),
			new It("feather", 1, 1, "Pluma", "Pluma de la fama de %u", 3), new It("snowball", 8, 16, "Bolas de nieve", "Bolas de nieve del chat", 3),
			new It("carved_pumpkin", 1, 1, "Calabaza", "Casco oficial de %u", 3), new It("poppy", 1, 1, "Flor", "Flor de %u", 3),
			new It("paper", 1, 1, "Factura", "Factura de %u (impaga)", 3), new It("clock", 1, 1, "Reloj", "Reloj de %u", 2),
			new It("compass", 1, 1, "Brújula", "Brújula que apunta a %u", 2), new It("spider_eye", 1, 2, "Snack", "Snack de %u", 3),
			new It("cactus", 1, 1, "Cactus", "Abrazo de %u", 3), new It("sugar", 3, 8, "Azúcar", null, 2),
			new It("melon_slice", 4, 10, "Sandía", null, 3), new It("cod", 2, 4, "Pescado", "Pescado fresco de %u", 3),
			new It("goat_horn", 1, 1, "Cuerno", "Trompeta de %u", 1), new It("music_disc", 1, 1, "Disco", "Disco favorito de %u", 2),
			new It("slime_block", 1, 2, "Bloque de slime", null, 1), new It("hay_block", 1, 3, "Fardo", null, 2),
			new It("glowstone", 2, 6, "Piedra luminosa", null, 2), new It("amethyst_shard", 2, 6, "Amatistas", null, 2),
			new It("pink_wool", 1, 1, "Lana rosa", "Peluche de %u", 2), new It("bell", 1, 1, "Campana", "Campana de %u", 1),
			new It("brush", 1, 1, "Pincel", "Pincel de arqueólogo de %u", 1), new It("fishing_rod", 1, 1, "Caña", "Caña de pescar de %u", 2));

	private static final List<String> DISCOS = List.of("13", "cat", "blocks", "chirp", "far", "mall", "mellohi", "stal",
			"strad", "ward", "11", "wait", "otherside", "5", "pigstep", "relic", "creator", "creator_music_box", "precipice");

	private static It elegirItem() {
		int total = 0;
		for (It i : ITEMS) total += i.peso();
		int x = r().nextInt(total);
		for (It i : ITEMS) { x -= i.peso(); if (x < 0) return i; }
		return ITEMS.get(0);
	}

	private static Premio item(String us) {
		It it = elegirItem();
		int cant = entre(it.min(), it.max());
		String id = it.id().equals("music_disc") ? "music_disc_" + uno(DISCOS) : it.id();
		String nombre = it.custom() == null ? null : u(it.custom(), us);
		String titulo = (cant > 1 ? cant + " " : "") + may(it.nombre());
		String comps = "[minecraft:lore=[{text:\"Ruleta de " + us + "\",italic:false,color:\"gray\"}]"
				+ (nombre != null ? ",minecraft:custom_name={text:\"" + nombre + "\",italic:false,color:\"gold\"}" : "") + "]";
		return new Premio(titulo, nombre != null ? nombre : "regalo de " + us, "aqua", Clase.BUENO, (s, p, u2) -> {
			cmd(s, "give @p minecraft:" + id + comps + " " + cant);
			cmd(s, "execute at @p run playsound minecraft:entity.item.pickup player @p ~ ~ ~ 1 1");
		});
	}

	// =====================================================================
	// Mobs hostiles (solo dañan al jugador, no rompen nada, no sueltan nada)
	// =====================================================================
	private enum Lugar { SUELO, VUELA, AGUA, GRANDE }

	private record Mob(String id, String nombre, int peso, int min, int max, Lugar lugar, String extra) {}

	private static final String ZOMBIE = ",CanBreakDoors:0b";
	private static final String INMUNE = ",IsImmuneToZombification:1b";

	private static final List<Mob> MOBS = List.of(
			new Mob("zombie", "Zombie", 10, 1, 2, Lugar.SUELO, ZOMBIE),
			new Mob("zombie", "Zombie bebé", 5, 1, 2, Lugar.SUELO, ZOMBIE + ",IsBaby:1b"),
			new Mob("husk", "Momia", 6, 1, 2, Lugar.SUELO, ZOMBIE),
			new Mob("drowned", "Ahogado", 5, 1, 2, Lugar.SUELO, ZOMBIE),
			new Mob("zombie_villager", "Aldeano zombie", 4, 1, 1, Lugar.SUELO, ZOMBIE),
			new Mob("skeleton", "Esqueleto", 9, 1, 2, Lugar.SUELO, ""),
			new Mob("stray", "Esqueleto glacial", 4, 1, 1, Lugar.SUELO, ""),
			new Mob("bogged", "Esqueleto pantanoso", 4, 1, 1, Lugar.SUELO, ""),
			new Mob("spider", "Araña", 8, 1, 2, Lugar.SUELO, ""),
			new Mob("cave_spider", "Araña de cueva", 4, 1, 2, Lugar.SUELO, ""),
			new Mob("creeper", "Creeper", 6, 1, 1, Lugar.SUELO, ""),
			new Mob("witch", "Bruja", 4, 1, 1, Lugar.SUELO, ""),
			new Mob("pillager", "Saqueador", 4, 1, 2, Lugar.SUELO, ""),
			new Mob("vindicator", "Vindicador", 2, 1, 1, Lugar.SUELO, ""),
			new Mob("evoker", "Evocador", 2, 1, 1, Lugar.SUELO, ""),
			new Mob("illusioner", "Ilusionista", 1, 1, 1, Lugar.SUELO, ""),
			new Mob("phantom", "Phantom", 4, 1, 2, Lugar.VUELA, ""),
			new Mob("slime", "Slime", 5, 1, 3, Lugar.SUELO, ",Size:0"),
			new Mob("magma_cube", "Cubo de magma", 4, 1, 3, Lugar.SUELO, ",Size:0"),
			new Mob("endermite", "Endermite", 3, 1, 3, Lugar.SUELO, ""),
			new Mob("piglin", "Piglin", 3, 1, 2, Lugar.SUELO, INMUNE),
			new Mob("piglin_brute", "Piglin bruto", 1, 1, 1, Lugar.SUELO, INMUNE),
			new Mob("hoglin", "Hoglin", 2, 1, 1, Lugar.SUELO, INMUNE),
			new Mob("zoglin", "Zoglin", 1, 1, 1, Lugar.SUELO, ""),
			new Mob("zombified_piglin", "Piglin zombificado", 3, 1, 2, Lugar.SUELO, ""),
			new Mob("wither_skeleton", "Esqueleto wither", 2, 1, 1, Lugar.SUELO, ""),
			new Mob("guardian", "Guardián", 2, 1, 1, Lugar.AGUA, ""),
			new Mob("shulker", "Shulker", 1, 1, 1, Lugar.SUELO, ""),
			new Mob("ghast", "Ghast", 1, 1, 1, Lugar.GRANDE, ""),
			new Mob("breeze", "Breeze", 2, 1, 1, Lugar.SUELO, ""),
			new Mob("creaking", "Creaking", 2, 1, 1, Lugar.SUELO, ""),
			new Mob("vex", "Vex", 2, 1, 3, Lugar.VUELA, ""),
			new Mob("spider", "Jinete araña", 2, 1, 1, Lugar.SUELO,
					",Passengers:[{id:\"minecraft:skeleton\"," + TAGR + "," + SIN + "}]"),
			new Mob("chicken", "Pollito jockey", 2, 1, 1, Lugar.SUELO,
					",EggLayTime:2147483647,Passengers:[{id:\"minecraft:zombie\",IsBaby:1b,CanBreakDoors:0b," + TAGR + "," + SIN + "}]"));

	private static Mob elegirMob() {
		int total = 0;
		for (Mob m : MOBS) total += m.peso();
		int x = r().nextInt(total);
		for (Mob m : MOBS) { x -= m.peso(); if (x < 0) return m; }
		return MOBS.get(0);
	}

	private static Premio hostil(ServerPlayer p, String us) {
		Mob m = elegirMob();
		ServerLevel lvl = (ServerLevel) p.level();
		if (m.lugar() == Lugar.AGUA && buscarAgua(lvl, p.blockPosition()) == null) return null;
		if (m.lugar() == Lugar.GRANDE && lugarGrande(lvl, p) == null) return null;
		int cant = entre(m.min(), m.max());
		String titulo = may(m.nombre()) + (cant > 1 ? " x" + cant : "");
		return new Premio(titulo, "mandado por " + us, "red", Clase.HOSTIL, (s, pl, u2) -> {
			ServerLevel l = (ServerLevel) pl.level();
			for (int i = 0; i < cant; i++) {
				BlockPos pos = switch (m.lugar()) {
					case VUELA -> aire(l, pl);
					case AGUA -> buscarAgua(l, pl.blockPosition());
					case GRANDE -> lugarGrande(l, pl);
					default -> suelo(l, pl, 4, 7);
				};
				if (pos == null) pos = suelo(l, pl, 2, 4);
				if (pos == null) pos = pl.blockPosition();
				invocar(s, pos, m.id(), m.extra(), us, "red");
			}
			Registro.info("RULETA", "Hostil: " + m.id() + " x" + cant + " para " + us);
		});
	}

	/** Invoca con tags, sin botín y sin levantar ítems; marca también jinetes y monturas que aparezcan solos. */
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

	// ---------- Lugares seguros ----------
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
			for (int dy = 2; dy >= -3; dy--) {
				BlockPos pos = new BlockPos(x, y0 + dy, z);
				if (libre(l, pos) && libre(l, pos.above()) && piso(l, pos)) return pos;
			}
		}
		return null;
	}

	private static BlockPos aire(ServerLevel l, ServerPlayer p) {
		for (int i = 0; i < 12; i++) {
			BlockPos pos = p.blockPosition().offset(entre(-3, 3), entre(4, 7), entre(-3, 3));
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
	// Animal random (usa la mascota con caída dorada)
	// =====================================================================
	private static Premio animal(String us) {
		return new Premio("ANIMAL RANDOM", "regalo de " + us, "aqua", Clase.BUENO,
				(s, p, u2) -> Mascota.crear(s, p, "aleatorio", us, us));
	}

	// =====================================================================
	// Especiales
	// =====================================================================
	private static Premio especial(ServerPlayer p, String us) {
		return switch (r().nextInt(24)) {
			case 0 -> new Premio("TAMAÑO RANDOM", "cortesía de " + us, "light_purple", Clase.NEUTRO, (s, pl, u2) -> Tamano.aplicar(s, pl, us));
			case 1 -> new Premio("¡TE LLEVÓ UN GLOBO!", "agarrate fuerte", "light_purple", Clase.NEUTRO, (s, pl, u2) -> {
				cmd(s, "effect give @p minecraft:levitation 3 1 true");
				cmd(s, "effect give @p minecraft:slow_falling 15 0 true");
				for (int i = 0; i < 6; i++) Ruleta.despues(i * 10, sv -> cmd(sv, "execute at @p run particle minecraft:dust{color:[1.0,0.3,0.4],scale:2.0} ~ ~2.5 ~ 0.4 0.4 0.4 0 10 force"));
			});
			case 2 -> new Premio("¡MIRÁ PARA ALLÁ!", "180° cortesía de " + us, "yellow", Clase.NEUTRO, (s, pl, u2) -> {
				cmd(s, "execute as @p at @s run tp @s ~ ~ ~ ~180 ~");
				cmd(s, "execute at @p run playsound minecraft:entity.player.attack.sweep master @p ~ ~ ~ 1 1");
			});
			case 3 -> new Premio("ENDERMAN BORRACHO", us + " te teletransportó", "dark_purple", Clase.NEUTRO, (s, pl, u2) -> {
				BlockPos pos = suelo((ServerLevel) pl.level(), pl, 5, 9);
				if (pos == null) { Krim.aviso(s, Krim.fuente(s), "El enderman se mareó y no te movió", "dark_purple"); return; }
				cmd(s, "execute at @p run particle minecraft:portal ~ ~1 ~ 0.5 1 0.5 0.5 60 force");
				cmd(s, "execute as @p at @s run tp @s " + (pos.getX() + 0.5) + " " + pos.getY() + " " + (pos.getZ() + 0.5));
				cmd(s, "execute at @p run playsound minecraft:entity.enderman.teleport master @p ~ ~ ~ 1 1");
				cmd(s, "execute at @p run particle minecraft:portal ~ ~1 ~ 0.5 1 0.5 0.5 60 force");
			});
			case 4 -> new Premio("FUEGOS ARTIFICIALES", "en honor a " + us, "gold", Clase.BUENO, (s, pl, u2) -> fuegos(6));
			case 5 -> new Premio("¡TNT!", "corré...", "red", Clase.NEUTRO, (s, pl, u2) -> {
				String m = "krim_t" + System.nanoTime();
				BlockPos pos = suelo((ServerLevel) pl.level(), pl, 2, 3);
				String donde = pos == null ? "~ ~ ~" : (pos.getX() + 0.5) + " " + pos.getY() + " " + (pos.getZ() + 0.5);
				cmd(s, "execute as @p at @s run summon minecraft:tnt " + donde + " {fuse:80,Tags:[\"" + Krim.TAG_RULETA + "\",\"" + m + "\"]}");
				Ruleta.despues(70, sv -> {
					cmd(sv, "execute at @e[tag=" + m + "] run particle minecraft:totem_of_undying ~ ~0.5 ~ 0.5 0.5 0.5 0.4 60 force");
					cmd(sv, "execute at @e[tag=" + m + "] run playsound minecraft:entity.firework_rocket.twinkle master @a ~ ~ ~ 1 1");
					cmd(sv, "kill @e[tag=" + m + "]");
					titulo(sv, "¡ERA DE MENTIRA!", "gold", "jajaja, saludos de " + us);
				});
			});
			case 6 -> new Premio("CREEPER DEL CHAT", "con el nombre de " + us, "green", Clase.HOSTIL, (s, pl, u2) -> Krim.creeper(s, pl, us, us));
			case 7 -> r().nextBoolean()
					? new Premio("¡QUE LLUEVA!", us + " trajo la lluvia", "blue", Clase.NEUTRO, (s, pl, u2) -> cmd(s, "weather rain 1200"))
					: new Premio("SOLCITO", us + " corrió las nubes", "yellow", Clase.NEUTRO, (s, pl, u2) -> cmd(s, "weather clear 6000"));
			case 8 -> {
				int xp = entre(20, 90);
				yield new Premio("+" + xp + " DE EXPERIENCIA", "regalo de " + us, "green", Clase.BUENO, (s, pl, u2) -> cmd(s, "xp add @p " + xp + " points"));
			}
			case 9 -> new Premio("CURACIÓN TOTAL", us + " te cuida", "green", Clase.BUENO, (s, pl, u2) -> {
				cmd(s, "effect give @p minecraft:instant_health 1 3 true");
				cmd(s, "effect give @p minecraft:saturation 2 1 true");
				cmd(s, "execute at @p run particle minecraft:heart ~ ~1.5 ~ 0.6 0.6 0.6 0 12 force");
			});
			case 10 -> new Premio("ÚLTIMO MOMENTO", "mirá el chat del juego", "yellow", Clase.NEUTRO, (s, pl, u2) -> {
				String yo = pl.getName().getString();
				String como = uno(List.of(" fue volado por ", " fue asesinado por ", " se cayó por culpa de ", " fue humillado por "));
				cmd(s, "tellraw @a {text:\"" + yo + como + us + "\"}");
				Ruleta.despues(80, sv -> cmd(sv, "tellraw @a {text:\"(mentira, sigue vivo)\",color:\"gray\",italic:true}"));
			});
			case 11 -> new Premio("ALGUIEN ENTRÓ...", "mirá el chat del juego", "dark_gray", Clase.NEUTRO, (s, pl, u2) -> {
				cmd(s, "tellraw @a {text:\"Herobrine se unió a la partida\",color:\"yellow\"}");
				cmd(s, "execute at @p run playsound minecraft:ambient.cave master @p ~ ~ ~ 1 0.6");
				cmd(s, "effect give @p minecraft:darkness 4 0 true");
				Ruleta.despues(140, sv -> cmd(sv, "tellraw @a {text:\"Herobrine abandonó la partida\",color:\"yellow\"}"));
			});
			case 12 -> lluvia("chicken", "gallinas", us, ",EggLayTime:2147483647");
			case 13 -> lluvia("rabbit", "conejos", us, "");
			case 14 -> lluvia(uno(List.of("pig", "sheep", "cow", "frog", "armadillo")), "animales", us, "");
			case 15 -> new Premio("DISCO LOCO", us + " puso música", "light_purple", Clase.NEUTRO, (s, pl, u2) -> {
				String disco = uno(List.of("pigstep", "otherside", "cat", "chirp", "blocks", "creator", "relic", "precipice"));
				cmd(s, "execute at @p run playsound minecraft:music_disc." + disco + " record @p ~ ~ ~ 1 1");
				for (int i = 0; i < 40; i++) Ruleta.despues(i * 10, sv -> cmd(sv, "execute at @p run particle minecraft:note ~ ~2.2 ~ 1.5 0.5 1.5 1 3 force"));
				Ruleta.despues(400, sv -> cmd(sv, "stopsound @p record"));
			});
			case 16 -> new Premio("PAPARAZZI", us + " te está sacando fotos", "white", Clase.NEUTRO, (s, pl, u2) -> {
				for (int i = 0; i < 10; i++) Ruleta.despues(i * 8 + r().nextInt(4), sv -> {
					cmd(sv, "execute at @p run particle minecraft:end_rod ^ ^1.6 ^1.5 0.3 0.3 0.3 0.15 25 force");
					cmd(sv, "execute at @p run playsound minecraft:block.note_block.hat master @p ~ ~ ~ 1 2");
				});
			});
			case 17 -> new Premio("MODO FANTASMA", "30 segundos invisible y brillando", "aqua", Clase.NEUTRO, (s, pl, u2) -> {
				cmd(s, "effect give @p minecraft:invisibility 30 0 false");
				cmd(s, "effect give @p minecraft:glowing 30 0 false");
			});
			case 18 -> new Premio("MODO FLASH", "15 segundos", "yellow", Clase.BUENO, (s, pl, u2) -> cmd(s, "effect give @p minecraft:speed 15 4 false"));
			case 19 -> new Premio("MODO TORTUGA", "10 segundos", "red", Clase.MALO, (s, pl, u2) -> cmd(s, "effect give @p minecraft:slowness 10 3 false"));
			case 20 -> new Premio("MODO CANGURO", "15 segundos", "green", Clase.NEUTRO, (s, pl, u2) -> {
				cmd(s, "effect give @p minecraft:jump_boost 15 4 false");
				cmd(s, "effect give @p minecraft:slow_falling 25 0 true");
			});
			case 21 -> new Premio("CONFETI", "felicitaciones de " + us, "gold", Clase.BUENO, (s, pl, u2) -> {
				for (int i = 0; i < 5; i++) Ruleta.despues(i * 6, sv -> {
					cmd(sv, "execute at @p run particle minecraft:totem_of_undying ~ ~1.5 ~ 1 1 1 0.4 40 force");
					cmd(sv, "execute at @p run playsound minecraft:entity.firework_rocket.twinkle master @p ~ ~ ~ 1 1.2");
				});
			});
			case 22 -> new Premio("¡GIRÁ OTRA VEZ!", "la ruleta te da otra chance", "gold", Clase.NEUTRO, (s, pl, u2) -> Ruleta.otraVez(us));
			default -> new Premio("ESTRELLA DE ROCK", "brillás 30 segundos", "gold", Clase.NEUTRO, (s, pl, u2) -> {
				cmd(s, "effect give @p minecraft:glowing 30 0 false");
				cmd(s, "effect give @p minecraft:speed 30 0 false");
				cmd(s, "execute at @p run particle minecraft:wax_on ~ ~1 ~ 0.5 1 0.5 0 30 force");
			});
		};
	}

	/** Llueven animales del cielo (sin daño por caída) y a los 20 s desaparecen en un "puf". */
	private static Premio lluvia(String id, String plural, String us, String extra) {
		return new Premio("¡LLUVIA DE " + may(plural) + "!", "cortesía de " + us, "aqua", Clase.NEUTRO, (s, pl, u2) -> {
			String m = "krim_ll" + System.nanoTime();
			for (int i = 0; i < 8; i++) {
				String nbt = "{Tags:[\"" + Krim.TAG_RULETA + "\",\"" + m + "\"]," + SIN + extra + "}";
				Ruleta.despues(i * 4, sv -> cmd(sv, "execute at @p run summon minecraft:" + id + " ~" + entre(-4, 4) + " ~7 ~" + entre(-4, 4) + " " + nbt));
			}
			Ruleta.despues(34, sv -> cmd(sv, "effect give @e[tag=" + m + "] minecraft:slow_falling 15 0 true"));
			Ruleta.despues(400, sv -> {
				cmd(sv, "execute at @e[tag=" + m + "] run particle minecraft:poof ~ ~0.5 ~ 0.3 0.3 0.3 0.05 10 force");
				cmd(sv, "tp @e[tag=" + m + "] ~ -1000 ~");
				cmd(sv, "kill @e[tag=" + m + "]");
			});
		});
	}

	static void fuegos(int cuantos) {
		String[] formas = { "large_ball", "star", "burst", "small_ball", "creeper" };
		for (int i = 0; i < cuantos; i++) {
			String forma = formas[r().nextInt(formas.length)];
			int c1 = r().nextInt(0xFFFFFF), c2 = r().nextInt(0xFFFFFF);
			String nbt = "{LifeTime:" + entre(18, 28) + ",Tags:[\"" + Krim.TAG_RULETA + "\"],FireworksItem:{id:\"minecraft:firework_rocket\",count:1,"
					+ "components:{\"minecraft:fireworks\":{flight_duration:1b,explosions:[{shape:\"" + forma + "\",colors:[I;" + c1 + "," + c2
					+ "],has_twinkle:1b,has_trail:1b}]}}}}";
			Ruleta.despues(i * 8, sv -> cmd(sv, "execute at @p run summon minecraft:firework_rocket ~" + entre(-5, 5) + " ~12 ~" + entre(-5, 5) + " " + nbt));
		}
	}

	private static void titulo(MinecraftServer s, String t, String color, String sub) {
		cmd(s, "title @p times 5 40 10");
		cmd(s, "title @p subtitle {text:\"" + sub + "\",color:\"gray\"}");
		cmd(s, "title @p title {text:\"" + t + "\",color:\"" + color + "\",bold:true}");
	}

	// =====================================================================
	// Doble, jackpot, nada
	// =====================================================================
	private static Premio doble(MinecraftServer s, ServerPlayer p, String us, boolean tregua, boolean hostilesOk) {
		Premio a = null, b = null;
		for (int i = 0; i < 20 && (a == null || b == null); i++) {
			Premio x = generar(s, p, us, tregua, hostilesOk, false);
			if (x == null || !permitido(x, tregua, hostilesOk)) continue;
			if (a == null) a = x; else b = x;
		}
		if (a == null || b == null) return null;
		Premio pa = a, pb = b;
		Clase peor = (pa.clase() == Clase.HOSTIL || pb.clase() == Clase.HOSTIL) ? Clase.HOSTIL
				: (pa.clase() == Clase.MALO || pb.clase() == Clase.MALO) ? Clase.MALO : Clase.BUENO;
		return new Premio("¡DOBLE PREMIO!", pa.titulo() + " + " + pb.titulo(), "gold", peor, (sv, pl, u2) -> {
			pa.accion().hacer(sv, pl, u2);
			Ruleta.despues(40, s2 -> {
				ServerPlayer p2 = Ruleta.jugador(s2);
				if (p2 != null) pb.accion().hacer(s2, p2, u2);
			});
		});
	}

	private static Premio jackpot(String us) {
		return new Premio("¡¡JACKPOT!!", us + " te hizo millonario", "gold", Clase.BUENO, (s, p, u2) -> {
			String lore = "[minecraft:lore=[{text:\"JACKPOT de " + us + "\",italic:false,color:\"gold\"}]]";
			cmd(s, "give @p minecraft:diamond" + lore + " 3");
			cmd(s, "give @p minecraft:golden_apple" + lore + " 2");
			cmd(s, "give @p minecraft:emerald" + lore + " 5");
			cmd(s, "execute at @p run playsound minecraft:ui.toast.challenge_complete master @p ~ ~ ~ 1 1");
			fuegos(10);
		});
	}

	private static Premio nada(String us) {
		return new Premio("NADA", "gracias por participar, " + us, "gray", Clase.NEUTRO, NADA);
	}

	// =====================================================================
	// Nombres para la animación de la ruleta
	// =====================================================================
	static final List<String> NOMBRES_GIRO;
	static {
		List<String> l = new ArrayList<>();
		for (Mob m : MOBS) l.add(may(m.nombre()));
		for (It i : ITEMS) l.add(may(i.nombre()));
		for (Ef e : BUENOS) l.add(may(e.nombre()));
		for (Ef e : MALOS) l.add(may(e.nombre()));
		l.addAll(List.of("¡¡JACKPOT!!", "NADA", "PUTO EL QUE LEE", "TAMAÑO RANDOM", "¡TNT!", "ENDERMAN BORRACHO",
				"DISCO LOCO", "MODO FANTASMA", "MODO FLASH", "ANIMAL RANDOM", "CREEPER DEL CHAT", "¡DOBLE PREMIO!",
				"CONFETI", "PAPARAZZI", "¡LLUVIA DE GALLINAS!", "GHAST", "TÓTEM", "DIAMANTE"));
		NOMBRES_GIRO = List.copyOf(l);
	}
}
