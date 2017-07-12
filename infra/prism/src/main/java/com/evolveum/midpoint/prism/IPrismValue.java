/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Collection;

/**
 * TODO rename to PrismValue and rename existing PrismValue to PrismValueImpl
 *
 * @author semancik
 * @author mederly
 */
public interface IPrismValue extends Visitable, PathVisitable, Serializable, DebugDumpable, Revivable {

	@Nullable
	OriginType getOriginType();

	@Nullable
	Objectable getOriginObject();

	@Nullable
	Object getUserData(@NotNull String key);

	void setUserData(@NotNull String key, @Nullable Object value);

	@Nullable
	Itemable getParent();

	void setParent(@Nullable Itemable parent);

	void clearParent();

	/**
	 * Computes a path in current prism structure this value is part of.
	 * If there's no parent (Itemable) for this or some parent value, an exception is thrown.
	 */
	@NotNull
	ItemPath getPath();

	PrismContext getPrismContext();

	void applyDefinition(ItemDefinition definition) throws SchemaException;

	void applyDefinition(ItemDefinition definition, boolean force) throws SchemaException;

	void recompute();

	void recompute(PrismContext prismContext);

	boolean isEmpty();

	void normalize();

	/**
	 * Returns true if the value is raw. Raw value is a semi-parsed value.
	 * A value for which we don't have a full definition yet and therefore
	 * the parsing could not be finished until the definition is supplied.
	 */
	boolean isRaw();

	Object find(ItemPath path);

	<X extends PrismValue,Y extends ItemDefinition> PartiallyResolvedItem<X,Y> findPartial(ItemPath path);

	boolean equals(PrismValue otherValue, boolean ignoreMetadata);

	Collection<? extends ItemDelta> diff(PrismValue otherValue);

	Collection<? extends ItemDelta> diff(PrismValue otherValue, boolean ignoreMetadata, boolean isLiteral);

	boolean match(PrismValue otherValue);

	/**
	 * Returns a short (one-line) representation of the real value stored in this object.
	 * The value is returned without any decorations or type demarcations (such as PPV, PRV, etc.)
	 */
	String toHumanReadableString();

	boolean isImmutable();
}
