package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.ChooseTypePanel;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import com.evolveum.midpoint.web.page.admin.users.component.AbstractAssignableSelectionPanel;
import com.evolveum.midpoint.web.page.admin.users.component.AssignableOrgSelectionPage;
import com.evolveum.midpoint.web.page.admin.users.component.AssignableSelectionPage;
import com.evolveum.midpoint.web.page.admin.users.component.AssignableSelectionPanel;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import javax.management.relation.Role;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Honchar.
 */
public class MultipleAssignmentSelectorPanel<F extends FocusType> extends BasePanel<List<AssignmentEditorDto>> {
    private LoadableModel<List<AssignmentEditorDto>> assignmentsModel;
    private static final String ID_BUTTON_REMOVE = "remove";
    private static final String ID_BUTTON_ADD = "add";
    private static final String ID_FORM = "form";
    private static final String ID_AVAILABLE_ASSIGNMENTS = "availableAssignments";
    private static final String ID_CURRENT_ASSIGNMENTS = "currentAssignments";
    private static final String ID_MODAL_ASSIGN = "assignablePopup";
    private static final String ID_MODAL_ASSIGN_ORG = "assignableOrgPopup";
    private static final String ID_CONTAINER_TENANT_REF = "tenantRefContainer";
    private static final String ID_TENANT_CHOOSER = "tenantRefChooser";
    private static final String ID_CONTAINER_ORG_REF = "orgRefContainer";
    private static final String ID_ORG_CHOOSER = "orgRefChooser";

    private static final String DOT_CLASS = MultipleAssignmentSelectorPanel.class.getName();
    private Class<F> type;

    AssignableSelectionPanel.Context assignableSelectionContext;
    AbstractAssignableSelectionPanel.Context assignableOrgSelectionContext;

    AssignmentEditorDto tenantOrgDto = null;

    ObjectDataProvider dataProvider;
    ISortableDataProvider currentAssignmentsProvider;
    private static final Trace LOGGER = TraceManager.getTrace(MultipleAssignmentSelectorPanel.class);

    public MultipleAssignmentSelectorPanel(String id, LoadableModel<List<AssignmentEditorDto>> assignmentsModel, Class<F> type) {
        super(id, assignmentsModel);
        this.assignmentsModel = assignmentsModel;
        this.type = type;
        initLayout();

    }

    private void initLayout() {

        IModel<List<AssignmentEditorDto>> availableAssignmentModel = createAvailableAssignmentModel();
        dataProvider = getAvailableAssignmentsDataProvider();
        final MultipleAssignmentSelector availableAssignmentsPanel = new MultipleAssignmentSelector<F>(ID_AVAILABLE_ASSIGNMENTS, availableAssignmentModel, dataProvider);
        availableAssignmentsPanel.setResetButtonVisibility(false);
        currentAssignmentsProvider = getCurrentAssignmentsDataProvider();
        final MultipleAssignmentSelector currentAssignmentsPanel = new MultipleAssignmentSelector<F>(ID_CURRENT_ASSIGNMENTS, assignmentsModel, currentAssignmentsProvider);

        AjaxButton add = new AjaxButton(ID_BUTTON_ADD) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form form) {
                addToAssignmentsModel(target, availableAssignmentsPanel, currentAssignmentsPanel);
            }
        };

        AjaxButton remove = new AjaxButton(ID_BUTTON_REMOVE) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form form) {
                deleteFromAssignmentsModel(target, currentAssignmentsPanel, availableAssignmentsPanel);
            }
        };

        ModalWindow assignWindowOrg = new ModalWindow(ID_MODAL_ASSIGN_ORG);
        assignableOrgSelectionContext = new AbstractAssignableSelectionPanel.Context(this) {

            @Override
            public MultipleAssignmentSelectorPanel getRealParent() {
                return WebComponentUtil.theSameForPage(MultipleAssignmentSelectorPanel.this, getCallingPageReference());
            }

            @Override
            protected void addPerformed(AjaxRequestTarget target, List<ObjectType> selected) {
                getRealParent().addSelectedAssignablePerformed(target, selected, ID_MODAL_ASSIGN_ORG);
            }

            @Override
            public ObjectQuery getProviderQuery() {
                    return new ObjectQuery();
            }

            @Override
            protected void handlePartialError(OperationResult result) {
            }
        };
        AssignableOrgSelectionPage.prepareDialog(assignWindowOrg, assignableOrgSelectionContext, this, "AssignmentTablePanel.modal.title.selectAssignment", "form");
        add(assignWindowOrg);


        ModalWindow assignWindow = new ModalWindow(ID_MODAL_ASSIGN);
        assignableSelectionContext = new AssignableSelectionPanel.Context(this) {

            @Override
            public MultipleAssignmentSelectorPanel getRealParent() {
                return WebComponentUtil.theSameForPage(MultipleAssignmentSelectorPanel.this, getCallingPageReference());
            }

            @Override
            protected void addPerformed(AjaxRequestTarget target, List<ObjectType> selected) {
                getRealParent().addSelectedAssignablePerformed(target, selected, ID_MODAL_ASSIGN);
            }

            @Override
            public ObjectQuery getProviderQuery() {
                    return new ObjectQuery();
            }

            @Override
            protected void handlePartialError(OperationResult result) {
            }

            @Override
            public PrismObject<UserType> getUserDefinition() {
                try {
                    return getRealParent().getPageBase().getSecurityEnforcer().getPrincipal().getUser().asPrismObject();
                } catch (SecurityViolationException e) {
                    LOGGER.error("Could not retrieve logged user for security evaluation.", e);
                }
                return null;
            }
        };
        AssignableSelectionPage.prepareDialog(assignWindow, assignableSelectionContext, this, "AssignmentTablePanel.modal.title.selectAssignment", "form");
        add(assignWindow);

        WebMarkupContainer tenantRefContainer = createTenantContainer();

        WebMarkupContainer orgRefContainer = createOrgContainer();

        Form<?> form = new Form<Void>(ID_FORM);
        form.add(tenantRefContainer);
        form.add(orgRefContainer);
        form.add(availableAssignmentsPanel);
        form.add(currentAssignmentsPanel);
        form.add(add);
        form.add(remove);
        add(form);
    }

    private IModel<List<AssignmentEditorDto>> createAvailableAssignmentModel() {
        return new IModel<List<AssignmentEditorDto>>() {
            @Override
            public List<AssignmentEditorDto> getObject() {
                return new ArrayList<>();
            }

            @Override
            public void setObject(List<AssignmentEditorDto> assignmentList) {
            }

            @Override
            public void detach() {
            }
        };
    }

    private void addToAssignmentsModel(AjaxRequestTarget target, MultipleAssignmentSelector from, MultipleAssignmentSelector to) {
        List<AssignmentEditorDto> fromProviderList = ((BaseSortableDataProvider) from.getProvider()).getAvailableData();
        List<AssignmentEditorDto> listToBeAdded = new ArrayList<>();
        List<AssignmentEditorDto> assignmentsList = assignmentsModel.getObject();
        if (tenantOrgDto.getOrgRef() != null || tenantOrgDto.getTenantRef() != null) {
            setTenantAndOrgToAssignmentsList(fromProviderList);
        }
        for (AssignmentEditorDto dto : fromProviderList) {
            if (dto.isSelected()) {
                boolean toBeAdded = true;
                for (AssignmentEditorDto assignmentDto : assignmentsList) {
                    if (assignmentDto.getTargetRef().getOid().equals(dto.getTargetRef().getOid())) {
                        if (assignmentDto.getStatus().equals(UserDtoStatus.DELETE)) {
                            assignmentDto.setStatus(UserDtoStatus.MODIFY);
                        }
                        assignmentDto.setTenantRef(dto.getTenantRef());
                        assignmentDto.setOrgRef(dto.getOrgRef());
                        toBeAdded = false;
                    }
                }
                if (toBeAdded) {
                    dto.setStatus(UserDtoStatus.ADD);
                    listToBeAdded.add(dto);
                }
                dto.setSelected(false);
            }
        }
        assignmentsList.addAll(listToBeAdded);
        target.add(to);
        target.add(from);
    }

    private void deleteFromAssignmentsModel(AjaxRequestTarget target, MultipleAssignmentSelector from, MultipleAssignmentSelector to) {
        List<AssignmentEditorDto> fromProviderList = ((BaseSortableDataProvider) from.getProvider()).getAvailableData();
        List<AssignmentEditorDto> listToBeRemoved = new ArrayList<>();
        List<AssignmentEditorDto> assignmentsList = assignmentsModel.getObject();
        for (AssignmentEditorDto dto : fromProviderList) {
            if (dto.isSelected()) {
                for (AssignmentEditorDto assignmentDto : assignmentsList) {
                    if (assignmentDto.getTargetRef().getOid().equals(dto.getTargetRef().getOid())) {
                        if (assignmentDto.getStatus().equals(UserDtoStatus.ADD)) {
                            listToBeRemoved.add(assignmentDto);
                        } else {
                            assignmentDto.setStatus(UserDtoStatus.DELETE);
                        }
                    }
                }
                dto.setSelected(false);
            }
        }
        assignmentsList.removeAll(listToBeRemoved);
        target.add(to);
        target.add(from);

    }



    private ObjectDataProvider getAvailableAssignmentsDataProvider() {
        return new ObjectDataProvider<AssignmentEditorDto, F>(this, type) {

            @Override
            public AssignmentEditorDto createDataObjectWrapper(PrismObject<F> obj) {
                return AssignmentEditorDto.createDtoFromObject(obj.asObjectable(), UserDtoStatus.MODIFY, getPageBase());
            }
        };
    }

    private ISortableDataProvider getCurrentAssignmentsDataProvider() {
        final ISortableDataProvider provider = new ListDataProvider(this, new IModel<List<AssignmentEditorDto>>() {
            @Override
            public List<AssignmentEditorDto> getObject() {
                List<AssignmentEditorDto> currentAssignments = getAssignmentsByType();
                List<AssignmentEditorDto> displayAssignmentsList = new ArrayList<>();
                ObjectQuery  query = ((BaseSortableDataProvider)currentAssignmentsProvider).getQuery();
                if (query != null){
                    ObjectDataProvider temporaryProvider = new ObjectDataProvider(MultipleAssignmentSelectorPanel.this, type);
                    temporaryProvider.setQuery(query);
                    for (AssignmentEditorDto dto : currentAssignments) {
                        Iterator it = temporaryProvider.internalIterator(0, temporaryProvider.size());
                        while (it.hasNext()) {
                            SelectableBean selectableBean = (SelectableBean) it.next();
                            F object = (F) selectableBean.getValue();
                            if (object.getOid().equals(dto.getTargetRef().getOid())) {
                                displayAssignmentsList.add(dto);
                                break;
                            }
                        }
                    }
                    return displayAssignmentsList;
                }
                return currentAssignments;
            }

            @Override
            public void setObject(List<AssignmentEditorDto> list) {
            }

            @Override
            public void detach() {

            }
        });
        ISortState sort = provider.getSortState();
        if (sort != null) {

        }
        return provider;
    }

    private void addSelectedAssignablePerformed(AjaxRequestTarget target, List<ObjectType> selected, String id){

    }

    private WebMarkupContainer createTenantContainer(){
        WebMarkupContainer tenantRefContainer = new WebMarkupContainer(ID_CONTAINER_TENANT_REF);
        ChooseTypePanel tenantRef = new ChooseTypePanel(ID_TENANT_CHOOSER,
                new PropertyModel<ObjectViewDto>(getTenantChooserModel(), AssignmentEditorDto.F_TENANT_REF)){

            @Override
            protected ObjectQuery getChooseQuery(){
                ObjectQuery query = new ObjectQuery();

                ObjectFilter filter = EqualFilter.createEqual(OrgType.F_TENANT, OrgType.class,
                        getPageBase().getPrismContext(), null, true);
                query.setFilter(filter);

                return query;
            }

            @Override
            protected boolean isSearchEnabled() {
                return true;
            }

            @Override
            protected QName getSearchProperty() {
                return OrgType.F_NAME;
            }
        };
        tenantRefContainer.add(tenantRef);
        tenantRefContainer.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                if ((RoleType.class).equals(type)) {
                    return true;
                }
                return false;
            }
        });
        return tenantRefContainer;
    }

    private WebMarkupContainer createOrgContainer(){
        WebMarkupContainer orgRefContainer = new WebMarkupContainer(ID_CONTAINER_ORG_REF);

        ChooseTypePanel orgRef = new ChooseTypePanel(ID_ORG_CHOOSER,
                new PropertyModel<ObjectViewDto>(getTenantChooserModel(), AssignmentEditorDto.F_ORG_REF)){

            @Override
            protected ObjectQuery getChooseQuery(){
                ObjectQuery query = new ObjectQuery();

                ObjectFilter filter = OrFilter.createOr(
                        EqualFilter.createEqual(OrgType.F_TENANT, OrgType.class, getPageBase().getPrismContext(), null, false),
                        EqualFilter.createEqual(OrgType.F_TENANT, OrgType.class, getPageBase().getPrismContext(), null, null));
                query.setFilter(filter);

                return query;
            }

            @Override
            protected boolean isSearchEnabled() {
                return true;
            }

            @Override
            protected QName getSearchProperty() {
                return OrgType.F_NAME;
            }
        };
        orgRefContainer.add(orgRef);
        orgRefContainer.add(new VisibleEnableBehaviour(){

            @Override
            public boolean isVisible() {
                 if((RoleType.class).equals(type)){
                        return true;
                    }

                return false;
            }
        });
        return orgRefContainer;
    }


    private IModel<AssignmentEditorDto> getTenantChooserModel(){
        return new IModel<AssignmentEditorDto>() {
            @Override
            public AssignmentEditorDto getObject() {
                if (tenantOrgDto == null){
                    createTenantOrgDto();
                }
                return tenantOrgDto;
            }

            @Override
            public void setObject(AssignmentEditorDto dto) {
                tenantOrgDto = dto;
            }

            @Override
            public void detach() {

            }
        };
    }

    private void createTenantOrgDto(){
        AssignmentEditorDtoType aType = AssignmentEditorDtoType.ROLE; //doesn't matter the type
        ObjectReferenceType targetRef = new ObjectReferenceType();
        targetRef.setOid("");
        targetRef.setType(aType.getQname());
        targetRef.setTargetName(new PolyStringType(""));

        AssignmentType assignment = new AssignmentType();
        assignment.setTargetRef(targetRef);

        tenantOrgDto = new AssignmentEditorDto(UserDtoStatus.ADD, assignment, getPageBase());

    }

    private void setTenantAndOrgToAssignmentsList(List<AssignmentEditorDto> selectedItems){
        for (AssignmentEditorDto dto : selectedItems){
            if (dto.isSelected()) {
                if (tenantOrgDto.getOrgRef() != null) {
                    dto.setOrgRef(tenantOrgDto.getOrgRef());
                }
                if (tenantOrgDto.getTenantRef() != null) {
                    dto.setTenantRef(tenantOrgDto.getTenantRef());
                }
            }
        }
    }

    private List<AssignmentEditorDto> getAssignmentsByType() {
        List<AssignmentEditorDto> modelObj = assignmentsModel.getObject();
        List<AssignmentEditorDto> currentUsersAssignments = new ArrayList<>();
        for (AssignmentEditorDto dto : modelObj) {
            if (dto.getType().equals(AssignmentEditorDtoType.getType(type)) && !dto.getStatus().equals(UserDtoStatus.DELETE)) {
                currentUsersAssignments.add(dto);
            }
        }
        return currentUsersAssignments;
    }
}