/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.cel.value;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import dev.cel.common.types.CelType;
import dev.cel.common.types.SimpleType;
import dev.cel.common.types.StructType;
import dev.cel.common.values.CelValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.polystring.PolyString;

/**
 * @author Radovan Semancik
 */
public abstract class MidPointCelValue<T> extends CelValue {

    public abstract T getJavaValue();

}

