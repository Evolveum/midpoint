/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.panels.tables;

import java.util.*;

import com.evolveum.midpoint.gui.api.component.mining.analyse.tools.utils.RoleUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MiningType;

public class ExtractIntersections {

    public static List<IntersectionObject> outerIntersectionUpdate(List<PrismObject<MiningType>>
            clusterMiningTypeObjects, int minIntersection, double frequency, HashMap<String, Double> frequencyMap) {

        List<List<String>> clusterRoles = new ArrayList<>();
        for (PrismObject<MiningType> clusterMiningTypeObject : clusterMiningTypeObjects) {
            List<String> miningObjectRoles = clusterMiningTypeObject.asObjectable().getRoles();
            List<String> preparedRoles = new ArrayList<>();
            for (String oid : miningObjectRoles) {
                if (frequency <= frequencyMap.get(oid)) {
                    preparedRoles.add(oid);
                }
            }
            clusterRoles.add(preparedRoles);
        }

        return getIntersectionOuterMap(clusterRoles, minIntersection, clusterMiningTypeObjects);
    }

    public static List<IntersectionObject> innerIntersectionUpdate(
            List<PrismObject<MiningType>> clusterMiningTypeObjects, int minIntersection,
            double frequency, HashMap<String, Double> frequencyMap) {

        List<IntersectionObject> intersectionMap = outerIntersectionUpdate(clusterMiningTypeObjects,
                minIntersection, frequency, frequencyMap);

        return getIntersectionInnerMap(minIntersection, intersectionMap, clusterMiningTypeObjects);
    }

    public static List<IntersectionObject> getIntersectionOuterMap(List<List<String>> roles, int minIntersection,
            List<PrismObject<MiningType>> clusterMiningTypeObjects) {

        Set<Set<String>> intersectionsSet = new HashSet<>();
        for (int i = 0; i < roles.size(); i++) {
            Set<String> rolesA = new HashSet<>(roles.get(i));
            for (int j = i + 1; j < roles.size(); j++) {
                Set<String> rolesB = new HashSet<>(roles.get(j));

                Set<String> intersection = RoleUtils.intersection(rolesA, rolesB);

                if (intersection.size() >= minIntersection) {
                    List<String> nInter = new ArrayList<>(intersection);
                    Collections.sort(nInter);
                    intersectionsSet.add(new HashSet<>(nInter));
                }
            }
        }

        return occupiedCount(clusterMiningTypeObjects, intersectionsSet, "outer");
    }

    public static List<IntersectionObject> occupiedCount(List<PrismObject<MiningType>> roles, Set<Set<String>> intersectionsSet,
            String type) {
        List<IntersectionObject> intersectionObjectList = new ArrayList<>();
        for (Set<String> intersections : intersectionsSet) {
            int counter = 0;
            for (PrismObject<MiningType> role : roles) {
                if (new HashSet<>(role.asObjectable().getRoles()).containsAll(intersections)) {
                    counter = counter + role.asObjectable().getMembersCount();
                }
            }
            intersectionObjectList.add(new IntersectionObject(intersections, counter * intersections.size(), type));
        }
        return intersectionObjectList;
    }

    public static List<IntersectionObject> getIntersectionInnerMap(int minIntersection, List<IntersectionObject> intersectionMap,
            List<PrismObject<MiningType>> clusterMiningTypeObjects) {

        Set<Set<String>> intersectionsSet = new HashSet<>();
        for (int i = 0; i < intersectionMap.size(); i++) {
            Set<String> rolesA = new HashSet<>(intersectionMap.get(i).getRolesId());

            for (int j = i + 1; j < intersectionMap.size(); j++) {
                Set<String> rolesB = new HashSet<>(intersectionMap.get(j).rolesId);

                Set<String> intersection = RoleUtils.intersection(rolesA, rolesB);

                if (intersection.size() >= minIntersection) {

                    List<String> nInter = new ArrayList<>(intersection);
                    Collections.sort(nInter);
                    intersectionsSet.add(new HashSet<>(nInter));

                }
            }
        }

        return occupiedCount(clusterMiningTypeObjects, intersectionsSet, "inner");
    }

}
