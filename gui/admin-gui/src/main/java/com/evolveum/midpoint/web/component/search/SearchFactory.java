package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Viliam Repan (lazyman)
 */
public class SearchFactory {

    public static <T extends ObjectType> Search createSearch(Class<T> type, PrismContext ctx) {
        Map<ItemPath, ItemDefinition> availableDefs = getAvailableDefinitionsForUser(ctx);

        Search search = new Search(type, availableDefs);

        SchemaRegistry registry = ctx.getSchemaRegistry();
        PrismObjectDefinition objDef = registry.findObjectDefinitionByCompileTimeClass(ObjectType.class);
        PrismPropertyDefinition def = objDef.findPropertyDefinition(ObjectType.F_NAME);

        search.addItem(def);

        return search;
    }

    private static Map<ItemPath, ItemDefinition> getAvailableDefinitionsForObject(PrismContext ctx) {
        Map<ItemPath, ItemDefinition> map = new HashMap<>();

        Map<ItemPath, ItemDefinition> object = createAvailableDefinitions(UserType.class, ctx,
                new ItemPath(ObjectType.F_NAME),
                new ItemPath(ObjectType.F_PARENT_ORG_REF),
                new ItemPath(ObjectType.F_TENANT_REF)
        );
        map.putAll(object);

        return map;
    }

    private static Map<ItemPath, ItemDefinition> getAvailableDefinitionsForFocus(PrismContext ctx) {
        Map<ItemPath, ItemDefinition> map = getAvailableDefinitionsForObject(ctx);

        Map<ItemPath, ItemDefinition> user = createAvailableDefinitions(UserType.class, ctx,
                new ItemPath(FocusType.F_LINK_REF),
                new ItemPath(FocusType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF),
                new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS));
        map.putAll(user);

        return map;
    }

    private static Map<ItemPath, ItemDefinition> getAvailableDefinitionsForUser(PrismContext ctx) {
        Map<ItemPath, ItemDefinition> map = getAvailableDefinitionsForFocus(ctx);

        map.putAll(createExtensionDefinitionList(UserType.class, ctx));

        Map<ItemPath, ItemDefinition> user = createAvailableDefinitions(UserType.class, ctx,
                new ItemPath(UserType.F_GIVEN_NAME),
                new ItemPath(UserType.F_FAMILY_NAME),
                new ItemPath(UserType.F_FULL_NAME),
                new ItemPath(UserType.F_ADDITIONAL_NAME),
                new ItemPath(UserType.F_COST_CENTER));
        map.putAll(user);

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
            Class<T> type, PrismContext ctx, ItemPath... paths) {

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
