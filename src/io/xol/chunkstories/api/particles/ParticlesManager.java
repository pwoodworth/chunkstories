package io.xol.chunkstories.api.particles;

import io.xol.engine.math.lalgb.vector.dp.Vector3dm;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface ParticlesManager
{
	public void spawnParticleAtPosition(String particleTypeName, Vector3dm position);
	
	public void spawnParticleAtPositionWithVelocity(String particleTypeName, Vector3dm position, Vector3dm velocity);
}
