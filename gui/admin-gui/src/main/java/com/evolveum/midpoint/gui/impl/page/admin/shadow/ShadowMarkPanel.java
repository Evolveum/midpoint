/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.shadow;

import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.mark.component.MarksOfObjectListPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ShadowDetailsModel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

@PanelType(name = "shadowMarks")
@PanelInstance(
        identifier = "shadowMarks",
        applicableForType = ShadowType.class,
        defaultPanel = true,
        display = @PanelDisplay(
                label = "PageFocus.marks", order = 15
        )
)
public class ShadowMarkPanel extends AbstractObjectMainPanel<ShadowType, ShadowDetailsModel> {

    private static final long serialVersionUID = 1L;

    public static final String PANEL_TYPE = "shadowMarks";

    private static final String ID_PANEL = "panel";

    public ShadowMarkPanel(String id, ShadowDetailsModel objectWrapperModel, ContainerPanelConfigurationType config) {
        super(id, objectWrapperModel, config);
    }

    protected void initLayout() {
        MarksOfObjectListPanel<ShadowType> shadowPanel = new MarksOfObjectListPanel<>(
                ID_PANEL,
                getObjectWrapperModel(),
                getPanelConfiguration());
        add(shadowPanel);
    }
}
