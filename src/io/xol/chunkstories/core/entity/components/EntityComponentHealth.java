package io.xol.chunkstories.core.entity.components;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.csf.StreamSource;
import io.xol.chunkstories.api.csf.StreamTarget;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.components.EntityComponent;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.core.events.EntityDeathEvent;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class EntityComponentHealth extends EntityComponent
{
	public float health;
	
	public EntityComponentHealth(Entity entity, float health)
	{
		super(entity);
		this.health = health;
	}

	public float getHealth()
	{
		return health;
	}
	
	public void setHealth(float health)
	{
		boolean wasntDead = health > 0.0;
		this.health = health;
		
		if(health < 0.0 && wasntDead)
		{
			EntityDeathEvent entityDeathEvent = new EntityDeathEvent(entity);
			entity.getWorld().getGameLogic().getPluginsManager().fireEvent(entityDeathEvent);
		}
		
		if(entity.getWorld() instanceof WorldMaster)
		{
			if(health > 0.0)
				this.pushComponentController();
			else
				this.pushComponentEveryone();
		}
	}
	
	public void damage(float dmg)
	{
		boolean wasntDead = health > 0.0;
		this.health -= dmg;

		if(health < 0.0 && wasntDead)
		{
			EntityDeathEvent entityDeathEvent = new EntityDeathEvent(entity);
			entity.getWorld().getGameLogic().getPluginsManager().fireEvent(entityDeathEvent);
		}
		
		if(entity.getWorld() instanceof WorldMaster)
		{
			if(health > 0.0)
				this.pushComponentController();
			else
				this.pushComponentEveryone();
		}
	}
	
	@Override
	public void push(StreamTarget destinator, DataOutputStream dos) throws IOException
	{
		dos.writeFloat(health);
	}

	@Override
	public void pull(StreamSource from, DataInputStream dis) throws IOException
	{
		health = dis.readFloat();
	}

}
