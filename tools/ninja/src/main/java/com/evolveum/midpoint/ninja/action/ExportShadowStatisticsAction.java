package com.evolveum.midpoint.ninja.action;

import java.io.Writer;
import java.util.function.Consumer;

import com.evolveum.midpoint.ninja.action.stats.MagnitudeCounter;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemFactory;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.SerializationOptions;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.AggregateQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class ExportShadowStatisticsAction extends RepositoryAction<ExportShadowStatisticsOptions, Void>{

    private static final ItemName C_SHADOW_STATISTICS = new ItemName(SchemaConstants.NS_C, "shadowStatistics");
    private static final ItemName C_SHADOW = new ItemName(SchemaConstants.NS_C, "shadow");
    private static final ItemName F_COUNT = new ItemName(SchemaConstants.NS_C, "count");

    @Override
    public String getOperationName() {
        return "export-statistics";
    }

    @Override
    public Void execute() throws Exception {
        // The repository needs to be retrieved before the query is created (using the AggregateQuery.forType)!
        // Otherwise, you may get NPE when you call the `forType` method.
        final RepositoryService repository = context.getRepository();
        ObjectQuery shadowQuery = NinjaUtils.createObjectQuery(this.options.getFilter(), context, ShadowType.class);
        final var query = createAggregationQuery(shadowQuery);

        final OperationResult result = new OperationResult("Shadow Statistics");
        final SearchResultList<PrismContainerValue<?>> aggregationResults = repository.searchAggregate(query, result);

        final PrismContainer<Containerable> statisticsContainer = wrapAggregationResults(aggregationResults,
                ExportShadowStatisticsAction::changeCountsToMagnitude);

        final var serializer = context.getPrismContext()
                .xmlSerializer()
                .options(SerializationOptions.createSerializeForExport());
        final Writer writer = NinjaUtils.createWriter(
                options.getOutput(), context.getCharset(), options.isZip(), options.isOverwrite(), context.out);
        writer.write(serializer.serialize(statisticsContainer));
        writer.flush();

        return null;
    }

    private PrismContainer<Containerable> wrapAggregationResults(
            SearchResultList<PrismContainerValue<?>> aggregationResults,
            Consumer<PrismContainerValue<?>> aggregationModifier) throws SchemaException {
        final ItemFactory itemFactory = context.getPrismContext().itemFactory();
        final PrismContainerValue<Containerable> statisticsValue = itemFactory.createContainerValue();
        final PrismContainer<Containerable> shadowContainer = itemFactory.createContainer(C_SHADOW);

        for (PrismContainerValue<?> aggregation : aggregationResults) {
            try {
                aggregationModifier.accept(aggregation);

                @SuppressWarnings("unchecked")
                final PrismContainerValue<Containerable> castedAggregation =
                        (PrismContainerValue<Containerable>) aggregation;
                shadowContainer.addIgnoringEquivalents(castedAggregation);
            } catch (SchemaException e) {
                log.error("Couldn't add shadow statistics", e);
            }
        }

        statisticsValue.add(shadowContainer);
        final PrismContainer<Containerable> statisticsContainer = itemFactory.createContainer(C_SHADOW_STATISTICS);
        statisticsContainer.add(statisticsValue);
        return statisticsContainer;
    }

    private static AggregateQuery<ShadowType> createAggregationQuery(ObjectQuery shadowQuery) {
        final AggregateQuery<ShadowType> query = AggregateQuery.forType(ShadowType.class)
                .retrieve(ShadowType.F_RESOURCE_REF)
                .retrieve(ShadowType.F_OBJECT_CLASS)
                .retrieve(ShadowType.F_KIND)
                .retrieve(ShadowType.F_INTENT)
                .retrieve(ShadowType.F_SYNCHRONIZATION_SITUATION)
                .count(F_COUNT, ShadowType.F_RESOURCE_REF);
        if (shadowQuery != null) {
            return query.filter(shadowQuery.getFilter());
        }
        return query;
    }

    private static void changeCountsToMagnitude(PrismContainerValue<?> aggregation) {
        final PrismProperty<Long> count = aggregation.findProperty(F_COUNT);
        final long orderOfMagnitude = new MagnitudeCounter(Math.toIntExact(count.getRealValue(Long.class)))
                .toOrderOfMagnitude();
        count.setRealValue(orderOfMagnitude);
    }

}
