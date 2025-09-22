/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.schema.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.tabs.CountablePanelTab;
import com.evolveum.midpoint.gui.api.component.tabs.IconPanelTab;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.table.ListItemWithPanelForItemPanel;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.schema.PageSchema;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormDefaultContainerablePanel;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormPrismContainerValuePanel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.*;
import com.evolveum.midpoint.web.component.TabCenterTabbedPanel;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaType;
import com.evolveum.midpoint.xml.ns._public.prism_schema_3.ComplexTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.prism_schema_3.DefinitionType;
import com.evolveum.midpoint.xml.ns._public.prism_schema_3.EnumerationTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.prism_schema_3.PrismSchemaType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@PanelType(name = "complex-type-definitions")
@PanelInstance(
        identifier = "complex-type-definitions",
        applicableForType = SchemaType.class,
        display = @PanelDisplay(label = "ComplexTypeDefinitionPanel.title", icon = GuiStyleConstants.CLASS_OBJECT_SCHEMA_TEMPLATE_ICON, order = 25))
@Counter(provider = SchemaDefinitionCounter.class)
public class ComplexTypeDefinitionPanel<AH extends AssignmentHolderType, ADM extends AssignmentHolderDetailsModel<AH>> extends AbstractObjectMainPanel<AH, ADM> {

    @Serial private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ComplexTypeDefinitionPanel.class);

    private static final String ID_PANEL = "panel";

    private final ItemPath containerPath;

    public ComplexTypeDefinitionPanel(String id, ADM model, ContainerPanelConfigurationType config) {
        this(id, model, config, ItemPath.EMPTY_PATH);
    }

    public ComplexTypeDefinitionPanel(String id, ADM model, ContainerPanelConfigurationType config, ItemPath containerPath) {
        super(id, model, config);
        this.containerPath = containerPath.append(WebPrismUtil.PRISM_SCHEMA);
    }

    private PrismContainerValueWrapper<DefinitionType> getSelectedValue(PrismContainerWrapper<DefinitionType> containerWrapper) {
        if (containerWrapper != null) {
            Optional<PrismContainerValueWrapper<DefinitionType>> typeValue =
                    containerWrapper.getValues().stream()
                            .filter(PrismContainerValueWrapper::isSelected)
                            .findFirst();
            if (typeValue.isPresent()) {
                typeValue.get().setExpanded(true);
                return typeValue.get();
            }
        }
        return null;
    }

    @Override
    protected void initLayout() {

        ListItemWithPanelForItemPanel<PrismContainerValueWrapper<? extends DefinitionType>> panel = new ListItemWithPanelForItemPanel<>(ID_PANEL) {
            @Override
            protected PrismContainerValueWrapper<? extends DefinitionType> getSelectedItem() {
                return ComplexTypeDefinitionPanel.this.getSelectedItem();
            }

            @Override
            protected List<PrismContainerValueWrapper<? extends DefinitionType>> createListOfItem(IModel<String> searchItemModel) {
                return ComplexTypeDefinitionPanel.this.createListOfItem(searchItemModel);
            }

            @Override
            protected WebMarkupContainer createPanelForItem(
                    String idPanelForItem, IModel<PrismContainerValueWrapper<? extends DefinitionType>> selectedItemModel) {
                return createTabPanel(idPanelForItem, selectedItemModel);
            }

            protected void unselectAllDefinitionValues() {
                try {
                    PrismContainerWrapper<ComplexTypeDefinitionType> complexContainerWrapper =
                            getObjectWrapper().findContainer(ItemPath.create(containerPath, PrismSchemaType.F_COMPLEX_TYPE));
                    complexContainerWrapper.getValues().forEach(value -> value.setSelected(false));

                    PrismContainerWrapper<EnumerationTypeDefinitionType> enumContainerWrapper =
                            getObjectWrapper().findContainer(ItemPath.create(containerPath, PrismSchemaType.F_ENUMERATION_TYPE));
                    enumContainerWrapper.getValues().forEach(value -> value.setSelected(false));
                } catch (SchemaException e) {
                    LOGGER.error("Couldn't find container.", e);
                }
            }

            @Override
            protected void clickOnListItem(
                    LoadableDetachableModel<List<PrismContainerValueWrapper<? extends DefinitionType>>> valuesModel,
                    ListItem<PrismContainerValueWrapper<? extends DefinitionType>> item,
                    AjaxRequestTarget target) {
                @Nullable PrismItemDefinitionsTable table = getTable();
                if (table != null) {
                    if (table.isValidFormComponents(target)) {
                        super.clickOnListItem(valuesModel, item, target);
                    }
                } else {
                    super.clickOnListItem(valuesModel, item, target);
                }
            }

            protected void onClickNewDefinitionType(AjaxRequestTarget target) {
                @Nullable PrismItemDefinitionsTable table = getTable();
                if (table != null) {
                    if (table.isValidFormComponents(target)) {
                        showWizardForNewDefinition(target);
                    }
                } else {
                    showWizardForNewDefinition(target);
                }
            }

            @Override
            protected IModel<String> createItemIcon(IModel<PrismContainerValueWrapper<? extends DefinitionType>> model) {
                return () -> {
                    if (model.getObject().getDefinition().getCompileTimeClass().equals(ComplexTypeDefinitionType.class)) {
                        return GuiStyleConstants.CLASS_SCHEMA_COMPLEX_TYPE_ICON;
                    }
                    return GuiStyleConstants.CLASS_SCHEMA_ENUM_TYPE_ICON;
                };
            }

            @Override
            protected LoadableDetachableModel<String> createItemLabel(IModel<PrismContainerValueWrapper<? extends DefinitionType>> model) {
                return new LoadableDetachableModel<>() {
                    @Override
                    protected String load() {
                        DefinitionType bean = model.getObject().getRealValue();
                        String label = bean.getName().getLocalPart();
                        if (StringUtils.isNotEmpty(bean.getDisplayName())) {
                            label = LocalizationUtil.translate(model.getObject().getRealValue().getDisplayName());
                        }
                        return label;
                    }
                };
            }

            @Override
            protected boolean isNewItemButtonVisible() {
                return ComplexTypeDefinitionPanel.this.isNewDefinitionButtonVisible();
            }

            @Override
            protected IModel<String> getLabelForNewItem() {
                return ComplexTypeDefinitionPanel.this.getLabelForNewItem();
            }
        };
        panel.setOutputMarkupId(true);
        add(panel);
    }

    protected IModel<String> getLabelForNewItem() {
        return createStringResource("ComplexTypeDefinitionPanel.createNewValue");
    }

    protected boolean isNewDefinitionButtonVisible() {
        return true;
    }

    private PrismContainerValueWrapper<? extends DefinitionType> getSelectedItem() {
        try {
            PrismContainerWrapper<DefinitionType> containerWrapper =
                    getObjectWrapper().findContainer(ItemPath.create(containerPath, PrismSchemaType.F_COMPLEX_TYPE));
            PrismContainerValueWrapper<DefinitionType> foundValue = getSelectedValue(containerWrapper);
            if (foundValue != null) {
                return foundValue;
            }

            containerWrapper = getObjectWrapper().findContainer(ItemPath.create(containerPath, PrismSchemaType.F_ENUMERATION_TYPE));
            return getSelectedValue(containerWrapper);

        } catch (SchemaException e) {
            LOGGER.error("Couldn't find container.", e);
        }
        return null;
    }

    protected List<PrismContainerValueWrapper<? extends DefinitionType>> createListOfItem(IModel<String> searchItemModel) {
        try {
            List<PrismContainerValueWrapper<? extends DefinitionType>> values = new ArrayList<>();

            PrismContainerWrapper<ComplexTypeDefinitionType> complexContainerWrapper =
                    getObjectWrapper().findContainer(ItemPath.create(containerPath, PrismSchemaType.F_COMPLEX_TYPE));
            if (complexContainerWrapper != null) {
                complexContainerWrapper.getValues().forEach(value -> {
                    if (complexContainerWrapper.isReadOnly() &&
                            value.getRealValue().getItemDefinitions().isEmpty()) {
                        return;
                    }
                    values.add(value);
                });
            }

            PrismContainerWrapper<EnumerationTypeDefinitionType> enumContainerWrapper =
                    getObjectWrapper().findContainer(ItemPath.create(containerPath, PrismSchemaType.F_ENUMERATION_TYPE));
            if (enumContainerWrapper != null) {
                values.addAll(enumContainerWrapper.getValues());
            }

            if (StringUtils.isNotBlank(searchItemModel.getObject())) {
                values.removeIf(value -> !value.getRealValue().getName().getLocalPart().contains(searchItemModel.getObject()));
            }

            return values;
        } catch (SchemaException e) {
            LOGGER.error("Couldn't find container.", e);
        }
        return null;
    }

    private TabCenterTabbedPanel<ITab> createTabPanel(
            String idPanelForItem, IModel<PrismContainerValueWrapper<? extends DefinitionType>> selectedItemModel) {
        List<ITab> tabs = new ArrayList<>();
        tabs.add(createTableTab(selectedItemModel));
        tabs.add(createBasicDetailsForm(selectedItemModel));

        return new TabCenterTabbedPanel<>(idPanelForItem, tabs) {
            @Override
            protected void onClickTabPerformed(int index, Optional<AjaxRequestTarget> target) {
                @Nullable PrismItemDefinitionsTable table = getTable();
                if (table != null) {
                    if (table.isValidFormComponents(target.orElse(null))) {
                        super.onClickTabPerformed(index, target);
                    }
                } else {
                    super.onClickTabPerformed(index, target);
                }
            }

            @Override
            protected WebMarkupContainer newLink(String linkId, int index) {
                WebMarkupContainer link = super.newLink(linkId, index);
                if (index == 0) {
                    link.add(AttributeAppender.append("style", "border-top-left-radius: 0 !important;"));
                }
                return link;
            }
        };
    }

    protected IconPanelTab createBasicDetailsForm(IModel<PrismContainerValueWrapper<? extends DefinitionType>> selectedItemModel) {
        return new IconPanelTab(
                getPageBase().createStringResource(
                        "ComplexTypeDefinitionPanel.basicSettings"),
                VisibleBehaviour.ALWAYS_VISIBLE_ENABLED) {

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                ItemPanelSettings settings = new ItemPanelSettingsBuilder()
                        .visibilityHandler(getDefinitionVisibilityHandler())
                        .headerVisibility(false)
                        .build();

                VerticalFormPrismContainerValuePanel panel
                        = new VerticalFormPrismContainerValuePanel(panelId, selectedItemModel, settings) {

                    @Override
                    protected void onInitialize() {
                        super.onInitialize();
                        Component parent = get(
                                createComponentPath(
                                        ID_MAIN_CONTAINER,
                                        ID_VALUE_CONTAINER,
                                        ID_INPUT,
                                        VerticalFormDefaultContainerablePanel.ID_PROPERTIES_LABEL,
                                        VerticalFormDefaultContainerablePanel.ID_FORM_CONTAINER));
                        if (parent != null) {
                            parent.add(AttributeAppender.replace("class", "mb-0 p-3"));
                        }
                        get(ID_MAIN_CONTAINER).add(AttributeAppender.remove("class"));
                    }
                };
                panel.setOutputMarkupId(true);
                return panel;
            }

            @Override
            public IModel<String> getCssIconModel() {
                return Model.of("fa fa-rectangle-list");
            }
        };
    }

    private ItemVisibilityHandler getDefinitionVisibilityHandler() {
        return wrapper -> {
            if (wrapper.getItemName().equals(DefinitionType.F_LIFECYCLE_STATE)
                    || wrapper.getItemName().equals(DefinitionType.F_DISPLAY_ORDER)
                    || wrapper.getItemName().equals(DefinitionType.F_DISPLAY_HINT)
                    || wrapper.getItemName().equals(EnumerationTypeDefinitionType.F_BASE_TYPE)) {
                return ItemVisibility.HIDDEN;
            }
            return ItemVisibility.AUTO;
        };
    }

    protected void showWizardForNewDefinition(AjaxRequestTarget target) {
        ((PageSchema) getPageBase()).showComplexOrEnumerationTypeWizard(target);
    }

    private CountablePanelTab createTableTab(IModel<PrismContainerValueWrapper<? extends DefinitionType>> selectedItemModel) {
        return new CountablePanelTab(
                getPageBase().createStringResource(
                        "ComplexTypeDefinitionPanel.definitionTable"),
                VisibleBehaviour.ALWAYS_VISIBLE_ENABLED) {

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                if (selectedItemModel.getObject() == null) {
                    return new WebMarkupContainer(panelId);
                }

                if (selectedItemModel.getObject().getRealValue() instanceof ComplexTypeDefinitionType) {
                    PrismItemDefinitionsTable table = new PrismItemDefinitionsTable(
                            panelId,
                            getComplexTypeValueModel(selectedItemModel),
                            getPanelConfiguration()) {
                        @Override
                        protected boolean showTableAsCard() {
                            return false;
                        }

                        @Override
                        protected boolean isCreateNewObjectVisible() {
                            return isVisibleNewItemDefinitionOrEnumValueButton();
                        }
                    };
                    table.setOutputMarkupId(true);
                    return table;
                }

                if (selectedItemModel.getObject().getRealValue() instanceof EnumerationTypeDefinitionType) {
                    EnumerationValueDefinitionsTable table = new EnumerationValueDefinitionsTable(
                            panelId, getEnumTypeValueModel(selectedItemModel), getPanelConfiguration()) {
                        @Override
                        protected boolean showTableAsCard() {
                            return false;
                        }

                        @Override
                        protected boolean isCreateNewObjectVisible() {
                            return isVisibleNewItemDefinitionOrEnumValueButton();
                        }
                    };
                    table.setOutputMarkupId(true);
                    return table;
                }

                return new WebMarkupContainer(panelId);
            }

            @Override
            public IModel<String> getCssIconModel() {
                return Model.of("fa fa-table");
            }

            @Override
            public String getCount() {
                if (selectedItemModel.getObject() == null) {
                    return "0";
                }

                if (selectedItemModel.getObject().getRealValue() instanceof ComplexTypeDefinitionType complexType) {
                    return String.valueOf(complexType.getItemDefinitions().size());
                }

                if (selectedItemModel.getObject().getRealValue() instanceof EnumerationTypeDefinitionType enumType) {
                    return String.valueOf(enumType.getValues().size());
                }

                return "0";
            }
        };
    }

    protected boolean isVisibleNewItemDefinitionOrEnumValueButton() {
        return true;
    }

    private IModel<PrismContainerValueWrapper<ComplexTypeDefinitionType>> getComplexTypeValueModel(
            IModel<PrismContainerValueWrapper<? extends DefinitionType>> selectedItemModel) {
        return () -> (PrismContainerValueWrapper<ComplexTypeDefinitionType>) selectedItemModel.getObject();
    }

    private IModel<PrismContainerValueWrapper<EnumerationTypeDefinitionType>> getEnumTypeValueModel(
            IModel<PrismContainerValueWrapper<? extends DefinitionType>> selectedItemModel) {
        return () -> (PrismContainerValueWrapper<EnumerationTypeDefinitionType>) selectedItemModel.getObject();
    }

    private TabbedPanel<ITab> getTabPanel() {
        return (TabbedPanel<ITab>) getPanelForItem().getPanelForItem();
    }

    private ListItemWithPanelForItemPanel getPanelForItem() {
        return (ListItemWithPanelForItemPanel) get(ID_PANEL);
    }

    @Nullable
    private PrismItemDefinitionsTable getTable() {
        TabbedPanel<ITab> tabPanel = getTabPanel();
        Component tabComponent = tabPanel.get(TabbedPanel.TAB_PANEL_ID);
        if (tabComponent instanceof PrismItemDefinitionsTable table) {
            return table;
        }
        return null;
    }
}
