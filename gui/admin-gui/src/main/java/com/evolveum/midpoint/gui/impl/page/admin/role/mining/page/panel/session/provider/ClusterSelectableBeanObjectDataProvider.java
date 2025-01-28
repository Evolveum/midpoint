package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session.provider;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanObjectDataProvider;

import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.google.common.collect.ListMultimap;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * A temporary implementation of a data provider for managing and sorting role analysis cluster objects in the session.
 *
 * <p>This class is part of the GUI layer and is specifically designed for the role analysis feature. It allows
 * efficient handling and presentation of session-based cluster data in the absence of full database support.
 * The provider handles sorting, filtering, and pagination logic for clusters based on different properties such as:
 * <ul>
 *     <li>Cluster name</li>
 *     <li>Detected reduction metrics</li>
 *     <li>Outlier count</li>
 * </ul>
 *
 * <p>Note: This is a temporary class and will be replaced when database support and a proper design for
 * logic implementation are completed.
 */
public class ClusterSelectableBeanObjectDataProvider extends SelectableBeanObjectDataProvider<RoleAnalysisClusterType> {

    public static final String SORT_NAME_PROPERTY = ObjectType.F_NAME.getLocalPart();
    public static final String SORT_REDUCTION_PROPERTY = AnalysisClusterStatisticType.F_DETECTED_REDUCTION_METRIC.getLocalPart();
    public static final String SORT_OUTLIER_COUNT_PROPERTY = "outlierCount";

    private List<RoleAnalysisClusterType> sessionClustersByType;
    private final LoadableModel<ListMultimap<String, String>> clusterMappedClusterOutliers;
    private final RoleAnalysisClusterCategory category;
    private final RoleAnalysisSessionType sessionObject;

    public ClusterSelectableBeanObjectDataProvider(Component component,
                                                   IModel<Search<RoleAnalysisClusterType>> search,
                                                   Set<RoleAnalysisClusterType> selected,
                                                   RoleAnalysisClusterCategory category,
                                                   LoadableModel<ListMultimap<String, String>> clusterMappedClusterOutliers,
                                                   RoleAnalysisSessionType sessionObject) {
        super(component, search, selected);

        this.clusterMappedClusterOutliers = clusterMappedClusterOutliers;
        this.category = category;
        this.sessionObject = sessionObject;

        RoleAnalysisOptionType analysisOption = this.sessionObject.getAnalysisOption();

        if (RoleAnalysisProcedureType.OUTLIER_DETECTION.equals(analysisOption.getAnalysisProcedureType())) {
            this.setSort(SORT_OUTLIER_COUNT_PROPERTY, SortOrder.DESCENDING);
        } else {
            this.setSort(SORT_REDUCTION_PROPERTY, SortOrder.DESCENDING);
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected List<RoleAnalysisClusterType> searchObjects(Class type,
            ObjectQuery query,
            Collection collection,
            Task task,
            OperationResult result) {
        ListMultimap<String, String> mappedClusterOutliers = resolveClusterMappedObjects();

        String property = getSort().getProperty();
        boolean ascending = getSort().isAscending();
        sortProvider(property, ascending, sessionClustersByType, mappedClusterOutliers);

        Integer offset = query.getPaging().getOffset();
        Integer maxSize = query.getPaging().getMaxSize();
        return sessionClustersByType.subList(offset, offset + maxSize);
    }

    @Override
    protected Integer countObjects(Class<RoleAnalysisClusterType> type,
            ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> currentOptions,
            Task task,
            OperationResult result) {
        this.sessionClustersByType = loadSessionClusters(category);

        return sessionClustersByType.size();
    }

    private List<RoleAnalysisClusterType> loadSessionClusters(RoleAnalysisClusterCategory category) {
        PageBase pageBase = getPageBase();
        Task task = pageBase.createSimpleTask("loadClusters");
        OperationResult result = task.getResult();
        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
        return roleAnalysisService.getSessionClustersByType(
                getSessionObject().getOid(), category, task, result);
    }

    private @Nullable ListMultimap<String, String> resolveClusterMappedObjects() {
        LoadableModel<ListMultimap<String, String>> clusterMappedClusterOutliersModel = getMappedClusterOutliers();

        if (clusterMappedClusterOutliersModel != null) {
            return clusterMappedClusterOutliersModel.getObject();
        }
        return null;
    }

    private void sortProvider(@NotNull String property,
            boolean ascending,
            List<RoleAnalysisClusterType> sessionClustersByType,
            ListMultimap<String, String> clusterMappedClusterOutliers) {
        if (property.equals(SORT_NAME_PROPERTY)) {
            sessionClustersByType.sort((o1, o2) -> {
                int compare = o1.getName().getOrig().compareTo(o2.getName().getOrig());
                return ascending ? compare : -compare;
            });
        } else if (property.equals(SORT_REDUCTION_PROPERTY)) {
            sortByReductionMetric(
                    sessionClustersByType, ascending);
        } else if (property.equals(SORT_OUTLIER_COUNT_PROPERTY)) {
            sortByOutliersCount(
                    sessionClustersByType, clusterMappedClusterOutliers, ascending);
        }
    }

    private static void sortByOutliersCount(
            @NotNull List<RoleAnalysisClusterType> sessionClustersByType,
            ListMultimap<String, String> clusterMappedClusterOutliers,
            boolean ascending) {
        sessionClustersByType.sort((o1, o2) -> {
            int outlierComparison = Integer.compare(
                    clusterMappedClusterOutliers.get(o1.getOid()).size(),
                    clusterMappedClusterOutliers.get(o2.getOid()).size()
            );

            return resolveSortingCompare(ascending, o1, o2, outlierComparison);
        });
    }

    private static void sortByReductionMetric(
            @NotNull List<RoleAnalysisClusterType> sessionClustersByType,
            boolean ascending) {
        sessionClustersByType.sort((o1, o2) -> {
            AnalysisClusterStatisticType o1ClusterStatistics = o1.getClusterStatistics();
            AnalysisClusterStatisticType o2ClusterStatistics = o2.getClusterStatistics();

            if (o1ClusterStatistics == null || o2ClusterStatistics == null) {
                return 0;
            }

            Double o1Metric = o1ClusterStatistics.getDetectedReductionMetric();
            Double o2Metric = o2ClusterStatistics.getDetectedReductionMetric();

            if (o1Metric == null || o2Metric == null) {
                return 0;
            }

            int metricComparison = Double.compare(o1Metric, o2Metric);

            return resolveSortingCompare(ascending, o1, o2, metricComparison);
        });
    }

    private static int resolveSortingCompare(boolean ascending,
            RoleAnalysisClusterType o1,
            RoleAnalysisClusterType o2,
            int metricComparison) {
        if (!ascending) {
            metricComparison = -metricComparison;
        }

        if (metricComparison != 0) {
            return metricComparison;
        }

        String name1 = o1.getName() != null ? o1.getName().getOrig().toLowerCase() : "";
        String name2 = o2.getName() != null ? o2.getName().getOrig().toLowerCase() : "";

        return ascending ? name1.compareTo(name2) : name2.compareTo(name1);
    }

    private RoleAnalysisSessionType getSessionObject() {
        return this.sessionObject;
    }

    private LoadableModel<ListMultimap<String, String>> getMappedClusterOutliers() {
        return this.clusterMappedClusterOutliers;
    }

}
