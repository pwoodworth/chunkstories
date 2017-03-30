package io.xol.chunkstories.net.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.interfaces.EntityWithInventory;
import io.xol.chunkstories.api.exceptions.PacketProcessingException;
import io.xol.chunkstories.api.item.inventory.Inventory;
import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.api.net.PacketSynchPrepared;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.net.InventoryTranslator;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class PacketOpenInventory extends PacketSynchPrepared
{
	Inventory inventory;
	
	public PacketOpenInventory() {
		
	}
	
	public PacketOpenInventory(Inventory inventory) {
		this.inventory = inventory;
	}

	@Override
	public void sendIntoBuffer(PacketDestinator destinator, DataOutputStream out) throws IOException
	{
		InventoryTranslator.writeInventoryHandle(out, inventory);
	}

	@Override
	public void process(PacketSender sender, DataInputStream in, PacketsProcessor processor) throws IOException, PacketProcessingException
	{
		inventory = InventoryTranslator.obtainInventoryHandle(in, processor);
		
		Entity entity = Client.getInstance().getPlayer().getControlledEntity();
		
		if(entity != null && entity instanceof EntityWithInventory)
			Client.getInstance().openInventories(((EntityWithInventory) entity).getInventory(), inventory);
		else
			Client.getInstance().openInventories(inventory);
	}

}
