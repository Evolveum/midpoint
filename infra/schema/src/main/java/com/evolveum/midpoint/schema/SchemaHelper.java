/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.prism.PrismContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * TEMPORARY
 */
@Component
public class SchemaHelper {

	@Autowired
	private PrismContext prismContext;

	public PrismContext getPrismContext() {
		return prismContext;
	}

	public GetOperationOptionsBuilder getOperationOptionsBuilder() {
		return new GetOperationOptionsBuilderImpl(prismContext);
	}
}
