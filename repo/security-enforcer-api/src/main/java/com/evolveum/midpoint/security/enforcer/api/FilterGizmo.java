/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.security.enforcer.api;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.security.api.Authorization;

import javax.xml.namespace.QName;

/**
 * Gizmo (device,gadget,thing) that handles search filters and other filter-like things during security processing of search filters.
 *
 * Normal authorization evaluation will work directly with search filters. But there are other cases when we want to work with
 * something else. For example, we may want to divide search filters based on relation, e.g. for the purposes of evaluating
 * assignable roles. We may also want to divide the filters according to other criteria. Those "gizmos" can be used to
 * do that.
 *
 * The gizmo could also be used to annotate the resulting filter. E.g. we might be able to put name of the authorization into
 * each filter for diagnostic purposes.
 *
 * @param <F> SearchFilter or other filter-like things (e.g. RoleSelectionSpecification)
 */
public interface FilterGizmo<F> {

    F and (F a, F b);

    F or (F a, F b);

    F not(F subfilter);

    F adopt(ObjectFilter objectFilter, Authorization autz);

    F createDenyAll();

    boolean isAll(F filter);

    boolean isNone(F filter);

    F simplify(F filter);

    ObjectFilter getObjectFilter(F filter);

    String debugDumpFilter(F filter, int indent);
}
