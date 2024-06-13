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
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanel;
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
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.TabCenterTabbedPanel;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaType;
import com.evolveum.midpoint.xml.ns._public.prism_schema_3.ComplexTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.prism_schema_3.PrismItemDefinitionType;
import com.evolveum.midpoint.xml.ns._public.prism_schema_3.PrismSchemaType;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
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
public class ComplexTypeDefinitionPanel<S extends SchemaType, ADM extends AssignmentHolderDetailsModel<S>> extends AbstractObjectMainPanel<S, ADM> {

    @Serial private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ComplexTypeDefinitionPanel.class);

    private static final String ID_NEW_VALUE_BUTTON = "newValueButton";
    private static final String ID_LIST_OF_VALUES = "listOfValues";
    private static final String ID_VALUE = "value";
    private static final String ID_TAB_PANEL = "tabPanel";

    private static final String DEFAULT_TITLE = "ComplexTypeDefinitionPanel.title";

    private LoadableDetachableModel<PrismContainerValueWrapper<ComplexTypeDefinitionType>> complexTypeModel;

    public ComplexTypeDefinitionPanel(String id, ADM model, ContainerPanelConfigurationType config) {
        super(id, model, null);
    }

    @Override
    protected void onInitialize() {
        initComplexTypeModel();
        super.onInitialize();
    }

    private void initComplexTypeModel() {
        if (complexTypeModel == null) {
            complexTypeModel = new LoadableDetachableModel<>() {
                @Override
                protected PrismContainerValueWrapper<ComplexTypeDefinitionType> load() {
                    try {
                        PrismContainerWrapper<ComplexTypeDefinitionType> containerWrapper =
                                getObjectWrapper().findContainer(ItemPath.create(WebPrismUtil.PRISM_SCHEMA, PrismSchemaType.F_COMPLEX_TYPE));
                        if (containerWrapper != null) {
                            Optional<PrismContainerValueWrapper<ComplexTypeDefinitionType>> complexTypeValue =
                                    containerWrapper.getValues().stream()
                                            .filter(PrismContainerValueWrapper::isSelected)
                                            .findFirst();
                            if (complexTypeValue.isPresent()) {
                                complexTypeValue.get().setExpanded(true);
                                return complexTypeValue.get();
                            }
                        }
                    } catch (SchemaException e) {
                        LOGGER.error("Couldn't find complex type container.");
                    }
                    return null;
                }
            };
        }
    }

    @Override
    protected void initLayout() {
        AjaxIconButton newValueButton = new AjaxIconButton(
                ID_NEW_VALUE_BUTTON,
                Model.of(GuiStyleConstants.CLASS_PLUS_CIRCLE),
                createStringResource("ComplexTypeDefinitionPanel.createNewValue")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onClickNewComplexType(target);
            }
        };
        newValueButton.showTitleAsLabel(true);
        add(newValueButton);

        createValueList();
        createTabPanel();
    }

    private void createTabPanel() {
        List<ITab> tabs = new ArrayList<>();
        tabs.add(createTableTab());
        tabs.add(createBasicDetailsForm());

        TabCenterTabbedPanel<ITab> tabPanel = new TabCenterTabbedPanel<>(ID_TAB_PANEL, tabs) {
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
        tabPanel.setOutputMarkupId(true);
        add(tabPanel);
    }

    protected IconPanelTab createBasicDetailsForm() {
        return new IconPanelTab(
                getPageBase().createStringResource(
                        "ComplexTypeDefinitionPanel.basicSettings"),
                VisibleBehaviour.ALWAYS_VISIBLE_ENABLED) {

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                ItemPanelSettings settings = new ItemPanelSettingsBuilder()
                        .visibilityHandler(getComplexTypeVisibilityHandler())
                        .headerVisibility(false)
                        .build();

                VerticalFormPrismContainerValuePanel panel
                        = new VerticalFormPrismContainerValuePanel(panelId, complexTypeModel, settings){

                    @Override
                    protected void onInitialize() {
                        super.onInitialize();
                        Component parent = get(
                                createComponentPath(
                                        ID_VALUE_FORM,
                                        ID_VALUE_CONTAINER,
                                        ID_INPUT,
                                        VerticalFormDefaultContainerablePanel.ID_PROPERTIES_LABEL,
                                        VerticalFormDefaultContainerablePanel.ID_FORM_CONTAINER));
                        if (parent != null) {
                            parent.add(AttributeAppender.replace("class", "mb-0 p-3"));
                        }
                        get(ID_VALUE_FORM).add(AttributeAppender.remove("class"));
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

    private ItemVisibilityHandler getComplexTypeVisibilityHandler() {
        return wrapper -> {
            if (wrapper.getItemName().equals(MappingType.F_LIFECYCLE_STATE)) {
                return ItemVisibility.HIDDEN;
            }
            return ItemVisibility.AUTO;
        };
    }

    protected void onClickNewComplexType(AjaxRequestTarget target) {
        @Nullable PrismItemDefinitionsTable table = getTable();
        if (table != null) {
            if (table.isValidFormComponents(target)) {
                ((PageSchema)getPageBase()).showComplexTypeWizard(target);
            }
        } else {
            ((PageSchema)getPageBase()).showComplexTypeWizard(target);
        }
    }

    private void createValueList() {
        LoadableDetachableModel<List<PrismContainerValueWrapper<ComplexTypeDefinitionType>>> valuesModel = new LoadableDetachableModel<>() {
            @Override
            protected List<PrismContainerValueWrapper<ComplexTypeDefinitionType>> load() {
                try {
                    PrismContainerWrapper<ComplexTypeDefinitionType> containerWrapper =
                            getObjectWrapper().findContainer(ItemPath.create(WebPrismUtil.PRISM_SCHEMA, PrismSchemaType.F_COMPLEX_TYPE));
                    if (containerWrapper != null) {
                        return containerWrapper.getValues();
                    }
                } catch (SchemaException e) {
                    LOGGER.error("Couldn't find complex type container.");
                }
                return null;
            }
        };

        if (!valuesModel.getObject().isEmpty() && !valuesModel.getObject().stream().anyMatch(PrismContainerValueWrapper::isSelected)) {
            valuesModel.getObject().get(0).setSelected(true);
        }

        ListView values = new ListView<>(ID_LIST_OF_VALUES, valuesModel) {
            @Override
            protected void populateItem(ListItem<PrismContainerValueWrapper<ComplexTypeDefinitionType>> item) {
                IModel<String> labelModel = new LoadableDetachableModel<>() {
                    @Override
                    protected String load() {
                        ComplexTypeDefinitionType bean = item.getModelObject().getRealValue();
                        String label = bean.getName().getLocalPart();
                        if (StringUtils.isNotEmpty(bean.getDisplayName())) {
                            label = LocalizationUtil.translate(item.getModelObject().getRealValue().getDisplayName());
                        }
                        return label;
                    }
                };

                IModel<String> iconModel = () -> {
                    if (item.getModelObject() instanceof ComplexTypeDefinitionType) {
                        return GuiStyleConstants.CLASS_SCHEMA_COMPLEX_TYPE_ICON;
                    }
                    return GuiStyleConstants.CLASS_SCHEMA_ENUM_TYPE_ICON;
                };
                AjaxIconButton value = new AjaxIconButton(ID_VALUE, iconModel, labelModel) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        @Nullable PrismItemDefinitionsTable table = getTable();
                        if (table != null) {
                            if (table.isValidFormComponents(target)) {
                                clickOnListItem(valuesModel, item, target);
                            }
                        } else {
                            clickOnListItem(valuesModel, item, target);
                        }

                    }
                };
                value.add(AttributeAppender.append("class", () -> item.getModelObject().isSelected() ? "active-second-level" : ""));
                value.showTitleAsLabel(true);
                item.add(value);
            }
        };
        values.setOutputMarkupId(true);
        add(values);
    }

    private void clickOnListItem(LoadableDetachableModel<List<PrismContainerValueWrapper<ComplexTypeDefinitionType>>> valuesModel, ListItem<PrismContainerValueWrapper<ComplexTypeDefinitionType>> item, AjaxRequestTarget target) {
        valuesModel.getObject().forEach(value -> value.setSelected(false));
        item.getModelObject().setSelected(true);
        complexTypeModel.detach();
        target.add(ComplexTypeDefinitionPanel.this);
        target.add(ComplexTypeDefinitionPanel.this.get(ID_TAB_PANEL));
    }

    private CountablePanelTab createTableTab() {
        return new CountablePanelTab(
                getPageBase().createStringResource(
                        "ComplexTypeDefinitionPanel.definitionTable"),
                VisibleBehaviour.ALWAYS_VISIBLE_ENABLED) {

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                PrismItemDefinitionsTable table = new PrismItemDefinitionsTable(panelId, complexTypeModel, getPanelConfiguration()){
                    @Override
                    protected boolean showTableAsCard() {
                        return false;
                    }
                };
                table.setOutputMarkupId(true);
                return table;
            }

            @Override
            public IModel<String> getCssIconModel() {
                return Model.of("fa fa-table");
            }

            @Override
            public String getCount() {
                return String.valueOf(complexTypeModel.getObject().getRealValue().getItemDefinitions().size());
            }
        };
    }

    private TabbedPanel<ITab> getTabPanel() {
        return (TabbedPanel<ITab>) get(ID_TAB_PANEL);
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

    private String getTitle() {
        ContainerPanelConfigurationType panelConfig = getPanelConfiguration();
        if (panelConfig == null) {
            return createStringResource(DEFAULT_TITLE).getString();
        }
        PolyStringType label = GuiDisplayTypeUtil.getLabel(panelConfig.getDisplay());
        if (label == null) {
            return createStringResource(DEFAULT_TITLE).getString();
        }
        return LocalizationUtil.translatePolyString(label);
    }

    private String getIcon() {
        ContainerPanelConfigurationType panelConfig = getPanelConfiguration();
        if (panelConfig == null) {
            return GuiStyleConstants.CLASS_CIRCLE_FULL;
        }
        String iconCssClass = GuiDisplayTypeUtil.getIconCssClass(panelConfig.getDisplay());
        if (StringUtils.isEmpty(iconCssClass)) {
            return GuiStyleConstants.CLASS_CIRCLE_FULL;
        }
        return iconCssClass;
    }
}