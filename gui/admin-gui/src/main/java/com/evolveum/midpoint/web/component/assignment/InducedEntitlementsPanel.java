package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.session.AssignmentsTabStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by honchar.
 */
public class InducedEntitlementsPanel extends InducementsPanel{

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(InducedEntitlementsPanel.class);
    private static final String DOT_CLASS = InducedEntitlementsPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_SHADOW_DISPLAY_NAME = DOT_CLASS + "loadShadowDisplayName";

    public InducedEntitlementsPanel(String id, IModel<ContainerWrapper<AssignmentType>> inducementContainerWrapperModel){
        super(id, inducementContainerWrapperModel);

    }

    @Override
    protected void initPaging() {
        getInducedEntitlementsTabStorage().setPaging(ObjectPaging.createPaging(0, getItemsPerPage()));
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.INDUCED_ENTITLEMENTS_TAB_TABLE;
    }

    @Override
    protected int getItemsPerPage() {
        return (int) getParentPage().getItemsPerPage(UserProfileStorage.TableId.INDUCED_ENTITLEMENTS_TAB_TABLE);
    }

    private AssignmentsTabStorage getInducedEntitlementsTabStorage(){
        return getParentPage().getSessionStorage().getInducedEntitlementsTabStorage();
    }

    @Override
    protected List<IColumn<ContainerValueWrapper<AssignmentType>, String>> initColumns() {
        List<IColumn<ContainerValueWrapper<AssignmentType>, String>> columns = new ArrayList<>();
        columns.add(new AbstractColumn<ContainerValueWrapper<AssignmentType>, String>(createStringResource("ConstructionType.kind")){
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<ContainerValueWrapper<AssignmentType>>> item, String componentId,
                                     final IModel<ContainerValueWrapper<AssignmentType>> rowModel) {
                item.add(new Label(componentId, getKindLabelModel(rowModel.getObject())));
            }
        });
        columns.add(new AbstractColumn<ContainerValueWrapper<AssignmentType>, String>(createStringResource("ConstructionType.intent")){
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<ContainerValueWrapper<AssignmentType>>> item, String componentId,
                                     final IModel<ContainerValueWrapper<AssignmentType>> rowModel) {
                item.add(new Label(componentId, getIntentLabelModel(rowModel.getObject())));
            }
        });
        columns.add(new AbstractColumn<ContainerValueWrapper<AssignmentType>, String>(createStringResource("ConstructionType.association")){
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<ContainerValueWrapper<AssignmentType>>> item, String componentId,
                                     final IModel<ContainerValueWrapper<AssignmentType>> rowModel) {
                String assocLabel = getAssociationLabel(rowModel.getObject());
                //in case when association label contains "-" symbol, break-all words property will
                //wrap the label text incorrectly. In order to avoid this, we add additional style
                if (assocLabel != null && assocLabel.contains("-")){
                    item.add(AttributeModifier.append("style", "white-space: pre-line"));
                }
                item.add(new Label(componentId, Model.of(assocLabel)));
            }
        });

        return columns;
    }

    @Override
    protected ObjectQuery createObjectQuery() {
        ObjectQuery query = super.createObjectQuery();
        ObjectFilter filter = query.getFilter();
        ObjectQuery entitlementsQuery = QueryBuilder.queryFor(AssignmentType.class, getParentPage().getPrismContext())
                .exists(AssignmentType.F_CONSTRUCTION)
                .build();
        if (filter != null){
            query.setFilter(AndFilter.createAnd(filter, entitlementsQuery.getFilter()));
        } else {
            query.setFilter(entitlementsQuery.getFilter());
        }
        return query;
    }

    @Override
    protected InducementDetailsPanel createDetailsPanel(String idAssignmentDetails, Form<?> form, IModel<ContainerValueWrapper<AssignmentType>> model) {
        return new InducedEntitlementDetailsPanel(ID_ASSIGNMENT_DETAILS, form, model);
    }

    @Override
    protected Class getDefaultNewAssignmentFocusType(){
        return ResourceType.class;
    }

    @Override
    protected boolean isRelationVisible() {
        return false;
    }

    protected List<ObjectTypes> getObjectTypesList(){
        return Arrays.asList(ObjectTypes.RESOURCE);
    }

    private IModel<String> getKindLabelModel(ContainerValueWrapper<AssignmentType> assignmentWrapper){
        if (assignmentWrapper == null){
            return Model.of("");
        }
        AssignmentType assignment = assignmentWrapper.getContainerValue().asContainerable();
        ConstructionType construction = assignment.getConstruction();
        if (construction == null || construction.getKind() == null){
            return Model.of("");
        }
        return WebComponentUtil.createLocalizedModelForEnum(construction.getKind(), InducedEntitlementsPanel.this);

    }

    private IModel<String> getIntentLabelModel(ContainerValueWrapper<AssignmentType> assignmentWrapper){
        if (assignmentWrapper == null){
            return Model.of("");
        }
        AssignmentType assignment = assignmentWrapper.getContainerValue().asContainerable();
        ConstructionType construction = assignment.getConstruction();
        if (construction == null || construction.getIntent() == null){
            return Model.of("");
        }
        return Model.of(construction.getIntent());

    }

    private String getAssociationLabel(ContainerValueWrapper<AssignmentType> assignmentWrapper){
        if (assignmentWrapper == null){
            return "";
        }
        ContainerWrapper<ConstructionType> constructionWrapper = assignmentWrapper.findContainerWrapper(assignmentWrapper.getPath()
                .append(AssignmentType.F_CONSTRUCTION));
        if (constructionWrapper == null || constructionWrapper.findContainerValueWrapper(constructionWrapper.getPath()) == null){
            return null;
        }
        ContainerWrapper<ResourceObjectAssociationType> associationWrapper = constructionWrapper.findContainerValueWrapper(constructionWrapper.getPath())
                .findContainerWrapper(constructionWrapper.getPath().append(ConstructionType.F_ASSOCIATION));
        if (associationWrapper == null || associationWrapper.getValues() == null){
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (ContainerValueWrapper<ResourceObjectAssociationType> associationValueWrapper : associationWrapper.getValues()){
            ResourceObjectAssociationType association = associationValueWrapper.getContainerValue().asContainerable();
            if (association.getOutbound() == null || association.getOutbound().getExpression() == null){
                continue;
            }
            ObjectReferenceType shadowRefValue = ExpressionUtil.getShadowRefValue(association.getOutbound().getExpression());
            if (shadowRefValue == null || StringUtils.isEmpty(shadowRefValue.getOid())){
                continue;
            }
            String shadowDisplayName = WebComponentUtil.getDisplayNameOrName(shadowRefValue, getPageBase(), OPERATION_LOAD_SHADOW_DISPLAY_NAME);
            if (sb.length() == 0){
                sb.append(createStringResource("ExpressionValuePanel.shadowRefValueTitle").getString() + ":");
            }
            if (StringUtils.isNotEmpty(shadowDisplayName)){
                sb.append("\n");
                sb.append(shadowDisplayName);
            }
        }
        return sb.toString();

    }

    @Override
    protected List<ContainerValueWrapper<AssignmentType>> postSearch(List<ContainerValueWrapper<AssignmentType>> assignments) {
        List<ContainerValueWrapper<AssignmentType>> filteredAssignments = new ArrayList<>();
        if (assignments == null){
            return filteredAssignments;
        }
        assignments.forEach(assignmentWrapper -> {
                AssignmentType assignment = assignmentWrapper.getContainerValue().asContainerable();
                if (assignment.getConstruction() != null && assignment.getConstruction().getAssociation() != null) {
                    List<ResourceObjectAssociationType> associations = assignment.getConstruction().getAssociation();
                    if (associations.size() == 0 && ValueStatus.ADDED.equals(assignmentWrapper.getStatus())){
                        filteredAssignments.add(assignmentWrapper);
                        return;
                    }
                    associations.forEach(association -> {
                        if (!filteredAssignments.contains(assignmentWrapper)) {
                            if (association.getOutbound() == null && ValueStatus.ADDED.equals(assignmentWrapper.getStatus())) {
                                filteredAssignments.add(assignmentWrapper);
                                return;
                            }
                            if (association.getOutbound() != null && association.getOutbound().getExpression() != null) {
                                ObjectReferenceType shadowRef = ExpressionUtil.getShadowRefValue(association.getOutbound().getExpression());
                                if ((shadowRef != null || ValueStatus.ADDED.equals(assignmentWrapper.getStatus()))) {
                                    filteredAssignments.add(assignmentWrapper);
                                    return;
                                }
                            }
                        }
                    });
            }
        });
        return filteredAssignments;
    }
}
