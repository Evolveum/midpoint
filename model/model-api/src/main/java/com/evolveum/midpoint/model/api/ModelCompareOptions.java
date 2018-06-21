/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.model.api;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ModelCompareOptionsType;

import java.io.Serializable;
import java.util.List;

/**
 * Options to be used for compareObject calls.
 *
 * EXPERIMENTAL
 *
 * @author mederly
 *
 */
public class ModelCompareOptions implements Serializable, Cloneable {

	/**
	 * Computes current-to-provided delta. ("Current" means the object that is currently available in the midPoint.)
	 */
	Boolean computeCurrentToProvided;

	/**
	 * Computes provided-to-current delta.
	 */
	Boolean computeProvidedToCurrent;

	/**
	 * Returns the normalized version of provided object.
	 */
	Boolean returnNormalized;

	/**
	 * Returns the current version of provided object.
	 */
	Boolean returnCurrent;

	/**
	 * Should the items marked as operational be ignored?
	 */
	Boolean ignoreOperationalItems;

	public Boolean getComputeCurrentToProvided() {
		return computeCurrentToProvided;
	}

	public void setComputeCurrentToProvided(Boolean computeCurrentToProvided) {
		this.computeCurrentToProvided = computeCurrentToProvided;
	}

	public static boolean isComputeCurrentToProvided(ModelCompareOptions options) {
		return options != null && options.computeCurrentToProvided != null && options.computeCurrentToProvided;
	}

	public Boolean getComputeProvidedToCurrent() {
		return computeProvidedToCurrent;
	}

	public void setComputeProvidedToCurrent(Boolean computeProvidedToCurrent) {
		this.computeProvidedToCurrent = computeProvidedToCurrent;
	}

	public static boolean isComputeProvidedToCurrent(ModelCompareOptions options) {
		return options != null && options.computeProvidedToCurrent != null && options.computeProvidedToCurrent;
	}

	public Boolean getReturnNormalized() {
		return returnNormalized;
	}

	public void setReturnNormalized(Boolean returnNormalized) {
		this.returnNormalized = returnNormalized;
	}

	public static boolean isReturnNormalized(ModelCompareOptions options) {
		return options != null && options.returnNormalized != null && options.returnNormalized;
	}

	public Boolean getReturnCurrent() {
		return returnCurrent;
	}

	public void setReturnCurrent(Boolean returnCurrent) {
		this.returnCurrent = returnCurrent;
	}

	public static boolean isReturnCurrent(ModelCompareOptions options) {
		return options != null && options.returnCurrent != null && options.returnCurrent;
	}

	public Boolean getIgnoreOperationalItems() {
		return ignoreOperationalItems;
	}

	public void setIgnoreOperationalItems(Boolean ignoreOperationalItems) {
		this.ignoreOperationalItems = ignoreOperationalItems;
	}

	public static boolean isIgnoreOperationalItems(ModelCompareOptions options) {
		return options != null && options.ignoreOperationalItems != null && options.ignoreOperationalItems;
	}

	public ModelCompareOptionsType toModelCompareOptionsType() {
		ModelCompareOptionsType retval = new ModelCompareOptionsType();
        retval.setComputeCurrentToProvided(computeCurrentToProvided);
		retval.setComputeProvidedToCurrent(computeProvidedToCurrent);
		retval.setReturnNormalized(returnNormalized);
		retval.setReturnCurrent(returnCurrent);
		retval.setIgnoreOperationalItems(ignoreOperationalItems);
        return retval;
    }

    public static ModelCompareOptions fromModelCompareOptionsType(ModelCompareOptionsType type) {
        if (type == null) {
            return null;
        }
        ModelCompareOptions retval = new ModelCompareOptions();
        retval.setComputeProvidedToCurrent(type.isComputeProvidedToCurrent());
		retval.setComputeCurrentToProvided(type.isComputeCurrentToProvided());
		retval.setReturnNormalized(type.isReturnNormalized());
		retval.setReturnCurrent(type.isReturnCurrent());
		retval.setIgnoreOperationalItems(type.isIgnoreOperationalItems());
        return retval;
    }

    public static ModelCompareOptions fromRestOptions(List<String> options){
    	if (options == null || options.isEmpty()){
    		return null;
    	}

    	ModelCompareOptions rv = new ModelCompareOptions();
    	for (String option : options){
    		if (ModelCompareOptionsType.F_COMPUTE_CURRENT_TO_PROVIDED.getLocalPart().equals(option)){
    			rv.setComputeCurrentToProvided(true);
    		}
    		if (ModelCompareOptionsType.F_COMPUTE_PROVIDED_TO_CURRENT.getLocalPart().equals(option)){
    			rv.setComputeProvidedToCurrent(true);
    		}
			if (ModelCompareOptionsType.F_RETURN_NORMALIZED.getLocalPart().equals(option)){
				rv.setReturnNormalized(true);
			}
    		if (ModelCompareOptionsType.F_RETURN_CURRENT.getLocalPart().equals(option)){
    			rv.setReturnCurrent(true);
    		}
			if (ModelCompareOptionsType.F_IGNORE_OPERATIONAL_ITEMS.getLocalPart().equals(option)){
				rv.setIgnoreOperationalItems(true);
			}
    	}
    	return rv;
    }

	@Override
    public String toString() {
    	StringBuilder sb = new StringBuilder("ModelCompareOptions(");
    	appendFlag(sb, "computeCurrentToProvided", computeCurrentToProvided);
    	appendFlag(sb, "computeProvidedToCurrent", computeProvidedToCurrent);
    	appendFlag(sb, "returnNormalized", returnNormalized);
    	appendFlag(sb, "returnCurrent", returnCurrent);
		appendFlag(sb, "ignoreOperationalItems", ignoreOperationalItems);
    	if (sb.charAt(sb.length() - 1) == ',') {
			sb.deleteCharAt(sb.length() - 1);
		}
		sb.append(")");
		return sb.toString();
    }

    private void appendFlag(StringBuilder sb, String name, Boolean val) {
		if (val == null) {
			return;
		} else if (val) {
			sb.append(name);
			sb.append(",");
		} else {
			sb.append(name);
			sb.append("=false,");
		}
	}

    public ModelCompareOptions clone() {
        // not much efficient, but...
        ModelCompareOptions clone = fromModelCompareOptionsType(toModelCompareOptionsType());
		return clone;
    }

}
