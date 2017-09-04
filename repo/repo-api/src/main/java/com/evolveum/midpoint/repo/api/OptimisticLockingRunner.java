/**
 * Copyright (c) 2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

/**
 * @author semancik
 *
 */
public class OptimisticLockingRunner<O extends ObjectType, R> {

	private static final Trace LOGGER = TraceManager.getTrace(OptimisticLockingRunner.class);
	protected static final Random RND = new Random();

	private PrismObject<O> object;
	private final OperationResult result;
	private final RepositoryService repositoryService;
	private final int maxNumberOfAttempts;
	private final Integer delayRange;

	private OptimisticLockingRunner(PrismObject<O> object, OperationResult result,
			RepositoryService repositoryService, int maxNumberOfAttempts, Integer delayRange) {
		super();
		this.object = object;
		this.result = result;
		this.repositoryService = repositoryService;
		this.maxNumberOfAttempts = maxNumberOfAttempts;
		this.delayRange = delayRange;
	}

	public PrismObject<O> getObject() {
		return object;
	}

	public R run(RepositoryOperation<O,R> lambda)
			throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		int numberOfAttempts = 0;
		while (true) {
			try {

				return lambda.run(object);

			} catch (PreconditionViolationException e) {
				if (numberOfAttempts < maxNumberOfAttempts) {
					LOGGER.trace("Restarting repository operation due to optimistic locking conflict (attempt {} of {})",
							numberOfAttempts, maxNumberOfAttempts);
					numberOfAttempts++;

					if (delayRange != null) {
						int delay = RND.nextInt(delayRange);
						try {
							Thread.sleep(delay);
						} catch (InterruptedException eint) {
							// nothing to do, just go on
						}
					}

					object = repositoryService.getObject(object.getCompileTimeClass(), object.getOid(), null, result);

				} else {
					LOGGER.trace("Optimistic locking conflict and maximum attempts exceeded ({})",
							maxNumberOfAttempts);
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
