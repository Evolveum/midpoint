package com.evolveum.midpoint.web.page.admin;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.web.component.form.multivalue.GenericMultiValueLabelEditPanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.page.admin.roles.component.MultiplicityPolicyDialog;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MultiplicityPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

public class BaseAbstractRolePanel extends SimplePanel<AbstractRoleType>{

	private static final String ID_MIN_ASSIGNMENTS = "minAssignmentsConfig";
    private static final String ID_MAX_ASSIGNMENTS = "maxAssignmentsConfig";

    private static final String ID_LABEL_SIZE = "col-md-4";
    private static final String ID_INPUT_SIZE = "col-md-6";

	
	private AbstractRoleType abstractRole;
	
	LoadableModel<List<MultiplicityPolicyConstraintType>> minAssignmentModel;
	LoadableModel<List<MultiplicityPolicyConstraintType>> maxAssignmentsModel;
	
	public BaseAbstractRolePanel(String id, AbstractRoleType abstractRole) {
		super(id);
		this.abstractRole = abstractRole;
		// TODO Auto-generated constructor stub
	}
	
	 private WebMarkupContainer getMinAssignmentsContainer(){
	        return (WebMarkupContainer) get(StringUtils.join(new String[]{"mainForm", ID_MIN_ASSIGNMENTS}, ":"));
	    }

	    private WebMarkupContainer getMaxAssignmentsContainer(){
	        return (WebMarkupContainer) get(StringUtils.join(new String[]{"mainForm", ID_MAX_ASSIGNMENTS}, ":"));
	    }
	    
	    private IModel<String> createMultiplicityPolicyLabel(final IModel<MultiplicityPolicyConstraintType> model){
	        return new AbstractReadOnlyModel<String>() {

	            @Override
	            public String getObject() {
	                StringBuilder sb = new StringBuilder();

	                if(model == null || model.getObject() == null || model.getObject().getMultiplicity() == null
	                        || model.getObject().getMultiplicity().isEmpty()){
	                    return getString("PageRoleEditor.label.assignmentConstraint.placeholder");
	                }

	                MultiplicityPolicyConstraintType policy = model.getObject();

	                sb.append(policy.getMultiplicity());

	                if(policy.getEnforcement() != null){
	                    sb.append(" (");
	                    sb.append(policy.getEnforcement());
	                    sb.append(")");
	                }

	                return sb.toString();
	           }
	        };
	    }

	   

	
	@Override
	protected void initLayout() {
		
		minAssignmentModel = new LoadableModel<List<MultiplicityPolicyConstraintType>>(false) {

            @Override
            protected List<MultiplicityPolicyConstraintType> load() {
//                RoleType role = (RoleType) getFocusWrapper().getObject().asObjectable();

                if(abstractRole.getPolicyConstraints() == null){
                	abstractRole.setPolicyConstraints(new PolicyConstraintsType());
                }

                return abstractRole.getPolicyConstraints().getMinAssignees();
            }
        };

        maxAssignmentsModel = new LoadableModel<List<MultiplicityPolicyConstraintType>>(false) {

            @Override
            protected List<MultiplicityPolicyConstraintType> load() {
//                RoleType role = (RoleType) getFocusWrapper().getObject().asObjectable();

                if(abstractRole.getPolicyConstraints() == null){
                	abstractRole.setPolicyConstraints(new PolicyConstraintsType());
                }

                return abstractRole.getPolicyConstraints().getMaxAssignees();
            }
        };
		
		GenericMultiValueLabelEditPanel minAssignments = new GenericMultiValueLabelEditPanel<MultiplicityPolicyConstraintType>(ID_MIN_ASSIGNMENTS,
                minAssignmentModel, createStringResource("PageRoleEditor.label.minAssignments"), ID_LABEL_SIZE, ID_INPUT_SIZE){

            @Override
            protected void initDialog() {
                ModalWindow dialog = new MultiplicityPolicyDialog(ID_MODAL_EDITOR, null){

                    @Override
                    protected void savePerformed(AjaxRequestTarget target) {
                        closeModalWindow(target);
                        target.add(getMinAssignmentsContainer());
                    }
                };
                add(dialog);
            }

            @Override
            protected IModel<String> createTextModel(IModel<MultiplicityPolicyConstraintType> model) {
                return createMultiplicityPolicyLabel(model);
            }

            @Override
            protected void editValuePerformed(AjaxRequestTarget target, IModel<MultiplicityPolicyConstraintType> rowModel) {
                MultiplicityPolicyDialog window = (MultiplicityPolicyDialog) get(ID_MODAL_EDITOR);
                window.updateModel(target, rowModel.getObject());
                window.show(target);
            }

            @Override
            protected MultiplicityPolicyConstraintType createNewEmptyItem() {
                return new MultiplicityPolicyConstraintType();
            }
        };
        minAssignments.setOutputMarkupId(true);
        add(minAssignments);

        GenericMultiValueLabelEditPanel maxAssignments = new GenericMultiValueLabelEditPanel<MultiplicityPolicyConstraintType>(ID_MAX_ASSIGNMENTS,
                maxAssignmentsModel, createStringResource("PageRoleEditor.label.maxAssignments"), ID_LABEL_SIZE, ID_INPUT_SIZE){

            @Override
            protected void initDialog() {
                ModalWindow dialog = new MultiplicityPolicyDialog(ID_MODAL_EDITOR, null){

                    @Override
                    protected void savePerformed(AjaxRequestTarget target) {
                        closeModalWindow(target);
                        target.add(getMaxAssignmentsContainer());
                    }
                };
                add(dialog);
            }

            @Override
            protected IModel<String> createTextModel(IModel<MultiplicityPolicyConstraintType> model) {
                return createMultiplicityPolicyLabel(model);
            }

            @Override
            protected void editValuePerformed(AjaxRequestTarget target, IModel<MultiplicityPolicyConstraintType> rowModel) {
                MultiplicityPolicyDialog window = (MultiplicityPolicyDialog) get(ID_MODAL_EDITOR);
                window.updateModel(target, rowModel.getObject());
                window.show(target);
            }

            @Override
            protected MultiplicityPolicyConstraintType createNewEmptyItem() {
                return new MultiplicityPolicyConstraintType();
            }
        };
        maxAssignments.setOutputMarkupId(true);
        add(maxAssignments);

	}

}
