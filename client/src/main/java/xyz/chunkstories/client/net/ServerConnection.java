//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.client.net;

import java.io.File;

import xyz.chunkstories.api.GameContext;
import xyz.chunkstories.api.net.RemoteServer;
import xyz.chunkstories.net.Connection;

/** A connection from a client *to* a server. */
public abstract class ServerConnection extends Connection {
	private final ClientConnectionSequence connectionSequence;

	public ServerConnection(ClientConnectionSequence connectionSequence) {
		super(connectionSequence.getServerAddress(), connectionSequence.getServerPort());
		this.connectionSequence = connectionSequence;
	}

	public abstract RemoteServer getRemoteServer();

	//public abstract ConnectionStep obtainModFile(String modMd5Hash, File cached);

	public ClientConnectionSequence getConnectionSequence() {
		return connectionSequence;
	}
}