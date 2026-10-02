package com.archimak.krim;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Preguntas de cultura general y de Minecraft, estilo Kahoot: pantalla con 4 opciones de colores y tiempo.
 * Acierto: ¡CORRECTO! en pantalla. Error: caquita en el medio de la pantalla y a los 2 s un creeper "BURRO" (o similar).
 * Mientras la pregunta está abierta el jugador tiene resistencia total (no lo matan por estar leyendo).
 */
public final class Trivia {
	private static final int DURACION_MS = 15000;
	private static int contador;
	private static final ArrayDeque<String> RECIENTES = new ArrayDeque<>();

	/** "pregunta|correcta|incorrecta|incorrecta|incorrecta" */
	private static final List<String> MUNDO = List.of(
			"¿Cuál es el río más largo de Sudamérica?|Amazonas|Paraná|Orinoco|Río de la Plata",
			"¿Cuál es la capital de Australia?|Canberra|Sídney|Melbourne|Perth",
			"¿Qué planeta es conocido como el planeta rojo?|Marte|Júpiter|Venus|Mercurio",
			"¿Quién pintó la Mona Lisa?|Leonardo da Vinci|Miguel Ángel|Rafael|Van Gogh",
			"¿En qué año llegó el hombre a la Luna?|1969|1965|1972|1959",
			"¿Cuál es el océano más grande?|Pacífico|Atlántico|Índico|Ártico",
			"¿Cuál es el símbolo químico del oro?|Au|Ag|Or|Go",
			"¿Cuántos jugadores tiene un equipo de fútbol en la cancha?|11|10|9|12",
			"¿Quién ganó el Mundial de Qatar 2022?|Argentina|Francia|Brasil|Croacia",
			"¿Cuántos huesos tiene el cuerpo humano adulto?|206|186|226|300",
			"¿Cuál es el animal terrestre más rápido?|Guepardo|León|Antílope|Caballo",
			"¿En qué país están las pirámides de Giza?|Egipto|México|Perú|Grecia",
			"¿Qué idioma tiene más hablantes nativos?|Chino mandarín|Inglés|Español|Hindi",
			"¿Cuál es la capital de Canadá?|Ottawa|Toronto|Montreal|Vancouver",
			"¿Quién escribió Don Quijote de la Mancha?|Cervantes|Lope de Vega|Borges|García Márquez",
			"¿Qué gas usan las plantas para la fotosíntesis?|Dióxido de carbono|Oxígeno|Nitrógeno|Helio",
			"¿Cuántos lados tiene un hexágono?|6|5|7|8",
			"¿Cuál es la montaña más alta del mundo?|Everest|K2|Aconcagua|Kilimanjaro",
			"¿Cuál es la montaña más alta de América?|Aconcagua|Chimborazo|Denali|Huascarán",
			"¿Cuál es la capital de Japón?|Tokio|Osaka|Kioto|Pekín",
			"¿Quién escribió Cien años de soledad?|García Márquez|Cortázar|Vargas Llosa|Neruda",
			"¿En qué año empezó la Segunda Guerra Mundial?|1939|1914|1941|1945",
			"¿Cuál es el país más grande del mundo?|Rusia|Canadá|China|Estados Unidos",
			"¿Cuántos minutos tiene un día?|1440|1200|1600|1340",
			"¿Qué órgano bombea la sangre?|Corazón|Hígado|Pulmón|Riñón",
			"¿A qué velocidad aproximada viaja la luz?|300.000 km/s|150.000 km/s|30.000 km/s|1.000.000 km/s",
			"¿Quién fue el primer presidente de Estados Unidos?|George Washington|Abraham Lincoln|Thomas Jefferson|John Adams",
			"¿Cuál es el planeta más grande del sistema solar?|Júpiter|Saturno|Neptuno|Tierra",
			"¿Quién propuso la teoría de la relatividad?|Einstein|Newton|Galileo|Tesla",
			"¿Cuál es la capital de Brasil?|Brasilia|Río de Janeiro|San Pablo|Salvador",
			"¿Cuántas patas tiene una araña?|8|6|10|12",
			"¿En qué continente está Kenia?|África|Asia|Oceanía|América",
			"¿Qué instrumento tocaba Jimi Hendrix?|Guitarra|Batería|Piano|Saxofón",
			"¿Cuál es el elemento más abundante del universo?|Hidrógeno|Helio|Oxígeno|Carbono",
			"¿Quién es el máximo goleador de la selección argentina?|Messi|Batistuta|Maradona|Agüero",
			"¿Cuántos colores tiene el arcoíris?|7|6|8|5",
			"¿En qué ciudad está la Torre Eiffel?|París|Londres|Roma|Madrid",
			"¿Cuál es el desierto cálido más grande?|Sahara|Gobi|Atacama|Kalahari",
			"¿Qué vitamina produce la piel con el sol?|Vitamina D|Vitamina C|Vitamina A|Vitamina B12",
			"¿Cuál es el hueso más largo del cuerpo?|Fémur|Húmero|Tibia|Columna",
			"¿Quién compuso la Quinta Sinfonía?|Beethoven|Mozart|Bach|Chopin",
			"¿Qué país tiene forma de bota?|Italia|Chile|Portugal|Grecia",
			"¿Cuál es la moneda de Japón?|Yen|Yuan|Won|Rupia",
			"¿A qué temperatura hierve el agua a nivel del mar?|100 °C|90 °C|120 °C|80 °C",
			"¿Cuál es el animal más grande del planeta?|Ballena azul|Elefante|Tiburón ballena|Jirafa",
			"¿Cuál es la capital de Perú?|Lima|Cusco|Quito|Bogotá",
			"¿En qué año cayó el Muro de Berlín?|1989|1991|1985|1979",
			"¿Cuántos planetas tiene el sistema solar?|8|9|7|10",
			"¿Cuál es la capital de Uruguay?|Montevideo|Punta del Este|Colonia|Salto",
			"¿Qué selección ganó más Mundiales?|Brasil|Alemania|Italia|Argentina",
			"¿Cuántos corazones tiene un pulpo?|3|1|2|4",
			"¿Quién escribió Rayuela?|Julio Cortázar|Borges|Sabato|Bioy Casares",
			"¿Cuál es el planeta más cercano al Sol?|Mercurio|Venus|Marte|Tierra",
			"¿Dónde nacieron los Juegos Olímpicos?|Grecia|Italia|Egipto|China",
			"¿Cuál es la fórmula del agua?|H2O|CO2|O2|H2O2",
			"¿A quién le dicen el Rey del Pop?|Michael Jackson|Elvis Presley|Prince|Freddie Mercury",
			"¿Cuántos segundos tiene una hora?|3600|360|6000|1200",
			"¿Qué idioma se habla oficialmente en Brasil?|Portugués|Español|Brasileño|Inglés",
			"¿Qué pesa más: 1 kg de plumas o 1 kg de hierro?|Pesan lo mismo|El hierro|Las plumas|Depende",
			"¿En qué año fue la Revolución de Mayo?|1810|1816|1806|1853",
			"¿Qué animal es símbolo de Australia?|Canguro|Koala gigante|Llama|Panda",
			"¿Cuál es el país con más habitantes?|India|China|Estados Unidos|Indonesia",
			"¿Qué inventó Graham Bell?|El teléfono|La radio|La bombilla|La televisión",
			"¿Cuántos años tiene un siglo?|100|1000|50|10",
			"¿En qué año se declaró la independencia argentina?|1816|1810|1820|1853",
			"¿Cuál es el metal más abundante en la corteza terrestre?|Aluminio|Hierro|Cobre|Oro",
			"¿Cuál es el continente más grande?|Asia|África|América|Europa",
			"¿Qué planeta tiene los anillos más famosos?|Saturno|Urano|Júpiter|Neptuno");

	private static final List<String> MINECRAFT = List.of(
			"¿Cuánta obsidiana mínima lleva un portal al Nether?|10|14|8|12",
			"¿Qué mob explota cuando se te acerca?|Creeper|Zombie|Esqueleto|Araña",
			"¿Con qué se domestica un lobo?|Huesos|Carne|Pescado|Trigo",
			"¿Con qué se domestica un gato?|Pescado crudo|Leche|Huesos|Semillas",
			"¿Qué pico mínimo necesitás para picar diamante?|Pico de hierro|Pico de piedra|Pico de oro|Pico de madera",
			"¿Qué pico necesitás para picar obsidiana?|Pico de diamante|Pico de hierro|Pico de oro|Pico de piedra",
			"¿Qué mob suelta perlas de ender?|Enderman|Shulker|Blaze|Ghast",
			"¿Cómo se llama el jefe del End?|Ender Dragon|Wither|Warden|Elder Guardian",
			"¿Qué se necesita, además de obsidiana y diamantes, para la mesa de encantamientos?|Un libro|Lana|Oro|Esmeralda",
			"¿Qué mob camina sobre la lava?|Strider|Ghast|Blaze|Hoglin",
			"¿Con qué se reproducen las vacas?|Trigo|Zanahorias|Semillas|Papas",
			"¿Cuántos corazones de vida tiene el jugador?|10|20|8|12",
			"¿Qué mob suelta pólvora?|Creeper|Zombie|Araña|Esqueleto",
			"¿De qué color es el polvo de redstone?|Rojo|Azul|Verde|Naranja",
			"¿Cuál es el material de herramientas más fuerte?|Netherite|Diamante|Hierro|Oro",
			"¿Qué mob te roba bloques?|Enderman|Zombie|Aldeano|Creeper",
			"¿En qué dimensión viven los shulkers?|El End|El Nether|El Overworld|Las cuevas",
			"¿Qué se necesita para invocar al Wither?|Cráneos de esqueleto wither|Cabezas de creeper|Calabazas|Estrellas del Nether",
			"¿Cómo se crea un golem de hierro?|Bloques de hierro y una calabaza|Lingotes y una sandía|Hierro y un cofre|Yunques",
			"¿Qué ítem te salva de morir una vez?|Tótem de la inmortalidad|Manzana dorada|Estrella del Nether|Ojo de ender",
			"¿Qué aldeano vende libros encantados?|Bibliotecario|Herrero|Clérigo|Granjero",
			"¿Qué mob puede soltar un tridente?|Ahogado|Guardián|Calamar|Delfín",
			"¿Cuántos ojos de ender activan un portal del End?|12|8|10|16",
			"¿Qué pasa si dormís en el Nether?|La cama explota|Se hace de día|No pasa nada|Aparecés en el Overworld",
			"¿Quién creó Minecraft?|Notch|Jeb|Herobrine|Dinnerbone",
			"¿En qué año salió Minecraft 1.0?|2011|2009|2013|2010",
			"¿Qué empresa compró Mojang?|Microsoft|Sony|Google|EA",
			"¿En qué se convierte un aldeano si le cae un rayo?|Bruja|Zombie|Ilusionista|Golem",
			"¿Qué mob se zombifica si pasa al Overworld?|Piglin|Ghast|Blaze|Strider",
			"¿Qué hace la leche de vaca?|Quita los efectos|Cura 4 corazones|Da velocidad|Nada",
			"¿A qué animal le tienen miedo los creepers?|Gatos|Lobos|Vacas|Gallinas",
			"¿De qué mob huyen los esqueletos?|Lobos|Gatos|Aldeanos|Cerdos",
			"¿Cuántos lingotes forman un bloque de hierro?|9|4|8|6",
			"¿En qué bioma viven las champivacas?|Isla de champiñones|Pantano|Jungla|Taiga",
			"¿Qué criatura ciega vive en la Ciudad Antigua?|Warden|Creaking|Enderman|Ghast",
			"¿Qué mob se enoja si lo mirás a los ojos?|Enderman|Creeper|Zombie|Aldeano",
			"¿Con qué se carga un ancla de reaparición?|Piedra luminosa|Redstone|Lava|Carbón",
			"¿Qué mob suelta varas de blaze?|Blaze|Ghast|Wither|Magma",
			"¿Qué bloque se usa para hacer pociones?|Soporte para pociones|Caldero|Horno|Mesa de crafteo",
			"¿Cuántos espacios tiene la barra rápida?|9|10|8|12",
			"¿Cómo se llama el personaje por defecto?|Steve|Alex|Herobrine|Notch",
			"¿Qué mob pone huevos?|Gallina|Pato|Loro|Tortuga bebé",
			"¿Qué bloque atrae a los rayos?|Pararrayos|Bloque de hierro|Antena|Cobre",
			"¿Qué se necesita para hacer una brújula?|Hierro y redstone|Oro y redstone|Hierro y lapislázuli|Cobre y redstone",
			"¿Qué mob se puede montar en el Nether caminando sobre lava?|Strider|Hoglin|Piglin|Ghast",
			"¿Qué se obtiene al esquilar una oveja?|Lana|Cuero|Hilo|Plumas",
			"¿Qué mob suelta lágrimas de ghast?|Ghast|Blaze|Wither|Vex",
			"¿Cuál de estos NO es un bioma?|Desierto de hielo rosa|Badlands|Pantano de manglares|Bosque de cerezos");

	/** La pregunta especial: 1 en 100. Todas las respuestas que dicen que sí son correctas. */
	private static final String ESPECIAL = "¿Clover es la más manca del chat?";
	private static final String[] ESPECIAL_OPC = { "Sí", "Obvio", "Clarísimo", "No" };

	static void iniciar() {
		ServerLifecycleEvents.SERVER_STARTED.register(s -> { TriviaEstado.actual = null; TriviaEstado.resultado = null; });
		ServerTickEvents.END_SERVER_TICK.register(Trivia::tick);
	}

	/** La ruleta espera mientras hay una pregunta abierta o se está mostrando el resultado. */
	static boolean activa() {
		TriviaEstado.Resultado r = TriviaEstado.resultado;
		return TriviaEstado.actual != null || (r != null && RuedaEstado.ahoraMs() - r.inicioMs() < 3500);
	}

	static void preguntar(MinecraftServer s, String usuario) {
		ThreadLocalRandom r = ThreadLocalRandom.current();
		String categoria, texto;
		String[] opciones;
		boolean[] correctas = new boolean[4];
		if (r.nextInt(100) == 0) {
			categoria = "PREGUNTA ESPECIAL";
			texto = ESPECIAL;
			opciones = ESPECIAL_OPC.clone();
			List<String> l = new ArrayList<>(List.of(opciones));
			Collections.shuffle(l);
			opciones = l.toArray(new String[0]);
			for (int i = 0; i < 4; i++) correctas[i] = !opciones[i].equals("No");
		} else {
			boolean mc = r.nextInt(100) < 45;
			List<String> banco = mc ? MINECRAFT : MUNDO;
			String linea = banco.get(r.nextInt(banco.size()));
			for (int i = 0; i < 10 && RECIENTES.contains(linea); i++) linea = banco.get(r.nextInt(banco.size()));
			RECIENTES.addLast(linea);
			while (RECIENTES.size() > 40) RECIENTES.removeFirst();
			String[] partes = linea.split("\\|");
			categoria = mc ? "CULTURA MINECRAFT" : "CULTURA GENERAL";
			texto = partes[0];
			List<String> l = new ArrayList<>(List.of(partes[1], partes[2], partes[3], partes[4]));
			Collections.shuffle(l);
			opciones = l.toArray(new String[0]);
			for (int i = 0; i < 4; i++) correctas[i] = opciones[i].equals(partes[1]);
		}
		TriviaEstado.respuesta = -2;
		TriviaEstado.resultado = null;
		TriviaEstado.actual = new TriviaEstado.Pregunta(++contador, categoria, texto, opciones, correctas,
				RuedaEstado.ahoraMs(), DURACION_MS, usuario);
		Krim.cmd(s, Krim.fuente(s), "effect give @p minecraft:resistance 17 4 true");
		Krim.cmd(s, Krim.fuente(s), "execute at @p run playsound minecraft:block.note_block.bell master @p ~ ~ ~ 1 1.2");
		Registro.info("TRIVIA", "Pregunta de " + usuario + ": " + texto);
	}

	private static void tick(MinecraftServer s) {
		TriviaEstado.Pregunta p = TriviaEstado.actual;
		if (p == null) return;
		int resp = TriviaEstado.respuesta;
		long el = RuedaEstado.ahoraMs() - p.inicioMs();
		if (resp == -2 && el < p.duracionMs() + 500) return;
		resolver(s, p, resp);
	}

	private static void resolver(MinecraftServer s, TriviaEstado.Pregunta p, int resp) {
		boolean ok = resp >= 0 && resp < 4 && p.correctas()[resp];
		String correcta = "";
		for (int i = 0; i < 4; i++) if (p.correctas()[i]) { correcta = p.opciones()[i]; break; }
		TriviaEstado.actual = null;
		TriviaEstado.resultado = new TriviaEstado.Resultado(ok, correcta, RuedaEstado.ahoraMs());
		Krim.cmd(s, Krim.fuente(s), "effect clear @p minecraft:resistance");
		String quien = resp == -2 ? "no respondió a tiempo" : resp == -1 ? "se rindió" : ok ? "acertó" : "le erró";
		Registro.info("TRIVIA", "El jugador " + quien + " (correcta: " + correcta + ")");
		Krim.cmd(s, Krim.fuente(s), "tellraw @a [{text:\"[Pregunta de " + p.usuario() + "] \",color:\"gold\"},{text:\""
				+ (ok ? "¡CORRECTO!" : "INCORRECTO") + "\",color:\"" + (ok ? "green" : "red") + "\",bold:true},{text:\" — era: "
				+ correcta.replace("\"", "'") + "\",color:\"gray\"}]");
		if (ok) {
			Krim.cmd(s, Krim.fuente(s), "execute at @p run playsound minecraft:ui.toast.challenge_complete master @p ~ ~ ~ 1 1");
			Krim.cmd(s, Krim.fuente(s), "execute at @p run particle minecraft:totem_of_undying ~ ~1.5 ~ 0.8 0.8 0.8 0.4 60 force");
		} else {
			Krim.cmd(s, Krim.fuente(s), "execute at @p run playsound minecraft:entity.slime.squish master @p ~ ~ ~ 1 0.5");
			Krim.cmd(s, Krim.fuente(s), "execute at @p run playsound minecraft:entity.villager.no master @p ~ ~ ~ 1 0.8");
			String nombre = List.of("BURRO", "IGNORANTE", "BURRÍSIMO", "NABO", "MANCO").get(ThreadLocalRandom.current().nextInt(5));
			Ruleta.despues(40, sv -> {
				ServerPlayer pl = Ruleta.jugador(sv);
				if (pl == null) return;
				BlockPos pos = Premios.suelo((ServerLevel) pl.level(), pl, 3, 5);
				if (pos == null) pos = pl.blockPosition();
				Premios.invocar(sv, pos, "creeper", "", nombre, "dark_red");
			});
		}
	}

	private Trivia() { }
}
