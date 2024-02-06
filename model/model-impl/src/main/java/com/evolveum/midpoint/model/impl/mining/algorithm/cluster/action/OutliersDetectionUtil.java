package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningOperationChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisSortMode;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class OutliersDetectionUtil {

    public static RoleAnalysisOutliersType executeOutliersAnalysis(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisClusterType cluster,
            @NotNull RoleAnalysisOptionType analysisOption,
            double minFrequency,
            @NotNull Task task,
            @NotNull OperationResult result) {

        RoleAnalysisProcessModeType processMode = analysisOption.getProcessMode();

        MiningOperationChunk miningOperationChunk = roleAnalysisService.prepareCompressedMiningStructure(cluster, true,
                processMode, result, task);

        List<MiningRoleTypeChunk> miningRoleTypeChunks = miningOperationChunk.getMiningRoleTypeChunks(RoleAnalysisSortMode.NONE);

        HashMap<String, RoleAnalysisOutlierResultType> map = new HashMap<>();

        //TODO role mode
        if (processMode.equals(RoleAnalysisProcessModeType.USER)) {
            for (MiningRoleTypeChunk miningRoleTypeChunk : miningRoleTypeChunks) {

                double frequency = miningRoleTypeChunk.getFrequency();
                if (frequency * 100 < minFrequency) {
                    List<String> roles = miningRoleTypeChunk.getMembers();
                    List<String> users = miningRoleTypeChunk.getProperties();

                    List<RoleAnalysisOutlierDescriptionType> prepareRoleOutliers = new ArrayList<>();

                    roles.forEach(role -> {
                        RoleAnalysisOutlierDescriptionType outlierDescription = new RoleAnalysisOutlierDescriptionType();
                        outlierDescription.setCategory(OutlierCategory.INNER_OUTLIER);
                        outlierDescription.setObject(new ObjectReferenceType().oid(role).type(RoleType.COMPLEX_TYPE));
                        outlierDescription.setConfidence(frequency);
                        prepareRoleOutliers.add(outlierDescription);
                    });

                    for (String user : users) {
                        RoleAnalysisOutlierResultType userOutliers = map.get(user);

                        if (userOutliers == null) {
                            userOutliers = new RoleAnalysisOutlierResultType();
                            userOutliers.setObject(new ObjectReferenceType().oid(user).type(UserType.COMPLEX_TYPE));
                            for (RoleAnalysisOutlierDescriptionType prepareRoleOutlier : prepareRoleOutliers) {
                                userOutliers.getOutliers().add(prepareRoleOutlier.clone());
                            }
                            map.put(user, userOutliers);
                        } else {
                            for (RoleAnalysisOutlierDescriptionType prepareRoleOutlier : prepareRoleOutliers) {
                                userOutliers.getOutliers().add(prepareRoleOutlier.clone());
                            }
                        }

                    }

                }
            }

        }

        RoleAnalysisOutliersType outliers = new RoleAnalysisOutliersType();
        outliers.getOutlier().addAll(map.values());
        return outliers;
    }
}
