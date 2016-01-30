package io.xol.chunkstories.gui.menus;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.FastConfig;
import io.xol.chunkstories.gui.GameplayScene;
import io.xol.chunkstories.gui.OverlayableScene;
import io.xol.engine.base.ObjectRenderer;
import io.xol.engine.base.XolioWindow;
import io.xol.engine.base.font.BitmapFont;
import io.xol.engine.base.font.FontRenderer2;
import io.xol.engine.gui.ClickableButton;
import io.xol.engine.gui.CorneredBoxDrawer;
import io.xol.engine.gui.Focusable;
import io.xol.engine.gui.FocusableObjectsHandler;
import io.xol.engine.shaders.ShadersLibrary;

public class OptionsOverlay extends MenuOverlay
{
	FocusableObjectsHandler guiHandler = new FocusableObjectsHandler();
	ClickableButton exitButton = new ClickableButton(0, 0, 300, 32, ("Back"), BitmapFont.SMALLFONTS, 1);

	List<ConfigTab> configTabs = new ArrayList<ConfigTab>();

	abstract class ConfigButton extends ClickableButton
	{
		Runnable run = null;
	
		public ConfigButton setApplyAction(Runnable run)
		{
			this.run = run;
			return this;
		}
		
		public void apply()
		{
			save();
			FastConfig.define();
			if(run != null)
				run.run();
		}
		
		String parameter;
		String value;

		public ConfigButton(String n)
		{
			super(0, 0, 320, 32, n, BitmapFont.SMALLFONTS, 1);
			this.parameter = n;
			this.value = Client.getConfig().getProp(parameter, value);
		}

		public void updateText()
		{
			this.text = parameter + " : " + value;
		}

		public abstract void onClick(int posx, int posy, int button);

		public void save()
		{
			Client.getConfig().setProp(parameter, value);
		}
	}

	class ConfigButtonToggle extends ConfigButton
	{

		public ConfigButtonToggle(String n)
		{
			super(n);
			value = Client.getConfig().getBooleanProp(n, false)+"";
		}

		@Override
		public void onClick(int posx, int posy, int button)
		{
			value = !Boolean.parseBoolean(value) + "";
		}

	}

	class ConfigButtonMultiChoice extends ConfigButton
	{
		String values[];
		int cuVal = 0;

		public ConfigButtonMultiChoice(String n, String values[])
		{
			super(n);
			this.values = values;
			for (int i = 0; i < values.length; i++)
			{
				if (values[i].equals(value))
					cuVal = i;
			}
		}

		@Override
		public void onClick(int posx, int posy, int button)
		{
			if (button == 0)
				cuVal++;
			else
				cuVal--;
			if (cuVal < 0)
				cuVal = values.length - 1;
			if (cuVal >= values.length)
				cuVal = 0;
			value = values[cuVal];
		}

	}

	class ConfigButtonKey extends ConfigButton
	{
		OptionsOverlay options;

		public ConfigButtonKey(String n, OptionsOverlay options)
		{
			super(n);
			this.options = options;
		}

		@Override
		public void updateText()
		{
			this.text = parameter + " : " + Keyboard.getKeyName(Integer.parseInt(value));
		}

		@Override
		public void onClick(int posx, int posy, int button)
		{
			options.mainScene.changeOverlay(new KeyBindSelectionOverlay(options.mainScene, options, this));
		}

		public void callBack(int key)
		{
			value = key + "";
			apply();
			//options.applyAndSave();
		}

	}

	class ConfigButtonScale extends ConfigButton
	{
		float min,max,steps;
		
		public ConfigButtonScale(String n, float min, float max, float steps)
		{
			super(n);
			this.min = min;
			this.max = max;
			this.steps = steps;
		}

		@Override
		public void updateText()
		{
			this.text = parameter + " : " + value;
		}

		@Override
		public void onClick(int posx, int posy, int button)
		{
			float relPos = posx - this.posx + 8;
			float scaled = (float) (160 + Math.min(160, Math.max(-160.0, relPos))) / 320.0f;
			scaled *= (max-min);
			scaled += min;
			scaled /= steps;
			scaled = (float) (Math.floor(scaled)) * steps;
					
			value = scaled+"";
			//options.mainScene.changeOverlay(new KeyBindSelectionOverlay(options.mainScene, options, this));
		}
		
		public int draw()
		{
			int textWidth = FontRenderer2.getTextLengthUsingFont(size * 16, text, font);
			if (width < 0)
			{
				width = textWidth;
			}
			int textDekal = -textWidth;
			CorneredBoxDrawer.drawCorneredBoxTiled(posx, posy, width, height, 8, "gui/scalableField", 32, 2);
			ObjectRenderer.renderTexturedRect(posx - 160 + 320 * (Float.parseFloat(value)-min)/(max-min), posy, 64, 64, 0, 0, 32, 32, 32, "gui/barCursor");
			FontRenderer2.drawTextUsingSpecificFont(textDekal + posx, posy - height / 2, 0, size * 32, text, font);
			return width * 2 * size - 12;
		}

	}
	
	class ConfigTab
	{
		String name;
		ConfigButton[] configButtons;

		public ConfigTab(String name, ConfigButton[] buttons)
		{
			this.name = name;
			this.configButtons = buttons;
		}
	}

	List<ClickableButton> tabsButtons = new ArrayList<ClickableButton>();
	int selectedConfigTab = 0;

	public OptionsOverlay(OverlayableScene scene, MenuOverlay parent)
	{
		super(scene, parent);
		guiHandler.add(exitButton);

		configTabs.add(new ConfigTab("Rendering", new ConfigButton[] { 
				new ConfigButtonMultiChoice("viewDistance",new String[] { "64", "96", "128", "144", "160", "192", "224", "256" }),
				new ConfigButtonToggle("doRealtimeReflections").setApplyAction(new Runnable(){
					public void run()
					{
						ShadersLibrary.getShaderProgram("shadows_apply").reload(FastConfig.getShaderConfig());
					}
				}),
				new ConfigButtonToggle("doShadows").setApplyAction(new Runnable(){
					public void run()
					{
						ShadersLibrary.getShaderProgram("shadows_apply").reload(FastConfig.getShaderConfig());
					}
				}),
				new ConfigButtonMultiChoice("shadowMapResolutions", new String[] { "512", "1024", "2048", "4096" }).setApplyAction(new Runnable(){
					public void run()
					{if (mainScene instanceof GameplayScene && shouldReload){
						GameplayScene gps = ((GameplayScene) mainScene);
						gps.worldRenderer.resizeShadowMaps();
					}}
				}),
				new ConfigButtonToggle("dynamicGrass").setApplyAction(new Runnable(){
					public void run()
					{
						ShadersLibrary.getShaderProgram("blocks_opaque").reload(FastConfig.getShaderConfig());
						ShadersLibrary.getShaderProgram("postprocess").reload(FastConfig.getShaderConfig());
						ShadersLibrary.getShaderProgram("shadows").reload(FastConfig.getShaderConfig());
					}
				}),
				new ConfigButtonToggle("hqTerrain").setApplyAction(new Runnable(){
					public void run()
					{
						ShadersLibrary.getShaderProgram("terrain").reload(FastConfig.getShaderConfig());
					}
				}),
				new ConfigButtonToggle("rainyMode").setApplyAction(new Runnable(){
					public void run()
					{
						if(Client.world != null)
							Client.world.setWeather(Client.getConfig().getBooleanProp("rainyMode", false));
					}
				}),
				new ConfigButtonToggle("perPixelFresnel").setApplyAction(new Runnable(){
					public void run()
					{
						ShadersLibrary.getShaderProgram("blocks_opaque").reload(FastConfig.getShaderConfig());
						ShadersLibrary.getShaderProgram("blocks_liquid_pass1").reload(FastConfig.getShaderConfig());
						ShadersLibrary.getShaderProgram("blocks_liquid_pass2").reload(FastConfig.getShaderConfig());
						ShadersLibrary.getShaderProgram("entities").reload(FastConfig.getShaderConfig());
					}
				}),
				new ConfigButtonToggle("doClouds").setApplyAction(new Runnable(){
					public void run()
					{
						ShadersLibrary.reloadAllShaders();
					}
				}),
				new ConfigButtonMultiChoice("ssaoQuality", new String[] { "0", "1", "2"}).setApplyAction(new Runnable(){
					public void run()
					{
						ShadersLibrary.getShaderProgram("blocks_opaque").reload(FastConfig.getShaderConfig());
						ShadersLibrary.getShaderProgram("shadows_apply").reload(FastConfig.getShaderConfig());
					}
				}),
				new ConfigButtonToggle("doBloom").setApplyAction(new Runnable(){
					public void run()
					{if (mainScene instanceof GameplayScene && shouldReload){
						GameplayScene gps = ((GameplayScene) mainScene);
						gps.worldRenderer.setupRenderSize(XolioWindow.frameW, XolioWindow.frameH);
					}
					ShadersLibrary.getShaderProgram("postprocess").reload(FastConfig.getShaderConfig()); }
				}),
				new ConfigButtonMultiChoice("framerate",new String[] { "30", "60", "120", "-1" }).setApplyAction(new Runnable(){
					public void run()
					{
						XolioWindow.setTargetFPS(Client.getConfig().getIntProp("framerate", -1));
					}
				}),
				}));

		configTabs.add(new ConfigTab("Video", new ConfigButton[] {
				new ConfigButtonScale("fov", 25f, 85f, 1f),
				new ConfigButtonToggle("fullScreen"),
				new ConfigButtonMultiChoice("fullScreenResolution", XolioWindow.getDisplayModes()),
				}));

		configTabs.add(new ConfigTab("Controls", new ConfigButton[] {
				new ConfigButtonScale("mouseSensitivity", 0.5f, 2f, 0.05f),
				new ConfigButtonKey("FORWARD_KEY", this),
				new ConfigButtonKey("BACK_KEY", this),
				new ConfigButtonKey("LEFT_KEY", this),
				new ConfigButtonKey("RIGHT_KEY", this),
				new ConfigButtonKey("JUMP_KEY", this),
				new ConfigButtonKey("RUN_KEY", this),
				new ConfigButtonKey("ENTER_KEY", this),
				new ConfigButtonKey("EXIT_KEY", this),
				new ConfigButtonKey("INVENTORY_KEY", this),
				new ConfigButtonKey("GRABUSE_KEY", this),
				new ConfigButtonKey("CHAT_KEY", this),
				}));
		configTabs.add(new ConfigTab("Sound", new ConfigButton[] {}));
		configTabs.add(new ConfigTab("Debug", new ConfigButton[] { 
				new ConfigButtonToggle("debugGBuffers").setApplyAction(new Runnable(){
					public void run()
					{
						ShadersLibrary.getShaderProgram("postprocess").reload(FastConfig.getShaderConfig());
					}
				}),
				new ConfigButtonToggle("physicsVisualization"),
				new ConfigButtonToggle("showDebugInfo"),
				new ConfigButtonToggle("frametimeGraph"),
				}));

		for (ConfigTab tab : configTabs)
		{
			for (Focusable f : tab.configButtons)
				guiHandler.add(f);
			String txt = tab.name;
			int txtlen = FontRenderer2.getTextLengthUsingFont(32, txt, BitmapFont.SMALLFONTS);
			ClickableButton tabButton = new ClickableButton(0, 0, txtlen + 32, 32, txt, BitmapFont.SMALLFONTS, 1);
			tabsButtons.add(tabButton);
			guiHandler.add(tabButton);
		}
	}

	public void drawToScreen(int x, int y, int w, int h)
	{
		int optionsPanelSize = 320 * 2 + 32 + 64;

		ObjectRenderer.renderColoredRect(XolioWindow.frameW / 2, XolioWindow.frameH / 2, XolioWindow.frameW, XolioWindow.frameH, 0, "000000", 0.5f);
		ObjectRenderer.renderColoredRect(XolioWindow.frameW / 2, XolioWindow.frameH / 2, optionsPanelSize, XolioWindow.frameH, 0, "000000", 0.25f);

		int dekal = 0;
		int i = 0;
		for (ClickableButton b : tabsButtons)
		{
			dekal += b.getWidth() + 32 + 16;
			b.setPos(XolioWindow.frameW / 2 - optionsPanelSize / 2 + dekal, XolioWindow.frameH - 128);
			b.draw();
			dekal += b.getWidth();
			if (b.clicked())
				selectedConfigTab = i;
			i++;
		}

		ConfigTab currentConfigTab = configTabs.get(selectedConfigTab);
		int a = 0, b = 0;
		int startPosX = XolioWindow.frameW / 2 - optionsPanelSize / 2 + 160 + 32;
		int startPosY = XolioWindow.frameH - 128 - 64;
		for (ConfigButton c : currentConfigTab.configButtons)
		{
			c.setPos(startPosX + b * (320 + 32), startPosY - (float) Math.floor(a / 2) * 64);
			c.updateText();
			c.draw();
			if(c instanceof ConfigButtonScale && c.isMouseOver() && Mouse.isButtonDown(0))
			{
				ConfigButtonScale cs = (ConfigButtonScale)c;
				cs.onClick(Mouse.getX(), Mouse.getY(), 0);
				cs.apply();
				//applyAndSave();
			}
			
			a++;
			b = a % 2;
		}

		FontRenderer2.drawTextUsingSpecificFont(XolioWindow.frameW / 2 - optionsPanelSize / 2 + 32, XolioWindow.frameH - 48 * 2, 0, 48, "Options menu", BitmapFont.SMALLFONTS);

		exitButton.setPos(XolioWindow.frameW / 2, 48);
		exitButton.draw();

		if(currentConfigTab.name.equals("Rendering") || currentConfigTab.name.equals("") || currentConfigTab.name.equals("Debug"))
			shouldReload = true;
		
		if (exitButton.clicked())
		{
			//applyAndSave();
			Client.getConfig().save();
			mainScene.changeOverlay(parent);
		}
	}

	public boolean handleKeypress(int k)
	{
		if (k == FastConfig.EXIT_KEY)
		{
			Client.getConfig().save();
			mainScene.changeOverlay(parent);
			return true;
		}
		guiHandler.handleInput(k);
		return true;
	}

	public boolean onClick(int posx, int posy, int button)
	{
		if (button == 0)
			guiHandler.handleClick(posx, posy);
		if (guiHandler.getFocusedObject() != null && guiHandler.getFocusedObject() instanceof ConfigButton)
		{
			((ConfigButton) guiHandler.getFocusedObject()).onClick(posx, posy, button);
		}
		for (ConfigButton b : configTabs.get(selectedConfigTab).configButtons)
		{
			if (b.isMouseOver())
			{
				b.onClick(posx, posy, button);
				b.apply();
				//applyAndSave();
			}
		}
		return true;
	}
	
	boolean shouldReload = false;

	/*private void applyAndSave()
	{
		boolean reloadChunks = false;
		// boolean interleavedBefore = FastConfig.interleavedRendering;
		for (ConfigTab tab : this.configTabs)
		{
			for (ConfigButton b : tab.configButtons)
				b.save();
		}
		Client.getConfig().save();
		FastConfig.define();
		// if(FastConfig.interleavedRendering != interleavedBefore)
		// reloadChunks = true;
		XolioWindow.setTargetFPS(Client.getConfig().getIntProp("framerate", -1));
		if (this.mainScene instanceof GameplayScene && shouldReload)
		{
			GameplayScene gps = ((GameplayScene) mainScene);
			//gps.worldRenderer.reloadShaders();
			ShadersLibrary.reloadAllShaders();
			gps.worldRenderer.resizeShadowMaps();
			if (reloadChunks)
			{
				Client.world.reRender();
				gps.worldRenderer.modified();
			}
		}
		if(Client.world != null)
			Client.world.setWeather(Client.getConfig().getBooleanProp("rainyMode", false));
		XolioWindow.switchResolution();
		//XolioWindow.setDisplayMode(Client.getConfig().getBooleanProp("fullscreen", false), Client.getConfig().getProp(fullScreenResolution));
		shouldReload = false;
	}*/
}
