/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.util;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Resolves a definition in a parent. Used in cases when there are non-standard resolution exceptions,
 * e.g. default string definitions for items in dynamic schema.
 *
 * @param <PD> parent definition
 * @param <ID> subitem definition
 *
 * @author semancik
 */
@FunctionalInterface
public interface DefinitionResolver<PD extends ItemDefinition, ID extends ItemDefinition> {

    ID resolve(PD parentDefinition, ItemPath path) throws SchemaException;

}
