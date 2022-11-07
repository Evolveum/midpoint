/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.ResourceContentPanel;

import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.wizard.AbstractWizardBasicPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

/**
 * @author lskublik
 */
public class PreviewResourceObjectTypeDataWizardPanel extends AbstractWizardBasicPanel {

    private static final String ID_TABLE = "table";

    private final IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> resourceObjectType;

    public PreviewResourceObjectTypeDataWizardPanel(
            String id,
            ResourceDetailsModel model,
            IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> resourceObjectType) {
        super(id, model);
        this.resourceObjectType = resourceObjectType;
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();

        getTable().setShowAsCard(false);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        ResourceContentPanel table = new ResourceContentPanel(
                ID_TABLE,
                resourceObjectType.getObject().getRealValue().getKind(),
                getResourceModel(),
                null,
                false) {

            @Override
            protected boolean isIntentAndObjectClassPanelVisible() {
                return false;
            }

            @Override
            protected String getIntent() {
                return resourceObjectType.getObject().getRealValue().getIntent();
            }

            @Override
            protected QName getObjectClass() {
                return resourceObjectType.getObject().getRealValue().getObjectClass();
            }

            @Override
            protected boolean isTopTableButtonsVisible() {
                return false;
            }

            @Override
            protected boolean isSourceChoiceVisible() {
                return false;
            }

            @Override
            protected boolean isTaskButtonsContainerVisible() {
                return false;
            }

            @Override
            protected boolean isResourceSearch() {
                return true;
            }

            @Override
            protected boolean isRepoSearch() {
                return false;
            }
        };
        table.setOutputMarkupId(true);
        add(table);
    }


    protected IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> getResourceObjectType() {
        return resourceObjectType;
    }

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return getPageBase().createStringResource("PreviewResourceObjectTypeDataWizardPanel.title");
    }

        @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("PreviewResourceObjectTypeDataWizardPanel.subText");
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("PreviewResourceObjectTypeDataWizardPanel.text");
    }

    public BoxedTablePanel getTable() {
        ResourceContentPanel panel =
                (ResourceContentPanel) get(ID_TABLE);
        if (panel == null) {
            return null;
        }
        return panel.getTable();
    }
}
