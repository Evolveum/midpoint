/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.table;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismPropertyValuePanel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTypesSuggestionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

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
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.io.Serial;
import java.util.List;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationStatusInfoUtils.loadObjectClassObjectTypeSuggestions;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.CLASS_CSS;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.STYLE_CSS;

public class SmartObjectTypeSuggestionPanel<C extends PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>>
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

    private static final String ID_FILTER_LABEL="filterLabel";
    private static final String ID_BASE_CONTEXT_FILTER_LABEL="baseContextFilterLabel";
    private static final String ID_BASE_CONTEXT_OBJECT_CLASS_LABEL="baseContextFilterObjectClassLabel";
    private static final String ID_BASE_CONTEXT_OBJECT_CLASS = "baseContextFilterObjectClass";

    private static final String ID_TOGGLE_ICON = "toggleIcon";

    IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> selectedTileModel;

    private static final String OP_DETERMINE_STATUS =
            SmartObjectTypeSuggestionPanel.class.getName() + ".determineStatus";

    boolean isFilterVisible = false;

    public SmartObjectTypeSuggestionPanel(@NotNull String id,
            @NotNull IModel<SmartObjectTypeSuggestionTileModel<C>> model,
            @NotNull IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> selectedTileModel) {
        super(id, model);
        this.selectedTileModel = selectedTileModel;
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

        AjaxIconButton moreActions = new AjaxIconButton(ID_MORE_ACTIONS,
                getMoreActionIcon(),
                createStringResource("SmartSuggestObjectTypeTilePanel.more.actions")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                performOnDelete(target);
            }
        };
        moreActions.setOutputMarkupId(true);
        add(moreActions);

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

        WebMarkupContainer filterCtn = new WebMarkupContainer(ID_FILTER_CTN);
        filterCtn.setOutputMarkupId(true);
        filterCtn.add(new VisibleBehaviour(() -> isFilterVisible));
        add(filterCtn);

        List<PrismPropertyValueWrapper<Object>> filterPropertyValueWrapper = getModelObject().getFilterPropertyValueWrapper();

        Label filterLabel = new Label(ID_FILTER_LABEL, createStringResource("SmartSuggestObjectTypeTilePanel.filter"));
        filterLabel.setOutputMarkupId(true);
        filterCtn.add(filterLabel);

        RepeatingView filterPanels = new RepeatingView(ID_ACE);
        populatePropertyPanels(filterPropertyValueWrapper, filterPanels);
        filterCtn.add(filterPanels);

        //TODO design whats with base context filter
        Label baseContextFilterLabel = new Label(ID_BASE_CONTEXT_FILTER_LABEL,
                createStringResource("SmartSuggestObjectTypeTilePanel.base.context.filter"));
        baseContextFilterLabel.setOutputMarkupId(true);
        filterCtn.add(baseContextFilterLabel);

        Label baseContextObjectClassLabel = new Label(ID_BASE_CONTEXT_OBJECT_CLASS_LABEL,
                createStringResource("SmartSuggestObjectTypeTilePanel.base.context.object.class"));
        baseContextObjectClassLabel.setOutputMarkupId(true);
        filterCtn.add(baseContextObjectClassLabel);

        List<PrismPropertyValueWrapper<Object>> baseContexFilterPropertyValueWrapper = getModelObject()
                .getBaseContexFilterPropertyValueWrapper(ResourceObjectReferenceType.F_FILTER);
        RepeatingView baseContextFilterPanels = new RepeatingView(ID_ACE_BASE);
        populatePropertyPanels(baseContexFilterPropertyValueWrapper, baseContextFilterPanels);
        filterCtn.add(baseContextFilterPanels);

        List<PrismPropertyValueWrapper<Object>> baseContexFilterObjectClassPropertyValueWrapper1 = getModelObject()
                .getBaseContexFilterPropertyValueWrapper(ResourceObjectReferenceType.F_OBJECT_CLASS);
        RepeatingView baseContextFilterObjectClassPanels = new RepeatingView(ID_BASE_CONTEXT_OBJECT_CLASS);
        populateObjectClassPropertyPanels(baseContexFilterObjectClassPropertyValueWrapper1, baseContextFilterObjectClassPanels);
        filterCtn.add(baseContextFilterObjectClassPanels);

        AjaxLinkPanel togglePanel = new AjaxLinkPanel(ID_TOGGLE, () -> isFilterVisible
                ? createStringResource("SmartSuggestObjectTypeTilePanel.hide.filter").getString()
                : createStringResource("SmartSuggestObjectTypeTilePanel.show.filter").getString()) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(@NotNull AjaxRequestTarget target) {
                isFilterVisible = !isFilterVisible;
                target.add(filterCtn.getParent(), this);
            }
        };
        togglePanel.setOutputMarkupId(true);

        WebMarkupContainer toggleIcon = new WebMarkupContainer(ID_TOGGLE_ICON);
        toggleIcon.add(AttributeModifier.append(CLASS_CSS, () ->
                isFilterVisible ? "fa fa-chevron-up" : "fa fa-chevron-down"));
        toggleIcon.setOutputMarkupId(true);

        add(toggleIcon);
        add(togglePanel);
    }

    private static void populatePropertyPanels(List<PrismPropertyValueWrapper<Object>> filterPropertyValueWrapper, RepeatingView filterPanels) {
        for (PrismPropertyValueWrapper<Object> valueWrapper : filterPropertyValueWrapper) {
            PrismPropertyValuePanel<?> valuePanel = new PrismPropertyValuePanel<>(filterPanels.newChildId(),
                    Model.of(valueWrapper), null);
            valuePanel.setOutputMarkupId(true);
            valuePanel.setEnabled(false);
            filterPanels.add(valuePanel);
        }
    }

    private static void populateObjectClassPropertyPanels(@NotNull List<PrismPropertyValueWrapper<Object>> filterPropertyValueWrapper, RepeatingView filterPanels) {
        for (PrismPropertyValueWrapper<Object> valueWrapper : filterPropertyValueWrapper) {
            if(valueWrapper.getRealValue() == null){
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

    protected Model<String> getMoreActionIcon() {
//        return Model.of("fa fa-ellipsis-h");
        return Model.of(GuiStyleConstants.CLASS_ICON_TRASH);
    }

    protected boolean atLeastOneSelected() {
        return true;
    }

    protected String getDefaultTileCss() {
        return "simple-tile selectable clickable-by-enter tile-panel d-flex flex-column align-items-center "
                + "rounded p-3 justify-content-center";
    }

    @Override
    public PageBase getPageBase() {
        return super.getPageBase();
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
            target.add(getPageBase().getFeedbackPanel());
        }
    }

}
