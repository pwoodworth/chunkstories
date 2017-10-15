package io.xol.chunkstories.server.commands.admin;

import io.xol.chunkstories.api.plugin.commands.Command;
import io.xol.chunkstories.api.plugin.commands.CommandEmitter;
import io.xol.chunkstories.api.server.DedicatedServerInterface;
import io.xol.chunkstories.api.server.ServerInterface;
import io.xol.chunkstories.server.commands.ServerCommandBasic;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/** A long-dead meme */
public class StopTheFuckingTree extends ServerCommandBasic{

	public StopTheFuckingTree(ServerInterface serverConsole) {
		super(serverConsole);
		server.getPluginManager().registerCommand("stop").setHandler(this);
	}

	@Override
	public boolean handleCommand(CommandEmitter emitter, Command command, String[] arguments) {
		if (command.getName().equals("stop") && emitter.hasPermission("server.stop"))
		{
			if(server instanceof DedicatedServerInterface) {
				emitter.sendMessage("Stopping server.");
				((DedicatedServerInterface) server).stop();
				return true;
			}
			else {
				emitter.sendMessage("This isn't a dedicated server.");
				return true;
			}
		}
		return false;
	}

}
