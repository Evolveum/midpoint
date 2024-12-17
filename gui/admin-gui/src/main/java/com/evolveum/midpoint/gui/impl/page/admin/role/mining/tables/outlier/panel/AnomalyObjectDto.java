/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.*;

//TODO this is ugly class, need to be redesigned and refactored. We need also design logic for created outlier access
// table (anomaly must be provided with associated partition and this is not in all cases clear "explanation").
/**
 * Data Transfer Object (DTO) for handling anomaly objects in role analysis.
 * This class is used to store and manage the data of the anomaly objects.
 */
public class AnomalyObjectDto implements Serializable {

    transient IModel<RoleAnalysisOutlierType> outlierModel;
    transient IModel<List<RoleAnalysisOutlierPartitionType>> partitionModel;
    transient IModel<ListMultimap<String, DetectedAnomalyResult>> anomalyResultMap;
    transient IModel<ListMultimap<String, RoleAnalysisOutlierPartitionType>> anomalyPartitionMap;
    transient IModel<RoleAnalysisOutlierPartitionType> associatedPartitionModel;

    transient Set<String> anomalyObjectOidSet;
    AnomalyTableCategory category;

    public AnomalyObjectDto(
            @NotNull RoleAnalysisOutlierType outlier,
            @Nullable RoleAnalysisOutlierPartitionType partition,
            @NotNull AnomalyTableCategory category) {
        this.category = category;
        initModels(outlier, partition);
    }

    private void initModels(@NotNull RoleAnalysisOutlierType outlier, @Nullable RoleAnalysisOutlierPartitionType partition) {

        associatedPartitionModel = new LoadableModel<>() {
            @Override
            protected RoleAnalysisOutlierPartitionType load() {
                return partition;
            }
        };

        outlierModel = new LoadableModel<>() {
            @Override
            protected RoleAnalysisOutlierType load() {
                return outlier;
            }
        };

        if (category == AnomalyTableCategory.PARTITION_ANOMALY && partition != null) {
            partitionModel = new LoadableModel<>() {
                @Override
                protected List<RoleAnalysisOutlierPartitionType> load() {
                    return Collections.singletonList(partition);
                }
            };
        } else {
            partitionModel = new LoadableModel<>() {
                @Override
                protected List<RoleAnalysisOutlierPartitionType> load() {
                    RoleAnalysisOutlierType outlier = getOutlierModelObject();
                    return outlier.getPartition();
                }
            };
        }

        anomalyResultMap = new LoadableModel<>() {
            @Override
            protected ListMultimap<String, DetectedAnomalyResult> load() {
                return ArrayListMultimap.create();
            }
        };

        anomalyPartitionMap = new LoadableModel<>() {
            @Override
            protected ListMultimap<String, RoleAnalysisOutlierPartitionType> load() {
                return ArrayListMultimap.create();
            }
        };

        anomalyObjectOidSet = new HashSet<>();
        for (RoleAnalysisOutlierPartitionType outlierPartition : getPartitionModelObject()) {
            List<DetectedAnomalyResult> partitionAnalysis = outlierPartition.getDetectedAnomalyResult();
            for (DetectedAnomalyResult anomalyResult : partitionAnalysis) {
                String oid = anomalyResult.getTargetObjectRef().getOid();
                anomalyObjectOidSet.add(oid);
                anomalyResultMap.getObject().put(oid, anomalyResult);
                anomalyPartitionMap.getObject().put(oid, outlierPartition);
            }
        }
    }

    protected @NotNull SelectableBeanObjectDataProvider<RoleType> buildProvider(
            @NotNull Component component,
            @NotNull PageBase pageBase,
            @NotNull Task task,
            @NotNull OperationResult result) {

        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
        List<RoleType> roles = new ArrayList<>();

        if (category.equals(AnomalyTableCategory.OUTLIER_ACCESS)) {
            handleOutlierAccessRoles(roleAnalysisService, roles, task, result);
        } else {
            loadRolesFromAnomalyOidSet(roleAnalysisService, roles, task, result);
        }

        return new SelectableBeanObjectDataProvider<>(component, Set.of()) {

            @SuppressWarnings("rawtypes")
            @Override
            protected List<RoleType> searchObjects(
                    Class type,
                    ObjectQuery query,
                    Collection collection,
                    Task task,
                    OperationResult result) {
                Integer offset = query.getPaging().getOffset();
                Integer maxSize = query.getPaging().getMaxSize();
                return roles.subList(offset, Math.min(offset + maxSize, roles.size()));
            }

            @Override
            protected Integer countObjects(
                    Class<RoleType> type,
                    ObjectQuery query,
                    Collection<SelectorOptions<GetOperationOptions>> currentOptions,
                    Task task,
                    OperationResult result) {
                return roles.size();
            }
        };
    }

    private void handleOutlierAccessRoles(@NotNull RoleAnalysisService roleAnalysisService,
            List<RoleType> roles,
            Task task,
            OperationResult result) {
        ObjectReferenceType targetObjectRef = outlierModel.getObject().getObjectRef();
        PrismObject<UserType> userTypeObject = roleAnalysisService.getUserTypeObject(targetObjectRef.getOid(), task, result);

        if (userTypeObject != null) {
            UserType user = userTypeObject.asObjectable();

            user.getAssignment().stream()
                    .filter(assignment -> RoleType.COMPLEX_TYPE.equals(assignment.getTargetRef().getType()))
                    .map(assignment -> roleAnalysisService.getRoleTypeObject(assignment.getTargetRef().getOid(), task, result))
                    .filter(Objects::nonNull)
                    .map(roleTypePrismObject -> roleTypePrismObject.asObjectable()).forEach(roles::add);

            user.getRoleMembershipRef().stream()
                    .filter(ref -> RoleType.COMPLEX_TYPE.equals(ref.getType()))
                    .map(ref -> roleAnalysisService.getRoleTypeObject(ref.getOid(), task, result))
                    .filter(Objects::nonNull)
                    .map(roleTypePrismObject -> roleTypePrismObject.asObjectable()).forEach(roles::add);
        }
    }

    private void loadRolesFromAnomalyOidSet(
            RoleAnalysisService roleAnalysisService,
            List<RoleType> roles,
            Task task,
            OperationResult result) {
        for (String oid : anomalyObjectOidSet) {
            PrismObject<RoleType> rolePrismObject = roleAnalysisService.getRoleTypeObject(oid, task, result);
            if (rolePrismObject != null) {
                roles.add(rolePrismObject.asObjectable());
            }
        }
    }

    public IModel<RoleAnalysisOutlierType> getOutlierModel() {
        return outlierModel;
    }

    public IModel<List<RoleAnalysisOutlierPartitionType>> getPartitionModel() {
        return partitionModel;
    }

    public List<RoleAnalysisOutlierPartitionType> getPartitionModelObject() {
        return partitionModel.getObject();
    }

    public RoleAnalysisOutlierType getOutlierModelObject() {
        return outlierModel.getObject();
    }

    public RoleAnalysisOutlierPartitionType getPartitionSingleModelObject() {
        if(getAssociatedPartitionModel() != null && getAssociatedPartitionModel().getObject() != null) {
            return getAssociatedPartitionModel().getObject();
        }
        //TODO
        return getPartitionModelObject().get(0);
    }

    public ListMultimap<String, DetectedAnomalyResult> getAnomalyResultMapModelObject() {
        return anomalyResultMap.getObject();
    }

    public ListMultimap<String, RoleAnalysisOutlierPartitionType> getAnomalyPartitionMapModelObject() {
        return anomalyPartitionMap.getObject();
    }
    public IModel<RoleAnalysisOutlierPartitionType> getAssociatedPartitionModel() {
        return associatedPartitionModel;
    }

    public AnomalyTableCategory getCategory() {
        return category;
    }
}
