/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.ResourceContentPanel;

import com.evolveum.midpoint.schema.TaskExecutionMode;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SerializableConsumer;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardBasicPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

/**
 * @author lskublik
 */
public class PreviewResourceObjectTypeDataWizardPanel extends AbstractWizardBasicPanel<ResourceDetailsModel> {

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

    //TODO rewrite to ResourceObjectsPanel or maybe better ResourceUncategorizedPanel. We
    // want to show all objects from resource, not only shadows as those might not exist yet.
    private void initLayout() {
        ResourceContentPanel table = new ResourceContentPanel(
                ID_TABLE,
                resourceObjectType.getObject().getRealValue().getKind(),
                getAssignmentHolderDetailsModel(),
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
            protected QName getObjectClassFromSearch() {
                QName objectClass = null;
                ResourceObjectTypeDefinitionType objectType = resourceObjectType.getObject().getRealValue();
                if (objectType.getDelineation() != null) {
                    objectClass = objectType.getDelineation().getObjectClass();
                }
                if (objectClass == null) {
                    objectClass = objectType.getObjectClass();
                }

                return objectClass;
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

            @Override
            protected void customizeProvider(SelectableBeanObjectDataProvider<ShadowType> provider) {
                provider.setTaskConsumer((SerializableConsumer<Task>)task -> {
                    String lifecycleState = resourceObjectType.getObject().getRealValue().getLifecycleState();
                    if (StringUtils.isEmpty(lifecycleState)) {
                        lifecycleState = getObjectDetailsModels().getObjectType().getLifecycleState();
                    }
                    if (SchemaConstants.LIFECYCLE_PROPOSED.equals(lifecycleState)) {
                        task.setExecutionMode(TaskExecutionMode.SIMULATED_SHADOWS_DEVELOPMENT);
                    }
                });
            }

            @Override
            protected boolean isReclassifyButtonVisible() {
                return false;
            }

            @Override
            protected boolean isShadowDetailsEnabled(IModel<SelectableBean<ShadowType>> rowModel) {
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
