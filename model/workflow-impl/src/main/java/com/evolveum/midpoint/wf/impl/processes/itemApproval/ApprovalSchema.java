/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.wf.impl.processes.itemApproval;

import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalSchemaType;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * TODO throw away this class completely
 *
 * @author mederly
 */
public class ApprovalSchema implements Serializable {

	private static final long serialVersionUID = 4756235518499474429L;

	@NotNull private final List<ApprovalLevel> levels = new ArrayList<>();

	public ApprovalSchema(ApprovalSchemaType schema) {
		WfContextUtil.checkLevelsOrderingStrict(schema);
		schema.getLevel().forEach(level -> levels.add(new ApprovalLevel(level.getOrder())));
	}

	@SuppressWarnings("WeakerAccess")
	@NotNull
	public List<ApprovalLevel> getLevels() {
		return levels;
	}

	@Override
	public String toString() {
		return "ApprovalSchema{levels: " + levels.size() + '}';
	}
}
