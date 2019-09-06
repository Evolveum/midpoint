/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathCollectionsUtil;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FullTextSearchConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FullTextSearchIndexedItemsConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author mederly
 */
public class FullTextSearchConfigurationUtil {

	public static boolean isEnabled(FullTextSearchConfigurationType config) {
		return config != null && !config.getIndexed().isEmpty() && !Boolean.FALSE.equals(config.isEnabled());
	}

	public static boolean isEnabledFor(FullTextSearchConfigurationType config, Class<? extends ObjectType> clazz) {
		return isEnabled(config) && !getFullTextSearchItemPaths(config, clazz).isEmpty();
	}

	@NotNull
	public static Set<ItemPath> getFullTextSearchItemPaths(@NotNull FullTextSearchConfigurationType config, Class<? extends ObjectType> clazz) {
		List<QName> types =
				ObjectTypes.getObjectType(clazz).thisAndSupertypes().stream()
						.map(ot -> ot.getTypeQName())
						.collect(Collectors.toList());
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
}
