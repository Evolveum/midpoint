/**
 * Shadow cache is a facade that covers all the operations with shadows. It
 * takes care of splitting the operations between repository and resource,
 * merging the data back, handling the errors and generally controlling the
 * process.
 *
 * The two principal classes that do the operations are:
 *
 * 1. {@link com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectConverter}: executes operations on resource
 * 2. {@link com.evolveum.midpoint.provisioning.impl.shadowmanager.ShadowManager}: executes operations in the repository
 *
 * The shadow cache package itself is structured like this:
 *
 * {@link com.evolveum.midpoint.provisioning.impl.shadowcache.ShadowCache} is a facade that dispatch method calls to a set
 * of helper classes, like {@link com.evolveum.midpoint.provisioning.impl.shadowcache.GetHelper},
 * {@link com.evolveum.midpoint.provisioning.impl.shadowcache.SearchHelper},
 * {@link com.evolveum.midpoint.provisioning.impl.shadowcache.ModifyHelper},
 * {@link com.evolveum.midpoint.provisioning.impl.shadowcache.DeleteHelper}, and so on.
 *
 * A special case is live sync and async update, which are invoked outside of the facade. (This will most probably be fixed.)
 */
package com.evolveum.midpoint.provisioning.impl.shadowcache;
