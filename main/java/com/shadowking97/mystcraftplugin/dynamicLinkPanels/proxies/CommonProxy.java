package com.shadowking97.mystcraftplugin.dynamicLinkPanels.proxies;

import net.minecraft.network.NetHandlerPlayServer;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.config.Property;

import com.shadowking97.mystcraftplugin.dynamicLinkPanels.DynamicLinkPanels;
import com.shadowking97.mystcraftplugin.dynamicLinkPanels.network.packets.ChunkInfoPacket;
import com.shadowking97.mystcraftplugin.dynamicLinkPanels.network.packets.LinkInfoPacket;
import com.shadowking97.mystcraftplugin.dynamicLinkPanels.network.packets.RequestPacket;
import com.shadowking97.mystcraftplugin.dynamicLinkPanels.network.packets.TileEntityNBTPacket;

import cpw.mods.fml.relauncher.Side;

public class CommonProxy {
	public void registerRenderers() { }
	public void startNetHandlers() {
	}
	public void registerNetHandlers(){
		DynamicLinkPanels.debug("CommonProxy.registerNetHandlers()");
		DynamicLinkPanels.packetPipeline.registerPacket(ChunkInfoPacket.class);
		DynamicLinkPanels.packetPipeline.registerPacket(LinkInfoPacket.class);
		DynamicLinkPanels.packetPipeline.registerPacket(RequestPacket.class);
		DynamicLinkPanels.packetPipeline.registerPacket(TileEntityNBTPacket.class);
	}
}
