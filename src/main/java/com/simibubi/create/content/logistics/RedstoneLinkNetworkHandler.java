package com.simibubi.create.content.logistics;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import com.simibubi.create.Create;
import com.simibubi.create.foundation.config.AllConfigs;
import com.simibubi.create.foundation.tileEntity.behaviour.linked.LinkBehaviour;
import com.simibubi.create.foundation.utility.WorldHelper;
import io.github.fabricators_of_create.porting_lib.util.LevelUtil;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;

public class RedstoneLinkNetworkHandler {

	static final Map<LevelAccessor, Map<Pair<Frequency, Frequency>, Set<IRedstoneLinkable>>> connections =
		new IdentityHashMap<>();

	public static class Frequency {
		public static final Frequency EMPTY = new Frequency(ItemStack.EMPTY);
		private static final Map<Item, Frequency> simpleFrequencies = new IdentityHashMap<>();
		private ItemStack stack;
		private Item item;
		private int color;

		public static Frequency of(ItemStack stack) {
			if (stack.isEmpty())
				return EMPTY;
			if (!stack.hasTag())
				return simpleFrequencies.computeIfAbsent(stack.getItem(), $ -> new Frequency(stack));
			return new Frequency(stack);
		}

		private Frequency(ItemStack stack) {
			this.stack = stack;
			item = stack.getItem();
			CompoundTag displayTag = stack.getTagElement("display");
			color = displayTag != null && displayTag.contains("color") ? displayTag.getInt("color") : -1;
		}

		public ItemStack getStack() {
			return stack;
		}

		@Override
		public int hashCode() {
			return (item.hashCode() * 31) ^ color;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			return obj instanceof Frequency ? ((Frequency) obj).item == item && ((Frequency) obj).color == color
				: false;
		}

	}

	public void onLoadWorld(LevelAccessor world) {
		connections.put(world, new HashMap<>());
		Create.LOGGER.debug("Prepared Redstone Network Space for " + WorldHelper.getDimensionID(world));
	}

	public void onUnloadWorld(LevelAccessor world) {
		connections.remove(world);
		Create.LOGGER.debug("Removed Redstone Network Space for " + WorldHelper.getDimensionID(world));
	}

	public Set<IRedstoneLinkable> getNetworkOf(LevelAccessor world, IRedstoneLinkable actor) {
		Map<Pair<Frequency, Frequency>, Set<IRedstoneLinkable>> networksInWorld = networksIn(world);
		Pair<Frequency, Frequency> key = actor.getNetworkKey();
		if (!networksInWorld.containsKey(key))
			networksInWorld.put(key, new LinkedHashSet<>());
		return networksInWorld.get(key);
	}

	public void addToNetwork(LevelAccessor world, IRedstoneLinkable actor) {
		getNetworkOf(world, actor).add(actor);
		updateNetworkOf(world, actor);
	}

	public void removeFromNetwork(LevelAccessor world, IRedstoneLinkable actor) {
		Set<IRedstoneLinkable> network = getNetworkOf(world, actor);
		network.remove(actor);
		if (network.isEmpty()) {
			networksIn(world).remove(actor.getNetworkKey());
			return;
		}
		updateNetworkOf(world, actor);
	}

	public void updateNetworkOf(LevelAccessor world, IRedstoneLinkable actor) {
		Set<IRedstoneLinkable> network = getNetworkOf(world, actor);
		int power = 0;

		for (Iterator<IRedstoneLinkable> iterator = network.iterator(); iterator.hasNext();) {
			IRedstoneLinkable other = iterator.next();
			if (!other.isAlive()) {
				iterator.remove();
				continue;
			}
			if (!LevelUtil.isAreaLoaded(world, other.getLocation(), 0)) {
				iterator.remove();
				continue;
			}
			if (!withinRange(actor, other))
				continue;

			if (power < 15)
				power = Math.max(other.getTransmittedStrength(), power);
		}

		if (actor instanceof LinkBehaviour) {
			LinkBehaviour linkBehaviour = (LinkBehaviour) actor;
			// fix one-to-one loading order problem
			if (linkBehaviour.isListening()) {
				linkBehaviour.newPosition = true;
				linkBehaviour.setReceivedStrength(power);
			}
		}

		for (IRedstoneLinkable other : network) {
			if (other != actor && other.isListening() && withinRange(actor, other))
				other.setReceivedStrength(power);
		}
	}

	public static boolean withinRange(IRedstoneLinkable from, IRedstoneLinkable to) {
		if (from == to)
			return true;
		return from.getLocation()
			.closerThan(to.getLocation(), AllConfigs.SERVER.logistics.linkRange.get());
	}

	public Map<Pair<Frequency, Frequency>, Set<IRedstoneLinkable>> networksIn(LevelAccessor world) {
		if (!connections.containsKey(world)) {
			Create.LOGGER.warn("Tried to Access unprepared network space of " + WorldHelper.getDimensionID(world));
			return new HashMap<>();
		}
		return connections.get(world);
	}

}
