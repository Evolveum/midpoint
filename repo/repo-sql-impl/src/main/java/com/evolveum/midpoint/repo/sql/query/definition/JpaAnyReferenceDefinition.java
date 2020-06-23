/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query.definition;

/**
 * Specifies "any" reference. In contrast to other JPA definitions, it is not derived by analyzing R-class
 * structure, but created on demand in the process if ItemPath translation.
 * <p>
 * It was created to ensure consistency of resolution mechanism, which should provide
 * HQL property + JPA definition for any item path provided.
 *
 * @author mederly
 */
public class JpaAnyReferenceDefinition extends JpaReferenceDefinition<JpaAnyReferenceDefinition> {

    public JpaAnyReferenceDefinition(Class jpaClass, Class referencedEntityJpaClass) {
        super(jpaClass, referencedEntityJpaClass);
    }

    @Override
    protected String getDebugDumpClassName() {
        return "AnyReference";
    }
}
