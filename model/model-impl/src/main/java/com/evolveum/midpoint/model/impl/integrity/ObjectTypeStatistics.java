/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.integrity;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryObjectDiagnosticData;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.util.histogram.Histogram;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author mederly
 */
public class ObjectTypeStatistics {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectTypeStatistics.class);

    private static final int SIZE_HISTOGRAM_STEP = 10000;
    private static final int MAX_SIZE_HISTOGRAM_LENGTH = 100000;        // corresponds to object size of 1 GB

    private final Histogram<ObjectInfo> sizeHistogram = new Histogram<>(SIZE_HISTOGRAM_STEP, MAX_SIZE_HISTOGRAM_LENGTH);

    public void register(PrismObject<ObjectType> object) {
        RepositoryObjectDiagnosticData diag = (RepositoryObjectDiagnosticData) object.getUserData(RepositoryService.KEY_DIAG_DATA);
        if (diag == null) {
            throw new IllegalStateException("No diagnostic data in " + object);
        }
        ObjectInfo info = new ObjectInfo(object.asObjectable());
        long size = diag.getStoredObjectSize();
        LOGGER.trace("Found object: {}: {}", info, size);
        sizeHistogram.register(info, size);
    }

    public String dump(int histogramColumns) {
        return sizeHistogram.dump(histogramColumns);
    }
}
