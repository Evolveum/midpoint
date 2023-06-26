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
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceBidirectionalMappingType;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.evolveum.midpoint.gui.api.util.WebComponentUtil.safeLongToInteger;

public class SpecificMappingProvider<C extends Containerable> extends ContainerListDataProvider {

    private static final Trace LOGGER = TraceManager.getTrace(SpecificMappingProvider.class);

    private final MappingDirection mappingDirection;
    private final IModel<PrismContainerValueWrapper<C>> parentModel;

    public SpecificMappingProvider(
            Component component,
            IModel<PrismContainerValueWrapper<C>> parentModel,
            MappingDirection mappingDirection) {
        super(component, Model.of());
        this.mappingDirection = mappingDirection;
        this.parentModel = parentModel;
    }

    @Override
    protected Iterator<? extends PrismContainerValueWrapper> doRepositoryIteration(long first, long count) {
        List<PrismContainerValueWrapper> list = getListOfValues();

        Integer indexFrom = safeLongToInteger(first);
        Integer indexTo = safeLongToInteger(count);
        if (list.size() > indexFrom) {
            if (list.size() <= indexTo) {
                indexTo = list.size();
            }
            getAvailableData().addAll(list.subList(indexFrom, indexTo));
        }
        return getAvailableData().iterator();
    }

    private List<PrismContainerValueWrapper> getListOfValues() {
        List<PrismContainerValueWrapper> list = new ArrayList<>();
        PrismContainerValueWrapper<? extends Containerable> parent = parentModel.getObject();
        if (parent != null) {
            WebPrismUtil.sortContainers(parent.getContainers());
            for (PrismContainerWrapper<? extends Containerable> child : parent.getContainers()) {
                if (QNameUtil.match(child.getTypeName(), ResourceBidirectionalMappingType.COMPLEX_TYPE)) {
                    try {
                        PrismContainerWrapper<Containerable> container = child.findContainer(mappingDirection.getContainerName());
                        container.getValues().forEach(value -> {
                            if (ValueStatus.DELETED.equals(value.getStatus())) {
                                return;
                            }
                            list.add(value);
                        });
                    } catch (SchemaException e) {
                        LOGGER.debug("Couldn't find container with name " + mappingDirection.getContainerName() + ", skipping it", e);
                    }
                } else if (MappingDirection.OUTBOUND.equals(mappingDirection)){
                    child.getValues().forEach(value -> {
                        if (ValueStatus.DELETED.equals(value.getStatus())) {
                            return;
                        }
                        list.add(value);
                    });
                }
            }
        }
        return list;
    }

    @Override
    public long size() {
        List<PrismContainerValueWrapper> list = getListOfValues();
        return list.size();
    }
}
