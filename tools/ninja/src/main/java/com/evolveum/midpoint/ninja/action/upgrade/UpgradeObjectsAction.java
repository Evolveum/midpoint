package com.evolveum.midpoint.ninja.action.upgrade;

import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.stream.Collectors;

import com.evolveum.midpoint.ninja.Main;
import com.evolveum.midpoint.ninja.action.Action;
import com.evolveum.midpoint.util.ClassPathUtil;

public abstract class UpgradeObjectsAction extends Action<UpgradeObjectsOptions, Void> {

    @Override
    public Void execute() throws Exception {
//        final VerifyResult verifyResult = context.getResult(VerifyResult.class);
//
//        final File output = verifyResult.getOutput();

        // todo load CSV, only OIDs + state (whether to update)
        // go through all oids that need to be updated
        // if csv not available go through all

        Set<Class<?>> classes = ClassPathUtil.listClasses(Main.class.getPackageName());
        Set<Class<?>> processors = classes.stream()
                .filter(UpgradeObjectProcessor.class::isAssignableFrom)
                .filter(c -> !Modifier.isAbstract(c.getModifiers()))
                .collect(Collectors.toUnmodifiableSet());

        System.out.println("Found " + processors.size() + " upgrade rules");

        return null;
    }
}
