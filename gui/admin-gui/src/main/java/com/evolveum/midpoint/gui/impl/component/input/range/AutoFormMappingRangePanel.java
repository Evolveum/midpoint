/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.input.range;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;

/**
 * Functionally the same as {@link MappingRangePanel} - only the markup differs, since this variant is used
 *
 * @author jjarabinec
 */
public class AutoFormMappingRangePanel extends MappingRangePanel {

    public AutoFormMappingRangePanel(String id, IModel<PrismContainerValueWrapper<MappingType>> model) {
        super(id, model);
    }
}
