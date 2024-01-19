/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.abstractrole.AbstractRoleDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.assignmentType.inducement.RoleInducementsPanel;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

@PanelType(name = "brw-existingAccess")
@PanelInstance(identifier = "brw-access",
        applicableForType = RoleType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageRole.wizard.step.access.applicationRole", icon = "fa fa-list"),
        containerPath = "empty")
public class ExsitingAccessApplicationRoleStepPanel<AR extends AbstractRoleType>
        extends AbstractWizardStepPanel<AbstractRoleDetailsModel<AR>> {

    private static final Trace LOGGER = TraceManager.getTrace(ExsitingAccessApplicationRoleStepPanel.class);

    public static final String PANEL_TYPE = "brw-existingAccess";
    private static final String ID_INDUCEMENTS = "inducements";


    public ExsitingAccessApplicationRoleStepPanel(AbstractRoleDetailsModel<AR> model) {
        super(model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        RoleInducementsPanel<AR> roleInducementsPanel = new RoleInducementsPanel<>(ID_INDUCEMENTS, getDetailsModel().getObjectWrapperModel(), getContainerConfiguration(PANEL_TYPE));
        add(roleInducementsPanel);
    }

    @Override
    protected boolean isExitButtonVisible() {
        return true;
    }

    protected String getIcon() {
        return "fa fa-list";
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageRole.wizard.step.existing.access");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageRole.wizard.step.existing.access.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageRole.wizard.step.existing.access.subText");
    }

    @Override
    public String getStepId() {
        return PANEL_TYPE;
    }
}
