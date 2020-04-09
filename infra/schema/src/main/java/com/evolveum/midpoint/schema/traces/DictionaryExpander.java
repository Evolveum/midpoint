/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.traces;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.Visitable;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TraceDictionaryEntryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TraceDictionaryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TracingOutputType;

/**
 * Expands object references (from pointers to dictionary to full objects).
 */
@Experimental
public class DictionaryExpander {

    private static final Trace LOGGER = TraceManager.getTrace(DictionaryExpander.class);

    private final TracingOutputType tracingOutput;

    DictionaryExpander(TracingOutputType tracingOutput) {
        this.tracingOutput = tracingOutput;
    }

    public void expand() {
        long start = System.currentTimeMillis();
        if (tracingOutput != null && tracingOutput.getResult() != null) {
            expandDictionary(tracingOutput.getResult(), new ExpandingVisitor(tracingOutput.getDictionary()));
        }
        LOGGER.info("Dictionary expanded in {} milliseconds", System.currentTimeMillis() - start);
    }

    @SuppressWarnings("rawtypes")
    private static class ExpandingVisitor implements Visitor {

        private final TraceDictionaryType dictionary;

        private ExpandingVisitor(TraceDictionaryType dictionary) {
            this.dictionary = dictionary;
        }

        @Override
        public void visit(Visitable visitable) {
            if (visitable instanceof PrismReferenceValue) {
                PrismReferenceValue refVal = (PrismReferenceValue) visitable;
                if (refVal.getObject() == null && refVal.getOid() != null && refVal.getOid().startsWith(SchemaConstants.TRACE_DICTIONARY_PREFIX)) {
                    String id = refVal.getOid().substring(SchemaConstants.TRACE_DICTIONARY_PREFIX.length());
                    TraceDictionaryEntryType entry = findEntry(id);
                    if (entry == null) {
                        LOGGER.error("No dictionary entry #{}", id);
                    } else if (entry.getObject() == null) {
                        LOGGER.error("No object in dictionary entry #{}", id);
                    } else if (entry.getObject().asReferenceValue().getObject() == null) {
                        LOGGER.error("No embedded object in dictionary entry #{}", id);
                    } else {
                        PrismObject object = entry.getObject().asReferenceValue().getObject();
                        refVal.setObject(object);
                        refVal.setOid(object.getOid());
                    }
                }
            }
        }

        private TraceDictionaryEntryType findEntry(String id) {
            for (TraceDictionaryEntryType entry : dictionary.getEntry()) {
                String qualifiedId = entry.getOriginDictionaryId() + ":" + entry.getIdentifier();
                if (qualifiedId.equals(id)) {
                    return entry;
                }
            }
            return null;
        }
    }

    private void expandDictionary(OperationResultType resultBean, ExpandingVisitor expandingVisitor) {
        resultBean.getTrace().forEach(trace -> trace.asPrismContainerValue().accept(expandingVisitor));
        resultBean.getPartialResults().forEach(partialResult -> expandDictionary(partialResult, expandingVisitor));
    }
}
