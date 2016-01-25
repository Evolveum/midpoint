/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Viliam Repan (lazyman)
 */
public class SearchFactory {

    private static final Map<Class, List<ItemPath>> SEARCHABLE_OBJECTS = new HashMap<>();

    static {
        SEARCHABLE_OBJECTS.put(ObjectType.class, Arrays.asList(
                new ItemPath(ObjectType.F_NAME)));
        SEARCHABLE_OBJECTS.put(FocusType.class, Arrays.asList(
                new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS)));
        SEARCHABLE_OBJECTS.put(UserType.class, Arrays.asList(
                new ItemPath(UserType.F_GIVEN_NAME),
                new ItemPath(UserType.F_FAMILY_NAME),
                new ItemPath(UserType.F_FULL_NAME),
                new ItemPath(UserType.F_ADDITIONAL_NAME),
                new ItemPath(UserType.F_COST_CENTER)));

        //todo add other object types and properties which can be used in search
    }

    public static <T extends ObjectType> Search createSearch(Class<T> type, PrismContext ctx) {
        Map<ItemPath, ItemDefinition> availableDefs = getAvailableDefinitions(type, ctx);

        Search search = new Search(type, availableDefs);

        SchemaRegistry registry = ctx.getSchemaRegistry();
        PrismObjectDefinition objDef = registry.findObjectDefinitionByCompileTimeClass(ObjectType.class);
        PrismPropertyDefinition def = objDef.findPropertyDefinition(ObjectType.F_NAME);

        search.addItem(def);

        return search;
    }

    private static <T extends ObjectType> Map<ItemPath, ItemDefinition> getAvailableDefinitions(
            Class<T> type, PrismContext ctx) {

        Map<ItemPath, ItemDefinition> map = new HashMap<>();
        map.putAll(createExtensionDefinitionList(type, ctx));

        Class<T> typeClass = type;
        while (typeClass != null && !com.evolveum.prism.xml.ns._public.types_3.ObjectType.class.equals(typeClass)) {
            List<ItemPath> pathList = SEARCHABLE_OBJECTS.get(typeClass);
            if (pathList != null) {
                map.putAll(createAvailableDefinitions(typeClass, ctx, pathList));

                typeClass = (Class<T>) typeClass.getSuperclass();
            }
        }

        return map;
    }

    private static <T extends ObjectType> Map<ItemPath, ItemDefinition> createExtensionDefinitionList(
            Class<T> type, PrismContext ctx) {

        Map<ItemPath, ItemDefinition> map = new HashMap<>();

        SchemaRegistry registry = ctx.getSchemaRegistry();
        PrismObjectDefinition objDef = registry.findObjectDefinitionByCompileTimeClass(type);

        ItemPath extensionPath = new ItemPath(ObjectType.F_EXTENSION);

        PrismContainerDefinition ext = objDef.findContainerDefinition(ObjectType.F_EXTENSION);
        for (ItemDefinition def : (List<ItemDefinition>) ext.getDefinitions()) {
            if (!(def instanceof PrismPropertyDefinition)
                    && !(def instanceof PrismReferenceDefinition)) {
                continue;
            }

            map.put(new ItemPath(extensionPath, def.getName()), def);
        }

        return map;
    }

    private static <T extends ObjectType> Map<ItemPath, ItemDefinition> createAvailableDefinitions(
            Class<T> type, PrismContext ctx, List<ItemPath> paths) {

        Map<ItemPath, ItemDefinition> map = new HashMap<>();

        SchemaRegistry registry = ctx.getSchemaRegistry();
        PrismObjectDefinition objDef = registry.findObjectDefinitionByCompileTimeClass(type);

        for (ItemPath path : paths) {
            ItemDefinition def = objDef.findItemDefinition(path);
            if (def != null) {
                map.put(path, def);
            }
        }

        return map;
    }
}
