package com.evolveum.midpoint.ninja.action.upgrade.step;

import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.ninja.Main;
import com.evolveum.midpoint.ninja.action.upgrade.StepResult;
import com.evolveum.midpoint.ninja.action.upgrade.UpgradeObjectProcessor;
import com.evolveum.midpoint.ninja.action.upgrade.UpgradeStep;
import com.evolveum.midpoint.ninja.action.upgrade.UpgradeStepsContext;
import com.evolveum.midpoint.util.ClassPathUtil;

public abstract class UpgradeObjectsStep implements UpgradeStep<StepResult> {

    private final UpgradeStepsContext context;

    public UpgradeObjectsStep(@NotNull UpgradeStepsContext context) {
        this.context = context;
    }

    @Override
    public StepResult execute() throws Exception {
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
