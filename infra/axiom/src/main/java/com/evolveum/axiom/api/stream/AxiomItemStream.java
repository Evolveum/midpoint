/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.api.stream;

import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.AxiomPrefixedName;
import com.evolveum.axiom.concepts.SourceLocation;
import com.evolveum.axiom.lang.spi.AxiomNameResolver;

public interface AxiomItemStream {

    interface Target extends AxiomStreamTarget<AxiomName, Object> {

    }

    interface TargetWithResolver extends Target {

        AxiomNameResolver itemResolver();
        AxiomNameResolver valueResolver();
        AxiomNameResolver infraResolver();

        default AxiomStreamTarget<AxiomPrefixedName, Object> asPrefixed(AxiomNameResolver sourceLocal) {
            return new PrefixedToQNameTarget(this, () -> itemResolver().or(sourceLocal), () -> valueResolver().or(sourceLocal), this::infraResolver);
        }
    }

    interface TargetWithContext extends Target, AxiomStreamTarget.WithContext<AxiomName, Object> {

    }

}
