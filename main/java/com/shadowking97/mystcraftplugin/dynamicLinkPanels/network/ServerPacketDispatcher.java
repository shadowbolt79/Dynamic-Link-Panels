package com.shadowking97.mystcraftplugin.dynamicLinkPanels.network;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.shadowking97.mystcraftplugin.dynamicLinkPanels.DynamicLinkPanels;
import com.shadowking97.mystcraftplugin.dynamicLinkPanels.network.impl.LinkPacket;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.chunk.Chunk;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ServerPacketDispatcher extends Thread {
	
	public static ServerPacketDispatcher instance = new ServerPacketDispatcher();

	private List<PacketHolder> packets;
	
	public int bytes;
	
	private boolean isRunning = true;
	
	public ServerPacketDispatcher()
	{
		bytes = 0;
		packets = new LinkedList<PacketHolder>();
	}
	public void addPacket(LinkPacket packet, EntityPlayer player)
	{
		DynamicLinkPanels.debug("ServerPacketDipatcher.addPacket(entity)");
		synchronized(this){
			packets.add(new PacketHolder(player, packet));
			this.notify();
		}
	}
	
	public void addPacket(Chunk c, boolean b, int dim, EntityPlayer player, int yBits)
	{DynamicLinkPanels.debug("ServerPacketDipatcher.addPacket(chunk)");
		synchronized(this){
			packets.add(new PacketHolder(player, c, b, dim, yBits));
			this.notify();
		}
	}
	
	public void removeAllPacketsOf(EntityPlayer player)
	{
		DynamicLinkPanels.debug("ServerPacketDipatcher.removeAllPacketsOf()");
		synchronized(this){
			for(int j = 0; j<packets.size(); j++)
			{
				if(packets.get(j).belongsToPlayer(player)){
					packets.remove(j);
					--j;
				}
			}
			this.notify();
		}
	}
	
	public void tick()
	{
		DynamicLinkPanels.debug("ServerPacketDipatcher.tick()");
		int j = packets.size();
		int byteLimit = DynamicLinkPanels.instance.dataRate;
		for(int bytes = 0; bytes<byteLimit&&!packets.isEmpty(); )
		{
			PacketHolder p = packets.get(0);
			bytes+=p.sendPacket();
			packets.remove(0);
		}
	}
	@Override
	public void run() {
		DynamicLinkPanels.debug("ServerPacketDipatcher.run()");
		while(isRunning){
			if(packets.size()>0){
				try {
					synchronized(this){
						tick();
						this.wait(20);
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else{
				try {
					synchronized(this){
						this.wait();
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}

