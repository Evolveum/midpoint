/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.query;

import com.evolveum.midpoint.prism.PrismReferenceValue;

/**
 *
 */
public interface OrgFilter extends ObjectFilter {

	enum Scope {
		ONE_LEVEL,
		SUBTREE,
		ANCESTORS       // EXPERIMENTAL; OID has to belong to an OrgType!
	}

	PrismReferenceValue getOrgRef();

	Scope getScope();

	boolean isRoot();

	@Override
	OrgFilter clone();

}
