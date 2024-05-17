/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.schema.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaType;
import com.evolveum.midpoint.xml.ns._public.common.prism_schema_3.ComplexTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.prism_schema_3.PrismItemDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.prism_schema_3.PrismSchemaType;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.w3c.dom.Attr;

import java.io.Serial;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@PanelType(name = "complex-type-definitions")
@PanelInstance(
        identifier = "complex-type-definitions",
        applicableForType = SchemaType.class,
        display = @PanelDisplay(label = "ComplexTypeDefinitionPanel.title", icon = GuiStyleConstants.CLASS_CIRCLE_FULL, order = 25))
public class ComplexTypeDefinitionPanel<S extends SchemaType, ADM extends AssignmentHolderDetailsModel<S>> extends AbstractObjectMainPanel<S, ADM> {

    @Serial private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ComplexTypeDefinitionPanel.class);

    private static final String ID_ICON = "icon";
    private static final String ID_TITLE = "title";
    private static final String ID_NEW_VALUE_BUTTON = "newValueButton";
    private static final String ID_LIST_OF_VALUES = "listOfValues";
    private static final String ID_VALUE = "value";
    private static final String ID_TABLE = "table";

    private static final String DEFAULT_TITLE = "ComplexTypeDefinitionPanel.title";

    public static final ItemName PRISM_SCHEMA = new ItemName(PrismSchemaType.F_COMPLEX_TYPE.getNamespaceURI(), "prismSchema");

    public ComplexTypeDefinitionPanel(String id, ADM model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        getTable().getTable().setShowAsCard(false);
    }

    @Override
    protected void initLayout() {
        WebMarkupContainer icon = new WebMarkupContainer(ID_ICON);
        icon.add(AttributeAppender.append("class", getIcon()));
        add(icon);

        add(new Label(ID_TITLE, getTitle()));

        AjaxIconButton newValueButton = new AjaxIconButton(
                ID_NEW_VALUE_BUTTON,
                Model.of(GuiStyleConstants.CLASS_PLUS_CIRCLE),
                createStringResource("SchemaPanel.createNewValue")) {
            @Override
            public void onClick(AjaxRequestTarget target) {

            }
        };
        newValueButton.showTitleAsLabel(true);
        add(newValueButton);

        createValueList();

        createTable();
    }

    private void createValueList() {
        LoadableDetachableModel<List<PrismContainerValueWrapper<ComplexTypeDefinitionType>>> valuesModel = new LoadableDetachableModel<>() {
            @Override
            protected List<PrismContainerValueWrapper<ComplexTypeDefinitionType>> load() {
                try {
                    PrismContainerWrapper<ComplexTypeDefinitionType> containerWrapper =
                            getObjectWrapper().findContainer(ItemPath.create(PRISM_SCHEMA, PrismSchemaType.F_COMPLEX_TYPE));
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

                AjaxLinkPanel value = new AjaxLinkPanel(ID_VALUE, labelModel) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        valuesModel.getObject().forEach(value -> value.setSelected(false));
                        item.getModelObject().setSelected(true);
                        target.add(ComplexTypeDefinitionPanel.this.get(ID_LIST_OF_VALUES));
                        target.add(ComplexTypeDefinitionPanel.this.get(ID_TABLE));
                    }
                };
                value.add(AttributeAppender.append("class", () -> item.getModelObject().isSelected() ? "active" : ""));
                item.add(value);
            }
        };
        values.setOutputMarkupId(true);
        add(values);
    }

    private void createTable() {
        MultivalueContainerListPanel<PrismItemDefinitionType> table = new MultivalueContainerListPanel<>(
                ID_TABLE, PrismItemDefinitionType.class, getPanelConfiguration()) {
            @Override
            protected void editItemPerformed(
                    AjaxRequestTarget target,
                    IModel<PrismContainerValueWrapper<PrismItemDefinitionType>> rowModel,
                    List<PrismContainerValueWrapper<PrismItemDefinitionType>> listItems) {
            }

            @Override
            protected boolean isCreateNewObjectVisible() {
                return false;
            }

            @Override
            protected IModel<PrismContainerWrapper<PrismItemDefinitionType>> getContainerModel() {
                return new LoadableDetachableModel<>() {
                    @Override
                    protected PrismContainerWrapper<PrismItemDefinitionType> load() {
                        try {
                            PrismContainerWrapper<ComplexTypeDefinitionType> containerWrapper =
                                    getObjectWrapper().findContainer(ItemPath.create(PRISM_SCHEMA, PrismSchemaType.F_COMPLEX_TYPE));
                            if (containerWrapper != null) {
                                Optional<PrismContainerValueWrapper<ComplexTypeDefinitionType>> complexTypeValue =
                                        containerWrapper.getValues().stream()
                                                .filter(PrismContainerValueWrapper::isSelected)
                                                .findFirst();
                                if (complexTypeValue.isPresent()) {
                                    return complexTypeValue.get().findContainer(ComplexTypeDefinitionType.F_ITEM_DEFINITIONS);
                                }
                            }
                        } catch (SchemaException e) {
                            LOGGER.error("Couldn't find complex type container.");
                        }
                        return null;
                    }
                };
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return null;
            }

            @Override
            protected IColumn<PrismContainerValueWrapper<PrismItemDefinitionType>, String> createCheckboxColumn() {
                return new CheckBoxHeaderColumn<>();
            }

            @Override
            protected List<IColumn<PrismContainerValueWrapper<PrismItemDefinitionType>, String>> createDefaultColumns() {
                LoadableDetachableModel<PrismContainerDefinition<PrismItemDefinitionType>> defModel = new LoadableDetachableModel() {
                    @Override
                    protected PrismContainerDefinition<PrismItemDefinitionType> load() {
                        PrismContainerDefinition<PrismSchemaType> schemaDef =
                                PrismContext.get().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(PrismSchemaType.class);
                        return schemaDef.findContainerDefinition(
                                ItemPath.create(PrismSchemaType.F_COMPLEX_TYPE, ComplexTypeDefinitionType.F_ITEM_DEFINITIONS));
                    }
                };

                return Arrays.asList(
                        new PrismPropertyWrapperColumn<>(defModel, PrismItemDefinitionType.F_NAME,
                                AbstractItemWrapperColumn.ColumnType.LINK, getPageBase()) {

                            @Override
                            protected void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<PrismItemDefinitionType>> model) {
                                editItemPerformed(target, model, getSelectedItems());
                            }
                        },
                        new PrismPropertyWrapperColumn<>(defModel, PrismItemDefinitionType.F_TYPE,
                                AbstractItemWrapperColumn.ColumnType.STRING, getPageBase()),
                        new PrismPropertyWrapperColumn<>(defModel, PrismItemDefinitionType.F_DISPLAY_NAME,
                                AbstractItemWrapperColumn.ColumnType.VALUE, getPageBase()),
                        new PrismPropertyWrapperColumn<>(defModel, PrismItemDefinitionType.F_DISPLAY_ORDER,
                                AbstractItemWrapperColumn.ColumnType.VALUE, getPageBase())
                );
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                return getDefaultMenuActions();
            }
        };
        table.setOutputMarkupId(true);
        add(table);
    }

    private MultivalueContainerListPanel getTable() {
        return (MultivalueContainerListPanel) get(ID_TABLE);
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
