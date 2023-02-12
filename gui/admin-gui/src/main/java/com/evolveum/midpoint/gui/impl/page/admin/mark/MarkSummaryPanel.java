/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.mark;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MarkType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SummaryPanelSpecificationType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class MarkSummaryPanel extends ObjectSummaryPanel<MarkType> {

    public MarkSummaryPanel(String id, IModel<MarkType> model, SummaryPanelSpecificationType summaryPanelSpecification) {
        super(id, MarkType.class, model, summaryPanelSpecification);
    }

    @Override
    protected String getDefaultIconCssClass() {
        return GuiStyleConstants.CLASS_MARK;
    }

    @Override
    protected String getIconBoxAdditionalCssClass() {
        return null;
    }

    @Override
    protected String getBoxAdditionalCssClass() {
        return null;
    }

    @Override
    protected IModel<String> getDisplayNameModel() {
        return () -> {
            DisplayType display = getModelObject().getDisplay();
            if (display == null || display.getLabel() == null) {
                return null;
            }

            return WebComponentUtil.getTranslatedPolyString(display.getLabel());
        };
    }
}
