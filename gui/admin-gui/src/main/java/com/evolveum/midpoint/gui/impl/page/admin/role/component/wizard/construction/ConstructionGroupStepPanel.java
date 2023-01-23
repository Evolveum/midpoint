package com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard.construction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.evolveum.midpoint.gui.impl.component.search.PredefinedSearchableItems;
import com.evolveum.midpoint.gui.impl.component.search.SearchContext;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.*;

import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.search.SearchFactory;
import com.evolveum.midpoint.gui.impl.component.search.SearchValue;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.FocusDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard.MultiSelectTileWizardStepPanel;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
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
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

@PanelType(name = "roleWizard-construction-group")
@PanelInstance(identifier = "roleWizard-construction-group",
        applicableForType = RoleType.class,
        applicableForOperation = OperationTypeType.ADD,
        display = @PanelDisplay(label = "PageRole.wizard.step.construction.group"),
        containerPath = "empty")
public class ConstructionGroupStepPanel
        extends MultiSelectTileWizardStepPanel<ConstructionGroupStepPanel.AssociationWrapper, ShadowType, FocusDetailsModels<RoleType>, ConstructionType> {

    private static final Trace LOGGER = TraceManager.getTrace(ConstructionGroupStepPanel.class);

    public static final String PANEL_TYPE = "roleWizard-construction-group";

    private static final String SKIP_INFO = "skipInfo";
    private IModel<List<AssociationWrapper>> selectedItems = Model.ofList(new ArrayList<>());
    private final IModel<PrismContainerValueWrapper<AssignmentType>> assignmentModel;
    private IModel<PrismContainerValueWrapper<ConstructionType>> valueModel;
//    private IModel<SearchValue<ItemName>> associationRef = Model.of();

    public ConstructionGroupStepPanel(FocusDetailsModels<RoleType> model,
            IModel<PrismContainerValueWrapper<AssignmentType>> assignmentModel) {
        super(model);
        this.assignmentModel = assignmentModel;
    }

    @Override
    protected void onBeforeRender() {
        if (isSkipInfoVisible()) {
            getPageBase().info(getPageBase().createStringResource("ConstructionGroupStepPanel.skipStep").getString());
        }
        super.onBeforeRender();
    }

    public IModel<PrismContainerValueWrapper<ConstructionType>> getValueModel() {
        if (valueModel == null) {
            valueModel = createValueModel();
        }
        return valueModel;
    }

    private boolean isSkipInfoVisible() {
        List<ResourceAssociationDefinition> associations = WebComponentUtil.getRefinedAssociationDefinition(getValueModel().getObject().getRealValue(), getPageBase());
        return associations.isEmpty();
    }

    @Override
    protected IModel<List<AssociationWrapper>> getSelectedItemsModel() {
        return selectedItems;
    }

    protected IModel<PrismContainerValueWrapper<ConstructionType>> createValueModel() {
        return new LoadableDetachableModel<>() {
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
    protected boolean isSelectedItemsPanelVisible() {
        return true;
    }

    @Override
    protected void processSelectOrDeselectItem(TemplateTile<SelectableBean<ShadowType>> tile) {
        if (getAssociationRef() == null || getAssociationRef().getValue() == null) {
            return;
        }

        SelectableBean<ShadowType> value = tile.getValue();
        ShadowType shadow = value.getValue();
        if (tile.isSelected()) {
            selectedItems.getObject().add(
                    new AssociationWrapper(
                            shadow.getOid(),
                            WebComponentUtil.getDisplayNameOrName(shadow.asPrismObject()),
                            getAssociationRef().getValue(),
                            getAssociationRef().getLabel()));
        } else {
            selectedItems.getObject().removeIf(
                    association -> association.oid.equals(shadow.getOid())
                            && QNameUtil.match(association.associationName, getAssociationRef().getValue()));
        }
    }

    private DisplayableValue<ItemName> getAssociationRef() {
        Optional<FilterableSearchItemWrapper> wrapper = getTable().getSearchModel().getObject().getItems().stream()
                .filter(item -> item instanceof AssociationSearchItemWrapper).findFirst();
        if (wrapper.isEmpty()) {
            return null;
        }
        return wrapper.get().getValue();
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
    protected void performSelectedTiles() {
        try {
            PrismContainerWrapper<ResourceObjectAssociationType> associationContainer =
                    getValueModel().getObject().findContainer(ConstructionType.F_ASSOCIATION);

            selectedItems.getObject().forEach(item -> {
                try {

                    PrismContainerValueWrapper<ResourceObjectAssociationType> valueWrapper;

                    Optional<PrismContainerValueWrapper<ResourceObjectAssociationType>> match = associationContainer.getValues().stream().filter(
                            value -> {
                                if (value.getRealValue() == null || value.getRealValue().getRef() == null) {
                                    return false;
                                }
                                return item.associationName.equivalent(value.getRealValue().getRef().getItemPath());
                            }).findFirst();
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
    protected SearchContext getAdditionalSearchContext() {
        SearchContext searchContext = new SearchContext();
        searchContext.setPanelType(PredefinedSearchableItems.PanelType.ASSOCIABLE_SHADOW);
        try {
            ResourceObjectDefinition oc = WebComponentUtil.getResourceObjectDefinition(
                    getValueModel().getObject().getRealValue(), getPageBase());
            searchContext.setResourceObjectDefinition(oc);
        } catch (Exception ex) {
            LOGGER.debug(
                    "Association for {} not supported by resource: {}",
                    getValueModel().getObject().getRealValue(),
                    ex.getLocalizedMessage());
        }
        return searchContext;
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
