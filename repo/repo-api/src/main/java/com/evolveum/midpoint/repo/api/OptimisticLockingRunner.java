/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.api;

import java.util.Random;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

/**
 * @author semancik
 */
public final class OptimisticLockingRunner<O extends ObjectType, R> {

    private static final Trace LOGGER = TraceManager.getTrace(OptimisticLockingRunner.class);
    private static final Random RND = new Random();

    @NotNull private PrismObject<O> object;
    private final OperationResult result;
    private final RepositoryService repositoryService;
    private final int maxNumberOfAttempts;
    private final Integer delayRange;

    private OptimisticLockingRunner(@NotNull PrismObject<O> object, OperationResult result,
            RepositoryService repositoryService, int maxNumberOfAttempts, Integer delayRange) {
        this.object = requireNonNull(object);
        this.result = result;
        this.repositoryService = repositoryService;
        this.maxNumberOfAttempts = maxNumberOfAttempts;
        this.delayRange = delayRange;
    }

    public @NotNull PrismObject<O> getObject() {
        return object;
    }

    public R run(RepositoryOperation<O,R> lambda)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        int numberOfAttempts = 0;
        while (true) {
            try {
                R ret = lambda.run(object);
                LOGGER.trace("Finished repository operation (attempt {} of {})", numberOfAttempts, maxNumberOfAttempts);
                return ret;
            } catch (PreconditionViolationException e) {
                if (numberOfAttempts < maxNumberOfAttempts) {
                    LOGGER.trace("Restarting repository operation due to optimistic locking conflict (attempt {} of {})",
                            numberOfAttempts, maxNumberOfAttempts);
                    numberOfAttempts++;

                    if (delayRange != null) {
                        int delay = RND.nextInt(delayRange);
                        try {
                            //noinspection BusyWait
                            Thread.sleep(delay);
                        } catch (InterruptedException ie) {
                            // nothing to do, just go on
                        }
                    }

                    object = repositoryService.getObject(object.getCompileTimeClass(), object.getOid(), null, result);

                } else {
                    LOGGER.trace("Optimistic locking conflict and maximum attempts exceeded ({})", maxNumberOfAttempts);
                    throw new SystemException("Repository optimistic locking conflict and maximum attempts exceeded");
                }
            }
        }
    }

    public static class Builder<O extends ObjectType,R> {

        private PrismObject<O> object;
        private OperationResult result;
        private RepositoryService repositoryService;
        private int maxNumberOfAttempts;
        private Integer delayRange;

        public Builder<O,R> object(PrismObject<O> object) {
            this.object = object;
            return this;
        }

        public Builder<O,R> result(OperationResult result) {
            this.result = result;
            return this;
        }

        public Builder<O,R> repositoryService(RepositoryService repositoryService) {
            this.repositoryService = repositoryService;
            return this;
        }

        public Builder<O,R> maxNumberOfAttempts(int maxNumberOfAttempts) {
            this.maxNumberOfAttempts = maxNumberOfAttempts;
            return this;
        }

        public Builder<O,R> delayRange(Integer delayRange) {
            this.delayRange = delayRange;
            return this;
        }

        public OptimisticLockingRunner<O,R> build() {
            return new OptimisticLockingRunner<>(object, result, repositoryService, maxNumberOfAttempts, delayRange);
        }
    }
}
