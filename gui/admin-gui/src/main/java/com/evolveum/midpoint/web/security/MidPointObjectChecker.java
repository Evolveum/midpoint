/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security;

import java.util.List;

import org.apache.wicket.core.util.objects.checker.IObjectChecker;
import org.w3c.dom.Document;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 *
 */
public class MidPointObjectChecker implements IObjectChecker {

    private static final Trace LOGGER = TraceManager.getTrace(MidPointObjectChecker.class);

    private String label;

    public MidPointObjectChecker() {
        this(null);
    }

    public MidPointObjectChecker(String label) {
        super();
        this.label = label;
    }

    /* (non-Javadoc)
     * @see org.apache.wicket.core.util.objects.checker.IObjectChecker#check(java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    @Override
    public Result check(Object object) {

        if (label != null) {
            LOGGER.info("CHECK: {}: {}", label, object);
        }

        if (object instanceof PrismObject<?>) {
            return checkObject((PrismObject<? extends ObjectType>)object);
        } else if (object instanceof ObjectType) {
            return checkObject(((ObjectType)object).asPrismObject());
        } else if (object instanceof Document) {
            return new Result( Result.Status.FAILURE, "Storage of DOM documents not allowed: "+object);
//            LOGGER.warn("Attempt to serialize DOM element: {}", DOMUtil.getQName((Element)object));
//        } else if (object instanceof Element) {
//            return new Result( Result.Status.FAILURE, "Storage of DOM elements not allowed: "+object);
////            LOGGER.warn("Attempt to serialize DOM element: {}", DOMUtil.getQName((Element)object));

        // JAXBElement: expression evaluator in expressions, it is JAXBElement even in prism objects
//        } else if (object instanceof JAXBElement) {
//            return new Result( Result.Status.FAILURE, "Storage of JAXB elements not allowed: "+object);
//            LOGGER.warn("Attempt to serialize DOM element: {}", DOMUtil.getQName((Element)object));
        }

        return Result.SUCCESS;
    }



    private <O extends ObjectType> Result checkObject(PrismObject<O> object) {

        LOGGER.info("Check for serialization of prism object: {}", object);
//        if (object.canRepresent(ResourceType.class)) {
//            return new Result( Result.Status.FAILURE, "Storage of ResourceType objects not allowed: "+object);
//        }

        return Result.SUCCESS;
    }



    /* (non-Javadoc)
     * @see org.apache.wicket.core.util.objects.checker.IObjectChecker#getExclusions()
     */
    @Override
    public List<Class<?>> getExclusions() {
        return null;
    }

}
