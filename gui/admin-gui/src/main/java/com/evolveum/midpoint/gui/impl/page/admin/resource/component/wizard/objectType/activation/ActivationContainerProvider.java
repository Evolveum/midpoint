/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.activation;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.MappingDirection;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.data.provider.BaseSortableDataProvider;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceActivationDefinitionType;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ActivationContainerProvider extends BaseSortableDataProvider<PrismContainerDefinition> {

    private final MappingDirection mappingDirection;
    private final IModel<PrismContainerValueWrapper<ResourceActivationDefinitionType>> parentModel;

    public ActivationContainerProvider(
            Component component,
            IModel<PrismContainerValueWrapper<ResourceActivationDefinitionType>> parentModel,
            MappingDirection mappingDirection) {
        super(component, false, false);
        this.mappingDirection = mappingDirection;
        this.parentModel = parentModel;
    }

    @Override
    public Iterator<? extends PrismContainerDefinition> internalIterator(long first, long count) {
        List<PrismContainerDefinition> list = new ArrayList<>();
        PrismContainerValueWrapper<? extends Containerable> parent = parentModel.getObject();
        if (parent != null) {
            WebPrismUtil.sortContainers(parent.getContainers());
            for (PrismContainerWrapper<? extends Containerable> child : parent.getContainers()) {
                if (skipContainerDefinition(child.getItem().getDefinition())) {
                    continue;
                }
                list.add(child.getItem().getDefinition());
            }
        }
        return list.iterator();
    }

    private boolean skipContainerDefinition(PrismContainerDefinition<? extends Containerable> definition){
        if (MappingDirection.INBOUND.equals(mappingDirection)) {
            if (ResourceActivationDefinitionType.F_EXISTENCE.equivalent(definition.getItemName())
                    || ResourceActivationDefinitionType.F_DISABLE_INSTEAD_OF_DELETE.equivalent(definition.getItemName())
                    || ResourceActivationDefinitionType.F_DELAYED_DELETE.equivalent(definition.getItemName())
                    || ResourceActivationDefinitionType.F_PRE_PROVISION.equivalent(definition.getItemName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int internalSize() {
        PrismContainerValueWrapper<? extends Containerable> parent = parentModel.getObject();
        if (parent == null) {
            return 0;
        }
        return parent.getContainers().size();
    }
}
