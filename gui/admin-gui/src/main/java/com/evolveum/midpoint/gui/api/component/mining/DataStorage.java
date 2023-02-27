/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.mining;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.evolveum.midpoint.gui.api.component.mining.analyse.structure.prune.CandidateRole;
import com.evolveum.midpoint.gui.api.component.mining.analyse.structure.prune.ResultData;
import com.evolveum.midpoint.gui.api.component.mining.analyse.structure.prune.UpType;
import com.evolveum.midpoint.gui.api.component.mining.analyse.structure.prune.UrType;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.component.mining.structure.ProbabilityStructure;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class DataStorage {

    static List<HashMap<Integer, CandidateRole>> rolesDegrees = new ArrayList<>();
    static HashMap<Integer, List<UrType>> candidateKeyUpStructureMapUR = new HashMap<>();
    static HashMap<Integer, List<UpType>> candidateKeyUpStructureMapUP = new HashMap<>();

    static int usersCount = 0;

    //cost model
    static int iterateRA = 0;
    static int iteratePA = 0;
    static int iterateUA = 0;

    static List<PrismObject<RoleType>> roles = new ArrayList<>();
    static List<RoleType> rolesObject = new ArrayList<>();
    static List<PrismObject<UserType>> users = new ArrayList<>();

    static List<UpType> userPermissionList = new ArrayList<>();
    static List<UrType> userRolesList = new ArrayList<>();

    static List<List<AuthorizationType>> permissionImport = new ArrayList<>();
    static List<List<RoleType>> rolesImport = new ArrayList<>();

    static HashMap<UserType, List<List<AuthorizationType>>> resultUaMap = new HashMap<>();
    static HashMap<UserType, List<List<RoleType>>> resultUrMap = new HashMap<>();

    static List<ResultData> resultDatumTypes = new ArrayList<>();

    static List<ProbabilityStructure> probabilitiesList = new ArrayList<>();

    public static void fillRolesAndUsers(PageBase pageBase) {
        roles = new RoleMiningFilter().filterRoles(pageBase);
        users = new RoleMiningFilter().filterUsers(pageBase);
        usersCount = users.size();
    }

    public static void resetProbabilities() {
        probabilitiesList = new ArrayList<>();
    }

    public static void resetAll() {
        usersCount = 0;

        iterateRA = 0;
        iteratePA = 0;
        iterateUA = 0;

        roles = new ArrayList<>();
        rolesObject = new ArrayList<>();
        users = new ArrayList<>();

        userPermissionList = new ArrayList<>();
        userRolesList = new ArrayList<>();

        permissionImport = new ArrayList<>();
        rolesImport = new ArrayList<>();

        resultUaMap = new HashMap<>();
        resultUrMap = new HashMap<>();

        resultDatumTypes = new ArrayList<>();

        probabilitiesList = new ArrayList<>();
        // fillStructures(pageBase);

        rolesDegrees = new ArrayList<>();
        candidateKeyUpStructureMapUR = new HashMap<>();
    }

    public static HashMap<Integer, List<UpType>> getCandidateKeyUpStructureMapUP() {
        return candidateKeyUpStructureMapUP;
    }

    public static void setCandidateKeyUpStructureMapUP(HashMap<Integer, List<UpType>> candidateKeyUpStructureMapUP) {
        DataStorage.candidateKeyUpStructureMapUP = candidateKeyUpStructureMapUP;
    }

    public static List<HashMap<Integer, CandidateRole>> getRolesDegrees() {
        return rolesDegrees;
    }

    public static void setRolesDegrees(List<HashMap<Integer, CandidateRole>> rolesDegrees) {
        DataStorage.rolesDegrees = rolesDegrees;
    }

    public static HashMap<Integer, List<UrType>> getCandidateKeyUpStructureMapUR() {
        return candidateKeyUpStructureMapUR;
    }

    public static void setCandidateKeyUpStructureMapUR(HashMap<Integer, List<UrType>> candidateKeyUpStructureMapUR) {
        DataStorage.candidateKeyUpStructureMapUR = candidateKeyUpStructureMapUR;
    }

    public static List<List<RoleType>> getRolesImport() {
        return rolesImport;
    }

    public static void setRolesImport(List<List<RoleType>> rolesImport) {
        DataStorage.rolesImport = rolesImport;
    }

    public static HashMap<UserType, List<List<RoleType>>> getResultUrMap() {
        return resultUrMap;
    }

    public static void setResultUrMap(HashMap<UserType, List<List<RoleType>>> resultUrMap) {
        DataStorage.resultUrMap = resultUrMap;
    }

    public static List<ProbabilityStructure> getProbabilitiesList() {
        return probabilitiesList;
    }

    public static void setProbabilitiesList(List<ProbabilityStructure> probabilitiesList) {
        DataStorage.probabilitiesList = probabilitiesList;
    }

    public static List<RoleType> getRolesObject() {
        return rolesObject;
    }

    public static void setRolesObject(List<RoleType> rolesObject) {
        DataStorage.rolesObject = rolesObject;
    }

    public static List<UrType> getUserRolesList() {
        return userRolesList;
    }

    public static void setUserRolesList(List<UrType> userRolesList) {
        DataStorage.userRolesList = userRolesList;
    }

    public static int getIterateRA() {
        return iterateRA;
    }

    public static void setIterateRA(int iterateRA) {
        DataStorage.iterateRA = iterateRA;
    }

    public static int getIteratePA() {
        return iteratePA;
    }

    public static void setIteratePA(int iteratePA) {
        DataStorage.iteratePA = iteratePA;
    }

    public static int getIterateUA() {
        return iterateUA;
    }

    public static void setIterateUA(int iterateUA) {
        DataStorage.iterateUA = iterateUA;
    }

    public static List<UpType> getUserPermissionList() {
        return userPermissionList;
    }

    public static void setUserPermissionList(List<UpType> userPermissionList) {
        DataStorage.userPermissionList = userPermissionList;
    }

    public static List<List<AuthorizationType>> getPermissionImport() {
        return permissionImport;
    }

    public static void setPermissionImport(List<List<AuthorizationType>> permissionImport) {
        DataStorage.permissionImport = permissionImport;
    }

    public static List<ResultData> getResultData() {
        return resultDatumTypes;
    }

    public static void setResultData(List<ResultData> resultDatumTypes) {
        DataStorage.resultDatumTypes = resultDatumTypes;
    }

    public static HashMap<UserType, List<List<AuthorizationType>>> getResultUaMap() {
        return resultUaMap;
    }

    public static void setResultUaMap(HashMap<UserType, List<List<AuthorizationType>>> resultUaMap) {
        DataStorage.resultUaMap = resultUaMap;
    }

    public static List<PrismObject<RoleType>> getDataStorageRoles() {
        return roles;
    }

    public static void setRoles(List<PrismObject<RoleType>> roles) {
        DataStorage.roles = roles;
    }

    public static List<PrismObject<UserType>> getDataStorageUsers() {
        return users;
    }

    public static void setUsers(List<PrismObject<UserType>> users) {
        DataStorage.users = users;
    }

    public static int getDataStorageUsersCount() {
        return usersCount;
    }

}
