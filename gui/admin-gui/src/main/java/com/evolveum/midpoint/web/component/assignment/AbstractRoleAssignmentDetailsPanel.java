package com.evolveum.midpoint.web.component.assignment;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PersonaConstructionType;

public class AbstractRoleAssignmentDetailsPanel<F extends FocusType> extends AbstractAssignmentDetailsPanel<F> {

	private static final long serialVersionUID = 1L;

	private static final String ID_RELATION_CONTAINER = "relationContainer";
    private static final String ID_RELATION = "relation";

	private static final String ID_TENANT_CONTAINER = "tenantContainer";
	private static final String ID_TENANT = "tenant";
	private static final String ID_PROJECT_CONTAINER = "projectContainer";
	private static final String ID_PROJECT = "project";
	private static final String ID_POLICY_SITUATIONS = "policySituations";
	private static final String ID_POLICY_SITUATION = "policySituation";

	private static List hiddenItems = new ArrayList<>();

	static  {
			hiddenItems.add(AssignmentType.F_POLICY_RULE);
	};

	public AbstractRoleAssignmentDetailsPanel(String id, Form<?> form, IModel<ContainerValueWrapper<AssignmentType>> assignmentModel) {
		super(id, form, assignmentModel);
	}

	
	@Override
	protected List<ItemPath> collectContainersToShow() {
		List<ItemPath> pathsToShow = new ArrayList<>();
		if (ConstructionType.COMPLEX_TYPE.equals(AssignmentsUtil.getTargetType(getModelObject().getContainerValue().getValue()))) {
			pathsToShow.add(getAssignmentPath().append(AssignmentType.F_CONSTRUCTION));
		}
		
		if (PersonaConstructionType.COMPLEX_TYPE.equals(AssignmentsUtil.getTargetType(getModelObject().getContainerValue().getValue()))) {
			pathsToShow.add(getAssignmentPath().append(AssignmentType.F_PERSONA_CONSTRUCTION));
		}
		return pathsToShow;
	}
	
	protected boolean getVisibilityModel(ItemPath itemToBeFound, ItemPath parentAssignmentPath) {
		return true;
//		AssignmentType assignment = getModelObject().getAssignment();
//		ObjectReferenceType targetRef = assignment.getTargetRef();
//		List<ItemPath> pathsToHide = new ArrayList<>();
//		QName targetType = null;
//		if (targetRef != null) {
//			targetType = targetRef.getType();
//		}
//		pathsToHide.add(parentAssignmentPath.append(AssignmentType.F_TARGET_REF));
//		
//		if (OrgType.COMPLEX_TYPE.equals(targetType)) {
//			pathsToHide.add(parentAssignmentPath.append(AssignmentType.F_TENANT_REF));
//			pathsToHide.add(parentAssignmentPath.append(AssignmentType.F_ORG_REF));
//		}
//		
//		if (assignment.getConstruction() == null) {
//			pathsToHide.add(parentAssignmentPath.append(AssignmentType.F_CONSTRUCTION));
//		}
//		pathsToHide.add(parentAssignmentPath.append(AssignmentType.F_PERSONA_CONSTRUCTION));
//		pathsToHide.add(parentAssignmentPath.append(AssignmentType.F_POLICY_RULE));
//		
//		return !WebComponentUtil.isItemVisible(pathsToHide, itemToBeFound);
	}

//	 private ChooseTypePanel<OrgType> createParameterChooserPanel(String id, ObjectReferenceType ref, boolean isTenant){
//	    	ChooseTypePanel<OrgType> orgSelector = new ChooseTypePanel<OrgType>(id, ref) {
//	
//	    		private static final long serialVersionUID = 1L;
//
//	    		@Override
//	    		protected void executeCustomAction(AjaxRequestTarget target, OrgType object) {
//	    			if (isTenant) {
//	    				AbstractRoleAssignmentDetailsPanel.this.getModelObject().getAssignment().setTenantRef(ObjectTypeUtil.createObjectRef(object));
//	    			} else {
//	    				AbstractRoleAssignmentDetailsPanel.this.getModelObject().getAssignment().setOrgRef(ObjectTypeUtil.createObjectRef(object));
//	    			}
//	    			target.add(AbstractRoleAssignmentDetailsPanel.this);
//	    		}
//	
//	    		@Override
//	    		protected void executeCustomRemoveAction(AjaxRequestTarget target) {
//	    			if (isTenant) {
//	    				AbstractRoleAssignmentDetailsPanel.this.getModelObject().getAssignment().setTenantRef(null);
//	    			} else {
//	    				AbstractRoleAssignmentDetailsPanel.this.getModelObject().getAssignment().setOrgRef(null);
//	    			}
//	    			target.add(AbstractRoleAssignmentDetailsPanel.this);
//	    		}
//	
//	    		@Override
//	    		protected ObjectQuery getChooseQuery() {
//	    			ObjectFilter tenantFilter = QueryBuilder.queryFor(OrgType.class, getPageBase().getPrismContext()).item(OrgType.F_TENANT).eq(true).buildFilter();
//	
//	    			if (isTenant) {
//	    				return ObjectQuery.createObjectQuery(tenantFilter);
//	    			}
//	    			return ObjectQuery.createObjectQuery(NotFilter.createNot(tenantFilter));
//	
//	    		}
//	
//	    		@Override
//	    		protected boolean isSearchEnabled() {
//	    			return true;
//	    		}
//	
//	    		@Override
//	    		public Class<OrgType> getObjectTypeClass() {
//	    			return OrgType.class;
//	    		}
//	
//	    	};
//	    	orgSelector.setOutputMarkupId(true);
//	    	return orgSelector;
//	
//	    	}


//	 private void initStatic(WebMarkupContainer propertiesPanel){
//		 WebMarkupContainer relationContainer = new WebMarkupContainer(ID_RELATION_CONTAINER);
//			DropDownChoicePanel<RelationTypes> relation = WebComponentUtil.createEnumPanel(RelationTypes.class, ID_RELATION,
//	                WebComponentUtil.createReadonlyModelFromEnum(RelationTypes.class), new PropertyModel(getModel(), AssignmentDto.F_RELATION_TYPE), this, true);
//	        relation.getBaseFormComponent().add(new AjaxFormComponentUpdatingBehavior("change") {
//	            private static final long serialVersionUID = 1L;
//
//	            @Override
//	            protected void onUpdate(AjaxRequestTarget target) {
//	            	target.add(AbstractRoleAssignmentDetailsPanel.this);
//	            }
//	        });
//	        relationContainer.add(relation);
//	        propertiesPanel.add(relationContainer);
//	        relationContainer.setOutputMarkupId(true);
//
//	        AssignmentType assignmentType = getModel().getObject().getAssignment();
//
//	        WebMarkupContainer tenantContainer = new WebMarkupContainer(ID_TENANT_CONTAINER);
//	        ChooseTypePanel<OrgType> tenantChooser = createParameterChooserPanel(ID_TENANT, assignmentType.getTenantRef(), true);
//	        tenantContainer.add(tenantChooser);
//	        propertiesPanel.add(tenantContainer);
//	        tenantContainer.setOutputMarkupId(true);
//	        tenantContainer.add(new VisibleEnableBehaviour() {
//
//	        	private static final long serialVersionUID = 1L;
//
//				@Override
//	        	public boolean isVisible() {
//	        		return AbstractRoleAssignmentDetailsPanel.this.isVisible(AssignmentType.F_TENANT_REF);
//	        	}
//	        });
//
//	        WebMarkupContainer projectContainer = new WebMarkupContainer(ID_PROJECT_CONTAINER);
//	        ChooseTypePanel<OrgType> projectChooser = createParameterChooserPanel(ID_PROJECT, assignmentType.getOrgRef(), false);
//	        projectContainer.add(projectChooser);
//	        propertiesPanel.add(projectContainer);
//	        projectContainer.setOutputMarkupId(true);
//	        projectContainer.add(new VisibleEnableBehaviour() {
//
//	        	private static final long serialVersionUID = 1L;
//
//	        	@Override
//	        	public boolean isVisible() {
//	        		return AbstractRoleAssignmentDetailsPanel.this.isVisible(AssignmentType.F_ORG_REF);
//	        	}
//	        });
//
//	        ListView<String> policySituations = new ListView<String>(ID_POLICY_SITUATIONS, new PropertyModel<List<String>>(getModel(), AssignmentDto.F_VALUE + "." + AssignmentType.F_POLICY_SITUATION.getLocalPart())) {
//
//				private static final long serialVersionUID = 1L;
//
//				@Override
//				protected void populateItem(ListItem<String> item) {
//					TextPanel<String> textPanel = new TextPanel<String>(ID_POLICY_SITUATION, item.getModel());
//					textPanel.setOutputMarkupId(true);
//					item.add(textPanel);
//
//				}
//			};
//	        policySituations.setOutputMarkupId(true);
//	        propertiesPanel.add(policySituations);
//	 }
}
