/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.archetype;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SummaryPanelSpecificationType;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.web.component.FocusSummaryPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;

public class ArchetypeSummaryPanel extends FocusSummaryPanel<ArchetypeType>{

    private static final long serialVersionUID = 1L;

    private final IModel<ArchetypeType> modelWithApplyDelta;

    public ArchetypeSummaryPanel(String id, IModel<ArchetypeType> modelWithApplyDelta, IModel<ArchetypeType> model,
            SummaryPanelSpecificationType summaryPanelSpecificationType) {
        super(id, ArchetypeType.class, model, summaryPanelSpecificationType);
        this.modelWithApplyDelta = modelWithApplyDelta;
    }


    @Override
    protected QName getDisplayNamePropertyName() {
        return ArchetypeType.F_DISPLAY_NAME;
    }

    @Override
    protected QName getTitlePropertyName() {
        return ArchetypeType.F_IDENTIFIER;
    }

    @Override
    protected String getDefaultIconCssClass() {
        return GuiStyleConstants.EVO_ARCHETYPE_TYPE_ICON;
    }

    @Override
    protected String getIconBoxAdditionalCssClass() {
        return "summary-panel-user";
    }

    @Override
    protected String getBoxAdditionalCssClass() {
        return "summary-panel-user";
    }

    @Override
    protected AssignmentHolderType getAssignmentHolderTypeObjectForArchetypeDisplayType() {
        return modelWithApplyDelta.getObject();
    }
}
