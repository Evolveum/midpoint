/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.query;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

/**
 *
 */
public interface TypeFilter extends ObjectFilter {

	@NotNull
	QName getType();

	ObjectFilter getFilter();

	void setFilter(ObjectFilter filter);

	@Override
	TypeFilter clone();

	TypeFilter cloneEmpty();

}
