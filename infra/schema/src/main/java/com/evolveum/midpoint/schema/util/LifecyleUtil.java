/**
 * Copyright (c) 2018 Evolveum
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
package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LifecycleStateModelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LifecycleStateType;

/**
 * @author semancik
 *
 */
public class LifecyleUtil {
	
	public static LifecycleStateType findStateDefinition(LifecycleStateModelType lifecycleStateModel, String targetLifecycleState) {
		if (lifecycleStateModel == null) {
			return null;
		}
		if (targetLifecycleState == null) {
			targetLifecycleState = SchemaConstants.LIFECYCLE_ACTIVE;
		}
		for (LifecycleStateType stateType: lifecycleStateModel.getState()) {
    		if (targetLifecycleState.equals(stateType.getName())) {
    			return stateType;
    		}
		}
		return null;
	}

}
