/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.task.quartzimpl.tracing;

import com.evolveum.midpoint.xml.ns._public.common.common_3.LogSegmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;

/**
 * Merges multiple log entries into one. This should improve performance of trace viewing.
 */
class LogCompressor {

    void compressResult(OperationResultType resultBean) {
        resultBean.getLog().forEach(this::compressLogSegment);
        resultBean.getPartialResults().forEach(this::compressResult);
    }

    private void compressLogSegment(LogSegmentType segment) {
        if (!segment.getEntry().isEmpty()) {
            boolean first = true;
            StringBuilder content = new StringBuilder();
            for (String entry : segment.getEntry()) {
                if (first) {
                    first = false;
                } else {
                    content.append('\n');
                }
                content.append(entry);
            }
            segment.getEntry().clear();
            segment.getEntry().add(content.toString());
        }
    }
}
