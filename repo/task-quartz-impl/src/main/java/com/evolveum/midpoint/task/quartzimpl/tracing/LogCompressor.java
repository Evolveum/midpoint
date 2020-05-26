/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
