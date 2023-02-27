package com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.wizard.MultiSelectTileWizardStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.FocusDetailsModels;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.*;

@PanelType(name = "roleWizard-access-application-role")
@PanelInstance(identifier = "roleWizard-access-application-role",
        applicableForType = RoleType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageRole.wizard.step.access.applicationRole", icon = "fa fa-list"),
        containerPath = "empty")
public class AccessApplicationRoleStepPanel
        extends MultiSelectTileWizardStepPanel<AbstractMap.SimpleEntry<String, String>, RoleType, FocusDetailsModels<RoleType>, RoleType> {

    private static final Trace LOGGER = TraceManager.getTrace(AccessApplicationRoleStepPanel.class);

    public static final String PANEL_TYPE = "roleWizard-access-application-role";

    private IModel<List<AbstractMap.SimpleEntry<String, String>>> selectedItems = Model.ofList(new ArrayList<>());

    public AccessApplicationRoleStepPanel(FocusDetailsModels<RoleType> model) {

        super(model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        if (getTable().getTilesModel().getObject().size() == 0) {
            getPageBase().info(getPageBase().createStringResource("AccessApplicationRoleStepPanel.skip").getString());
        }
    }

    @Override
    protected IModel<List<AbstractMap.SimpleEntry<String, String>>> getSelectedItemsModel() {
        return selectedItems;
    }

    @Override
    protected IModel<String> getItemLabelModel(AbstractMap.SimpleEntry<String, String> entry) {
        return Model.of(entry.getValue());
    }

    @Override
    protected void deselectItem(AbstractMap.SimpleEntry<String, String> removedEntry) {
        removeSelectedItem(removedEntry.getKey());
    }

    private void removeSelectedItem(String oid) {
        selectedItems.getObject().removeIf(entry -> entry.getKey().equals(oid));
    }

    @Override
    protected void processSelectOrDeselectItem(SelectableBean<RoleType> value) {
        RoleType applicationRole = value.getValue();
        if (value.isSelected()) {
            selectedItems.getObject().add(
                    new AbstractMap.SimpleEntry(
                            applicationRole.getOid(),
                            WebComponentUtil.getDisplayNameOrName(applicationRole.asPrismObject())));
        } else {
            removeSelectedItem(applicationRole.getOid());
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
        return PrismContext.get().queryFor(RoleType.class)
                .item(AssignmentHolderType.F_ARCHETYPE_REF).ref(SystemObjectsType.ARCHETYPE_APPLICATION_ROLE.value())
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

    protected void performSelectedObjects() {
        List<AbstractMap.SimpleEntry<String, String>> selectedNewItems = new ArrayList<>(selectedItems.getObject());

        ItemPath containerPath = getPathForValueContainer();
        PrismContainerWrapper<AssignmentType> container;
        try {
            container = getDetailsModel().getObjectWrapper().findContainer(containerPath);
        } catch (SchemaException e) {
            LOGGER.error("Couldn't find assignment container in " + getDetailsModel().getObjectWrapper());
            return;
        }
        container.getValues().forEach(value -> {
            if (!ValueStatus.ADDED.equals(value.getStatus())) {
                return;
            }
            boolean match = selectedItems.getObject().stream()
                    .anyMatch(entry -> entry.getKey().equals(value.getRealValue().getTargetRef().getOid()));
            if (!match) {
                try {
                    container.remove(value, getPageBase());
                } catch (SchemaException e) {
                    LOGGER.error("Couldn't remove deselected value " + value);
                }
            } else {
                selectedNewItems.removeIf(entry -> entry.getKey().equals(value.getRealValue().getTargetRef().getOid()));
            }
        });
        selectedNewItems.forEach(entry -> {
            try {
                PrismContainerValue<AssignmentType> newValue = container.getItem().createNewValue();
                PrismContainerValueWrapper<AssignmentType> valueWrapper = WebPrismUtil.createNewValueWrapper(
                        container, newValue, getPageBase(), getDetailsModel().createWrapperContext());
                container.getValues().add(valueWrapper);
                performSelectedTile(entry.getKey(), RoleType.COMPLEX_TYPE, valueWrapper);
            } catch (SchemaException e) {
                LOGGER.error("Couldn't create new value for assignment container " + container);
            }
        });
    }

    @Override
    protected Class<RoleType> getType() {
        return RoleType.class;
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
        return createStringResource("PageRole.wizard.step.access.applicationRole");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageRole.wizard.step.access.applicationRole.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageRole.wizard.step.access.applicationRole.subText");
    }

    @Override
    public VisibleEnableBehaviour getBackBehaviour() {
        return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
    }
}
