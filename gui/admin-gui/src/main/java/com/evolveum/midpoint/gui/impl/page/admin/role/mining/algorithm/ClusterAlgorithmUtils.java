package com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClusterType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.math3.ml.clustering.Cluster;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.evolveum.midpoint.gui.api.component.mining.analyse.tools.jaccard.JacquardSorter.jaccSortDataPoints;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.Tools.endTimer;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.Tools.startTimer;
import static com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions.LOGGER;

public class ClusterAlgorithmUtils {

    PageBase pageBase;
    OperationResult operationResult;

    public ClusterAlgorithmUtils(PageBase pageBase, OperationResult operationResult) {
        this.pageBase = pageBase;
        this.operationResult = operationResult;
    }

    @NotNull
    public List<PrismObject<ClusterType>> processClusters(String identifier, List<DataPoint> dataPoints,
            List<Cluster<DataPoint>> clusters) {
        long start;
        start = startTimer("generate clusters mp objects");
        List<PrismObject<ClusterType>> clusterTypeObjectWithStatistic = IntStream.range(0, clusters.size())
                .mapToObj(i -> prepareClusters(clusters.get(i).getPoints(), String.valueOf(i), identifier, dataPoints))
                .collect(Collectors.toList());
        endTimer(start, "generate clusters mp objects");

        start = startTimer("generate outliers mp objects");
        PrismObject<ClusterType> clusterTypePrismObject = prepareOutlierClusters(dataPoints, identifier);
        clusterTypeObjectWithStatistic.add(clusterTypePrismObject);
        endTimer(start, "generate outliers mp objects");
        return clusterTypeObjectWithStatistic;
    }

    public static List<DataPoint> prepareDataPoints(Map<List<String>, List<String>> chunkMap) {
        List<DataPoint> dataPoints = new ArrayList<>();
        for (Map.Entry<List<String>, List<String>> entry : chunkMap.entrySet()) {
            List<String> points = entry.getKey();
            List<String> elements = entry.getValue();
            double[] vectorPoints = new double[points.size()];
            for (int i = 0; i < points.size(); i++) {
                String point = points.get(i);
                vectorPoints[i] = UUIDToDoubleConverter.convertUUid(point);
            }
            dataPoints.add(new DataPoint(vectorPoints, elements.get(0), elements, points));
        }
        return dataPoints;
    }

    public PrismObject<ClusterType> prepareClusters(List<DataPoint> dataPointCluster, String clusterIndex,
            String identifier, List<DataPoint> dataPoints) {
        List<DataPoint> sortedDataPoints = jaccSortDataPoints(dataPointCluster);

        int minVectorPoint = Integer.MAX_VALUE;
        int maxVectorPoint = -1;

        int totalDataPoints = sortedDataPoints.size();
        int sumPoints = 0;

        List<String> elementsOid = new ArrayList<>();
        Map<String, Integer> frequencyMap = new HashMap<>();

        for (DataPoint dataPoint : sortedDataPoints) {
            List<String> points = dataPoint.getPoints();
            dataPoints.remove(dataPoint);
            for (String point : points) {
                frequencyMap.compute(point, (key, counter) -> counter == null ? 1 : counter + 1);
            }

            List<String> elements = dataPoint.getElements();

            elementsOid.addAll(elements);

            int pointsCount = points.size();
            sumPoints += pointsCount;
            minVectorPoint = Math.min(minVectorPoint, pointsCount);
            maxVectorPoint = Math.max(maxVectorPoint, pointsCount);
        }

        double meanPoints = (double) sumPoints / totalDataPoints;

        List<String> sortedPoints = frequencyMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        double density = (sumPoints / (double) (dataPointCluster.size() * frequencyMap.size())) * 100;

        PolyStringType name = PolyStringType.fromOrig("cluster_" + clusterIndex);

        return generateClusterObject(elementsOid, sortedPoints, meanPoints, density, minVectorPoint,
                maxVectorPoint, name, identifier);
    }

    public PrismObject<ClusterType> prepareOutlierClusters(List<DataPoint> dataPoints,
            String identifier) {

        int minVectorPoint = Integer.MAX_VALUE;
        int maxVectorPoint = -1;

        int totalDataPoints = dataPoints.size();
        int sumPoints = 0;

        List<String> elementsOid = new ArrayList<>();
        Set<String> pointsSet = new HashSet<>();
        for (DataPoint dataPoint : dataPoints) {
            List<String> points = dataPoint.getPoints();
            pointsSet.addAll(points);
            List<String> elements = dataPoint.getElements();
            elementsOid.addAll(elements);

            int pointsSize = points.size();
            sumPoints += pointsSize;
            minVectorPoint = Math.min(minVectorPoint, pointsSize);
            maxVectorPoint = Math.max(maxVectorPoint, pointsSize);
        }

        double meanPoints = (double) sumPoints / totalDataPoints;

        double density = (sumPoints / (double) (dataPoints.size() * pointsSet.size())) * 100;

        PolyStringType name = PolyStringType.fromOrig("outliers");

        return generateClusterObject(elementsOid, new ArrayList<>(pointsSet), meanPoints, density, minVectorPoint,
                maxVectorPoint, name, identifier);
    }

    private @NotNull PrismObject<ClusterType> generateClusterObject(List<String> elementsOid, List<String> sortedPoints,
            double meanPoints, double density, int minVectorPoint, int maxVectorPoint, PolyStringType name, String identifier) {
        PrismObject<ClusterType> clusterTypePrismObject = null;
        try {
            clusterTypePrismObject = pageBase.getPrismContext()
                    .getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ClusterType.class).instantiate();
        } catch (SchemaException e) {
            LOGGER.error("Error while finding object definition by compile time class ClusterType object: {}", e.getMessage(), e);
        }
        assert clusterTypePrismObject != null;

        ClusterType clusterType = clusterTypePrismObject.asObjectable();

        clusterType.setOid(String.valueOf(UUID.randomUUID()));
        clusterType.setElementCount(elementsOid.size());
        clusterType.getElements().addAll(elementsOid);
        clusterType.getPoints().addAll(sortedPoints);
        clusterType.setPointCount(sortedPoints.size());
        clusterType.setMean(String.format("%.3f", meanPoints));
        clusterType.setDensity(String.format("%.3f", density));
        clusterType.setMinOccupation(minVectorPoint);
        clusterType.setMaxOccupation(maxVectorPoint);
        clusterType.setName(name);
        clusterType.setIdentifier(identifier);

        return clusterTypePrismObject;
    }

}
