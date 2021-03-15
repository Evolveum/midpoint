/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.object;

import java.util.UUID;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.repo.sqale.qmodel.SqaleTransformerBase;
import com.evolveum.midpoint.repo.sqale.qmodel.common.MContainer;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainer;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainerMapping;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;

public class ContainerSqlTransformer
        <S extends Containerable, Q extends QContainer<R>, R extends MContainer>
        extends SqaleTransformerBase<S, Q, R> {

    public ContainerSqlTransformer(
            SqlTransformerSupport transformerSupport, QContainerMapping<S, Q, R> mapping) {
        super(transformerSupport, mapping);
    }

    /**
     * This creates the right type of object and fills in the base {@link MContainer} attributes.
     */
    public R initRowObject(S schemaObject, UUID ownerOid) {
        R row = mapping.newRowObject();
        row.ownerOid = ownerOid;
        row.cid = schemaObject.asPrismContainerValue().getId();
        // containerType is generated in DB, must be left null!
        return row;
    }
}
