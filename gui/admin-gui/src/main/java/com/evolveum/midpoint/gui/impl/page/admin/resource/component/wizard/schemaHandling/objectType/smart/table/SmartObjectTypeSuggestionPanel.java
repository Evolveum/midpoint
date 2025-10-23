/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.table;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonDto;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.component.CompareContainerPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormPrismPropertyValuePanel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.dialog.RequestDetailsConfirmationPanel;
import com.evolveum.midpoint.web.component.dialog.RequestDetailsRecordDto;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.tile.TemplateTilePanel;

import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationStatusInfoUtils.loadObjectClassObjectTypeSuggestions;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils.*;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.CLASS_CSS;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.STYLE_CSS;

public abstract class SmartObjectTypeSuggestionPanel<C extends PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>>
        extends TemplateTilePanel<C, SmartObjectTypeSuggestionTileModel<C>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_RADIO = "selectRadio";
    private static final String ID_TITLE = "title";
    private static final String ID_DESC = "description";
    private static final String ID_CHIPS = "chips";
    private static final String ID_CHIP = "chip";
    private static final String ID_TOGGLE = "toggleFilter";
    private static final String ID_FILTER_CTN = "filterContainer";
    private static final String ID_ACE = "aceEditorFilter";
    private static final String ID_ACE_BASE = "aceEditorBaseFilter";
    private static final String ID_MORE_ACTIONS = "moreActions";

    private static final String ID_FILTER_LABEL = "filterLabel";
    private static final String ID_BASE_CONTEXT_LABEL = "baseContextLabel";
    private static final String ID_BASE_CONTEXT_OBJECT_CLASS_LABEL = "baseContextObjectClassLabel";
    private static final String ID_BASE_CONTEXT_OBJECT_CLASS_VALUE = "baseContextObjectClassValue";

    IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> selectedTileModel;
    IModel<Boolean> forceDeleteEnabled;

    private static final String OP_DETERMINE_STATUS =
            SmartObjectTypeSuggestionPanel.class.getName() + ".determineStatus";

    boolean isShowFilter = false;

    public SmartObjectTypeSuggestionPanel(@NotNull String id,
            @NotNull IModel<SmartObjectTypeSuggestionTileModel<C>> model,
            @NotNull IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> selectedTileModel,
            @NotNull IModel<Boolean> forceDeleteEnabled) {
        super(id, model);
        this.selectedTileModel = selectedTileModel;
        this.forceDeleteEnabled = forceDeleteEnabled;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        buildPanel();
    }

    @Override
    protected void initLayout() {
        // No additional layout initialization needed
    }

    @Override
    protected void onConfigure() {
        super.onConfigure();
        if (atLeastOneSelected()) {
            selectIfNoneSelected();
        }
        applySelectionStyling();
    }

    protected void buildPanel() {
        initDefaultCssStyle();

        initTitle();
        initDescription();
        initSelectRadio();
        initMoreActionsPanel();
        initChipsInfoPanel();
        initFilterPart();
    }

    private void initFilterPart() {
        WebMarkupContainer filterCtn = new WebMarkupContainer(ID_FILTER_CTN);
        filterCtn.setOutputMarkupId(true);
        filterCtn.add(new VisibleBehaviour(() -> isShowFilter));
        add(filterCtn);

        List<PrismPropertyValueWrapper<Object>> filterPropertyValueWrapper = getModelObject().getFilterPropertyValueWrapper();

        Label filterLabel = new Label(ID_FILTER_LABEL, createStringResource("SmartSuggestObjectTypeTilePanel.filter"));
        filterLabel.setOutputMarkupId(true);
        filterLabel.add(new VisibleBehaviour(this::isFilterExists));
        filterCtn.add(filterLabel);

        RepeatingView filterPanels = new RepeatingView(ID_ACE);
        populatePropertyPanels(filterPropertyValueWrapper, filterPanels);
        filterPanels.add(new VisibleBehaviour(this::isFilterExists));
        filterCtn.add(filterPanels);

        Label baseContextFilterLabel = new Label(ID_BASE_CONTEXT_LABEL,
                createStringResource("SmartSuggestObjectTypeTilePanel.base.context.filter"));
        baseContextFilterLabel.setOutputMarkupId(true);
        baseContextFilterLabel.add(new VisibleBehaviour(this::isBaseContextFilterVisible));
        filterCtn.add(baseContextFilterLabel);

        List<PrismPropertyValueWrapper<Object>> baseContexFilterPropertyValueWrapper = getModelObject()
                .getBaseContexFilterPropertyValueWrapper(ResourceObjectReferenceType.F_FILTER);
        RepeatingView baseContextFilterPanels = new RepeatingView(ID_ACE_BASE);
        populatePropertyPanels(baseContexFilterPropertyValueWrapper, baseContextFilterPanels);
        baseContextFilterPanels.add(new VisibleBehaviour(this::isBaseContextFilterVisible));
        filterCtn.add(baseContextFilterPanels);

        Label baseContextObjectClassLabel = new Label(ID_BASE_CONTEXT_OBJECT_CLASS_LABEL,
                createStringResource("SmartSuggestObjectTypeTilePanel.base.context.object.class"));
        baseContextObjectClassLabel.setOutputMarkupId(true);
        baseContextObjectClassLabel.add(new VisibleBehaviour(this::isBaseContextFilterVisible));
        filterCtn.add(baseContextObjectClassLabel);

        List<PrismPropertyValueWrapper<Object>> baseContexFilterObjectClassPropertyValueWrapper1 = getModelObject()
                .getBaseContexFilterPropertyValueWrapper(ResourceObjectReferenceType.F_OBJECT_CLASS);
        RepeatingView baseContextFilterObjectClassPanels = new RepeatingView(ID_BASE_CONTEXT_OBJECT_CLASS_VALUE);
        populateObjectClassPropertyPanels(baseContexFilterObjectClassPropertyValueWrapper1, baseContextFilterObjectClassPanels);
        baseContextFilterObjectClassPanels.add(new VisibleBehaviour(this::isBaseContextFilterVisible));
        filterCtn.add(baseContextFilterObjectClassPanels);

        add(createTogglePanel(filterCtn));
    }

    private @NotNull AjaxIconButton createTogglePanel(WebMarkupContainer filterCtn) {
        AjaxIconButton togglePanel = new AjaxIconButton(ID_TOGGLE,
                () -> isShowFilter
                        ? "fa fa-chevron-up"
                        : "fa fa-chevron-down", () -> isShowFilter
                ? createStringResource("SmartSuggestObjectTypeTilePanel.hide.filter").getString()
                : createStringResource("SmartSuggestObjectTypeTilePanel.show.filter").getString()) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(@NotNull AjaxRequestTarget target) {
                isShowFilter = !isShowFilter;
                target.add(filterCtn.getParent(), this);
            }
        };
        togglePanel.setOutputMarkupId(true);
        togglePanel.showTitleAsLabel(true);
        togglePanel.add(AttributeModifier.append("class", "flex-row-reverse"));
        togglePanel.add(new VisibleBehaviour(this::isAnyFilterExists));
        return togglePanel;
    }

    private static void populatePropertyPanels(
            @NotNull List<PrismPropertyValueWrapper<Object>> filterPropertyValueWrapper,
            @NotNull RepeatingView filterPanels) {
        for (PrismPropertyValueWrapper<Object> valueWrapper : filterPropertyValueWrapper) {
            VerticalFormPrismPropertyValuePanel<?> valuePanel = new VerticalFormPrismPropertyValuePanel<>(filterPanels.newChildId(),
                    Model.of(valueWrapper), null);
            valuePanel.setOutputMarkupId(true);
            valuePanel.setEnabled(false);
            filterPanels.add(valuePanel);
        }
    }

    private static void populateObjectClassPropertyPanels(
            @NotNull List<PrismPropertyValueWrapper<Object>> filterPropertyValueWrapper,
            @NotNull RepeatingView filterPanels) {
        for (PrismPropertyValueWrapper<Object> valueWrapper : filterPropertyValueWrapper) {
            if (valueWrapper.getRealValue() == null) {
                Label valuePanel = new Label(filterPanels.newChildId(), "N/A");
                valuePanel.setOutputMarkupId(true);
                valuePanel.setEnabled(false);
                filterPanels.add(valuePanel);
                continue;
            }
            QName realValue = (QName) valueWrapper.getRealValue();
            Label valuePanel = new Label(filterPanels.newChildId(), realValue.getLocalPart());
            valuePanel.setOutputMarkupId(true);
            valuePanel.setEnabled(false);
            filterPanels.add(valuePanel);
        }
    }

    private void initMoreActionsPanel() {
        DropdownButtonPanel buttonPanel = new DropdownButtonPanel(ID_MORE_ACTIONS, new DropdownButtonDto(
                null, "fa-ellipsis-h ml-1", null, buildMoreActionsMenuItems())) {
            @Override
            protected boolean hasToggleIcon() {
                return false;
            }

            @Override
            protected boolean showIcon() {
                return true;
            }

            @Contract(pure = true)
            @Override
            protected @NotNull String getSpecialButtonClass() {
                return " px-1 py-0 ";
            }
        };
        buttonPanel.setOutputMarkupId(true);
        add(buttonPanel);
    }

    private @NotNull List<InlineMenuItem> buildMoreActionsMenuItems() {
        List<InlineMenuItem> items = new ArrayList<>();
        items.add(createCompareWithExistingItemMenu());
        items.add(createDeleteItemMenu());
        return items;
    }

    protected ButtonInlineMenuItem createDeleteItemMenu() {
        return new ButtonInlineMenuItem(createStringResource("SmartSuggestObjectTypeTilePanel.discard.suggestion")) {
            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_ICON_TRASH);
            }

            @Override
            public IModel<String> getAdditionalCssClass() {
                return Model.of("text-danger");
            }

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new InlineMenuItemAction() {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (forceDeleteEnabled.getObject()) {
                            performOnDelete(target);
                            return;
                        }

                        performDeleteConfirmationAction(target);
                    }
                };
            }
        };
    }

    private @NotNull ButtonInlineMenuItem createCompareWithExistingItemMenu() {
        return new ButtonInlineMenuItem(createStringResource("SmartSuggestObjectTypeTilePanel.compare.with.existing")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder("fa fa-balance-scale");
            }

            @Override
            public InlineMenuItemAction initAction() {
                List<ItemPath> requiredPaths = getDefaultObjectTypeComparePaths();

                return new InlineMenuItemAction() {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        C selectedDefinition = getModelObject().getValue();
                        QName targetObjectClass = selectedDefinition.getRealValue()
                                .getDelineation()
                                .getObjectClass();

                        var existingObjectClassDefs = getExistingObjectTypeDefinitions(targetObjectClass);
                        var compareDto = createCompareObjectDto(selectedDefinition, existingObjectClassDefs, requiredPaths);
                        var comparePanel = new CompareContainerPanel<>(getPageBase().getMainPopupBodyId(), () -> compareDto);
                        getPageBase().showMainPopup(comparePanel, target);
                    }
                };
            }
        };
    }

    protected void performDeleteConfirmationAction(AjaxRequestTarget target) {

        List<RequestDetailsRecordDto.RequestRecord> records = List.of(
                new RequestDetailsRecordDto.RequestRecord(
                        getString("RequestDetailsConfirmationPanel.request.details.title"),
                        null,
                        forceDeleteEnabled,
                        null
                )
        );

        RequestDetailsConfirmationPanel dialog = new RequestDetailsConfirmationPanel(getPageBase().getMainPopupBodyId(),
                () -> new RequestDetailsRecordDto(createStringResource("RequestDetailsConfirmationPanel.discard.suggestion"),
                        records) {
                    @Override
                    public IModel<String> getRequestLabelModel(@NotNull PageBase pageBase) {
                        return Model.of("");
                    }
                }) {
            @Contract(pure = true)
            @Override
            protected @Nullable StringResourceModel getInfoMessageModel() {
                return null;
            }

            @Contract(pure = true)
            @Override
            protected @NotNull String getTitleIconAdditionalCssClass() {
                return "text-danger";
            }

            @Override
            protected StringResourceModel getSubtitleModel() {
                return createStringResource("RequestDetailsConfirmationPanel.discard.suggestion.message");
            }

            @Override
            protected boolean isLearnMoreVisible() {
                return false;
            }

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                performOnDelete(target);
            }

            @Override
            protected IModel<String> createYesLabel() {
                return createStringResource("RequestDetailsConfirmationPanel.discard");
            }
        };
        getPageBase().showMainPopup(dialog, target);
    }

    private void initChipsInfoPanel() {
        RepeatingView chips = new RepeatingView(ID_CHIPS);
        List<IModel<String>> chipsData = getModelObject().buildChipsData(getPageBase());

        if (chipsData != null) {
            for (IModel<String> text : chipsData) {
                WebMarkupContainer c = new WebMarkupContainer(chips.newChildId());
                c.add(new Label(ID_CHIP, text));
                chips.add(c);
            }
        }
        add(chips);
    }

    private void initSelectRadio() {
        Radio<C> radio = new Radio<>(ID_RADIO, Model.of(getModelObject().getValue()));
        radio.setOutputMarkupId(true);
        add(radio);
    }

    private void initTitle() {
        Label title = new Label(ID_TITLE, getModelObject().getName());
        title.setOutputMarkupId(true);
        add(title);
    }

    private void initDescription() {
        Label description = new Label(ID_DESC, getModelObject().getDescription());
        description.setOutputMarkupId(true);
        add(description);
    }

    private void initDefaultCssStyle() {
        setOutputMarkupId(true);

        add(AttributeModifier.append(CLASS_CSS, "bg-white "
                + "d-flex flex-column align-items-center"
                + " rounded w-100 h-100 p-3 card-shadow"));

        add(AttributeModifier.append(STYLE_CSS, ""));
    }

    private void applySelectionStyling() {
        PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> selectedValue = selectedTileModel.getObject();
        PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> tileValue = getModelObject().getValue();

        if (selectedValue == null || tileValue == null) {
            return;
        }

        String defaultTileCss = getDefaultTileCss();
        String cssClass = selectedValue.equals(tileValue) ? defaultTileCss + " active" : defaultTileCss;
        add(AttributeModifier.replace(CLASS_CSS, cssClass));
    }

    private void selectIfNoneSelected() {
        PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> currentSelection = selectedTileModel.getObject();
        PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> thisTile = getModelObject().getValue();

        if (currentSelection == null && thisTile != null) {
            selectedTileModel.setObject(thisTile);
        }
    }

    protected void performOnDelete(AjaxRequestTarget target) {
        Task task = getPageBase().createSimpleTask(OP_DETERMINE_STATUS);
        OperationResult result = task.getResult();
        IModel<SmartObjectTypeSuggestionTileModel<C>> model = SmartObjectTypeSuggestionPanel.this.getModel();
        if (model == null || model.getObject() == null) {
            return;
        }

        SmartObjectTypeSuggestionTileModel<C> modelObject = model.getObject();
        if (modelObject.getValue() == null) {
            return;
        }

        QName objectClass = modelObject.getObjectClass();
        String resourceOid = modelObject.getResourceOid();

        StatusInfo<ObjectTypesSuggestionType> statusInfo = loadObjectClassObjectTypeSuggestions(getPageBase(),
                resourceOid,
                objectClass,
                task,
                result);

        PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> thisTile = SmartObjectTypeSuggestionPanel.this
                .getModelObject().getValue();
        if (statusInfo != null) {
            SmartIntegrationUtils.removeObjectTypeSuggestionNew(
                    getPageBase(),
                    statusInfo,
                    thisTile.getRealValue(),
                    task,
                    result);

            if (result.isFatalError()) {
                result.recomputeStatus();
                getPageBase().showResult(result);
                target.add(getPageBase().getFeedbackPanel());
            }
        }
    }

    protected String getDefaultTileCss() {
        return "simple-tile selectable clickable-by-enter tile-panel d-flex flex-column align-items-center "
                + "rounded p-3 justify-content-center";
    }

    protected boolean atLeastOneSelected() {
        return true;
    }

    protected boolean isBaseContextFilterVisible() {
        return getModelObject().baseContexFilterExists();
    }

    protected boolean isFilterExists() {
        return getModelObject().filterExists();
    }

    protected boolean isAnyFilterExists() {
        return getModelObject().isAnyFilterExists();
    }

    /**
     * Get existing object type definitions for the given object class name.
     *
     * @param objectClassName QName of the object class.
     * @return List of existing object type definitions.
     */
    protected abstract List<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> getExistingObjectTypeDefinitions(
            QName objectClassName);

}
