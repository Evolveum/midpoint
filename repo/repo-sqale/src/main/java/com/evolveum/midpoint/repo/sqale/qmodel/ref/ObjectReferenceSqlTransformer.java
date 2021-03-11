/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.ref;

import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;

public class ObjectReferenceSqlTransformer
        extends ReferenceSqlTransformer<QObjectReference, MReference> {

    public ObjectReferenceSqlTransformer(
            SqlTransformerSupport transformerSupport, QObjectReferenceMapping mapping) {
        super(transformerSupport, mapping);
    }
}
