package com.archimak.krim;

import java.util.ArrayDeque;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Bardeos al que canjeó: frases fijas + plantillas que se combinan (miles de variantes).
 * Humor de barrio: sin insultos por orientación, discapacidad, origen ni nada de eso.
 * No repite ninguno de los últimos 40.
 */
public final class Bardeos {
	private static final List<String> FIJOS = List.of(
			"%u tiene un palo metido en el orto", "%u se baña una vez por mes y se hace el limpio",
			"%u le pide permiso a la mamá para canjear", "%u juega en pacífico y se muere igual",
			"%u todavía cree que Herobrine es real", "%u tiene la autoestima de un zombie bebé",
			"%u perdió una pelea contra una gallina", "%u gastó sus puntos en esto porque no tiene amigos",
			"%u come la pizza con cuchillo y tenedor", "%u le pone ketchup al asado", "%u le pone azúcar al mate y lo defiende",
			"%u se tira pedos en el colectivo y mira al de al lado", "a %u lo dejan en visto hasta los bots",
			"%u tiene menos onda que un cartel de CERRADO", "%u se quedó dormido en su propio cumpleaños",
			"%u usa medias con ojotas y se cree facha", "%u le debe plata al kiosquero desde 2019",
			"%u se peina con un tenedor", "%u le tiene miedo a las ovejas", "%u se mira al espejo y el espejo pide perdón",
			"%u llora con las publicidades de seguros", "%u se ríe de sus propios chistes y no son buenos",
			"%u tiene el cerebro en modo avión", "%u tiene más deudas que diamantes", "%u se cae de la cama y le echa la culpa al lag",
			"%u es la razón por la que el shampoo tiene instrucciones", "%u nació con lag", "%u es tan feo que el creeper explotó del susto",
			"%u se hace el misterioso pero no tiene ningún secreto", "%u perdió al ta-te-ti contra un aldeano",
			"%u tiene la billetera más vacía que tus cofres", "%u saluda a los NPC y espera respuesta",
			"%u pone el microondas en 1:11 porque le da paz", "%u se sacó un 2 en recreo", "%u se pierde en su propia casa",
			"%u tira la cadena antes de terminar", "%u se pone perfume para ir a sacar la basura", "%u le habla a las plantas y las plantas lo ignoran",
			"%u se comió el guisito de la abuela y le echó la culpa al perro", "%u googlea cómo hacer hielo",
			"%u se quedó encerrado en el baño de su casa", "%u canjea la ruleta y reza", "%u tiene el récord mundial de muertes por creeper",
			"%u se asusta con su propia sombra en Minecraft", "%u maneja como juega: mal", "%u todavía no aprendió a atarse los cordones",
			"%u se confundió de stream y se quedó por lástima", "a %u lo echaron de un grupo de WhatsApp de una sola persona",
			"%u tiene un palo metido en el orto y otro de repuesto", "%u huele a pata de creeper mojado",
			"%u se hace el que labura pero mira streams todo el día", "%u pone 'jajaja' sin reírse", "%u se cree pro y muere en el tutorial",
			"%u tiene más fe en la ruleta que en su vida", "%u se compró una cama gamer para dormir mejor y sigue manco",
			"%u le grita al juego como si lo escuchara", "%u le pide perdón a los mobs antes de matarlos",
			"%u se olvidó de respirar mientras leía esto", "%u tiene un hámster de mascota y el hámster lo mantiene",
			"%u le cuenta sus problemas al aldeano", "%u se enamoró de una vaca de Minecraft", "%u todavía no sabe para qué sirve la mesa de crafteo",
			"%u se ahoga en un balde de agua", "%u le tiene miedo a la oscuridad y a la luz", "%u se pone nervioso en el chat de su propia familia",
			"%u es el único que se perdió en un mundo plano", "%u se comió la galleta de la fortuna y le salió 'no'",
			"%u tiene el mismo nivel de habilidad que una papa", "%u no sabe si es lunes o domingo hace tres semanas",
			"%u le dice 'amigo' al repartidor", "%u se ofende con los memes de él mismo", "%u canjeó esto y se arrepintió en el acto",
			"a %u le dicen 'el de los puntos' porque es lo único que aporta", "%u es fan de sí mismo y es el único",
			"%u pide 'una más y me voy' desde 2020", "%u se cree el protagonista y es un extra", "%u se escapó de un creeper caminando para atrás",
			"%u tiene la paciencia de un creeper con hipo");

	private static final List<String> ADJ = List.of("lento", "inútil", "aburrido", "manco", "pesado", "tacaño", "perdido", "raro",
			"malo", "torpe", "olvidadizo", "cagón", "dormido", "denso", "desorganizado", "llorón", "quejoso", "distraído",
			"gruñón", "ruidoso");
	private static final List<String> COMP = List.of("una tortuga con sueño", "un creeper sin pólvora", "un lunes a la mañana",
			"el WiFi de la abuela", "una gallina en el Nether", "un zombie bebé", "un aldeano desempleado", "un colectivo en hora pico",
			"el chiste de un tío", "una cama en el Nether", "un pico de madera", "un esqueleto sin arco", "un enderman bajo la lluvia",
			"un caracol con resaca", "una fila en el banco");
	private static final List<String> ACCION = List.of("intentó domar un creeper", "se metió a nadar en lava", "quiso tradear con un zombie",
			"se puso a minar para abajo", "le pegó a un golem de hierro", "trató de dormir de día", "se olvidó dónde dejó su casa",
			"quiso hacer un portal con tierra", "le tiró un huevo a un warden", "se comió una papa venenosa a propósito",
			"saltó al vacío del End", "le robó la cama a un aldeano", "se peleó con una abeja", "construyó una casa de arena sin techo",
			"siguió a un enderman a casa", "le prendió fuego a su propia casa", "se tiró de un árbol para ver qué pasaba",
			"le quiso poner montura a una gallina");
	private static final List<String> CONSEC = List.of("y todavía no entiende qué pasó", "y lo publicó en sus historias",
			"y se lo contó orgulloso a la familia", "y lloró en vivo", "y le echó la culpa al chat", "y dijo que era estrategia",
			"y lo volvería a hacer", "y perdió todo el inventario", "y ahora vive en un pozo", "y el creeper se rió de él",
			"y se fue a dormir enojado", "y pidió ayuda a la abuela", "y lo filmó en vertical", "y desde ese día no es el mismo",
			"y se cree un genio", "y pidió un reembolso al chat", "y se lo cuenta a todos en el asado", "y lo puso en su CV");
	private static final List<String> COSA = List.of("un palo", "un pico de madera", "un cactus", "una antorcha", "un creeper chiquito",
			"un bloque de tierra", "una caña de pescar", "un pan entero", "una espada de oro", "un balde de lava", "una pala");
	private static final List<String> LUGAR = List.of("en el orto", "en la cola", "en el culo");
	private static final List<String> REMATE = List.of("y no se lo saca ni con tótem", "y dice que es un accesorio",
			"y lo usa de antena para el WiFi", "y anda sonriendo igual", "y le cobra entrada a los que miran", "desde 2017",
			"y no le molesta", "y lo pide en cada cumpleaños", "y lo muestra en las juntadas", "y le puso nombre",
			"y dice que es por salud", "y lo heredó del abuelo");

	private static final ArrayDeque<String> RECIENTES = new ArrayDeque<>();

	private static ThreadLocalRandom r() { return ThreadLocalRandom.current(); }
	private static String uno(List<String> l) { return l.get(r().nextInt(l.size())); }

	/** Un bardeo nuevo para el usuario (no repite los últimos 40). */
	static String generar(String usuario) {
		String b = "";
		for (int i = 0; i < 20; i++) {
			int x = r().nextInt(100);
			if (x < 45) b = uno(FIJOS);
			else if (x < 65) b = "%u es más " + uno(ADJ) + " que " + uno(COMP);
			else if (x < 85) b = "%u " + uno(ACCION) + " " + uno(CONSEC);
			else b = "%u tiene " + uno(COSA) + " metido " + uno(LUGAR) + " " + uno(REMATE);
			if (!RECIENTES.contains(b)) break;
		}
		RECIENTES.addLast(b);
		while (RECIENTES.size() > 40) RECIENTES.removeFirst();
		String t = b.replace("%u", usuario);
		return Character.toUpperCase(t.charAt(0)) + t.substring(1);
	}

	private Bardeos() { }
}
