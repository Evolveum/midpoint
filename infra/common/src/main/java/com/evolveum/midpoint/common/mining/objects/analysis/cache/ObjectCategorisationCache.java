package com.evolveum.midpoint.common.mining.objects.analysis.cache;

import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
     * @param sessionObject The RoleAnalysisSessionType containing session data.
     * @return The RoleAnalysisIdentifiedCharacteristicsType container with categorized items.
     */
    public RoleAnalysisIdentifiedCharacteristicsType build(RoleAnalysisSessionType sessionObject) {
        RoleAnalysisIdentifiedCharacteristicsType container = new RoleAnalysisIdentifiedCharacteristicsType();

        markExcludedObjects(sessionObject);
        addInsufficientCategory(sessionObject.getAnalysisOption());

        processCategoryMap(rolesCategoryMap, container::setRoles, container::setRolesCount);
        processCategoryMap(usersCategoryMap, container::setUsers, container::setUsersCount);
        processCategoryMap(focusCategoryMap, container::setFocus, container::setFocusCount);

        RoleAnalysisIdentifiedCharacteristicsType identifiedCharacteristics = sessionObject.getIdentifiedCharacteristics();
        if (identifiedCharacteristics != null && identifiedCharacteristics.getExclude() != null) {
            container.setExclude(identifiedCharacteristics.getExclude());
        }

        return container;
    }

    /**
     * Processes a category map, computes categories, and sets the corresponding items and count.
     *
     * @param categoryMap The map of categories to process.
     * @param setItems A method reference to set the items in the container.
     * @param setCount A method reference to set the count in the container.
     */
    private void processCategoryMap(
            @NotNull Map<?, RoleAnalysisIdentifiedCharacteristicsItemType> categoryMap,
            Consumer<RoleAnalysisIdentifiedCharacteristicsItemsType> setItems,
            Consumer<Integer> setCount) {

        if (!categoryMap.values().isEmpty()) {
            RoleAnalysisIdentifiedCharacteristicsItemsType items = new RoleAnalysisIdentifiedCharacteristicsItemsType();
            items.getItem().addAll(CloneUtil.cloneCollectionMembers(categoryMap.values()));
            computeCategories(items);
            setItems.accept(items);
            setCount.accept(categoryMap.values().size());
        }
    }




    /**
     * Marks objects as excluded based on the analysis options and manually unwanted objects.
     *
     * @param sessionObject The session object containing the analysis options and identified characteristics.
     */
    public void markExcludedObjects(@NotNull RoleAnalysisSessionType sessionObject) {
        RoleAnalysisOptionType analysisOption = sessionObject.getAnalysisOption();
        assert analysisOption != null;
        RoleAnalysisProcessModeType processMode = analysisOption.getProcessMode();

        RoleAnalysisIdentifiedCharacteristicsType identifiedCharacteristics = sessionObject.getIdentifiedCharacteristics();
        Set<String> manuallyUnwantedAccess = new HashSet<>();
        Set<String> manuallyUnwantedUsers = new HashSet<>();

        if (identifiedCharacteristics != null) {
            loadManuallyUnwantedObjects(identifiedCharacteristics, manuallyUnwantedAccess, manuallyUnwantedUsers);
        }

        manuallyUnwantedAccess.forEach(oid -> putCategory(oid, RoleAnalysisObjectCategorizationType.EXCLUDED, RoleType.COMPLEX_TYPE));
        manuallyUnwantedUsers.forEach(oid -> putCategory(oid, RoleAnalysisObjectCategorizationType.EXCLUDED, UserType.COMPLEX_TYPE));

        for (RoleAnalysisIdentifiedCharacteristicsItemType role : rolesCategoryMap.values()) {
            List<RoleAnalysisObjectCategorizationType> category = role.getCategory();
            if (processMode.equals(RoleAnalysisProcessModeType.USER)
                    && category.contains(RoleAnalysisObjectCategorizationType.UN_POPULAR)) {
                role.getCategory().add(RoleAnalysisObjectCategorizationType.EXCLUDED);
            }
        }

        for (RoleAnalysisIdentifiedCharacteristicsItemType user : usersCategoryMap.values()) {
            List<RoleAnalysisObjectCategorizationType> category = user.getCategory();
            if (processMode.equals(RoleAnalysisProcessModeType.ROLE)
                    && category.contains(RoleAnalysisObjectCategorizationType.UN_POPULAR)) {
                user.getCategory().add(RoleAnalysisObjectCategorizationType.EXCLUDED);
            }
        }

    }

    public void addInsufficientCategory(@NotNull RoleAnalysisOptionType analysisOption) {
        RoleAnalysisProcessModeType processMode = analysisOption.getProcessMode();

        Map<?, RoleAnalysisIdentifiedCharacteristicsItemType> targetMap =
                (processMode.equals(RoleAnalysisProcessModeType.USER)) ? usersCategoryMap : rolesCategoryMap;

        for (RoleAnalysisIdentifiedCharacteristicsItemType item : targetMap.values()) {
            updateCategoryIfNecessary(item);
        }
    }

    private void updateCategoryIfNecessary(@NotNull RoleAnalysisIdentifiedCharacteristicsItemType item) {
        List<RoleAnalysisObjectCategorizationType> categories = item.getCategory();
        if (categories.contains(RoleAnalysisObjectCategorizationType.NOISE_EXCLUSIVE)
                && !categories.contains(RoleAnalysisObjectCategorizationType.UN_POPULAR)) {

            categories.add(RoleAnalysisObjectCategorizationType.INSUFFICIENT);
        }else if (categories.contains(RoleAnalysisObjectCategorizationType.UN_POPULAR)
                && categories.contains(RoleAnalysisObjectCategorizationType.NOISE_EXCLUSIVE)) {
            categories.add(RoleAnalysisObjectCategorizationType.NOISE_EXCLUSIVE_UNPOPULAR);
        }

    }

    /**
     * Loads manually unwanted objects into the provided sets for excluded access and users.
     * Object can be excluded using category mark or directly by corresponding object reference list.
     *
     * @param identifiedCharacteristics The identified characteristics containing the exclude information.
     * @param manuallyUnwantedAccess The set to store manually unwanted access OIDs.
     * @param manuallyUnwantedUsers The set to store manually unwanted user OIDs.
     */
    private static void loadManuallyUnwantedObjects(
            @NotNull RoleAnalysisIdentifiedCharacteristicsType identifiedCharacteristics,
            Set<String> manuallyUnwantedAccess,
            Set<String> manuallyUnwantedUsers) {
        RoleAnalysisExcludeType excludeObject = identifiedCharacteristics.getExclude();
        if (excludeObject != null) {

            List<String> excludeRoleRef = excludeObject.getExcludeRoleRef();
            if (excludeRoleRef != null) {
                manuallyUnwantedAccess.addAll(excludeRoleRef);
            }

            List<RoleAnalysisObjectCategorizationType> excludeRoleCategory = excludeObject.getExcludeRoleCategory();
            if (excludeRoleCategory != null) {
                RoleAnalysisIdentifiedCharacteristicsItemsType roles = identifiedCharacteristics.getRoles();
                loadUnwantedCategoryItems(manuallyUnwantedAccess, excludeRoleCategory, roles);
            }

            List<String> excludeUserRef = excludeObject.getExcludeUserRef();
            if (excludeUserRef != null) {
                manuallyUnwantedUsers.addAll(excludeUserRef);
            }

            List<RoleAnalysisObjectCategorizationType> excludeUserCategory = excludeObject.getExcludeUserCategory();
            if (excludeUserCategory != null) {
                RoleAnalysisIdentifiedCharacteristicsItemsType users = identifiedCharacteristics.getUsers();
                loadUnwantedCategoryItems(manuallyUnwantedUsers, excludeUserCategory, users);
            }
        }
    }

    /**
     * Computes the counts of various categories within the given RoleAnalysisIdentifiedCharacteristicsItemsType items.
     *
     * @param items The items containing the categories to be counted.
     */
    private void computeCategories(@NotNull RoleAnalysisIdentifiedCharacteristicsItemsType items) {
        Map<RoleAnalysisObjectCategorizationType, Integer> categoryCounts = new EnumMap<>(RoleAnalysisObjectCategorizationType.class);

        for (RoleAnalysisObjectCategorizationType category : RoleAnalysisObjectCategorizationType.values()) {
            categoryCounts.put(category, 0);
        }

        List<RoleAnalysisIdentifiedCharacteristicsItemType> itemList = items.getItem();
        for (RoleAnalysisIdentifiedCharacteristicsItemType value : itemList) {
            for (RoleAnalysisObjectCategorizationType category : value.getCategory()) {
                categoryCounts.put(category, categoryCounts.get(category) + 1);
            }
        }

        items.setUnPopularCount(categoryCounts.getOrDefault(RoleAnalysisObjectCategorizationType.UN_POPULAR, 0));
        items.setAbovePopularCount(categoryCounts.getOrDefault(RoleAnalysisObjectCategorizationType.ABOVE_POPULAR, 0));
        items.setNoiseCount(categoryCounts.getOrDefault(RoleAnalysisObjectCategorizationType.NOISE, 0));
        items.setNoiseExclusiveCount(categoryCounts.getOrDefault(RoleAnalysisObjectCategorizationType.NOISE_EXCLUSIVE, 0));
        items.setAnomalyCount(categoryCounts.getOrDefault(RoleAnalysisObjectCategorizationType.ANOMALY, 0));
        items.setOverallAnomalyCount(categoryCounts.getOrDefault(RoleAnalysisObjectCategorizationType.OVERALL_ANOMALY, 0));
        items.setOutlierCount(categoryCounts.getOrDefault(RoleAnalysisObjectCategorizationType.OUTLIER, 0));
        items.setExcludedCount(categoryCounts.getOrDefault(RoleAnalysisObjectCategorizationType.EXCLUDED, 0));
        items.setInsufficientCount(categoryCounts.getOrDefault(RoleAnalysisObjectCategorizationType.INSUFFICIENT, 0));
        items.setNoiseExclusiveUnpopular(categoryCounts.getOrDefault(RoleAnalysisObjectCategorizationType.NOISE_EXCLUSIVE_UNPOPULAR, 0));
    }

    public Set<String> getUnpopularRoles() {
        return rolesCategoryMap.entrySet().stream()
                .filter(entry -> entry.getValue().getCategory().contains(RoleAnalysisObjectCategorizationType.UN_POPULAR))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    public Set<String> getUnpopularUsers() {
        return usersCategoryMap.entrySet().stream()
                .filter(entry -> entry.getValue().getCategory().contains(RoleAnalysisObjectCategorizationType.UN_POPULAR))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    public RoleAnalysisIdentifiedCharacteristicsType updateUnPopularityIdentifiedChar(@NotNull RoleAnalysisSessionType
            session) {
        Set<String> unpopularRoles = getUnpopularRoles();
        Set<String> unpopularUsers = getUnpopularUsers();

        RoleAnalysisIdentifiedCharacteristicsType identifiedCharacteristics = session.getIdentifiedCharacteristics();

        boolean userDone = false;
        boolean roleDone = false;
        if (identifiedCharacteristics == null) {
            identifiedCharacteristics = new RoleAnalysisIdentifiedCharacteristicsType();
        }

        if (identifiedCharacteristics.getRoles() == null) {
            identifiedCharacteristics.setRoles(new RoleAnalysisIdentifiedCharacteristicsItemsType());
            identifiedCharacteristics.getRoles().getItem().addAll(rolesCategoryMap.values());
            roleDone = true;
        }

        if (identifiedCharacteristics.getUsers() == null) {
            identifiedCharacteristics.setUsers(new RoleAnalysisIdentifiedCharacteristicsItemsType());
            identifiedCharacteristics.getUsers().getItem().addAll(usersCategoryMap.values());
            userDone = true;
        }

        if (identifiedCharacteristics.getRoles() != null && !roleDone) {
            List<RoleAnalysisIdentifiedCharacteristicsItemType> roleItems = identifiedCharacteristics.getRoles().getItem();
            for (RoleAnalysisIdentifiedCharacteristicsItemType role : roleItems) {
                if (unpopularRoles.contains(role.getObjectRef().getOid())) {
                    role.getCategory().add(RoleAnalysisObjectCategorizationType.UN_POPULAR);
                    unpopularRoles.remove(role.getObjectRef().getOid());
                }
            }

            if (!unpopularRoles.isEmpty()) {
                RoleAnalysisIdentifiedCharacteristicsItemsType roles = identifiedCharacteristics.getRoles();
                for (String oid : unpopularRoles) {
                    RoleAnalysisIdentifiedCharacteristicsItemType role = new RoleAnalysisIdentifiedCharacteristicsItemType();
                    role.setObjectRef(createObjectReference(oid, RoleType.COMPLEX_TYPE));
                    role.getCategory().add(RoleAnalysisObjectCategorizationType.UN_POPULAR);
                    roles.getItem().add(role);
                }
            }
        }

        if (identifiedCharacteristics.getUsers() != null && !userDone) {
            List<RoleAnalysisIdentifiedCharacteristicsItemType> userItems = identifiedCharacteristics.getUsers().getItem();
            for (RoleAnalysisIdentifiedCharacteristicsItemType user : userItems) {
                if (unpopularUsers.contains(user.getObjectRef().getOid())) {
                    user.getCategory().add(RoleAnalysisObjectCategorizationType.UN_POPULAR);
                    unpopularUsers.remove(user.getObjectRef().getOid());
                }
            }

            if (!unpopularUsers.isEmpty()) {
                RoleAnalysisIdentifiedCharacteristicsItemsType users = identifiedCharacteristics.getUsers();
                for (String oid : unpopularUsers) {
                    RoleAnalysisIdentifiedCharacteristicsItemType user = new RoleAnalysisIdentifiedCharacteristicsItemType();
                    user.setObjectRef(createObjectReference(oid, UserType.COMPLEX_TYPE));
                    user.getCategory().add(RoleAnalysisObjectCategorizationType.UN_POPULAR);
                    users.getItem().add(user);
                }
            }
        }

        return identifiedCharacteristics;
    }

    private static void loadUnwantedCategoryItems(
            Set<String> unwantedAccess,
            List<RoleAnalysisObjectCategorizationType> excludeRoleCategory,
            RoleAnalysisIdentifiedCharacteristicsItemsType items) {
        if (items != null) {
            items.getItem().forEach(role -> {
                List<RoleAnalysisObjectCategorizationType> category = role.getCategory();
                if (category != null) {
                    for (RoleAnalysisObjectCategorizationType roleCategory : category) {
                        if (excludeRoleCategory.contains(roleCategory)) {
                            unwantedAccess.add(role.getObjectRef().getOid());
                            break;
                        }
                    }
                }
            });

        }
    }
}
