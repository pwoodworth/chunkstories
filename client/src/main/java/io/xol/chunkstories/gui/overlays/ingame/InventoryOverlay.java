package io.xol.chunkstories.gui.overlays.ingame;

import org.lwjgl.input.Mouse;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.events.player.PlayerMoveItemEvent;
import io.xol.chunkstories.api.item.inventory.Inventory;
import io.xol.chunkstories.api.item.inventory.ItemPile;
import io.xol.chunkstories.api.math.vector.sp.Vector4fm;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.core.entity.EntityGroundItem;
import io.xol.chunkstories.core.net.packets.PacketInventoryMoveItemPile;
import io.xol.chunkstories.gui.InventoryDrawer;
import io.xol.chunkstories.gui.OverlayableScene;
import io.xol.chunkstories.world.WorldClientRemote;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.chunkstories.world.WorldClientLocal;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.gui.Overlay;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class InventoryOverlay extends Overlay
{
	Inventory[] inventories;
	InventoryDrawer[] drawers;

	public static ItemPile selectedItem;
	public static int selectedItemAmount;

	public InventoryOverlay(OverlayableScene scene, Overlay parent, Inventory[] entityInventories)
	{
		super(scene, parent);
		this.inventories = entityInventories;
		this.drawers = new InventoryDrawer[entityInventories.length];
		for (int i = 0; i < drawers.length; i++)
			drawers[i] = new InventoryDrawer(entityInventories[i]);
	}

	@Override
	public void drawToScreen(RenderingContext renderer, int x, int y, int w, int h)
	{
		int totalWidth = 0;
		for (Inventory inv : inventories)
			totalWidth += 2 + inv.getWidth();
		totalWidth -= 2;
		int widthAccumulation = 0;
		for (int i = 0; i < drawers.length; i++)
		{
			int thisWidth = inventories[i].getWidth();
			drawers[i].drawInventoryCentered(renderer, renderer.getWindow().getWidth() / 2 - totalWidth * 24 + thisWidth * 24 + widthAccumulation * 48, renderer.getWindow().getHeight() / 2, 2, false, 4 - i * 4);
			widthAccumulation += 1 + thisWidth;

			int[] highlightedSlot = drawers[i].getSelectedSlot();
			if (highlightedSlot != null)
			{
				ItemPile pileHighlighted = inventories[i].getItemPileAt(highlightedSlot[0], highlightedSlot[1]);
				if (pileHighlighted != null)
				{
					int mx = Mouse.getX();
					int my = Mouse.getY();
					
					renderer.getFontRenderer().drawString(renderer.getFontRenderer().defaultFont(), mx, my, pileHighlighted.getItem().getName(), 2);
					//System.out.println(pileHighlighted);
				}
			}
		}

		if (selectedItem != null)
		{
			int slotSize = 24 * 2;
			
			int width = slotSize * selectedItem.getItem().getType().getSlotsWidth();
			int height = slotSize * selectedItem.getItem().getType().getSlotsHeight();
			//GuiDrawer.drawBoxWindowsSpaceWithSize(Mouse.getX() - width / 2, Mouse.getY() - height / 2, width, height, 0, 1, 1, 0, textureId, true, true, null);

			//
			selectedItem.getItem().getType().getRenderer().renderItemInInventory(renderer, selectedItem, Mouse.getX() - width / 2, Mouse.getY() - height / 2, 2);

			if (selectedItemAmount != 1)
				renderer.getFontRenderer().drawStringWithShadow(renderer.getFontRenderer().defaultFont(), Mouse.getX() - width / 2 + (selectedItem.getItem().getType().getSlotsWidth() - 1.0f) * slotSize, Mouse.getY() - height / 2, selectedItemAmount + "", 2, 2,
						new Vector4fm(1, 1, 1, 1));

		}
		//System.out.println(inventories[0]);
	}

	@Override
	public boolean handleKeypress(int k)
	{
		if (Client.getInstance().getInputsManager().getInputByName("exit").isPressed())
		{
			//Exit
			this.mainScene.changeOverlay(parent);
			selectedItem = null;
		}
		return true;
	}

	@Override
	public boolean onClick(int posx, int posy, int button)
	{
		//We assume a player has to be spawned in order to do items manipulation
		Player player = Client.getInstance().getPlayer();
		if(player == null)
		{
			this.mainScene.changeOverlay(parent);
			selectedItem = null;
			return false;
		}
		
		for (int i = 0; i < drawers.length; i++)
		{
			//Close button
			if (drawers[i].isOverCloseButton())
			{
				this.mainScene.changeOverlay(parent);
				selectedItem = null;
			}
			else
			{
				
				int[] c = drawers[i].getSelectedSlot();
				if (c == null)
					continue;
				
				else
				{
					int x = c[0];
					int y = c[1];
					if (selectedItem == null)
					{
						if (button == 0)
						{
							selectedItem = inventories[i].getItemPileAt(x, y);
							selectedItemAmount = selectedItem == null ? 0 : selectedItem.getAmount();
						}
						else if (button == 1)
						{
							selectedItem = inventories[i].getItemPileAt(x, y);
							selectedItemAmount = selectedItem == null ? 0 : 1;
						}
						else if (button == 2)
						{
							selectedItem = inventories[i].getItemPileAt(x, y);
							selectedItemAmount = selectedItem == null ? 0 : (selectedItem.getAmount() > 1 ? selectedItem.getAmount() / 2 : 1);
						}
						//selectedItemInv = inventory;
					}
					else if (button == 1)
					{
						if (selectedItem.equals(inventories[i].getItemPileAt(x, y)))
						{
							if (selectedItemAmount < inventories[i].getItemPileAt(x, y).getAmount())
								selectedItemAmount++;
						}
					}
					else if (button == 0)
					{
						//Ignore null-sum games
						if (selectedItem.getInventory() == inventories[i] && x == selectedItem.getX() && y == selectedItem.getY())
						{
							selectedItem = null;
							return true;
						}

						if (Client.world instanceof WorldClientLocal)
						{
							PlayerMoveItemEvent moveItemEvent = new PlayerMoveItemEvent(player, selectedItem, selectedItem.getInventory(), inventories[i], selectedItem.getX(), selectedItem.getY(), x, y, selectedItemAmount);
							player.getContext().getPluginManager().fireEvent(moveItemEvent);
							
							//If move was successfull
							if(!moveItemEvent.isCancelled())
								selectedItem.moveItemPileTo(inventories[i], x, y, selectedItemAmount);
							
							selectedItem = null;
						}
						else if (Client.world instanceof WorldClientRemote)
						{
							//When in a remote MP scenario, send a packet
							PacketInventoryMoveItemPile packetMove = new PacketInventoryMoveItemPile(selectedItem, selectedItem.getInventory(), inventories[i], selectedItem.getX(), selectedItem.getY(), x, y, selectedItemAmount);
							((WorldClientRemote) Client.world).getConnection().pushPacket(packetMove);
							
							//And unsellect item
							selectedItem = null;
						}
						
						/*else if (selectedItem.moveItemPileTo(inventories[i], x, y, selectedItemAmount))
							selectedItem = null;*/
					}
					return true;
				}
			}
		}
		
		//Clicked outside of any other inventory (drop!)
		if(selectedItem != null)
		{
			//SP scenario, replicated logic in PacketInventoryMoveItemPile
			if (Client.world instanceof WorldClientLocal)
			{
				//For local item drops, we need to make sure we have a sutiable entity
				Entity playerEntity = player.getControlledEntity();
				if(playerEntity != null)
				{
					PlayerMoveItemEvent dropItemEvent = new PlayerMoveItemEvent(player, selectedItem, selectedItem.getInventory(), null, selectedItem.getX(), selectedItem.getY(), 0, 0, selectedItemAmount);
					player.getContext().getPluginManager().fireEvent(dropItemEvent);
					
					if(!dropItemEvent.isCancelled())
					{
						//If we're pulling this out of an inventory ( and not /dev/null ), we need to remove it from that
						if(dropItemEvent.getSourceInventory() != null)
							dropItemEvent.getSourceInventory().setItemPileAt(dropItemEvent.getFromX(), dropItemEvent.getFromY(), null);
						
						//Spawn a new ground item
						Location loc = playerEntity.getLocation();
						EntityGroundItem entity = new EntityGroundItem((WorldImplementation) loc.getWorld(), loc.getX(), loc.getY(), loc.getZ(), selectedItem);
						loc.getWorld().addEntity(entity);
						
						player.sendMessage("Notice : throwing stuff on ground is still glitchy and experimental.");
					}
				}
				selectedItem = null;
			}
			//In MP scenario, move into /dev/null
			else if (Client.world instanceof WorldClientRemote)
			{
				PacketInventoryMoveItemPile packetMove = new PacketInventoryMoveItemPile(selectedItem, selectedItem.getInventory(), null, selectedItem.getX(), selectedItem.getY(), 0, 0, selectedItemAmount);
				((WorldClientRemote) Client.world).getConnection().pushPacket(packetMove);
				
				selectedItem = null;
			}
		}
		return true;

	}
}
