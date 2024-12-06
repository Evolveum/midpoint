package com.evolveum.midpoint.model.impl.mining;

import com.evolveum.midpoint.common.mining.objects.analysis.cache.AttributeAnalysisCache;
import com.evolveum.midpoint.common.mining.objects.analysis.cache.ObjectCategorisationCache;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility class for analysis data preparation service.
 */
public class RoleAnalysisDataServiceUtils {

    public RoleAnalysisDataServiceUtils() {
        //empty constructor
    }

    private static boolean isExcludeRoleOn(@NotNull RoleAnalysisSessionType session) {
        RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
        if (analysisOption == null || analysisOption.getProcessMode() == null) {
            return false;
        }
        RoleAnalysisProcessModeType processMode = analysisOption.getProcessMode();
        return processMode.equals(RoleAnalysisProcessModeType.USER);
    }

    private static boolean isExcludeUserOn(@NotNull RoleAnalysisSessionType session) {
        RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
        if (analysisOption == null || analysisOption.getProcessMode() == null) {
            return false;
        }
        RoleAnalysisProcessModeType processMode = analysisOption.getProcessMode();
        return processMode.equals(RoleAnalysisProcessModeType.ROLE);
    }

    /**
     * Retrieves the set of manually unwanted access OIDs based on the session's identified characteristics.
     *
     * @param roleAnalysisService The service used to retrieve the session object.
     * @param sessionOid The OID of the session.
     * @param task The task in which the operation is performed.
     * @param result The operation result.
     * @return A set of manually unwanted access OIDs.
     */
    protected static @NotNull Set<String> getManuallyUnwantedAccesses(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull String sessionOid,
            @NotNull Task task,
            @NotNull OperationResult result) {

        PrismObject<RoleAnalysisSessionType> sessionPrismObject = roleAnalysisService.getSessionTypeObject(
                sessionOid, task, result);
        if (sessionPrismObject == null) {
            return new HashSet<>();
        }

        RoleAnalysisSessionType sessionObject = sessionPrismObject.asObjectable();
        RoleAnalysisIdentifiedCharacteristicsType identifiedCharacteristics = sessionObject.getIdentifiedCharacteristics();
        Set<String> unwantedAccess = new HashSet<>();

        RoleAnalysisExcludeType excludeObject = identifiedCharacteristics.getExclude();
        if (excludeObject != null) {

            List<String> excludeRoleRef = excludeObject.getExcludeRoleRef();
            if (excludeRoleRef != null) {
                unwantedAccess.addAll(excludeRoleRef);
            }

            List<RoleAnalysisObjectCategorizationType> excludeRoleCategory = excludeObject.getExcludeRoleCategory();
            if (excludeRoleCategory != null) {
                RoleAnalysisIdentifiedCharacteristicsItemsType roles = identifiedCharacteristics.getRoles();
                loadUnwantedCategoryItems(unwantedAccess, excludeRoleCategory, roles);
            }

        }

        boolean excludeRoles = isExcludeRoleOn(sessionObject);
        if (excludeRoles) {
            RoleAnalysisIdentifiedCharacteristicsItemsType roles = identifiedCharacteristics.getRoles();
            loadUnwantedUnpopularItems(unwantedAccess, roles);
        }

        return unwantedAccess;
    }

    /**
     * Retrieves the set of manually unwanted user OIDs based on the session's identified characteristics.
     *
     * @param roleAnalysisService The service used to retrieve the session object.
     * @param sessionOid The OID of the session.
     * @param task The task in which the operation is performed.
     * @param result The operation result.
     * @return A set of manually unwanted access OIDs.
     */
    protected static @NotNull Set<String> getManuallyUnwantedUsers(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull String sessionOid,
            @NotNull Task task,
            @NotNull OperationResult result) {
        PrismObject<RoleAnalysisSessionType> sessionPrismObject = roleAnalysisService.getSessionTypeObject(
                sessionOid, task, result);
        if (sessionPrismObject == null) {
            return new HashSet<>();
        }

        RoleAnalysisSessionType sessionObject = sessionPrismObject.asObjectable();
        if (sessionObject.getIdentifiedCharacteristics() == null) {
            return new HashSet<>();
        }

        RoleAnalysisIdentifiedCharacteristicsType identifiedCharacteristics = sessionObject.getIdentifiedCharacteristics();
        Set<String> unwantedUsers = new HashSet<>();

        RoleAnalysisExcludeType excludeObject = identifiedCharacteristics.getExclude();
        if (excludeObject != null) {

            List<String> excludeUserRef = excludeObject.getExcludeUserRef();
            if (excludeUserRef != null) {
                unwantedUsers.addAll(excludeUserRef);
            }

            List<RoleAnalysisObjectCategorizationType> excludeUserCategory = excludeObject.getExcludeUserCategory();
            if (excludeUserCategory != null) {
                RoleAnalysisIdentifiedCharacteristicsItemsType users = identifiedCharacteristics.getUsers();
                loadUnwantedCategoryItems(unwantedUsers, excludeUserCategory, users);
            }

        }

        boolean excludeUsers = isExcludeUserOn(sessionObject);
        if (excludeUsers) {
            RoleAnalysisIdentifiedCharacteristicsItemsType users = identifiedCharacteristics.getUsers();
            loadUnwantedUnpopularItems(unwantedUsers, users);
        }

        return unwantedUsers;
    }

    private static void loadUnwantedUnpopularItems(
            Set<String> unwantedAccess,
            RoleAnalysisIdentifiedCharacteristicsItemsType roles) {
        if (roles != null) {
            roles.getItem().forEach(role -> {
                List<RoleAnalysisObjectCategorizationType> category = role.getCategory();
                if (category != null && category.contains(RoleAnalysisObjectCategorizationType.UN_POPULAR)) {
                    unwantedAccess.add(role.getObjectRef().getOid());
                }
            });
        }
    }

    private static void loadUnwantedCategoryItems(
            Set<String> unwantedAccess,
            List<RoleAnalysisObjectCategorizationType> excludeRoleCategory,
            RoleAnalysisIdentifiedCharacteristicsItemsType roles) {
        if (roles != null) {
            roles.getItem().forEach(role -> {
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

    /**
     * Retrieves the set of manually unwanted access OIDs based on the session's identified characteristics.
     * Part of loading the analysis data (assignments or roleMembershipRefs).
     */
    protected static @NotNull ListMultimap<String, String> prepareAnalysisData(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisSessionType sessionObject,
            boolean updateStatistics,
            @Nullable AttributeAnalysisCache attributeAnalysisCache,
            @NotNull ObjectCategorisationCache objectCategorisationCache,
            @NotNull RoleAnalysisProcessModeType processMode,
            @NotNull ListMultimap<String, String> roleMembersMap,
            @NotNull Task task,
            @NotNull OperationResult result) {

        ListMultimap<String, String> userRolesMap = reverseMap(roleMembersMap);

        if (updateStatistics) {
            RoleAnalysisSessionStatisticType sessionStatistic = sessionObject.getSessionStatistic();
            if (sessionStatistic == null) {
                sessionStatistic = new RoleAnalysisSessionStatisticType();
                sessionObject.setSessionStatistic(sessionStatistic);
            }

            sessionStatistic.setProcessedAssignmentCount(roleMembersMap.keys().size());

            if (processMode == RoleAnalysisProcessModeType.USER) {
                sessionStatistic.setProcessedPropertiesCount(roleMembersMap.keySet().size());
            } else if (processMode == RoleAnalysisProcessModeType.ROLE) {
                sessionStatistic.setProcessedPropertiesCount(userRolesMap.keySet().size());
            }

            loadSessionPopularityAccessCategorization(objectCategorisationCache, sessionObject, roleMembersMap);
            loadSessionPopularityUserCategorization(objectCategorisationCache, sessionObject, userRolesMap);
            //TODO test
            RoleAnalysisIdentifiedCharacteristicsType characteristicsContainer = objectCategorisationCache
                    .updateUnPopularityIdentifiedChar(sessionObject);
            roleAnalysisService.updateSessionIdentifiedCharacteristics(sessionObject, characteristicsContainer, task, result);
            roleAnalysisService
                    .updateSessionStatistics(sessionObject, sessionStatistic, task, result);
        }

        if (processMode == RoleAnalysisProcessModeType.USER) {
            //exclude roles
            Set<String> unpopularRoles = getManuallyUnwantedAccesses(roleAnalysisService, sessionObject.getOid(), task, result);
            if (!unpopularRoles.isEmpty()) {
                if (updateStatistics) {
                    objectCategorisationCache.putAllCategory(
                            unpopularRoles, RoleAnalysisObjectCategorizationType.EXCLUDED, RoleType.COMPLEX_TYPE);
                }
                roleMembersMap.keySet().removeAll(unpopularRoles);
            }

            userRolesMap.clear();
            userRolesMap = reverseMap(roleMembersMap);

            if (updateStatistics && attributeAnalysisCache != null) {
                attributeAnalysisCache.setRoleMemberCache(roleMembersMap);
                attributeAnalysisCache.setUserMemberCache(userRolesMap);
            }
            return userRolesMap;
        } else {
            //exclude users
            Set<String> unpopularUsers = getManuallyUnwantedUsers(roleAnalysisService, sessionObject.getOid(), task, result);
            if (!unpopularUsers.isEmpty()) {
                if (updateStatistics) {
                    objectCategorisationCache.putAllCategory(
                            unpopularUsers, RoleAnalysisObjectCategorizationType.EXCLUDED, UserType.COMPLEX_TYPE);
                }
                userRolesMap.keySet().removeAll(unpopularUsers);
            }

            roleMembersMap.clear();
            roleMembersMap = reverseMap(userRolesMap);

            if (updateStatistics && attributeAnalysisCache != null) {
                attributeAnalysisCache.setRoleMemberCache(roleMembersMap);
                attributeAnalysisCache.setUserMemberCache(userRolesMap);
            }
            return roleMembersMap;
        }
    }

    public static @NotNull ListMultimap<String, String> reverseMap(@NotNull ListMultimap<String, String> roleMemberCache) {
        ListMultimap<String, String> revertMap = ArrayListMultimap.create();
        for (Map.Entry<String, String> entry : roleMemberCache.entries()) {
            String key = entry.getKey();
            String value = entry.getValue();
            revertMap.put(value, key);
        }
        return revertMap;
    }

    private static void loadSessionPopularityUserCategorization(
            @NotNull ObjectCategorisationCache objectCategorisationCache,
            @NotNull RoleAnalysisSessionType sessionObject,
            @NotNull ListMultimap<String, String> userRolesMap) {
        Integer minUserPopularity = getSessionMinUserPopularityOptionsValue(sessionObject);
        Integer maxUserPopularity = getSessionMaxUserPopularityOptionsValue(sessionObject);

        if (minUserPopularity != null) {

            Set<String> nonPopularUsersMin = new HashSet<>();
            Set<String> nonPopularUsersMax = new HashSet<>();
            userRolesMap.keys().forEach(userOid -> {
                int size = userRolesMap.get(userOid).size();
                if (maxUserPopularity != null && maxUserPopularity > 0 && size > maxUserPopularity) {
                    nonPopularUsersMax.add(userOid);
                }

                if (size < minUserPopularity) {
                    nonPopularUsersMin.add(userOid);
                }

            });

            for (String userOid : nonPopularUsersMin) {
                objectCategorisationCache
                        .putCategory(userOid, RoleAnalysisObjectCategorizationType.UN_POPULAR, UserType.COMPLEX_TYPE);
            }

            for (String userOid : nonPopularUsersMax) {
                objectCategorisationCache
                        .putCategory(userOid, RoleAnalysisObjectCategorizationType.ABOVE_POPULAR, UserType.COMPLEX_TYPE);
            }
        }
    }

    private static void loadSessionPopularityAccessCategorization(
            @NotNull ObjectCategorisationCache objectCategorisationCache,
            @NotNull RoleAnalysisSessionType sessionObject,
            @NotNull ListMultimap<String, String> roleMembersMap) {

        Integer minAccessPopularity = getSessionAccessPopularityOptionsValue(sessionObject);
        Integer maxAccessPopularity = getSessionMaxAccessPopularityOptionsValue(sessionObject);

        if (minAccessPopularity != null) {
            Set<String> nonPopularRolesMin = new HashSet<>();
            Set<String> nonPopularRolesMax = new HashSet<>();
            for (String role : roleMembersMap.keySet()) {
                if (roleMembersMap.get(role).size() < minAccessPopularity) {
                    nonPopularRolesMin.add(role);
                }

                if (maxAccessPopularity != null && roleMembersMap.get(role).size() > maxAccessPopularity) {
                    nonPopularRolesMax.add(role);
                }
            }

            for (String roleOid : nonPopularRolesMin) {
                objectCategorisationCache
                        .putCategory(roleOid, RoleAnalysisObjectCategorizationType.UN_POPULAR, RoleType.COMPLEX_TYPE);
            }

            for (String roleOid : nonPopularRolesMax) {
                objectCategorisationCache
                        .putCategory(roleOid, RoleAnalysisObjectCategorizationType.ABOVE_POPULAR, RoleType.COMPLEX_TYPE);
            }
        }
    }

    private static @Nullable Integer getSessionMinUserPopularityOptionsValue(@Nullable RoleAnalysisSessionType session) {
        if (session == null) {
            return null;
        }

        RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
        if (analysisOption == null) {
            return null;
        }

        RoleAnalysisProcessModeType processMode = analysisOption.getProcessMode();

        if (processMode == null) {
            return null;
        }
        AbstractAnalysisSessionOptionType sessionOptions = null;
        if (processMode == RoleAnalysisProcessModeType.ROLE) {
            sessionOptions = session.getRoleModeOptions();
        } else if (processMode == RoleAnalysisProcessModeType.USER) {
            sessionOptions = session.getUserModeOptions();
        }

        if (sessionOptions == null) {
            return null;
        }

        if (sessionOptions.getMinUsersPopularity() != null) {
            return sessionOptions.getMinUsersPopularity();
        } else if (processMode == RoleAnalysisProcessModeType.USER && sessionOptions.getMinPropertiesOverlap() != null) {
            return sessionOptions.getMinPropertiesOverlap();
        }

        return sessionOptions.getMinUsersPopularity();
    }

    private static @Nullable Integer getSessionMaxUserPopularityOptionsValue(@Nullable RoleAnalysisSessionType session) {
        if (session == null) {
            return null;
        }

        RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
        if (analysisOption == null) {
            return null;
        }

        RoleAnalysisProcessModeType processMode = analysisOption.getProcessMode();

        if (processMode == null) {
            return null;
        }
        AbstractAnalysisSessionOptionType sessionOptions = null;
        if (processMode == RoleAnalysisProcessModeType.ROLE) {
            sessionOptions = session.getRoleModeOptions();
        } else if (processMode == RoleAnalysisProcessModeType.USER) {
            sessionOptions = session.getUserModeOptions();
        }

        if (sessionOptions == null || sessionOptions.getMaxUsersPopularity() == null) {
            return null;
        }

        return sessionOptions.getMaxUsersPopularity();
    }

    protected static @Nullable Integer getSessionAccessPopularityOptionsValue(@Nullable RoleAnalysisSessionType session) {
        if (session == null) {
            return null;
        }

        RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
        if (analysisOption == null) {
            return null;
        }

        RoleAnalysisProcessModeType processMode = analysisOption.getProcessMode();

        if (processMode == null) {
            return null;
        }
        AbstractAnalysisSessionOptionType sessionOptions = null;
        if (processMode == RoleAnalysisProcessModeType.ROLE) {
            sessionOptions = session.getRoleModeOptions();
        } else if (processMode == RoleAnalysisProcessModeType.USER) {
            sessionOptions = session.getUserModeOptions();
        }

        if (sessionOptions == null) {
            return null;
        }

        if (sessionOptions.getMinAccessPopularity() != null) {
            return sessionOptions.getMinAccessPopularity();
        } else if (processMode == RoleAnalysisProcessModeType.ROLE && sessionOptions.getMinPropertiesOverlap() != null) {
            return sessionOptions.getMinPropertiesOverlap();
        }

        return sessionOptions.getMinAccessPopularity();
    }

    protected static @Nullable Integer getSessionMaxAccessPopularityOptionsValue(@Nullable RoleAnalysisSessionType session) {
        if (session == null) {
            return null;
        }

        RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
        if (analysisOption == null) {
            return null;
        }

        RoleAnalysisProcessModeType processMode = analysisOption.getProcessMode();

        if (processMode == null) {
            return null;
        }

        AbstractAnalysisSessionOptionType sessionOptions = null;
        if (processMode == RoleAnalysisProcessModeType.ROLE) {
            sessionOptions = session.getRoleModeOptions();
        } else if (processMode == RoleAnalysisProcessModeType.USER) {
            sessionOptions = session.getUserModeOptions();
        }

        if (sessionOptions == null || sessionOptions.getMaxAccessPopularity() == null) {
            return null;
        }

        return sessionOptions.getMinAccessPopularity();
    }
}
