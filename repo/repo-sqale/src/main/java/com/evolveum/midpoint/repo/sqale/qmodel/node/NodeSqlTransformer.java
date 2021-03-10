/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.node;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.qmodel.object.ObjectSqlTransformer;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;

public class NodeSqlTransformer
        extends ObjectSqlTransformer<NodeType, QNode, MNode> {

    public NodeSqlTransformer(
            SqlTransformerSupport transformerSupport, QNodeMapping mapping) {
        super(transformerSupport, mapping);
    }

    @Override
    public @NotNull MNode toRowObjectWithoutFullObject(NodeType schemaObject, JdbcSession jdbcSession) {
        MNode node = super.toRowObjectWithoutFullObject(schemaObject, jdbcSession);

        node.nodeIdentifier = schemaObject.getNodeIdentifier();
        return node;
    }
}
