/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.page;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardBasicPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.table.SmartObjectClassTable;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectClassSizeEstimationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import com.evolveum.midpoint.xml.ns._public.prism_schema_3.ComplexTypeDefinitionType;

import com.evolveum.midpoint.xml.ns._public.prism_schema_3.PrismSchemaType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils.computeObjectClassSizeEstimationType;

@PanelType(name = "rw-object-class")
@PanelInstance(identifier = "rw-object-class",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "ResourceObjectClassTableWizardPanel.headerLabel", icon = "fa fa-arrows-rotate"))
public abstract class ResourceObjectClassTableWizardPanel<C extends ResourceObjectTypeDefinitionType, P extends Containerable> extends AbstractResourceWizardBasicPanel<P> {

    private static final String ID_PANEL = "panel";

    private static final String OP_DETERMINE_STATUS = ResourceObjectClassTableWizardPanel.class.getName() + ".determineStatus";

    IModel<PrismContainerValueWrapper<ComplexTypeDefinitionType>> selectedModel = Model.of();

    public ResourceObjectClassTableWizardPanel(String id, WizardPanelHelper<P, ResourceDetailsModel> superHelper) {
        super(id, superHelper);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        String resourceOid = getAssignmentHolderDetailsModel().getObjectType().getOid();
        LoadableModel<List<PrismContainerValueWrapper<ComplexTypeDefinitionType>>> complexTypeDefinitionTypes = getComplexTypeDefinitionTypes();
        Map<QName, ObjectClassSizeEstimationType> objectClassSizeEstimations = loadObjectClassesSizeEstimations(
                complexTypeDefinitionTypes, resourceOid, getPageBase());

        SmartObjectClassTable<PrismContainerValueWrapper<ComplexTypeDefinitionType>> table = new SmartObjectClassTable<>(
                ID_PANEL,
                UserProfileStorage.TableId.PANEL_RESOURCE_OBJECT_CLASSES,
                complexTypeDefinitionTypes,
                selectedModel,
                resourceOid,
                objectClassSizeEstimations);
        table.setOutputMarkupId(true);
        add(table);
    }

    private @NotNull Map<QName, ObjectClassSizeEstimationType> loadObjectClassesSizeEstimations(
            @NotNull IModel<List<PrismContainerValueWrapper<ComplexTypeDefinitionType>>> model,
            @NotNull String resourceOid,
            @NotNull PageBase pageBase) {
        Map<QName, ObjectClassSizeEstimationType> objectClassSizeEstimationCache = new HashMap<>();

        Task task = pageBase.createSimpleTask(OP_DETERMINE_STATUS);
        OperationResult result = task.getResult();

        List<PrismContainerValueWrapper<ComplexTypeDefinitionType>> object = model.getObject();
        if (object != null && !object.isEmpty()) {
            for (PrismContainerValueWrapper<ComplexTypeDefinitionType> item : object) {
                ComplexTypeDefinitionType realValue = item.getRealValue();
                QName objectClassName = realValue.getName();
                ObjectClassSizeEstimationType objectClassSizeEstimationType = computeObjectClassSizeEstimationType(
                        pageBase,
                        resourceOid,
                        objectClassName,
                        task,
                        result);

                objectClassSizeEstimationCache.put(objectClassName, objectClassSizeEstimationType);
            }
        }
        return objectClassSizeEstimationCache;
    }

    @Contract(pure = true)
    private @NotNull LoadableModel<List<PrismContainerValueWrapper<ComplexTypeDefinitionType>>> getComplexTypeDefinitionTypes() {
        return new LoadableModel<>() {
            @Override
            protected List<PrismContainerValueWrapper<ComplexTypeDefinitionType>> load() {
                try {
                    PrismContainerWrapper<ComplexTypeDefinitionType> container = getAssignmentHolderDetailsModel()
                            .getObjectWrapper().findContainer(ItemPath.create(
                                    ResourceType.F_SCHEMA,
                                    WebPrismUtil.PRISM_SCHEMA,
                                    PrismSchemaType.F_COMPLEX_TYPE));
                    return container.getValues();
                } catch (SchemaException e) {
                    throw new RuntimeException("Error while loading complex type definition", e);
                }
            }
        };
    }

    @Override
    protected void onSubmitPerformed(AjaxRequestTarget target) {
        if (selectedModel.getObject() == null) {
            return;
        }
        onContinueWithSelected(selectedModel, target);
    }

    @Override
    protected String getSaveLabelKey() {
        return "ResourceObjectClassTableWizardPanel.saveButton";
    }

    protected abstract void onContinueWithSelected(IModel<PrismContainerValueWrapper<ComplexTypeDefinitionType>> model, AjaxRequestTarget target);

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return getPageBase().createStringResource("ResourceObjectClassTableWizardPanel.breadcrumbLabel");
    }

    @Override
    protected @Nullable IModel<String> getBreadcrumbIcon() {
        return Model.of("fa-solid fa-wand-magic-sparkles");
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
