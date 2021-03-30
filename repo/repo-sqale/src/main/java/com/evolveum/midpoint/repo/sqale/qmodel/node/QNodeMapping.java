/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.node;

import static com.evolveum.midpoint.repo.sqlbase.filtering.item.SimpleItemFilterProcessor.stringMapper;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType.F_NODE_IDENTIFIER;

import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;

/**
 * Mapping between {@link QNode} and {@link NodeType}.
 */
public class QNodeMapping
        extends QObjectMapping<NodeType, QNode, MNode> {

    public static final String DEFAULT_ALIAS_NAME = "nod";

    public static final QNodeMapping INSTANCE = new QNodeMapping();

    private QNodeMapping() {
        super(QNode.TABLE_NAME, DEFAULT_ALIAS_NAME, NodeType.class, QNode.class);

        addItemMapping(F_NODE_IDENTIFIER, stringMapper(path(q -> q.nodeIdentifier)));
    }

    @Override
    protected QNode newAliasInstance(String alias) {
        return new QNode(alias);
    }

    @Override
    public NodeSqlTransformer createTransformer(
            SqlTransformerSupport transformerSupport) {
        return new NodeSqlTransformer(transformerSupport, this);
    }

    @Override
    public MNode newRowObject() {
        return new MNode();
    }
}
