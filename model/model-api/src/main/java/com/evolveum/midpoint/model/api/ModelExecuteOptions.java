/**
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */
package com.evolveum.midpoint.model.api;


import com.evolveum.midpoint.xml.ns._public.common.common_2a.ModelExecuteOptionsType;

import java.io.Serializable;

/**
 * @author semancik
 *
 */
public class ModelExecuteOptions implements Serializable {
	
	/**
	 * Force the operation even if it would otherwise fail due to external failure. E.g. attempt to delete an account
	 * that no longer exists on resource may fail without a FORCE option. If FORCE option is used then the operation is
	 * finished even if the account does not exist (e.g. at least shadow is removed from midPoint repository).
	 */
	Boolean force;
	
	/**
	 * Avoid any smart processing of the data except for schema application. Do not synchronize the data, do not apply
	 * any expressions, etc.
	 */
	Boolean raw;
	
	/**
	 * Encrypt any cleartext data on write, decrypt any encrypted data on read. Applies only to the encrypted
	 * data formats (ProtectedString, ProtectedByteArray).
	 */
	Boolean noCrypt;
	
	/**
	 * Option to reconcile user while executing changes. 
	 */
	Boolean reconcile;

    /**
     * Option to execute changes as soon as they are approved. (For the primary stage approvals, the default behavior
     * is to wait until all changes are approved/rejected and then execute the operation as a whole.)
     */
    Boolean executeImmediatelyAfterApproval;
    
    
    /**
     * Option to user overwrite flag. It can be used from web service, if we want to re-import some object
     */
    Boolean overwrite;
    

    public Boolean getForce() {
		return force;
	}

	public void setForce(Boolean force) {
		this.force = force;
	}
	
	public static boolean isForce(ModelExecuteOptions options) {
		if (options == null) {
			return false;
		}
		if (options.force == null) {
			return false;
		}
		return options.force;
	}
	
	public static ModelExecuteOptions createForce() {
		ModelExecuteOptions opts = new ModelExecuteOptions();
		opts.setForce(true);
		return opts;
	}


	public Boolean getRaw() {
		return raw;
	}

	public void setRaw(Boolean raw) {
		this.raw = raw;
	}
	
	public static boolean isRaw(ModelExecuteOptions options) {
		if (options == null) {
			return false;
		}
		if (options.raw == null) {
			return false;
		}
		return options.raw;
	}
	
	public static ModelExecuteOptions createRaw() {
		ModelExecuteOptions opts = new ModelExecuteOptions();
		opts.setRaw(true);
		return opts;
	}

	public Boolean getNoCrypt() {
		return noCrypt;
	}

	public void setNoCrypt(Boolean noCrypt) {
		this.noCrypt = noCrypt;
	}
	
	public static boolean isNoCrypt(ModelExecuteOptions options) {
		if (options == null) {
			return false;
		}
		if (options.noCrypt == null) {
			return false;
		}
		return options.noCrypt;
	}

	public static ModelExecuteOptions createNoCrypt() {
		ModelExecuteOptions opts = new ModelExecuteOptions();
		opts.setNoCrypt(true);
		return opts;
	}

	public Boolean getReconcile() {
		return reconcile;
	}
	
	public void setReconcile(Boolean reconcile) {
		this.reconcile = reconcile;
	}
	
	public static boolean isReconcile(ModelExecuteOptions options){
		if (options == null){
			return false;
		}
		if (options.reconcile == null){
			return false;
		}
		return options.reconcile;
	}
	
	public static ModelExecuteOptions createReconcile(){
		ModelExecuteOptions opts = new ModelExecuteOptions();
		opts.setReconcile(true);
		return opts;
	}
	
	public Boolean getOverwrite() {
		return overwrite;
	}
	
	public void setOverwrite(Boolean overwrite) {
		this.overwrite = overwrite;
	}
	
	public static boolean isOverwrite(ModelExecuteOptions options){
		if (options == null){
			return false;
		}
		if (options.overwrite == null){
			return false;
		}
		return options.overwrite;
	}
	
	public static ModelExecuteOptions createOverwrite(){
		ModelExecuteOptions opts = new ModelExecuteOptions();
		opts.setOverwrite(true);
		return opts;
	}

    public void setExecuteImmediatelyAfterApproval(Boolean executeImmediatelyAfterApproval) {
        this.executeImmediatelyAfterApproval = executeImmediatelyAfterApproval;
    }

    public static boolean isExecuteImmediatelyAfterApproval(ModelExecuteOptions options){
        if (options == null){
            return false;
        }
        if (options.executeImmediatelyAfterApproval == null) {
            return false;
        }
        return options.executeImmediatelyAfterApproval;
    }

    public static ModelExecuteOptions createExecuteImmediatelyAfterApproval(){
        ModelExecuteOptions opts = new ModelExecuteOptions();
        opts.setExecuteImmediatelyAfterApproval(true);
        return opts;
    }

    public ModelExecuteOptionsType toModelExecutionOptionsType() {
        ModelExecuteOptionsType retval = new ModelExecuteOptionsType();
        retval.setForce(force);
        retval.setRaw(raw);
        retval.setNoCrypt(noCrypt);
        retval.setReconcile(reconcile);
        retval.setExecuteImmediatelyAfterApproval(executeImmediatelyAfterApproval);
        retval.setOverwrite(overwrite);
        return retval;
    }

    public static ModelExecuteOptions fromModelExecutionOptionsType(ModelExecuteOptionsType type) {
        if (type == null) {
            return null;
        }
        ModelExecuteOptions retval = new ModelExecuteOptions();
        retval.setForce(type.isForce());
        retval.setRaw(type.isRaw());
        retval.setNoCrypt(type.isNoCrypt());
        retval.setReconcile(type.isReconcile());
        retval.setExecuteImmediatelyAfterApproval(type.isExecuteImmediatelyAfterApproval());
        retval.setOverwrite(type.isOverwrite());
        return retval;
    }

}
