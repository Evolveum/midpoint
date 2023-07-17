package com.evolveum.midpoint.schema.validator;

import java.lang.reflect.Modifier;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.ClassPathUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class UpgradeProcessor {

    public static final List<UpgradeObjectProcessor<?>> PROCESSORS;

    static {
        PROCESSORS = initProcessors();
    }

    public static <T extends ObjectType> UpgradeObjectProcessor<T> getProcessor(String identifier) {
        return (UpgradeObjectProcessor<T>) PROCESSORS.stream()
                .filter(p -> Objects.equals(identifier, p.getIdentifier()))
                .findFirst()
                .orElse(null);
    }

    private static List<UpgradeObjectProcessor<?>> initProcessors() {
        Set<Class<?>> processors = ClassPathUtil.listClasses("com.evolveum.midpoint")
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

    private <T extends ObjectType> UpgradeValidationItem process(PrismObject<T> object, ValidationItem item) {
        ItemPath path = item.getItemPath();

        PrismObject<T> cloned = object.clone();

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

        boolean changed = processor.process((PrismObject) cloned, item.getItemPath());

        UpgradeValidationItem result = new UpgradeValidationItem(item);
        result.setChanged(changed);
        result.setIdentifier(processor.getIdentifier());
        result.setPhase(processor.getPhase());
        result.setType(processor.getType());
        result.setPriority(processor.getPriority());

        ObjectDelta<?> delta = object.diff(cloned);
        result.setDelta(delta);

        return result;
    }

    public <T extends ObjectType> UpgradeValidationResult process(PrismObject<T> object, ValidationResult result) {
        UpgradeValidationResult verificationResult = new UpgradeValidationResult(result);

        for (ValidationItem item : result.getItems()) {
            UpgradeValidationItem upgrade = process(object, item);
            if (upgrade != null) {
                verificationResult.getItems().add(upgrade);
            }
        }

        return verificationResult;
    }
}
