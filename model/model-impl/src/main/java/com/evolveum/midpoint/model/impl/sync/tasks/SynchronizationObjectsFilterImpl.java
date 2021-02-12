/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks;

import com.evolveum.midpoint.prism.PrismObject;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Specifies which objects are to be synchronized.
 * Currently uses combination of object class, kind, and intent. (No custom filters yet.)
 *
 * Introduced to streamline clumsy filtering by [refined] object class definition, kind, and intent.
 * See e.g. MID-5672.
 */
@Experimental
public class SynchronizationObjectsFilterImpl implements SynchronizationObjectsFilter {

    /**
     * This is either "simple" OCD or refined OCD.
     * FIXME this should be clarified; the current state mirrors objectClassDef information in sync handlers
     */
    @NotNull private final ObjectClassComplexTypeDefinition objectClassDefinition;

    /**
     * Kind, as specified in the task extension. Null means "any kind".
     */
    private final ShadowKindType kind;

    /**
     * Intent, as specified in the task extension. Null means "any intent".
     */
    private final String intent;

    public SynchronizationObjectsFilterImpl(@NotNull ObjectClassComplexTypeDefinition objectClassDefinition, ShadowKindType kind, String intent) {
        this.objectClassDefinition = objectClassDefinition;
        this.kind = kind;
        this.intent = intent;
    }

    @Override
    public boolean matches(@NotNull PrismObject<ShadowType> shadow) {
        return matchesObjectClassName(shadow) && matchesKind(shadow) && matchesIntent(shadow);
    }

    private boolean matchesObjectClassName(@NotNull PrismObject<ShadowType> shadow) {
        return QNameUtil.match(objectClassDefinition.getTypeName(), shadow.asObjectable().getObjectClass());
    }

    /**
     * Does the shadow kind match?
     *
     * Originally we looked also at the kind corresponding to the refined object class definition (if present).
     */
    private boolean matchesKind(PrismObject<ShadowType> shadow) {
        return kind == null || ShadowUtil.getKind(shadow) == kind;
    }

    /**
     * Does the shadow intent match?
     */
    private boolean matchesIntent(PrismObject<ShadowType> shadow) {
        return intent == null || intent.equals(ShadowUtil.getIntent(shadow));
    }
}
