/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.expression;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;

/**
 *
 * Built for lazy resolving.
 *
 * @author semancik
 *
 */
public interface ValuePolicyResolver {

	void setOutputDefinition(ItemDefinition outputDefinition);

	void setOutputPath(ItemPath outputPath);

	ValuePolicyType resolve();

}
