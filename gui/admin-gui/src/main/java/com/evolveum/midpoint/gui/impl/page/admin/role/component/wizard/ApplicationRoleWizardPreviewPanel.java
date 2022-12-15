/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard;

import com.evolveum.midpoint.gui.impl.component.wizard.EnumWizardChoicePanel;
import com.evolveum.midpoint.gui.api.component.wizard.TileEnum;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;

import com.evolveum.midpoint.web.page.admin.roles.PageRoles;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

public abstract class ApplicationRoleWizardPreviewPanel
        extends EnumWizardChoicePanel<ApplicationRoleWizardPreviewPanel.PreviewTileType, AssignmentHolderDetailsModel<RoleType>> {

    public ApplicationRoleWizardPreviewPanel(String id, AssignmentHolderDetailsModel<RoleType> roleModel) {
        super(id, roleModel, PreviewTileType.class);
    }

    @Override
    protected QName getObjectType() {
        return RoleType.COMPLEX_TYPE;
    }

    enum PreviewTileType implements TileEnum {

        CONFIGURE_CONSTRUCTION("fa fa-retweet");

        private final String icon;

        PreviewTileType(String icon) {
            this.icon = icon;
        }

        @Override
        public String getIcon() {
            return icon;
        }
    }

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        String name = WebComponentUtil.getDisplayNameOrName(getAssignmentHolderDetailsModel().getObjectWrapper().getObject());
        if (StringUtils.isEmpty(name)) {
            return getPageBase().createStringResource("ApplicationRoleWizardPreviewPanel.title");
        }
        return Model.of(name);
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("ApplicationRoleWizardPreviewPanel.subText");
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("ApplicationRoleWizardPreviewPanel.text");
    }

    @Override
    protected void onExitPerformed(AjaxRequestTarget target) {
        getPageBase().navigateToNext(PageRoles.class);
    }

}
