/*
 * Copyright (c) 2010-2014 Evolveum
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

package com.evolveum.midpoint.wf.impl.processors.primary;

/**
 * Process variables to be used in processes used with the PrimaryChangeProcessor.
 *
 * @author mederly
 */
public class PcpProcessVariableNames {

    // Java class name of the process aspect [String]
    public static final String VARIABLE_CHANGE_ASPECT = "changeAspect";

//    // Object that is about to be added (for ADD operation). [ObjectType]
//    public static final String VARIABLE_MIDPOINT_OBJECT_TO_BE_ADDED = "midPointObjectToBeAdded";

//    // XML representation of the deltas to be approved. (Note that technically a process
//    // can approve more than one focus/projection deltas; if necessary, this variable would have to be changed.)
//    // [StringHolder]
//    public static final String VARIABLE_MIDPOINT_OBJECT_TREE_DELTAS = "midPointObjectTreeDeltas";
}
