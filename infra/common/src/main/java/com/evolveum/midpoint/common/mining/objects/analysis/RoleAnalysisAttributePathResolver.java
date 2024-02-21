package com.evolveum.midpoint.common.mining.objects.analysis;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcessModeType;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Class that resolves the paths for the role analysis process mode.
 * It resolves the paths for the user and role objects.
 * The paths that are defined here is used for attribute analysis.
 * Used in role analysis cluster chart.
 */
// TODO - this class is just fast experiment
public class RoleAnalysisAttributePathResolver {

    public static final ItemName F_TITLE = new ItemName(ObjectFactory.NAMESPACE, "title");
    public static final ItemName F_ORGANIZATION = new ItemName(ObjectFactory.NAMESPACE, "organization");
    public static final ItemName F_ORGANIZATIONAL_UNIT = new ItemName(ObjectFactory.NAMESPACE, "organizationalUnit");
    public static final ItemName F_LOCALE = new ItemName(ObjectFactory.NAMESPACE, "locale");
    public static final ItemName F_TIMEZONE = new ItemName(ObjectFactory.NAMESPACE, "timezone");
    public static final ItemName F_PREFERRED_LANGUAGE = new ItemName(ObjectFactory.NAMESPACE, "preferredLanguage");
    public static final ItemName F_COST_CENTER = new ItemName(ObjectFactory.NAMESPACE, "costCenter");
    public static final ItemName F_LIFECYCLE_STATE = new ItemName(ObjectFactory.NAMESPACE, "lifecycleState");
    public static final ItemName F_LOCALITY = new ItemName(ObjectFactory.NAMESPACE, "locality");

    public static final ItemName F_RISK_LEVEL = new ItemName(ObjectFactory.NAMESPACE, "riskLevel");

    public static List<ItemPath> resolveProcessModePath(RoleAnalysisProcessModeType mode) {
        if (mode.equals(RoleAnalysisProcessModeType.USER)) {
            return getUserSingleValuePaths();
        } else if (mode.equals(RoleAnalysisProcessModeType.ROLE)) {
            return getRoleSingleValuePaths();
        }
        return new ArrayList<>();
    }

    public static List<ItemPath> getUserSingleValuePaths() {
        List<ItemPath> paths = new ArrayList<>();
        paths.add(createPath(F_TITLE));
        paths.add(createPath(F_LOCALE));
        paths.add(createPath(F_TIMEZONE));
        paths.add(createPath(F_PREFERRED_LANGUAGE));
        paths.add(createPath(F_COST_CENTER));
        paths.add(createPath(F_LIFECYCLE_STATE));
        paths.add(createPath(F_LOCALITY));
        return paths;
    }

    public static Set<String> getUserPathLabels() {
        Set<String> set = new HashSet<>();
        set.add("title");
        set.add("locale");
        set.add("timezone");
        set.add("preferredLanguage");
        set.add("costCenter");
        set.add("lifecycleState");
        set.add("locality");
        set.add("roleMembershipRef");
        set.add("linkRef");
        set.add("assignment/role");
        set.add("assignment/service");
        set.add("assignment/org");
        set.add("assignment/user");
        set.add("assignment/resource");
        return set;
    }

    public static Set<String> getRolePathLabels() {
        Set<String> set = new HashSet<>();
        set.add("lifecycleState");
        set.add("preferredLanguage");
        set.add("locale");
        set.add("timezone");
        set.add("locality");
        set.add("costCenter");
        set.add("riskLevel");
        set.add("member/user");
        set.add("linkRef");
        set.add("archetypeRef");
        set.add("roleMembershipRef");
        set.add("assignment/role");
        set.add("assignment/service");
        set.add("assignment/org");
        set.add("assignment/user");
        set.add("assignment/resource");
        set.add("inducement/role");
        set.add("inducement/service");
        set.add("inducement/org");
        set.add("inducement/user");
        set.add("inducement/resource");
        return set;
    }

    public static List<ItemPath> getRoleSingleValuePaths() {
        List<ItemPath> paths = new ArrayList<>();
        paths.add(createPath(F_LIFECYCLE_STATE));
        paths.add(createPath(F_PREFERRED_LANGUAGE));
        paths.add(createPath(F_LOCALE));
        paths.add(createPath(F_TIMEZONE));
        paths.add(createPath(F_LOCALITY));
        paths.add(createPath(F_COST_CENTER));
        paths.add(createPath(F_RISK_LEVEL));
        return paths;
    }

    private static @NotNull ItemPath createPath(ItemName itemName) {
        return ItemPath.create(itemName);
    }
}
