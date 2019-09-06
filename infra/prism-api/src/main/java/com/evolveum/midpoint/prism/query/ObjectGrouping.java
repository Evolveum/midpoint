/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.query;

import com.evolveum.midpoint.prism.path.ItemPath;

import java.io.Serializable;

/**
 *
 */
public interface ObjectGrouping extends Serializable {

	ItemPath getGroupBy();

	boolean equals(Object o, boolean exact);

}
