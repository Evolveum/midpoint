package com.evolveum.midpoint.ninja.action.upgrade;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.evolveum.midpoint.ninja.action.upgrade.action.UpgradeObjectsOptions;
import com.evolveum.midpoint.ninja.action.worker.BaseWorker;
import com.evolveum.midpoint.ninja.impl.Log;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.validator.ObjectUpgradeValidator;
import com.evolveum.midpoint.schema.validator.UpgradeValidationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
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
        PrismObject prismObject = object.asPrismObject();

        // todo skip upgrade for specific objects based on CSV report
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

        ObjectUpgradeValidator validator = new ObjectUpgradeValidator(context.getPrismContext());
        UpgradeValidationResult result = validator.validate(prismObject);
        if (!result.hasChanges()) {
            return;
        }

        PrismObject cloned = prismObject.clone();
        result.getItems().forEach(item -> {
            if (!item.isChanged()) {
                return;
            }

            if (!matchesOption(options.getIdentifiers(), item.getIdentifier())) {
                return;
            }

            if (!matchesOption(options.getTypes(), item.getType())) {
                return;
            }

            if (!matchesOption(options.getPhases(), item.getPhase())) {
                return;
            }

            if (!matchesOption(options.getPriorities(), item.getPriority())) {
                return;
            }

            try {
                ObjectDelta delta = item.getDelta();
                if (!delta.isEmpty()) {
                    delta.applyTo(cloned);
                }
            } catch (SchemaException ex) {
                // todo error handling
                ex.printStackTrace();
            }
        });

        OperationResult opResult = new OperationResult("Modify object");
        try {
            ObjectDelta<?> delta = cloned.diff(prismObject);
            repository.modifyObject(object.getClass(), object.getOid(), delta.getModifications(), opResult);
        } catch (Exception ex) {
            log.error("Couldn't modify object");
        } finally {
            opResult.computeStatusIfUnknown();

            // todo if not success, print?
        }
    }

    private <T> boolean matchesOption(List<T> options, T option) {
        if (options == null || options.isEmpty()) {
            return true;
        }

        return options.stream().anyMatch(o -> o.equals(option));
    }
}
