/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.page;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.table.SmartSuggestedObjectTypeRadioTileTable;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.cxf.common.util.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardBasicPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.table.SmartObjectClassRadioTileTable;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;

import javax.xml.namespace.QName;

@PanelType(name = "rw-suggested-object-type")
@PanelInstance(identifier = "w-suggested-object-type",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "ResourceObjectClassTableWizardPanel.headerLabel", icon = "fa fa-arrows-rotate"))
public abstract class ResourceSuggestedObjectTypeTableWizardPanel<C extends ResourceObjectTypeDefinitionType, P extends Containerable> extends AbstractResourceWizardBasicPanel<P> {

    private static final String ID_PANEL = "panel";

    IModel<ObjectTypeSuggestionType> selectedModel = Model.of();
    QName selectedObjectClassName;

    public ResourceSuggestedObjectTypeTableWizardPanel(
            String id,
            WizardPanelHelper<P, ResourceDetailsModel> superHelper,
            QName objectClassName) {
        super(id, superHelper);
        this.selectedObjectClassName = objectClassName;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

        SmartSuggestedObjectTypeRadioTileTable table = new SmartSuggestedObjectTypeRadioTileTable(ID_PANEL,
                getPageBase(), this::getAssignmentHolderDetailsModel, selectedObjectClassName, selectedModel) {
        };

        table.setOutputMarkupId(true);
        add(table);
    }

    private SmartObjectClassRadioTileTable getTable() {
        return (SmartObjectClassRadioTileTable) get(ID_PANEL);
    }

    @Override
    protected void onSubmitPerformed(AjaxRequestTarget target) {

        if (selectedModel.getObject() == null || selectedModel.getObject().getIdentification() == null) {
            getPageBase().warn(getPageBase().createStringResource("Smart.suggestion.noSelection")
                    .getString());
            target.add(getPageBase().getFeedbackPanel());
            return;
        }

        var suggestion = selectedModel.getObject();
        var id = suggestion.getIdentification();
        var kind = id.getKind();
        var intent = id.getIntent();
        ResourceObjectTypeDelineationType delineation = suggestion.getDelineation();

        if (kind == null || intent == null || intent.isBlank()) {
            getPageBase().error(getPageBase().createStringResource("Smart.suggestion.missingKindIntent")
                    .getString());
            target.add(getPageBase().getFeedbackPanel());
            return;
        }

        IModel<PrismContainerWrapper<ResourceObjectTypeDefinitionType>> containerModel = createContainerModel();
        var newValue = createNewValue(containerModel, kind, intent, delineation);

        onContinueWithSelected(selectedModel, newValue, containerModel, target);
    }

    private @NotNull PrismContainerValue<ResourceObjectTypeDefinitionType> createNewValue(IModel<PrismContainerWrapper<ResourceObjectTypeDefinitionType>> containerModel, @NotNull ShadowKindType kind, String intent, ResourceObjectTypeDelineationType delineation) {
        PrismContainerWrapper<ResourceObjectTypeDefinitionType> containerWrapper = containerModel.getObject();

        var newValue = containerWrapper.getItem().createNewValue();
        var bean = newValue.asContainerable();

        String displayName = StringUtils.capitalize(kind.value()) + " " + StringUtils.capitalize(intent);

        bean.setDisplayName(displayName);
        bean.setKind(kind);
        bean.setIntent(intent);
        bean.setObjectClass(selectedObjectClassName);
        bean.setDelineation(delineation);

        return newValue;
    }

    public <C extends Containerable> IModel<PrismContainerWrapper<C>> createContainerModel() {
        ItemPath itemPath = ItemPath.create(ResourceType.F_SCHEMA_HANDLING, SchemaHandlingType.F_OBJECT_TYPE);
        LoadableModel<PrismObjectWrapper<ResourceType>> objectWrapperModel = getAssignmentHolderDetailsModel().getObjectWrapperModel();
        return PrismContainerWrapperModel.fromContainerWrapper(objectWrapperModel, itemPath);
    }

    @Override
    protected String getSaveLabelKey() {
        return "ResourceObjectClassTableWizardPanel.saveButton";
    }

    protected abstract void onContinueWithSelected(
            @NotNull IModel<ObjectTypeSuggestionType> model,
            @NotNull PrismContainerValue<ResourceObjectTypeDefinitionType> newValue,
            @NotNull IModel<PrismContainerWrapper<ResourceObjectTypeDefinitionType>> containerModel,
            @NotNull AjaxRequestTarget target);

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return getPageBase().createStringResource("ResourceObjectClassTableWizardPanel.breadcrumbLabel");
    }

    @Override
    protected @Nullable IModel<String> getBreadcrumbIcon() {
        return Model.of(GuiStyleConstants.CLASS_ICON_WIZARD);
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("ResourceObjectClassTableWizardPanel.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("ResourceObjectClassTableWizardPanel.subText");
    }

    @Override
    protected String getCssForWidthOfFeedbackPanel() {
        return "col-10";
    }

}
