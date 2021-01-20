/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmapping;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sqale.qbean.MNode;
import com.evolveum.midpoint.repo.sqale.qmodel.QNode;
import com.evolveum.midpoint.repo.sqlbase.SqlRepoContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;

public class NodeSqlTransformer
        extends ObjectSqlTransformer<NodeType, QNode, MNode> {

    public NodeSqlTransformer(
            PrismContext prismContext, QNodeMapping mapping, SqlRepoContext sqlRepoContext) {
        super(prismContext, mapping, sqlRepoContext);
    }
}
