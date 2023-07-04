package com.evolveum.midpoint.ninja.action.upgrade;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.evolveum.midpoint.ninja.action.upgrade.action.UpgradeObjectsOptions;
import com.evolveum.midpoint.ninja.action.worker.BaseWorker;
import com.evolveum.midpoint.ninja.impl.Log;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.util.ConsoleFormat;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.validator.UpgradeObjectsHandler;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class UpgradeObjectsConsumerWorker<T extends ObjectType> extends BaseWorker<UpgradeObjectsOptions, T> {

    private final Map<UUID, Set<String>> skipUpgradeForOids;

    private final Log log;

    public UpgradeObjectsConsumerWorker(
            Map<UUID, Set<String>> skipUpgradeForOids, NinjaContext context, UpgradeObjectsOptions options,
            BlockingQueue<T> queue, OperationStatus operation) {

        super(context, options, queue, operation);

        this.skipUpgradeForOids = skipUpgradeForOids;
        this.log = context.getLog();
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

    private void processObject(RepositoryService repository, T object) {
        PrismObject<? extends ObjectType> prismObject = object.asPrismObject();

        Set<String> identifiers = skipUpgradeForOids.get(UUID.fromString(object.getOid()));
        if (identifiers == null) {
            identifiers = new HashSet<>();
        }


//        if (skipUpgradeForOids.contains(object.getOid())) {
//            log.info(ConsoleFormat.formatInfoMessageWithParameter(
//                    "Skipping object: ", prismObject.getBusinessDisplayName() + "(" + object.getOid() + ")"));
//            return;
//        }
//
//        UpgradeObjectsHandler upgradeHandler = new UpgradeObjectsHandler();
//        UpgradeObjectResult result = upgradeHandler.handle(prismObject);
//
//        if (result.isChanged()) {
//            ObjectDelta<?> delta = result.getDelta();
//
//            OperationResult opResult = new OperationResult("Modify object");
//            try {
//                repository.modifyObject(object.getClass(), object.getOid(), delta.getModifications(), opResult);
//            } catch (Exception ex) {
//                log.error("Couldn't modify object");
//            } finally {
//                opResult.computeStatusIfUnknown();
//
//                // todo if not success, print?
//            }
//        }
    }
}
