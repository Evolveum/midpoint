/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.ninja.action;

import java.util.concurrent.BlockingQueue;

import com.evolveum.midpoint.ninja.action.worker.ExportConsumerWorker;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.opts.ExportOptions;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.PrismObject;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ExportRepositoryAction extends AbstractRepositorySearchAction<ExportOptions> {

    @Override
	protected String getOperationShortName() {
		return "export";
	}

	@Override
	protected Runnable createConsumer(BlockingQueue<PrismObject> queue, OperationStatus operation) {
		return new ExportConsumerWorker(context, options, queue, operation);
	}

}
