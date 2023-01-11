package com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard.construction;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.search.ChoicesSearchItemWrapper;
import com.evolveum.midpoint.gui.impl.component.search.SearchConfigurationWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.FocusDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard.MultiSelectTileWizardStepPanel;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
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
import com.evolveum.midpoint.web.component.search.SearchValue;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@PanelType(name = "roleWizard-construction-group")
@PanelInstance(identifier = "roleWizard-construction-group",
        applicableForType = RoleType.class,
        applicableForOperation = OperationTypeType.ADD,
        display = @PanelDisplay(label = "PageRole.wizard.step.construction.group", icon = "fa fa-building"),
        containerPath = "empty")
public class ConstructionGroupStepPanel
        extends MultiSelectTileWizardStepPanel<ConstructionGroupStepPanel.AssociationWrapper, ShadowType, FocusDetailsModels<RoleType>, ConstructionType> {

    private static final Trace LOGGER = TraceManager.getTrace(ConstructionGroupStepPanel.class);

    public static final String PANEL_TYPE = "roleWizard-construction-group";
    private IModel<List<AssociationWrapper>> selectedItems = Model.ofList(new ArrayList<>());
    private final IModel<PrismContainerValueWrapper<AssignmentType>> assignmentModel;
    private IModel<SearchValue<ItemName>> associationRef = Model.of();

    public ConstructionGroupStepPanel(FocusDetailsModels<RoleType> model,
            IModel<PrismContainerValueWrapper<AssignmentType>> assignmentModel) {
        super(model);
        this.assignmentModel = assignmentModel;
    }

    @Override
    protected IModel<List<AssociationWrapper>> getSelectedItemsModel() {
        return selectedItems;
    }

    @Override
    protected IModel<PrismContainerValueWrapper<ConstructionType>> createValueModel() {
        return new LoadableModel<>() {
            @Override
            protected PrismContainerValueWrapper<ConstructionType> load() {

                ItemPath path = getPathForValueContainer();
                try {
                    PrismContainerWrapper<ConstructionType> container =
                            assignmentModel.getObject().findContainer(path);
                    return container.getValue();
                } catch (SchemaException e) {
                    LOGGER.error("Couldn't find construction container in " + getDetailsModel().getObjectWrapper());
                }
                return null;
            }
        };
    }

    @Override
    protected IModel<String> getItemLabelModel(AssociationWrapper entry) {
        return () -> entry.name + " (" + entry.associationDisplayName + ")";
    }

    @Override
    protected void deselectItem(AssociationWrapper entry) {
        selectedItems.getObject().removeIf(selectedItem -> selectedItem.equals(entry));
    }

    @Override
    protected void processSelectOrDeselectItem(TemplateTile<SelectableBean<ShadowType>> tile) {
        if (associationRef.getObject() == null) {
            return;
        }

        SelectableBean<ShadowType> value = tile.getValue();
        ShadowType shadow = value.getValue();
        if (tile.isSelected()) {
            selectedItems.getObject().add(
                    new AssociationWrapper(
                            shadow.getOid(),
                            WebComponentUtil.getDisplayNameOrName(shadow.asPrismObject()),
                            associationRef.getObject().getValue(),
                            associationRef.getObject().getLabel()));
        } else {
            selectedItems.getObject().removeIf(
                    association -> association.oid.equals(shadow.getOid())
                            && QNameUtil.match(association.associationName, associationRef.getObject().getValue()));
        }
    }

    @Override
    protected ItemPath getPathForValueContainer() {
        return AssignmentType.F_CONSTRUCTION;
    }

    @Override
    protected Collection<SelectorOptions<GetOperationOptions>> getSearchOptions() {
        return GetOperationOptions.createNoFetchCollection();
    }

    @Override
    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    protected Class<ShadowType> getType() {
        return ShadowType.class;
    }

    @Override
    protected void onSelectPerformed() {
        try {
            PrismContainerWrapper<ResourceObjectAssociationType> associationContainer =
                    getValueModel().getObject().findContainer(ConstructionType.F_ASSOCIATION);

            selectedItems.getObject().forEach(item -> {
                try {

                    PrismContainerValueWrapper<ResourceObjectAssociationType> valueWrapper;

                    Optional<PrismContainerValueWrapper<ResourceObjectAssociationType>> match = associationContainer.getValues().stream().filter(
                            value -> value.getRealValue().getRef().equivalent(item.associationName)).findFirst();
                    if (match.isPresent()) {
                        valueWrapper = match.get();
                    } else {

                        PrismContainerValue<ResourceObjectAssociationType> newValue = associationContainer.getItem().createNewValue();

                        NameItemPathSegment segment = new NameItemPathSegment(item.associationName);
                        newValue.asContainerable().ref(new ItemPathType(ItemPath.create(segment)));
                        newValue.asContainerable().beginOutbound().beginExpression();

                        valueWrapper = WebPrismUtil.createNewValueWrapper(
                                associationContainer,
                                newValue,
                                getPageBase(),
                                getDetailsModel().createWrapperContext());
                        associationContainer.getValues().add(valueWrapper);
                    }

                    PrismPropertyWrapper<ExpressionType> expression =
                            valueWrapper.findProperty(
                                    ItemPath.create(ResourceObjectAssociationType.F_OUTBOUND, MappingType.F_EXPRESSION));
                    ExpressionUtil.addShadowRefEvaluatorValue(
                            expression.getValue().getRealValue(),
                            item.oid,
                            PrismContext.get());

                } catch (SchemaException e) {
                    LOGGER.error("Couldn't create new value for association container.");
                }

            });
        } catch (SchemaException e) {
            LOGGER.error("Couldn't find association expression.");
        }
    }

    @Override
    protected boolean isExitButtonVisible() {
        return true;
    }

    protected String getIcon() {
        return "fa fa-building";
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageRole.wizard.step.construction.group");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageRole.wizard.step.construction.group.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageRole.wizard.step.construction.group.subText");
    }

    @Override
    protected SearchConfigurationWrapper<ShadowType> createSearchConfigWrapper(Class<ShadowType> type) {
        SearchConfigurationWrapper<ShadowType> config = super.createSearchConfigWrapper(type);
        List<DisplayableValue<ItemName>> values = new ArrayList<>();

        List<ResourceAssociationDefinition> associations =
                WebComponentUtil.getRefinedAssociationDefinition(getValueModel().getObject().getRealValue(), getPageBase());
        associations.forEach(association -> values.add(
                new SearchValue<>(
                        association.getName(),
                        WebComponentUtil.getAssociationDisplayName(association))));
        if (!values.isEmpty()) {
            associationRef.setObject((SearchValue<ItemName>) values.get(0));
        }

        config.addSearchItem(new ChoicesSearchItemWrapper<>(ItemPath.EMPTY_PATH, values) {
            @Override
            public ObjectFilter createFilter(Class type, PageBase pageBase, VariablesMap variables) {
                IModel<PrismContainerValueWrapper<ConstructionType>> valueModel = getValueModel();
                ConstructionType construction = valueModel.getObject().getRealValue();
                return WebComponentUtil.getShadowTypeFilterForAssociation(
                        construction, (ItemName)getValue().getValue(), "load resource", getPageBase());
            }

            @Override
            public boolean isVisible() {
                return !getAvailableValues().isEmpty() || getAvailableValues().size() != 1;
            }

            @Override
            public void setValue(DisplayableValue value) {
                super.setValue(value);
                associationRef.setObject((SearchValue<ItemName>) value);
            }

            @Override
            public DisplayableValue getDefaultValue() {
                if (!getAvailableValues().isEmpty()) {
                    return getAvailableValues().get(0);
                }
                return super.getDefaultValue();
            }

            @Override
            public boolean allowNull() {
                return false;
            }
        });
        return config;
    }

    public class AssociationWrapper implements Serializable {

        private final String oid;
        private final String name;
        private final ItemName associationName;

        private final String associationDisplayName;

        private AssociationWrapper(String oid, String name, ItemName associationName, String associationDisplayName) {
            this.oid = oid;
            this.name = name;
            this.associationName = associationName;
            this.associationDisplayName = associationDisplayName;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof AssociationWrapper)) {
                return false;
            }
            AssociationWrapper associationWrapper = (AssociationWrapper) obj;
            if (this.oid == null || associationWrapper.oid == null
                    || this.associationName == null || associationWrapper.associationName == null) {
                return false;
            }
            return this.oid.equals(associationWrapper.oid) && this.associationName.equals(associationWrapper.associationName);
        }

        @Override
        public int hashCode() {
            return oid.hashCode() + name.hashCode() + associationName.hashCode() + associationDisplayName.hashCode();
        }
    }
}
