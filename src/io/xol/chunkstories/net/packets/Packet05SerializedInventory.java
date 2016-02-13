package io.xol.chunkstories.net.packets;

import io.xol.chunkstories.entity.Entity;
import io.xol.chunkstories.item.inventory.Inventory;
import io.xol.chunkstories.item.inventory.InventoryHolder;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class Packet05SerializedInventory extends Packet
{
	//This packets defines an entire inventory (player's or else)
	public Packet05SerializedInventory(boolean client)
	{
		super(client);
	}

	@Override
	public void send(DataOutputStream out) throws IOException
	{
		//If no inventory given, fuck off
		if(inventory == null)
			return;
		out.writeByte(0x05);
		//Who does it belong to ?
		if(inventory.holder == null)
			out.writeByte(0x00);
		else if(inventory.holder instanceof Entity)
		{
			out.writeByte(0x01);
			out.writeLong(((Entity)inventory.holder).getUUID());
		}
		//Write the inventory
		inventory.save(out);
	}

	@Override
	public void read(DataInputStream in) throws IOException
	{
		holderType = in.readByte();
		if(holderType == 0x01)
			eId = in.readLong();
		//Load the inventory
		inventory = new Inventory(in);
	}

	public Inventory inventory;
	//public InventoryHolder holder;
	byte holderType;
	long eId;
	
	@Override
	public void process(PacketsProcessor processor)
	{
		InventoryHolder holder = null;
		if(holderType == 0x01)
		{
			holder = processor.getWorld().getEntityByUUID(eId);
		}
		if(holder != null)
		{
			holder.setInventory(inventory);
			System.out.println("Processed packet 05SerializedInventory.");
		}
	}

}
