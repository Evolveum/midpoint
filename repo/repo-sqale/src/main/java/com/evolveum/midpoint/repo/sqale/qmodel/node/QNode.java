/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.node;

import java.sql.Types;

import com.querydsl.core.types.dsl.EnumPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;

import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeOperationalStateType;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QNode extends QAssignmentHolder<MNode> {

    private static final long serialVersionUID = 2042159341967925185L;

    public static final String TABLE_NAME = "m_node";

    public static final ColumnMetadata NODE_IDENTIFIER =
            ColumnMetadata.named("nodeIdentifier").ofType(Types.VARCHAR);
    public static final ColumnMetadata OPERATIONAL_STATE =
            ColumnMetadata.named("operationalState").ofType(Types.OTHER);

    // columns and relations

    public final StringPath nodeIdentifier = createString("nodeIdentifier", NODE_IDENTIFIER);
    public final EnumPath<NodeOperationalStateType> operationalState =
            createEnum("operationalState", NodeOperationalStateType.class, OPERATIONAL_STATE);

    public QNode(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QNode(String variable, String schema, String table) {
        super(MNode.class, variable, schema, table);
    }
}
