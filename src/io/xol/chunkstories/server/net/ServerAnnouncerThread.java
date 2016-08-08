package io.xol.chunkstories.server.net;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import io.xol.chunkstories.VersionInfo;
import io.xol.chunkstories.server.Server;
import io.xol.engine.net.HttpRequests;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ServerAnnouncerThread extends Thread
{

	AtomicBoolean run = new AtomicBoolean(true);

	int lolcode = 0;
	public long updatedelay = 0;

	public String srv_name;
	public String srv_desc;

	public void init()
	{
		lolcode = Server.getInstance().getServerConfig().getIntProp("lolcode", "0");
		if (lolcode == 0L)
		{
			// System.out.println("lolcode = 0");
			Random rnd = new Random();
			lolcode = rnd.nextInt(100000);
			Server.getInstance().getServerConfig().setInteger("lolcode", lolcode);
		}
		updatedelay = Long.parseLong(Server.getInstance().getServerConfig().getProp("update-delay", "10000"));
		String hostname = HttpRequests.sendPost("http://chunkstories.xyz/api/sayMyName.php?host=1", "");
		srv_name = Server.getInstance().getServerConfig().getProp("server-name", "unnamedserver@" + hostname);
		srv_desc = Server.getInstance().getServerConfig().getProp("server-desc", "Default description.");
		setName("Multiverse thread");
	}

	public void stopAnnouncer()
	{
		run.set(false);
	}

	@Override
	public void run()
	{
		try
		{
			String ip = HttpRequests.sendPost("http://chunkstories.xyz/api/sayMyName.php?ip=1", "");
			while (run.get())
			{
				// System.out.println("Updating server data on Multiverse.");
				if (Server.getInstance().getServerConfig().getProp("enable-multiverse", "false").equals("true"))
				{
					HttpRequests.sendPost("http://chunkstories.xyz/api/serverAnnounce.php", "srvname=" + srv_name + "&desc=" + srv_desc + "&ip=" + ip + "&mu=" + Server.getInstance().getHandler().getMaxClients() + "&u="
							+ Server.getInstance().getHandler().getNumberOfConnectedClients() + "&n=0&w=default&p=1&v=" + VersionInfo.version + "&lolcode=" + lolcode);
					sleep(updatedelay);
				}
				else
					sleep(6000);
			}
		}
		catch (Exception e)
		{
			Server.getInstance().getLogger().error("An unexpected error happened during multiverse stuff. More info below.");
			e.printStackTrace();
		}
	}
}
