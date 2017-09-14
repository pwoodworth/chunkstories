package io.xol.chunkstories.server.commands.debug;

import io.xol.chunkstories.api.plugin.commands.Command;
import io.xol.chunkstories.api.plugin.commands.CommandEmitter;
import io.xol.chunkstories.api.server.ServerInterface;
import io.xol.chunkstories.server.commands.ServerCommandBasic;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class MiscDebugCommands extends ServerCommandBasic {

	public MiscDebugCommands(ServerInterface serverConsole) {
		super(serverConsole);
		server.getPluginManager().registerCommand("gc").setHandler(this);
	}

	@Override
	public boolean handleCommand(CommandEmitter emitter, Command command, String[] arguments) {
		if (command.getName().equals("gc") && emitter.hasPermission("server.debug.gc"))
		{
			emitter.sendMessage("Performing gc...");
			System.gc();
			return true;
		}
		return false;
	}

}
