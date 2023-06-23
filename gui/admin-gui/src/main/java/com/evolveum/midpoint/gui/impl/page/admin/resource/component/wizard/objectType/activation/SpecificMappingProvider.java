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
import com.evolveum.midpoint.gui.impl.component.data.provider.ContainerListDataProvider;
import com.evolveum.midpoint.prism.Containerable;

import com.evolveum.midpoint.util.QNameUtil;

import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceBidirectionalMappingType;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class SpecificMappingProvider<C extends Containerable> extends ContainerListDataProvider<Containerable> {

    private static final Trace LOGGER = TraceManager.getTrace(SpecificMappingProvider.class);

    private final MappingDirection mappingDirection;
    private final IModel<PrismContainerValueWrapper<C>> parentModel;
    private List<PrismContainerValueWrapper> availableValues;

    public SpecificMappingProvider(
            Component component,
            IModel<PrismContainerValueWrapper<C>> parentModel,
            MappingDirection mappingDirection) {
        super(component, Model.of());
        this.mappingDirection = mappingDirection;
        this.parentModel = parentModel;
    }

    @Override
    public Iterator<? extends PrismContainerValueWrapper> iterator(long l, long l1) {
        if (getAvailableData().isEmpty()) {
            PrismContainerValueWrapper<? extends Containerable> parent = parentModel.getObject();
            if (parent != null) {
                WebPrismUtil.sortContainers(parent.getContainers());
                for (PrismContainerWrapper<? extends Containerable> child : parent.getContainers()) {
                    if (QNameUtil.match(child.getTypeName(), ResourceBidirectionalMappingType.COMPLEX_TYPE)) {
                        try {
                            PrismContainerWrapper<Containerable> container = child.findContainer(mappingDirection.getContainerName());
                            getAvailableData().addAll(container.getValues());
                        } catch (SchemaException e) {
                            LOGGER.debug("Couldn't find container with name " + mappingDirection.getContainerName() + ", skipping it", e);
                            continue;
                        }
                    } else {
                        getAvailableData().addAll((Collection<? extends PrismContainerValueWrapper<Containerable>>) child.getValues());
                    }
                }
            }
        }
        return getAvailableData().iterator();
    }

    @Override
    public long size() {
        int size = 0;
        PrismContainerValueWrapper<? extends Containerable> parent = parentModel.getObject();
        for (PrismContainerWrapper<? extends Containerable> child : parent.getContainers()) {
            if (QNameUtil.match(child.getTypeName(), ResourceBidirectionalMappingType.COMPLEX_TYPE)) {
                try {
                    PrismContainerWrapper<Containerable> container = child.findContainer(mappingDirection.getContainerName());
                    size += container.getValues().size();
                } catch (SchemaException e) {
                    LOGGER.debug("Couldn't find container with name " + mappingDirection.getContainerName() + ", skipping it", e);
                    continue;
                }
            } else {
                size += child.getValues().size();
            }
        }

        return size;
    }

    @Override
    public IModel<PrismContainerValueWrapper> model(PrismContainerValueWrapper object) {
        return Model.of(object);
    }

    @Override
    public void detach() {
        super.detach();
        getAvailableData().clear();
    }
}
