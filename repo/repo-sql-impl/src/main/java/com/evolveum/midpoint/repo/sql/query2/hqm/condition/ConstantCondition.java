/*
 * Copyright (c) 2010-2018 Evolveum
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

package com.evolveum.midpoint.repo.sql.query2.hqm.condition;

import com.evolveum.midpoint.repo.sql.query2.hqm.HibernateQuery;
import com.evolveum.midpoint.repo.sql.query2.hqm.RootHibernateQuery;

/**
 * @author mederly
 */
public class ConstantCondition extends Condition {

	private final boolean value;

	public ConstantCondition(RootHibernateQuery rootHibernateQuery, boolean value) {
		super(rootHibernateQuery);
		this.value = value;
	}

	@Override
	public void dumpToHql(StringBuilder sb, int indent) {
		HibernateQuery.indent(sb, indent);
		sb.append(value ? "1=1" : "1=0");
	}
}
