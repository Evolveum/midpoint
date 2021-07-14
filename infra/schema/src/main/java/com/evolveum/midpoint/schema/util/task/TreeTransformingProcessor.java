/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task;

import com.evolveum.midpoint.schema.util.task.ActivityTreeUtil.ActivityStateTransformer;
import com.evolveum.midpoint.util.TreeNode;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

class TreeTransformingProcessor<X> implements ActivityTreeUtil.ActivityStateProcessor {

    @NotNull private final ActivityStateTransformer<X> transformer;
    @NotNull private TreeNode<X> currentNode;

    TreeTransformingProcessor(@NotNull ActivityStateTransformer<X> transformer, @NotNull TreeNode<X> root) {
        this.transformer = transformer;
        this.currentNode = root;
    }

    @Override
    public void process(@NotNull ActivityPath path, @Nullable ActivityStateType state,
            @Nullable List<ActivityStateType> workerStates, @NotNull TaskType task) {
        currentNode.setUserObject(
                transformer.transform(path, state, workerStates, task));
    }

    @Override
    public void toNewChild(@NotNull ActivityStateType childState) {
        TreeNode<X> child = new TreeNode<>();
        currentNode.add(child);
        currentNode = child;
    }

    @Override
    public void toParent() {
        currentNode = currentNode.getParent();
    }
}
