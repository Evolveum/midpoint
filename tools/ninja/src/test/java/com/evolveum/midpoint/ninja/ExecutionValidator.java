package com.evolveum.midpoint.ninja;

import com.evolveum.midpoint.ninja.impl.NinjaContext;

/**
 * Created by Viliam Repan (lazyman).
 */
@FunctionalInterface
public interface ExecutionValidator {

    void validate(NinjaContext context) throws Exception;
}
