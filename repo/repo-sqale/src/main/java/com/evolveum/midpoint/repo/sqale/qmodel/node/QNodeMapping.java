/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.node;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType.F_NODE_IDENTIFIER;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType.F_OPERATIONAL_STATE;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolderMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;

/**
 * Mapping between {@link QNode} and {@link NodeType}.
 */
public class QNodeMapping
        extends QAssignmentHolderMapping<NodeType, QNode, MNode> {

    public static final String DEFAULT_ALIAS_NAME = "nod";

    public static QNodeMapping init(@NotNull SqaleRepoContext repositoryContext) {
        return new QNodeMapping(repositoryContext);
    }

    private QNodeMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QNode.TABLE_NAME, DEFAULT_ALIAS_NAME,
                NodeType.class, QNode.class, repositoryContext);

        addItemMapping(F_NODE_IDENTIFIER, stringMapper(q -> q.nodeIdentifier));
        addItemMapping(F_OPERATIONAL_STATE, enumMapper(q -> q.operationalState));
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
        row.operationalState = node.getOperationalState();
        return row;
    }
}
