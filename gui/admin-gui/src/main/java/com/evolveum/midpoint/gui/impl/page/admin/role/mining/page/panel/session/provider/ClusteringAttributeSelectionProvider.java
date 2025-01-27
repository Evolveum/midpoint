/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session.provider;

import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClusteringAttributeRuleType;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ClusteringAttributeSelectionProvider extends AbstractAttributeSelectionProvider<ClusteringAttributeRuleType> {

    public ClusteringAttributeSelectionProvider(Class<?> complexType, ModelServiceLocator modelServiceLocator) {
        super(complexType, modelServiceLocator);
    }

    @Override
    protected boolean includeMultivaluedDef() {
        return true;
    }

    @Override
    protected ClusteringAttributeRuleType createAttribute(@NotNull ItemPath path, @NotNull ItemDefinition<?> definition) {
        ClusteringAttributeRuleType rule = new ClusteringAttributeRuleType();
        rule.path(path.toBean())
                .similarity(100.0)
                .isMultiValue(definition.isMultiValue())
                .weight(1.0);
        return rule;
    }

    @Override
    protected boolean isMatchingValue(@NotNull ClusteringAttributeRuleType attribute, @NotNull ClusteringAttributeRuleType value) {
        return Objects.equals(simpleValue(attribute.getPath()), simpleValue(value.getPath()));
    }
}
