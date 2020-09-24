/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.certification;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDefinitionType;

import org.apache.wicket.model.IModel;

/**
 * @author mederly
 */
public class CertDefinitionSummaryPanel extends ObjectSummaryPanel<AccessCertificationDefinitionType> {
    private static final long serialVersionUID = 1L;

    public CertDefinitionSummaryPanel(String id,
            IModel<AccessCertificationDefinitionType> model, ModelServiceLocator serviceLocator) {
        super(id, AccessCertificationDefinitionType.class, model, serviceLocator);
    }

    @Override
    protected String getDefaultIconCssClass() {
        return GuiStyleConstants.CLASS_OBJECT_CERT_DEF_ICON;
    }

    @Override
    protected String getIconBoxAdditionalCssClass() {        // TODO
        return "summary-panel-task"; // TODO
    }

    @Override
    protected String getBoxAdditionalCssClass() {            // TODO
        return "summary-panel-task"; // TODO
    }

    @Override
    protected boolean isIdentifierVisible() {
        return false;
    }
}
