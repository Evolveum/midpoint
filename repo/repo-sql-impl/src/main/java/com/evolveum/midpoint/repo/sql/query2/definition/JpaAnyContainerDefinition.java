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

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.any.RAnyConverter;
import com.evolveum.midpoint.repo.sql.data.common.type.RObjectExtensionType;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.resolution.DataSearchResult;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author lazyman
 */
public class JpaAnyContainerDefinition extends JpaDataNodeDefinition {

    public JpaAnyContainerDefinition(Class jpaClass) {
        super(jpaClass, null);
    }

    @Override
    protected String getDebugDumpClassName() {
        return "Any";
    }

    @Override
    public DataSearchResult nextLinkDefinition(ItemPath path, ItemDefinition itemDefinition, PrismContext prismContext) throws QueryException {
        if (ItemPath.asSingleName(path) == null) {
            throw new QueryException("Couldn't resolve paths other than those in the form of single name in extension/attributes container: " + path);
        }
        if (itemDefinition == null) {
            throw new QueryException("Couldn't resolve dynamically defined item path '" + path + "' without proper definition");
        }

        CollectionSpecification collSpec = itemDefinition.isSingleValue() ? null : new CollectionSpecification();
		String jpaName;        // longs, strings, ...
		JpaDataNodeDefinition jpaNodeDefinition;
        if (itemDefinition instanceof PrismPropertyDefinition) {
            try {
                jpaName = RAnyConverter.getAnySetType(itemDefinition, prismContext);
            } catch (SchemaException e) {
                throw new QueryException(e.getMessage(), e);
            }
			jpaNodeDefinition = new JpaAnyPropertyDefinition(Object.class, null);      // TODO
        } else if (itemDefinition instanceof PrismReferenceDefinition) {
        	jpaName = "references";
			jpaNodeDefinition = new JpaAnyReferenceDefinition(Object.class, RObject.class);
        } else {
            throw new QueryException("Unsupported 'any' item: " + itemDefinition);
        }
		JpaLinkDefinition<?> linkDefinition = new JpaAnyItemLinkDefinition(itemDefinition, jpaName, collSpec, getOwnerType(), jpaNodeDefinition);
		return new DataSearchResult<>(linkDefinition, ItemPath.EMPTY_PATH);

	}

    // assignment extension has no ownerType, but virtual (object extension / shadow attributes) do have
    protected RObjectExtensionType getOwnerType() {
        return null;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String debugDump(int indent) {
        return super.getShortInfo();
    }
}
