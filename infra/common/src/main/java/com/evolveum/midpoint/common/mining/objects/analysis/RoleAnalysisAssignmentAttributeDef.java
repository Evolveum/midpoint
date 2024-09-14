/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.mining.objects.analysis;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class RoleAnalysisAssignmentAttributeDef extends RoleAnalysisAttributeDef {

    private AnalysisAttributeRuleType rule;

    public RoleAnalysisAssignmentAttributeDef(ItemPath path, ItemDefinition<?> definition, AnalysisAttributeRuleType rule) {
        super(path, definition);
        this.rule = rule;
    }

    @Override
    public @NotNull Set<String> resolveMultiValueItem(@NotNull PrismObject<?> prismObject, @NotNull ItemPath itemPath) {

        return resolveAssignment(prismObject, itemPath, rule.getTargetType());
    }

    private static Set<String> resolveAssignment(
            @NotNull PrismObject<?> prismObject,
            @NotNull ItemPath itemPath,
            @NotNull QName targetType) {
        Set<String> resolvedValues = new HashSet<>();
        Collection<Item<?, ?>> allItems = prismObject.getAllItems(itemPath);
        for (Item<?, ?> item : allItems) {
            boolean isMultiValue = !item.isSingleValue();

            if (isMultiValue) {
                Collection<?> realValues = item.getRealValues();
                for (Object realValue : realValues) {
                    if (realValue instanceof ObjectReferenceType objectReference) {
                        QName refTargetType = objectReference.getType();
                        if (refTargetType.equals(targetType)) {
                            resolvedValues.add(objectReference.getOid());
                        }
                    }
                }
            } else {
                Object realValue = item.getRealValue();
                if (realValue instanceof ObjectReferenceType objectReference) {
                    QName refTargetType = objectReference.getType();
                    if (refTargetType.equals(targetType)) {
                        resolvedValues.add(objectReference.getOid());
                    }
                }
            }
        }

        return resolvedValues;
    }

    @Override
    public ObjectQuery getQuery(String value) {
        QName targetType = rule.getTargetType();
//        Class<? extends ObjectType> objectClass = PrismContext.get().getSchemaRegistry().determineClassForType(targetType);

//        ObjectReferenceType ref = new ObjectReferenceType();
//        ref.setOid(value);
//        ref.setType(targetType);

        return PrismContext.get().queryFor(UserType.class)
                .item(getPath()).ref(value, targetType)
                .build();
    }
}
