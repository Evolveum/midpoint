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

package com.evolveum.midpoint.ninja.action.worker;

import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.opts.ImportOptions;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ImportConsumerWorker extends BaseWorker<ImportOptions, PrismObject> {

    private OperationStatus operation;

    public ImportConsumerWorker(NinjaContext context, ImportOptions options, BlockingQueue<PrismObject> queue,
                                OperationStatus operation) {
        super(context, options, queue);

        this.operation = operation;
    }

    @Override
    public void run() {
        while (!shouldStop()) {
            PrismObject object = null;
            try {
                object = queue.poll(2, TimeUnit.SECONDS);
                if (object == null) {
                    continue;
                }

                RepositoryService repository = context.getRepository();
                RepoAddOptions opts = createRepoAddOptions(options);

                repository.addObject(object, opts, new OperationResult("Import object"));

                operation.incrementCount();
            } catch (Exception ex) {
                context.getLog().error("Couldn't add object {}, reason: {}", ex, object.toString(), ex.getMessage());
            }
        }
    }

    private boolean shouldStop() {
        if (operation.isFinished()) {
            return true;
        }

        if (operation.isStarted()) {
            return false;
        }

        if (operation.isProducerFinished() && !queue.isEmpty()) {
            return false;
        }

        return true;
    }

    private RepoAddOptions createRepoAddOptions(ImportOptions options) {
        RepoAddOptions opts = new RepoAddOptions();
        opts.setOverwrite(options.isOverwrite());
        opts.setAllowUnencryptedValues(options.isAllowUnencryptedValues());

        return opts;
    }
}