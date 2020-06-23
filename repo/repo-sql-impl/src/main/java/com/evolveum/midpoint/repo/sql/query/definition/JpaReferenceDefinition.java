/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query.definition;

import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.query.resolution.DataSearchResult;
import com.evolveum.midpoint.schema.constants.SchemaConstants;

/**
 * @author lazyman
 */
public class JpaReferenceDefinition<T extends JpaReferenceDefinition<T>>
        extends JpaDataNodeDefinition<T> {

    private final JpaEntityPointerDefinition referencedEntityDefinition;

    public JpaReferenceDefinition(
            Class<? extends RObject> jpaClass, Class<? extends RObject> referencedEntityJpaClass) {
        super(jpaClass, null);          // JAXB class not important here
        Validate.notNull(referencedEntityJpaClass, "referencedEntityJpaClass");
        this.referencedEntityDefinition = new JpaEntityPointerDefinition(referencedEntityJpaClass);
    }

    @Override
    protected String getDebugDumpClassName() {
        return "Ref";
    }

    @Override
    public DataSearchResult<?> nextLinkDefinition(ItemPath path, ItemDefinition itemDefinition, PrismContext prismContext) {
        if (ItemPath.isObjectReference(path.first())) {
            // returning artificially created transition definition, used to allow dereferencing target object in a generic way
            return new DataSearchResult<>(
                    new JpaLinkDefinition<>(SchemaConstants.PATH_OBJECT_REFERENCE, "target", null, false, referencedEntityDefinition.getResolvedEntityDefinition()),
                    path.rest());
        } else {
            return null;
        }
    }

    public JpaEntityPointerDefinition getReferencedEntityDefinition() {
        return referencedEntityDefinition;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
        referencedEntityDefinition.accept(visitor);
    }

    @Override
    public String debugDump(int indent) {
        return super.getShortInfo() + ", target=" + getReferencedEntityDefinition();
    }

    @Override
    public String getShortInfo() {
        return super.getShortInfo() + "<" + referencedEntityDefinition.getJpaClassName() + ">";
    }
}
