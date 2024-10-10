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
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationDefinitionType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractAssociationMappingContainerTableWizardPanel extends AbstractResourceWizardBasicPanel<ShadowAssociationDefinitionType> {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractAssociationMappingContainerTableWizardPanel.class);

    private static final String ID_TITLE_ICON = "titleIcon";
    private static final String ID_TITLE_LABEL = "titleLabel";
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

        WebMarkupContainer icon = new WebMarkupContainer(ID_TITLE_ICON);
        add(icon);
        icon.add(AttributeAppender.append("class", getTitleIconClass()));

        add(new Label(ID_TITLE_LABEL, getTitleLabelModel()));

        IModel<List<PrismContainerValueWrapper<MappingType>>> model = new IModel<List<PrismContainerValueWrapper<MappingType>>>() {
            @Override
            public List<PrismContainerValueWrapper<MappingType>> getObject() {
                List<PrismContainerValueWrapper<MappingType>> list = new ArrayList<>(getContainerModel().getObject().getValues());
                list.removeIf(value -> ValueStatus.DELETED == value.getStatus());
                return list;
            }
        };

        MappingContainerTablePanel table = new MappingContainerTablePanel(ID_TABLE, getTableId(), model) {
            @Override
            protected void onTileClick(AjaxRequestTarget target, MappingTile modelObject) {
                AbstractAssociationMappingContainerTableWizardPanel.this.onTileClick(target, modelObject);
            }

            @Override
            protected void onClickCreateMapping(AjaxRequestTarget target) {
                PrismContainerWrapper<MappingType> containerWrapper = getContainerModel().getObject();
                try {
                    PrismContainerValue<MappingType> newValue = containerWrapper.getItem().createNewValue();
                    postProcessNewMapping(newValue);

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

    protected abstract void postProcessNewMapping(PrismContainerValue<MappingType> newValue) throws SchemaException;

    protected IModel<String> getTitleLabelModel() {
        return getTextModel();
    }

    protected abstract String getTitleIconClass();

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
    protected void onExitPerformed(AjaxRequestTarget target) {
        getHelper().onExitPerformed(target);
    }

    @Override
    protected void onSubmitPerformed(AjaxRequestTarget target) {
        super.onSubmitPerformed(target);
    }
}
