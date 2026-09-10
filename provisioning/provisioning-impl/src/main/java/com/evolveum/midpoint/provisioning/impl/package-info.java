/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

/**
 * For a general description, see https://docs.evolveum.com/midpoint/architecture/#provisioning-subsystem[the docs site].
 *
 * Constituent components:
 *
 * [%autowidth]
 * [%header]
 * |===
 * | Component | Description
 *
 * | `operations`
 * | Offloading main {@link com.evolveum.midpoint.provisioning.impl.ProvisioningServiceImpl} class from handling more complex
 * operations, like `get`, `search`, and the like. No specific logic should be there, besides basic handling of special cases
 * like the `raw` operation mode. The "meat" is in the following components.
 *
 * | `resources`
 * | Deals with resources and connectors.
 *
 * | `resourceobjects`
 * | Deals with resource objects (represented by `ShadowType` objects in midPoint), and their relatives, like live sync
 * and async update events. Handles the interaction with resources via UCF. Works with simulated associations.
 * Does _not_ contact repository, except for very special cases.
 *
 * | `shadows`
 * | Provides all the necessary functionality above the "resource objects" layer: manages repository shadows,
 * including their classification and handling of pending operations. Implements live sync and asynchronous updates.
 * Handles communication and other kinds of errors.
 *
 * |===
 *
 * @see com.evolveum.midpoint.provisioning.impl.resourceobjects
 * @see com.evolveum.midpoint.provisioning.impl.shadows
 */
package com.evolveum.midpoint.provisioning.impl;
