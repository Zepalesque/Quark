package org.violetmoon.zeta.module;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

import net.minecraftforge.api.distmarker.Dist;
import org.apache.commons.lang3.text.WordUtils;
import org.violetmoon.zeta.util.ZetaSide;

/**
 * performs some common data-munging of the data straight off a ZetaLoadModule annotation
 */
public record TentativeModule(
	Class<? extends ZetaModule> clazz,
	Class<? extends ZetaModule> keyClass,

	ZetaCategory category,
	String displayName,
	String lowercaseName,
	String description,
	Set<String> antiOverlap,
	boolean enabledByDefault,

	boolean clientReplacement
) {
	@SuppressWarnings("unchecked")
	public static TentativeModule from(ZetaLoadModuleAnnotationData data, Function<String, ZetaCategory> categoryResolver) {
		Class<?> clazzUnchecked = data.clazz();
		if(!ZetaModule.class.isAssignableFrom(clazzUnchecked))
			throw new RuntimeException("Class " + clazzUnchecked.getName() + " does not extend ZetaModule");
		Class<? extends ZetaModule> clazz = (Class<? extends ZetaModule>) clazzUnchecked;

		String displayName;
		if(data.name().isEmpty())
			displayName = WordUtils.capitalizeFully(clazz.getSimpleName().replaceAll("Module$", "").replaceAll("(?<=.)([A-Z])", " $1"));
		else
			displayName = data.name();
		String lowercaseName = displayName.toLowerCase(Locale.ROOT).replace(" ", "_");

		boolean clientReplacement = data.clientReplacement();

		Class<? extends ZetaModule> keyClass;
		if(clientReplacement) {
			Class<?> sup = clazz.getSuperclass();
			if(ZetaModule.class.isAssignableFrom(sup) && ZetaModule.class != sup)
				keyClass = (Class<? extends ZetaModule>) clazz.getSuperclass();
			else
				throw new RuntimeException("Client extension module " + clazz.getName() + " should `extend` the module it's an extension of");
		} else {
			keyClass = clazz;

			//leaving the category out of the annotation is only valid for client replacements
			//TODO: maybe guess the category from the package name isntead ^^
			if(data.category() == null || data.category().isEmpty())
				throw new RuntimeException("Module " + clazz.getName() + " should specify a category");
		}

		return new TentativeModule(
			clazz,
			keyClass,
			categoryResolver.apply(data.category()),
			displayName,
			lowercaseName,
			data.description(),
			Set.of(data.antiOverlap()),
			data.enabledByDefault(),
			clientReplacement
		);
	}

	public TentativeModule replaceWith(TentativeModule replacement) {
		return new TentativeModule(
			replacement.clazz,
			this.keyClass,
			this.category,
			this.displayName,
			this.lowercaseName,
			this.description,
			this.antiOverlap,
			this.enabledByDefault,
			false
		);
	}

	public boolean appliesTo(ZetaSide side) {
		return switch(side) {
			case CLIENT -> true;
			case SERVER -> !clientReplacement;
		};
	}
}
