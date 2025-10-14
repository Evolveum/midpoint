package com.evolveum.midpoint.ninja.action.stats;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

import com.evolveum.midpoint.ninja.action.AbstractRepositorySearchAction;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class ExportFocusStatisticsAction extends AbstractRepositorySearchAction<ExportFocusStatisticsOptions, Void> {
    private static final List<ItemName> INCLUDE_ITEMS = List.of(
            new ItemName(SchemaConstants.NS_C, "description"), new ItemName(SchemaConstants.NS_C, "documentation"),
            new ItemName(SchemaConstants.NS_C, "subtype"), new ItemName(SchemaConstants.NS_C, "lifecycleState"),
            new ItemName(SchemaConstants.NS_C, "jpegPhoto"), new ItemName(SchemaConstants.NS_C, "costCenter"),
            new ItemName(SchemaConstants.NS_C, "locality"), new ItemName(SchemaConstants.NS_C, "preferredLanguage"),
            new ItemName(SchemaConstants.NS_C, "locale"), new ItemName(SchemaConstants.NS_C, "timezone"),
            new ItemName(SchemaConstants.NS_C, "emailAddress"), new ItemName(SchemaConstants.NS_C, "telephoneNumber"),
            new ItemName(SchemaConstants.NS_C, "fullName"), new ItemName(SchemaConstants.NS_C, "givenName"),
            new ItemName(SchemaConstants.NS_C, "familyName"), new ItemName(SchemaConstants.NS_C, "additionalName"),
            new ItemName(SchemaConstants.NS_C, "nickName"), new ItemName(SchemaConstants.NS_C, "honorificPrefix"),
            new ItemName(SchemaConstants.NS_C, "honorificSuffix"), new ItemName(SchemaConstants.NS_C, "title"),
            new ItemName(SchemaConstants.NS_C, "employeeNumber"), new ItemName(SchemaConstants.NS_C, "personalNumber"),
            new ItemName(SchemaConstants.NS_C, "organization"),
            new ItemName(SchemaConstants.NS_C, "organizationalUnit"), new ItemName(SchemaConstants.NS_C, "displayName"),
            new ItemName(SchemaConstants.NS_C, "identifier"), new ItemName(SchemaConstants.NS_C, "riskLevel"),
            new ItemName(SchemaConstants.NS_C, "mailDomain"), new ItemName(SchemaConstants.NS_C, "displayOrder"),
            new ItemName(SchemaConstants.NS_C, "url"), new ItemName(SchemaConstants.NS_C, "extension"));

    @Override
    protected Callable<Void> createConsumer(BlockingQueue<ObjectType> queue, OperationStatus operation) {
        return () -> {
            final List<ItemPath> excludeItems = this.options.getExcludeItems();

            Predicate<Item<?, ?>> includePredicate =
                    definition -> INCLUDE_ITEMS.stream().anyMatch(definition.getPath()::isSuperPathOrEquivalent);
            includePredicate = includePredicate.and(
                    definition -> excludeItems.stream().noneMatch(definition.getPath()::isSuperPathOrEquivalent));
            final StatsCounter statsCounter = new StatsCounter(includePredicate);
            new ExportFocusStatisticsWorker(context, options, queue, operation, statsCounter).run();
            return null;
        };
    }

    @Override
    protected Iterable<ObjectTypes> supportedObjectTypes() {
        return List.of(ObjectTypes.USER,
                ObjectTypes.ROLE,
                ObjectTypes.SERVICE,
                ObjectTypes.ORG

        );
    }

    @Override
    public String getOperationName() {
        return "Focus statistics counter";
    }
}
