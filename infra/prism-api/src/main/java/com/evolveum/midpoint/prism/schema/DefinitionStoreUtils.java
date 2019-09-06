/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.schema;

import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.prism.ItemDefinition;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author mederly
 */
public class DefinitionStoreUtils {
	public static <D extends Definition> D getOne(List<D> list) {
		if (list.isEmpty()) {
			return null;
		} else if (list.size() == 1) {
			return list.get(0);
		} else {
			// consider not deprecated ones
			List<D> notDeprecated = list.stream()
					.filter(def -> !def.isDeprecated())
					.collect(Collectors.toList());
			if (notDeprecated.size() == 1) {
				return notDeprecated.get(0);
			} else {
				throw new IllegalStateException("More than one definition found: " + list);
			}
		}
	}

	public static <ID extends ItemDefinition> ID getOne(List<ID> list, boolean exceptionIfAmbiguous, String message) {
		if (list.isEmpty()) {
			return null;
		} else if (list.size() == 1) {
			return list.get(0);
		} else {
			// consider not deprecated ones
			List<ID> notDeprecated = list.stream()
					.filter(def -> !def.isDeprecated())
					.collect(Collectors.toList());
			if (notDeprecated.size() == 1) {
				return notDeprecated.get(0);
			} else {
				if (exceptionIfAmbiguous) {
					throw new IllegalArgumentException(message + ": " +
							list.stream().map(ItemDefinition::getItemName).collect(Collectors.toList()));
				} else {
					return null;
				}
			}
		}
	}
}
