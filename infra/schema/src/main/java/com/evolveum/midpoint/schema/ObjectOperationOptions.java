/**
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.schema;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PropertyPath;

/**
 * @author semancik
 *
 */
public class ObjectOperationOptions {
	
	private ObjectSelector selector;
	private Collection<ObjectOperationOption> options;
	
	public ObjectOperationOptions(ObjectSelector selector, Collection<ObjectOperationOption> options) {
		super();
		this.selector = selector;
		this.options = options;
	}

	public ObjectSelector getSelector() {
		return selector;
	}

	public Collection<ObjectOperationOption> getOptions() {
		return options;
	}
	
	public boolean hasOption(ObjectOperationOption option) {
		return ObjectOperationOption.hasOption(options, option);
	}
	
	public static ObjectOperationOptions create(ObjectOperationOption... options) {
		return create((ObjectSelector)null, options);
	}
	
	public static ObjectOperationOptions create(PropertyPath path, ObjectOperationOption... options) {
		return create(new ObjectSelector(path), options);
	}
	
	public static ObjectOperationOptions create(QName pathQName, ObjectOperationOption... options) {
		return create(new ObjectSelector(new PropertyPath(pathQName)), options);
	}
	
	public static ObjectOperationOptions create(ObjectSelector selector, ObjectOperationOption... options) {
		List<ObjectOperationOption> optionsList = Arrays.asList(options);
		return new ObjectOperationOptions(selector, optionsList);
	}
	
	public static Collection<ObjectOperationOptions> createCollection(PropertyPath path, ObjectOperationOption... options) {
		Collection<ObjectOperationOptions> optionsCollection = new ArrayList<ObjectOperationOptions>(1);
		optionsCollection.add(create(path, options));
		return optionsCollection;
	}

	public static Collection<ObjectOperationOptions> createCollection(QName pathQName, ObjectOperationOption... options) {
		Collection<ObjectOperationOptions> optionsCollection = new ArrayList<ObjectOperationOptions>(1);
		optionsCollection.add(create(pathQName, options));
		return optionsCollection;
	}
	
	public static Collection<ObjectOperationOptions> createCollectionRoot(ObjectOperationOption... options) {
		return createCollection((ObjectSelector)null, options);
	}

	public static Collection<ObjectOperationOptions> createCollection(ObjectSelector selector, ObjectOperationOption... options) {
		Collection<ObjectOperationOptions> optionsCollection = new ArrayList<ObjectOperationOptions>(1);
		optionsCollection.add(create(selector, options));
		return optionsCollection;
	}

	public static Collection<ObjectOperationOptions> createCollection(ObjectOperationOption options, PropertyPath... paths) {
		Collection<ObjectOperationOptions> optionsCollection = new ArrayList<ObjectOperationOptions>(paths.length);
		for (PropertyPath path: paths) {
			optionsCollection.add(create(path, options));
		}
		return optionsCollection;
	}

	public static Collection<ObjectOperationOptions> createCollection(ObjectOperationOption options, QName... pathQNames) {
		Collection<ObjectOperationOptions> optionsCollection = new ArrayList<ObjectOperationOptions>(pathQNames.length);
		for (QName qname: pathQNames) {
			optionsCollection.add(create(qname, options));
		}
		return optionsCollection;
	}

	/**
	 * Returns options that apply to the "root" object. I.e. options that have null selector, null path, empty path, ...
	 */
	public static Collection<ObjectOperationOption> findRootOptions(Collection<ObjectOperationOptions> options) {
		if (options == null) {
			return null;
		}
		Collection<ObjectOperationOption> objectOptions = new ArrayList<ObjectOperationOption>();
		for (ObjectOperationOptions oooption: options) {
			if (oooption.isRoot()) {
				objectOptions.addAll(oooption.getOptions());
			}
		}
		return objectOptions;
	}

	private boolean isRoot() {
		if (selector == null) {
			return true;
		}
		if (selector.getPath() == null || selector.getPath().isEmpty()) {
			return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((options == null) ? 0 : options.hashCode());
		result = prime * result + ((selector == null) ? 0 : selector.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ObjectOperationOptions other = (ObjectOperationOptions) obj;
		if (options == null) {
			if (other.options != null)
				return false;
		} else if (!options.equals(other.options))
			return false;
		if (selector == null) {
			if (other.selector != null)
				return false;
		} else if (!selector.equals(other.selector))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ObjectOperationOptions(" + selector + ": " + options + ")";
	}

}
