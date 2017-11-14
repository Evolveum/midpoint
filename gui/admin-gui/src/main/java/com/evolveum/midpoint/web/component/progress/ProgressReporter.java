/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.progress;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.io.Serializable;

/**
 * @author Viliam Repan (lazyman)
 * @author mederly
 */
public class ProgressReporter implements Serializable {

    private String id;

    // Operation result got from the asynchronous operation (null if async op not yet finished)
    private OperationResult asyncOperationResult;
    private ModelContext<? extends ObjectType> previewResult;

    // configuration properties
    private int refreshInterval;
    private boolean asynchronousExecution;
    private boolean abortEnabled;

    public String getId() {
        return id;
    }

    public OperationResult getAsyncOperationResult() {
        return asyncOperationResult;
    }

    public int getRefreshInterval() {
        return refreshInterval;
    }

    public boolean isAsynchronousExecution() {
        return asynchronousExecution;
    }

    public boolean isAbortEnabled() {
        return abortEnabled;
    }

    public ModelContext<? extends ObjectType> getPreviewResult() {
        return previewResult;
    }

    public void setAsyncOperationResult(OperationResult asyncOperationResult) {
        this.asyncOperationResult = asyncOperationResult;
    }

    public void setPreviewResult(ModelContext<? extends ObjectType> previewResult) {
        this.previewResult = previewResult;
    }

    public void setRefreshInterval(int refreshInterval) {
        this.refreshInterval = refreshInterval;
    }

    public void setAsynchronousExecution(boolean asynchronousExecution) {
        this.asynchronousExecution = asynchronousExecution;
    }

    public void setAbortEnabled(boolean abortEnabled) {
        this.abortEnabled = abortEnabled;
    }
}
