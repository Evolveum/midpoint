/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.object;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.repo.sqale.qmodel.SqaleTransformerBase;
import com.evolveum.midpoint.repo.sqale.qmodel.TransformerForOwnedBy;
import com.evolveum.midpoint.repo.sqale.qmodel.common.MContainer;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainer;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainerMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;

/**
 * @param <S> schema type
 * @param <Q> type of entity path
 * @param <R> type of the row bean for the table
 * @param <OR> type of the owner row (table owning the container)
 */
public class ContainerSqlTransformer
        <S extends Containerable, Q extends QContainer<R, OR>, R extends MContainer, OR>
        extends SqaleTransformerBase<S, Q, R>
        implements TransformerForOwnedBy<S, R, OR> {

    private final QContainerMapping<S, Q, R, OR> mapping;

    public ContainerSqlTransformer(
            SqlTransformerSupport transformerSupport, QContainerMapping<S, Q, R, OR> mapping) {
        super(transformerSupport);
        this.mapping = mapping;
    }

    @Override
    protected QContainerMapping<S, Q, R, OR> mapping() {
        return mapping;
    }

    /**
     * This creates the right type of object and fills in the base {@link MContainer} attributes.
     */
    public R initRowObject(S schemaObject, OR ownerRow) {
        R row = mapping.newRowObject(ownerRow);
        row.cid = schemaObject.asPrismContainerValue().getId();
        // containerType is generated in DB, must be left null!
        return row;
    }

    @Override
    public R insert(S schemaObject, OR ownerRow, JdbcSession jdbcSession) {
        throw new UnsupportedOperationException("insert not implemented in the subclass");
    }
}
