package com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard.focusMapping;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.focus.FocusDetailsModels;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ObjectTypeAttributeMappingWrapper;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import java.util.List;

@PanelType(name = "arw-focusMapping-mappings")
@PanelInstance(identifier = "arw-focusMapping-mappings",
        applicableForType = AbstractRoleType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageRole.wizard.step.focusMapping.mapping", icon = "fa fa-arrow-right-from-bracket"),
        containerPath = "empty")
public class FocusMappingMappingsStepPanel<AR extends AbstractRoleType>
        extends AbstractWizardStepPanel<FocusDetailsModels<AR>> {

    private static final Trace LOGGER = TraceManager.getTrace(FocusMappingMappingsStepPanel.class);

    public static final String PANEL_TYPE = "arw-focusMapping-mappings";

    protected static final String ID_PANEL = "panel";
    private final IModel<PrismContainerValueWrapper<MappingsType>> focusMappingModel;

    public FocusMappingMappingsStepPanel(FocusDetailsModels<AR> model,
                                         IModel<PrismContainerValueWrapper<AssignmentType>> assignmentModel) {
        super(model);
        this.focusMappingModel = createValueModel(assignmentModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        add(new FocusMappingMappingsTable(ID_PANEL, getValueModel(), getContainerConfiguration(PANEL_TYPE)) {
            @Override
            public void editItemPerformed(
                        AjaxRequestTarget target,
                        IModel<PrismContainerValueWrapper<MappingType>> rowModel,
                        List<PrismContainerValueWrapper<MappingType>> listItems) {
                    if (isValidFormComponentsOfRow(rowModel, target)) {
                        inEditMappingValue(rowModel, target);
                    }
                }
            }
        );
    }

    protected void inEditMappingValue(IModel<PrismContainerValueWrapper<MappingType>> rowModel, AjaxRequestTarget target) {

    }

    private IModel<PrismContainerValueWrapper<MappingsType>> createValueModel(
            IModel<PrismContainerValueWrapper<AssignmentType>> parent) {
        return new LoadableModel<>() {
            @Override
            protected PrismContainerValueWrapper<MappingsType> load() {
                try {
                    PrismContainerWrapper<MappingsType> container =
                            parent.getObject().findContainer(AssignmentType.F_FOCUS_MAPPINGS);
                    return container.getValue();
                } catch (SchemaException e) {
                    LOGGER.error("Couldn't find focusMapping container in " + getDetailsModel().getObjectWrapper());
                }
                return null;
            }
        };
    }

    protected IModel<PrismContainerValueWrapper<MappingsType>> getValueModel() {
        return focusMappingModel;
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    protected boolean isExitButtonVisible() {
        return true;
    }

    protected String getIcon() {
        return "fa fa-right-left";
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageRole.wizard.step.focusMapping.mapping");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageRole.wizard.step.focusMapping.mapping.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageRole.wizard.step.focusMapping.mapping.subText");
    }

    @Override
    public String getStepId() {
        return PANEL_TYPE;
    }

    @Override
    public String appendCssToWizard() {
        return "mt-5 mx-auto col-12";
    }
}
