/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.construction;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.assignments.AssignmentPathImpl;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Builder for all the constructions (resource object and persona).
 */
public class AbstractConstructionBuilder
        <AH extends AssignmentHolderType,
        ACT extends AbstractConstructionType,
        EC extends EvaluatedAbstractConstruction<AH>,
        RT extends AbstractConstructionBuilder<AH, ACT, EC, RT>> {

    ACT constructionBean;
    AssignmentPathImpl assignmentPath;
    ObjectType source;
    OriginType originType;
    LensContext<AH> lensContext;
    XMLGregorianCalendar now;
    boolean valid;

    public RT constructionBean(ACT val) {
        constructionBean = val;
        return typedThis();
    }

    public RT assignmentPath(AssignmentPathImpl val) {
        assignmentPath = val;
        return typedThis();
    }

    public RT source(ObjectType val) {
        source = val;
        return typedThis();
    }

    public RT originType(OriginType val) {
        originType = val;
        return typedThis();
    }

    public RT lensContext(LensContext<AH> val) {
        lensContext = val;
        return typedThis();
    }

    public RT now(XMLGregorianCalendar val) {
        now = val;
        return typedThis();
    }

    public RT valid(boolean val) {
        valid = val;
        return typedThis();
    }

    private RT typedThis() {
        //noinspection unchecked
        return (RT) this;
    }
}
