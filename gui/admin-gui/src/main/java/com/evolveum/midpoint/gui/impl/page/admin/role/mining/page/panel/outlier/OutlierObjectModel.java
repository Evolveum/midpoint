package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier;

import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.XMLGregorianCalendar;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.getRolesOidAssignment;

public class OutlierObjectModel {

    String outlierName;
    String outlierDescription;
    double outlierConfidence;
    List<OutlierItemModel> outlierItemModels;

    String timeCreated;

    public OutlierObjectModel(
            @NotNull String outlierName,
            @NotNull String outlierDescription,
            double outlierConfidence,
            String timeCreated) {
        this.outlierName = outlierName;
        this.outlierDescription = outlierDescription;
        this.outlierConfidence = outlierConfidence;
        this.timeCreated = timeCreated;
        this.outlierItemModels = new ArrayList<>();
    }

    public void addOutlierItemModel(OutlierItemModel outlierItemModel) {
        this.outlierItemModels.add(outlierItemModel);
    }

    public OutlierObjectModel(
            @NotNull String outlierName,
            @NotNull String outlierDescription,
            double outlierConfidence,
            @NotNull List<OutlierItemModel> outlierItemModels) {
        this.outlierName = outlierName;
        this.outlierDescription = outlierDescription;
        this.outlierConfidence = outlierConfidence;
        this.outlierItemModels = outlierItemModels;
    }

    public String getOutlierName() {
        return outlierName;
    }

    public String getOutlierDescription() {
        return outlierDescription;
    }

    public double getOutlierConfidence() {
        return outlierConfidence;
    }

    public List<OutlierItemModel> getOutlierItemModels() {
        return outlierItemModels;
    }

    public String getTimeCreated() {
        return timeCreated;
    }

    public static @Nullable OutlierObjectModel generateUserOutlierResultModel(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisOutlierType outlierResult,
            @NotNull Task task,
            @NotNull OperationResult result,
            @NotNull RoleAnalysisClusterType cluster) {
        DecimalFormat decimalFormat = new DecimalFormat("#.##");
        decimalFormat.setGroupingUsed(false);
        decimalFormat.setRoundingMode(RoundingMode.DOWN);

        ObjectReferenceType targetObjectRef = outlierResult.getTargetObjectRef();
        PrismObject<UserType> userTypeObject = roleAnalysisService.getUserTypeObject(targetObjectRef.getOid(), task, result);
        XMLGregorianCalendar createTimestamp = outlierResult.getMetadata().getCreateTimestamp();
        if (userTypeObject == null) {
            return null;
        }

        PolyString name = userTypeObject.getName();
        List<RoleAnalysisOutlierDescriptionType> propertyOutlier = outlierResult.getResult();

        double min = 100;
        double max = 0;
        double confidenceSum = 0;
        for (RoleAnalysisOutlierDescriptionType property : propertyOutlier) {
            Double confidence = property.getConfidence();
            if (confidence != null) {
                min = Math.min(min, confidence);
                max = Math.max(max, confidence);
                confidenceSum += confidence;
            }
        }

        min = min * 100;
        max = max * 100;

        int propertyCount = propertyOutlier.size();
        double outlierConfidence = confidenceSum / propertyCount;
        int outlierConfidenceInt = (int) (outlierConfidence * 100);
        String outlierDescription = "User has been marked as outlier object due to confidence score:";
        String propertyConfidenceRange = decimalFormat.format(min) + " - " + decimalFormat.format(max) + "%";
        String propertyDescription = "There is detected " + propertyCount + " outlier assignment(s) with high confidence";

        OutlierObjectModel outlierObjectModel = new OutlierObjectModel(name.getOrig(), outlierDescription, outlierConfidenceInt, createTimestamp.toString());

        AnalysisClusterStatisticType clusterStatistics = cluster.getClusterStatistics();
        int similarObjectCount = cluster.getMember().size();
        Double membershipDensity = clusterStatistics.getMembershipDensity();
        String clusterDescription = "Detected " + similarObjectCount + " similar objects with membership density " + String.format("%.2f", membershipDensity) + "%";
        OutlierItemModel clusterItemModel = new OutlierItemModel(similarObjectCount + " similar object(s)", clusterDescription, "fa fa-cubes");
        outlierObjectModel.addOutlierItemModel(clusterItemModel);

        OutlierItemModel outlierItemModel = new OutlierItemModel(propertyConfidenceRange, propertyDescription, "fe fe-role");
        outlierObjectModel.addOutlierItemModel(outlierItemModel);

        RoleAnalysisAttributeAnalysisResult userAttributeAnalysisResult = cluster.getClusterStatistics().getUserAttributeAnalysisResult();

        RoleAnalysisAttributeAnalysisResult userAttributes = roleAnalysisService.resolveUserAttributes(userTypeObject);

        RoleAnalysisAttributeAnalysisResult compareAttributeResult = roleAnalysisService.resolveSimilarAspect(userAttributes, userAttributeAnalysisResult);

        double averageItemsOccurs = 0;
        assert compareAttributeResult != null;
        List<RoleAnalysisAttributeAnalysis> attributeAnalysis = compareAttributeResult.getAttributeAnalysis();

        int attributeAboveThreshold = 0;
        int threshold = 80;
        StringBuilder attributeDescriptionThreshold = new StringBuilder();
        attributeDescriptionThreshold.append("Attributes with occurrence above ").append(threshold).append("%: ");

        double roleAssignmentsOccurs = 0;
        String roleAssignmentsDescription = "Outlier object coverage in role assignments compared to similar objects.";
        for (RoleAnalysisAttributeAnalysis attribute : attributeAnalysis) {
            Double density = attribute.getDensity();
            if (density != null) {
                if (attribute.getItemPath().equals("role assignment")) {
                    roleAssignmentsOccurs = density;
                }
                if (density > 0.8) {
                    attributeAboveThreshold++;
                    attributeDescriptionThreshold.append(attribute.getItemPath()).append(", ");
                }
                averageItemsOccurs += density;
            }
        }

        averageItemsOccurs = averageItemsOccurs / attributeAnalysis.size();

        OutlierItemModel roleAssignmentsItemModel = new OutlierItemModel(String.format("%.2f", roleAssignmentsOccurs) + "%", roleAssignmentsDescription, "fe fe-role");
        outlierObjectModel.addOutlierItemModel(roleAssignmentsItemModel);

        String attributeDescription = "Items factor outlier vs cluster. (average overlap value of outlier attributes with similar objects.)";

        OutlierItemModel attributeItemModel = new OutlierItemModel(String.format("%.2f", averageItemsOccurs) + "%", attributeDescription, "fa fa-cogs");
        outlierObjectModel.addOutlierItemModel(attributeItemModel);

        OutlierItemModel attributeItemModelThreshold = new OutlierItemModel(String.valueOf(attributeAboveThreshold) + " attribute(s)", attributeDescriptionThreshold.toString(), "fa fa-cogs");
        outlierObjectModel.addOutlierItemModel(attributeItemModelThreshold);

        List<String> rolesOid = getRolesOidAssignment(userTypeObject.asObjectable());

        int directRoles = 0;
        String directRolesDescription = "Direct role assignments of the outlier object.";
        int indirectRoles = 0;
        String indirectRolesDescription = "Indirect role assignments of the outlier object.";
        for (String roleOid : rolesOid) {
            PrismObject<RoleType> roleAssignment = roleAnalysisService.getRoleTypeObject(roleOid, task, result);
            if (roleAssignment != null) {
                RoleType role = roleAssignment.asObjectable();
                List<AssignmentType> inducement = role.getInducement();
                if (inducement != null && !inducement.isEmpty()) {
                    indirectRoles += inducement.size();
                } else {
                    directRoles++;
                }
            }
        }

        OutlierItemModel directRolesItemModel = new OutlierItemModel(String.valueOf(directRoles), directRolesDescription, "fe fe-role");
        outlierObjectModel.addOutlierItemModel(directRolesItemModel);

        OutlierItemModel indirectRolesItemModel = new OutlierItemModel(String.valueOf(indirectRoles), indirectRolesDescription, "fe fe-role");
        outlierObjectModel.addOutlierItemModel(indirectRolesItemModel);

        int rolesByCondition = 0;
        String rolesByConditionDescription = "Role assignments of the outlier object by condition. (?)";

        OutlierItemModel rolesByConditionItemModel = new OutlierItemModel(String.valueOf(rolesByCondition), rolesByConditionDescription, "fe fe-role");
        outlierObjectModel.addOutlierItemModel(rolesByConditionItemModel);

        int outdatedAccessRights = 0;
        String outdatedAccessRightsDescription = "Outdated access rights of the outlier object.";

        OutlierItemModel outdatedAccessRightsItemModel = new OutlierItemModel(String.valueOf(outdatedAccessRights), outdatedAccessRightsDescription, "fa fa-key");
        outlierObjectModel.addOutlierItemModel(outdatedAccessRightsItemModel);

        return outlierObjectModel;
    }

    public static @Nullable OutlierObjectModel generateRoleOutlierResultModel(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisOutlierType outlierResult,
            @NotNull Task task,
            @NotNull OperationResult result,
            @NotNull RoleAnalysisClusterType cluster) {
        DecimalFormat decimalFormat = new DecimalFormat("#.##");
        decimalFormat.setGroupingUsed(false);
        decimalFormat.setRoundingMode(RoundingMode.DOWN);

        ObjectReferenceType targetObjectRef = outlierResult.getTargetObjectRef();
        PrismObject<RoleType> roleTypePrismObject = roleAnalysisService.getRoleTypeObject(targetObjectRef.getOid(), task, result);
        XMLGregorianCalendar createTimestamp = outlierResult.getMetadata().getCreateTimestamp();
        if (roleTypePrismObject == null) {
            return null;
        }

        PolyString name = roleTypePrismObject.getName();
        List<RoleAnalysisOutlierDescriptionType> propertyOutlier = outlierResult.getResult();

        double min = 100;
        double max = 0;
        double confidenceSum = 0;
        for (RoleAnalysisOutlierDescriptionType property : propertyOutlier) {
            Double confidence = property.getConfidence();
            if (confidence != null) {
                min = Math.min(min, confidence);
                max = Math.max(max, confidence);
                confidenceSum += confidence;
            }
        }

        min = min * 100;
        max = max * 100;

        int propertyCount = propertyOutlier.size();
        double outlierConfidence = confidenceSum / propertyCount;
        int outlierConfidenceInt = (int) (outlierConfidence * 100);
        String outlierDescription = "User has been marked as outlier object due to confidence score:";
        String propertyConfidenceRange = decimalFormat.format(min) + " - " + decimalFormat.format(max) + "%";
        String propertyDescription = "There is detected " + propertyCount + " outlier assignment(s) with high confidence";

        OutlierObjectModel outlierObjectModel = new OutlierObjectModel(
                name.getOrig(), outlierDescription, outlierConfidenceInt, createTimestamp.toString());

        AnalysisClusterStatisticType clusterStatistics = cluster.getClusterStatistics();
        int similarObjectCount = cluster.getMember().size();
        Double membershipDensity = clusterStatistics.getMembershipDensity();
        String clusterDescription = "Detected " + similarObjectCount + " similar objects with membership density "
                + String.format("%.2f", membershipDensity) + "%";
        OutlierItemModel clusterItemModel = new OutlierItemModel(similarObjectCount
                + " similar object(s)", clusterDescription, "fa fa-cubes");
        outlierObjectModel.addOutlierItemModel(clusterItemModel);

        OutlierItemModel outlierItemModel = new OutlierItemModel(propertyConfidenceRange, propertyDescription, "fe fe-role");
        outlierObjectModel.addOutlierItemModel(outlierItemModel);

        RoleAnalysisAttributeAnalysisResult userAttributeAnalysisResult = cluster.getClusterStatistics()
                .getUserAttributeAnalysisResult();

        RoleAnalysisAttributeAnalysisResult userAttributes = roleAnalysisService
                .resolveRoleMembersAttribute(roleTypePrismObject.getOid(), task, result);

        RoleAnalysisAttributeAnalysisResult compareAttributeResult = roleAnalysisService
                .resolveSimilarAspect(userAttributes, userAttributeAnalysisResult);

        double averageItemsOccurs = 0;
        assert compareAttributeResult != null;
        List<RoleAnalysisAttributeAnalysis> attributeAnalysis = compareAttributeResult.getAttributeAnalysis();

        int attributeAboveThreshold = 0;
        int threshold = 80;
        StringBuilder attributeDescriptionThreshold = new StringBuilder();
        attributeDescriptionThreshold.append("Attributes with occurrence above ").append(threshold).append("%: ");

        double roleAssignmentsOccurs = 0;
        String roleAssignmentsDescription = "Outlier object coverage in role assignments compared to similar objects.";
        for (RoleAnalysisAttributeAnalysis attribute : attributeAnalysis) {
            Double density = attribute.getDensity();
            if (density != null) {
                if (attribute.getItemPath().equals("role assignment")) {
                    roleAssignmentsOccurs = density;
                }
                if (density > 0.8) {
                    attributeAboveThreshold++;
                    attributeDescriptionThreshold.append(attribute.getItemPath()).append(", ");
                }
                averageItemsOccurs += density;
            }
        }

        averageItemsOccurs = averageItemsOccurs / attributeAnalysis.size();

        OutlierItemModel roleAssignmentsItemModel = new OutlierItemModel(String.format("%.2f", roleAssignmentsOccurs)
                + "%", roleAssignmentsDescription, "fe fe-role");
        outlierObjectModel.addOutlierItemModel(roleAssignmentsItemModel);

        String attributeDescription = "Items factor outlier vs cluster. "
                + "(average overlap value of outlier attributes with similar objects.)";

        OutlierItemModel attributeItemModel = new OutlierItemModel(String.format("%.2f", averageItemsOccurs)
                + "%", attributeDescription, "fa fa-cogs");
        outlierObjectModel.addOutlierItemModel(attributeItemModel);

        OutlierItemModel attributeItemModelThreshold = new OutlierItemModel(attributeAboveThreshold
                + " attribute(s)", attributeDescriptionThreshold.toString(), "fa fa-cogs");
        outlierObjectModel.addOutlierItemModel(attributeItemModelThreshold);

        List<String> rolesOid = Collections.singletonList(roleTypePrismObject.getOid());

        int directRoles = 0;
        String directRolesDescription = "Direct role assignments of the outlier object.";
        int indirectRoles = 0;
        String indirectRolesDescription = "Indirect role assignments of the outlier object.";
        for (String roleOid : rolesOid) {
            PrismObject<RoleType> roleAssignment = roleAnalysisService.getRoleTypeObject(roleOid, task, result);
            if (roleAssignment != null) {
                RoleType role = roleAssignment.asObjectable();
                List<AssignmentType> inducement = role.getInducement();
                if (inducement != null && !inducement.isEmpty()) {
                    indirectRoles += inducement.size();
                } else {
                    directRoles++;
                }
            }
        }

        OutlierItemModel directRolesItemModel = new OutlierItemModel(String.valueOf(directRoles),
                directRolesDescription, "fe fe-role");
        outlierObjectModel.addOutlierItemModel(directRolesItemModel);

        OutlierItemModel indirectRolesItemModel = new OutlierItemModel(String.valueOf(indirectRoles),
                indirectRolesDescription, "fe fe-role");
        outlierObjectModel.addOutlierItemModel(indirectRolesItemModel);

        int rolesByCondition = 0;
        String rolesByConditionDescription = "Role assignments of the outlier object by condition. (?)";

        OutlierItemModel rolesByConditionItemModel = new OutlierItemModel(String.valueOf(rolesByCondition),
                rolesByConditionDescription, "fe fe-role");
        outlierObjectModel.addOutlierItemModel(rolesByConditionItemModel);

        int outdatedAccessRights = 0;
        String outdatedAccessRightsDescription = "Outdated access rights of the outlier object.";

        OutlierItemModel outdatedAccessRightsItemModel = new OutlierItemModel(String.valueOf(outdatedAccessRights),
                outdatedAccessRightsDescription, "fa fa-key");
        outlierObjectModel.addOutlierItemModel(outdatedAccessRightsItemModel);

        return outlierObjectModel;
    }

    public static @Nullable OutlierObjectModel generateAssignmentOutlierResultModel(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisOutlierDescriptionType outlierResult,
            @NotNull Task task,
            @NotNull OperationResult result,
            @NotNull PrismObject<UserType> userTypeObject) {
        DecimalFormat decimalFormat = new DecimalFormat("#.##");
        decimalFormat.setGroupingUsed(false);
        decimalFormat.setRoundingMode(RoundingMode.DOWN);

        ObjectReferenceType targetObjectRef = outlierResult.getObject();
        PrismObject<RoleType> roleTypeObject = roleAnalysisService.getRoleTypeObject(targetObjectRef.getOid(), task, result);
        if (roleTypeObject == null) {
            return null;
        }

        PolyString name = roleTypeObject.getName();
        double confidence = 100 - outlierResult.getConfidence();
        int outlierConfidenceInt = (int) confidence;
        String description = "Assignment has been marked as outlier object due to confidence score:";

        OutlierObjectModel outlierObjectModel = new OutlierObjectModel(
                name.getOrig(), description, outlierConfidenceInt, outlierResult.getCreateTimestamp().toString());

        double occurInCluster = outlierResult.getFrequency() * 100;
        String descriptionOccurInCluster = "Outlier assignment coverage in cluster.";

        OutlierItemModel occurInClusterItemModel = new OutlierItemModel(String.format("%.2f", occurInCluster)
                + "%", descriptionOccurInCluster, "fa fa-cubes");
        outlierObjectModel.addOutlierItemModel(occurInClusterItemModel);

        int indirectAssignments = roleTypeObject.asObjectable().getInducement().size();
        String indirectAssignmentsDescription = "Indirect role assignments of the outlier assignment.";

        OutlierItemModel indirectAssignmentsItemModel = new OutlierItemModel(String.valueOf(indirectAssignments),
                indirectAssignmentsDescription, "fe fe-role");
        outlierObjectModel.addOutlierItemModel(indirectAssignmentsItemModel);

        int roleMemberCount = 0;

        Map<String, PrismObject<UserType>> userExistCache = new HashMap<>();
        roleAnalysisService.extractUserTypeMembers(
                userExistCache, null,
                new HashSet<>(Collections.singleton(roleTypeObject.getOid())),
                task, result);
        roleMemberCount = userExistCache.size();

        String roleMemberDescription = "Role member count of the outlier assignment.";

        OutlierItemModel roleMemberItemModel = new OutlierItemModel(String.valueOf(roleMemberCount),
                roleMemberDescription, "fe fe-user");
        outlierObjectModel.addOutlierItemModel(roleMemberItemModel);

        RoleAnalysisAttributeAnalysisResult roleAnalysisAttributeAnalysisResult = roleAnalysisService
                .resolveRoleMembersAttribute(roleTypeObject.getOid(), task, result);

        RoleAnalysisAttributeAnalysisResult userAttributes = roleAnalysisService.resolveUserAttributes(userTypeObject);

        RoleAnalysisAttributeAnalysisResult compareAttributeResult = roleAnalysisService
                .resolveSimilarAspect(userAttributes, roleAnalysisAttributeAnalysisResult);

        int userCountInRepo = roleAnalysisService.countObjects(UserType.class, null, null, task, result);

        double memberPercentageRepo = (((double) roleMemberCount / userCountInRepo) * 100);
        String memberPercentageRepoDescription = "Role member percentage compared to all users in the repository.";

        OutlierItemModel memberPercentageRepoItemModel = new OutlierItemModel(String.format("%.2f",
                memberPercentageRepo) + "%", memberPercentageRepoDescription, "fe fe-user");
        outlierObjectModel.addOutlierItemModel(memberPercentageRepoItemModel);

        double averageItemsOccurs = 0;
        assert compareAttributeResult != null;
        int attributeAboveThreshold = 0;
        int threshold = 80;
        StringBuilder attributeDescriptionThreshold = new StringBuilder();
        attributeDescriptionThreshold.append("Attributes with occurrence above ").append(threshold).append("%: ");
        List<RoleAnalysisAttributeAnalysis> attributeAnalysis = compareAttributeResult.getAttributeAnalysis();
        for (RoleAnalysisAttributeAnalysis analysis : attributeAnalysis) {
            Double density = analysis.getDensity();
            if (density != null) {

                if (density >= threshold) {
                    attributeAboveThreshold++;
                    attributeDescriptionThreshold.append(analysis.getItemPath()).append(", ");
                }
                averageItemsOccurs += density;
            }
        }

        if (attributeAboveThreshold == 0) {
            attributeDescriptionThreshold = new StringBuilder("No attributes with occurrence above ")
                    .append(threshold).append("%.");
        }

        OutlierItemModel attributeItemModelThreshold = new OutlierItemModel(attributeAboveThreshold
                + " attribute(s)", attributeDescriptionThreshold.toString(), "fa fa-cogs");
        outlierObjectModel.addOutlierItemModel(attributeItemModelThreshold);

        averageItemsOccurs = averageItemsOccurs / attributeAnalysis.size();

        String attributeDescription = "Items factor outlier assignment vs members.";

        OutlierItemModel attributeItemModel = new OutlierItemModel(String.format("%.2f", averageItemsOccurs)
                + "%", attributeDescription, "fa fa-cogs");
        outlierObjectModel.addOutlierItemModel(attributeItemModel);

        return outlierObjectModel;
    }

}
