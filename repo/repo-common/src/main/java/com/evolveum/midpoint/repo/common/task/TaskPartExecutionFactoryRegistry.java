/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.schema.util.task.TaskPartUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartDefinitionType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Experimental
public class TaskPartExecutionFactoryRegistry {

    private static final Trace LOGGER = TraceManager.getTrace(TaskPartExecutionFactoryRegistry.class);

    private final Collection<RegistryEntry> registeredFactories = ConcurrentHashMap.newKeySet();

    public static PartMatcher partMatcher(Set<String> handlerUris, Set<String> relativeUris, Set<QName> parameterTypes) {
        return new PartMatcher(handlerUris, relativeUris, parameterTypes);
    }

    public static PartMatcher partMatcher(Set<String> handlerUris, Set<QName> parameterTypes) {
        return new PartMatcher(handlerUris, Collections.emptySet(), parameterTypes);
    }

    public void register(PartMatcher partMatcher, TaskPartExecutionFactory factory) {
        registeredFactories.add(new RegistryEntry(partMatcher, factory));
    }

    public void unregister(TaskPartExecutionFactory factory) {
        registeredFactories.removeIf(entry -> entry.factory == factory);
    }

    @NotNull TaskPartExecutionFactory getFactory(@NotNull TaskPartDefinitionType partDef, @Nullable TaskPartExecution parent,
            @NotNull Task task) throws SchemaException {

        // TODO consider parent
        WorkSpec workSpec = getWorkSpec(partDef, task);
        LOGGER.info("Work spec: {} for {}", workSpec, partDef);

        for (RegistryEntry registeredFactory : registeredFactories) {
            if (registeredFactory.matches(workSpec)) {
                return registeredFactory.factory;
            }
        }

        throw new IllegalStateException("Couldn't find task part execution factory for " + workSpec);
    }

    private WorkSpec getWorkSpec(TaskPartDefinitionType partDef, Task task) throws SchemaException {
        WorkSpec workSpec = new WorkSpec();
        while (partDef != null) {
            workSpec.update(partDef);
            partDef = getParent(partDef);
        }
        workSpec.update(task);
        return workSpec;
    }

    private TaskPartDefinitionType getParent(TaskPartDefinitionType partDef) {
        PrismContainer<?> container = (PrismContainer<?>) partDef.asPrismContainerValue().getParent();
        if (container == null) {
            return null;
        }
        PrismContainerValue<?> parent = container.getParent();
        if (parent == null) {
            return null;
        }

        if (TaskPartDefinitionType.class.equals(parent.getCompileTimeClass())) {
            return (TaskPartDefinitionType) parent.asContainerable();
        } else {
            return null;
        }
    }

    public static class WorkSpec {

        private String handlerUri;
        private String relativeHandlerUri;
        private QName configurationTypeName;

        @Override
        public String toString() {
            return "WorkSpec{" +
                    "handlerUri='" + handlerUri + '\'' +
                    ", relativeHandlerUri='" + relativeHandlerUri + '\'' +
                    ", configurationTypeName=" + configurationTypeName +
                    '}';
        }

        public void update(TaskPartDefinitionType partDef) throws SchemaException {
            if (handlerUri == null) {
                handlerUri = partDef.getHandlerUri();
                if (relativeHandlerUri == null) {
                    relativeHandlerUri = partDef.getRelativeHandlerUri();
                } else {
                    // We ignore any further updates to relative handler URI.
                }
            } else {
                // After handler URI is set, we ignore any further updates to handler URI or relative handler URI.
            }

            if (configurationTypeName == null) {
                Collection<QName> typesFound = TaskPartUtil.getPartParametersTypesFound(partDef.getParameters());
                if (!typesFound.isEmpty()) {
                    configurationTypeName = typesFound.iterator().next(); // Ignoring the others
                }
            }
        }

        public void update(Task task) {
            if (handlerUri == null) {
                if (task.getHandlerUri() != null && !task.getHandlerUri().equals(GenericTaskHandler.TEMPORARY_HANDLER_URI)) {
                    handlerUri = task.getHandlerUri();
                }
            }
        }
    }

    private static class PartMatcher {
        @NotNull private final Collection<String> partHandlerUris;
        @NotNull private final Collection<String> relativeUris;
        @NotNull private final Collection<QName> parameterTypes;

        PartMatcher(@NotNull Collection<String> partHandlerUris, @NotNull Collection<String> relativeUris,
                @NotNull Collection<QName> parameterTypes) {
            this.partHandlerUris = partHandlerUris;
            this.relativeUris = relativeUris;
            this.parameterTypes = parameterTypes;
        }

        public boolean matches(WorkSpec workSpec) {
            if (workSpec.handlerUri != null) {
                return partHandlerUris.contains(workSpec.handlerUri);
            } else if (workSpec.configurationTypeName != null) {
                return parameterTypes.contains(workSpec.configurationTypeName);
            } else {
                return false;
            }
        }
    }

    private static class RegistryEntry {
        @NotNull private final PartMatcher partMatcher;
        @NotNull private final TaskPartExecutionFactory factory;

        private RegistryEntry(@NotNull PartMatcher partMatcher,
                @NotNull TaskPartExecutionFactory factory) {
            this.partMatcher = partMatcher;
            this.factory = factory;
        }

        public boolean matches(WorkSpec workSpec) {
            return partMatcher.matches(workSpec);
        }
    }
}
