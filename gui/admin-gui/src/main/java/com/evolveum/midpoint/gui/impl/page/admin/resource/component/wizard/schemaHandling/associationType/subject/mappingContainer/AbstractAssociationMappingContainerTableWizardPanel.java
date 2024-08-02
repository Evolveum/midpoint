/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.subject.mappingContainer;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardBasicPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.MappingTile;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationDefinitionType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractAssociationMappingContainerTableWizardPanel extends AbstractResourceWizardBasicPanel<ShadowAssociationDefinitionType> {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractAssociationMappingContainerTableWizardPanel.class);

    private static final String ID_TABLE = "table";
    private IModel<PrismContainerWrapper<MappingType>> containerModel;

    public AbstractAssociationMappingContainerTableWizardPanel(String id, WizardPanelHelper<ShadowAssociationDefinitionType, ResourceDetailsModel> superHelper) {
        super(id, superHelper);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initContainerModel();
        initLayout();
    }

    private void initContainerModel() {
        if (containerModel == null) {
            containerModel = PrismContainerWrapperModel.fromContainerValueWrapper(getValueModel(), getItemNameForMappingContainer());
        }
    }

    protected abstract ItemName getItemNameForMappingContainer();

    private void initLayout() {

        MappingContainerTablePanel table = new MappingContainerTablePanel(ID_TABLE, getTableId(), new PropertyModel<>(getContainerModel(), "values")) {
            @Override
            protected void onTileClick(AjaxRequestTarget target, MappingTile modelObject) {
                AbstractAssociationMappingContainerTableWizardPanel.this.onTileClick(target, modelObject);
            }

            @Override
            protected void onClickCreateMapping(AjaxRequestTarget target) {
                PrismContainerWrapper<MappingType> containerWrapper = getContainerModel().getObject();
                try {
                    PrismContainerValue<MappingType> newValue = containerWrapper.getItem().createNewValue();

                    PrismContainerValueWrapper<MappingType> valueWrapper = WebPrismUtil.createNewValueWrapper(
                            containerWrapper, newValue, getPageBase(), getAssignmentHolderDetailsModel().createWrapperContext());
                    containerWrapper.getValues().add(valueWrapper);
                    refresh(target);

                    AbstractAssociationMappingContainerTableWizardPanel.this.onClickCreateMapping(valueWrapper, target);
                } catch (SchemaException e) {
                    LOGGER.debug("Couldn't create new value for container " + containerWrapper);
                }
            }

            @Override
            protected String getAddButtonLabelKey() {
                return AbstractAssociationMappingContainerTableWizardPanel.this.getAddButtonLabelKey();
            }
        };
        add(table);
    }

    protected abstract String getAddButtonLabelKey();

    protected abstract void onClickCreateMapping(PrismContainerValueWrapper<MappingType> valueWrapper, AjaxRequestTarget target);

    protected abstract void onTileClick(AjaxRequestTarget target, MappingTile modelObject);

    private IModel<PrismContainerWrapper<MappingType>> getContainerModel() {
        return containerModel;
    }

    protected abstract UserProfileStorage.TableId getTableId();

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return getTextModel();
    }

    @Override
    protected boolean isSubmitButtonVisible() {
        return false;
    }
}
