/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl.polystring;

import com.evolveum.midpoint.prism.polystring.PolyStringNormalizerRegistry;

/**
 *
 */
public class PolyStringNormalizerRegistryFactory {

	public static PolyStringNormalizerRegistry createRegistry() {

		PolyStringNormalizerRegistryImpl registry = new PolyStringNormalizerRegistryImpl();
		registry.registerDefaultNormalizer(new AlphanumericPolyStringNormalizer());
		registry.registerNormalizer(new Ascii7PolyStringNormalizer());
		registry.registerNormalizer(new PassThroughPolyStringNormalizer());
		return registry;
	}

}
