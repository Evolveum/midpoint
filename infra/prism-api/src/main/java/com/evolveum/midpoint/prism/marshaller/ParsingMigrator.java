/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.marshaller;

import com.evolveum.midpoint.prism.ParsingContext;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;

/**
 * Migrator that comes into play when content is parsed.
 *
 * Currently used to treat situations when simple content (e.g. message: string) was replaced by complex one (message: LocalizableMessageType).
 *
 * @author mederly
 */
public interface ParsingMigrator {

    /**
     * Tries to unmarshal primitive value into a given bean (if standard ways fail).
     * @return non-null if it could be done
     */
    <T> T tryParsingPrimitiveAsBean(PrimitiveXNode<T> primitive, Class<T> beanClass, ParsingContext pc);
}
