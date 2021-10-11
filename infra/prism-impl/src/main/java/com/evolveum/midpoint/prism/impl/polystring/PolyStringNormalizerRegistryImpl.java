/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
