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

import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.opts.ImportOptions;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ImportConsumerWorker extends BaseWorker<ImportOptions, PrismObject> {

    public ImportConsumerWorker(NinjaContext context, ImportOptions options, BlockingQueue<PrismObject> queue,
                                OperationStatus operation, List<ImportConsumerWorker> consumers) {
        super(context, options, queue, operation, consumers);
    }

    @Override
    public void run() {
        ApplicationContext ctx = context.getApplicationContext();
        Protector protector = ctx.getBean(Protector.class);

        try {
            while (!shouldConsumerStop()) {
                PrismObject object = null;
                try {
                    object = queue.poll(CONSUMER_POLL_TIMEOUT, TimeUnit.SECONDS);
                    if (object == null) {
                        continue;
                    }

                    RepoAddOptions opts = createRepoAddOptions(options);

                    if (!opts.isAllowUnencryptedValues()) {
                        CryptoUtil.encryptValues(protector, object);
                    }

                    RepositoryService repository = context.getRepository();
                    repository.addObject(object, opts, new OperationResult("Import object"));

                    operation.incrementTotal();
                } catch (Exception ex) {
                    context.getLog().error("Couldn't add object {}, reason: {}", ex, object, ex.getMessage());
                    operation.incrementError();
                }
            }
        } finally {
            markDone();

            if (isWorkersDone()) {
                operation.finish();
            }
        }
    }

    private RepoAddOptions createRepoAddOptions(ImportOptions options) {
        RepoAddOptions opts = new RepoAddOptions();
        opts.setOverwrite(options.isOverwrite());
        opts.setAllowUnencryptedValues(options.isAllowUnencryptedValues());

        return opts;
    }
}