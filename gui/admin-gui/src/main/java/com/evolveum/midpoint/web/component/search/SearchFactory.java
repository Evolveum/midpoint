package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Viliam Repan (lazyman)
 */
public class SearchFactory {

    public static <T extends ObjectType> Search createSearch(Class<T> type, PrismContext ctx) {
        List<ItemDefinition> availableDefs = getAvailableDefinitionsForUser(ctx);

        Search search = new Search(availableDefs);

        SchemaRegistry registry = ctx.getSchemaRegistry();
        PrismObjectDefinition objDef = registry.findObjectDefinitionByCompileTimeClass(ObjectType.class);
        PrismPropertyDefinition def = objDef.findPropertyDefinition(ObjectType.F_NAME);

        SearchItem item = new SearchItem(search, def);
        search.add(item);

        return search;
    }

    private static List<ItemDefinition> getAvailableDefinitionsForObject(PrismContext ctx) {
        List<ItemDefinition> list = new ArrayList<>();

        List<ItemDefinition> object = createAvailableDefinitions(UserType.class, ctx,
                new ItemPath(ObjectType.F_NAME),
                new ItemPath(ObjectType.F_PARENT_ORG_REF),
                new ItemPath(ObjectType.F_TENANT_REF));
        list.addAll(object);

        return list;
    }

    private static List<ItemDefinition> getAvailableDefinitionsForFocus(PrismContext ctx) {
        List<ItemDefinition> list = getAvailableDefinitionsForObject(ctx);

        List<ItemDefinition> user = createAvailableDefinitions(UserType.class, ctx,
                new ItemPath(FocusType.F_LINK_REF),
                new ItemPath(FocusType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF),
                new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS));
        list.addAll(user);

        return list;
    }

    private static List<ItemDefinition> getAvailableDefinitionsForUser(PrismContext ctx) {
        List<ItemDefinition> list = getAvailableDefinitionsForFocus(ctx);

        List<ItemDefinition> user = createAvailableDefinitions(UserType.class, ctx,
                new ItemPath(UserType.F_GIVEN_NAME),
                new ItemPath(UserType.F_FAMILY_NAME),
                new ItemPath(UserType.F_FULL_NAME),
                new ItemPath(UserType.F_ADDITIONAL_NAME),
                new ItemPath(UserType.F_COST_CENTER));
        list.addAll(user);

        return list;
    }

    private static <T extends ObjectType> List<ItemDefinition> createAvailableDefinitions(
            Class<T> type, PrismContext ctx, ItemPath... paths) {

        List<ItemDefinition> list = new ArrayList<>();

        SchemaRegistry registry = ctx.getSchemaRegistry();
        PrismObjectDefinition objDef = registry.findObjectDefinitionByCompileTimeClass(type);

        for (ItemPath path : paths) {
            ItemDefinition def = objDef.findItemDefinition(path);
            if (def != null) {
                list.add(def);
            }
        }

        return list;
    }
}
