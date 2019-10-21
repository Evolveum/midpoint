/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query.definition;

import java.util.Comparator;

/**
 * @author lazyman
 */
public class DefinitionComparator implements Comparator<Definition> {

    @Override
    public int compare(Definition o1, Definition o2) {
        if (o1.getClass().equals(o2.getClass())) {
            return String.CASE_INSENSITIVE_ORDER.compare(o1.getJaxbName().getLocalPart(),
                    o2.getJaxbName().getLocalPart());
        }

        return getType(o1) - getType(o2);
    }

    private int getType(Definition def) {
        if (def == null) {
            return 0;
        }

        if (def instanceof PropertyDefinition) {
            return 1;
        } else if (def instanceof ReferenceDefinition) {
            return 2;
        } else if (def instanceof CollectionDefinition) {
            return 3;
        } else if (def instanceof AnyDefinition) {
            return 4;
        } else if (def instanceof EntityDefinition) {
            return 5;
        }

        return 0;
    }
}
