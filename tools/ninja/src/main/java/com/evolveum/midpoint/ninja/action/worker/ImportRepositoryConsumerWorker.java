/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action.worker;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.springframework.context.ApplicationContext;

import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.action.ImportOptions;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ImportRepositoryConsumerWorker extends BaseWorker<ImportOptions, ObjectType> {

    public ImportRepositoryConsumerWorker(
            NinjaContext context, ImportOptions options, BlockingQueue<ObjectType> queue,
            OperationStatus operation, List<ImportRepositoryConsumerWorker> consumers) {
        super(context, options, queue, operation, consumers);
    }

    @Override
    public void run() {
        ApplicationContext ctx = context.getApplicationContext();
        Protector protector = ctx.getBean(Protector.class);

        try {
            while (!shouldConsumerStop()) {
                ObjectType object = null;
                try {
                    object = queue.poll(CONSUMER_POLL_TIMEOUT, TimeUnit.SECONDS);
                    if (object == null) {
                        continue;
                    }
                    PrismObject<? extends ObjectType> prismObject = object.asPrismObject();

                    RepoAddOptions opts = createRepoAddOptions(options);

                    if (!opts.isAllowUnencryptedValues()) {
                        CryptoUtil.encryptValues(protector, prismObject);
                    }

                    RepositoryService repository = context.getRepository();
                    repository.addObject(prismObject, opts, new OperationResult("Import object"));

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
