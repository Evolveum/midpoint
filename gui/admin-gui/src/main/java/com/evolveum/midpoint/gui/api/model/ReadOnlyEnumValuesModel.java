/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.model;

import org.apache.wicket.model.IModel;

import java.util.Arrays;
import java.util.List;

/**
 * EXPERIMENTAL
 *
 * @author mederly
 */
public class ReadOnlyEnumValuesModel<E extends Enum<E>> implements IModel<List<E>> {

	private final List<E> values;

	public ReadOnlyEnumValuesModel(Class<E> e) {
		this.values = Arrays.asList(e.getEnumConstants());
	}

	@Override
	public List<E> getObject() {
		return values;
	}
}
