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
package com.evolveum.midpoint.util;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.apache.commons.lang.StringUtils;

/**
 * @author semancik
 *
 */
public class ReflectionUtil {

	/**
	 * Try to get java property from the object by reflection
	 */
	public static <T> T getJavaProperty(Object object, String propertyName, Class<T> propetyClass) {
		String getterName = getterName(propertyName);
		Method method;
		try {
			method = object.getClass().getMethod(getterName);
		} catch (SecurityException e) {
			throw new IllegalArgumentException("Security error getting getter for property "+propertyName+": "+e.getMessage(),e);
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException("No getter for property "+propertyName+" in "+object+" ("+object.getClass()+")");
		}
		if (method == null) {
			throw new IllegalArgumentException("No getter for property "+propertyName+" in "+object+" ("+object.getClass()+")");
		}
		if (!propetyClass.isAssignableFrom(method.getReturnType())) {
			throw new IllegalArgumentException("The getter for property " + propertyName + " returns " + method.getReturnType() +
					", expected " + propetyClass+" in "+object+" ("+object.getClass()+")");
		}
		try {
			return (T) method.invoke(object);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Error invoking getter for property "+propertyName+" in "+object+" ("+object.getClass()+"): "
					+e.getClass().getSimpleName()+": "+e.getMessage(),e);
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException("Error invoking getter for property "+propertyName+" in "+object+" ("+object.getClass()+"): "
					+e.getClass().getSimpleName()+": "+e.getMessage(),e);
		} catch (InvocationTargetException e) {
			throw new IllegalArgumentException("Error invoking getter for property "+propertyName+" in "+object+" ("+object.getClass()+"): "
					+e.getClass().getSimpleName()+": "+e.getMessage(),e);
		}
	}

	public static boolean hasJavaProperty(Object object, String propertyName) {
		return findGetter(object, propertyName) != null;
	}

	public static Method findGetter(Object object, String propertyName) {
		String getterName = getterName(propertyName);
		return findMethod(object, getterName, 0);
	}

	private static String getterName(String propertyName) {
		return "get" + StringUtils.capitalize(propertyName);
	}

	public static Method findMethod(Object object, String methodName, int arity) {
		for (Method method: object.getClass().getMethods()) {
			if (method.getName().equals(methodName) &&
					method.getParameterTypes().length == arity &&
					!method.isVarArgs()) {
				return method;
			}
		}
		return null;
	}

	public static Method findMethod(Object object, String methodName, List<?> argList) throws SecurityException {
		Method method = findMethodDirect(object, methodName, argList);
		if (method != null) {
			return method;
		}
		method = findMethodCompatible(object, methodName, argList);
		if (method != null) {
			return method;
		}
		// We cannot find method directly. Try varargs.
		method = findVarArgsMethod(object, methodName);
		return method;
	}

	private static Method findMethodDirect(Object object, String methodName, List<?> argList) throws SecurityException {
		Class<?>[] parameterTypes = new Class[argList.size()];
		for (int i=0; i < argList.size(); i++) {
			parameterTypes[i] = argList.get(i).getClass();
		}
		try {
			return object.getClass().getMethod(methodName, parameterTypes);
		} catch (NoSuchMethodException e) {
			return null;
		}
	}

	/**
	 * Rough lookup of a compatible method. It takes first method with matching name, number of parameters and compatible
	 *  parameter values. It is not perfect, e.g. it cannot select foo(String) instead of foo(Object). But it is better than
	 *  nothing. And stock Java reflection has really nothing.
	 */
	private static Method findMethodCompatible(Object object, String methodName, List<?> argList) throws SecurityException {
		for (Method method: object.getClass().getMethods()) {
			if (method.getName().equals(methodName)) {
				Class<?>[] parameterTypes = method.getParameterTypes();
				if (parameterTypes.length == argList.size()) {
					boolean wrong = false;
					for (int i=0; i < parameterTypes.length; i++) {
						Object arg = argList.get(i);
						if (arg == null) {
							// null argument matches any parameter type
						} else {
							if (!parameterTypes[i].isAssignableFrom(arg.getClass())) {
								wrong = true;
								break;
							}
						}
					}
					if (!wrong) {
						// We got it. We have compatible signature here.
						return method;
					}
				}
			}
		}
		return null;
	}

	public static Method findVarArgsMethod(Object object, String methodName) {
		for (Method method: object.getClass().getMethods()) {
			if (method.getName().equals(methodName) && method.isVarArgs()) {
				return method;
			}
		}
		return null;
	}

	public static Object invokeMethod(Object object, String methodName, List<?> argList) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		Method method = findMethod(object, methodName, argList);
		if (method == null) {
			throw new NoSuchMethodException("No method "+methodName+" for arguments "+debugDumpArgList(argList)+" in "+object);
		}
		Object[] args = argList.toArray();
		if (method.isVarArgs()) {
			Class<?> parameterTypeClass = method.getParameterTypes()[0];
			Object[] varArgs = (Object[]) Array.newInstance(parameterTypeClass.getComponentType(), args.length);
			for (int i = 0; i < args.length; i++) {
				varArgs[i] = args[i];
			}
			args = new Object[] { varArgs };
		}
		try {
			return method.invoke(object, args);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(e.getMessage()+" for arguments "+debugDumpArgList(argList)+" in "+object, e);
		}
	}

	public static String debugDumpArgList(List<?> argList) {
		StringBuilder sb = new StringBuilder();
		boolean sep = false;
		for (Object arg: argList) {
			if (sep) {
				sb.append(", ");
			} else {
				sep = true;
			}
			if (arg == null) {
				sb.append("null");
			} else {
				sb.append(arg.getClass().getName());
				sb.append(":");
				sb.append(arg);
			}
		}
		return sb.toString();
	}

}
