package io.xol.chunkstories.world.io;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import io.xol.chunkstories.net.packets.PacketChunkCompressedData;
import io.xol.chunkstories.net.packets.PacketRegionSummary;
import io.xol.chunkstories.server.net.UserConnection;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.chunkstories.world.region.RegionImplementation;
import io.xol.chunkstories.world.summary.RegionSummaryImplementation;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class IOTasksMultiplayerServer extends IOTasks
{
	public IOTasksMultiplayerServer(WorldImplementation world)
	{
		super(world);
		try
		{
			md = MessageDigest.getInstance("MD5");
		}
		catch (NoSuchAlgorithmException e)
		{
			e.printStackTrace();
		}
	}
	MessageDigest md = null;
	
	class IOTaskSendCompressedChunk extends IOTask
	{
		UserConnection client;
		int chunkX, chunkY, chunkZ;
		
		public IOTaskSendCompressedChunk(int x, int y, int z, UserConnection client)
		{
			this.client = client;
			this.chunkX = x;
			this.chunkY = y;
			this.chunkZ = z;
		}
		
		@Override
		public boolean run()
		{
			try
			{
				//Don't bother if the client died.
				if(!client.isAlive())
					return true;
				
				RegionImplementation holder = world.getRegionsHolder().getRegionChunkCoordinates(chunkX, chunkY, chunkZ);
				if(holder == null) {
				
					//System.out.println("world.getRegionsHolder().getRegionChunkCoordinates("+chunkX+", "+chunkY+", "+chunkZ+")");
					return false;
				}
				
				if(holder.isDiskDataLoaded())
				{
					PacketChunkCompressedData packet = new PacketChunkCompressedData();
					packet.setPosition(chunkX, chunkY, chunkZ);
					packet.data = holder.getCompressedData(chunkX, chunkY, chunkZ);
					client.pushPacket(packet);
					
					return true;
				}
				else
				{
					return false;
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			return true;
		}

		@Override
		public boolean equals(Object o)
		{
			if(o != null && o instanceof IOTaskSendCompressedChunk)
			{
				IOTaskSendCompressedChunk comp = ((IOTaskSendCompressedChunk)o);
				if(comp.client.equals(this.client) && comp.chunkX == this.chunkX && comp.chunkY == this.chunkY && comp.chunkZ == this.chunkZ)
					return true;
			}
			return false;
		}
		
		@Override
		public int hashCode()
		{
			return (-65536 + 7777 * chunkX + 256 * chunkY + chunkZ) % 2147483647;
		}
	}

	public void requestCompressedChunkSend(int x, int y, int z, UserConnection sender)
	{
		IOTaskSendCompressedChunk task = new IOTaskSendCompressedChunk(x, y, z, sender);
		scheduleTask(task);
	}
	
	class IOTaskSendRegionSummary extends IOTask
	{
		UserConnection client;
		int rx, rz;
		
		public IOTaskSendRegionSummary(int x, int z, UserConnection client)
		{
			this.client = client;
			this.rx = x;
			this.rz = z;
		}
		
		@Override
		public boolean run()
		{
			try
			{
				//Don't bother if the client died.
				if(!client.isAlive())
					return true;
				
				//Player player = client.getProfile();
				//int x = rx * 256;
				//int z = rz * 256;
				
				//int px = (int)(double)player.getLocation().getX();
				//int pz = (int)(double)player.getLocation().getY();
				
				//double dx = LoopingMathHelper.moduloDistance(x, px, player.getWorld().getWorldSize());
				//double dz = LoopingMathHelper.moduloDistance(z, pz, player.getWorld().getWorldSize());
				
				//System.out.println("dx"+dx+"dz"+dz);
				//if(dx > 1024 || dz > 1024)
				//	return true;
				
				RegionSummaryImplementation summary = world.getRegionsSummariesHolder().getRegionSummaryWorldCoordinates(rx * 256, rz * 256);
				
				//Don't send the data until we have the summary loaded in
				if(summary == null || !summary.isLoaded())
					return false;
				
				PacketRegionSummary packet = new PacketRegionSummary();
				packet.summary = summary;
				client.pushPacket(packet);
				return true;
				
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			return true;
		}
	}

	public void requestRegionSummary(int x, int z, UserConnection sender)
	{
		IOTaskSendRegionSummary task = new IOTaskSendRegionSummary(x, z, sender);
		scheduleTask(task);
	}
}
