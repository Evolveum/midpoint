/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.correlation;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.tile.ViewToggle;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardBasicPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.evolveum.midpoint.web.session.UserProfileStorage.TableId.TABLE_SMART_CORRELATION;

/**
 * @author lskublik
 */
@PanelType(name = "rw-correlationRules")
@PanelInstance(identifier = "rw-correlationRules",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "CorrelationWizardPanelWizardPanel.headerLabel", icon = "fa fa-code-branch"))
public abstract class CorrelationItemsTableWizardPanel extends AbstractResourceWizardBasicPanel<CorrelationDefinitionType> {

    private static final Trace LOGGER = TraceManager.getTrace(CorrelationItemsTableWizardPanel.class);

    private static final String ID_NOT_SHOWN_CONTAINER_INFO = "notShownContainerInfo";
    private static final String PANEL_TYPE = "rw-correlationRules";
    private static final String ID_TABLE = "table";

    public CorrelationItemsTableWizardPanel(
            String id,
            WizardPanelHelper<CorrelationDefinitionType, ResourceDetailsModel> superHelper) {
        super(id, superHelper);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    @Contract(pure = true)
    private @NotNull LoadableModel<List<PrismContainerValueWrapper<ItemsSubCorrelatorType>>> getItemsSubCorrelatorTypes() {
        return new LoadableModel<>() {
            @Override
            protected List<PrismContainerValueWrapper<ItemsSubCorrelatorType>> load() {
                try {
                    PrismContainerValueWrapper<CorrelationDefinitionType> object = getValueModel().getObject();
                    if (object == null) {
                        return List.of();
                    }
                    PrismContainerWrapper<ItemsSubCorrelatorType> container = object.findContainer(
                            ItemPath.create(
                                    CorrelationDefinitionType.F_CORRELATORS,
                                    CompositeCorrelatorType.F_ITEMS));
                    return container.getValues();
                } catch (SchemaException e) {
                    throw new RuntimeException("Error while loading items sub-correlator types", e);
                }
            }
        };
    }

    private void initLayout() {

        ResourceDetailsModel detailsModel = getHelper().getDetailsModel();
        ResourceType resource = detailsModel.getObjectType();
        String resourceOid = resource.getOid();
        SmartCorrelationTable table = new SmartCorrelationTable(
                ID_TABLE,
                TABLE_SMART_CORRELATION,
                Model.of(ViewToggle.TILE),
                getItemsSubCorrelatorTypes(),
                resourceOid);
        table.setOutputMarkupId(true);
        add(table);

        CorrelationItemsTable table2 = new CorrelationItemsTable("old", getValueModel(), getConfiguration()) {
            @Override
            public void editItemPerformed(
                    AjaxRequestTarget target,
                    IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> rowModel,
                    List<PrismContainerValueWrapper<ItemsSubCorrelatorType>> listItems) {
                showTableForItemRefs(target, rowModel);
            }
        };
        table2.setOutputMarkupId(true);
        add(table2);

        Label info = new Label(
                ID_NOT_SHOWN_CONTAINER_INFO,
                getPageBase().createStringResource("CorrelationItemsTableWizardPanel.notShownContainer"));
        info.setOutputMarkupId(true);
        info.add(new VisibleBehaviour(this::isNotShownContainerInfo));
        add(info);
    }

    private Boolean isNotShownContainerInfo() {
        PrismContainerValueWrapper<CorrelationDefinitionType> objectType = getValueModel().getObject();
        if (objectType != null) {
            try {
                PrismContainerWrapper<Containerable> correlators = objectType.findContainer(
                        ItemPath.create(CorrelationDefinitionType.F_CORRELATORS));
                if (correlators != null) {
                    PrismContainerValueWrapper<Containerable> correlatorsValue = correlators.getValue();
                    if (correlatorsValue != null) {
                        for (PrismContainerWrapper<? extends Containerable> container : correlatorsValue.getContainers()) {
                            if (container == null
                                    || container.isOperational()
                                    || container.getItemName().equivalent(CompositeCorrelatorType.F_ITEMS)
                                    || container.getItemName().equivalent(CompositeCorrelatorType.F_EXTENSION)) {
                                continue;
                            }

                            PrismContainer<? extends Containerable> cloneContainer = container.getItem().clone();
                            WebPrismUtil.cleanupEmptyContainers(cloneContainer);
                            if (!cloneContainer.isEmpty()) {
                                return true;
                            }
                        }
                    }
                }
            } catch (SchemaException e) {
                LOGGER.debug("Couldn't find correlators container in " + objectType);
            }
        }
        return false;
    }

    protected abstract void showTableForItemRefs(
            AjaxRequestTarget target,
            IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> rowModel);

    @Override
    protected String getSaveLabelKey() {
        return "CorrelationWizardPanelWizardPanel.saveButton";
    }

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return getPageBase().createStringResource("CorrelationWizardPanelWizardPanel.breadcrumb");
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("CorrelationWizardPanelWizardPanel.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("CorrelationWizardPanelWizardPanel.subText");
    }

    protected SmartCorrelationTable getTable() {
        return (SmartCorrelationTable) get(ID_TABLE);
    }

    @Override
    protected boolean isValid(AjaxRequestTarget target) {
        return false;
    }

    @Override
    protected String getCssForWidthOfFeedbackPanel() {
        return "col-11";
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }
}
