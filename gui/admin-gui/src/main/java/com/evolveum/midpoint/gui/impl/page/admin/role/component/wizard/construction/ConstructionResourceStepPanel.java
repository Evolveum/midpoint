package com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard.construction;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard.SelectTileWizardStepPanel;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.FocusDetailsModels;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@PanelType(name = "roleWizard-construction-resource")
@PanelInstance(identifier = "roleWizard-construction-resource",
        applicableForType = RoleType.class,
        applicableForOperation = OperationTypeType.ADD,
        display = @PanelDisplay(label = "PageRole.wizard.step.construction.resource", icon = "fa fa-database"),
        containerPath = "empty")
public class ConstructionResourceStepPanel extends SelectTileWizardStepPanel<ResourceType, FocusDetailsModels<RoleType>, AssignmentType> {

    public static final String PANEL_TYPE = "roleWizard-construction-resource";

    public ConstructionResourceStepPanel(FocusDetailsModels<RoleType> model) {
        super(model);
    }

    @Override
    protected ItemPath getPathForValueContainer() {
        return RoleType.F_INDUCEMENT;
    }

    @Override
    protected ItemPath getPathForTargetReference() {
        return ItemPath.create(AssignmentType.F_CONSTRUCTION, ConstructionType.F_RESOURCE_REF);
    }

    @Override
    protected ObjectQuery getCustomQuery() {
        return PrismContext.get().queryFor(ResourceType.class)
                .item(ResourceType.F_TEMPLATE).isNull().or().item(ResourceType.F_TEMPLATE).eq(false)
                .build();
    }

    @Override
    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    protected Class<ResourceType> getType() {
        return ResourceType.class;
    }

    protected PrismContainerValue<AssignmentType> createNewValue(PrismContainerWrapper<AssignmentType> parent) {
        PrismContainerValue<AssignmentType> newValue =  super.createNewValue(parent);
        newValue.asContainerable().beginConstruction();
        return newValue;
    }

    @Override
    protected boolean isExitButtonVisible() {
        return true;
    }

    protected String getIcon() {
        return "fa fa-database";
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageRole.wizard.step.construction.resource");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageRole.wizard.step.construction.resource.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageRole.wizard.step.construction.resource.subText");
    }

    @Override
    public VisibleEnableBehaviour getBackBehaviour() {
        return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
    }

    @Override
    protected boolean isMandatory() {
        return true;
    }
}
