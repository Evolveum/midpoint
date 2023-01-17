package com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard.construction;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.search.ChoicesSearchItemWrapper;
import com.evolveum.midpoint.gui.impl.component.search.SearchConfigurationWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.FocusDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.attributeMapping.OutboundAttributeMappingsTable;
import com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard.MultiSelectTileWizardStepPanel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ResourceAttributeMappingWrapper;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.processor.ResourceAssociationDefinition;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.search.SearchValue;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@PanelType(name = "roleWizard-construction-mapping")
@PanelInstance(identifier = "roleWizard-construction-mapping",
        applicableForType = RoleType.class,
        applicableForOperation = OperationTypeType.ADD,
        display = @PanelDisplay(label = "PageRole.wizard.step.construction.mapping", icon = "fa fa-building"),
        containerPath = "empty")
public class ConstructionOutboundMappingsStepPanel
        extends AbstractWizardStepPanel<FocusDetailsModels<RoleType>> {

    private static final Trace LOGGER = TraceManager.getTrace(ConstructionOutboundMappingsStepPanel.class);

    public static final String PANEL_TYPE = "roleWizard-construction-mapping";

    protected static final String ID_PANEL = "panel";
    private final IModel<PrismContainerValueWrapper<ConstructionType>> constructionModel;

    public ConstructionOutboundMappingsStepPanel(FocusDetailsModels<RoleType> model,
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
        add(new OutboundAttributeMappingsTable<>(ID_PANEL, getValueModel()) {
                @Override
                protected void editItemPerformed(
                        AjaxRequestTarget target,
                        IModel<PrismContainerValueWrapper<MappingType>> rowModel,
                        List<PrismContainerValueWrapper<MappingType>> listItems) {
                    inEditOutboundValue(rowModel, target);
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
                if (container instanceof ResourceAttributeMappingWrapper) {
                    ((ResourceAttributeMappingWrapper)container).applyDelta();
                }
            }
        } catch (SchemaException e) {
            LOGGER.error("Couldn't apply delta from attribute value container.");
        }

    }
}
