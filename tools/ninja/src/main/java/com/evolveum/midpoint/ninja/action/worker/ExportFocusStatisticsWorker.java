package com.evolveum.midpoint.ninja.action.worker;

import com.evolveum.midpoint.ninja.action.ExportOptions;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

public class ExportFocusStatisticsWorker extends AbstractWriterConsumerWorker<ExportOptions, ObjectType> {

    public ExportFocusStatisticsWorker(NinjaContext context, ExportOptions options, BlockingQueue<ObjectType> queue, OperationStatus operation) {
        super(context, options, queue, operation);
    }

    Map<Class<?>, StatisticsState> focusTypeStates = new HashMap<>();

    @Override
    protected String getProlog() {
        // FIXME: Overwrite this to write start of XML export
        return NinjaUtils.XML_OBJECTS_PREFIX;
    }

    @Override
    protected void write(Writer writer, ObjectType object) throws SchemaException, IOException {
        // This is only sample state tracking
        var state = getState(object);
        // Maybe here we can remove items explicitly hidden by user?
        state.process(object);
    }

    private StatisticsState getState(ObjectType object) {
        return focusTypeStates.computeIfAbsent(object.getClass(), k -> new StatisticsState(k));
    }

    @Override
    protected String getEpilog() {
        var ret = new StringWriter();
        // FIXME: Here we should write out all statistics to output
        for (Map.Entry<Class<?>, StatisticsState> entry : focusTypeStates.entrySet()) {

            context.getLog().info("Focus Type {}. Processed {}", entry.getKey().getSimpleName(), entry.getValue().totalCount);

        }
        ret.append(NinjaUtils.XML_OBJECTS_SUFFIX);
        return ret.toString();
    }

    private class StatisticsState {

        final Class<?> focusType;
        int totalCount;

        public StatisticsState(Class<?> focusType) {
            this.focusType = focusType;
        }

        public void process(ObjectType object) {
            totalCount++;
            // FIXME: Update processing here
        }
    }
}
