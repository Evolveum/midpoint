/*
 * Copyright (c) 2010-2019 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import com.evolveum.midpoint.provisioning.ucf.api.Change;
import com.evolveum.midpoint.provisioning.ucf.api.ChangeHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
class CollectingChangeHandler implements ChangeHandler {
    private List<Change> changes = new ArrayList<>();

    @Override
    public boolean handleChange(Change change, OperationResult result) {
        changes.add(change);
        return true;
    }

    @Override
    public boolean handleError(@Nullable Change change, @NotNull Throwable exception, @NotNull OperationResult result) {
        return false;
    }

    public List<Change> getChanges() {
        return changes;
    }
}
