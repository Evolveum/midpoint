/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.mining.objects.analysis;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import javax.xml.namespace.QName;

public class RoleAnalysisAssignmentAttributeDef extends RoleAnalysisAttributeDef {

    private AnalysisAttributeRuleType rule;

    public RoleAnalysisAssignmentAttributeDef(ItemPath path, ItemDefinition<?> definition, AnalysisAttributeRuleType rule) {
        super(path, definition);
        this.rule = rule;
    }

    @Override
    public ObjectQuery getQuery(String value) {
        QName targetType = rule.getTargetType();
        Class<? extends ObjectType> objectClass = PrismContext.get().getSchemaRegistry().determineClassForType(targetType);
        return PrismContext.get().queryFor(objectClass)
                .item(getPath()).ref(value)
                .build();
    }
}
