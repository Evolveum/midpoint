/**
 * 
 */
package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.schema.holder.ExpressionCodeHolder;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ExpressionType;

/**
 * 
 * @author Radovan Semancik
 *
 */
public class ExpressionUtil {

	public static boolean isEmpty(ExpressionType expression) {
		return (expression==null || expression.getCode()==null);
	}
	
	public static String getCodeAsString(ExpressionType expression) {
		if (expression.getCode()==null) {
			return null;
		}
		ExpressionCodeHolder codeHolder = new ExpressionCodeHolder(expression.getCode());
		return codeHolder.getExpressionAsString();
	}
	
}
