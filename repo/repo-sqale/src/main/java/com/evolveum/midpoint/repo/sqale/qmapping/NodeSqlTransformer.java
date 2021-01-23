/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmapping;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqlbase.SqlTransformerContext;
import com.evolveum.midpoint.repo.sqale.qbean.MNode;
import com.evolveum.midpoint.repo.sqale.qmodel.QNode;
import com.evolveum.midpoint.repo.sqlbase.SqlRepoContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;

public class NodeSqlTransformer
        extends ObjectSqlTransformer<NodeType, QNode, MNode> {

    public NodeSqlTransformer(
            SqlTransformerContext transformerContext, QNodeMapping mapping, SqlRepoContext sqlRepoContext) {
        super(transformerContext, mapping, sqlRepoContext);
    }

    @Override
    public @NotNull MNode toRowObjectWithoutFullObject(NodeType schemaObject) {
        MNode node = super.toRowObjectWithoutFullObject(schemaObject);

        node.nodeIdentifier = schemaObject.getNodeIdentifier();
        return node;
    }
}
