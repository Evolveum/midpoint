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
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.jetbrains.annotations.NotNull;

public class AnalysisAttributeSelectionProvider extends AbstractAttributeSelectionProvider<ItemPathType> {

    public AnalysisAttributeSelectionProvider(ModelServiceLocator modelServiceLocator) {
        super(UserType.class, modelServiceLocator);
    }

    @Override
    protected boolean includeMultivaluedDef() {
        return true;
    }

    @Override
    protected ItemPathType createAttribute(@NotNull ItemPath path, ItemDefinition<?> definition) {
        return path.toBean();
    }

    @Override
    protected boolean isMatchingValue(@NotNull ItemPathType attribute, @NotNull ItemPathType value) {
        return simpleValue(attribute).equals(simpleValue(value));
    }

}
