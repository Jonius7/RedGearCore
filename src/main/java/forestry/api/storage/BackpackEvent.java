package forestry.api.storage;

import cpw.mods.fml.common.eventhandler.Event;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;

public abstract class BackpackEvent extends Event {

	public final EntityPlayer player;
	public final IBackpackDefinition backpackDefinition;
	public final IInventory backpackInventory;

	public BackpackEvent(EntityPlayer player, IBackpackDefinition backpackDefinition, IInventory backpackInventory) {
		this.player = player;
		this.backpackDefinition = backpackDefinition;
		this.backpackInventory = backpackInventory;
	}
}
