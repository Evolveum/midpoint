/**
 * Classes in this package cover operations with shadows. They take care of splitting the operations
 * between repository and resource, merging the data back, handling the errors and generally controlling the
 * process.
 *
 * The two principal classes that actually execute the operations are:
 *
 * 1. {@link com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectConverter}: executes operations on resource
 * (resides in `resourceobjects` sibling package)
 * 2. {@link com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowManager}: executes operations in the repository
 * (resides in `manager` child package)
 *
 * The shadow cache package itself is structured like this:
 *
 * {@link com.evolveum.midpoint.provisioning.impl.shadows.ShadowsFacade} is a facade that dispatch method calls to a set
 * of helper classes, like {@link com.evolveum.midpoint.provisioning.impl.shadows.GetHelper},
 * {@link com.evolveum.midpoint.provisioning.impl.shadows.SearchHelper},
 * {@link com.evolveum.midpoint.provisioning.impl.shadows.ModifyHelper},
 * {@link com.evolveum.midpoint.provisioning.impl.shadows.DeleteHelper}, and so on.
 *
 * A special case is live sync and async update, which are invoked outside of the facade. (This will most probably be fixed.)
 *
 * Other subpackages are:
 *
 * 1. `sync` - takes care of live sync and async update
 * 2. `errors` - takes care of the error handling
 * 3. `task` - various task handlers related to shadows processing (currently the operation propagation/multi-propagation)
 */
package com.evolveum.midpoint.provisioning.impl.shadows;
