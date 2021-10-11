/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClassLoggerConfigurationType;

/**
 * @author skublik
 *
 */
public class ProfilingClassLoggerPanel extends PrismContainerPanel<ClassLoggerConfigurationType> {

    public ProfilingClassLoggerPanel(String id, IModel<PrismContainerWrapper<ClassLoggerConfigurationType>> model, ItemPanelSettings settings) {
        super(id, model, new ItemPanelSettingsBuilder().visibilityHandler(itemWrapper -> checkVisibility(itemWrapper, settings.getVisibilityHandler())).build());
    }

    private static ItemVisibility checkVisibility(ItemWrapper itemWrapper, ItemVisibilityHandler visibilitytHandler) {

        if(itemWrapper.getItemName().equals(ClassLoggerConfigurationType.F_PACKAGE)) {
            return ItemVisibility.HIDDEN;
        }
        return visibilitytHandler != null ? visibilitytHandler.isVisible(itemWrapper) : ItemVisibility.AUTO;
    }
}
