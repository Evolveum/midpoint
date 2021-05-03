/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.node;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType.F_NODE_IDENTIFIER;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
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

        addItemMapping(F_NODE_IDENTIFIER, stringMapper(q -> q.nodeIdentifier));
    }

    @Override
    protected QNode newAliasInstance(String alias) {
        return new QNode(alias);
    }

    @Override
    public MNode newRowObject() {
        return new MNode();
    }

    @Override
    public @NotNull MNode toRowObjectWithoutFullObject(NodeType node, JdbcSession jdbcSession) {
        MNode row = super.toRowObjectWithoutFullObject(node, jdbcSession);

        row.nodeIdentifier = node.getNodeIdentifier();
        return row;
    }
}
