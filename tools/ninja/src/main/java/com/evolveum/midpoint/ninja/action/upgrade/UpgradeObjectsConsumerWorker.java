package com.evolveum.midpoint.ninja.action.upgrade;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.evolveum.midpoint.ninja.action.upgrade.action.UpgradeObjectsOptions;
import com.evolveum.midpoint.ninja.action.worker.BaseWorker;
import com.evolveum.midpoint.ninja.impl.Log;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.api.RepoModifyOptions;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class UpgradeObjectsConsumerWorker<T extends ObjectType> extends BaseWorker<UpgradeObjectsOptions, T> {

    private final Map<UUID, Set<SkipUpgradeItem>> skipUpgradeForOids;

    private final Log log;

    private UpgradeObjectsItemsSummary itemsSummary = new UpgradeObjectsItemsSummary();

    public UpgradeObjectsConsumerWorker(
            Map<UUID, Set<SkipUpgradeItem>> skipUpgradeForOids, NinjaContext context, UpgradeObjectsOptions options,
            BlockingQueue<T> queue, OperationStatus operation) {

        super(context, options, queue, operation);

        this.skipUpgradeForOids = skipUpgradeForOids;
        this.log = context.getLog();
    }

    public UpgradeObjectsItemsSummary getItemsSummary() {
        return itemsSummary;
    }

    @Override
    public void run() {
        RepositoryService repository = context.getRepository();

        try {
            while (!shouldConsumerStop()) {
                T object = null;
                try {
                    object = queue.poll(CONSUMER_POLL_TIMEOUT, TimeUnit.SECONDS);
                    if (object == null) {
                        continue;
                    }

                    processObject(repository, object);

                    operation.incrementTotal();
                } catch (Exception ex) {
                    log.error("Couldn't store object {}, reason: {}", ex, object, ex.getMessage());
                    operation.incrementError();
                }
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            markDone();

            if (isWorkersDone()) {
                operation.finish();
            }
        }
    }

    private void processObject(RepositoryService repository, T object) throws Exception {
        PrismObject prismObject = object.asPrismObject();

        PrismObject cloned = prismObject.clone();
        UpgradeObjectHandler executor = new UpgradeObjectHandler(options, context, skipUpgradeForOids, itemsSummary);
        UpgradeObjectResult result = executor.execute(cloned);
        switch (result) {
            case SKIPPED:
                operation.incrementSkipped();
            case NO_CHANGES:
                return;
            case UPDATED:
                // we'll continue processing
        }

        OperationResult opResult = new OperationResult("Modify object");
        try {
            ObjectDelta<?> delta = prismObject.diff(cloned);
            Collection<? extends ItemDelta<?, ?>> modifications = delta.getModifications();
            RepoModifyOptions opts = modifications.isEmpty() ? RepoModifyOptions.createForceReindex() : new RepoModifyOptions();

            repository.modifyObject(object.getClass(), object.getOid(), delta.getModifications(), opts, opResult);
        } catch (Exception ex) {
            log.error("Couldn't modify object {} ({})", ex, object.getName(), object.getOid());
        } finally {
            opResult.computeStatusIfUnknown();

            if (!opResult.isSuccess()) {
                log.error(
                        "Modification of '{} ({})' didn't finished with success\n{}",
                        object.getName(), object.getOid(), opResult.debugDumpLazily());
            }
        }
    }
}
