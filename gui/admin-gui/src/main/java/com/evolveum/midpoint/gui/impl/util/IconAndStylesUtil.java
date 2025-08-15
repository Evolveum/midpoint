/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.util;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.page.admin.server.dto.OperationResultStatusPresentationProperties;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.List;

public class IconAndStylesUtil {

    public static <T extends ObjectType> String createDefaultIcon(PrismObject<T> object) {
        Class<T> type = object.getCompileTimeClass();
        if (type.equals(UserType.class)) {
            return createUserIcon((PrismObject<UserType>) object);
        } else if (type.equals(ApplicationType.class)) {
            return createApplicationIcon();
        } else if (RoleType.class.equals(type)) {
            return createRoleIcon((PrismObject<RoleType>) object);
        } else if (OrgType.class.equals(type)) {
            return createOrgIcon();
        } else if (ServiceType.class.equals(type)) {
            return createServiceIcon();
        } else if (type.equals(TaskType.class)) {
            return createTaskIcon();
        } else if (type.equals(ResourceType.class)) {
            return createResourceIcon((PrismObject<ResourceType>) object);
        } else if (type == ShadowType.class) {
            return createShadowIcon((PrismObject<ShadowType>) object);
        } else if (type == ObjectCollectionType.class) {
            return createObjectColletionIcon();
        } else if (type == ReportType.class) {
            return createReportIcon();
        } else if (type == ObjectTemplateType.class) {
            return createObjectTemplateIcon();
        } else if (type == SimulationResultType.class) {
            return createSimulationResultIcon();
        } else if (type == MarkType.class) {
            return createMarkIcon();
        } else if (type == RoleAnalysisSessionType.class) {
            return createRoleAnalysisSessionIcon();
        } else if (type == RoleAnalysisClusterType.class) {
            return createRoleAnalysisClusterIcon();
        } else if (type == RoleAnalysisOutlierType.class) {
            return createRoleAnalysisOutlierIcon();
        } else if (type == SchemaType.class) {
            return createSchemaIcon();
        } else if (type == PolicyType.class) {
            return createPolicyIcon();
        } else if (type == AccessCertificationCampaignType.class) {
            return createCertCampaignIcon();
        } else if (type == AccessCertificationDefinitionType.class) {
            return createCertCampaignDefinitionIcon();
        }
        return "";
    }

    private static String createCertCampaignDefinitionIcon() {
        return getObjectNormalIconStyle(GuiStyleConstants.CLASS_OBJECT_CERT_DEF_ICON);
    }

    private static String createCertCampaignIcon() {
        return getObjectNormalIconStyle(GuiStyleConstants.CLASS_OBJECT_CERT_CAMPAIGN_ICON);
    }

    private static String createPolicyIcon() {
        return getObjectNormalIconStyle(GuiStyleConstants.CLASS_OBJECT_POLICY_ICON);
    }

    private static String createSchemaIcon() {
        return getObjectNormalIconStyle(GuiStyleConstants.CLASS_OBJECT_SCHEMA_TEMPLATE_ICON);
    }

    // TODO reconcile with ObjectTypeGuiDescriptor
    public static <T extends ObjectType> String createDefaultColoredIcon(QName objectType) {
        if (objectType == null) {
            return "";
        } else if (QNameUtil.match(UserType.COMPLEX_TYPE, objectType) || QNameUtil.match(PersonaConstructionType.COMPLEX_TYPE, objectType)) {
            return GuiStyleConstants.CLASS_OBJECT_USER_ICON_COLORED;
        } else if (QNameUtil.match(RoleType.COMPLEX_TYPE, objectType)) {
            return GuiStyleConstants.CLASS_OBJECT_ROLE_ICON_COLORED;
        } else if (QNameUtil.match(OrgType.COMPLEX_TYPE, objectType)) {
            return GuiStyleConstants.CLASS_OBJECT_ORG_ICON_COLORED;
        } else if (QNameUtil.match(ServiceType.COMPLEX_TYPE, objectType)) {
            return GuiStyleConstants.CLASS_OBJECT_SERVICE_ICON_COLORED;
        } else if (QNameUtil.match(TaskType.COMPLEX_TYPE, objectType)) {
            return GuiStyleConstants.CLASS_OBJECT_TASK_ICON_COLORED;
        } else if (QNameUtil.match(ResourceType.COMPLEX_TYPE, objectType) || QNameUtil.match(ConstructionType.COMPLEX_TYPE, objectType)) {
            return GuiStyleConstants.CLASS_OBJECT_RESOURCE_ICON_COLORED;
        } else if (QNameUtil.match(AccessCertificationCampaignType.COMPLEX_TYPE, objectType)) {
            return GuiStyleConstants.CLASS_OBJECT_CERT_CAMPAIGN_ICON_COLORED;
        } else if (QNameUtil.match(AccessCertificationDefinitionType.COMPLEX_TYPE, objectType)) {
            return GuiStyleConstants.CLASS_OBJECT_CERT_DEF_ICON_COLORED;
        } else if (QNameUtil.match(CaseWorkItemType.COMPLEX_TYPE, objectType)) {
            return GuiStyleConstants.CLASS_OBJECT_WORK_ITEM_ICON_COLORED;
        } else if (QNameUtil.match(ShadowType.COMPLEX_TYPE, objectType)) {
            return GuiStyleConstants.CLASS_OBJECT_SHADOW_ICON_COLORED;
        } else if (QNameUtil.match(PolicyRuleType.COMPLEX_TYPE, objectType)) {
            return GuiStyleConstants.CLASS_POLICY_RULES_ICON_COLORED;
        } else if (QNameUtil.match(ObjectPolicyConfigurationType.COMPLEX_TYPE, objectType) || QNameUtil.match(GlobalPolicyRuleType.COMPLEX_TYPE, objectType)
                || QNameUtil.match(FileAppenderConfigurationType.COMPLEX_TYPE, objectType) || QNameUtil.match(SyslogAppenderConfigurationType.COMPLEX_TYPE, objectType)) {
            return GuiStyleConstants.CLASS_SYSTEM_CONFIGURATION_ICON_COLORED;
        } else if (QNameUtil.match(ResourceObjectTypeDefinitionType.COMPLEX_TYPE, objectType)) {
            return GuiStyleConstants.CLASS_ICON_RESOURCE_SCHEMA_HANDLING_COLORED;
        } else if (QNameUtil.match(ResourceAttributeDefinitionType.COMPLEX_TYPE, objectType)) {
            return "fa fa-navicon";
        } else if (QNameUtil.match(RoleAnalysisClusterType.COMPLEX_TYPE, objectType)) {
            return GuiStyleConstants.CLASS_ROLE_ANALYSIS_CLUSTER_ICON;
        } else if (QNameUtil.match(RoleAnalysisSessionType.COMPLEX_TYPE, objectType)) {
            return GuiStyleConstants.CLASS_ROLE_ANALYSIS_SESSION_ICON;
        } else if (QNameUtil.match(RoleAnalysisOutlierType.COMPLEX_TYPE, objectType)) {
            return GuiStyleConstants.CLASS_OUTLIER_ICON;
        } else {
            return "";
        }
    }

    // TODO reconcile with ObjectTypeGuiDescriptor
    public static <T extends ObjectType> String createDefaultBlackIcon(QName objectType) {
        if (objectType == null) {
            return "";
        } else if (QNameUtil.match(UserType.COMPLEX_TYPE, objectType) || QNameUtil.match(PersonaConstructionType.COMPLEX_TYPE, objectType)) {
            return GuiStyleConstants.CLASS_OBJECT_USER_ICON;
        } else if (QNameUtil.match(ApplicationType.COMPLEX_TYPE, objectType)) {
            return GuiStyleConstants.CLASS_OBJECT_APPLICATION_ICON;
        }else if (QNameUtil.match(RoleType.COMPLEX_TYPE, objectType)) {
            return GuiStyleConstants.CLASS_OBJECT_ROLE_ICON;
        } else if (QNameUtil.match(OrgType.COMPLEX_TYPE, objectType)) {
            return GuiStyleConstants.CLASS_OBJECT_ORG_ICON;
        } else if (QNameUtil.match(ServiceType.COMPLEX_TYPE, objectType)) {
            return GuiStyleConstants.CLASS_OBJECT_SERVICE_ICON;
        } else if (QNameUtil.match(PolicyType.COMPLEX_TYPE, objectType)) {
            return GuiStyleConstants.CLASS_OBJECT_POLICY_ICON;
        } else if (QNameUtil.match(TaskType.COMPLEX_TYPE, objectType)) {
            return GuiStyleConstants.CLASS_OBJECT_TASK_ICON;
        } else if (QNameUtil.match(ResourceType.COMPLEX_TYPE, objectType) || QNameUtil.match(ConstructionType.COMPLEX_TYPE, objectType)) {
            return GuiStyleConstants.CLASS_OBJECT_RESOURCE_ICON;
        } else if (QNameUtil.match(AccessCertificationCampaignType.COMPLEX_TYPE, objectType)) {
            return GuiStyleConstants.CLASS_OBJECT_CERT_CAMPAIGN_ICON;
        } else if (QNameUtil.match(AccessCertificationDefinitionType.COMPLEX_TYPE, objectType)) {
            return GuiStyleConstants.CLASS_OBJECT_CERT_DEF_ICON;
        } else if (QNameUtil.match(CaseWorkItemType.COMPLEX_TYPE, objectType)) {
            return GuiStyleConstants.CLASS_OBJECT_WORK_ITEM_ICON;
        } else if (QNameUtil.match(ShadowType.COMPLEX_TYPE, objectType)) {
            return GuiStyleConstants.CLASS_OBJECT_SHADOW_ICON;
        } else if (QNameUtil.match(PolicyRuleType.COMPLEX_TYPE, objectType)) {
            return GuiStyleConstants.CLASS_POLICY_RULES_ICON;
        } else if (QNameUtil.match(SystemConfigurationType.COMPLEX_TYPE, objectType)) {
            return GuiStyleConstants.CLASS_SYSTEM_CONFIGURATION_ICON;
        } else if (QNameUtil.match(ReportType.COMPLEX_TYPE, objectType)) {
            return GuiStyleConstants.CLASS_REPORT_ICON;
        } else if (QNameUtil.match(ObjectCollectionType.COMPLEX_TYPE, objectType)) {
            return GuiStyleConstants.CLASS_OBJECT_COLLECTION_ICON;
        } else if (QNameUtil.match(ArchetypeType.COMPLEX_TYPE, objectType)) {
            return GuiStyleConstants.EVO_ARCHETYPE_TYPE_ICON;
        } else if (QNameUtil.match(ObjectTemplateType.COMPLEX_TYPE, objectType)) {
            return GuiStyleConstants.CLASS_OBJECT_TEMPLATE_ICON;
        } else if (QNameUtil.match(RoleAnalysisClusterType.COMPLEX_TYPE, objectType)) {
            return GuiStyleConstants.CLASS_ROLE_ANALYSIS_CLUSTER_ICON;
        } else if (QNameUtil.match(RoleAnalysisSessionType.COMPLEX_TYPE, objectType)) {
            return GuiStyleConstants.CLASS_ROLE_ANALYSIS_SESSION_ICON;
        } else if (QNameUtil.match(MappingType.COMPLEX_TYPE, objectType)) {
            //TODO fix icon style for mapping type
            return "";
        } else if (QNameUtil.match(RoleAnalysisOutlierType.COMPLEX_TYPE, objectType)) {
            return GuiStyleConstants.CLASS_ICON_OUTLIER;
        } else {
            return "";
        }
    }

    public static <T extends ObjectType> String getBoxCssClasses(QName objectType) {
        if (QNameUtil.match(UserType.COMPLEX_TYPE, objectType)) {
            return GuiStyleConstants.CLASS_OBJECT_USER_BOX_CSS_CLASSES;
        } else if (QNameUtil.match(RoleType.COMPLEX_TYPE, objectType)) {
            return GuiStyleConstants.CLASS_OBJECT_ROLE_BOX_CSS_CLASSES;
        } else if (QNameUtil.match(OrgType.COMPLEX_TYPE, objectType)) {
            return GuiStyleConstants.CLASS_OBJECT_ORG_BOX_CSS_CLASSES;
        } else if (QNameUtil.match(ServiceType.COMPLEX_TYPE, objectType)) {
            return GuiStyleConstants.CLASS_OBJECT_SERVICE_BOX_CSS_CLASSES;
        } else if (QNameUtil.match(TaskType.COMPLEX_TYPE, objectType)) {
            return GuiStyleConstants.CLASS_OBJECT_TASK_BOX_CSS_CLASSES;
        } else if (QNameUtil.match(ResourceType.COMPLEX_TYPE, objectType)) {
            return GuiStyleConstants.CLASS_OBJECT_RESOURCE_BOX_CSS_CLASSES;
        } else {
            return "";
        }
    }

    public static <T extends ObjectType> String getBoxThinCssClasses(QName objectType) {
        if (QNameUtil.match(UserType.COMPLEX_TYPE, objectType)) {
            return GuiStyleConstants.CLASS_OBJECT_USER_BOX_THIN_CSS_CLASSES;
        } else if (QNameUtil.match(RoleType.COMPLEX_TYPE, objectType)) {
            return GuiStyleConstants.CLASS_OBJECT_ROLE_BOX_THIN_CSS_CLASSES;
        } else if (QNameUtil.match(OrgType.COMPLEX_TYPE, objectType)) {
            return GuiStyleConstants.CLASS_OBJECT_ORG_BOX_THIN_CSS_CLASSES;
        } else if (QNameUtil.match(ServiceType.COMPLEX_TYPE, objectType)) {
            return GuiStyleConstants.CLASS_OBJECT_SERVICE_BOX_THIN_CSS_CLASSES;
        } else if (QNameUtil.match(TaskType.COMPLEX_TYPE, objectType)) {
            return GuiStyleConstants.CLASS_OBJECT_TASK_BOX_THIN_CSS_CLASSES;
        } else if (QNameUtil.match(ResourceType.COMPLEX_TYPE, objectType)) {
            return GuiStyleConstants.CLASS_OBJECT_RESOURCE_BOX_THIN_CSS_CLASSES;
        } else {
            return "";
        }
    }

    public static String createUserIcon(PrismObject<UserType> object) {
        UserType user = object.asObjectable();

        // if user has superuser role assigned, or if user has an assigned role whose inducement is superuser then it's superuser
        List<ObjectReferenceType> roleMembershipRef = object.asObjectable().getRoleMembershipRef();

        boolean isEndUser = false;

        for (ObjectReferenceType objectReferenceType : roleMembershipRef) {
            if (objectReferenceType.getOid() == null) {
                continue;
            }

            QName relation = objectReferenceType.getRelation();
            if (!RelationUtil.isDefaultRelation(relation)) {
                continue;
            }

            if (StringUtils.equals(objectReferenceType.getOid(), SystemObjectsType.ROLE_SUPERUSER.value())) {
                return GuiStyleConstants.CLASS_OBJECT_USER_ICON + " "
                        + GuiStyleConstants.CLASS_ICON_STYLE_PRIVILEGED;
            }
            if (StringUtils.equals(objectReferenceType.getOid(), SystemObjectsType.ROLE_END_USER.value())) {
                isEndUser = true;
            }

        }

        boolean isManager = false;
        for (ObjectReferenceType parentOrgRef : user.getParentOrgRef()) {
            if (RelationUtil.isManagerRelation(parentOrgRef.getRelation())) {
                isManager = true;
                break;
            }
        }

        String additionalStyle;
//                getIconEnabledDisabled(object);
//        if (additionalStyle == null) {
        // Set manager and end-user icon only as a last resort. All other
        // colors have priority.
        if (isManager) {
            additionalStyle = GuiStyleConstants.CLASS_ICON_STYLE_MANAGER;
        } else if (isEndUser) {
            additionalStyle = GuiStyleConstants.CLASS_ICON_STYLE_END_USER;
        } else {
            additionalStyle = GuiStyleConstants.CLASS_ICON_STYLE_NORMAL;
        }
//        }
        return GuiStyleConstants.CLASS_OBJECT_USER_ICON + " " + additionalStyle;
    }

    public static String createRoleIcon(PrismObject<RoleType> object) {
        for (AuthorizationType authorization : object.asObjectable().getAuthorization()) {
            if (authorization.getAction().contains(AuthorizationConstants.AUTZ_ALL_URL)) {
                return GuiStyleConstants.CLASS_OBJECT_ROLE_ICON + " "
                        + GuiStyleConstants.CLASS_ICON_STYLE_PRIVILEGED;
            }
        }

        return getObjectNormalIconStyle(GuiStyleConstants.CLASS_OBJECT_ROLE_ICON);
    }

    public static String createApplicationIcon() {
        return getObjectNormalIconStyle(GuiStyleConstants.CLASS_OBJECT_APPLICATION_ICON);
    }

    public static String createOrgIcon() {
        return getObjectNormalIconStyle(GuiStyleConstants.CLASS_OBJECT_ORG_ICON);
    }

    public static String createServiceIcon() {
        return getObjectNormalIconStyle(GuiStyleConstants.CLASS_OBJECT_SERVICE_ICON);
    }

    private static String getObjectNormalIconStyle(String baseIcon) {
        return baseIcon + " " + GuiStyleConstants.CLASS_ICON_STYLE_NORMAL;
    }

    public static <F extends FocusType> String getIconEnabledDisabled(PrismObject<F> object) {
        ActivationType activation = object.asObjectable().getActivation();
        if (activation != null) {
            if (ActivationStatusType.DISABLED.equals(activation.getEffectiveStatus())) {
                return GuiStyleConstants.CLASS_ICON_STYLE_DISABLED;
            } else if (ActivationStatusType.ARCHIVED.equals(activation.getEffectiveStatus())) {
                return GuiStyleConstants.CLASS_ICON_STYLE_ARCHIVED;
            }
        }

        return null;
    }

    public static String createResourceIcon(PrismObject<ResourceType> object) {
        OperationalStateType operationalState = object.asObjectable().getOperationalState();
        AdministrativeOperationalStateType administrativeOperationalState = object.asObjectable().getAdministrativeOperationalState();

        if (administrativeOperationalState != null) {
            AdministrativeAvailabilityStatusType administrativeAvailabilityStatus = administrativeOperationalState.getAdministrativeAvailabilityStatus();
            if (administrativeAvailabilityStatus == AdministrativeAvailabilityStatusType.MAINTENANCE) {
                return GuiStyleConstants.CLASS_OBJECT_RESOURCE_ICON + " "
                        + GuiStyleConstants.CLASS_ICON_STYLE_MAINTENANCE;
            }
        }
        if (operationalState != null) {
            AvailabilityStatusType lastAvailabilityStatus = operationalState.getLastAvailabilityStatus();
            if (lastAvailabilityStatus == AvailabilityStatusType.UP) {
                return GuiStyleConstants.CLASS_OBJECT_RESOURCE_ICON + " "
                        + GuiStyleConstants.CLASS_ICON_STYLE_UP;
            }
            if (lastAvailabilityStatus == AvailabilityStatusType.DOWN) {
                return GuiStyleConstants.CLASS_OBJECT_RESOURCE_ICON + " "
                        + GuiStyleConstants.CLASS_ICON_STYLE_DOWN;
            }

            if (lastAvailabilityStatus == AvailabilityStatusType.BROKEN) {
                return GuiStyleConstants.CLASS_OBJECT_RESOURCE_ICON + " "
                        + GuiStyleConstants.CLASS_ICON_STYLE_BROKEN;
            }
        }
        return GuiStyleConstants.CLASS_OBJECT_RESOURCE_ICON + " " + GuiStyleConstants.CLASS_ICON_STYLE_NORMAL;
    }

    public static String createTaskIcon() {
        return GuiStyleConstants.CLASS_OBJECT_TASK_ICON + " " + GuiStyleConstants.CLASS_ICON_STYLE_NORMAL;
    }

    public static String createShadowIcon(PrismObject<ShadowType> object) {
        ShadowType shadow = object.asObjectable();

        if (ShadowUtil.isProtected(object)) {
            return GuiStyleConstants.CLASS_SHADOW_ICON_PROTECTED;
        }

        return createShadowIcon(shadow.getKind());
    }

    public static String createShadowIcon(@Nullable ShadowKindType kind) {
        if (kind == null) {
            return GuiStyleConstants.CLASS_SHADOW_ICON_UNKNOWN;
        }

        switch (kind) {
            case ACCOUNT:
                return GuiStyleConstants.CLASS_SHADOW_ICON_ACCOUNT;
            case GENERIC:
                return GuiStyleConstants.CLASS_SHADOW_ICON_GENERIC;
            case ENTITLEMENT:
                return GuiStyleConstants.CLASS_SHADOW_ICON_ENTITLEMENT;

        }

        return GuiStyleConstants.CLASS_SHADOW_ICON_UNKNOWN;
    }

    public static String createObjectColletionIcon() {
        return getObjectNormalIconStyle(GuiStyleConstants.CLASS_OBJECT_COLLECTION_ICON);
    }

    private static String createObjectTemplateIcon() {
        return getObjectNormalIconStyle(GuiStyleConstants.CLASS_OBJECT_TEMPLATE_ICON);
    }

    private static String createMarkIcon() {
        return getObjectNormalIconStyle(GuiStyleConstants.CLASS_MARK);
    }

    private static String createRoleAnalysisSessionIcon() {
        return getObjectNormalIconStyle(GuiStyleConstants.CLASS_ROLE_ANALYSIS_SESSION_ICON);
    }

    private static String createRoleAnalysisClusterIcon() {
        return getObjectNormalIconStyle(GuiStyleConstants.CLASS_ROLE_ANALYSIS_CLUSTER_ICON);
    }

    private static String createRoleAnalysisOutlierIcon() {
        return getObjectNormalIconStyle(GuiStyleConstants.CLASS_ICON_OUTLIER);
    }

    private static String createSimulationResultIcon() {
        return getObjectNormalIconStyle(GuiStyleConstants.CLASS_SIMULATION_RESULT);
    }

    public static String createReportIcon() {
        return getObjectNormalIconStyle(GuiStyleConstants.CLASS_REPORT_ICON);
    }

    public static String createErrorIcon(OperationResult result) {
        if (result == null) {
            return "";
        }
        OperationResultStatus status = result.getStatus();
        OperationResultStatusPresentationProperties icon = OperationResultStatusPresentationProperties
                .parseOperationalResultStatus(status);
        return icon.getIcon() + " fa-lg";
    }

    public static <O extends ObjectType> IconType getIconForLifecycleState(O obj) {
        IconType icon = new IconType();
        if (obj == null) {
            return null;
        }
        String lifecycle = obj.getLifecycleState();
        if (lifecycle == null) {
            return null;
        }
        switch (lifecycle) {
            case SchemaConstants.LIFECYCLE_ARCHIVED:
                icon.setCssClass(GuiStyleConstants.CLASS_FILE_EXCEL);
                break;
            case SchemaConstants.LIFECYCLE_DRAFT:
                icon.setCssClass(GuiStyleConstants.CLASS_FILE_BLACK_FILLED);
                break;
            case SchemaConstants.LIFECYCLE_PROPOSED:
                icon.setCssClass(GuiStyleConstants.CLASS_FILE_WHITE_FILLED);
                break;
        }

        if (icon.getCssClass() == null) {
            icon.setCssClass("");
        }
        icon.setColor("blue");
        return icon;
    }

    public static <O extends ObjectType> IconType getIconForActivationStatus(O obj) {
        if (obj == null || !(obj instanceof FocusType)) {
            return null;
        }

        FocusType focus = (FocusType) obj;
        ActivationType activation = focus.getActivation();
        if (activation == null) {
            return null;
        }

        ActivationStatusType status = activation.getEffectiveStatus();
        IconType icon = new IconType();
        if (LockoutStatusType.LOCKED == activation.getLockoutStatus()) {
            icon.setCssClass(GuiStyleConstants.CLASS_LOCK_STATUS);
        } else if (ActivationStatusType.ENABLED == status) {
            return null;
        } else if (ActivationStatusType.DISABLED == status) {
            icon.setCssClass(GuiStyleConstants.CLASS_BAN);
        } else if (ActivationStatusType.ARCHIVED == status) {
            icon.setCssClass(GuiStyleConstants.CLASS_ICON_NO_OBJECTS);
        } else {
            icon.setCssClass(GuiStyleConstants.CLASS_TEST_CONNECTION_MENU_ITEM);
        }
        if (icon.getCssClass() == null) {
            icon.setCssClass("");
        }
        icon.setColor("red");
        return icon;
    }

    public static IconType createIconType(String iconStyle) {
        return createIconType(iconStyle, "");
    }

    public static IconType createIconType(String iconStyle, String color) {
        IconType icon = new IconType();
        icon.setCssClass(iconStyle);
        icon.setColor(color);
        return icon;
    }

    public static String createMappingIcon(PrismContainerValueWrapper<? extends Containerable> object) {
        if (object == null) {
            return "fa fa-circle";
        }

        PrismContainerDefinition<? extends Containerable> def = object.getDefinition();
        if (def == null) {
            return "fa fa-circle";
        }

        if (object.getParentContainerValue(ResourceActivationDefinitionType.class) != null
                || object.getParentContainerValue(ResourcePasswordDefinitionType.class) != null) {
            if (QNameUtil.match(def.getTypeName(), MappingType.COMPLEX_TYPE)) {

                PrismContainerValueWrapper parent =
                        object.getParentContainerValue(ResourceBidirectionalMappingType.class);
                if (parent == null) {
                    parent = object.getParentContainerValue(ResourcePasswordDefinitionType.class);
                }
                if (parent == null) {
                    return "fa fa-circle";
                }

                PrismContainerDefinition parentDef = parent.getDefinition();
                return createMappingIcon(parentDef);

            } else {
                return createMappingIcon(def);
            }
        }
        return "fa fa-circle";
    }

    public static String createMappingIcon(PrismContainerDefinition<? extends Containerable> def) {
        if (def == null) {
            return "fa fa-circle";
        }

        if (QNameUtil.match(def.getItemName(), ResourceActivationDefinitionType.F_ADMINISTRATIVE_STATUS)) {
            return "fa fa-id-card-clip";
        } else if (QNameUtil.match(def.getItemName(), ResourceActivationDefinitionType.F_EXISTENCE)) {
            return "fa fa-universal-access";
        } else if (QNameUtil.match(def.getItemName(), ResourceActivationDefinitionType.F_VALID_FROM)) {
            return "fa fa-arrow-right-from-bracket";
        } else if (QNameUtil.match(def.getItemName(), ResourceActivationDefinitionType.F_VALID_TO)) {
            return "fa fa-arrow-right-to-bracket";
        } else if (QNameUtil.match(def.getItemName(), ResourceActivationDefinitionType.F_LOCKOUT_STATUS)) {
            return "fa fa-user-lock";
        } else if (QNameUtil.match(def.getItemName(), ResourceActivationDefinitionType.F_DISABLE_INSTEAD_OF_DELETE)) {
            return "fa fa-user-slash";
        } else if (QNameUtil.match(def.getItemName(), ResourceActivationDefinitionType.F_DELAYED_DELETE)) {
            return "fa fa-clock";
        } else if (QNameUtil.match(def.getItemName(), ResourceActivationDefinitionType.F_PRE_PROVISION)) {
            return "fa fa-user-plus";
        } else if (QNameUtil.match(def.getItemName(), ResourceCredentialsDefinitionType.F_PASSWORD)) {
            return "fa fa-key";
        }
        return "fa fa-circle";
    }
}
