/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.path;

/**
 * Denotes reference path segment: either ".." meaning owner, or "@" meaning referenced object.
 * (Note that these are to be used only in filters and order instructions, for now.)
 *
 * @author mederly
 */
public abstract class ReferencePathSegment extends ItemPathSegment {
}
