package com.evolveum.midpoint.ninja.action;

import com.evolveum.midpoint.ninja.impl.NinjaException;
import com.evolveum.midpoint.ninja.util.FileReference;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.SerializationOptions;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.api.AggregateQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GenericObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowCorrelationStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

public class ExportShadowStatisticsAction extends RepositoryAction<ExportShadowStatisticsOptions, Void>{

    private static final ItemName F_CORRELATION_SITUATION = new ItemName(SchemaConstants.NS_C, "correlationSituation");
    private static final ItemName F_COUNT = new ItemName(SchemaConstants.NS_C, "count");
    private static final ItemName F_SHADOW_STATISTICS = new ItemName(SchemaConstants.NS_C, "shadowStatistics");

    @Override
    public String getOperationName() {
        return "export-statistics";
    }

    @Override
    public Void execute() throws Exception {
        // FIXME: Here we should do execute
        RepositoryService repository = context.getRepository();

        FileReference fileReference = options.getFilter();
        if (fileReference != null && options.getFilter() == null) {
            throw new NinjaException("Type must be defined");
        }
        int total = 0;
        OperationResult result = new OperationResult("Shadow Statistics");
        var type = ObjectTypes.SHADOW;

        var shadowQuery = NinjaUtils.createObjectQuery(options.getFilter(), context, ShadowType.class);

        var query = AggregateQuery.forType(ShadowType.class)
                .retrieve(ShadowType.F_RESOURCE_REF)
                .retrieve(ShadowType.F_OBJECT_CLASS)
                .retrieve(ShadowType.F_KIND)
                .retrieve(ShadowType.F_INTENT)
                .retrieve(F_CORRELATION_SITUATION, ItemPath.create(ShadowType.F_CORRELATION, ShadowCorrelationStateType.F_SITUATION))
                .count(F_COUNT, ShadowType.F_RESOURCE_REF);
        if (shadowQuery != null) {
            query = query.filter(shadowQuery.getFilter());
        }

        var results = repository.searchAggregate(query, result);


        var writer = NinjaUtils.createWriter(
                options.getOutput(), context.getCharset(), options.isZip(), options.isOverwrite(), context.out);

        var wrapper = context.getPrismContext().itemFactory().createContainer(F_SHADOW_STATISTICS);
        for (PrismContainerValue prismContainerValue : results) {
            try {
                wrapper.addIgnoringEquivalents(prismContainerValue);
            } catch (SchemaException e) {
                log.error("Couldn't add shadow statistics", e);
            }
        }
        var obj = new GenericObjectType();
        obj.asPrismObject().findOrCreateContainer(GenericObjectType.F_EXTENSION).add(wrapper);
        var serializer = context.getPrismContext()
                .xmlSerializer()
                .options(SerializationOptions.createSerializeForExport());
        writer.write(serializer.serialize(obj.asPrismObject()));
        writer.flush();

        return null;
    }



}
