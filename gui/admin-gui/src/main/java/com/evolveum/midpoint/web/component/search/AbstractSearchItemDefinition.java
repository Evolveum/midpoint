/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import java.io.Serializable;

public abstract class AbstractSearchItemDefinition<D extends AbstractSearchItemDefinition> implements Serializable, Comparable<D> {

    @Override
    public int compareTo(D def) {
        String n1 = getName();
        String n2 = def.getName();

        if (n1 == null || n2 == null) {
            return 0;
        }
        return String.CASE_INSENSITIVE_ORDER.compare(n1, n2);
    }

    public abstract String getName();

    public abstract String getHelp();

}
