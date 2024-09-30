package com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard.construction;

import java.util.List;

import com.evolveum.midpoint.prism.path.ItemName;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.focus.FocusDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attributeMapping.OutboundAttributeMappingsTable;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ObjectTypeAttributeMappingWrapper;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@PanelType(name = "arw-construction-mappings")
@PanelInstance(identifier = "arw-construction-mappings",
        applicableForType = AbstractRoleType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageRole.wizard.step.construction.mapping", icon = "fa fa-arrow-right-from-bracket"),
        containerPath = "empty")
public class ConstructionOutboundMappingsStepPanel<AR extends AbstractRoleType>
        extends AbstractWizardStepPanel<FocusDetailsModels<AR>> {

    private static final Trace LOGGER = TraceManager.getTrace(ConstructionOutboundMappingsStepPanel.class);

    public static final String PANEL_TYPE = "arw-construction-mappings";

    protected static final String ID_PANEL = "panel";
    private final IModel<PrismContainerValueWrapper<ConstructionType>> constructionModel;

    public ConstructionOutboundMappingsStepPanel(FocusDetailsModels<AR> model,
            IModel<PrismContainerValueWrapper<AssignmentType>> assignmentModel) {
        super(model);
        this.constructionModel = createValueModel(assignmentModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        add(new OutboundAttributeMappingsTable<>(ID_PANEL, getValueModel(), getContainerConfiguration(PANEL_TYPE)) {
            @Override
            protected ItemName getItemNameOfContainerWithMappings() {
                return ConstructionType.F_ATTRIBUTE;
            }

            @Override
            public void editItemPerformed(
                        AjaxRequestTarget target,
                        IModel<PrismContainerValueWrapper<MappingType>> rowModel,
                        List<PrismContainerValueWrapper<MappingType>> listItems) {
                    if (isValidFormComponentsOfRow(rowModel, target)) {
                        inEditOutboundValue(rowModel, target);
                    }
                }
            }
        );
    }

    protected void inEditOutboundValue(IModel<PrismContainerValueWrapper<MappingType>> rowModel, AjaxRequestTarget target) {

    }

    private IModel<PrismContainerValueWrapper<ConstructionType>> createValueModel(
            IModel<PrismContainerValueWrapper<AssignmentType>> parent) {
        return new LoadableModel<>() {
            @Override
            protected PrismContainerValueWrapper<ConstructionType> load() {
                try {
                    PrismContainerWrapper<ConstructionType> container =
                            parent.getObject().findContainer(AssignmentType.F_CONSTRUCTION);
                    return container.getValue();
                } catch (SchemaException e) {
                    LOGGER.error("Couldn't find construction container in " + getDetailsModel().getObjectWrapper());
                }
                return null;
            }
        };
    }

    protected IModel<PrismContainerValueWrapper<ConstructionType>> getValueModel() {
        return constructionModel;
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    protected boolean isExitButtonVisible() {
        return true;
    }

    protected String getIcon() {
        return "fa fa-arrow-right-from-bracket";
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageRole.wizard.step.construction.mapping");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageRole.wizard.step.construction.mapping.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageRole.wizard.step.construction.mapping.subText");
    }

    @Override
    public String getStepId() {
        return PANEL_TYPE;
    }

    @Override
    protected void onSubmitPerformed(AjaxRequestTarget target) {
        super.onSubmitPerformed(target);
        try {
            if (ValueStatus.ADDED.equals(constructionModel.getObject().getParentContainerValue(AssignmentType.class).getStatus())) {

                PrismContainerWrapper container =
                        ((PrismContainerWrapper) constructionModel.getObject().getParent()).findContainer(ConstructionType.F_ATTRIBUTE);
                if (container instanceof ObjectTypeAttributeMappingWrapper) {
                    ((ObjectTypeAttributeMappingWrapper)container).applyDelta();
                }
            }
        } catch (SchemaException e) {
            LOGGER.error("Couldn't apply delta from attribute value container.");
        }
    }

    @Override
    public String appendCssToWizard() {
        return "mt-5 mx-auto col-11";
    }
}
