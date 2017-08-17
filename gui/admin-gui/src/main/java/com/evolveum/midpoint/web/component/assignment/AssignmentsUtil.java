package com.evolveum.midpoint.web.component.assignment;

import java.util.Date;

import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.visit.IVisit;
import org.apache.wicket.util.visit.IVisitor;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleType;

/**
 * Created by honchar.
 */
public class AssignmentsUtil {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(AssignmentsUtil.class);

    public static IModel<String> createActivationTitleModel(IModel<AssignmentEditorDto> model, String defaultTitle, BasePanel basePanel) {
        return new AbstractReadOnlyModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                AssignmentEditorDto dto = model.getObject();
                ActivationType activation = dto.getActivation();
                if (activation == null) {
                    return defaultTitle;
                }

                ActivationStatusType status = activation.getAdministrativeStatus();
                String strEnabled = basePanel.createStringResource(status, "lower", "ActivationStatusType.null")
                        .getString();

                if (activation.getValidFrom() != null && activation.getValidTo() != null) {
                    return basePanel.getString("AssignmentEditorPanel.enabledFromTo", strEnabled,
                            MiscUtil.asDate(activation.getValidFrom()),
                            MiscUtil.asDate(activation.getValidTo()));
                } else if (activation.getValidFrom() != null) {
                    return basePanel.getString("AssignmentEditorPanel.enabledFrom", strEnabled,
                            MiscUtil.asDate(activation.getValidFrom()));
                } else if (activation.getValidTo() != null) {
                    return basePanel.getString("AssignmentEditorPanel.enabledTo", strEnabled,
                            MiscUtil.asDate(activation.getValidTo()));
                }

                return strEnabled;
            }
        };
    }
    
    public static IModel<String> createActivationTitleModelExperimental(IModel<AssignmentDto> model, String defaultTitle, BasePanel basePanel) {
        return new AbstractReadOnlyModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                AssignmentType dto = model.getObject().getAssignment();
                ActivationType activation = dto.getActivation();
                if (activation == null) {
                    return defaultTitle;
                }

                ActivationStatusType status = activation.getAdministrativeStatus();
                String strEnabled = basePanel.createStringResource(status, "lower", "ActivationStatusType.null")
                        .getString();

                if (activation.getValidFrom() != null && activation.getValidTo() != null) {
                    return basePanel.getString("AssignmentEditorPanel.enabledFromTo", strEnabled,
                            MiscUtil.asDate(activation.getValidFrom()),
                            MiscUtil.asDate(activation.getValidTo()));
                } else if (activation.getValidFrom() != null) {
                    return basePanel.getString("AssignmentEditorPanel.enabledFrom", strEnabled,
                            MiscUtil.asDate(activation.getValidFrom()));
                } else if (activation.getValidTo() != null) {
                    return basePanel.getString("AssignmentEditorPanel.enabledTo", strEnabled,
                            MiscUtil.asDate(activation.getValidTo()));
                }

                return strEnabled;
            }
        };
    }

    public static IModel<Date> createDateModel(final IModel<XMLGregorianCalendar> model) {
        return new Model<Date>() {

            @Override
            public Date getObject() {
                XMLGregorianCalendar calendar = model.getObject();
                if (calendar == null) {
                    return null;
                }
                return MiscUtil.asDate(calendar);
            }

            @Override
            public void setObject(Date object) {
                if (object == null) {
                    model.setObject(null);
                } else {
                    model.setObject(MiscUtil.asXMLGregorianCalendar(object));
                }
            }
        };
    }

    public static void addAjaxOnUpdateBehavior(WebMarkupContainer container) {
        container.visitChildren(new IVisitor<Component, Object>() {
            @Override
            public void component(Component component, IVisit<Object> objectIVisit) {
                if (component instanceof InputPanel) {
                    addAjaxOnBlurUpdateBehaviorToComponent(((InputPanel) component).getBaseFormComponent());
                } else if (component instanceof FormComponent) {
                    addAjaxOnBlurUpdateBehaviorToComponent(component);
                }
            }
        });
    }

//    public static IModel<String> createAssignmentStatusClassModel(final IModel<AssignmentEditorDto> model) {
//        return new AbstractReadOnlyModel<String>() {
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            public String getObject() {
//                AssignmentEditorDto dto = model.getObject();
//                return dto.getStatus().name().toLowerCase();
//            }
//        };
//    }
    
    public static IModel<String> createAssignmentStatusClassModel(final IModel<AssignmentDto> model) {
        return new AbstractReadOnlyModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                AssignmentDto dto = model.getObject();
                return dto.getStatus().name().toLowerCase();
            }
        };
    }
    
    public static IModel<String> createAssignmentStatusClassModel(final UserDtoStatus model) {
        return new AbstractReadOnlyModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                return model.name().toLowerCase();
            }
        };
    }

    private static void addAjaxOnBlurUpdateBehaviorToComponent(final Component component) {
        component.setOutputMarkupId(true);
        component.add(new AjaxFormComponentUpdatingBehavior("blur") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
            }
        });
    }

    public static VisibleEnableBehaviour getEnableBehavior(IModel<AssignmentEditorDto> dtoModel){
        return new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled(){
                return dtoModel.getObject().isEditable();
            }
        };
    }

    public static IModel<String> createAssignmentIconTitleModel(BasePanel panel, AssignmentEditorDtoType type){
        return new AbstractReadOnlyModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                if (type == null) {
                    return "";
                }

                switch (type) {
                    case CONSTRUCTION:
                        return panel.getString("MyAssignmentsPanel.type.accountConstruction");
                    case ORG_UNIT:
                        return panel.getString("MyAssignmentsPanel.type.orgUnit");
                    case ROLE:
                        return panel.getString("MyAssignmentsPanel.type.role");
                    case SERVICE:
                        return panel.getString("MyAssignmentsPanel.type.service");
                    case USER:
                        return panel.getString("MyAssignmentsPanel.type.user");
                    case POLICY_RULE:
                        return panel.getString("MyAssignmentsPanel.type.policyRule");
                    default:
                        return panel.getString("MyAssignmentsPanel.type.error");
                }
            }
        };
    }
    
    public static String getName(AssignmentType assignment, PageBase pageBase) {
		if (assignment == null) {
			return null;
		}

		if (assignment.getPolicyRule() != null){
			PolicyRuleType policyRuleContainer = assignment.getPolicyRule();
			return policyRuleContainer.getName();

		}
		StringBuilder sb = new StringBuilder();

		if (assignment.getConstruction() != null) {
			// account assignment through account construction
			ConstructionType construction = assignment.getConstruction();
			if (construction.getResource() != null) {
				sb.append(WebComponentUtil.getName(construction.getResource()));
			} else if (construction.getResourceRef() != null) {
				sb.append(WebComponentUtil.getName(construction.getResourceRef()));
			}
			return sb.toString();
		}

		if (assignment.getTarget() != null) {
			sb.append(WebComponentUtil.getEffectiveName(assignment.getTarget(), OrgType.F_DISPLAY_NAME));
		} else if (assignment.getTargetRef() != null) {
			sb.append(WebComponentUtil.getEffectiveName(assignment.getTargetRef(), OrgType.F_DISPLAY_NAME, pageBase, "loadTargetName"));
		} 
		appendTenantAndOrgName(assignment, sb, pageBase);

		appendRelation(assignment, sb);

		return sb.toString();
	}
    
    private static void appendTenantAndOrgName(AssignmentType assignmentType, StringBuilder sb, PageBase pageBase) {
    	ObjectReferenceType tenantRef = assignmentType.getTenantRef();
		if (tenantRef != null) {
			WebComponentUtil.getEffectiveName(tenantRef, OrgType.F_DISPLAY_NAME, pageBase, "loadTargetName");
		}
		
		ObjectReferenceType orgRef = assignmentType.getOrgRef();
		if (orgRef != null) {
			WebComponentUtil.getEffectiveName(orgRef, OrgType.F_DISPLAY_NAME, pageBase, "loadTargetName");
		}

	}
    
    private static void appendRelation(AssignmentType assignment, StringBuilder sb) {
    	if (assignment.getTargetRef() == null) {
    		return;
    	}
    	sb.append(" - "  + RelationTypes.getRelationType(assignment.getTargetRef().getRelation()).getHeaderLabel());

    }
    
    public static AssignmentEditorDtoType getType(AssignmentType assignment) {
		if (assignment.getTarget() != null) {
			// object assignment
			return AssignmentEditorDtoType.getType(assignment.getTarget().getClass());
		} else if (assignment.getTargetRef() != null) {
			return AssignmentEditorDtoType.getType(assignment.getTargetRef().getType());
		}
		if (assignment.getPolicyRule() != null){
			return AssignmentEditorDtoType.POLICY_RULE;
		}
		
		if (assignment.getPersonaConstruction() != null) {
			return AssignmentEditorDtoType.PERSONA_CONSTRUCTION;
		}
		// account assignment through account construction
		return AssignmentEditorDtoType.CONSTRUCTION;

	}


}
