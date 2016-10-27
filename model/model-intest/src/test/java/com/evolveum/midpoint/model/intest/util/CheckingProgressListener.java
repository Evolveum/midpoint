/**
 * Copyright (c) 2016 Evolveum
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
package com.evolveum.midpoint.model.intest.util;

import com.evolveum.midpoint.model.api.ProgressInformation;
import com.evolveum.midpoint.model.api.ProgressListener;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 *
 */
public class CheckingProgressListener implements ProgressListener {

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.model.api.ProgressListener#onProgressAchieved(com.evolveum.midpoint.model.api.context.ModelContext, com.evolveum.midpoint.model.api.ProgressInformation)
	 */
	@Override
	public void onProgressAchieved(ModelContext modelContext, ProgressInformation progressInformation) {
		LensContext<ObjectType> lensContext = (LensContext<ObjectType>)modelContext;
		lensContext.checkConsistence();
		for (LensProjectionContext projectionContext: lensContext.getProjectionContexts()) {
			// MID-3213
			assert projectionContext.getResourceShadowDiscriminator().getResourceOid() != null;
		}
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.model.api.ProgressListener#isAbortRequested()
	 */
	@Override
	public boolean isAbortRequested() {
		return false;
	}

}
