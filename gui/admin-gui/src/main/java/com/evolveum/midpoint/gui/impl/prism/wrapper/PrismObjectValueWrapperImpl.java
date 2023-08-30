/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.wrapper;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectValueWrapper;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.prism.PrismContainerValue;

import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObjectValue;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author katka
 *
 */
public class PrismObjectValueWrapperImpl<O extends ObjectType> extends PrismContainerValueWrapperImpl<O> implements PrismObjectValueWrapper<O> {

    private static final long serialVersionUID = 1L;

    public PrismObjectValueWrapperImpl(PrismObjectWrapper<O> parent, PrismObjectValue<O> pcv, ValueStatus status) {
        super(parent, pcv, status);
        setExpanded(true);
    }

    @Override
    public List<PrismContainerWrapper<? extends Containerable>> getContainers() {
        List<PrismContainerWrapper<? extends Containerable>> containers = new ArrayList<>();
        for (ItemWrapper<?, ?> container : getItems()) {

            collectExtensionItems(container, true, containers);

            if (!DetailsPageUtil.isNewDesignEnabled() && container instanceof PrismContainerWrapper && ((PrismContainerWrapper<?>) container).isVirtual()) {
                containers.add((PrismContainerWrapper<? extends Containerable>) container);
            }

//            if (container instanceof  PrismContainerWrapper && ((PrismContainerWrapper) container).isVirtual()) {
//                ((List)containers).add(container);
//            }

//            if (ObjectType.F_METADATA.equals(container.getItemName())) {
//                ((List)containers).add(container);
//            }

        }
        return containers;
    }


    @Override
    public String getDisplayName() {
        return new StringResourceModel("prismContainer.mainPanelDisplayName").getString();
    }

    @Override
    public PrismContainerValue<O> getNewValue() {
        return super.getNewValue();
    }
}
