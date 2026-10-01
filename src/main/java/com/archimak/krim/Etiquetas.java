package com.archimak.krim;

import net.minecraft.world.entity.Entity;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * Lee los tags de comando de una entidad (los de /tag y NBT "Tags").
 * En 26.2 el método de Entity ya no se llama getTags(), así que se busca por reflexión
 * una vez y se guarda. krim.log registra qué nombre encontró (categoría TAGS),
 * para reemplazar esto por la llamada directa en una próxima versión.
 */
public final class Etiquetas {
	private static final String[] NOMBRES = { "entityTags", "getTags", "tags", "getEntityTags", "commandTags", "getCommandTags" };
	private static Method metodo;
	private static boolean buscado;

	static synchronized Method metodo() {
		if (buscado) return metodo;
		buscado = true;
		for (String n : NOMBRES) {
			try {
				Method m = Entity.class.getMethod(n);
				if (Set.class.isAssignableFrom(m.getReturnType())) { metodo = m; break; }
			} catch (NoSuchMethodException ignored) { }
		}
		if (metodo == null) {
			for (Method m : Entity.class.getMethods()) {
				if (m.getParameterCount() == 0 && Set.class.isAssignableFrom(m.getReturnType())
						&& m.getName().toLowerCase().contains("tag")) { metodo = m; break; }
			}
		}
		if (metodo != null) {
			Registro.info("TAGS", "Metodo de tags de Entity: " + metodo.getName() + "()");
		} else {
			StringBuilder sb = new StringBuilder();
			for (Method m : Entity.class.getMethods()) {
				if (m.getParameterCount() == 0 && Set.class.isAssignableFrom(m.getReturnType())) sb.append(m.getName()).append("() ");
			}
			Registro.error("TAGS", "No se encontro el metodo de tags. Candidatos que devuelven Set: " + sb, null);
		}
		return metodo;
	}

	public static boolean tiene(Entity e, String tag) {
		Method m = metodo();
		if (m == null || e == null) return false;
		try {
			return m.invoke(e) instanceof Set<?> s && s.contains(tag);
		} catch (Exception ex) {
			Registro.error("TAGS", "Fallo leyendo tags de " + e.getName().getString(), ex);
			return false;
		}
	}
}
