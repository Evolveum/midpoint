/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClassLoggerConfigurationType;

/**
 * @author skublik
 */
public class ProfilingClassLoggerPanel extends PrismContainerPanel<ClassLoggerConfigurationType, PrismContainerWrapper<ClassLoggerConfigurationType>> {

    public ProfilingClassLoggerPanel(String id, IModel<PrismContainerWrapper<ClassLoggerConfigurationType>> model, ItemPanelSettings settings) {
        super(id, model,
                new ItemPanelSettingsBuilder()
                        .visibilityHandler(itemWrapper -> checkVisibility(itemWrapper, settings.getVisibilityHandler()))
                        .mandatoryHandler(itemWrapper -> false)
                        .build());
    }

    private static ItemVisibility checkVisibility(ItemWrapper wrapper, ItemVisibilityHandler handler) {
        if (wrapper.getItemName().equals(ClassLoggerConfigurationType.F_PACKAGE)) {
            return ItemVisibility.HIDDEN;
        }
        return handler != null ? handler.isVisible(wrapper) : ItemVisibility.AUTO;
    }
}
