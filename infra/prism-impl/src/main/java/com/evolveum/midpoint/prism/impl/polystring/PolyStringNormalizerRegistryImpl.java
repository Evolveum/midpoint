/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.prism.impl.polystring;

import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizerRegistry;
import com.evolveum.midpoint.util.QNameUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class PolyStringNormalizerRegistryImpl implements PolyStringNormalizerRegistry {

	private PolyStringNormalizer defaultNormalizer;
	@NotNull private final Map<QName, PolyStringNormalizer> normalizers = new HashMap<>();

	@NotNull
	@Override
	public PolyStringNormalizer getNormalizer(@Nullable QName name) {
		if (name == null) {
			if (defaultNormalizer != null) {
				return defaultNormalizer;
			} else {
				throw new IllegalStateException("Default PolyString normalizer is not set");
			}
		}
		PolyStringNormalizer normalizer = normalizers.get(name);
		if (normalizer != null) {
			return normalizer;
		} else {
			for (Map.Entry<QName, PolyStringNormalizer> entry : normalizers.entrySet()) {
				if (QNameUtil.match(entry.getKey(), name)) {
					return entry.getValue();
				}
			}
		}
		throw new IllegalArgumentException("Unknown polystring normalizer: " + name);   // todo or SchemaException?
	}

	void registerNormalizer(PolyStringNormalizer normalizer) {
		normalizers.put(normalizer.getName(), normalizer);
	}

	void registerDefaultNormalizer(PolyStringNormalizer normalizer) {
		registerNormalizer(normalizer);
		defaultNormalizer = normalizer;
	}
}
