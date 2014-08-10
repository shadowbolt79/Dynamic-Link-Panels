package com.shadowking97.mystcraftplugin.dynamicLinkPanels.network;

import com.shadowking97.mystcraftplugin.dynamicLinkPanels.DynamicLinkPanels;
import com.shadowking97.mystcraftplugin.dynamicLinkPanels.network.impl.LinkPacket;
import com.shadowking97.mystcraftplugin.dynamicLinkPanels.network.packets.ChunkInfoPacket;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.chunk.Chunk;

public class PacketHolder {
	EntityPlayer player;
	LinkPacket packet;
	
	Chunk c;
	boolean par1;
	int par3;
	int yCh;
	
	public PacketHolder(EntityPlayer p, LinkPacket packet2)
	{
		DynamicLinkPanels.debug("PacketHolder(EntityPlayer,LinkPacket)");
		player = p;
		packet = packet2;
	}
	
	public PacketHolder(EntityPlayer p, Chunk ch, boolean b, int dimension, int yBits)
	{

		DynamicLinkPanels.debug("PacketHolder(EntityPlayer, Chunk, boolean, int, int)");
		c = ch;
		par1 = b;
		par3 = dimension;
		yCh = yBits;
		player = p;
	}
	
	public boolean belongsToPlayer(EntityPlayer p)
	{
		return player == p;
	}
	
	public int sendPacket()
	{

		DynamicLinkPanels.debug("PacketHolder.sendPacket()");
		if(c!=null)
		{
			packet = new ChunkInfoPacket(c, par1, yCh, par3);
		}
		if(packet !=null)
		{
			DynamicLinkPanels.packetPipeline.sendTo(packet, (EntityPlayerMP) player);
			return packet.getSize();
		}
		return 0;
	}
}
