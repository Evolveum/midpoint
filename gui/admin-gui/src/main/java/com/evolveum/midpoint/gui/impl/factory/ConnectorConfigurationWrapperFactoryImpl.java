/*
 * Copyright (c) 2010-2019 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.gui.impl.factory;

import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
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
