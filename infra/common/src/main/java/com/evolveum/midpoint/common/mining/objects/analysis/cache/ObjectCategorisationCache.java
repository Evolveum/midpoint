package com.evolveum.midpoint.common.mining.objects.analysis.cache;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.*;

//TODO build RoleAnalysisIdentifiedCharacteristicsItemType
//TODO count containers

/**
 * A cache for categorizing roles, users, and focus objects
 * based on their identified characteristics.
 */
public class ObjectCategorisationCache {

    private final Map<String, RoleAnalysisIdentifiedCharacteristicsItemType> rolesCategoryMap = new HashMap<>();
    private final Map<String, RoleAnalysisIdentifiedCharacteristicsItemType> usersCategoryMap = new HashMap<>();
    private final Map<String, RoleAnalysisIdentifiedCharacteristicsItemType> focusCategoryMap = new HashMap<>();

    /**
     * Adds a category to the specified object type (Role, User, or Focus).
     *
     * @param oid The OID of the object.
     * @param category The category to add.
     * @param type The type of the object (RoleType, UserType, FocusType).
     */
    public void putCategory(String oid, RoleAnalysisObjectCategorizationType category, QName type) {
        Map<String, RoleAnalysisIdentifiedCharacteristicsItemType> categoryMap = getCategoryMap(type);

        RoleAnalysisIdentifiedCharacteristicsItemType item = categoryMap.get(oid);
        if (item == null) {
            item = new RoleAnalysisIdentifiedCharacteristicsItemType();
            item.setObjectRef(createObjectReference(oid, type));
            categoryMap.put(oid, item);
        }
        item.getCategory().add(category);
    }

    public void putAllCategory(@NotNull Set<String> oids, RoleAnalysisObjectCategorizationType category, QName type) {
        for (String oid : oids) {
            putCategory(oid, category, type);
        }
    }

    /**
     * Retrieves the category for the specified object type and OID.
     *
     * @param oid The OID of the object.
     * @param type The type of the object (RoleType, UserType, FocusType).
     * @return The characteristics item for the given OID and type.
     */
    public RoleAnalysisIdentifiedCharacteristicsItemType getCategory(String oid, QName type) {
        return getCategoryMap(type).get(oid);
    }

    /**
     * Clears all cached categories.
     */
    public void clear() {
        rolesCategoryMap.clear();
        usersCategoryMap.clear();
        focusCategoryMap.clear();
    }

    /**
     * Determines the map to use based on the object type.
     *
     * @param type The type of the object.
     * @return The map corresponding to the type.
     * @throws IllegalArgumentException if the type is unsupported.
     */
    private Map<String, RoleAnalysisIdentifiedCharacteristicsItemType> getCategoryMap(QName type) {
        if (type == RoleType.COMPLEX_TYPE) {
            return rolesCategoryMap;
        } else if (type == UserType.COMPLEX_TYPE) {
            return usersCategoryMap;
        } else if (type == FocusType.COMPLEX_TYPE) {
            return focusCategoryMap;
        } else {
            throw new IllegalArgumentException("Unsupported object type: " + type);
        }
    }

    /**
     * Creates an object reference with the specified OID and type.
     *
     * @param oid The OID of the object.
     * @param type The type of the object.
     * @return The object reference.
     */
    private ObjectReferenceType createObjectReference(String oid, QName type) {
        return new ObjectReferenceType().oid(oid).type(type);
    }

    /**
     * Builds the RoleAnalysisIdentifiedCharacteristicsType container by aggregating
     * the categorized items from roles, users, and focus maps.
     *
     * @return The RoleAnalysisIdentifiedCharacteristicsType container with categorized items.
     */
    public RoleAnalysisIdentifiedCharacteristicsType build() {
        RoleAnalysisIdentifiedCharacteristicsType container = new RoleAnalysisIdentifiedCharacteristicsType();

        if (!rolesCategoryMap.values().isEmpty()) {
            RoleAnalysisIdentifiedCharacteristicsItemsType roleItems = new RoleAnalysisIdentifiedCharacteristicsItemsType();
            roleItems.getItem().addAll(rolesCategoryMap.values());
            computeCategories(roleItems);
            container.setRoles(roleItems);
            container.setRolesCount(rolesCategoryMap.values().size());
        }

        if (!usersCategoryMap.values().isEmpty()) {
            RoleAnalysisIdentifiedCharacteristicsItemsType userItems = new RoleAnalysisIdentifiedCharacteristicsItemsType();
            userItems.getItem().addAll(usersCategoryMap.values());
            computeCategories(userItems);
            container.setUsers(userItems);
            container.setUsersCount(usersCategoryMap.values().size());
        }

        if (!focusCategoryMap.values().isEmpty()) {
            RoleAnalysisIdentifiedCharacteristicsItemsType focusItems = new RoleAnalysisIdentifiedCharacteristicsItemsType();
            focusItems.getItem().addAll(focusCategoryMap.values());
            computeCategories(focusItems);
            container.setFocus(focusItems);
            container.setFocusCount(focusCategoryMap.values().size());
        }
        return container;
    }

    /**
     * Computes the counts of various categories within the given RoleAnalysisIdentifiedCharacteristicsItemsType items.
     *
     * @param items The items containing the categories to be counted.
     */
    private void computeCategories(@NotNull RoleAnalysisIdentifiedCharacteristicsItemsType items) {
        int unpopularCount = 0;
        int abovePopularCount = 0;
        int noiseCount = 0;
        int noiseExclusiveCount = 0;
        int anomalyCount = 0;
        int anomalyExclusiveCount = 0;
        int outlierCount = 0;

        List<RoleAnalysisIdentifiedCharacteristicsItemType> item = items.getItem();
        for (RoleAnalysisIdentifiedCharacteristicsItemType value : item) {
            for (RoleAnalysisObjectCategorizationType category : value.getCategory()) {
                if (category == RoleAnalysisObjectCategorizationType.UN_POPULAR) {
                    unpopularCount++;
                } else if (category == RoleAnalysisObjectCategorizationType.ABOVE_POPULAR) {
                    abovePopularCount++;
                } else if (category == RoleAnalysisObjectCategorizationType.NOISE) {
                    noiseCount++;
                } else if (category == RoleAnalysisObjectCategorizationType.NOISE_EXCLUSIVE) {
                    noiseExclusiveCount++;
                } else if (category == RoleAnalysisObjectCategorizationType.ANOMALY) {
                    anomalyCount++;
                } else if (category == RoleAnalysisObjectCategorizationType.ANOMALY_EXCLUSIVE) {
                    anomalyExclusiveCount++;
                } else if (category == RoleAnalysisObjectCategorizationType.OUTLIER) {
                    outlierCount++;
                }
            }
        }

        items.setUnPopularCount(unpopularCount);
        items.setAbovePopularCount(abovePopularCount);
        items.setNoiseCount(noiseCount);
        items.setNoiseExclusiveCount(noiseExclusiveCount);
        items.setAnomalyCount(anomalyCount);
        items.setAnomalyExclusiveCount(anomalyExclusiveCount);
        items.setOutlierCount(outlierCount);
    }
}
