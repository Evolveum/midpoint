/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;

/**
 * Interface for objects that behave like an item: they have a name and may have a definition.
 *
 * Currently provides common abstraction on top of Item and ItemDelta, as both can hold values and
 * construct them in a similar way.
 *
 * Also used for ValueFilter, although semantics of e.g. getPath() is quite different in this case.
 *
 * @author Radovan Semancik
 *
 */
public interface Itemable extends PrismContextSensitive {

    ItemName getElementName();

    ItemDefinition getDefinition();

    ItemPath getPath();

}
