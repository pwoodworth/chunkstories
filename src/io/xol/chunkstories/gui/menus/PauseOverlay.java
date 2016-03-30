package io.xol.chunkstories.gui.menus;

import io.xol.chunkstories.api.gui.Overlay;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.gui.MainMenu;
import io.xol.chunkstories.gui.OverlayableScene;
import io.xol.chunkstories.input.KeyBinds;
import io.xol.engine.base.ObjectRenderer;
import io.xol.engine.base.XolioWindow;
import io.xol.engine.font.BitmapFont;
import io.xol.engine.font.FontRenderer2;
import io.xol.engine.gui.ClickableButton;
import io.xol.engine.gui.FocusableObjectsHandler;

public class PauseOverlay extends Overlay
{
	FocusableObjectsHandler guiHandler = new FocusableObjectsHandler();
	
	ClickableButton resumeButton = new ClickableButton(0, 0, 320, 32, "Resume", BitmapFont.SMALLFONTS, 1);
	ClickableButton optionsButton = new ClickableButton(0, 0, 320, 32, "Options", BitmapFont.SMALLFONTS, 1);
	ClickableButton exitButton = new ClickableButton(0, 0, 320, 32, "Quit to menu", BitmapFont.SMALLFONTS, 1);
	
	public PauseOverlay(OverlayableScene scene, Overlay parent)
	{
		super(scene, parent);
		guiHandler.add(resumeButton);
		guiHandler.add(optionsButton);
		guiHandler.add(exitButton);
	}

	@Override
	public void drawToScreen(int x, int y, int w, int h)
	{
		ObjectRenderer.renderColoredRect(XolioWindow.frameW / 2, XolioWindow.frameH / 2, XolioWindow.frameW, XolioWindow.frameH, 0, "000000", 0.5f);
		FontRenderer2.drawTextUsingSpecificFont(XolioWindow.frameW / 2 - FontRenderer2.getTextLengthUsingFont(48, "In-game menu", BitmapFont.SMALLFONTS) / 2, XolioWindow.frameH / 2 + 48 * 3, 0, 48, "In-game menu", BitmapFont.SMALLFONTS);

		resumeButton.setPos(XolioWindow.frameW/2, XolioWindow.frameH/2 + 48 * 2);
		optionsButton.setPos(XolioWindow.frameW/2, XolioWindow.frameH/2 + 48);
		exitButton.setPos(XolioWindow.frameW/2, XolioWindow.frameH/2 - 48);
		
		resumeButton.draw();
		optionsButton.draw();
		exitButton.draw();
		

		if(resumeButton.clicked())
			mainScene.changeOverlay(parent);
		
		if(optionsButton.clicked())
		{
			mainScene.changeOverlay(new OptionsOverlay(mainScene, this));
		}
		
		if(exitButton.clicked())
		{
			if(Client.world != null)
				Client.world.clear();
			mainScene.eng.changeScene(new MainMenu(mainScene.eng, false));
		}
	}
	
	@Override
	public boolean handleKeypress(int k)
	{
		if (KeyBinds.getKeyBind("exit").isPressed())
		{
			mainScene.changeOverlay(parent);
			return true;
		}
		guiHandler.handleInput(k);
		return true;
	}

	@Override
	public boolean onClick(int posx, int posy, int button)
	{
		if (button == 0)
			guiHandler.handleClick(posx, posy);
		return true;
	}
}
