/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.assignment;

import java.util.Date;
import java.util.List;
import java.util.function.Function;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.factory.ItemRealValueModel;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyWrapper;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.web.session.RoleCatalogStorage;

/**
 * Created by honchar.
 */
public class AssignmentsUtil {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(AssignmentsUtil.class);

    public static String createActivationTitleModel(ActivationType activation, String defaultTitle, PageBase basePanel) {
        if (activation == null) {
            return"";
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

    public static IModel<String> createActivationTitleModelExperimental(IModel<AssignmentType> model, BasePanel basePanel) {
        return createActivationTitleModelExperimental(model.getObject(), s -> s.value(), basePanel);
    }

    public static IModel<String> createActivationTitleModelExperimental(AssignmentType assignmentType, Function<ActivationStatusType, String> transformStatusLambda, BasePanel basePanel) {

//        AssignmentDto assignmentDto = model.getObject();
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

    public static IModel<String> createAssignmentStatusClassModel(final UserDtoStatus model) {
        return new IModel<String>() {
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

        if (assignment == null) {
            return null;
        }

        if (assignment.getPolicyRule() != null){
            StringBuilder sbName = new StringBuilder("");
            PrismPropertyWrapper<String> property;
            try {
                property = assignmentValueWrapper.findProperty(ItemPath.create(AssignmentType.F_POLICY_RULE, PolicyRuleType.F_NAME));
            } catch (SchemaException e) {
                LOGGER.error("Cannot find name property for policy rules.");
                pageBase.getSession().error("Cannot find name for the policy rule");
                return null;
            }
            if (property != null && !property.getValues().isEmpty()) {
                for (PrismPropertyValueWrapper<String> value : property.getValues()) {
                    ItemRealValueModel<String> name = new ItemRealValueModel<String>(Model.of(value));
                    if (StringUtils.isNotEmpty(name.getObject())) {
                        sbName.append(name.getObject()).append("\n");
                    }
                }
            }
            if (StringUtils.isNotEmpty(sbName.toString())){
                return sbName.toString();
            } else {
                PolicyRuleType policyRuleContainer = assignment.getPolicyRule();
                StringBuilder sb = new StringBuilder("");
                PolicyConstraintsType constraints = policyRuleContainer.getPolicyConstraints();
                if (constraints != null && constraints.getExclusion() != null && constraints.getExclusion().size() > 0){
                    sb.append(pageBase.createStringResource("PolicyConstraintsType.exclusion").getString() + ": ");
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

        //TODO fix this.. what do we want to show in the name columns in the case of assignemtnRelation assignemt??
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

        if (isNotEmptyRef(assignment.getTargetRef())) {
            sb.append(WebComponentUtil.getEffectiveName(assignment.getTargetRef(), OrgType.F_DISPLAY_NAME, pageBase,
                    "loadTargetName", true));
        }

//        appendTenantAndOrgName(assignment, pageBase);

        if(sb.toString().isEmpty() && assignment.getFocusMappings() != null) {
            for(MappingType mapping : assignment.getFocusMappings().getMapping()) {
                String name = mapping.getName() == null ? "" : mapping.getName();
                String description = mapping.getDescription() == null ? "" : mapping.getDescription();
                if(name.isEmpty()) {
                    sb.append(!description.isEmpty() ? "Mapping - " + description + "\n": "");
                } else {
                    sb.append(name);
                    sb.append(!description.isEmpty() ? " - " + description + "\n" : "\n");
                }
            }
        }
        return sb.toString();
    }


    private static boolean isNotEmptyRef(ObjectReferenceType ref) {
        return ref != null && ref.getOid() != null && ref.getType() != null;
    }

    public static String getAssignmentSpecificInfoLabel(AssignmentType assignmentType, PageBase pageBase) {
        if (assignmentType == null){
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (assignmentType.getConstruction() != null){
            ShadowKindType kindValue = assignmentType.getConstruction().getKind();
            if (kindValue != null){
                sb.append(pageBase.createStringResource("AssignmentPanel.kind").getString());
                sb.append(" ");
                sb.append(kindValue.value());
            }
            String intentValue = assignmentType.getConstruction().getIntent();
            if (StringUtils.isNotEmpty(intentValue)){
                if (StringUtils.isNotEmpty(sb.toString())){
                    sb.append(", ");
                }
                sb.append(pageBase.createStringResource("AssignmentPanel.intent").getString());
                sb.append(" ");
                sb.append(intentValue);
            }
            return sb.toString();
        }

        ObjectReferenceType targetRefObj = assignmentType.getTargetRef();
        if (targetRefObj != null && !SchemaConstants.ORG_DEFAULT.equals(targetRefObj.getRelation())) {
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
            if (StringUtils.isNotEmpty(tenantDisplayName)){
                if (StringUtils.isNotEmpty(sb.toString())){
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
            if (StringUtils.isNotEmpty(orgDisplayName)){
                if (StringUtils.isNotEmpty(sb.toString())){
                    sb.append(", ");
                }
                sb.append(pageBase.createStringResource("roleMemberPanel.project").getString());
                sb.append(" ");
                sb.append(orgDisplayName);
            }
        }

        return sb.toString();
    }

    private static void appendRelation(AssignmentType assignment, StringBuilder sb, PageBase pageBase) {
        if (assignment.getTargetRef() == null) {
            return;
        }

        String labelKey = WebComponentUtil.getRelationHeaderLabelKeyIfKnown(assignment.getTargetRef().getRelation());
        if (StringUtils.isNotEmpty(labelKey)) {
            sb.append(" - ").append(pageBase.createStringResource(labelKey).getString());
        }

    }

    public static AssignmentEditorDtoType getType(AssignmentType assignment) {
        ObjectReferenceType targetRef = assignment.getTargetRef();
        if (targetRef.asReferenceValue().getObject() != null) {
            // object assignment
            return AssignmentEditorDtoType.getType(targetRef.asReferenceValue().getObject().getCompileTimeClass());
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
//        if (assignment.getTargetRef() != null && assignment.getTargetRef().getRelation().equals(SchemaConstants.ORG_CONSENT)) {
//            return false;
//        }

        return true;
    }

    public static QName getTargetType(AssignmentType assignment) {
        if (assignment.getConstruction() != null) {
            return ConstructionType.COMPLEX_TYPE;
        }
        ObjectReferenceType targetRef = assignment.getTargetRef();
        if (targetRef.asReferenceValue().getObject() != null) {
            // object assignment
            return targetRef.asReferenceValue().getObject().getComplexTypeDefinition().getTypeName();
        } else if (assignment.getTargetRef() != null && assignment.getTargetRef().getType() != null) {
            return assignment.getTargetRef().getType();
        }
        if (assignment.getPolicyRule() != null){
            return PolicyRuleType.COMPLEX_TYPE;
        }

        if (assignment.getPersonaConstruction() != null) {
            return PersonaConstructionType.COMPLEX_TYPE;
        }
        if (assignment.getFocusMappings() != null){
            return MappingType.COMPLEX_TYPE;
        }
        // account assignment through account construction
        return ConstructionType.COMPLEX_TYPE;

    }

    public static IModel<String> getShoppingCartAssignmentsLimitReachedTitleModel(PageBase pageBase){
                return new LoadableModel<String>(true) {
            @Override
            protected String load() {
                int assignmentsLimit = pageBase.getSessionStorage().getRoleCatalog().getAssignmentRequestLimit();
                return isShoppingCartAssignmentsLimitReached(assignmentsLimit, pageBase) ?
                                                pageBase.createStringResource("RoleCatalogItemButton.assignmentsLimitReachedTitle", assignmentsLimit)
                                                        .getString() : "";
                            }
        };
            }

            public static boolean isShoppingCartAssignmentsLimitReached(int assignmentsLimit, PageBase pageBase){
                RoleCatalogStorage storage = pageBase.getSessionStorage().getRoleCatalog();
                return assignmentsLimit >= 0 && storage.getAssignmentShoppingCart().size() >= assignmentsLimit;
            }

    public static int loadAssignmentsLimit(OperationResult result, PageBase pageBase){
        int assignmentsLimit = -1;
        try {
            CompiledGuiProfile adminGuiConfig = pageBase.getModelInteractionService().getCompiledGuiProfile(
                    pageBase.createSimpleTask(result.getOperation()), result);//pageBase.loadUserSelf().asObjectable().getAdminGuiConfiguration();
            if (adminGuiConfig != null && adminGuiConfig.getRoleManagement() != null){
                assignmentsLimit = adminGuiConfig.getRoleManagement().getAssignmentApprovalRequestLimit();
            }
        } catch (ObjectNotFoundException | SchemaException | CommunicationException | ConfigurationException | SecurityViolationException | ExpressionEvaluationException ex){
            LOGGER.error("Error getting system configuration: {}", ex.getMessage(), ex);
        }
        return assignmentsLimit;
    }

}
