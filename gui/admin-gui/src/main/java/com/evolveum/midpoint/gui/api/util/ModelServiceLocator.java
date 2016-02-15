/**
 * Copyright (c) 2016 Evolveum
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
package com.evolveum.midpoint.gui.api.util;

import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;

/**
 * Interface that allows location of ModelService and ModelInteractionService.
 * Used by GUI components that need to interact with the midPoint IDM model,
 * especially for loading data.
 * Usually implemented by PageBase and similar "central" GUI classes.
 * 
 * @author Radovan Semancik
 */
public interface ModelServiceLocator {
	
	ModelService getModelService();
	
	ModelInteractionService getModelInteractionService();

}
