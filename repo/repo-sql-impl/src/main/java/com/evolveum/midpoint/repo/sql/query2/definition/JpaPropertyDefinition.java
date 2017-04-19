/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.query2.definition;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.query2.resolution.DataSearchResult;

/**
 * @author lazyman
 */
public class JpaPropertyDefinition extends JpaDataNodeDefinition {

    private final boolean lob;
    private final boolean enumerated;
	private final boolean indexed;			// unused now (true if @Index-ed)
	private final boolean count;			// "count"-type variable, like RShadow.pendingOperationCount

    JpaPropertyDefinition(Class jpaClass, Class jaxbClass, boolean lob, boolean enumerated, boolean indexed, boolean count) {
        super(jpaClass, jaxbClass);
        this.lob = lob;
        this.enumerated = enumerated;
        this.indexed = indexed;
        this.count = count;
    }

    public boolean isLob() {
        return lob;
    }

    public boolean isEnumerated() {
        return enumerated;
    }

    public boolean isIndexed() {
        return indexed;
    }

	public boolean isCount() {
		return count;
	}

	@Override
    protected String getDebugDumpClassName() {
        return "Prop";
    }

    @Override
    public DataSearchResult nextLinkDefinition(ItemPath path, ItemDefinition itemDefinition, PrismContext prismContext) {
        // nowhere to come from here
        return null;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(super.getShortInfo());
        if (lob) {
            sb.append(", lob");
        }
        if (enumerated) {
            sb.append(", enumerated");
        }
        if (indexed) {
            sb.append(", indexed");
        }
        if (count) {
            sb.append(", count");
        }
        return sb.toString();
    }
}
