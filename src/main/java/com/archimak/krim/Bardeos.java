package com.archimak.krim;

import java.util.ArrayDeque;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Bardeos al que canjeó: frases fijas + plantillas que se combinan (miles de variantes).
 * Puteadas fuertes de barrio. Lo único que no se usa: insultos por discapacidad, orientación u origen.
 * No repite ninguno de los últimos 40.
 */
public final class Bardeos {
	private static final List<String> FIJOS = List.of(
			"%u es un pelotudo de mierda", "%u es un forro de primera", "%u es un sorete con patas", "%u, andá a cagar",
			"%u tiene un palo metido en el orto", "%u es más inútil que un cenicero en una moto", "%u es un boludo atómico",
			"la concha de tu madre, %u", "%u es tan forro que lo echaron del grupo de la familia", "%u, sos un desperdicio de oxígeno",
			"%u es un pajero de mierda y lo sabe todo el chat", "%u juega como el orto y encima canjea", "%u, chupame un huevo",
			"%u es un hijo de puta con suerte de mierda", "%u, sos tan feo que tu vieja te daba la teta de espaldas",
			"%u es un cagón que llora con los creepers", "%u es el error más grande de sus viejos", "%u tiene el cerebro de un sorete seco",
			"%u, tomatela y no vuelvas", "%u es un rata que no regala ni la hora", "%u es un salame de mierda con WiFi",
			"%u nació por una apuesta perdida", "%u es tan pelotudo que se perdió en un pasillo", "%u, metete los puntos en el orto",
			"%u es un forro que pide permiso para respirar", "%u tiene menos luces que un pozo de mina", "%u es la cagada del barrio",
			"a %u lo parieron por el orto y se nota", "%u es un mamerto de manual", "%u, sos el hijo que nadie pidió",
			"%u es un gil de goma", "%u huele a culo de zombie", "%u es un tarado con micrófono", "%u es más pajero que un mono en un zoológico",
			"%u, ojalá te explote un creeper en la jeta", "%u es un forro y su perro también", "%u es tan inútil que el creeper le tuvo lástima",
			"%u es un pelotudo con premio", "%u se caga encima cuando oscurece", "%u, sos un sorete flotando en la pileta del chat",
			"%u tiene la personalidad de un ladrillo mojado", "%u es un boludo profesional con título", "%u es la vergüenza de su árbol genealógico",
			"%u, ni tu vieja te banca", "%u es un forrazo de proporciones bíblicas", "%u es un pelotudo que se cree vivo",
			"%u canjeó esto con la plata del almuerzo, pobre gil", "%u, cerrá el orto un rato", "%u tiene la cara de un culo con bigote",
			"%u es un mocoso de mierda con ínfulas", "%u es más malo que pisar un Lego descalzo", "%u, chupala", "%u es un pancho sin salchicha",
			"%u es un pelotudo nivel dios", "%u es un forro que aplaude cuando aterriza el avión", "a %u le chupa un huevo todo y se le nota en la cara",
			"%u se chupa los dedos después de rascarse el culo", "%u es un sorete de dos patas y medio cerebro", "%u, sos una cagada envuelta en papel de regalo",
			"%u es tan pelotudo que le pegó a un creeper con la mano", "%u, la próxima canjeá un poco de dignidad", "%u es un hijo de puta pero hijo de puta",
			"%u es un boludo con más suerte que habilidad", "%u se tira pedos y se los huele orgulloso", "%u tiene olor a pata desde 2015",
			"%u, ni el warden te quiere escuchar", "%u es un forro de los que ya no se fabrican");

	private static final List<String> ADJ = List.of("pelotudo", "forro", "boludo", "inútil", "pajero", "cagón", "sorete",
			"tarado", "gil", "salame", "rata", "manco", "llorón", "chupamedias", "mamerto", "pancho", "otario", "garca", "pesado", "feo");
	private static final List<String> COMP = List.of("un sorete en una pileta", "la concha de la lora", "un perro con dos colas",
			"un creeper sin pólvora", "un lunes a la mañana",
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
			"y se cree un genio", "y pidió un reembolso al chat", "y se lo cuenta a todos en el asado", "y lo puso en su CV",
			"el muy pelotudo", "como el forro que es", "y encima se hace el vivo", "porque es un boludo de nacimiento");
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
			else if (x < 65) b = uno(List.of("%u es más ", "%u, sos más ", "la puta madre, %u es más ")) + uno(ADJ) + " que " + uno(COMP);
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
