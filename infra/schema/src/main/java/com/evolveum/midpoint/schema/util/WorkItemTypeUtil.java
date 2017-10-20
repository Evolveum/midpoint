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

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemOutputType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemType;

/**
 * @author mederly
 */
public class WorkItemTypeUtil {

	public static AbstractWorkItemOutputType getOutput(AbstractWorkItemType workItem) {
		return workItem != null ? workItem.getOutput() : null;
	}

	public static String getOutcome(AbstractWorkItemType workItem) {
		return getOutcome(getOutput(workItem));
	}

	public static String getComment(AbstractWorkItemType workItem) {
		return getComment(getOutput(workItem));
	}

	public static String getComment(AbstractWorkItemOutputType output) {
		return output != null ? output.getComment() : null;
	}

	public static byte[] getProof(AbstractWorkItemType workItem) {
		return getProof(getOutput(workItem));
	}

	public static byte[] getProof(AbstractWorkItemOutputType output) {
		return output != null ? output.getProof() : null;
	}

	public static String getOutcome(AbstractWorkItemOutputType output) {
		return output != null ? output.getOutcome() : null;
	}

}
