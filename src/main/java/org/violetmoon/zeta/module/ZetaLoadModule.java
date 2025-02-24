package org.violetmoon.zeta.module;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ZetaLoadModule {
	/**
	 * The name of the category this module belongs to.
	 * See ZetaModuleManager.addCategories.
	 */
	String category() default "";

	/**
	 * The name of this module. If unspecified, defaults to de-camelcasing the module's class name.
	 * Ex "MyCoolModule"'s name defaults to "My Cool Module".
	 */
	String name() default "";
	String description() default "";

	/**
	 * Mod IDs that, if present, this module will disable itself by default on.
	 */
	String[] antiOverlap() default {};

	boolean enabledByDefault() default true;

	/**
	 * If "true", this module will load *instead* of the module the class extends, on the client.
	 *
	 * For example:
	 *
	 * <pre>
	 * @ZetaLoadModule
	 * class FooModule {
	 *
	 *   @ZetaLoadModule(clientReplacement = true)
	 *   static class Client extends FooModule {
	 *     //client-only code
	 *   }
	 * }
	 * </pre>
	 *
	 * On the client, `FooModule.Client` will load, and on the server, `FooModule` will load.
	 * Either way, the module is accessible with `FooModule.class` in APIs like ZetaModuleManager#get.
	 */
	boolean clientReplacement() default false;
}
