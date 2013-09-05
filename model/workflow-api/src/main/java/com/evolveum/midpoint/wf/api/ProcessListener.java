/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.wf.api;

import com.evolveum.midpoint.schema.result.OperationResult;

import java.util.Map;

/**
 * @author mederly
 */
public interface ProcessListener {

    void onProcessInstanceStart(String instanceName, Map<String, Object> variables, OperationResult result);

    // beware, 'decision' depends on WF_ANSWER process variable, which may or may not be present!
    void onProcessInstanceEnd(String instanceName, Map<String, Object> variables, String decision, OperationResult result);

}
