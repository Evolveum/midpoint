/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.delta.item;

import java.util.Collection;

import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.repo.sqale.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqale.delta.ItemDeltaValueProcessor;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QObjectReferenceMapping;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;

public class RefTableItemDeltaProcessor implements ItemDeltaValueProcessor<Referencable> {

    protected final SqaleUpdateContext<?, ?, ?> context;
    private final QObjectReferenceMapping refTableMapping;

    public RefTableItemDeltaProcessor(
            SqaleUpdateContext<?, ?, ?> context,
            QObjectReferenceMapping refTableMapping) {
        this.context = context;
        this.refTableMapping = refTableMapping;
    }

    @Override
    public void process(ItemDelta<?, ?> modification) throws RepositoryException {
        System.out.println("modification = " + modification);
        // TODO

//        ReferenceSqlTransformer<QObjectReference, MReference> transformer =
//                refTableMapping.createTransformer(context.transformerSupport());
//        transformer.insert();
    }

    @Override
    public void setRealValues(Collection<?> values) {
        System.out.println("values = " + values);
        // TODO
    }

    @Override
    public void delete() {
        System.out.println("DELETE");
        // TODO
    }
}
