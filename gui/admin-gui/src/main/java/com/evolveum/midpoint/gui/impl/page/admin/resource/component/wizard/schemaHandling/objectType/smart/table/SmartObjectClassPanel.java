/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.table;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.CLASS_CSS;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.STYLE_CSS;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.button.DropdownButtonDto;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonPanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.tile.TemplateTilePanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.stats.action.ObjectClassStatisticsActions;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemBuilder;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectClassSizeEstimationPrecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectClassSizeEstimationType;
import com.evolveum.midpoint.xml.ns._public.prism_schema_3.ComplexTypeDefinitionType;

public class SmartObjectClassPanel<C extends PrismContainerValueWrapper<ComplexTypeDefinitionType>>
        extends TemplateTilePanel<C, SmartObjectClassTileModel<C>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_NAME = "name";
    private static final String IDD_DESCRIPTION = "description";
    private static final String ID_COUNT_TITLE = "countTitle";
    private static final String ID_COUNT = "count";
    private static final String ID_VIEW_SCHEMA_LINK = "viewSchemaLink";
    private static final String ID_SELECT_CHECKBOX = "selectCheckbox";
    private static final String ID_ACTIONS = "actions";

    IModel<PrismContainerValueWrapper<ComplexTypeDefinitionType>> selectedTileModel;

    public SmartObjectClassPanel(@NotNull String id,
            @NotNull IModel<SmartObjectClassTileModel<C>> model,
            @NotNull IModel<PrismContainerValueWrapper<ComplexTypeDefinitionType>> selectedTileModel) {
        super(id, model);
        this.selectedTileModel = selectedTileModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
    }

    @Override
    protected void initLayout() {
        initDefaultCssStyle();

        initName();
        initDescription();
        initCountTitle();
        initCount();
        initViewSchemaLink();
        initSelectRadio();
        initActionsDropdownPanel();
    }

    @Override
    protected void onConfigure() {
        super.onConfigure();

        if (atLeastOneSelected()) {
            selectIfNoneSelected();
        }

        applySelectionStyling();
    }

    private void initSelectRadio() {
        Radio<C> radio = new Radio<>(ID_SELECT_CHECKBOX, Model.of(getModelObject().getValue()));
        radio.setOutputMarkupId(true);
        add(radio);
    }

    private void initName() {
        Label title = new Label(ID_NAME, getModelObject().getName());
        title.setOutputMarkupId(true);
        add(title);
    }

    private void initDescription() {
        Label description = new Label(IDD_DESCRIPTION, getModelObject().getDescription());
        description.setOutputMarkupId(true);
        description.setOutputMarkupPlaceholderTag(true);
        description.add(new VisibleBehaviour(() -> StringUtils.isNotBlank(getModelObject().getDescription())));
        add(description);
    }

    private void initCountTitle() {
        Label countTitle = new Label(ID_COUNT_TITLE,
                createStringResource("SuggestTilePanel.count.title"));
        countTitle.setOutputMarkupId(true);
        add(countTitle);
    }

    private void initCount() {
        Label count = new Label(ID_COUNT, buildCountValueLabel());
        count.setOutputMarkupId(true);
        add(count);
    }

    private void initViewSchemaLink() {
        AjaxLinkPanel viewSchemaLink = new AjaxLinkPanel(ID_VIEW_SCHEMA_LINK,
                createStringResource("SuggestTilePanel.view.schema")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onViewSchema(target);
            }
        };
        viewSchemaLink.setOutputMarkupId(true);
        add(viewSchemaLink);
    }

    protected InlineMenuItem createStatisticsInlineMenuAction() {
        return InlineMenuItemBuilder.create()
                .icon("fa-solid fa-chart-bar")
                .label(createStringResource("SmartObjectClassPanel.statistics.title"))
                .action(new InlineMenuItemAction() {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        ObjectClassStatisticsActions.handleClick(
                                target,
                                getPageBase(),
                                getPageBase().getSmartIntegrationService(),
                                getModelObject().getResourceOid(),
                                getModelObject().getObjectClassName(),
                                false);
                    }
                })
                .buildInlineMenu();
    }

    private void initActionsDropdownPanel() {
        List<InlineMenuItem> menuItems = new ArrayList<>();
        menuItems.add(createStatisticsInlineMenuAction());

        DropdownButtonPanel inlineMenu = new DropdownButtonPanel(
                ID_ACTIONS,
                new DropdownButtonDto(null, "fa fa-ellipsis-h", null, menuItems)) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected @NotNull String getSpecialButtonClass() {
                return "btn-tool m-0";
            }

            @Override
            protected boolean hasToggleIcon() {
                return false;
            }

            @Override
            protected boolean showIcon() {
                return true;
            }
        };

        add(inlineMenu);
    }

    private void initDefaultCssStyle() {
        setOutputMarkupId(true);

        add(AttributeModifier.append(CLASS_CSS, "bg-white "
                + "d-flex flex-column align-items-center"
                + " rounded w-100 h-100 p-3 card-shadow"));

        add(AttributeModifier.append(STYLE_CSS, ""));
    }

    private void applySelectionStyling() {
        PrismContainerValueWrapper<ComplexTypeDefinitionType> selectedValue = selectedTileModel.getObject();
        PrismContainerValueWrapper<ComplexTypeDefinitionType> tileValue = getModelObject().getValue();

        if (selectedValue == null || tileValue == null) {
            return;
        }

        String defaultTileCss = getDefaultTileCss();
        String cssClass = selectedValue.equals(tileValue) ? defaultTileCss + " active" : defaultTileCss;
        add(AttributeModifier.replace(CLASS_CSS, cssClass));
    }

    private IModel<String> buildCountValueLabel() {
        ObjectClassSizeEstimationType estimatedSize = getModelObject().getEstimatedSize();
        if (estimatedSize == null || estimatedSize.getValue() == null) {
            return createStringResource("SuggestTilePanel.count.unknown");
        }
        ObjectClassSizeEstimationPrecisionType precision = estimatedSize.getPrecision();
        Integer count = estimatedSize.getValue();
        return createStringResource("ObjectClassSizeEstimationType."
                + precision.value(), count);
    }

    protected void onViewSchema(AjaxRequestTarget target) {
        // This method can be overridden to handle the view schema action
    }

    /**
     * Selects the tile if no other tile is currently selected.
     * This is useful for ensuring that at least one tile is selected
     * when the user interacts with the UI.
     */
    private void selectIfNoneSelected() {
        PrismContainerValueWrapper<ComplexTypeDefinitionType> currentSelection = selectedTileModel.getObject();
        PrismContainerValueWrapper<ComplexTypeDefinitionType> thisTile = getModelObject().getValue();

        if (currentSelection == null && thisTile != null) {
            selectedTileModel.setObject(thisTile);
        }
    }

    protected boolean atLeastOneSelected() {
        return true;
    }

    protected String getDefaultTileCss() {
        return "simple-tile selectable clickable-by-enter tile-panel d-flex flex-column align-items-center "
                + "rounded p-4 justify-content-center";
    }
}
