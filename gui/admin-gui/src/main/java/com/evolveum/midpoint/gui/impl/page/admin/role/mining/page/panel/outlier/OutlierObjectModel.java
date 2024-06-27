package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.LOGGER;
import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.getRolesOidAssignment;

import java.io.Serializable;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.xml.datatype.XMLGregorianCalendar;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class OutlierObjectModel implements Serializable {

    String outlierName;
    String outlierDescription;

    public void setOutlierConfidence(double outlierConfidence) {
        this.outlierConfidence = outlierConfidence;
    }

    double outlierConfidence;
    List<OutlierItemModel> outlierItemModels;

    String timeCreated;

    RoleAnalysisPatternInfo patternInfo;

    public OutlierObjectModel(
            @NotNull String outlierName,
            @NotNull String outlierDescription,
            double outlierConfidence,
            String timeCreated,
            RoleAnalysisPatternInfo patternInfo) {
        this.outlierName = outlierName;
        this.outlierDescription = outlierDescription;
        this.outlierConfidence = outlierConfidence;
        this.timeCreated = timeCreated;
        this.outlierItemModels = new ArrayList<>();
        this.patternInfo = patternInfo;
    }

    public void addOutlierItemModel(OutlierItemModel outlierItemModel) {
        this.outlierItemModels.add(outlierItemModel);
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
        GregorianCalendar gregorianCalendar = createTimestamp.toGregorianCalendar();
        Date date = gregorianCalendar.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formattedDate = sdf.format(date);
        if (userTypeObject == null) {
            return null;
        }

        PolyString name = userTypeObject.getName();
        Double clusterConfidence = outlierResult.getClusterConfidence();
        double clusterConfidenceDouble = clusterConfidence != null ? clusterConfidence : 0;
        int outlierConfidenceInt = (int) (clusterConfidenceDouble);
        String outlierDescription = "User has been marked as outlier object due to confidence score:";
        RoleAnalysisPatternInfo patternInfo = outlierResult.getPatternInfo();
        OutlierObjectModel outlierObjectModel = new OutlierObjectModel(
                name.getOrig(), outlierDescription, outlierConfidenceInt, formattedDate, patternInfo);

        List<RoleAnalysisOutlierDescriptionType> propertyOutlier = outlierResult.getResult();
        int propertyCount = propertyOutlier.size();
        Double outlierPropertyConfidence = outlierResult.getOutlierPropertyConfidence();
        double outlierPropertyConfidenceDouble = outlierPropertyConfidence != null ? outlierPropertyConfidence : 0;
        String propertyConfidence = String.format("%.2f", outlierPropertyConfidenceDouble) + "%";
        String propertyDescription;
        if (propertyCount > 1) {
            propertyDescription = "Has been detected multiple (" + propertyCount + ") "
                    + "outlier assignment(s) anomaly with high confidence";
        } else {
            propertyDescription = "Has been detected single outlier assignment anomaly with high confidence";
        }
        OutlierItemModel outlierItemModel = new OutlierItemModel(propertyConfidence, propertyDescription, "fe fe-role");
        outlierObjectModel.addOutlierItemModel(outlierItemModel);

        if (patternInfo != null) {
            Integer detectedPatternCount = patternInfo.getDetectedPatternCount();
            Integer topPatternRelation = patternInfo.getTopPatternRelation();
            Integer totalRelations = patternInfo.getTotalRelations();
            String value = detectedPatternCount + " pattern(s) detected";
            int averageRelation = 0;
            if (totalRelations != 0 && detectedPatternCount != 0) {
                averageRelation = totalRelations / detectedPatternCount;
            }
            String patternDescription = "Maximum coverage is " + String.format("%.2f", patternInfo.getConfidence())
                    + "% (" + topPatternRelation + "relations) "
                    + "and average relation per pattern is " + averageRelation;
            OutlierItemModel patternItemModel = new OutlierItemModel(value, patternDescription, "fa fa-cubes");
            outlierObjectModel.addOutlierItemModel(patternItemModel);
        }

        int similarObjectCount = 0;
        if (outlierResult.getSimilarObjects() != null) {
            similarObjectCount = outlierResult.getSimilarObjects();
        }
        Double membershipDensity = outlierResult.getSimilarObjectsDensity();
        String clusterDescription = "Detected " + similarObjectCount + " similar objects with membership density "
                + String.format("%.2f", membershipDensity) + "%"
                + " and threshold above " + outlierResult.getSimilarObjectsThreshold() + "%. ";
        OutlierItemModel clusterItemModel = new OutlierItemModel(similarObjectCount + " similar object(s)",
                clusterDescription, "fa fa-cubes");
        outlierObjectModel.addOutlierItemModel(clusterItemModel);

        AttributeAnalysis outlierAttributeAnalysis = outlierResult.getAttributeAnalysis();
        RoleAnalysisAttributeAnalysisResult compareAttributeResult = null;
        if (outlierAttributeAnalysis != null) {
            compareAttributeResult = outlierAttributeAnalysis.getUserClusterCompare();
        }

        double averageItemsOccurs = 0;
        int attributeAboveThreshold = 0;
        int threshold = 80;

        StringBuilder attributeDescriptionThreshold = new StringBuilder();
        attributeDescriptionThreshold.append("Attributes with occurrence above ").append(threshold).append("%: ");
        if (compareAttributeResult != null && compareAttributeResult.getAttributeAnalysis() != null) {
            List<RoleAnalysisAttributeAnalysis> attributeAnalysis = compareAttributeResult.getAttributeAnalysis();

            for (RoleAnalysisAttributeAnalysis attribute : attributeAnalysis) {
                Double density = attribute.getDensity();
                if (density != null) {
                    if (density >= threshold) {
                        attributeAboveThreshold++;
                        attributeDescriptionThreshold.append(attribute.getItemPath()).append(", ");
                    }
                    averageItemsOccurs += density;
                }
            }

            if (averageItemsOccurs != 0 && !attributeAnalysis.isEmpty()) {
                averageItemsOccurs = averageItemsOccurs / attributeAnalysis.size();
            }
        }

        String assignmentsFrequencyDescription = "Assignment of the outlier object confidence in the cluster.";
        OutlierItemModel roleAssignmentsFrequencyItemModel = new OutlierItemModel(
                String.format("%.2f", outlierResult.getOutlierAssignmentFrequencyConfidence())
                        + "%", assignmentsFrequencyDescription, "fe fe-role");
        outlierObjectModel.addOutlierItemModel(roleAssignmentsFrequencyItemModel);

        String attributeDescription = "Attribute factor difference outlier vs cluster. "
                + "(average overlap value of outlier attributes with similar objects.)";

        OutlierItemModel attributeItemModel = new OutlierItemModel(String.format("%.2f", averageItemsOccurs) + "%",
                attributeDescription, "fa fa-cogs");
        outlierObjectModel.addOutlierItemModel(attributeItemModel);

        OutlierItemModel attributeItemModelThreshold = new OutlierItemModel(attributeAboveThreshold + " attribute(s)",
                attributeDescriptionThreshold.toString(), "fa fa-cogs");
        outlierObjectModel.addOutlierItemModel(attributeItemModelThreshold);

        String outlierNoiseCategoryDescription = "Outlier noise category of the outlier object.";

        RoleAnalysisOutlierNoiseCategoryType outlierNoiseCategory = outlierResult.getOutlierNoiseCategory();
        if (outlierNoiseCategory != null && outlierNoiseCategory.value() != null) {
            OutlierItemModel noiseCategoryItemModel = new OutlierItemModel(outlierNoiseCategory.value(),
                    outlierNoiseCategoryDescription, "fa fa-cogs");
            outlierObjectModel.addOutlierItemModel(noiseCategoryItemModel);
        }

        List<ObjectReferenceType> duplicatedRoleAssignment = outlierResult.getDuplicatedRoleAssignment();
        String duplicatedRoleAssignmentDescription = "Duplicated role assignments/inducements of the outlier object.";
        int numberOfDuplicatedRoleAssignment = 0;
        if (duplicatedRoleAssignment != null) {
            if (duplicatedRoleAssignment.size() == 1) {
                ObjectReferenceType ref = duplicatedRoleAssignment.get(0);
                if (ref == null || ref.getOid() == null) {
                    numberOfDuplicatedRoleAssignment = 0;
                } else {
                    numberOfDuplicatedRoleAssignment = 1;
                }
            } else {
                numberOfDuplicatedRoleAssignment = duplicatedRoleAssignment.size();
            }
        }
        OutlierItemModel duplicatedRoleAssignmentModel = new OutlierItemModel(String.valueOf(numberOfDuplicatedRoleAssignment),
                duplicatedRoleAssignmentDescription, "fa fa-cogs");
        outlierObjectModel.addOutlierItemModel(duplicatedRoleAssignmentModel);

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
        GregorianCalendar gregorianCalendar = createTimestamp.toGregorianCalendar();
        Date date = gregorianCalendar.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formattedDate = sdf.format(date);
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
        String propertyDescription;
        if (propertyCount > 1) {
            propertyDescription = "Has been detected multiple (" + propertyCount + ") "
                    + "outlier assignment(s) anomaly with high confidence";
        } else {
            propertyDescription = "Has been detected single outlier assignment anomaly with high confidence";
        }

        RoleAnalysisPatternInfo patternInfo = outlierResult.getPatternInfo();
        OutlierObjectModel outlierObjectModel = new OutlierObjectModel(
                name.getOrig(), outlierDescription, outlierConfidenceInt, formattedDate, patternInfo);

        if (patternInfo != null) {
            Integer detectedPatternCount = patternInfo.getDetectedPatternCount();
            Integer topPatternRelation = patternInfo.getTopPatternRelation();
            Integer totalRelations = patternInfo.getTotalRelations();
            String value = detectedPatternCount + " pattern(s) detected";
            int averageRelation = 0;
            if (totalRelations != 0 && detectedPatternCount != 0) {
                averageRelation = totalRelations / detectedPatternCount;
            }
            String patternDescription = "Maximum coverage is " + String.format("%.2f", patternInfo.getConfidence())
                    + "% (" + topPatternRelation + "relations) "
                    + "and average relation per pattern is " + averageRelation;
            OutlierItemModel patternItemModel = new OutlierItemModel(value, patternDescription, "fa fa-cubes");
            outlierObjectModel.addOutlierItemModel(patternItemModel);
        }

        int similarObjectCount = outlierResult.getSimilarObjects();
        Double membershipDensity = outlierResult.getSimilarObjectsDensity();
        String clusterDescription = "Detected " + similarObjectCount + " similar objects with membership density "
                + String.format("%.2f", membershipDensity) + "%";
        OutlierItemModel clusterItemModel = new OutlierItemModel(similarObjectCount
                + " similar object(s)", clusterDescription, "fa fa-cubes");
        outlierObjectModel.addOutlierItemModel(clusterItemModel);

        OutlierItemModel outlierItemModel = new OutlierItemModel(propertyConfidenceRange, propertyDescription,
                "fe fe-role");
        outlierObjectModel.addOutlierItemModel(outlierItemModel);

        AttributeAnalysis outlierAttributeAnalysis = outlierResult.getAttributeAnalysis();
        RoleAnalysisAttributeAnalysisResult compareAttributeResult = null;
        if (outlierAttributeAnalysis != null) {
            compareAttributeResult = outlierAttributeAnalysis.getUserClusterCompare();
        }

        double averageItemsOccurs = 0;
        int attributeAboveThreshold = 0;
        int threshold = 80;

        StringBuilder attributeDescriptionThreshold = new StringBuilder();
        attributeDescriptionThreshold.append("Attributes with occurrence above ").append(threshold).append("%: ");
        if (compareAttributeResult != null && compareAttributeResult.getAttributeAnalysis() != null) {
            List<RoleAnalysisAttributeAnalysis> attributeAnalysis = compareAttributeResult.getAttributeAnalysis();

            for (RoleAnalysisAttributeAnalysis attribute : attributeAnalysis) {
                Double density = attribute.getDensity();
                if (density != null) {
                    if (density >= threshold) {
                        attributeAboveThreshold++;
                        attributeDescriptionThreshold.append(attribute.getItemPath()).append(", ");
                    }
                    averageItemsOccurs += density;
                }
            }

            if (averageItemsOccurs != 0 && !attributeAnalysis.isEmpty()) {
                averageItemsOccurs = averageItemsOccurs / attributeAnalysis.size();
            }

        }

        String assignmentsFrequencyDescription = "Assignment of the outlier object confidence in the cluster.";
        OutlierItemModel roleAssignmentsFrequencyItemModel = new OutlierItemModel(
                String.format("%.2f", outlierResult.getOutlierAssignmentFrequencyConfidence())
                        + "%", assignmentsFrequencyDescription, "fe fe-role");
        outlierObjectModel.addOutlierItemModel(roleAssignmentsFrequencyItemModel);

        String attributeDescription = "Attribute factor difference outlier vs cluster. "
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
            @NotNull PrismObject<UserType> userTypeObject,
            @NotNull RoleAnalysisOutlierType outlierParent) {
        DecimalFormat decimalFormat = new DecimalFormat("#.##");
        decimalFormat.setGroupingUsed(false);
        decimalFormat.setRoundingMode(RoundingMode.DOWN);

        ObjectReferenceType targetObjectRef = outlierResult.getObject();
        PrismObject<RoleType> roleTypeObject = roleAnalysisService.getRoleTypeObject(targetObjectRef.getOid(), task, result);
        if (roleTypeObject == null) {
            return null;
        }

        PolyString name = roleTypeObject.getName();
        double confidence = 100 - outlierResult.getConfidenceDeviation();
        int outlierConfidenceInt = (int) confidence;
        String description = "Assignment has been marked as outlier object due to confidence score:";

        XMLGregorianCalendar createTimestamp = outlierResult.getCreateTimestamp();
        GregorianCalendar gregorianCalendar = createTimestamp.toGregorianCalendar();
        Date date = gregorianCalendar.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formattedDate = sdf.format(date);
        RoleAnalysisPatternInfo patternInfo = outlierResult.getPatternInfo();
        OutlierObjectModel outlierObjectModel = new OutlierObjectModel(
                name.getOrig(), description, outlierConfidenceInt, formattedDate, patternInfo);

        if (patternInfo != null) {
            Integer detectedPatternCount = patternInfo.getDetectedPatternCount();
            Integer topPatternRelation = patternInfo.getTopPatternRelation();
            Integer totalRelations = patternInfo.getTotalRelations();
            String value = detectedPatternCount + " pattern(s) detected";
            int averageRelation = 0;
            if (totalRelations != 0 && detectedPatternCount != 0) {
                averageRelation = totalRelations / detectedPatternCount;
            }
            String patternDescription = "Maximum coverage is " + String.format("%.2f", patternInfo.getConfidence())
                    + "% (" + topPatternRelation + "relations) "
                    + "and average relation per pattern is " + averageRelation;
            OutlierItemModel patternItemModel = new OutlierItemModel(value, patternDescription, "fa fa-cubes");
            outlierObjectModel.addOutlierItemModel(patternItemModel);
        }
        Double outlierCoverageConfidence = outlierResult.getOutlierCoverageConfidence();
        double occurInCluster = outlierCoverageConfidence == null ? 0 : outlierCoverageConfidence;

        String descriptionOccurInCluster = "Outlier assignment coverage in cluster.";

        OutlierItemModel occurInClusterItemModel = new OutlierItemModel(String.format("%.2f", occurInCluster)
                + "%", descriptionOccurInCluster, "fa fa-cubes");
        outlierObjectModel.addOutlierItemModel(occurInClusterItemModel);

        Double memberCoverageConfidence = outlierResult.getMemberCoverageConfidence();
        double memberPercentageRepo = memberCoverageConfidence == null ? 0 : memberCoverageConfidence;

        String memberPercentageRepoDescription = "Role member percentage compared to all users in the repository.";

        OutlierItemModel memberPercentageRepoItemModel = new OutlierItemModel(String.format("%.2f",
                memberPercentageRepo) + "%", memberPercentageRepoDescription, "fa fa-users");
        outlierObjectModel.addOutlierItemModel(memberPercentageRepoItemModel);

        ObjectReferenceType sessionRef = outlierResult.getSession();
        PrismObject<RoleAnalysisSessionType> session = roleAnalysisService.getSessionTypeObject(sessionRef.getOid(), task, result);

        List<RoleAnalysisAttributeDef> attributesForUserAnalysis = null;
        if (session == null) {
            LOGGER.warn("Session object is null");
        } else {
            attributesForUserAnalysis = roleAnalysisService.resolveAnalysisAttributes(
                    session.asObjectable(), UserType.COMPLEX_TYPE);
        }

        if (attributesForUserAnalysis != null) {
            RoleAnalysisAttributeAnalysisResult roleAnalysisAttributeAnalysisResult = roleAnalysisService
                    .resolveRoleMembersAttribute(roleTypeObject.getOid(), task, result, attributesForUserAnalysis);

            RoleAnalysisAttributeAnalysisResult userAttributes = roleAnalysisService.resolveUserAttributes(
                    userTypeObject, attributesForUserAnalysis);

            RoleAnalysisAttributeAnalysisResult compareAttributeResult = roleAnalysisService
                    .resolveSimilarAspect(userAttributes, roleAnalysisAttributeAnalysisResult);

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

            if (averageItemsOccurs != 0 && !attributeAnalysis.isEmpty()) {
                averageItemsOccurs = averageItemsOccurs / attributeAnalysis.size();
            }
            if (attributeAboveThreshold == 0) {
                attributeDescriptionThreshold = new StringBuilder("No attributes with occurrence above ")
                        .append(threshold).append("%.");
            }

            String attributeDescription = "Attribute factor difference outlier assignment vs members.";

            OutlierItemModel attributeItemModel = new OutlierItemModel(String.format("%.2f", averageItemsOccurs)
                    + "%", attributeDescription, "fa fa-cogs");
            outlierObjectModel.addOutlierItemModel(attributeItemModel);

            OutlierItemModel attributeItemModelThreshold = new OutlierItemModel(attributeAboveThreshold
                    + " attribute(s)", attributeDescriptionThreshold.toString(), "fa fa-cogs");
            outlierObjectModel.addOutlierItemModel(attributeItemModelThreshold);

        }

        List<ObjectReferenceType> duplicatedRoleAssignment = outlierParent.getDuplicatedRoleAssignment();
        String oid = roleTypeObject.asObjectable().getOid();
        String value = "Not duplicated";
        if (duplicatedRoleAssignment != null && !duplicatedRoleAssignment.isEmpty()) {
            for (ObjectReferenceType ref : duplicatedRoleAssignment) {
                if (ref != null && ref.getOid() != null && ref.getOid().equals(oid)) {
                    value = "Duplicated";
                }
            }
        }

        String duplicatedRoleAssignmentDescription = "Specifies if the assignment was duplicated.";
        OutlierItemModel duplicatedRoleAssignmentModel = new OutlierItemModel(value,
                duplicatedRoleAssignmentDescription, "fa fa-cogs");
        outlierObjectModel.addOutlierItemModel(duplicatedRoleAssignmentModel);

        String roleMemberDescription = "Specifies from which source the assignment was assigned.";

        OutlierItemModel roleMemberItemModel = new OutlierItemModel("Unknown source",
                roleMemberDescription, "fe fe-user");
        outlierObjectModel.addOutlierItemModel(roleMemberItemModel);

        int indirectAssignments = roleTypeObject.asObjectable().getInducement().size();
        String indirectAssignmentsDescription = "Indirect role assignments of the outlier assignment.";

        OutlierItemModel indirectAssignmentsItemModel = new OutlierItemModel(String.valueOf(indirectAssignments),
                indirectAssignmentsDescription, "fe fe-role");
        outlierObjectModel.addOutlierItemModel(indirectAssignmentsItemModel);

        String deviationDescription = "Unreliability of assignment based on a standard distribution";

        double deviationConfidence = 0;
        if (outlierResult.getConfidenceDeviation() != null) {
            deviationConfidence = outlierResult.getConfidenceDeviation() * 100;
        }

        OutlierItemModel deviationItemModel = new OutlierItemModel(String.format("%.2f", deviationConfidence)
                + "%", deviationDescription, "fa fa-key");
        outlierObjectModel.addOutlierItemModel(deviationItemModel);

        Double totalConfidence = outlierResult.getConfidence();
        if (totalConfidence != null) {
            double totalConfidenceDouble = totalConfidence;
            int outlierAssignmentConfidence = (int) (totalConfidenceDouble);
            outlierObjectModel.setOutlierConfidence(outlierAssignmentConfidence);
        } else {
            outlierObjectModel.setOutlierConfidence(0);
        }

        return outlierObjectModel;
    }

}
