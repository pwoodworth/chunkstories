//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.voxel;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.xol.chunkstories.api.client.ClientContent;
import io.xol.chunkstories.api.content.Asset;
import io.xol.chunkstories.api.content.Content.Voxels;
import io.xol.chunkstories.api.exceptions.content.IllegalVoxelDeclarationException;
import io.xol.chunkstories.api.rendering.voxel.VoxelRenderer;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.content.GameContentStore;
import io.xol.chunkstories.voxel.material.VoxelMaterialsStore;
import io.xol.chunkstories.voxel.models.VoxelModelsStore;

public class VoxelsStore implements ClientContent.ClientVoxels
{
	private final GameContentStore content;

	private final VoxelMaterialsStore materials;
	private final VoxelTexturesStoreAndAtlaser textures;
	private final VoxelModelsStore models;
	
	private VoxelRenderer defaultVoxelRenderer;
	
	public Map<String, Voxel> voxelsByName = new HashMap<String, Voxel>();
	public int voxelTypes = 0;
	public int lastAllocatedId;

	private Voxel air;

	public VoxelsStore(GameContentStore content)
	{
		this.content = content;

		this.materials = new VoxelMaterialsStore(this);
		this.textures = new VoxelTexturesStoreAndAtlaser(this);
		this.models = new VoxelModelsStore(this);
		//this.reloadVoxelTypes();
	}
	
	@Override
	public VoxelMaterialsStore materials() {
		return materials;
	}
	
	@Override
	public VoxelTexturesStoreAndAtlaser textures()
	{
		return textures;
	}

	@Override
	public VoxelModelsStore models()
	{
		return models;
	}
	
	public void reload()
	{
		this.materials.reload();
		this.textures.buildTextureAtlas();
		this.models.resetAndLoadModels();
		
		this.reloadVoxelTypes();
	}
	
	@SuppressWarnings("unchecked")
	private void reloadVoxelTypes()
	{
		//Discard previous voxels
		//Arrays.fill(voxels, null);
		//attributedIds.clear();
		voxelsByName.clear();
		
		//First load the default voxel renderer
		try {
			Class<VoxelRenderer> defaultVoxelRendererClass = (Class<VoxelRenderer>) content.modsManager().getClassByName("io.xol.chunkstories.core.voxel.renderers.DefaultVoxelRenderer");
			Constructor<VoxelRenderer> defaultVoxelRendererConstructor = defaultVoxelRendererClass.getConstructor(Voxels.class);
			defaultVoxelRenderer = defaultVoxelRendererConstructor.newInstance(this);
		}
		catch(Exception e) {
			System.out.println("could not instanciate the default voxel renderer");
			System.exit(-900);
		}
		
		Iterator<Asset> i = content.modsManager().getAllAssetsByExtension("voxels");
		while (i.hasNext())
		{
			Asset f = i.next();
			logger().debug("Reading voxels definitions in : " + f);
			readVoxelsDefinitions(f);
		}
	}
	
	private void readVoxelsDefinitions(Asset f)
	{
		if (f == null)
			return;
		try
		{
			BufferedReader reader = new BufferedReader(f.reader());

			String line = "";

			//Voxel voxel = null;
			int ln = 0;
			int loadedVoxels = 0;
			while ((line = reader.readLine()) != null)
			{
				line = line.replace("\t", "");
				if (line.startsWith("#"))
				{
					// It's a comment, ignore.
				}
				else
				{
					if (line.startsWith("voxel"))
					{
						String splitted[] = line.split(" ");
						
						if(splitted.length < 2)
						{
							logger().warn("Parse error in file " + f + ", line " + ln + ", malformed voxel tag. Aborting read.");
							break;
						}
						
						//int id = Integer.parseInt(splitted[2]);
						String name = splitted[1];
						
						//if (voxels[id] != null)
						//	logger().warn("Voxel redefinition in file " + f + ", line " + ln + ", overriding id " + id + " with " + name);

						try
						{
							VoxelDefinitionImplementation voxelType = new VoxelDefinitionImplementation(this, name, reader);
							Voxel voxel = voxelType.getVoxelObject();
							
							voxelsByName.put(voxel.getName(), voxel);
							loadedVoxels++;
							
							if(name.equals("air"))
								air = voxel;
						}
						catch (IllegalVoxelDeclarationException e)
						{
							e.printStackTrace();
						}
					}
					else if (line.startsWith("end"))
					{
						logger().warn("Parse error in file " + f + ", line " + ln + ", unexpected 'end' token.");
					}
				}
				ln++;
			}
			logger().debug("Parsed file " + f + " correctly, loading " + loadedVoxels + " voxels.");

			reader.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public Voxel getVoxel(String voxelName)
	{
		return voxelsByName.get(voxelName);
	}

	@Override
	public Iterator<Voxel> all()
	{
		return voxelsByName.values().iterator();
	}

	@Override
	public GameContentStore parent()
	{
		return content;
	}

	@Override
	public VoxelRenderer getDefaultVoxelRenderer() {
		return defaultVoxelRenderer;
	}
	
	private static final Logger logger = LoggerFactory.getLogger("content.voxels");
	public Logger logger() {
		return logger;
	}

	@Override
	public Voxel air() {
		return air;
	}


}
