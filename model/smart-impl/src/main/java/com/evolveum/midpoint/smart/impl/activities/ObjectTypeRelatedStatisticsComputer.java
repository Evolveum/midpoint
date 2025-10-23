package com.evolveum.midpoint.smart.impl.activities;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.processor.ShadowAttributeDefinition;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributeStatisticsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowObjectClassStatisticsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ObjectTypeRelatedStatisticsComputer {

    private final Map<QName, LinkedList<List<?>>> shadowCache = new HashMap<>();

    /**
     * JAXB statistics object being built.
     */
    private final ShadowObjectClassStatisticsType statistics = new ShadowObjectClassStatisticsType();

    public ObjectTypeRelatedStatisticsComputer(ResourceObjectTypeDefinition typeDefinition) {
        statistics.setSize(0);
        List<? extends ShadowAttributeDefinition<?, ?, ?, ?>> attributeDefinitions = typeDefinition.getAttributeDefinitions();
        for (ShadowAttributeDefinition<?, ?, ?, ?> attrDef : attributeDefinitions) {
            createAttributeStatisticsIfNeeded(attrDef.getItemName());
            shadowCache.put(attrDef.getItemName(), new LinkedList<>());
        }
    }

    public void process(ShadowType shadow) {
        statistics.setSize(statistics.getSize() + 1);
        updateShadowCache(shadow);
    }

    /**
     * Performs post-processing of collected values and converts internal structures
     * to JAXB-compatible statistics.
     */
    public void postProcessStatistics() {
        assert shadowCache.values().stream().map(List::size).distinct().count() <= 1;

        for (ShadowAttributeStatisticsType stats : statistics.getAttribute()) {
            QName attrKey = fromAttributeRef(stats.getRef());

            stats.setMissingValueCount(getCountOfMissing(attrKey));
            stats.setUniqueValueCount(getValueCounts(attrKey).size());
        }
    }

    /**
     * Returns the statistics object.
     *
     * @return Shadow object class statistics.
     */
    public ShadowObjectClassStatisticsType getStatistics() {
        return statistics;
    }

    /**
     * Creates attribute statistics for the given attribute name.
     *
     * @param attrName Attribute item name.
     */
    private void createAttributeStatisticsIfNeeded(ItemName attrName) {
        for (ShadowAttributeStatisticsType stats : statistics.getAttribute()) {
            if (attrName.equals(fromAttributeRef(stats.getRef()))) {
                return;
            }
        }
        ShadowAttributeStatisticsType newStats = new ShadowAttributeStatisticsType().ref(toAttributeRef(attrName));
        statistics.getAttribute().add(newStats);
    }

    /** Converts plain attribute name to an {@link ItemPathType} used in statistics beans. */
    private ItemPathType toAttributeRef(QName attrName) {
        return ShadowType.F_ATTRIBUTES.append(attrName).toBean();
    }

    /** Converts {@link ItemPathType} (assuming it's `attributes/xyz`) to a single attribute name. */
    private QName fromAttributeRef(ItemPathType attrRef) {
        return attrRef.getItemPath().rest().asSingleNameOrFail();
    }

    /**
     * Updates the shadow storage with attribute values from the provided shadow object.
     * If the number of value-count pairs for a particular key exceeds the allowed limit,
     * the entry is removed from the storage.
     */
    private void updateShadowCache(ShadowType shadow) {
        for (Map.Entry<QName, LinkedList<List<?>>> entry : shadowCache.entrySet()) {
            entry.getValue().add(ShadowUtil.getAttributeValues(shadow, entry.getKey()));
        }
    }

    /**
     * Calculates and sets the count of missing (null or empty) values for each attribute
     * in the statistics object, updating the corresponding statistics entry.
     */
    private int getCountOfMissing(QName attrKey) {
        return (int) shadowCache.get(attrKey).stream()
                .filter(list -> list == null || list.isEmpty())
                .count();
    }

    /**
     * Returns a map counting the occurrences of each unique single (size == 1) value
     * for the specified attribute key within the shadow storage.
     *
     * @param attrKey the QName key of the attribute to analyze
     * @return a map where keys are string representations of values and values are their counts
     */
    private Map<String, Integer> getValueCounts(QName attrKey) {
        Map<String, Integer> result = new HashMap<>();
        for (List<?> list : shadowCache.get(attrKey)) {
            if (list == null) {
                continue;
            }
            if (list.size() == 1) {
                Object v = list.get(0);
                if (v != null) {
                    String value = v.toString();
                    result.merge(value, 1, Integer::sum);
                }
            }
        }
        return result;
    }

}
