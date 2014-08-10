package com.shadowking97.mystcraftplugin.dynamicLinkPanels;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.MathHelper;

import com.shadowking97.mystcraftplugin.dynamicLinkPanels.linkWorld.ChunkFinderManager;
import com.shadowking97.mystcraftplugin.dynamicLinkPanels.network.packets.RequestPacket;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class DLPTickEventHandler {
	private static DynamicLinkPanels dlp = DynamicLinkPanels.instance;
	
	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void onClientTick(TickEvent.ClientTickEvent event) {
	    if (event.phase == TickEvent.Phase.START) {
	    	if(dlp.linkWorld!=null&&Minecraft.getMinecraft().theWorld!=null)
			{
				
				{
					if(dlp.linkWorld.lastLightningBolt>0)
						--dlp.linkWorld.lastLightningBolt;
					dlp.linkWorld.tick();
				}
			}
	    }
	}

	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void onRenderTick(TickEvent.RenderTickEvent event){
		if(event.phase==TickEvent.Phase.END){
			
			if(dlp.linkWorld!=null&&dlp.linkWorld.last_tick>1)
			{
				dlp.packetPipeline.sendToServer(new RequestPacket(RequestPacket.closeLink, 0, 0, 0, dlp.linkWorld.provider.dimensionId));
				dlp.linkWorld=null;
				dlp.linkRenderGlobal.setWorldAndLoadRenderers(null);
				RenderManager.instance.set(Minecraft.getMinecraft().theWorld);
				dlp.linkEffectRenderer.clearEffects(null);
				Minecraft.getMinecraft().theWorld.provider.registerWorld(Minecraft.getMinecraft().theWorld);
			}
			long renderT = Minecraft.getMinecraft().getSystemTime();
			if(dlp.linkWorld!=null&&Minecraft.getMinecraft().theWorld!=null){
				dlp.linkWorld.getWorldVec3Pool().clear();
				dlp.linkRenderGlobal.updateClouds();
				dlp.linkWorld.lCamera.fly(0.1f*(renderT-dlp.last_time));
				dlp.linkWorld.doVoidFogParticles(MathHelper.floor_double(dlp.linkWorld.lCamera.posX), MathHelper.floor_double((dlp.linkWorld).lCamera.posY), MathHelper.floor_double((dlp.linkWorld).lCamera.posZ));
				dlp.linkEffectRenderer.updateEffects();
				dlp.linkWorld.renderTick();
				
				dlp.renderWorldToTexture(0.1f);
			}
			dlp.last_time = renderT;
		}
	}
	
	@SubscribeEvent
	public void onServerTick(TickEvent.ServerTickEvent event) {
		if(event.phase == TickEvent.Phase.END){
			ChunkFinderManager.instance.tick();
		}
	}
	
	
}
