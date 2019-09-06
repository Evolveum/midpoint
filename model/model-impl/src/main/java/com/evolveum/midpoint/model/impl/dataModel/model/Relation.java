/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.dataModel.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author mederly
 */
public class Relation {

	@NotNull final private List<DataItem> sources;
	@Nullable final private DataItem target;

	public Relation(@NotNull List<DataItem> sources, @Nullable DataItem target) {
		this.sources = sources;
		this.target = target;
	}

	@NotNull
	public List<DataItem> getSources() {
		return sources;
	}

	@Nullable
	public DataItem getTarget() {
		return target;
	}

	@Override
	public String toString() {
		return "Relation{" +
				"sources=" + sources +
				", target=" + target +
				'}';
	}

}
