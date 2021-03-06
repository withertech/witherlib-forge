/*
 * witherlib-forge
 * Copyright (C) 2021 WitherTech
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.withertech.witherlib.nbt;

import com.withertech.witherlib.util.ClassUtils;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks variables that should be automatically synced with the client. Currently, this is used just
 * for tile entities, but could have other uses I guess?
 *
 * @author Witherking25
 * @since 2.0.6
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface SyncVariable
{
	/**
	 * The name to read/write to NBT.
	 *
	 * @return The variables NBT key
	 */
	String name();

	/**
	 * Should the variable be loaded in {@link TileEntity#load}?
	 *
	 * @return True if we should load on read
	 */
	boolean onLoad() default true;

	/**
	 * Should the variable be saved in {@link TileEntity#save}?
	 *
	 * @return True if we should save on write
	 */
	boolean onSave() default true;

	/**
	 * Should the variable be saved in {@link TileEntity#getUpdatePacket} and {@link
	 * TileEntity#getUpdateTag}?
	 *
	 * @return True if we should save on sync packet
	 */
	boolean onPacket() default true;

	/**
	 * Used together with onRead, onWrite, and onPacket to determine when a variable should be
	 * saved/loaded. In most cases, you should probably just sync everything at all times.
	 */
	enum Type
	{
		READ, WRITE, PACKET
	}

	/**
	 * Reads/writes sync variables for any object. Used by TileEntitySL in Lib. Gems uses this in
	 * PlayerDataHandler.
	 *
	 * @author SilentChaos512
	 * @since 2.1.1
	 */
	final class Helper
	{
		static final Map<Class<?>, NBTSerializer<?>> SERIALIZERS = new HashMap<>();

		private Helper()
		{
		}

		public static <T> void registerSerializer(
				Class<T> clazz,
				Function<CompoundNBT, T> loader,
				BiConsumer<CompoundNBT, T> saver
		)
		{
			SERIALIZERS.put(clazz, new NBTSerializer<T>()
			{
				@Override
				public T read(CompoundNBT tags)
				{
					return loader.apply(tags);
				}

				@Override
				public void write(CompoundNBT tags, T obj)
				{
					saver.accept(tags, obj);
				}
			});
		}

		/**
		 * Reads sync variables for the object. This method will attempt to read a value from NBT
		 * and assign that value for any field marked with the SyncVariable annotation.
		 *
		 * @param obj  The object with SyncVariable fields.
		 * @param tags The NBT to read values from.
		 */
		public static <T> void readSyncVars(T obj, CompoundNBT tags)
		{
			readSyncVars(obj.getClass(), obj, tags);
		}

		/**
		 * Reads sync variables for the object. This method will attempt to read a value from NBT
		 * and assign that value for any field marked with the SyncVariable annotation.
		 *
		 * @param clazz The class to search for fields in
		 * @param obj   The object with SyncVariable fields.
		 * @param tags  The NBT to read values from.
		 */
		@SuppressWarnings({"unchecked", "rawtypes"})
		public static <T> void readSyncVars(@Nonnull Class<? extends T> clazz, T obj, CompoundNBT tags)
		{
			// Try to read from NBT for fields marked with SyncVariable.
			for (Field field : clazz.getDeclaredFields())
			{
				for (Annotation annotation : field.getDeclaredAnnotations())
				{
					if (annotation instanceof SyncVariable)
					{
						SyncVariable sync = (SyncVariable) annotation;

						try
						{
							// Set fields accessible if necessary.

							if (!field.isAccessible())
							{
								field.setAccessible(true);
							}
							String name = sync.name();

							//noinspection ChainOfInstanceofChecks
							if (field.getType() == int.class)
							{
								field.setInt(obj, tags.getInt(name));
							} else if (field.getType() == float.class)
							{
								field.setFloat(obj, tags.getFloat(name));
							} else if (field.getType() == String.class)
							{
								field.set(obj, tags.getString(name));
							} else if (field.getType() == boolean.class)
							{
								field.setBoolean(obj, tags.getBoolean(name));
							} else if (field.getType() == double.class)
							{
								field.setDouble(obj, tags.getDouble(name));
							} else if (field.getType() == long.class)
							{
								field.setLong(obj, tags.getLong(name));
							} else if (field.getType() == short.class)
							{
								field.setShort(obj, tags.getShort(name));
							} else if (field.getType() == byte.class)
							{
								field.setByte(obj, tags.getByte(name));
							} else if (ClassUtils.<LazyOptional<INBTSerializable<INBT>>>castClass(LazyOptional.class).isAssignableFrom(
									field.getType()))
							{
								((LazyOptional<INBTSerializable<INBT>>) field.get(obj)).ifPresent(
										compoundNBTINBTSerializable -> compoundNBTINBTSerializable.deserializeNBT(tags.get(
												name)));
							} else if (ClassUtils.<INBTSerializable<INBT>>castClass(INBTSerializable.class).isAssignableFrom(
									field.getType()))
							{
								((INBTSerializable<INBT>) field.get(obj)).deserializeNBT(tags.get(name));
							} else if (SERIALIZERS.containsKey(field.getType()))
							{
								NBTSerializer serializer = SERIALIZERS.get(field.getType());
								CompoundNBT compound = tags.getCompound(name);
								field.set(obj, serializer.read(compound));
							} else
							{
								throw new IllegalArgumentException("Don't know how to read type " + field.getType() +
										" from NBT!");
							}
						} catch (IllegalAccessException | IllegalArgumentException ex)
						{
							ex.printStackTrace();
						}
					}
				}
			}
		}

		/**
		 * Writes sync variables for the object. This method will take the values in all fields
		 * marked with the SyncVariable annotation and save them to NBT.
		 *
		 * @param obj      The object with SyncVariable fields.
		 * @param tags     The NBT to save values to.
		 * @param syncType The sync type (WRITE or PACKET).
		 * @return The modified tags.
		 */
		public static <T> CompoundNBT writeSyncVars(T obj, CompoundNBT tags, Type syncType)
		{
			return writeSyncVars(obj.getClass(), obj, tags, syncType);
		}

		/**
		 * Writes sync variables for the object. This method will take the values in all fields
		 * marked with the SyncVariable annotation and save them to NBT.
		 *
		 * @param clazz    The class to search for fields in.
		 * @param obj      The object with SyncVariable fields.
		 * @param tags     The NBT to save values to.
		 * @param syncType The sync type (WRITE or PACKET).
		 * @return The modified tags.
		 */
		@SuppressWarnings({"unchecked", "rawtypes"}) // from serializer
		public static <T> CompoundNBT writeSyncVars(
				@Nonnull Class<? extends T> clazz,
				T obj,
				CompoundNBT tags,
				Type syncType
		)
		{


			// Try to write to NBT for fields marked with SyncVariable.
			for (Field field : clazz.getDeclaredFields())
			{
				for (Annotation annotation : field.getDeclaredAnnotations())
				{
					if (annotation instanceof SyncVariable)
					{
						SyncVariable sync = (SyncVariable) annotation;

						// Does variable allow writing in this case?
						if (syncType == Type.WRITE && sync.onSave()
								|| syncType == Type.PACKET && sync.onPacket())
						{
							try
							{
								// Set fields accessible if necessary.
								if (!field.isAccessible())
								{
									field.setAccessible(true);
								}
								String name = sync.name();

								//noinspection ChainOfInstanceofChecks
								if (field.getType() == int.class)
								{
									tags.putInt(name, field.getInt(obj));
								} else if (field.getType() == float.class)
								{
									tags.putFloat(name, field.getFloat(obj));
								} else if (field.getType() == String.class)
								{
									tags.putString(name, (String) field.get(obj));
								} else if (field.getType() == boolean.class)
								{
									tags.putBoolean(name, field.getBoolean(obj));
								} else if (field.getType() == double.class)
								{
									tags.putDouble(name, field.getDouble(obj));
								} else if (field.getType() == long.class)
								{
									tags.putLong(name, field.getLong(obj));
								} else if (field.getType() == short.class)
								{
									tags.putShort(name, field.getShort(obj));
								} else if (field.getType() == byte.class)
								{
									tags.putByte(name, field.getByte(obj));
								} else if (ClassUtils.<LazyOptional<INBTSerializable<INBT>>>castClass(LazyOptional.class).isAssignableFrom(
										field.getType()))
								{
									((LazyOptional<INBTSerializable<INBT>>) field.get(obj)).ifPresent(
											compoundNBTINBTSerializable ->
													tags.put(name, compoundNBTINBTSerializable.serializeNBT()));
								} else if (ClassUtils.<INBTSerializable<INBT>>castClass(INBTSerializable.class).isAssignableFrom(
										field.getType()))
								{
									tags.put(name, ((INBTSerializable<INBT>) field.get(obj)).serializeNBT());
								} else if (SERIALIZERS.containsKey(field.getType()))
								{
									CompoundNBT compound = new CompoundNBT();
									NBTSerializer serializer = SERIALIZERS.get(field.getType());
									serializer.write(compound, field.get(obj));
									tags.put(name, compound);
								} else
								{
									throw new IllegalArgumentException("Don't know how to write type " + field.getType() + " to NBT!");
								}
							} catch (IllegalAccessException | IllegalArgumentException ex)
							{
								ex.printStackTrace();
							}
						}
					}
				}
			}

			return tags;
		}
	}
}
