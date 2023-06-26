package com.evolveum.midpoint.ninja.action.upgrade;

import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.stream.Collectors;

import com.evolveum.midpoint.ninja.Main;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.validator.ValidationItem;
import com.evolveum.midpoint.util.ClassPathUtil;

public class UpgradeObjectsHelper {

    private static final Set<UpgradeObjectHandler<?>> HANDLERS;

    static {
        HANDLERS = initHandlers();
    }

    private static Set<UpgradeObjectHandler<?>> initHandlers() {
        Set<Class<?>> processors = ClassPathUtil.listClasses(Main.class.getPackageName())
                .stream()
                .filter(UpgradeObjectHandler.class::isAssignableFrom)
                .filter(c -> !Modifier.isAbstract(c.getModifiers()))
                .collect(Collectors.toUnmodifiableSet());

        return processors.stream()
                .map(c -> {
                    try {
                        return (UpgradeObjectHandler<?>) c.getConstructor().newInstance();
                    } catch (Exception ex) {
                        // todo
                        ex.printStackTrace();

                        return null;
                    }
                })
                .filter(p -> p != null)
                .collect(Collectors.toUnmodifiableSet());
    }

    public ObjectDelta upgradeObject(PrismObject<?> object, ItemPath path) {
        // todo implement
//        for (ValidationItem validationItem : result.getItems()) {
//            for (UpgradeObjectHandler<?> processor : HANDLERS) {
//                if (processor.isApplicable(object, validationItem.getItemPath())) {
//                    // todo finish
//                }
//            }
//        }

        // todo implement
        return null;
    }
}
