package com.evolveum.midpoint.ninja.action.upgrade;

import java.lang.reflect.Modifier;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.evolveum.midpoint.ninja.Main;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.validator.ValidationItem;
import com.evolveum.midpoint.schema.validator.ValidationResult;
import com.evolveum.midpoint.util.ClassPathUtil;

public class UpgradeObjectsHandler {

    private static final List<UpgradeObjectProcessor<?>> PROCESSORS;

    static {
        PROCESSORS = initProcessors();
    }

    private static List<UpgradeObjectProcessor<?>> initProcessors() {
        Set<Class<?>> processors = ClassPathUtil.listClasses(Main.class.getPackageName())
                .stream()
                .filter(UpgradeObjectProcessor.class::isAssignableFrom)
                .filter(c -> !Modifier.isAbstract(c.getModifiers()))

                .collect(Collectors.toUnmodifiableSet());

        return processors.stream()
                .map(c -> {
                    try {
                        return (UpgradeObjectProcessor<?>) c.getConstructor().newInstance();
                    } catch (Exception ex) {
                        throw new IllegalStateException("Processor " + c.getName() + " doesn't have constructor without arguments");
                    }
                })
                .sorted(Comparator.comparing(p -> p.getClass().getName()))
                .collect(Collectors.toUnmodifiableList());
    }

    public UpgradeObjectResult upgrade(PrismObject<?> object, ItemPath path) {
        PrismObject cloned = object.clone();

        UpgradeObjectProcessor<?> processor = null;
        for (UpgradeObjectProcessor<?> p : PROCESSORS) {
            if (p.isApplicable(cloned, path)) {
                processor = p;
                break;
            }
        }

        if (processor == null) {
            return null;
        }

        boolean changed = processor.process(cloned);

        UpgradeObjectResult result = new UpgradeObjectResult();
        result.setChanged(changed);
        result.setIdentifier(processor.getIdentifier());
        result.setPhase(processor.getPhase());
        result.setType(processor.getType());
        result.setPriority(processor.getPriority());

        ObjectDelta<?> delta = object.diff(cloned);
        result.setDelta(delta);

        return result;
    }

    public VerificationResult verify(PrismObject<?> object, ValidationResult result) {
        VerificationResult verificationResult = new VerificationResult(result);

        for (ValidationItem item : result.getItems()) {
            UpgradeObjectResult upgrade = upgrade(object, item.getItemPath());
            VerificationResultItem vi = new VerificationResultItem(item, upgrade);

            verificationResult.getItems().add(vi);
        }

        // todo implement
        return verificationResult;
    }
}
