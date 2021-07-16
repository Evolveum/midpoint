/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.objectTemplate;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectCollectionType;

/**
 * @author skublik
 */

public class ObjectTemplateSummaryPanel extends ObjectSummaryPanel<ObjectTemplateType> {

    public ObjectTemplateSummaryPanel(String id, IModel<ObjectTemplateType> model, ModelServiceLocator serviceLocator) {
        super(id, ObjectTemplateType.class, model, serviceLocator);
    }

    @Override
    protected String getDefaultIconCssClass() {
        return GuiStyleConstants.CLASS_OBJECT_TEMPLATE_ICON;
    }

    @Override
    protected String getIconBoxAdditionalCssClass() {
        return "summary-panel-shadow";
    }

    @Override
    protected String getBoxAdditionalCssClass() {
        return "summary-panel-shadow";
    }
}
