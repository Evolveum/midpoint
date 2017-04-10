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

package com.evolveum.midpoint.model.impl.dataModel.dot;

import com.evolveum.midpoint.model.impl.dataModel.model.Relation;
import org.jetbrains.annotations.NotNull;

/**
 * @author mederly
 */
public class DotOtherRelation implements DotRelation {

	@NotNull private final Relation relation;

	public DotOtherRelation(@NotNull Relation relation) {
		this.relation = relation;
	}

	public String getEdgeLabel() {
		return "";
	}

	public String getNodeLabel(String defaultLabel) {
		return null;
	}

	public String getEdgeStyle() {
		return "";
	}

	public String getNodeStyleAttributes() {
		return "";
	}

	public String getEdgeTooltip() {
		return "";
	}

	public String getNodeTooltip() {
		return "";
	}

}
