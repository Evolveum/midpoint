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
 * HIGHLY EXPERIMENTAL. MAYBE NOT USABLE FOR THE FUTURE. MAYBE YES, WITH SOME CODE GENERATION SUPPORT.
 */
package com.evolveum.midpoint.schema.config;
