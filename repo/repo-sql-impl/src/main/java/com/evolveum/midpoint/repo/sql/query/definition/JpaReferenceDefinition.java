/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query.definition;

import java.util.Objects;
import java.util.Optional;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ObjectReferencePathSegment;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.query.QueryDefinitionRegistry;
import com.evolveum.midpoint.repo.sql.query.resolution.DataSearchResult;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
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
        Objects.requireNonNull(referencedEntityJpaClass, "referencedEntityJpaClass");
        this.referencedEntityDefinition = new JpaEntityPointerDefinition(referencedEntityJpaClass);
    }

    @Override
    protected String getDebugDumpClassName() {
        return "Ref";
    }

    @Override
    public DataSearchResult<?> nextLinkDefinition(ItemPath path, ItemDefinition itemDefinition, PrismContext prismContext) throws QueryException {
        var first = path.first();
        var rest = path.rest();
        if (ItemPath.isObjectReference(first)) {
            // returning artificially created transition definition, used to allow dereferencing target object in a generic way
            // Here we could use type hint?
            JpaEntityDefinition resolvedEntityDef = referencedEntityDefinition.getResolvedEntityDefinition();
            if (first instanceof ObjectReferencePathSegment) {
                Optional<QName> typeHint = ((ObjectReferencePathSegment) first).typeHint();

                if (typeHint.isPresent()) {
                    // Now we need to find RObject class for type hint
                    // And now, we somehow need resolvedEntityDefinition

                    // We have type hint, first lets try resolve in original definition
                    DataSearchResult<?> nextDef = resolvedEntityDef.nextLinkDefinition(rest, itemDefinition, prismContext);
                    // If we did not found item using original entity definition, we try to use type hint
                    if (nextDef == null) {
                        resolvedEntityDef = QueryDefinitionRegistry.getInstance().findEntityDefinition(typeHint.get());
                    }
                }

            }

            //
            return new DataSearchResult<>(
                    new JpaLinkDefinition<>(SchemaConstants.PATH_OBJECT_REFERENCE, "target", null, false, resolvedEntityDef),
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
