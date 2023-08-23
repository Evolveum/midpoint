/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.chunk;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils.getRoleTypeObject;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils.getUserTypeObject;

public class CacheUtils {

    protected static PrismObject<RoleType> cacheRole(PageBase pageBase, OperationResult result,
            Map<String, PrismObject<RoleType>> roleExistCache, String roleOid) {
        PrismObject<RoleType> role = roleExistCache.get(roleOid);
        if (role == null) {
            role = getRoleTypeObject(pageBase, roleOid, result);
            if (role == null) {
                return null;
            }
            roleExistCache.put(roleOid, role);
        }
        return role;
    }

    @Nullable
    protected static PrismObject<UserType> cacheUser(PageBase pageBase, OperationResult result,
            Map<String, PrismObject<UserType>> userExistCache, String userOid) {
        PrismObject<UserType> user = userExistCache.get(userOid);
        if (user == null) {
            user = getUserTypeObject(pageBase, userOid, result);
            if (user == null) {
                return null;
            }
            userExistCache.put(userOid, user);
        }
        return user;
    }

}
