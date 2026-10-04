package com.archimak.krim;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Botón de configuración de KRIM en Mod Menu. Si Mod Menu no está instalado, esta clase nunca se carga. */
public class KrimModMenu implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return KrimConfigPantalla::new;
	}
}
