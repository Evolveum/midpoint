package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;
import com.evolveum.midpoint.web.session.AssignmentsTabStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * Created by honchar.
 */
public class InducedEntitlementsPanel extends InducementsPanel{

    private static final long serialVersionUID = 1L;


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
    protected ObjectQuery createObjectQuery() {
        ObjectQuery query = super.createObjectQuery();
        ObjectFilter filter = query.getFilter();
        ObjectQuery entitlementsQuery = QueryBuilder.queryFor(AssignmentType.class, getParentPage().getPrismContext())
                .exists(AssignmentType.F_CONSTRUCTION, ConstructionType.F_ASSOCIATION)
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

    protected void initAssociationContainer(ConstructionType constructionType){
        constructionType.beginAssociation();
    }
}
