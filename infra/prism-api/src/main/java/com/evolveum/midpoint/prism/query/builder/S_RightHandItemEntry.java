/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.query.builder;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;

import javax.xml.namespace.QName;

/**
 * @author mederly
 */
public interface S_RightHandItemEntry {
    // TODO add support for matching rules
    S_AtomicFilterExit item(QName... names);
    S_AtomicFilterExit item(ItemPath itemPath, ItemDefinition itemDefinition);
}
