package io.xol.chunkstories.server.net;

import io.xol.chunkstories.VersionInfo;
import io.xol.chunkstories.api.events.core.PlayerLoginEvent;
import io.xol.chunkstories.api.events.core.PlayerLogoutEvent;
import io.xol.chunkstories.net.SendQueue;
import io.xol.chunkstories.net.packets.IllegalPacketException;
import io.xol.chunkstories.net.packets.Packet;
import io.xol.chunkstories.net.packets.PacketText;
import io.xol.chunkstories.net.packets.PacketsProcessor;
import io.xol.chunkstories.net.packets.UnknowPacketException;
import io.xol.chunkstories.server.Server;
import io.xol.chunkstories.server.ServerPlayer;
import io.xol.chunkstories.server.tech.UsersPrivileges;
import io.xol.chunkstories.tools.ChunkStoriesLogger;
import io.xol.engine.net.HttpRequestThread;
import io.xol.engine.net.HttpRequester;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ServerClient extends Thread implements HttpRequester
{
	int id = 0;

	//Streams.
	Socket sock;
	PacketsProcessor packetsProcessor;
	DataInputStream in = null;
	SendQueue queue;

	boolean validToken = false;
	String token = "undefined";
	
	//Has the user provided a valid login ?
	//public boolean authentificated = false;
	
	//Did the connection died at some point ?
	boolean died = false;
	//Used to pevent calling close() twice
	boolean alreadyKilled = false;

	public String name = "undefined";
	public String version = "undefined";
	//Assert : if the player is authentificated it has a profile
	public ServerPlayer profile;

	ServerClient(Socket s)
	{
		packetsProcessor = new PacketsProcessor(this);
		sock = s;
		id = s.getPort();
		this.setName("Client thread " + id);
	}

	// Here's the usefull things !

	public void handleLogin(String m)
	{
		if (m.startsWith("username:"))
		{
			this.name = m.replace("username:", "");
		}
		if (m.startsWith("logintoken:"))
		{
			token = m.replace("logintoken:", "");
		}
		if (m.startsWith("version:"))
		{
			version = m.replace("version:", "");
			if (Server.getInstance().serverConfig.getProp("check-version", "true").equals("true"))
			{
				if (!version.equals(VersionInfo.version))
					Server.getInstance().handler.disconnectClient(this, "Wrong version ! " + version + " != " + VersionInfo.version);
			}
		}
		if (m.startsWith("confirm"))
		{
			if (name.equals("undefined"))
				return;
			if (UsersPrivileges.isUserBanned(name))
			{
				Server.getInstance().handler.disconnectClient(this, "Banned username - " + name);
				return;
			}
			if (token.length() != 20)
			{
				Server.getInstance().handler.disconnectClient(this, "No valid token supplied");
				return;
			}
			if (Server.getInstance().serverConfig.getIntProp("offline-mode", "0") == 1)
			{
				// Offline-mode !
				System.out.println("Warning : Offline-mode is on, letting " + this.name + " connecting without verification");
				//authentificated = true;
				profile = new ServerPlayer(this);
				Server.getInstance().handler.sendAllChat("#FFD000" + name + " (" + getIp() + ")" + " joined.");
				send("login/ok");
			}
			else
				new HttpRequestThread(this, "checktoken", "http://chunkstories.xyz/api/serverTokenChecker.php", "username=" + this.name + "&token=" + token).start();
		}
	}

	// Just socket bullshit !
	@Override
	public void run()
	{
		// Server.getInstance().log.info("Client " + id +
		// " handling thread started properly.");
		while (!died)
		{
			try
			{
				//byte type = in.readByte();
				//handlePacket(type, in);
				
				Packet packet = packetsProcessor.getPacket(in, false, false);
				packet.read(in);
				packet.process(packetsProcessor);
				
				/*if (type == 0x00)
					Server.getInstance().handler.handle(this, in.readUTF());
				else
					handleBinary(type, in);*/
			}
			
			/*catch (IOException e)
			{
				died = true;
				System.out.println("Socket " + id + " (" + getIp() + ") died (" + e.getClass().getName() + ")");
			}*/
			catch (IllegalPacketException | UnknowPacketException e)
			{
				ChunkStoriesLogger.getInstance().info("Disconnected "+this+" for causing an "+e.getClass().getSimpleName());
				Server.getInstance().handler.disconnectClient(this, e.getMessage());
			}
			catch(Exception e)
			{
				Server.getInstance().handler.disconnectClient(this, e.getMessage());
			}
		}
		Server.getInstance().handler.disconnectClient(this);
	}

	public String getIp()
	{
		return sock.getInetAddress().getHostAddress();
	}

	public String getHost()
	{
		return sock.getInetAddress().getHostName();
	}

	public void open()
	{
		try
		{
			in = new DataInputStream(new BufferedInputStream(sock.getInputStream()));
			queue = new SendQueue(new DataOutputStream(new BufferedOutputStream(sock.getOutputStream())), packetsProcessor);
			queue.start();
			//out = );
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void close()
	{
		if (alreadyKilled)
			return;
		died = true;
		if (profile != null)
		{
			//authentificated = true;
			//profile = new ServerPlayer(this);
			PlayerLogoutEvent playerDisconnectionEvent = new PlayerLogoutEvent(profile);
			Server.getInstance().getPluginsManager().fireEvent(playerDisconnectionEvent);

			Server.getInstance().handler.sendAllChat(playerDisconnectionEvent.getLogoutMessage());

			//Server.getInstance().handler.sendAllChat("#FFD000" + name + " (" + getIp() + ") left.");
			assert profile != null;
			profile.destroy();
			profile.save();
		}

		try
		{
			if (in != null)
				in.close();
			queue.kill();
		}
		catch (Exception e)
		{
		}
		alreadyKilled = true;
	}

	public void sendChat(String msg)
	{
		send("chat/" + msg);
	}

	public void send(String msg)
	{
		// Text flag
		PacketText packet = new PacketText(false);
		packet.text = msg;
		sendPacket(packet);
	}

	public void sendPacket(Packet packet)
	{
		queue.queue(packet);
	}

	@Override
	public void handleHttpRequest(String info, String result)
	{
		if (info.equals("checktoken"))
		{
			if (result.equals("ok"))
			{
				//authentificated = true;
				profile = new ServerPlayer(this);
				PlayerLoginEvent playerConnectionEvent = new PlayerLoginEvent(profile);
				Server.getInstance().getPluginsManager().fireEvent(playerConnectionEvent);
				boolean allowPlayerIn = !playerConnectionEvent.isCancelled();
				if (!allowPlayerIn)
				{
					Server.getInstance().handler.disconnectClient(this, playerConnectionEvent.getRefusedConnectionMessage());
					return;
				}
				//System.out.println(allowPlayerIn+"allow");
				Server.getInstance().handler.sendAllChat(playerConnectionEvent.getConnectionMessage());
				//Server.getInstance().handler.sendAllChat("#FFD000" + name + " (" + getIp() + ")" + " joined.");
				//profile.onJoin();
				send("login/ok");
			}
			else
			{
				Server.getInstance().handler.disconnectClient(this, "Invalid session id !");
			}
		}
	}
	
	@Override
	public boolean equals(Object o)
	{
		if(o != null && o instanceof ServerClient)
		{
			ServerClient c = (ServerClient)o;
			if(c.name.equals(name) && id == c.id)
				return true;
		}
			return false;
	}

	public boolean isAuthentificated()
	{
		return profile != null;
	}
}
