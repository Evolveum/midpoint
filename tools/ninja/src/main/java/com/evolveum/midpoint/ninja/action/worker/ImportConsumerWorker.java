/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
