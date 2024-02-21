package com.evolveum.midpoint.model.impl.mining.analysis;

import com.evolveum.midpoint.common.mining.objects.analysis.AttributeAnalysisStructure;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.impl.PrismPropertyValueImpl;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import java.util.*;

// TODO - this class is just fast experiment

/**
 * Utility class for attribute analysis.
 * Used for calculating the density and similarity of the attributes.
 * Used for role analysis cluster similarity chart.
 */
public class AttributeAnalysisUtil {
    private static String extractRealValue(Object object) {
        if (object != null) {
            if (object instanceof PolyString) {
                return ((PolyString) object).getOrig();
            } else if (object instanceof PrismPropertyValueImpl) {
                Object realValue = ((PrismPropertyValueImpl<?>) object).getRealValue();
                if (realValue != null) {
                    return realValue.toString();
                }
            } else {
                return object.toString();
            }
        }
        return null;
    }

    public static Set<PrismObject<UserType>> fetchPrismUsers(@NotNull RoleAnalysisService roleAnalysisService,
            @NotNull Set<String> objectOid,
            @NotNull Task task,
            @NotNull OperationResult result) {
        Set<PrismObject<UserType>> prismUsers = new HashSet<>();

        objectOid.forEach(userOid -> {
            PrismObject<UserType> userTypeObject = roleAnalysisService.getUserTypeObject(userOid, task, result);
            if (userTypeObject != null) {
                prismUsers.add(userTypeObject);
            }
        });

        return prismUsers;
    }

    public static Set<PrismObject<RoleType>> fetchPrismRoles(@NotNull RoleAnalysisService roleAnalysisService,
            @NotNull Set<String> objectOid,
            @NotNull Task task,
            @NotNull OperationResult result) {
        Set<PrismObject<RoleType>> prismRolesSet = new HashSet<>();

        objectOid.forEach(roleOid -> {
            PrismObject<RoleType> rolePrismObject = roleAnalysisService.getRoleTypeObject(roleOid, task, result);
            if (rolePrismObject != null) {
                prismRolesSet.add(rolePrismObject);
            }
        });

        return prismRolesSet;
    }

    public static void processUserItemPaths(Set<PrismObject<UserType>> prismUsers,
            @NotNull List<ItemPath> itemPathSet,
            List<AttributeAnalysisStructure> attributeAnalysisStructures,
            int usersCount) {

        for (ItemPath itemPath : itemPathSet) {
//            Set<String> uniqueValues = new HashSet<>();
            Map<String, Integer> frequencyMap = new HashMap<>();

            int totalRelations = 0;

            for (PrismObject<UserType> userTypeObject : prismUsers) {
                Item<PrismValue, ItemDefinition<?>> item = userTypeObject.findItem(itemPath);

                if (item != null) {
                    Object object = item.getRealValue();
                    String comparedValue = extractRealValue(object);
                    if (comparedValue != null) {
//                        uniqueValues.add(comparedValue);
                        frequencyMap.put(comparedValue, frequencyMap.getOrDefault(comparedValue, 0) + 1);
                        totalRelations++;
                    }
                }
            }

            AttributeAnalysisStructure attributeAnalysisStructure = new AttributeAnalysisStructure(
                    frequencyMap.size(), usersCount, totalRelations, itemPath.toString());
            attributeAnalysisStructures.add(attributeAnalysisStructure);
        }
    }

    public static void processRoleItemPaths(Set<PrismObject<RoleType>> prismRolesSet,
            @NotNull List<ItemPath> itemPathSet,
            List<AttributeAnalysisStructure> attributeAnalysisStructures,
            int rolesCount) {
        for (ItemPath itemPath : itemPathSet) {
            Set<String> uniqueValues = new HashSet<>();
            int totalRelations = 0;

            for (PrismObject<RoleType> rolePrismObject : prismRolesSet) {
                Item<PrismValue, ItemDefinition<?>> item = rolePrismObject.findItem(itemPath);

                if (item != null) {
                    Object object = item.getRealValue();
                    String comparedValue = extractRealValue(object);
                    if (comparedValue != null) {
                        uniqueValues.add(comparedValue);
                        totalRelations++;
                    }
                }
            }

            AttributeAnalysisStructure attributeAnalysisStructure = new AttributeAnalysisStructure(
                    uniqueValues.size(), rolesCount, totalRelations, itemPath.toString());
            attributeAnalysisStructures.add(attributeAnalysisStructure);
        }
    }

    public static void processUserAssignments(@NotNull Set<PrismObject<UserType>> prismUsers,
            Double membershipDensity,
            List<AttributeAnalysisStructure> attributeAnalysisStructures,
            int usersCount) {
        Set<String> resourceOid = new HashSet<>();
        int totalRelationsResource = 0;

        Set<String> roleOid = new HashSet<>();
        int totalRelationsRole = 0;

        Set<String> orgOid = new HashSet<>();
        int totalRelationsOrg = 0;

        Set<String> serviceOid = new HashSet<>();
        int totalRelationsService = 0;

        Set<String> roleMembershipRefOid = new HashSet<>();
        int totalRelationsRoleMembershipRefOid = 0;

        Set<String> linkRefOid = new HashSet<>();
        int totalRelationsLinkRefOid = 0;

        for (PrismObject<UserType> userTypeObject : prismUsers) {
            UserType userObject = userTypeObject.asObjectable();
            List<AssignmentType> assignment = userObject.getAssignment();
            List<ObjectReferenceType> roleMembershipRef = userObject.getRoleMembershipRef();

            for (ObjectReferenceType objectReferenceType : roleMembershipRef) {
                roleMembershipRefOid.add(objectReferenceType.getOid());
                totalRelationsRoleMembershipRefOid++;
            }

            List<ObjectReferenceType> linkRef = userObject.getLinkRef();
            for (ObjectReferenceType objectReferenceType : linkRef) {
                linkRefOid.add(objectReferenceType.getOid());
                totalRelationsLinkRefOid++;
            }

            for (AssignmentType assignmentType : assignment) {
                ObjectReferenceType targetRef = assignmentType.getTargetRef();

                if (targetRef == null) {
                    continue;
                }

                String oid = targetRef.getOid();
                String type = targetRef.getType().getLocalPart();

                if (ResourceType.COMPLEX_TYPE.getLocalPart().equals(type)) {
                    resourceOid.add(oid);
                    totalRelationsResource++;
                } else if (RoleType.COMPLEX_TYPE.getLocalPart().equals(type)) {
                    roleOid.add(oid);
                    totalRelationsRole++;
                } else if (OrgType.COMPLEX_TYPE.getLocalPart().equals(type)) {
                    orgOid.add(oid);
                    totalRelationsOrg++;
                } else if (ServiceType.COMPLEX_TYPE.getLocalPart().equals(type)) {
                    serviceOid.add(oid);
                    totalRelationsService++;
                }
            }
        }

        attributeAnalysisStructures.add(new AttributeAnalysisStructure(
                linkRefOid.size(), usersCount, totalRelationsLinkRefOid, "linkRef"));

        attributeAnalysisStructures.add(new AttributeAnalysisStructure(
                roleMembershipRefOid.size(), usersCount, totalRelationsRoleMembershipRefOid, "roleMembershipRef"));

        attributeAnalysisStructures.add(new AttributeAnalysisStructure(
                resourceOid.size(), usersCount, totalRelationsResource, "assignment/resource"));

        attributeAnalysisStructures.add(new AttributeAnalysisStructure(
                orgOid.size(), usersCount, totalRelationsOrg, "assignment/org"));

        attributeAnalysisStructures.add(new AttributeAnalysisStructure(
                serviceOid.size(), usersCount, totalRelationsService, "assignment/service"));

        if (membershipDensity != null) {
            attributeAnalysisStructures.add(new AttributeAnalysisStructure(membershipDensity, "assignment/role"));
        } else {
            attributeAnalysisStructures.add(new AttributeAnalysisStructure(
                    roleOid.size(), usersCount, totalRelationsRole, "assignment/role"));
        }
    }

    public static void processRoleAssignmentsAndInducements(@NotNull Set<PrismObject<RoleType>> prismRolesSet,
            Double membershipDensity,
            List<AttributeAnalysisStructure> attributeAnalysisStructures,
            int rolesCount) {
        Set<String> resourceOidAssignments = new HashSet<>();
        int totalRelationsResourceAssignments = 0;

        Set<String> roleOidAssignments = new HashSet<>();
        int totalRelationsRoleAssignments = 0;

        Set<String> orgOidAssignments = new HashSet<>();
        int totalRelationsOrgAssignments = 0;

        Set<String> serviceOidAssignments = new HashSet<>();
        int totalRelationsServiceAssignments = 0;

        Set<String> resourceOidInducements = new HashSet<>();
        int totalRelationsResourceInducements = 0;

        Set<String> roleOidInducements = new HashSet<>();
        int totalRelationsRoleInducements = 0;

        Set<String> orgOidInducements = new HashSet<>();
        int totalRelationsOrgInducements = 0;

        Set<String> serviceOidInducements = new HashSet<>();
        int totalRelationsServiceInducements = 0;

        Set<String> roleMembershipRefOid = new HashSet<>();
        int totalRelationsRoleMembershipRefOid = 0;

        Set<String> linkRefOid = new HashSet<>();
        int totalRelationsLinkRefOid = 0;

        Set<String> archetypeRefOid = new HashSet<>();
        int totalRelationsArchetypeRefOid = 0;

        for (PrismObject<RoleType> rolePrismObject : prismRolesSet) {
            RoleType roleObject = rolePrismObject.asObjectable();

            List<ObjectReferenceType> archetypeRef = roleObject.getArchetypeRef();

            for (ObjectReferenceType objectReferenceType : archetypeRef) {
                archetypeRefOid.add(objectReferenceType.getOid());
                totalRelationsArchetypeRefOid++;
            }
            List<ObjectReferenceType> roleMembershipRef = roleObject.getRoleMembershipRef();
            for (ObjectReferenceType objectReferenceType : roleMembershipRef) {
                roleMembershipRefOid.add(objectReferenceType.getOid());
                totalRelationsRoleMembershipRefOid++;
            }

            List<ObjectReferenceType> linkRef = roleObject.getLinkRef();
            for (ObjectReferenceType objectReferenceType : linkRef) {
                linkRefOid.add(objectReferenceType.getOid());
                totalRelationsLinkRefOid++;
            }

            processAssignmentAndInducement(roleObject.getAssignment(),
                    resourceOidAssignments,
                    roleOidAssignments,
                    orgOidAssignments,
                    serviceOidAssignments,
                    totalRelationsResourceAssignments,
                    totalRelationsRoleAssignments,
                    totalRelationsOrgAssignments,
                    totalRelationsServiceAssignments);

            processAssignmentAndInducement(roleObject.getInducement(),
                    resourceOidInducements,
                    roleOidInducements,
                    orgOidInducements,
                    serviceOidInducements,
                    totalRelationsResourceInducements,
                    totalRelationsRoleInducements,
                    totalRelationsOrgInducements,
                    totalRelationsServiceInducements);
        }

        attributeAnalysisStructures.add(new AttributeAnalysisStructure(
                archetypeRefOid.size(), rolesCount, totalRelationsArchetypeRefOid, "archetypeRef"));

        attributeAnalysisStructures.add(new AttributeAnalysisStructure(
                roleMembershipRefOid.size(), rolesCount, totalRelationsRoleMembershipRefOid, "roleMembershipRef"));

        attributeAnalysisStructures.add(new AttributeAnalysisStructure(
                linkRefOid.size(), rolesCount, totalRelationsLinkRefOid, "linkRef"));

        addToAnalysisStructure(attributeAnalysisStructures,
                rolesCount,
                resourceOidAssignments.size(),
                totalRelationsResourceAssignments,
                roleOidAssignments.size(),
                totalRelationsRoleAssignments,
                orgOidAssignments.size(),
                totalRelationsOrgAssignments,
                serviceOidAssignments.size(),
                totalRelationsServiceAssignments,
                "assignment");

        addToAnalysisStructure(attributeAnalysisStructures,
                rolesCount,
                resourceOidInducements.size(),
                totalRelationsResourceInducements,
                roleOidInducements.size(),
                totalRelationsRoleInducements,
                orgOidInducements.size(),
                totalRelationsOrgInducements,
                serviceOidInducements.size(),
                totalRelationsServiceInducements,
                "inducement");

        if (membershipDensity != null) {
            attributeAnalysisStructures.add(new AttributeAnalysisStructure(membershipDensity, "member/user"));
        }
    }

    private static void processAssignmentAndInducement(@NotNull List<AssignmentType> assignments,
            Set<String> resourceOid,
            Set<String> roleOid,
            Set<String> orgOid,
            Set<String> serviceOid,
            int totalRelationsResource,
            int totalRelationsRole,
            int totalRelationsOrg,
            int totalRelationsService) {
        for (AssignmentType assignmentType : assignments) {
            ObjectReferenceType targetRef = assignmentType.getTargetRef();
            if (targetRef == null) {
                continue;
            }

            String oid = targetRef.getOid();
            String type = targetRef.getType().getLocalPart();

            if (ResourceType.COMPLEX_TYPE.getLocalPart().equals(type)) {
                resourceOid.add(oid);
                totalRelationsResource++;
            } else if (RoleType.COMPLEX_TYPE.getLocalPart().equals(type)) {
                roleOid.add(oid);
                totalRelationsRole++;
            } else if (OrgType.COMPLEX_TYPE.getLocalPart().equals(type)) {
                orgOid.add(oid);
                totalRelationsOrg++;
            } else if (ServiceType.COMPLEX_TYPE.getLocalPart().equals(type)) {
                serviceOid.add(oid);
                totalRelationsService++;
            }
        }
    }

    private static void addToAnalysisStructure(@NotNull List<AttributeAnalysisStructure> attributeAnalysisStructures,
            int rolesCount,
            int resourceOidSize, int totalRelationsResource,
            int roleOidSize, int totalRelationsRole,
            int orgOidSize, int totalRelationsOrg,
            int serviceOidSize, int totalRelationsService,
            String assignmentType) {
        attributeAnalysisStructures.add(new AttributeAnalysisStructure(
                resourceOidSize, rolesCount, totalRelationsResource, assignmentType + "/resource"));
        attributeAnalysisStructures.add(new AttributeAnalysisStructure(
                roleOidSize, rolesCount, totalRelationsRole, assignmentType + "/role"));
        attributeAnalysisStructures.add(new AttributeAnalysisStructure(
                orgOidSize, rolesCount, totalRelationsOrg, assignmentType + "/org"));
        attributeAnalysisStructures.add(new AttributeAnalysisStructure(
                serviceOidSize, rolesCount, totalRelationsService, assignmentType + "/service"));
    }

}
