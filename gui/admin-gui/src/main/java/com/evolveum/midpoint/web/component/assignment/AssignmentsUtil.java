package com.evolveum.midpoint.web.component.assignment;

import java.util.Date;
import java.util.function.Function;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang3.StringUtils;
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
import org.exolab.castor.dsml.XML;

/**
 * Created by honchar.
 */
public class AssignmentsUtil {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(AssignmentsUtil.class);

    public static IModel<String> createActivationTitleModel(ActivationType activation, String defaultTitle, BasePanel basePanel) {
        if (activation == null) {
            return Model.of("");
        }
        return createActivationTitleModel(activation.getAdministrativeStatus(), activation.getValidFrom(), activation.getValidTo(), basePanel);
    }

    public static IModel<String> createActivationTitleModel(ActivationStatusType administrativeStatus, XMLGregorianCalendar validFrom, XMLGregorianCalendar validTo,
                                                            BasePanel basePanel) {
        return new AbstractReadOnlyModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                String strEnabled = basePanel.createStringResource(administrativeStatus, "lower", "ActivationStatusType.null")
                        .getString();

                if (validFrom != null && validTo != null) {
                    return basePanel.getString("AssignmentEditorPanel.enabledFromTo", strEnabled,
                            MiscUtil.asDate(validFrom),
                            MiscUtil.asDate(validTo));
                } else if (validFrom != null) {
                    return basePanel.getString("AssignmentEditorPanel.enabledFrom", strEnabled,
                            MiscUtil.asDate(validFrom));
                } else if (validTo != null) {
                    return basePanel.getString("AssignmentEditorPanel.enabledTo", strEnabled,
                            MiscUtil.asDate(validTo));
                }

                return strEnabled;
            }
        };
    }

    public static IModel<String> createActivationTitleModelExperimental(IModel<AssignmentType> model, BasePanel basePanel) {
    	return createActivationTitleModelExperimental(model.getObject(), s -> s.value(), basePanel);
    }

    public static IModel<String> createActivationTitleModelExperimental(AssignmentType assignmentType, Function<ActivationStatusType, String> transformStatusLambda, BasePanel basePanel) {

//    	AssignmentDto assignmentDto = model.getObject();
    	ActivationType activation = assignmentType.getActivation();
    	if (activation == null) {
    		return basePanel.createStringResource("lower.ActivationStatusType.null");
    	}

		TimeIntervalStatusType timeIntervalStatus = activation.getValidityStatus();
		if (timeIntervalStatus != null) {
			return createTimeIntervalStatusMessage(timeIntervalStatus, activation, basePanel);
		}

		ActivationStatusType status = activation.getEffectiveStatus();
		String statusString = transformStatusLambda.apply(status);

                if (activation.getValidFrom() != null && activation.getValidTo() != null) {
                	basePanel.createStringResource("AssignmentEditorPanel.enabledFromTo", statusString, MiscUtil.asDate(activation.getValidFrom()),
                            MiscUtil.asDate(activation.getValidTo()));
                } else if (activation.getValidFrom() != null) {
                    return basePanel.createStringResource("AssignmentEditorPanel.enabledFrom", statusString,
                            MiscUtil.asDate(activation.getValidFrom()));
                } else if (activation.getValidTo() != null) {
                    return basePanel.createStringResource("AssignmentEditorPanel.enabledTo", statusString,
                            MiscUtil.asDate(activation.getValidTo()));
                }

                return basePanel.createStringResource(statusString);

    }
    
    public static IModel<String> createConsentActivationTitleModel(IModel<AssignmentType> model, BasePanel basePanel) {
    	return createActivationTitleModelExperimental(model.getObject(), 
    			s -> { 
    				// TODO: localization
    				switch (s) {
    					case ENABLED:
    						return "Consent given";
    					case ARCHIVED:
    					case DISABLED:
    						return "Consent not given";
    				}
    				return "";
    			}, basePanel);
    }


    private static IModel<String> createTimeIntervalStatusMessage(TimeIntervalStatusType timeIntervalStatus, ActivationType activation, BasePanel basePanel) {
    	switch (timeIntervalStatus) {
			case AFTER:
				return basePanel.createStringResource("ActivationType.validity.after", activation.getValidTo());
			case BEFORE:
				return basePanel.createStringResource("ActivationType.validity.before", activation.getValidFrom());
			case IN:
				return basePanel.createStringResource(activation.getEffectiveStatus());

			default:
				return basePanel.createStringResource(activation.getEffectiveStatus());
		}
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

    public static String createAssignmentStatusClassModel(final ContainerValueWrapper<AssignmentType> assignment) {
        switch (assignment.getStatus()) {
            case ADDED:
                return "success";
            case DELETED:
                return "danger";
            case NOT_CHANGED:
            default:
                return null;
        }
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
			if (StringUtils.isNotEmpty(policyRuleContainer.getName())){
                return policyRuleContainer.getName();
            } else {
			    StringBuilder sb = new StringBuilder("");
			    PolicyConstraintsType constraints = policyRuleContainer.getPolicyConstraints();
			    if (constraints != null && constraints.getExclusion() != null && constraints.getExclusion().size() > 0){
			        sb.append(pageBase.createStringResource("PolicyConstraintsType.exclusion").getString() + ": ");
                    constraints.getExclusion().forEach(exclusion -> {
                        sb.append(WebComponentUtil.getName(exclusion.getTargetRef()));
                        sb.append("; ");
                    });
                }
                return sb.toString();
            }

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

    public static boolean isAssignmentRelevant(AssignmentType assignment) {
        return assignment.getTargetRef() == null ||
                !UserType.COMPLEX_TYPE.equals(assignment.getTargetRef().getType());
    }

    public static boolean isPolicyRuleAssignment(AssignmentType assignment) {
        return assignment.asPrismContainerValue() != null
                && assignment.asPrismContainerValue().findContainer(AssignmentType.F_POLICY_RULE) != null;
    }

    public static boolean isConsentAssignment(AssignmentType assignment) {
        if (assignment.getTargetRef() == null) {
            return false;
        }

        return QNameUtil.match(assignment.getTargetRef().getRelation(), SchemaConstants.ORG_CONSENT);
    }

    /**
     *
     * @return true if this is an assignment of a RoleType, OrgType, ServiceType or Resource
     * @return false if this is an assignment of a User(delegation, deputy) or PolicyRules
     */
    public static boolean isAssignableObject(AssignmentType assignment){
        if (assignment.getPersonaConstruction() != null) {
            return false;
        }

        if (assignment.getPolicyRule() != null) {
            return false;
        }

        //TODO: uncomment when GDPR is in
//		if (assignment.getTargetRef() != null && assignment.getTargetRef().getRelation().equals(SchemaConstants.ORG_CONSENT)) {
//			return false;
//		}

        return true;
    }

    public static QName getTargetType(AssignmentType assignment) {
        if (assignment.getTarget() != null) {
            // object assignment
            return assignment.getTarget().asPrismObject().getComplexTypeDefinition().getTypeName();
        } else if (assignment.getTargetRef() != null) {
            return assignment.getTargetRef().getType();
        }
        if (assignment.getPolicyRule() != null){
            return PolicyRuleType.COMPLEX_TYPE;
        }

        if (assignment.getPersonaConstruction() != null) {
            return PersonaConstructionType.COMPLEX_TYPE;
        }
        // account assignment through account construction
        return ConstructionType.COMPLEX_TYPE;

    }
}
