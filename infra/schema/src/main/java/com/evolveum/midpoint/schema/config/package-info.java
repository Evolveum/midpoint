/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

/**
 * This is an experiment how to provide reliable and consistent information about the origin of individual configuration items
 * (mappings, expressions, etc). The primary goal is to be able to derive expression profiles for execution of embedded
 * expressions.
 *
 * Secondary goals:
 *
 * . Understandable, consistent, and reliable error reporting. We use the origin information, along with custom "config item"
 * classes to do that.
 *
 * . Easy access to the content of configuration items. Again, this is due to the use of custom "config item" classes.
 *
 * These custom config classes are on a halfway between raw "beans" like `ExpressionType`, `MappingType`,
 * and fully processed (parsed, compiled) objects like `Expression`, `Mapping`, and so on.
 *
 * Design notes:
 *
 * . The names of the classes should correspond to the names of respective beans, even if they may not seem the optimal ones
 * (from the point of view of the current knowledge). The idea is to make them easily discernible and, in the future, maybe
 * also (semi)automatically generated.
 * +
 * The exception is when a bean has multiple uses like `ExpressionType` used as a library function.
 *
 * == Broader context
 *
 * The idea of providing a wrapper for configuration items is not new. However, the previous attempts were limited
 * to specific domains, for example:
 *
 * - activity definitions (`ActivityDefinition` and its constituents, especially `WorkDefinition`),
 * - refined resource object class and attribute definitions
 * ({@link com.evolveum.midpoint.schema.processor.ResourceObjectDefinition} and its parts) - although these are more parsed
 * than configuration items here,
 * - TODO what else?
 *
 * == Boundaries: what is this NOT meant to be
 *
 * This is not to store the fully processed (parsed) information like `Expression` or `Mapping`.
 * It should be really a quite thin wrapper around the raw beans, with limited responsibilities mentioned above.
 *
 * HIGHLY EXPERIMENTAL. MAYBE NOT USABLE FOR THE FUTURE. MAYBE YES, WITH SOME CODE GENERATION SUPPORT.
 */
package com.evolveum.midpoint.schema.config;
