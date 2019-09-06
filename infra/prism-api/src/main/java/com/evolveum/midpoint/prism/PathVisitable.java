/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.path.ItemPath;

/**
 * Visits only objects that are on the specified path or below.
 *
 * @author Radovan Semancik
 *
 */
@FunctionalInterface
public interface PathVisitable {

	void accept(Visitor visitor, ItemPath path, boolean recursive);

}
