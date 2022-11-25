/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

/**
 * Responsibilities of the shadow manager package:
 *
 * Manage repository shadows, including:
 *
 * 1. Lookup, create, and update shadows (as part of the provisioning process).
 * 2. Getting and searching for shadows (e.g. as part of get/search operations in higher layers).
 * 3. Management of pending operations.
 *
 * Limitations:
 *
 * - Does NOT communicate with the resource (means: please do NOT do anything with the connector)
 *
 * Naming conventions:
 *
 * - _Search_ = plain search in the repository (no modifications of repo objects)
 * - _Lookup_ = looking up single shadow in the repository; name = "lookup [Live] Shadow By [What]"
 * - _Acquire_ = lookup + create if needed
 * - When talking about _shadow_ we always mean _repository shadow_ here.
 *
 * There are four main public classes here:
 *
 * - {@link com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowFinder}: looking up shadows
 * - {@link com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowCreator}: creating (or looking up + creating) shadows
 * - {@link com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowUpdater}: updating existing shadows
 * - {@link com.evolveum.midpoint.provisioning.impl.shadows.manager.OperationResultRecorder}: records the result of
 * add/modify/delete operation into the corresponding shadow
 * - plus some (package-private) helpers
 *
 * @author Katarina Valalikova
 * @author Radovan Semancik
 */
package com.evolveum.midpoint.provisioning.impl.shadows.manager;
