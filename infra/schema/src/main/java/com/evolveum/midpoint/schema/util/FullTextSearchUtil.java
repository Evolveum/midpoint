/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.util;

import java.util.*;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathCollectionsUtil;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FullTextSearchConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FullTextSearchIndexedItemsConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

public class FullTextSearchUtil {

    private static final Trace LOGGER = TraceManager.getTrace(FullTextSearchUtil.class);

    public static boolean isEnabled(FullTextSearchConfigurationType config) {
        return config != null
                && !config.getIndexed().isEmpty()
                && !Boolean.FALSE.equals(config.isEnabled());
    }

    public static boolean isEnabledFor(FullTextSearchConfigurationType config, Class<? extends ObjectType> clazz) {
        return isEnabled(config)
                && !getFullTextSearchItemPaths(config, clazz).isEmpty();
    }

    public static boolean isEnabledFor(FullTextSearchConfigurationType config, List<QName> types) {
        return isEnabled(config)
                && !getFullTextSearchItemPaths(config, types).isEmpty();
    }

    @NotNull
    public static Set<ItemPath> getFullTextSearchItemPaths(
            @NotNull FullTextSearchConfigurationType config, Class<? extends ObjectType> clazz) {
        List<QName> types = ObjectTypes.getObjectType(clazz)
                .thisAndSupertypes()
                .stream()
                .map(ot -> ot.getTypeQName())
                .collect(Collectors.toList());
        return getFullTextSearchItemPaths(config, types);
    }

    @NotNull
    public static Set<ItemPath> getFullTextSearchItemPaths(
            @NotNull FullTextSearchConfigurationType config, List<QName> types) {
        Set<ItemPath> paths = new HashSet<>();
        for (FullTextSearchIndexedItemsConfigurationType indexed : config.getIndexed()) {
            if (isApplicable(indexed, types)) {
                for (ItemPathType itemPathType : indexed.getItem()) {
                    ItemPath path = itemPathType.getItemPath();
                    if (!ItemPath.isEmpty(path) && !ItemPathCollectionsUtil.containsEquivalent(paths, path)) {
                        paths.add(path);
                    }
                }
            }
        }
        return paths;
    }

    private static boolean isApplicable(FullTextSearchIndexedItemsConfigurationType indexed, List<QName> types) {
        if (indexed.getObjectType().isEmpty()) {
            return true;
        }
        for (QName type : types) {
            if (QNameUtil.matchAny(type, indexed.getObjectType())) {
                return true;
            }
        }
        return false;
    }

    public static <T extends ObjectType> boolean isObjectTextInfoRecomputationNeeded(
            FullTextSearchConfigurationType config, Class<T> type, Collection<? extends ItemDelta<?, ?>> modifications) {
        if (!FullTextSearchUtil.isEnabled(config)) {
            return false;
        }

        Set<ItemPath> paths = FullTextSearchUtil.getFullTextSearchItemPaths(config, type);

        for (ItemDelta<?, ?> modification : modifications) {
            ItemPath namesOnly = modification.getPath().namedSegmentsOnly();
            for (ItemPath path : paths) {
                if (path.startsWith(namesOnly)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Returns words to index or null if there is nothing to index.
     */
    public static @Nullable Set<String> createWords(
            FullTextSearchConfigurationType config, ObjectType object) {
        Set<ItemPath> paths = getFullTextSearchItemPaths(config, object.getClass());

        List<PrismValue> values = new ArrayList<>();
        for (ItemPath path : paths) {
            Object o = object.asPrismObject().find(path);
            if (o == null) {
                // shouldn't occur
            } else if (o instanceof PrismValue) {
                values.add((PrismValue) o);
            } else if (o instanceof Item) {
                values.addAll(((Item<?, ?>) o).getValues());
            } else {
                throw new IllegalStateException("Unexpected value " + o + " in " + object + " at " + path);
            }
        }
        if (values.isEmpty()) {
            return null;
        }

        Set<String> allWords = new LinkedHashSet<>(); // not sure why, but old repo preserved order too
        for (PrismValue value : values) {
            if (value == null) {
                continue;
            }
            if (value instanceof PrismPropertyValue) {
                Object realValue = value.getRealValue();
                if (realValue == null) {
                    // skip
                } else if (realValue instanceof String) {
                    append(allWords, (String) realValue);
                } else if (realValue instanceof PolyString) {
                    append(allWords, ((PolyString) realValue).getOrig());
                } else {
                    append(allWords, realValue.toString());
                }
            }
        }
        LOGGER.trace("Indexing {}:\n  items: {}\n  values: {}\n  words:  {}",
                object, paths, values, allWords);

        return allWords.isEmpty() ? null : allWords;
    }

    private static void append(Set<String> allWords, String text) {
        if (StringUtils.isBlank(text)) {
            return;
        }
        String normalized = PrismContext.get().getDefaultPolyStringNormalizer().normalize(text);
        String[] words = StringUtils.split(normalized);
        for (String word : words) {
            if (StringUtils.isNotBlank(word)) {
                allWords.add(word); // set will handle duplicates
            }
        }
    }
}
