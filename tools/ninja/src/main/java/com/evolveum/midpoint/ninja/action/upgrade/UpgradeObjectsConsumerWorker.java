package com.evolveum.midpoint.ninja.action.upgrade;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.evolveum.midpoint.ninja.action.worker.BaseWorker;
import com.evolveum.midpoint.ninja.impl.Log;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.api.RepoModifyOptions;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class UpgradeObjectsConsumerWorker<T extends ObjectType> extends BaseWorker<UpgradeObjectsOptions, T> {

    private final Set<UUID> skipUpgradeForUUIDs;

    private final Log log;

    public UpgradeObjectsConsumerWorker(
            Set<UUID> skipUpgradeForUUIDs, NinjaContext context, UpgradeObjectsOptions options,
            BlockingQueue<T> queue, OperationStatus operation) {

        super(context, options, queue, operation);

        this.skipUpgradeForUUIDs = skipUpgradeForUUIDs;
        this.log = context.getLog();
    }

    @Override
    public void run() {
        RepositoryService repository = context.getRepository();
        // todo implement

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

    private void processObject(RepositoryService repository, T object)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {

        PrismObject<? extends ObjectType> prismObject = object.asPrismObject();

        if (skipUpgradeForUUIDs.contains(object.getOid())) {
            log.info("Skipping object {} with oid {}", prismObject.getBusinessDisplayName(), object.getOid());
            return;
        }

        ObjectDelta<T> delta = new UpgradeObjectsHelper().upgradeObject(prismObject);

        if (delta != null && !delta.isEmpty()) {
            OperationResult result = new OperationResult("aaaa"); // todo fix operation result name and handling!
            repository.modifyObject(object.getClass(), object.getOid(), delta.getModifications(), result);
        }
    }
}
