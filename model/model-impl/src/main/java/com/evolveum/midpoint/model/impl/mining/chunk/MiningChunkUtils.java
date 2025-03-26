package com.evolveum.midpoint.model.impl.mining.chunk;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.common.mining.objects.chunk.*;
import com.evolveum.midpoint.common.mining.utils.values.FrequencyItem;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisChunkMode;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisObjectStatus;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisOperationMode;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.model.impl.mining.RoleAnalysisServiceImpl;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcessModeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import com.google.common.collect.ListMultimap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.getRolesOidAssignment;

import static java.util.Collections.singleton;

/**
 * Utility class for processing and managing mining chunks in role analysis.
 * <p>
 * This class provides various methods for handling role and user chunks, including:
 * <ul>
 *     <li>Resolving table pattern chunks based on detected patterns.</li>
 *     <li>Expanding chunks based on role and user patterns.</li>
 *     <li>Processing role and user chunk patterns.</li>
 *     <li>Including missing objects into chunks.</li>
 * </ul>
 * <p>
 * It interacts with the {@link RoleAnalysisService} to retrieve role and user data
 * and applies analysis modes such as {@link RoleAnalysisChunkMode} to determine how chunks should be processed.
 * The class also includes helper methods for chunk naming and handling object statuses.
 * </p>
 */
public class MiningChunkUtils {

    public static void resolveTablePatternChunk(
            RoleAnalysisServiceImpl roleAnalysisService,
            RoleAnalysisProcessModeType processMode,
            MiningOperationChunk basicChunk,
            List<List<String>> detectedPatternsRoles,
            List<String> candidateRolesIds,
            List<List<String>> detectedPatternsUsers,
            @NotNull Task task, @NotNull OperationResult result) {

        if (processMode == RoleAnalysisProcessModeType.ROLE) {
            resolveRoleModeChunkPattern(roleAnalysisService,
                    basicChunk,
                    detectedPatternsRoles,
                    candidateRolesIds,
                    detectedPatternsUsers,
                    task, result);
        } else {
            resolveUserModeChunkPattern(roleAnalysisService,
                    basicChunk,
                    detectedPatternsRoles,
                    candidateRolesIds,
                    detectedPatternsUsers,
                    task, result);
        }
    }

    private static void resolveUserModeChunkPattern(
            @NotNull RoleAnalysisServiceImpl roleAnalysisService,
            @NotNull MiningOperationChunk basicChunk,
            List<List<String>> detectedPatternsRoles,
            List<String> candidateRolesIds,
            List<List<String>> detectedPatternsUsers,
            @NotNull Task task, @NotNull OperationResult result) {

        List<MiningRoleTypeChunk> miningRoleTypeChunks = basicChunk.getMiningRoleTypeChunks();
        List<MiningUserTypeChunk> miningUserTypeChunks = basicChunk.getMiningUserTypeChunks();

        DisplayValueOption displayValueOption = basicChunk.getDisplayValueOption();

        expandChunk(roleAnalysisService,
                true,
                miningRoleTypeChunks,
                detectedPatternsRoles,
                displayValueOption,
                task,
                result);

        for (MiningRoleTypeChunk role : miningRoleTypeChunks) {
            FrequencyItem frequencyItem = role.getFrequencyItem();
            double frequency = frequencyItem.getFrequency();

            for (int i = 0; i < detectedPatternsRoles.size(); i++) {
                List<String> detectedPatternsRole = detectedPatternsRoles.get(i);
                List<String> chunkRoles = role.getRoles();
                resolvePatternChunk(basicChunk, candidateRolesIds, role, detectedPatternsRole, chunkRoles, i, frequency);
            }
        }

        expandChunk(roleAnalysisService,
                false,
                miningUserTypeChunks,
                detectedPatternsUsers,
                displayValueOption,
                task,
                result);

        for (MiningUserTypeChunk user : miningUserTypeChunks) {
            for (int i = 0; i < detectedPatternsUsers.size(); i++) {
                List<String> detectedPatternsUser = detectedPatternsUsers.get(i);
                List<String> chunkUsers = user.getUsers();
                resolveMemberPatternChunk(candidateRolesIds, user, detectedPatternsUser, chunkUsers, i);
            }
        }
    }

    private static void resolveRoleModeChunkPattern(
            @NotNull RoleAnalysisServiceImpl roleAnalysisService,
            @NotNull MiningOperationChunk basicChunk,
            List<List<String>> detectedPatternsRoles,
            List<String> candidateRolesIds,
            List<List<String>> detectedPatternsUsers,
            @NotNull Task task, @NotNull OperationResult result) {

        List<MiningRoleTypeChunk> miningRoleTypeChunks = basicChunk.getMiningRoleTypeChunks();
        List<MiningUserTypeChunk> miningUserTypeChunks = basicChunk.getMiningUserTypeChunks();

        DisplayValueOption displayValueOption = basicChunk.getDisplayValueOption();

        expandChunk(roleAnalysisService,
                false,
                miningUserTypeChunks,
                detectedPatternsUsers,
                displayValueOption,
                task,
                result);

        for (MiningUserTypeChunk user : miningUserTypeChunks) {
            FrequencyItem frequencyItem = user.getFrequencyItem();
            double frequency = frequencyItem.getFrequency();

            for (int i = 0; i < detectedPatternsUsers.size(); i++) {
                List<String> detectedPatternsUser = detectedPatternsUsers.get(i);
                List<String> chunkUsers = user.getUsers();
                resolvePatternChunk(basicChunk, candidateRolesIds, user, detectedPatternsUser, chunkUsers, i, frequency);
            }
        }

        expandChunk(roleAnalysisService,
                true,
                miningRoleTypeChunks,
                detectedPatternsRoles,
                displayValueOption,
                task,
                result);

        for (MiningRoleTypeChunk role : miningRoleTypeChunks) {
            for (int i = 0; i < detectedPatternsRoles.size(); i++) {
                List<String> detectedPatternsRole = detectedPatternsRoles.get(i);
                List<String> chunkRoles = role.getRoles();
                resolveMemberPatternChunk(candidateRolesIds, role, detectedPatternsRole, chunkRoles, i);
            }
        }
    }

    /**
     * Expands the given list of mining member chunks based on detected patterns.
     * This method iterates through the mining member chunks and expands them if they match the detected patterns.
     */
    private static <M extends MiningBaseTypeChunk> void expandChunk(
            @NotNull RoleAnalysisServiceImpl roleAnalysisService,
            boolean isRoleTypeChunk,
            @NotNull List<M> miningMemberChunk,
            List<List<String>> detectedPatternsObjectsOids,
            @NotNull DisplayValueOption displayValueOption,
            @NotNull Task task,
            @NotNull OperationResult result) {

        RoleAnalysisChunkMode chunkMode = displayValueOption.getChunkMode();
        if (isExpandUseful(isRoleTypeChunk, detectedPatternsObjectsOids, chunkMode)) {
            return;
        }

        List<M> expandedChunksVersion = new ArrayList<>();
        for (M chunk : new ArrayList<>(miningMemberChunk)) {
            Set<String> chunkMembers = new HashSet<>(chunk.getMembers());
            if (chunkMembers.size() <= 1) {
                expandedChunksVersion.add(chunk);
                continue;
            }

            processExpandingProcess(roleAnalysisService,
                    detectedPatternsObjectsOids,
                    displayValueOption,
                    chunk,
                    chunkMembers,
                    expandedChunksVersion,
                    task,
                    result);
        }

        miningMemberChunk.clear();
        miningMemberChunk.addAll(expandedChunksVersion);
    }

    private static <M extends MiningBaseTypeChunk> void processExpandingProcess(
            @NotNull RoleAnalysisServiceImpl roleAnalysisService,
            @NotNull List<List<String>> detectedPatternsObjectsOids,
            @NotNull DisplayValueOption displayValueOption,
            M chunk, Set<String> chunkMembers, List<M> expandedChunksVersion,
            @NotNull Task task, @NotNull OperationResult result) {

        for (List<String> detectedPatternOids : detectedPatternsObjectsOids) {
            int intersectionCount = intersectionCount(detectedPatternOids, chunkMembers);
            if (intersectionCount > 0 && intersectionCount < chunkMembers.size()) {
                List<String> remainingMembers = new ArrayList<>(chunkMembers);

                for (String chunkMember : new ArrayList<>(chunkMembers)) {
                    if (detectedPatternOids.contains(chunkMember)) {
                        expandedChunksVersion.add(buildExpandedChunk(
                                roleAnalysisService, displayValueOption, chunkMember, chunk, task, result));
                        remainingMembers.remove(chunkMember);
                    }
                }

                if (remainingMembers.size() == 1) {
                    expandedChunksVersion.add(buildExpandedChunk(
                            roleAnalysisService, displayValueOption, remainingMembers.get(0), chunk, task, result));
                } else if (!remainingMembers.isEmpty()) {
                    expandedChunksVersion.add(prepareMultiMemberChunk(chunk, remainingMembers));
                }
                return;
            }
        }
        expandedChunksVersion.add(chunk);
    }

    @SuppressWarnings("unchecked")
    private static <M extends MiningBaseTypeChunk> @NotNull M prepareMultiMemberChunk(M chunk, List<String> chunkMembersCopy) {
        M newChunk;
        if (chunk instanceof MiningRoleTypeChunk) {
            newChunk = (M) new MiningRoleTypeChunk(chunk);
            newChunk.setChunkName("'" + chunkMembersCopy.size() + "' Roles");
        } else {
            newChunk = (M) new MiningUserTypeChunk(chunk);
            newChunk.setChunkName("'" + chunkMembersCopy.size() + "' Users");
        }
        newChunk.setMembers(chunkMembersCopy);
        return newChunk;
    }

    private static boolean isExpandUseful(boolean isRoleTypeChunk,
            List<List<String>> detectedPatternsObjectsOids,
            RoleAnalysisChunkMode chunkMode) {
        return chunkMode == RoleAnalysisChunkMode.EXPAND ||
                (isRoleTypeChunk && chunkMode == RoleAnalysisChunkMode.EXPAND_ROLE) ||
                (!isRoleTypeChunk && chunkMode == RoleAnalysisChunkMode.EXPAND_USER) ||
                detectedPatternsObjectsOids == null || detectedPatternsObjectsOids.isEmpty();
    }

    private static int intersectionCount(@NotNull List<String> detectedPatternOids, Set<String> chunkMembers) {
        int intersectionCount = 0;
        for (String detectedPatternOid : detectedPatternOids) {
            if (chunkMembers.contains(detectedPatternOid)) {
                intersectionCount++;
            }
        }
        return intersectionCount;
    }

    @SuppressWarnings("unchecked")
    private static <M extends MiningBaseTypeChunk> @NotNull M buildExpandedChunk(
            @NotNull RoleAnalysisServiceImpl roleAnalysisService,
            DisplayValueOption displayValueOption,
            String chunkMember, M chunk, @NotNull Task task,
            @NotNull OperationResult result) {
        M newChunk;

        if (chunk instanceof MiningRoleTypeChunk) {
            newChunk = (M) new MiningRoleTypeChunk(chunk);
        } else {
            newChunk = (M) new MiningUserTypeChunk(chunk);
        }

        String iconColor = null;
        PrismObject<FocusType> prismObject = roleAnalysisService.getFocusTypeObject(chunkMember, task, result);
        String chunkName = resolveSingleMemberChunkName(prismObject, displayValueOption);
        newChunk.setChunkName(chunkName);
        if (prismObject != null) {
            iconColor = roleAnalysisService.resolveFocusObjectIconColor(prismObject.asObjectable(), task, result);
        }

        if (iconColor != null) {
            newChunk.setIconColor(iconColor);
        }

        newChunk.setMembers(Collections.singletonList(chunkMember));
        return newChunk;
    }

    private static void resolveMemberPatternChunk(
            List<String> candidateRolesIds,
            MiningBaseTypeChunk memberChunk,
            List<String> detectedPatternsMembers,
            List<String> chunkMembers,
            int i) {
        if (new HashSet<>(detectedPatternsMembers).containsAll(chunkMembers)) {
            RoleAnalysisObjectStatus objectStatus = memberChunk.getObjectStatus();
            objectStatus.setRoleAnalysisOperationMode(RoleAnalysisOperationMode.INCLUDE);
            objectStatus.addContainerId(candidateRolesIds.get(i));
            detectedPatternsMembers.removeAll(chunkMembers);
        } else if (!memberChunk.getStatus().isInclude()) {
            memberChunk.setStatus(RoleAnalysisOperationMode.EXCLUDE);
        }
    }

    private static void resolvePatternChunk(MiningOperationChunk basicChunk,
            List<String> candidateRolesIds,
            MiningBaseTypeChunk chunk,
            List<String> detectedPatternsMembers,
            List<String> chunkMembers,
            int i,
            double frequency) {
        if (new HashSet<>(detectedPatternsMembers).containsAll(chunkMembers)) {
            RoleAnalysisObjectStatus objectStatus = chunk.getObjectStatus();
            objectStatus.setRoleAnalysisOperationMode(RoleAnalysisOperationMode.INCLUDE);
            objectStatus.addContainerId(candidateRolesIds.get(i));
            detectedPatternsMembers.removeAll(chunkMembers);
        } else if (basicChunk.getMinFrequency() > frequency && frequency < basicChunk.getMaxFrequency()
                && !chunk.getStatus().isInclude()) {
            chunk.setStatus(RoleAnalysisOperationMode.DISABLE);
        } else if (!chunk.getStatus().isInclude()) {
            chunk.setStatus(RoleAnalysisOperationMode.EXCLUDE);
        }
    }

    public static void includeMissingObjectIntoChunks(
            @NotNull RoleAnalysisService roleAnalysisService,
            String candidateRoleId,
            @NotNull List<String> detectedPatternUsers,
            @NotNull List<String> detectedPatternRoles,
            @NotNull List<MiningUserTypeChunk> users,
            @NotNull List<MiningRoleTypeChunk> roles,
            @NotNull Task task,
            @NotNull OperationResult result) {

        RoleAnalysisObjectStatus roleAnalysisObjectStatus = new RoleAnalysisObjectStatus(RoleAnalysisOperationMode.INCLUDE);
        roleAnalysisObjectStatus.setContainerId(singleton(candidateRoleId));

        if (!detectedPatternRoles.isEmpty()) {
            processMissingRoles(roleAnalysisService, detectedPatternRoles, roles, task, result, roleAnalysisObjectStatus);

        }

        if (!detectedPatternUsers.isEmpty()) {
            processMissingUsers(roleAnalysisService, detectedPatternUsers, users, task, result, roleAnalysisObjectStatus);
        }
    }

    private static void processMissingUsers(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull List<String> detectedPatternUsers,
            @NotNull List<MiningUserTypeChunk> users,
            @NotNull Task task,
            @NotNull OperationResult result,
            RoleAnalysisObjectStatus roleAnalysisObjectStatus) {
        for (String detectedPatternUser : detectedPatternUsers) {
            PrismObject<UserType> userTypeObject = roleAnalysisService.getUserTypeObject(detectedPatternUser, task, result);
            List<String> properties = new ArrayList<>();
            String chunkName = "Unknown";
            String iconColor = null;
            if (userTypeObject != null) {
                chunkName = userTypeObject.getName().toString();
                properties = getRolesOidAssignment(userTypeObject.asObjectable());
                iconColor = roleAnalysisService.resolveFocusObjectIconColor(userTypeObject.asObjectable(), task, result);
            }

            MiningUserTypeChunk miningUserTypeChunk = new MiningUserTypeChunk(
                    Collections.singletonList(detectedPatternUser),
                    properties,
                    chunkName,
                    new FrequencyItem(100.0),
                    roleAnalysisObjectStatus);

            if (iconColor != null) {
                miningUserTypeChunk.setIconColor(iconColor);
            }

            users.add(miningUserTypeChunk);
        }
    }

    private static void processMissingRoles(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull List<String> detectedPatternRoles,
            @NotNull List<MiningRoleTypeChunk> roles,
            @NotNull Task task,
            @NotNull OperationResult result,
            RoleAnalysisObjectStatus roleAnalysisObjectStatus) {
        Map<String, PrismObject<UserType>> userExistCache = new HashMap<>();
        ListMultimap<String, String> mappedMembers = roleAnalysisService.extractUserTypeMembers(
                userExistCache, null, new HashSet<>(detectedPatternRoles), task, result);

        for (String detectedPatternRole : detectedPatternRoles) {
            List<String> properties = new ArrayList<>(mappedMembers.get(detectedPatternRole));
            PrismObject<RoleType> roleTypeObject = roleAnalysisService.getRoleTypeObject(detectedPatternRole, task, result);
            String chunkName = "Unknown";
            String iconColor = null;
            if (roleTypeObject != null) {
                chunkName = roleTypeObject.getName().toString();
                iconColor = roleAnalysisService.resolveFocusObjectIconColor(roleTypeObject.asObjectable(), task, result);
            }

            MiningRoleTypeChunk miningRoleTypeChunk = new MiningRoleTypeChunk(
                    Collections.singletonList(detectedPatternRole),
                    properties,
                    chunkName,
                    new FrequencyItem(100.0),
                    roleAnalysisObjectStatus);
            if (iconColor != null) {
                miningRoleTypeChunk.setIconColor(iconColor);
            }
            roles.add(miningRoleTypeChunk);
        }
    }

    public static @NotNull String getUserChunkName(int usersCount) {
        return "'" + usersCount + "' Users";
    }

    protected static @NotNull String getRoleChunkName(int rolesCount) {
        return "'" + rolesCount + "' Roles";
    }

    protected static String resolveSingleMemberChunkName(
            @Nullable PrismObject<? extends FocusType> focusObject,
            @Nullable DisplayValueOption option) {
        String chunkName = "NOT FOUND";
        if (focusObject == null) {
            return chunkName;
        }

        String roleName = focusObject.getName().toString();
        if (option != null) {
            RoleAnalysisAttributeDef analysisDef = extractAttributeAnalysisDef(focusObject, option);
            if (analysisDef != null) {
                ItemPath path = analysisDef.getPath();
                chunkName = analysisDef.resolveSingleValueItem(focusObject, path);
                return Objects.requireNonNullElse(chunkName, "(N/A) " + roleName);
            }
        }

        return roleName;
    }

    @Nullable
    private static RoleAnalysisAttributeDef extractAttributeAnalysisDef(
            @NotNull PrismObject<? extends FocusType> focusObject,
            @NotNull DisplayValueOption option) {
        RoleAnalysisAttributeDef analysisDef;
        if (focusObject.asObjectable() instanceof UserType) {
            analysisDef = option.getUserAnalysisUserDef();
        } else {
            analysisDef = option.getRoleAnalysisRoleDef();
        }
        return analysisDef;
    }

}
