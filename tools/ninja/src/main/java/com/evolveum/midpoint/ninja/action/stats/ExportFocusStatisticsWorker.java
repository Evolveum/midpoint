package com.evolveum.midpoint.ninja.action.stats;

import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.BlockingQueue;

import com.evolveum.midpoint.ninja.action.ExportOptions;
import com.evolveum.midpoint.ninja.action.worker.AbstractWriterConsumerWorker;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemFactory;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismSerializer;
import com.evolveum.midpoint.prism.SerializationOptions;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class ExportFocusStatisticsWorker extends AbstractWriterConsumerWorker<ExportOptions, ObjectType> {
    private static final ItemName C_FOCUS_STATS = new ItemName(SchemaConstants.NS_C, "focusStats");
    private final StatsCounter statsCounter;

    public ExportFocusStatisticsWorker(NinjaContext context, ExportOptions options, BlockingQueue<ObjectType> queue,
            OperationStatus operation, StatsCounter statsCounter) {
        super(context, options, queue, operation);
        this.statsCounter = statsCounter;
    }

    @Override
    protected String getProlog() {
        return "";
    }

    @Override
    protected void write(Writer writer, ObjectType object) throws SchemaException, IOException {
        this.statsCounter.count(object);
    }

    @Override
    protected String getEpilog() {
        try {
            final PrismSerializer<String> serializer = this.context.getPrismContext().xmlSerializer()
                    .options(SerializationOptions.createSerializeForExport());
            final ItemFactory itemFactory = context.getPrismContext().itemFactory();
            final PrismContainer<Containerable> focusStats = itemFactory.createContainer(C_FOCUS_STATS);
            focusStats.add(this.statsCounter.calculate().asPrismContainerValue(itemFactory));
            return serializer.serialize(focusStats);
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
    }

}
