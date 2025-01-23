/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.mining.utils;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AnalysisAttributeSettingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

public class RoleAnalysisAttributeDefUtils {

    private static final Trace LOGGER = TraceManager.getTrace(RoleAnalysisAttributeDefUtils.class);

    public static RoleAnalysisAttributeDef getAttributeByItemPath(ItemPath itemPath, AnalysisAttributeSettingType analysisAttributeSettings) {
        List<RoleAnalysisAttributeDef> attributeMap = createAttributeList(analysisAttributeSettings);
        for (RoleAnalysisAttributeDef attribute : attributeMap) {
            if (attribute.getPath().equivalent(itemPath)) {
                return attribute;
            }
        }
        return null;
    }

    public static List<RoleAnalysisAttributeDef> createAttributeList(AnalysisAttributeSettingType analysisAttributeSetting) {
        List<RoleAnalysisAttributeDef> attributeDefs = new ArrayList<>();

        PrismObjectDefinition<UserType> userDefinition = PrismContext.get().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
        List<ItemPathType> analysisAttributeRule = analysisAttributeSetting.getPath();

        if (analysisAttributeRule.isEmpty()) {
            return attributeDefs;
        }

        for (ItemPathType itemPathType : analysisAttributeRule) {
            if (itemPathType == null) {
                continue;
            }
            ItemPath path = itemPathType.getItemPath();
            ItemDefinition<?> itemDefinition = userDefinition.findItemDefinition(path);
            if (itemDefinition instanceof PrismContainerDefinition<?>) {
                LOGGER.debug("Skipping {} because container items are not supported for attribute analysis.", itemDefinition);
                continue;
            }
            //TODO reference vs. property
            RoleAnalysisAttributeDef attributeDef = new RoleAnalysisAttributeDef(path, itemDefinition, UserType.class);
            attributeDefs.add(attributeDef);
        }
        return attributeDefs;
    }



    public static boolean isSupportedPropertyType(@Nullable Class<?> typeClass) {
        if(typeClass == null) {
            return false;
        }
        return typeClass.equals(Integer.class) || typeClass.equals(Long.class) || typeClass.equals(Boolean.class)
                || typeClass.equals(Double.class) || typeClass.equals(String.class) || typeClass.equals(PolyString.class);
    }
}
