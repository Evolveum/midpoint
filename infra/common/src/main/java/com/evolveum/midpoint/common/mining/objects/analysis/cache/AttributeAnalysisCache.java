package com.evolveum.midpoint.common.mining.objects.analysis.cache;

import com.evolveum.midpoint.common.mining.objects.analysis.AttributePathResult;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisAttributeAnalysisResult;

import com.google.common.collect.ListMultimap;

import java.util.Map;

//TODO think tmp
public class AttributeAnalysisCache {

    //TODO RoleAttributeAnalyseCache
    private UserAttributeAnalyseCache userCache;
    private RoleMemberAttributeAnalyseCache roleMemberAttributeCache;
    private MemberUserAttributeAnalysisCache memberUserCache;
    private ListMultimap<String, String> roleMemberCache;
    private ListMultimap<String, String> userMemberCache;
    private final RoleMemberCountCache roleMemberCountCache;

    public AttributeAnalysisCache(UserAttributeAnalyseCache userCache,
            RoleMemberAttributeAnalyseCache roleMemberAttributeCache,
            MemberUserAttributeAnalysisCache memberUserCache,
            RoleMemberCountCache roleMemberCountCache,
            ListMultimap<String, String> roleMemberCache) {
        this.userCache = userCache;
        this.roleMemberAttributeCache = roleMemberAttributeCache;
        this.memberUserCache = memberUserCache;
        this.roleMemberCountCache = roleMemberCountCache;
        this.roleMemberCache = roleMemberCache;
    }

    public AttributeAnalysisCache() {
        this.userCache = new UserAttributeAnalyseCache();
        this.roleMemberAttributeCache = new RoleMemberAttributeAnalyseCache();
        this.memberUserCache = new MemberUserAttributeAnalysisCache();
        this.roleMemberCountCache = new RoleMemberCountCache();
    }

    public void putMemberUserAnalysisCache(String user, ItemPath key, AttributePathResult value) {
        memberUserCache.putPathResult(user, key, value);
    }

    public void putUserAttributeAnalysisCache(String user, RoleAnalysisAttributeAnalysisResult value) {
        userCache.put(user, value);
    }

    public void putRoleMemberAnalysisCache(String role, RoleAnalysisAttributeAnalysisResult value) {
        roleMemberAttributeCache.put(role, value);
    }

    public AttributePathResult getMemberUserAnalysisPathCache(String user, String key) {
        return memberUserCache.getPathResult(user, key);
    }

    public Map<ItemPath, AttributePathResult> getMemberUserAnalysisCache(String user) {
        return memberUserCache.getUserResult(user);
    }

    public void putMemberUserAnalysisCache(String user, Map<ItemPath, AttributePathResult> value) {
        memberUserCache.putUserResult(user, value);
    }

    public RoleAnalysisAttributeAnalysisResult getUserAttributeAnalysisCache(String user) {
        return userCache.get(user);
    }

    public RoleAnalysisAttributeAnalysisResult getRoleMemberAnalysisCache(String role) {
        return roleMemberAttributeCache.get(role);
    }

    public void putRoleMemberCountCache(String role, Integer value) {
        roleMemberCountCache.put(role, value);
    }

    public Integer getRoleMemberCountCache(String role) {
        return roleMemberCountCache.get(role);
    }

    public UserAttributeAnalyseCache getUserCache() {
        return userCache;
    }

    public RoleMemberAttributeAnalyseCache getRoleMemberAttributeCache() {
        return roleMemberAttributeCache;
    }

    public MemberUserAttributeAnalysisCache getMemberUserCache() {
        return memberUserCache;
    }

    public RoleMemberCountCache getRoleMemberCountCache() {
        return roleMemberCountCache;
    }

    public ListMultimap<String, String> getRoleMemberCache() {
        return roleMemberCache;
    }

    public void setRoleMemberCache(ListMultimap<String, String> roleMemberCache) {
        this.roleMemberCache = roleMemberCache;
    }

    public ListMultimap<String, String> getUserMemberCache() {
        return userMemberCache;
    }

    public void setUserMemberCache(ListMultimap<String, String> userMemberCache) {
        this.userMemberCache = userMemberCache;
    }

}
