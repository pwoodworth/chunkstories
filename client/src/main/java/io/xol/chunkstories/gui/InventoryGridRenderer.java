//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.gui;

import org.joml.Vector4f;

import io.xol.chunkstories.api.entity.components.EntityInventory;
import io.xol.chunkstories.api.entity.components.EntitySelectedItem;
import io.xol.chunkstories.api.input.Mouse;
import io.xol.chunkstories.api.item.inventory.Inventory;
import io.xol.chunkstories.api.item.inventory.ItemPile;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.textures.Texture2D;
import io.xol.chunkstories.gui.layer.ingame.InventoryView;

/** Helps with rendering the inventory grid */
public class InventoryGridRenderer {
	private Inventory inventory;

	public InventoryGridRenderer(Inventory entityInventories) {
		this.inventory = entityInventories;
	}

	public void drawInventoryCentered(RenderingInterface renderer, int x, int y, int scale, boolean summary,
			int blankLines) {
		drawInventory(renderer, x - slotsWidth(getInventory().getWidth(), scale) / 2,
				y - slotsHeight(getInventory().getHeight(), scale, summary, blankLines) / 2, scale, summary, blankLines,
				-1);
	}

	int[] selectedSlot;
	boolean closedButton = false;

	public int[] getSelectedSlot() {
		return selectedSlot;
	}

	public boolean isOverCloseButton() {
		return closedButton;
	}

	public void drawPlayerInventorySummary(RenderingInterface renderer, int x, int y) {
		int selectedSlot = -1;
		if(inventory instanceof EntityInventory) {
			EntitySelectedItem esi = ((EntityInventory)inventory).entity.components.get(EntitySelectedItem.class);
			if(esi != null)
				selectedSlot = esi.getSelectedSlot();
		}
		
		drawInventory(renderer, x - slotsWidth(getInventory().getWidth(), 2) / 2,
				y - slotsHeight(getInventory().getHeight(), 2, true, 0) / 2, 2, true, 0, selectedSlot);
	}

	public void drawInventory(RenderingInterface renderer, int x, int y, int scale, boolean summary, int blankLines,
			int highlightSlot) {
		Mouse mouse = renderer.getClient().getInputsManager().getMouse();
		if (getInventory() == null)
			return;

		int cornerSize = 8 * scale;
		int internalWidth = getInventory().getWidth() * 24 * scale;

		int height = summary ? 1 : getInventory().getHeight();

		int internalHeight = (height + (summary ? 0 : 1) + blankLines) * 24 * scale;
		int slotSize = 24 * scale;

		Texture2D inventoryTexture = renderer.textures().getTexture("./textures/gui/inventory/inventory.png");
		inventoryTexture.setLinearFiltering(false);

		Vector4f color = new Vector4f(1f, 1f, 1f, summary ? 0.5f : 1f);
		// All 8 corners
		renderer.getGuiRenderer().drawBoxWindowsSpaceWithSize(x, y + internalHeight + cornerSize, cornerSize, cornerSize,
				0, 0.03125f, 0.03125f, 0, inventoryTexture, true, true, color);
		renderer.getGuiRenderer().drawBoxWindowsSpaceWithSize(x + cornerSize, y + internalHeight + cornerSize,
				internalWidth, cornerSize, 0.03125f, 0.03125f, 0.96875f, 0, inventoryTexture, true, true, color);
		renderer.getGuiRenderer().drawBoxWindowsSpaceWithSize(x + cornerSize + internalWidth,
				y + internalHeight + cornerSize, cornerSize, cornerSize, 0.96875f, 0.03125f, 1f, 0, inventoryTexture,
				true, true, color);
		renderer.getGuiRenderer().drawBoxWindowsSpaceWithSize(x, y, cornerSize, cornerSize, 0, 1f, 0.03125f, 248 / 256f,
				inventoryTexture, true, true, color);
		renderer.getGuiRenderer().drawBoxWindowsSpaceWithSize(x + cornerSize, y, internalWidth, cornerSize, 0.03125f, 1f,
				0.96875f, 248 / 256f, inventoryTexture, true, true, color);
		renderer.getGuiRenderer().drawBoxWindowsSpaceWithSize(x + cornerSize + internalWidth, y, cornerSize, cornerSize,
				0.96875f, 1f, 1f, 248 / 256f, inventoryTexture, true, true, color);
		renderer.getGuiRenderer().drawBoxWindowsSpaceWithSize(x, y + cornerSize, cornerSize, internalHeight, 0,
				248f / 256f, 0.03125f, 8f / 256f, inventoryTexture, true, true, color);
		renderer.getGuiRenderer().drawBoxWindowsSpaceWithSize(x + cornerSize + internalWidth, y + cornerSize, cornerSize,
				internalHeight, 248 / 256f, 248f / 256f, 1f, 8f / 256f, inventoryTexture, true, true, color);
		// Actual inventory slots
		int sumSlots2HL = 0;
		selectedSlot = null;
		for (int i = 0; i < getInventory().getWidth(); i++) {
			for (int j = 0; j < height; j++) {
				boolean mouseOver = mouse.getCursorX() > x + cornerSize + i * slotSize
						&& mouse.getCursorX() <= x + cornerSize + i * slotSize + slotSize
						&& mouse.getCursorY() > y + cornerSize + j * slotSize
						&& mouse.getCursorY() <= y + cornerSize + j * slotSize + slotSize;
				// Just a dirt hack to always keep selected slot values where we want them
				if (mouseOver && selectedSlot == null) {
					selectedSlot = new int[] { i, j };
				}

				ItemPile selectedPile = null;
				if (selectedSlot != null)
					selectedPile = getInventory().getItemPileAt(selectedSlot[0], selectedSlot[1]);
				ItemPile thisPile = getInventory().getItemPileAt(i, j);

				if (summary) {
					ItemPile summaryBarSelected = getInventory().getItemPileAt(highlightSlot, 0);
					if (summaryBarSelected != null && i == summaryBarSelected.getX()) {
						sumSlots2HL = summaryBarSelected.getItem().getDefinition().getSlotsWidth();
					}
					if (sumSlots2HL > 0 || (summaryBarSelected == null && highlightSlot == i)) {
						sumSlots2HL--;
						renderer.getGuiRenderer().drawBoxWindowsSpaceWithSize(x + cornerSize + i * slotSize,
								y + cornerSize + j * slotSize, slotSize, slotSize, 32f / 256f, 176 / 256f, 56 / 256f,
								152 / 256f, inventoryTexture, true, true, color);
					} else
						renderer.getGuiRenderer().drawBoxWindowsSpaceWithSize(x + cornerSize + i * slotSize,
								y + cornerSize + j * slotSize, slotSize, slotSize, 8f / 256f, 176 / 256f, 32f / 256f,
								152 / 256f, inventoryTexture, true, true, color);

				} else {
					if (mouseOver || (selectedPile != null && thisPile != null && selectedPile.getX() == thisPile.getX()
							&& selectedPile.getY() == thisPile.getY())) {
						renderer.getGuiRenderer().drawBoxWindowsSpaceWithSize(x + cornerSize + i * slotSize,
								y + cornerSize + j * slotSize, slotSize, slotSize, 32f / 256f, 176 / 256f, 56 / 256f,
								152 / 256f, inventoryTexture, true, true, color);
					} else
						renderer.getGuiRenderer().drawBoxWindowsSpaceWithSize(x + cornerSize + i * slotSize,
								y + cornerSize + j * slotSize, slotSize, slotSize, 8f / 256f, 176 / 256f, 32f / 256f,
								152 / 256f, inventoryTexture, true, true, color);

				}
			}
		}
		
		// Blank part ( usefull for special inventories, ie player )
		for (int j = getInventory().getHeight(); j < getInventory().getHeight() + blankLines; j++) {
			for (int i = 0; i < getInventory().getWidth(); i++) {
				if (j == getInventory().getHeight()) {
					if (i == getInventory().getWidth() - 1)
						renderer.getGuiRenderer().drawBoxWindowsSpaceWithSize(x + cornerSize + i * slotSize,
								y + cornerSize + j * slotSize, slotSize, slotSize, 224f / 256f, 152 / 256f, 248 / 256f,
								128 / 256f, inventoryTexture, true, true, color);
					else
						renderer.getGuiRenderer().drawBoxWindowsSpaceWithSize(x + cornerSize + i * slotSize,
								y + cornerSize + j * slotSize, slotSize, slotSize, 8f / 256f, 152 / 256f, 32f / 256f,
								128 / 256f, inventoryTexture, true, true, color);
				} else {
					if (i == getInventory().getWidth() - 1)
						renderer.getGuiRenderer().drawBoxWindowsSpaceWithSize(x + cornerSize + i * slotSize,
								y + cornerSize + j * slotSize, slotSize, slotSize, 224f / 256f, 56 / 256f, 248 / 256f,
								32 / 256f, inventoryTexture, true, true, color);
					else
						renderer.getGuiRenderer().drawBoxWindowsSpaceWithSize(x + cornerSize + i * slotSize,
								y + cornerSize + j * slotSize, slotSize, slotSize, 8f / 256f, 56 / 256f, 32f / 256f,
								32 / 256f, inventoryTexture, true, true, color);
				}
			}
		}
		// Top part
		if (!summary) {
			renderer.getGuiRenderer().drawBoxWindowsSpaceWithSize(x + cornerSize,
					y + cornerSize + internalHeight - slotSize, slotSize, slotSize, 8f / 256f, 32f / 256f, 32f / 256f,
					8f / 256f, inventoryTexture, true, true, color);

			for (int i = 1; i < getInventory().getWidth() - 2; i++) {
				renderer.getGuiRenderer().drawBoxWindowsSpaceWithSize(x + cornerSize + i * slotSize,
						y + cornerSize + internalHeight - slotSize, slotSize, slotSize, 32f / 256f, 32f / 256f,
						56f / 256f, 8f / 256f, inventoryTexture, true, true, color);
			}
			renderer.getGuiRenderer().drawBoxWindowsSpaceWithSize(
					x + cornerSize + (getInventory().getWidth() - 2) * slotSize,
					y + cornerSize + internalHeight - slotSize, slotSize, slotSize, 200f / 256f, 32f / 256f, 224 / 256f,
					8f / 256f, inventoryTexture, true, true, color);
			closedButton = mouse.getCursorX() > x + cornerSize + (getInventory().getWidth() - 1) * slotSize
					&& mouse.getCursorX() <= x + cornerSize + (getInventory().getWidth() - 1) * slotSize + slotSize
					&& mouse.getCursorY() > y + cornerSize + internalHeight - slotSize
					&& mouse.getCursorY() <= y + cornerSize + internalHeight;

			renderer.getGuiRenderer().drawBoxWindowsSpaceWithSize(
					x + cornerSize + (getInventory().getWidth() - 1) * slotSize,
					y + cornerSize + internalHeight - slotSize, slotSize, slotSize, 224f / 256f, 32f / 256f,
					248f / 256f, 8f / 256f, inventoryTexture, true, true, color);

			renderer.getFontRenderer().drawStringWithShadow(renderer.getFontRenderer().getFont("LiberationSans-Bold", 12),
					x + cornerSize + 6, y + cornerSize + internalHeight - slotSize + 2 * scale,
					getInventory().getInventoryName(), scale, scale, new Vector4f(1, 1, 1, 1));
		}

		// Get rid of any remaining GUI elements or else they will draw on top of the
		// items
		renderer.getGuiRenderer().drawBuffer();

		// Draw the actual items
		for (ItemPile pile : getInventory()) {
			int i = pile.getX();
			int j = pile.getY();
			if (pile != null && (!summary || j == 0)) {
				int center = summary ? slotSize * (pile.getItem().getDefinition().getSlotsHeight() - 1) / 2 : 0;
				pile.getItem().getDefinition().getRenderer().renderItemInInventory(renderer, pile,
						x + cornerSize + i * slotSize, y - center + cornerSize + j * slotSize, scale);
			}
		}

		// Draws the item's text ( done later to allow fontRenderer to pool their draws
		// )
		for (ItemPile pile : getInventory()) {
			int i = pile.getX();
			int j = pile.getY();

			if (pile != null && (!summary || j == 0)) {
				int amountToDisplay = pile.getAmount();

				// If we selected this item
				if ((InventoryView.selectedItem != null && InventoryView.selectedItem.getInventory() != null
						&& getInventory().equals(InventoryView.selectedItem.getInventory())
						&& InventoryView.selectedItem.getX() == i && InventoryView.selectedItem.getY() == j)) {
					amountToDisplay -= InventoryView.selectedItemAmount;
				}

				// Draw amount of items in the pile
				if (amountToDisplay > 1)
					renderer.getFontRenderer().drawStringWithShadow(renderer.getFontRenderer().defaultFont(),
							x + cornerSize + ((pile.getItem().getDefinition().getSlotsWidth() - 1.0f) + i) * slotSize,
							y + cornerSize + j * slotSize, amountToDisplay + "", scale, scale,
							new Vector4f(1, 1, 1, 1));
			}
		}
	}

	public int slotsWidth(int slots, int scale) {
		return (8 + slots * 24) * scale;
	}

	public int slotsHeight(int slots, int scale, boolean summary, int blankLines) {
		return (8 + (slots + (summary ? 0 : 1) + blankLines) * 24) * scale;
	}

	public Inventory getInventory() {
		return inventory;
	}
}
