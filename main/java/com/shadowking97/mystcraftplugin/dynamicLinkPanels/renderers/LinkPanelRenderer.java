package com.shadowking97.mystcraftplugin.dynamicLinkPanels.renderers;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Random;

import org.lwjgl.opengl.ARBFragmentShader;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

import com.shadowking97.mystcraftplugin.dynamicLinkPanels.DynamicLinkPanels;
import com.shadowking97.mystcraftplugin.dynamicLinkPanels.linkWorld.LinkWorld;
import com.shadowking97.mystcraftplugin.dynamicLinkPanels.network.packets.RequestPacket;
import com.shadowking97.mystcraftplugin.dynamicLinkPanels.proxies.ClientProxy;
import com.xcompwiz.mystcraft.api.client.ILinkPanelEffect;
import com.xcompwiz.mystcraft.api.linking.ILinkInfo;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.DimensionManager;
@SideOnly(Side.CLIENT)
public class LinkPanelRenderer implements ILinkPanelEffect {
	  private DynamicLinkPanels dlp = DynamicLinkPanels.instance;
	  
	  private float bookDamage = 0;
	  private float colorScale = 0.5f;
	  private float waveScale = 0.5f;
	  
	  private float sinceOpened = 0;
	  
	  private float linkColorR;
	  private float linkColorG;
	  private float linkColorB;
	  
	  public int hash(String s)
	  {
		  int hash=7;
		  for (int i=0; i < s.length(); i++) {
		      hash = hash*31+s.charAt(i);
		  }
		  return hash;
	  }
	  
	@Override
	public void render(int left, int top, int width, int height,
			ILinkInfo linkInfo) {

		DynamicLinkPanels.debug("LinkPanelRenderer.render()");
		if(dlp.linkWorld==null)
		{
			//TODO Create world
			Minecraft mc = Minecraft.getMinecraft();
			ChunkCoordinates spawn = linkInfo.getSpawn();
			dlp.linkWorld=new LinkWorld(linkInfo.getDimensionUID(),spawn,linkInfo.getSpawnYaw());
			EntityLivingBase backup = mc.renderViewEntity;
			mc.renderViewEntity = (dlp.linkWorld).lCamera;
			dlp.linkRenderGlobal.setWorldAndLoadRenderers(dlp.linkWorld);
			dlp.linkEffectRenderer.clearEffects(dlp.linkWorld);
			mc.renderViewEntity = backup;
			colorScale = 0.5f;
			waveScale = 0.5f;
			RenderManager.instance.set(mc.theWorld);
			mc.theWorld.provider.registerWorld(mc.theWorld);
			dlp.linkWorld.provider.registerWorld(dlp.linkWorld);
			dlp.openTime=-1;
			
			Random r = new Random(hash(dlp.linkWorld.provider.getDimensionName()));
			linkColorR = r.nextFloat();
			linkColorG = r.nextFloat();
			linkColorB = r.nextFloat();
			
			int x =0;
			int y=-1;
			int z=0;
			
			if(spawn!=null)
			{
				x = spawn.posX>>4;
				y = spawn.posY>>4;
				z = spawn.posZ>>4;
			}
			DynamicLinkPanels.packetPipeline.sendToServer(new RequestPacket(RequestPacket.linkInfo, x,y,z, linkInfo.getDimensionUID()));
		}
		else
		{
			if(dlp.linkWorld.provider.dimensionId!=linkInfo.getDimensionUID())
			{
				DynamicLinkPanels.packetPipeline.sendToServer(new RequestPacket(RequestPacket.closeLink,0,0,0, linkInfo.getDimensionUID()));
				Minecraft mc = Minecraft.getMinecraft();
				
				ChunkCoordinates spawn = linkInfo.getSpawn();
				dlp.linkWorld=new LinkWorld(linkInfo.getDimensionUID(),spawn,linkInfo.getSpawnYaw());
				EntityLivingBase backup = mc.renderViewEntity;
				mc.renderViewEntity = (dlp.linkWorld).lCamera;
				dlp.linkRenderGlobal.setWorldAndLoadRenderers(dlp.linkWorld);
				dlp.linkEffectRenderer.clearEffects(dlp.linkWorld);
				mc.renderViewEntity = backup;
				RenderManager.instance.set(mc.theWorld);
				mc.theWorld.provider.registerWorld(mc.theWorld);
				dlp.linkWorld.provider.registerWorld(dlp.linkWorld);
				colorScale = 0.5f;
				waveScale = 0.5f;
				dlp.openTime=-1;
				
				Random r = new Random(hash(dlp.linkWorld.provider.getDimensionName()));
				linkColorR = r.nextFloat();
				linkColorG = r.nextFloat();
				linkColorB = r.nextFloat();

				int x =0;
				int y=-1;
				int z=0;
				
				if(spawn!=null)
				{
					x = spawn.posX>>4;
					y = spawn.posY>>4;
					z = spawn.posZ>>4;
				}
				DynamicLinkPanels.packetPipeline.sendToServer(new RequestPacket(RequestPacket.linkInfo, x,y,z, linkInfo.getDimensionUID()));
			}
			else if(Minecraft.getMinecraft().theWorld!=null)
			{
					if(dlp.openTime>=0)
						sinceOpened = (Minecraft.getSystemTime()-dlp.openTime)*0.0003f;
					else
						sinceOpened=0;
					waveScale+=(dlp.linkWorld.rand.nextDouble()-0.5d)/10;
					if(waveScale>1)
						waveScale=1;
					if(waveScale<0)
						waveScale=0;
					colorScale+=(dlp.linkWorld.rand.nextDouble()-0.5d)/10;
					if(colorScale>1)
						colorScale=1;
					if(colorScale<0)
						colorScale=0;
			        Tessellator tessellator = Tessellator.instance;
			        
			        dlp.linkWorld.last_tick=0;
			        TextureManager renderEngine = FMLClientHandler.instance().getClient().getTextureManager();
			        
			        if(OpenGlHelper.shadersSupported){
			        	ARBShaderObjects.glUseProgramObjectARB(dlp.shaderARB);

						dlp.textureLoc = ARBShaderObjects.glGetUniformLocationARB(dlp.shaderARB, "u_texture");
						ARBShaderObjects.glUniform1iARB(dlp.textureLoc, 14);
						GL13.glActiveTexture(GL13.GL_TEXTURE14);
				        GL11.glBindTexture(GL11.GL_TEXTURE_2D, dlp.colorTexture);
				        
				        dlp.damageLoc = ARBShaderObjects.glGetUniformLocationARB(dlp.shaderARB, "damage");
						dlp.resLoc = ARBShaderObjects.glGetUniformLocationARB(dlp.shaderARB, "iResolution");
						dlp.timeLoc = ARBShaderObjects.glGetUniformLocationARB(dlp.shaderARB, "iGlobalTime");
						dlp.waveScaleLoc = ARBShaderObjects.glGetUniformLocationARB(dlp.shaderARB, "dWave");
						dlp.colorScaleLoc = ARBShaderObjects.glGetUniformLocationARB(dlp.shaderARB, "dColor");
						dlp.linkColorLoc = ARBShaderObjects.glGetUniformLocationARB(dlp.shaderARB, "linkColor");
						
						ARBShaderObjects.glUniform1fARB(dlp.timeLoc, sinceOpened);
						ARBShaderObjects.glUniform2fARB(dlp.resLoc, dlp.width, dlp.height);
						ARBShaderObjects.glUniform1fARB(dlp.damageLoc, (bookDamage-0.5f)*2f);
						ARBShaderObjects.glUniform1fARB(dlp.waveScaleLoc,waveScale);
						ARBShaderObjects.glUniform1fARB(dlp.colorScaleLoc, colorScale);
						ARBShaderObjects.glUniform4fARB(dlp.linkColorLoc, linkColorR, linkColorG, linkColorB, 1f);
						
						GL13.glActiveTexture(GL13.GL_TEXTURE0);
			        }
			        GL13.glActiveTexture(GL13.GL_TEXTURE0);
			        GL11.glBindTexture(GL11.GL_TEXTURE_2D, dlp.colorTexture);
			        tessellator.setColorRGBA_F(0, 0, 0, 1);
			        tessellator.startDrawingQuads();
			        if(OpenGlHelper.shadersSupported){
			        	tessellator.addVertexWithUV(left, height + top, 0.0D, 0.0D, 1.0D);
			        	tessellator.addVertexWithUV(width + left, height + top, 0.0D, 1.0D, 1.0D);
			        	tessellator.addVertexWithUV(width + left, top, 0.0D, 1.0D, 0.0D);
			        	tessellator.addVertexWithUV(left, top, 0.0D, 0.0D, 0.0D);
			        }
			        else{
			        	tessellator.addVertexWithUV(left,top, 0.0D, 0.0D, 1.0D);
			        	tessellator.addVertexWithUV(width + left,top, 0.0D, 1.0D, 1.0D);
			        	tessellator.addVertexWithUV(width + left, height +  top, 0.0D, 1.0D, 0.0D);
			        	tessellator.addVertexWithUV(left, height +  top, 0.0D, 0.0D, 0.0D);
			        }
			        tessellator.draw();
			        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
			        if(OpenGlHelper.shadersSupported)
			        {
			        	ARBShaderObjects.glUseProgramObjectARB(0);

			        	GL13.glActiveTexture(GL13.GL_TEXTURE0);
			        	
			        }
			}
			else
			{
				Tessellator tessellator = Tessellator.instance;
		        
				tessellator.setColorRGBA_F(0, 0, 0, 1);
		        tessellator.startDrawingQuads();
		        tessellator.addVertexWithUV(left, height + top, 0.0D, 0.0D, 1.0D);
		        tessellator.addVertexWithUV(width + left, height + top, 0.0D, 1.0D, 1.0D);
		        tessellator.addVertexWithUV(width + left, top, 0.0D, 1.0D, 0.0D);
		        tessellator.addVertexWithUV(left, top, 0.0D, 0.0D, 0.0D);
		        tessellator.draw();
			}
		}
	}
	@Override
	public void onOpen() {
		DynamicLinkPanels.debug("LinkPanelRenderer.onOpen()");
		GuiScreen s = Minecraft.getMinecraft().currentScreen;
		
		if(s!=null)
		{
			if(s instanceof GuiContainer)
			{
				GuiContainer g = (GuiContainer)s;
				ItemStack s1 = g.inventorySlots.getSlot(0).getStack();
				if(s1!=null)
					bookDamage = ((float)s1.getItemDamageForDisplay())/s1.getMaxDamage();
				else
					bookDamage = 0;
			}
		}
	}
}