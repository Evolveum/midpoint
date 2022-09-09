/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.assignment;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Created by honchar.
 */
public class AssignmentsUtil {

    public enum AssignmentTypeType {
        CONSTRUCTION, ABSTRACT_ROLE, POLICY_RULE, FOCUS_MAPPING, PERSONA_CONSTRUCTION, ASSIGNMENT_RELATION;
    }

    private static final Trace LOGGER = TraceManager.getTrace(AssignmentsUtil.class);

    public static AssignmentTypeType getAssignmentType(PrismContainerValueWrapper<AssignmentType> assignmentValueWrapper) {
        if (assignmentValueWrapper == null) {
            return AssignmentTypeType.ABSTRACT_ROLE;
        }
        return getAssignmentType(assignmentValueWrapper.getRealValue());
    }

    public static AssignmentTypeType getAssignmentType(AssignmentType assignment) {
        if (assignment.getConstruction() != null) {
            return AssignmentTypeType.CONSTRUCTION;
        }

        if (assignment.getPolicyRule() != null) {
            return AssignmentTypeType.POLICY_RULE;
        }

        if (assignment.getFocusMappings() != null) {
            return AssignmentTypeType.FOCUS_MAPPING;
        }

        if (assignment.getPersonaConstruction() != null) {
            return AssignmentTypeType.PERSONA_CONSTRUCTION;
        }

//        if (assignment.getAssignmentRelation() != null) {
//            return AssignmentTypeType.ASSIGNMENT_RELATION;
//        }

        return AssignmentTypeType.ABSTRACT_ROLE;

    }

    public static String createActivationTitleModel(ActivationType activation, String defaultTitle, PageBase basePanel) {
        if (activation == null) {
            return defaultTitle;
        }
        return createActivationTitleModel(activation.getAdministrativeStatus(), activation.getValidFrom(), activation.getValidTo(), basePanel);
    }

    public static String createActivationTitleModel(ActivationStatusType administrativeStatus, XMLGregorianCalendar validFrom, XMLGregorianCalendar validTo,
            PageBase basePanel) {
        IModel<String> label = new IModel<String>() {
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

        return label.getObject();
    }

    public static IModel<String> createActivationTitleModelExperimental(AssignmentType assignmentType, Function<ActivationStatusType, String> transformStatusLambda, BasePanel basePanel) {

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

    @SuppressWarnings("unchecked")
    private static IModel<String> createTimeIntervalStatusMessage(TimeIntervalStatusType timeIntervalStatus, ActivationType activation, BasePanel basePanel) {
        switch (timeIntervalStatus) {
            case AFTER:
                return basePanel.createStringResource("ActivationType.validity.after", activation.getValidTo());
            case BEFORE:
                return basePanel.createStringResource("ActivationType.validity.before", activation.getValidFrom());
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

    public static IModel<String> createAssignmentStatusClassModel(final UserDtoStatus model) {
        return new IModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                return model.name().toLowerCase();
            }
        };
    }

    public static IModel<String> createAssignmentIconTitleModel(BasePanel panel, AssignmentEditorDtoType type) {
        return new IModel<String>() {
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

    public static String getName(PrismContainerValueWrapper<AssignmentType> assignmentValueWrapper, PageBase pageBase) {
        AssignmentType assignment = assignmentValueWrapper.getRealValue();
        return getName(assignment, pageBase);
    }

    public static String getName(AssignmentType assignment, PageBase pageBase) {
        if (assignment == null) {
            return null;
        }

        if (assignment.getPolicyRule() != null) {
            StringBuilder sbName = new StringBuilder();
            String policyRuleName = assignment.getPolicyRule().getName();

            if (StringUtils.isNotEmpty(policyRuleName)) {
                sbName.append(policyRuleName).append("\n");
            }

            if (StringUtils.isNotEmpty(sbName.toString())) {
                return sbName.toString();
            } else {
                PolicyRuleType policyRuleContainer = assignment.getPolicyRule();
                StringBuilder sb = new StringBuilder();
                PolicyConstraintsType constraints = policyRuleContainer.getPolicyConstraints();
                if (constraints != null && constraints.getExclusion() != null && constraints.getExclusion().size() > 0) {
                    if (pageBase == null) {
                        sb.append(PageBase.createStringResourceStatic("PolicyConstraintsType.exclusion").getString()).append(": ");
                    } else {
                        sb.append(pageBase.createStringResource("PolicyConstraintsType.exclusion").getString()).append(": ");
                    }
                    constraints.getExclusion().forEach(exclusion -> {
                        sb.append(WebComponentUtil.getName(exclusion.getTargetRef(), true));
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
            if (construction.getResourceRef() != null) {
                sb.append(WebComponentUtil.getName(construction.getResourceRef(), true));
            }
            return sb.toString();
        }

        //TODO fix this.. what do we want to show in the name columns in the case of assignmentRelation assignment??
        if (assignment.getAssignmentRelation() != null && !assignment.getAssignmentRelation().isEmpty()) {
            for (AssignmentRelationType assignmentRelation : assignment.getAssignmentRelation()) {
                sb.append("Assignment relation");
                List<QName> holders = assignmentRelation.getHolderType();
                if (!holders.isEmpty()) {
                    sb.append(": ").append(holders.iterator().next());
                }

            }
            String name = sb.toString();
            if (name.length() > 1) {
                return name;
            }
        }

        ObjectReferenceType targetRef = assignment.getTargetRef();
        if (isNotEmptyRef(targetRef)) {
            PrismObject<? extends ObjectType> targetObject = targetRef.getObject();
            if (targetObject != null) {
                return WebComponentUtil.getEffectiveName(targetObject, OrgType.F_DISPLAY_NAME);
            }

            if (pageBase == null) {
                PolyStringType targetName = targetRef.getTargetName();
                if (targetName != null) {
                    sb.append(WebComponentUtil.getOrigStringFromPoly(targetName)).append(" - ");
                }
                QName type = targetRef.getType();
                if (type != null) {
                    sb.append(type.getLocalPart());
                }
            } else {
                sb.append(WebComponentUtil.getEffectiveName(assignment.getTargetRef(), OrgType.F_DISPLAY_NAME, pageBase,
                        "loadTargetName", true));
            }
        }

//        appendTenantAndOrgName(assignment, pageBase);

        if (sb.toString().isEmpty() && assignment.getFocusMappings() != null) {
            sb.append("Focus mapping - ");
            List<MappingType> mappings = assignment.getFocusMappings().getMapping();
            Iterator<MappingType> mappingsIterator = mappings.iterator();
            while (mappingsIterator.hasNext()) {
                MappingType mapping = mappingsIterator.next();
                String name = mapping.getName();
                if (name == null) {
                    name = mapping.getDescription();
                }

                if (name == null) {
                    VariableBindingDefinitionType target = mapping.getTarget();
                    if (target != null) {
                        name = target.getPath().toString();
                    }
                }

                //should no happened
                if (name == null) {
                    sb.append("Unknown");
                }

                sb.append(name);
                if (mappingsIterator.hasNext()) {
                    sb.append(", ");
                }

            }
        }
        return sb.toString();
    }

    private static boolean isNotEmptyRef(ObjectReferenceType ref) {
        return ref != null && ref.getOid() != null && ref.getType() != null;
    }

    public static String getAssignmentSpecificInfoLabel(AssignmentType assignmentType, PageBase pageBase) {
        if (assignmentType == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (assignmentType.getConstruction() != null) {
            ShadowKindType kindValue = assignmentType.getConstruction().getKind();
            if (kindValue != null) {
                sb.append(pageBase.createStringResource("AssignmentPanel.kind").getString());
                sb.append(" ");
                sb.append(kindValue.value());
            }
            String intentValue = assignmentType.getConstruction().getIntent();
            if (StringUtils.isNotEmpty(intentValue)) {
                if (StringUtils.isNotEmpty(sb.toString())) {
                    sb.append(", ");
                }
                sb.append(pageBase.createStringResource("AssignmentPanel.intent").getString());
                sb.append(" ");
                sb.append(intentValue);
            }
            return sb.toString();
        }

        ObjectReferenceType targetRefObj = assignmentType.getTargetRef();
        if (targetRefObj != null) {
            sb.append(pageBase.createStringResource("AbstractRoleAssignmentPanel.relationLabel").getString());
            sb.append(": ");
            String relationDisplayName = WebComponentUtil.getRelationHeaderLabelKeyIfKnown(targetRefObj.getRelation());
            sb.append(StringUtils.isNotEmpty(relationDisplayName) ?
                    pageBase.createStringResource(relationDisplayName).getString() :
                    pageBase.createStringResource(targetRefObj.getRelation().getLocalPart()).getString());
        }
        ObjectReferenceType tenantRef = assignmentType.getTenantRef();
        if (tenantRef != null && tenantRef.getOid() != null) {
            String tenantDisplayName = WebComponentUtil.getEffectiveName(tenantRef, OrgType.F_DISPLAY_NAME, pageBase, "loadTenantName");
            if (StringUtils.isNotEmpty(tenantDisplayName)) {
                if (StringUtils.isNotEmpty(sb.toString())) {
                    sb.append(", ");
                }
                sb.append(pageBase.createStringResource("roleMemberPanel.tenant").getString());
                sb.append(" ");
                sb.append(tenantDisplayName);
            }
        }

        ObjectReferenceType orgRef = assignmentType.getOrgRef();
        if (orgRef != null && orgRef.getOid() != null) {
            String orgDisplayName = WebComponentUtil.getEffectiveName(orgRef, OrgType.F_DISPLAY_NAME, pageBase, "loadOrgName");
            if (StringUtils.isNotEmpty(orgDisplayName)) {
                if (StringUtils.isNotEmpty(sb.toString())) {
                    sb.append(", ");
                }
                sb.append(pageBase.createStringResource("roleMemberPanel.project").getString());
                sb.append(" ");
                sb.append(orgDisplayName);
            }
        }

        return sb.toString();
    }

    public static AssignmentEditorDtoType getType(AssignmentType assignment) {
        ObjectReferenceType targetRef = assignment.getTargetRef();
        if (targetRef.asReferenceValue().getObject() != null) {
            // object assignment
            return AssignmentEditorDtoType.getType(targetRef.asReferenceValue().getObject().getCompileTimeClass());
        } else if (assignment.getTargetRef() != null) {
            return AssignmentEditorDtoType.getType(assignment.getTargetRef().getType());
        }
        if (assignment.getPolicyRule() != null) {
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

    public static boolean isArchetypeAssignment(AssignmentType assignment) {
        return assignment.getTargetRef() != null
                && ArchetypeType.COMPLEX_TYPE.equals(assignment.getTargetRef().getType());
    }

    public static boolean isConsentAssignment(AssignmentType assignment) {
        if (assignment.getTargetRef() == null) {
            return false;
        }

        return QNameUtil.match(assignment.getTargetRef().getRelation(), SchemaConstants.ORG_CONSENT);
    }

    public static QName getTargetType(AssignmentType assignment) {
        if (assignment.getConstruction() != null) {
            return ConstructionType.COMPLEX_TYPE;
        }
        if (assignment.getPolicyRule() != null) {
            return PolicyRuleType.COMPLEX_TYPE;
        }
        if (assignment.getPersonaConstruction() != null) {
            return PersonaConstructionType.COMPLEX_TYPE;
        }
        if (assignment.getFocusMappings() != null) {
            return MappingType.COMPLEX_TYPE;
        }
        ObjectReferenceType targetRef = assignment.getTargetRef();
        if (targetRef != null && targetRef.asReferenceValue().getObject() != null) {
            // object assignment
            return targetRef.asReferenceValue().getObject().getComplexTypeDefinition().getTypeName();
        } else if (assignment.getTargetRef() != null && assignment.getTargetRef().getType() != null) {
            return assignment.getTargetRef().getType();
        }
        // account assignment through account construction
        return ConstructionType.COMPLEX_TYPE;

    }

    public static int loadAssignmentsLimit(OperationResult result, PageBase pageBase) {
        int assignmentsLimit = -1;
        try {
            CompiledGuiProfile adminGuiConfig = pageBase.getModelInteractionService().getCompiledGuiProfile(
                    pageBase.createSimpleTask(result.getOperation()), result);
            if (adminGuiConfig.getRoleManagement() != null) {
                assignmentsLimit = adminGuiConfig.getRoleManagement().getAssignmentApprovalRequestLimit();
            }
        } catch (ObjectNotFoundException | SchemaException | CommunicationException | ConfigurationException |
                SecurityViolationException | ExpressionEvaluationException ex) {
            LOGGER.error("Error getting system configuration: {}", ex.getMessage(), ex);
        }
        return assignmentsLimit;
    }

    public static <AR extends AbstractRoleType> IModel<String> getIdentifierLabelModel(AssignmentType assignment, PageBase pageBase) {
        if (assignment == null) {
            return Model.of("");
        }
        PrismObject<AR> targetObject = loadTargetObject(assignment, pageBase);
        if (targetObject != null) {
            AR targetRefObject = targetObject.asObjectable();
            if (StringUtils.isNotEmpty(targetRefObject.getIdentifier())) {
                return Model.of(targetRefObject.getIdentifier());
            }
            if (targetRefObject.getDisplayName() != null && !targetRefObject.getName().getOrig().equals(targetRefObject.getDisplayName().getOrig())) {
                return Model.of(targetRefObject.getName().getOrig());
            }
        }
        return Model.of("");
    }

    public static <F extends FocusType> PrismObject<F> loadTargetObject(AssignmentType assignmentType, PageBase pageBase) {
        if (assignmentType == null) {
            return null;
        }

        ObjectReferenceType targetRef = assignmentType.getTargetRef();
        if (targetRef == null || targetRef.getOid() == null) {
            return null;
        }

        PrismObject<F> targetObject = targetRef.getObject();
        if (targetObject == null) {
            Task task = pageBase.createSimpleTask("load assignment targets");
            OperationResult result = task.getResult();
            targetObject = WebModelServiceUtils.loadObject(targetRef, pageBase, task, result);
            result.recomputeStatus();
        }
        return targetObject;
    }

}
