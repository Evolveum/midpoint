/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
	private static final int MAX_SIZE_HISTOGRAM_LENGTH = 100000;		// corresponds to object size of 1 GB

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
