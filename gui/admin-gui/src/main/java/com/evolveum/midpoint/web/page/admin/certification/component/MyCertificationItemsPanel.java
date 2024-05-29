/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.certification.component;

import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;


import java.io.Serial;

@PanelType(name = "myCertItems")
public class MyCertificationItemsPanel extends CertificationItemsPanel {

    @Serial private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = MyCertificationItemsPanel.class.getName() + ".";
    private static final Trace LOGGER = TraceManager.getTrace(MyCertificationItemsPanel.class);
    private static final String OPERATION_COUNT_ALL_CERTIFICATION_ITEMS = DOT_CLASS + "loadCertItems";
    private static final String OPERATION_COUNT_NOT_DECIDED_CERTIFICATION_ITEMS = DOT_CLASS + "loadNotDecidedCertItems";

    public MyCertificationItemsPanel(String id) {
        super(id);
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
