package com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard;

import java.util.Collection;

import com.evolveum.midpoint.gui.impl.component.wizard.SingleTileWizardStepPanel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.impl.page.admin.focus.FocusDetailsModels;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@PanelType(name = "apprw-access")
@PanelInstance(identifier = "apprw-access",
        applicableForType = RoleType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageRole.wizard.step.access.application", icon = "fa fa-list"),
        containerPath = "empty")
public class AccessApplicationStepPanel extends SingleTileWizardStepPanel<ServiceType, FocusDetailsModels<RoleType>, AssignmentType> {

    public static final String PANEL_TYPE = "apprw-access";

    public AccessApplicationStepPanel(FocusDetailsModels<RoleType> model) {
        super(model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        if (getTable().getTilesModel().getObject().size() == 0) {
            getPageBase().info(getPageBase().createStringResource("AccessApplicationStepPanel.skip").getString());
        }
    }

    @Override
    protected ItemPath getPathForValueContainer() {
        return RoleType.F_INDUCEMENT;
    }


    @Override
    protected ItemPath getPathForTargetReference() {
        return AssignmentType.F_TARGET_REF;
    }

    @Override
    protected ObjectQuery getCustomQuery() {
        return PrismContext.get().queryFor(ServiceType.class)
                .item(AssignmentHolderType.F_ARCHETYPE_REF).ref(SystemObjectsType.ARCHETYPE_APPLICATIONS.value())
                .build();
    }

    @Override
    protected Collection<SelectorOptions<GetOperationOptions>> getSearchOptions() {
        return getPageBase().getOperationOptionsBuilder()
                .item(FocusType.F_JPEG_PHOTO).retrieve()
                .build();
    }

    @Override
    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    protected Class<ServiceType> getType() {
        return ServiceType.class;
    }

    @Override
    protected boolean isExitButtonVisible() {
        return true;
    }

    protected String getIcon() {
        return "fa fa-list";
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageRole.wizard.step.access.application");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageRole.wizard.step.access.application.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageRole.wizard.step.access.application.subText");
    }

    @Override
    public VisibleEnableBehaviour getBackBehaviour() {
        return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
    }
}
