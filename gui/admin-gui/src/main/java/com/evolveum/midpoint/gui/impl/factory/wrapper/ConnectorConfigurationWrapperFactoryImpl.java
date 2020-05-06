/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.factory.wrapper;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorConfigurationType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class ConnectorConfigurationWrapperFactoryImpl extends PrismContainerWrapperFactoryImpl {

    @Override
    public boolean match(ItemDefinition def) {
        if (def instanceof PrismContainerDefinition && ((PrismContainerDefinition) def).getCompileTimeClass() != null) {
            return ConnectorConfigurationType.class.isAssignableFrom(((PrismContainerDefinition) def).getCompileTimeClass());
        }
        return false;
    }

    @Override
    public int getOrder() {
        return 10;
    }

    @Override
    protected List<? extends ItemDefinition> getItemDefinitions(PrismContainerWrapper parent, PrismContainerValue value) {
        List<PrismContainerDefinition> relevantDefinitions = new ArrayList<>();
        List<? extends ItemDefinition> defs = parent.getDefinitions();
        for (ItemDefinition<?> def : defs) {
            if (def instanceof PrismContainerDefinition) {
                relevantDefinitions.add((PrismContainerDefinition) def);
            }
        }
        Collections.sort(relevantDefinitions, (o1, o2) -> {
            int ord1 = o1.getDisplayOrder() != null ? o1.getDisplayOrder() : Integer.MAX_VALUE;
            int ord2 = o2.getDisplayOrder() != null ? o2.getDisplayOrder() : Integer.MAX_VALUE;
            return Integer.compare(ord1, ord2);
        });
        return relevantDefinitions;
    }
}
