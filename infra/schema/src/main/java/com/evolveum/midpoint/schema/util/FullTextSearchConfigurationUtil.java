/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.UniformItemPath;
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
	public static Set<UniformItemPath> getFullTextSearchItemPaths(@NotNull FullTextSearchConfigurationType config, Class<? extends ObjectType> clazz) {
		List<QName> types =
				ObjectTypes.getObjectType(clazz).thisAndSupertypes().stream()
						.map(ot -> ot.getTypeQName())
						.collect(Collectors.toList());
		Set<UniformItemPath> paths = new HashSet<>();
		for (FullTextSearchIndexedItemsConfigurationType indexed : config.getIndexed()) {
			if (isApplicable(indexed, types)) {
				for (ItemPathType itemPathType : indexed.getItem()) {
					UniformItemPath path = itemPathType.getUniformItemPath();
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
