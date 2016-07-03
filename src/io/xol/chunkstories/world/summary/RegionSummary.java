package io.xol.chunkstories.world.summary;

import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.voxel.VoxelTypes;
import io.xol.chunkstories.world.WorldImplementation;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicBoolean;

import org.lwjgl.BufferUtils;

import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.ARBTextureRg.*;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

/**
 * A region summary contains metadata about an 8x8 chunks ( or 256x256 blocks ) vertical slice of the world
 */
public class RegionSummary
{
	// LZ4 compressors & decompressors
	static LZ4Factory factory = LZ4Factory.fastestInstance();
	public static LZ4Compressor compressor = factory.highCompressor(10);
	public static LZ4FastDecompressor decompressor = factory.fastDecompressor();

	public WorldImplementation world;
	public int regionX, regionZ;

	public final File handler;
	public AtomicBoolean summaryLoaded = new AtomicBoolean(false);

	public int[] heights;
	public int[] ids;
	private int[][] minChunkHeight = new int[8][8];

	//Textures (client renderer)
	//TODO use real textures
	public AtomicBoolean texturesUpToDate = new AtomicBoolean(false);
	public int heightsTextureId = -1;
	public int voxelTypesTextureId = -1;

	//Mesh (client renderer)
	public int vboId = -1;
	public int vboSize = 0;

	private byte[] vboDataToUpload = null;

	public RegionSummary(WorldImplementation world, int rx, int rz)
	{
		this.world = world;
		this.regionX = rx;
		this.regionZ = rz;

		// 512kb per summary, use of max mipmaps for heights
		heights = new int[(int) Math.ceil(256 * 256 * (1 + 1 / 3D))];
		ids = new int[256 * 256];

		if (world instanceof WorldMaster)
			handler = new File(world.getFolderPath() + "/summaries/" + rx + "." + rz + ".sum");
		else
			handler = null;

		load();
	}

	private void load()
	{
		//this.handler = handler;
		this.world.ioHandler.requestChunkSummaryLoad(this);
	}

	public void save()
	{
		//this.handler = handler;
		this.world.ioHandler.requestChunkSummarySave(this);
	}

	private int index(int x, int z)
	{
		return x * 256 + z;
	}

	public void set(int x, int y, int z, int t)
	{
		Voxel voxel = VoxelTypes.get(t);
		int h = getHeight(x, z);
		//If we place something solid over the last solid thing
		if ((voxel.isVoxelSolid() || voxel.isVoxelLiquid()) && y >= h)
		{
			if (y >= h)
			{
				heights[index(x, z)] = y;
				ids[index(x, z)] = t;
			}
		}
		else
		{
			// If removing the top block, start a loop to find bottom.
			if (y == h)
			{
				boolean loaded = false;
				boolean solid = false;
				boolean liquid = false;
				do
				{
					y--;
					loaded = world.isChunkLoaded(x / 32, y / 32, z / 32);
					solid = VoxelTypes.get(world.getDataAt(x, y, z, false)).isVoxelSolid();
					liquid = VoxelTypes.get(world.getDataAt(x, y, z, false)).isVoxelLiquid();
				}
				while (y >= 0 && loaded && !solid && !liquid);

				heights[index(x, z)] = y;
				ids[index(x, z)] = t;
			}
		}
	}

	public int getMinChunkHeight(int x, int z)
	{
		int cx = x / 32;
		int cz = z / 32;
		return this.minChunkHeight[cx][cz];
	}

	public int getHeight(int x, int z)
	{
		return heights[index(x, z)];
	}

	public int getID(int x, int z)
	{
		return ids[index(x, z)];
	}

	public void forceSet(int x, int y, int z, int t)
	{
		heights[index(x, z)] = y;
		ids[index(x, z)] = t;
	}

	public float dekalX;
	public float dekalZ;

	public boolean uploadNeededData()
	{
		return uploadTextures() || uploadModel();
	}

	private boolean uploadModel()
	{
		if (vboDataToUpload == null)
			return false;
		synchronized (vboDataToUpload)
		{
			if (vboId == -1)
				vboId = glGenBuffers();

			vboSize = 0;
			ByteBuffer byteBuffer = BufferUtils.createByteBuffer(vboDataToUpload.length);
			byteBuffer.put(vboDataToUpload);
			byteBuffer.flip();

			glBindBuffer(GL_ARRAY_BUFFER, vboId);
			glBufferData(GL_ARRAY_BUFFER, byteBuffer, GL_DYNAMIC_DRAW);
			vboSize = vboDataToUpload.length / 12;

			vboDataToUpload = null;
		}
		return true;
	}

	private boolean uploadTextures()
	{
		if (!summaryLoaded.get())
			return false;
		if (voxelTypesTextureId == -1)
		{
			heightsTextureId = glGenTextures();
			voxelTypesTextureId = glGenTextures();
		}
		if (texturesUpToDate.get())
			return false;

		//Upload stuff
		glBindTexture(GL_TEXTURE_2D, heightsTextureId);
		ByteBuffer bb = ByteBuffer.allocateDirect(4 * 256 * 256);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		for (int i = 0; i < 256 * 256; i++)
		{
			bb.putFloat(heights[i]);
		}

		bb.flip(); // 0x822e is GL_R32F, 0x8235 is GL_R32I
		glTexImage2D(GL_TEXTURE_2D, 0, GL_R32F /* GL_R32F */, 256, 256, 0, GL_RED/* GL_RED_INTEGER */, GL_FLOAT, bb);

		//Setup texture filtering
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

		//Generate mipmaps
		/*if (FastConfig.openGL3Capable)
			GL30.glGenerateMipmap(GL_TEXTURE_2D);
		else if (FastConfig.fbExtCapable)
			ARBFramebufferObject.glGenerateMipmap(GL_TEXTURE_2D);*/

		//Upload stuff
		bb = ByteBuffer.allocateDirect(4 * 256 * 256);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		for (int i = 0; i < 256 * 256; i++)
		{
			int id = ids[i];
			bb.putFloat(id & 0x0000FFFF);
		}
		bb.rewind();
		glBindTexture(GL_TEXTURE_2D, voxelTypesTextureId);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_R32F /* GL_R32F */, 256, 256, 0, GL_RED/* GL_RED_INTEGER */, GL_FLOAT, bb);

		//Setup texture filtering
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

		//Generate mipmaps
		/*if (FastConfig.openGL3Capable)
			GL30.glGenerateMipmap(GL_TEXTURE_2D);
		else if (FastConfig.fbExtCapable)
			ARBFramebufferObject.glGenerateMipmap(GL_TEXTURE_2D);*/

		//Tell world renderer
		((WorldClient) this.world).getWorldRenderer().farTerrainRenderer.markFarTerrainMeshDirty();
		
		//Flag it
		texturesUpToDate.set(true);
		return true;
	}

	public void free()
	{
		if (voxelTypesTextureId != -1)
		{
			glDeleteTextures(heightsTextureId);
			glDeleteTextures(voxelTypesTextureId);
		}
		if (vboId != -1)
			glDeleteBuffers(vboId);
	}

	public void sendNewModel(byte[] newModelData)
	{
		synchronized(vboDataToUpload)
		{
			vboDataToUpload = newModelData;
		}
	}
		
	public boolean isLoaded()
	{
		return summaryLoaded.get();
	}

	public void computeHeightMetadata()
	{
		//Reset
		for (int i = 0; i < 8; i++)
			for (int j = 0; j < 8; j++)
			{
				this.minChunkHeight[i][j] = world.getMaxHeight();
			}

		for (int x = 0; x < 256; x++)
			for (int z = 0; z < 256; z++)
			{
				int cx = x / 32;
				int cz = z / 32;
				if (this.getHeight(x, z) < this.minChunkHeight[cx][cz])
					this.minChunkHeight[cx][cz] = this.getHeight(x, z);
			}
		//Max mipmaps

		int resolution = 128;
		int offset = 0;
		while (resolution > 1)
		{
			for (int x = 0; x < resolution; x++)
				for (int z = 0; z < resolution; z++)
				{
					//Fetch from the current resolution
					int v00 = heights[offset + (resolution * 2) * (x * 2) + (z * 2)];
					int v01 = heights[offset + (resolution * 2) * (x * 2) + (z * 2 + 1)];
					int v10 = heights[offset + (resolution * 2) * (x * 2 + 1) + (z * 2)];
					int v11 = heights[offset + (resolution * 2) * (x * 2 + 1) + (z * 2) + 1];
					//Max out
					int max = max(max(v00, v01), max(v10, v11));

					//Skip the already passed steps and the current resolution being sampled data to go write the next one
					heights[offset + (resolution * 2) * (resolution * 2) + resolution * x + z] = max;
				}

			offset += resolution * 2 * resolution * 2;
			resolution /= 2;
		}
	}

	private int max(int a, int b)
	{
		if (a > b)
			return a;
		return b;
	}

	static int[] offsets = { 0, 65536, 81920, 86016, 87040, 87296, 87360, 87376, 87380, 87381 };

	public int getHeightMipmapped(int x, int z, int level)
	{
		if (level > 8)
			return -1;
		int resolution = 256 >> level;
		x >>= level;
		z >>= level;
		int offset = offsets[level];
		return heights[offset + resolution * x + z];
	}
}
