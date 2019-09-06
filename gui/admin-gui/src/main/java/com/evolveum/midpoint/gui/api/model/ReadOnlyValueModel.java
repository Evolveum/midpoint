/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.model;

import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

/**
 * EXPERIMENTAL
 * TODO better name
 *
 * @author mederly
 */
public class ReadOnlyValueModel<T> implements IModel<T> {

	@NotNull private final T object;

	public ReadOnlyValueModel(@NotNull T object) {
		this.object = object;
	}

	@Override
	public T getObject() {
		return object;
	}
}
