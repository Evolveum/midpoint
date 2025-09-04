/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 *    This work is dual-licensed under the Apache License 2.0
 *    and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.correlation;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.impl.component.data.provider.ListDataProvider;
import com.evolveum.midpoint.gui.impl.component.input.expression.ExpressionPanel;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationWrapperUtils.getSingleInboundMapping;

public class CorrelationAddMappingConfirmationPanel extends ConfirmationPanel {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_SUBTITLE = "subtitle";
    private static final String ID_TABLE = "table";
    IModel<List<ResourceAttributeDefinitionType>> definitionsModel;

    public CorrelationAddMappingConfirmationPanel(
            @NotNull String id,
            @Nullable IModel<String> message,
            @NotNull IModel<List<ResourceAttributeDefinitionType>> definitionsModel) {
        super(id, message);
        this.definitionsModel = definitionsModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initSubTitleComponent();

        initBodyComponent();

    }

    protected void initBodyComponent() {

        ListDataProvider<ResourceAttributeDefinitionType> provider =
                new ListDataProvider<>(this, Model.ofList(definitionsModel.getObject()));

        BoxedTablePanel<ResourceAttributeDefinitionType> table = new BoxedTablePanel<>(ID_TABLE, provider, createColumns()) {

            @Contract(pure = true)
            @Override
            protected @Nullable String getPaginationCssClass() {
                return null;
            }

            @Contract(pure = true)
            @Override
            protected @NotNull String getAdditionalFooterCssClasses() {
                return "bg-white border-top";
            }

            @Contract(pure = true)
            @Override

            public @NotNull String getAdditionalBoxCssClasses() {
                return "table-td-middle";
            }

            @Override
            public String getTableAdditionalCssClasses() {
                return super.getTableAdditionalCssClasses();
            }

            @Override
            public boolean isNavigatorPanelVisible() {
                return provider.size() > 5;
            }

            @Override
            protected boolean isPagingSizePanelVisible() {
                return provider.size() > 5;
            }
        };
        table.setOutputMarkupId(true);
        table.getDataTable().setItemsPerPage(5);
        add(table);
    }

    private @NotNull List<IColumn<ResourceAttributeDefinitionType, String>> createColumns() {
        List<IColumn<ResourceAttributeDefinitionType, String>> columns = new ArrayList<>();

        columns.add(new AbstractColumn<>(createStringResource("CorrelationAddMappingConfirmationPanel.column.name")) {
            @Override
            public void populateItem(Item<ICellPopulator<ResourceAttributeDefinitionType>> item, String id,
                    IModel<ResourceAttributeDefinitionType> rowModel) {
                InboundMappingType singleInboundMapping = getSingleInboundMapping(rowModel.getObject());
                String name = singleInboundMapping != null && singleInboundMapping.getName() != null
                        ? singleInboundMapping.getName() : "";
                Label nameLabel = new Label(id, Model.of(name));
                item.add(nameLabel);
            }
        });

        columns.add(new AbstractColumn<>(createStringResource("CorrelationAddMappingConfirmationPanel.column.resourceAttribute")) {
            @Override
            public void populateItem(Item<ICellPopulator<ResourceAttributeDefinitionType>> item, String id,
                    IModel<ResourceAttributeDefinitionType> rowModel) {
                Label nameLabel = new Label(id, new PropertyModel<>(rowModel, ResourceAttributeDefinitionType.F_REF.getLocalPart()));
                item.add(nameLabel);
            }
        });

        initIconColumnSeparator(columns, "fa fa-minus text-secondary");

        columns.add(new AbstractColumn<>(createStringResource("CorrelationAddMappingConfirmationPanel.column.expression")) {
            @Override
            public void populateItem(Item<ICellPopulator<ResourceAttributeDefinitionType>> item, String id,
                    IModel<ResourceAttributeDefinitionType> rowModel) {
                InboundMappingType singleInboundMapping = getSingleInboundMapping(rowModel.getObject());
                if (singleInboundMapping == null) {
                    item.add(new Label(id, ""));
                    return;
                }
                ExpressionType expression = singleInboundMapping.getExpression();
                //TODO
                ExpressionPanel expressionPanel = new ExpressionPanel(id, () -> expression != null ? expression : new ExpressionType());
//                Label expressionPanel = new Label(id, () -> {
//                    if (expression != null && expression.getName() != null) {
//                        return expression.getName();
//                    } else {
//                        return "-";
//                    }
//                });
                expressionPanel.setOutputMarkupId(true);
                item.add(expressionPanel);
            }
        });

        initIconColumnSeparator(columns, "fa fa-arrow-right-long text-secondary");

        columns.add(new AbstractColumn<>(createStringResource("CorrelationAddMappingConfirmationPanel.column.target")) {
            @Override
            public void populateItem(Item<ICellPopulator<ResourceAttributeDefinitionType>> item, String id,
                    IModel<ResourceAttributeDefinitionType> rowModel) {
                InboundMappingType singleInboundMapping = getSingleInboundMapping(rowModel.getObject());
                if (singleInboundMapping == null) {
                    item.add(new Label(id, ""));
                    return;
                }

                VariableBindingDefinitionType target = singleInboundMapping.getTarget();
                Label targetLabel = new Label(id, target != null && target.getPath() != null
                        ? target.getPath().toString() : "");
                item.add(targetLabel);
            }
        });

        return columns;
    }

    private static void initIconColumnSeparator(@NotNull List<IColumn<ResourceAttributeDefinitionType, String>> columns, String value) {
        columns.add(new IconColumn<>(Model.of()) {
            @Override
            protected DisplayType getIconDisplayType(IModel<ResourceAttributeDefinitionType> rowModel) {
                return new DisplayType().beginIcon().cssClass(value).end();
            }

            @Override
            public String getCssClass() {
                return "px-0";
            }
        });
    }

    private void initSubTitleComponent() {
        Label subtitleLabel = createLabelComponent(getSubtitleModel());
        subtitleLabel.setOutputMarkupId(true);
        add(subtitleLabel);
    }

    @Override
    protected IModel<String> createYesLabel() {
        return getAllowAndContinueModel();
    }

    @Override
    protected IModel<String> createNoLabel() {
        return getCancelButtonModel();
    }

    /**
     * Creates a label component with common output markup settings.
     *
     * @param title The label model (typically a localized string resource)
     * @return Configured {@link Label} instance
     */
    private @NotNull Label createLabelComponent(StringResourceModel title) {
        Label label = new Label(CorrelationAddMappingConfirmationPanel.ID_SUBTITLE, title);
        label.setOutputMarkupId(true);
        label.setOutputMarkupPlaceholderTag(true);
        return label;
    }

    protected Component createYesButton() {
        AjaxIconButton yesButton = new AjaxIconButton(ID_YES, Model.of("fa fa-check"), createYesLabel()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                getPageBase().hideMainPopup(target);
                yesPerformed(target);
            }
        };
        yesButton.showTitleAsLabel(true);
        yesButton.add(new VisibleBehaviour(this::isYesButtonVisible));
        return yesButton;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("CorrelationAddMappingConfirmationPanel.title", this, null);
    }

    @Override
    public @Nullable Component getTitleComponent() {
        Label titleComponent = new Label(ID_TITLE, getTitle());
        titleComponent.setOutputMarkupId(true);
        titleComponent.add(AttributeModifier.append("class", "align-self-center"));
        return titleComponent;
    }

    @Override
    public IModel<String> getTitleIconClass() {
        return Model.of(GuiStyleConstants.CLASS_INFO_CIRCLE + " text-info fa-xl");
    }

    private StringResourceModel getSubtitleModel() {
        return createStringResource("CorrelationAddMappingConfirmationPanel.subtitle", this, null);
    }

    private StringResourceModel getCancelButtonModel() {
        return createStringResource("CorrelationAddMappingConfirmationPanel.cancel", this, null);
    }

    private StringResourceModel getAllowAndContinueModel() {
        return createStringResource("CorrelationAddMappingConfirmationPanel.acceptAndAdd", this, null);
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
    }

    @Override
    public int getWidth() {
        return 40;
    }

    @Override
    public int getHeight() {
        return 30;
    }

    @Override
    public String getWidthUnit() {
        return "%";
    }

    @Override
    public String getHeightUnit() {
        return "%";
    }

    @Override
    protected String getNoButtonCssClass() {
        return null;
    }
}
