package com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard.construction;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard.SelectTileWizardStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard.SingleTileWizardStepPanel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ConstructionValueWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;

import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.SelectableBean;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.FocusDetailsModels;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import javax.xml.namespace.QName;
import java.util.Collections;

@PanelType(name = "roleWizard-construction-resource")
@PanelInstance(identifier = "roleWizard-construction-resource",
        applicableForType = RoleType.class,
        applicableForOperation = OperationTypeType.ADD,
        display = @PanelDisplay(label = "PageRole.wizard.step.construction.resource", icon = "fa fa-database"),
        containerPath = "empty")
public class ConstructionResourceStepPanel extends SingleTileWizardStepPanel<ResourceType, FocusDetailsModels<RoleType>, AssignmentType> {

    private static final Trace LOGGER = TraceManager.getTrace(SelectTileWizardStepPanel.class);

    public static final String PANEL_TYPE = "roleWizard-construction-resource";

    public ConstructionResourceStepPanel(
            FocusDetailsModels<RoleType> model, IModel<PrismContainerValueWrapper<AssignmentType>> valueModel) {
        super(model, valueModel);
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
        PrismContainerValue<AssignmentType> newValue = super.createNewValue(parent);
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

    @Override
    protected <C extends Containerable> void performSelectedTile(String oid, QName typeName, PrismContainerValueWrapper<C> value) {
        super.performSelectedTile(oid, typeName, value);

        try {
            PrismContainerWrapper constructionWrapper = value.findContainer(AssignmentType.F_CONSTRUCTION);

            if (constructionWrapper.getValue() instanceof ConstructionValueWrapper) {
                ((ConstructionValueWrapper) constructionWrapper.getValue()).setResourceOid(oid);
            }
        } catch (SchemaException e) {
            LOGGER.error("Couldn't find construction wrapper.");
        }
    }
}
