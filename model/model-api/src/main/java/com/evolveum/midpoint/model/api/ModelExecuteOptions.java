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


/**
 * @author semancik
 *
 */
public class ModelExecuteOptions {
	
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
	Boolean crypt;

	/**
	 * Option to reconcile user while executing changes. 
	 */
	Boolean reconcile;
	
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

	public Boolean getCrypt() {
		return crypt;
	}

	public void setCrypt(Boolean crypt) {
		this.crypt = crypt;
	}
	
	public static boolean isCrypt(ModelExecuteOptions options) {
		if (options == null) {
			return false;
		}
		if (options.crypt == null) {
			return false;
		}
		return options.crypt;
	}

	public static ModelExecuteOptions createCrypt() {
		ModelExecuteOptions opts = new ModelExecuteOptions();
		opts.setCrypt(true);
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
	

}
