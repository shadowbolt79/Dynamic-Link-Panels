package com.shadowking97.mystcraftplugin.dynamicLinkPanels.linkWorld;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.shadowking97.mystcraftplugin.dynamicLinkPanels.DynamicLinkPanels;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldManager;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.Ticket;

public class ServerTicketHandler {
	public static ServerTicketHandler instance = new ServerTicketHandler();
	
	Set<Ticket> mapTickets;
	
	public ServerTicketHandler()
	{
		DynamicLinkPanels.debug("ServerTicketHandler()");
		mapTickets = new HashSet<Ticket>();
	}
	
	public Ticket createTicket(EntityPlayer p, World w)
	{
		DynamicLinkPanels.debug("ServerTicketHandler.createTicket()");
		if(p==null||w==null)
			return null;
		Ticket t = ForgeChunkManager.requestPlayerTicket(DynamicLinkPanels.instance, p.getDisplayName(), w, ForgeChunkManager.Type.NORMAL);
		mapTickets.add(t);
		return t;
	}
	
	public Ticket getPlayerTicket(EntityPlayer p)
	{
		DynamicLinkPanels.debug("ServerTicketHandler.getPlayerTicket()");
		if(p==null)
			return null;
		Iterator i = mapTickets.iterator();
		while(i.hasNext())
		{
			Ticket t = (Ticket) i.next();
			if(t.getPlayerName()==p.getDisplayName())
			{
				return t;
			}
		}
		return null;
	}
	
	public Ticket popTicket(EntityPlayer p)
	{
		DynamicLinkPanels.debug("ServerTicketHandler.popTicket()");
		if(p==null)
			return null;
		Ticket t = getPlayerTicket(p);
		if(t!=null)
			mapTickets.remove(t);
		return t;
	}
	
	public void releaseTicket(EntityPlayer p)
	{
		DynamicLinkPanels.debug("ServerTicketHandler.releaseTicket()");
		if(p==null)
			return;
		Ticket t = getPlayerTicket(p);
		if(t!=null)
		{
			int d = t.world.provider.dimensionId;
			ForgeChunkManager.releaseTicket(t);
			mapTickets.remove(t);
		}
	}
}