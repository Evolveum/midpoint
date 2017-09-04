/*
 * Copyright (c) 2014-2015 Evolveum
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
package com.evolveum.midpoint.tools.testng;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.testng.IMethodInstance;
import org.testng.IMethodInterceptor;
import org.testng.ITestContext;
import org.testng.collections.Lists;
import org.testng.collections.Maps;

public class AlphabeticalMethodInterceptor implements IMethodInterceptor {

	@Override
	public List<IMethodInstance> intercept(List<IMethodInstance> methods, ITestContext context) {
		List<Object> instanceList = Lists.newArrayList();
	    Map<Object, List<IMethodInstance>> map = Maps.newHashMap();
	    for (IMethodInstance mi : methods) {
	    	Object instance = mi.getInstance();
	        if (!instanceList.contains(instance)) {
	          instanceList.add(instance);
	        }
	        List<IMethodInstance> l = map.get(instance);
	        if (l == null) {
	          l = Lists.newArrayList();
	          map.put(instance, l);
	        }
	        l.add(mi);
	    }

	    Comparator<IMethodInstance> comparator = new Comparator<IMethodInstance>() {
			@Override
			public int compare(IMethodInstance o1, IMethodInstance o2) {
				return o1.getMethod().getMethodName().compareTo(o2.getMethod().getMethodName());
			}
		};
	    List<IMethodInstance> result = Lists.newArrayList();
	    for (Object instance : instanceList) {
	    	List<IMethodInstance> methodlist = map.get(instance);
	    	IMethodInstance[] array = methodlist.toArray(new IMethodInstance[methodlist.size()]);
			Arrays.sort(array, comparator);
			result.addAll(Arrays.asList(array));
	    }

	    System.out.println("AlphabeticalMethodInterceptor: "+result);

	    return result;
	}
}
