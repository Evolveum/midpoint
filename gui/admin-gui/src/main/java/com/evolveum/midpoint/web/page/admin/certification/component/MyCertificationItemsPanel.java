/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.certification.component;

import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.io.Serial;

@PanelType(name = "myCertItems")
public class MyCertificationItemsPanel extends CertificationItemsPanel {

    @Serial private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = MyCertificationItemsPanel.class.getName() + ".";

    public MyCertificationItemsPanel(String id, String campaignOid) {
        super(id, campaignOid);
    }

    public MyCertificationItemsPanel(String id, ContainerPanelConfigurationType configurationType) {
        super(id, configurationType);
    }

    //todo cleanup; the same hack as for MyCaseWorkItemsPanel
    public MyCertificationItemsPanel(String id, AssignmentHolderDetailsModel model, ContainerPanelConfigurationType configurationType) {
        super(id, null, configurationType);
    }

    @Override
    protected boolean isMyCertItems() {
        return true;
    }

}
