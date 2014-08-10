package com.shadowking97.mystcraftplugin.dynamicLinkPanels;

import java.io.File;

import org.apache.logging.log4j.Level;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.config.Property;

import com.shadowking97.mystcraftplugin.dynamicLinkPanels.entities.EntityCamera;
import com.shadowking97.mystcraftplugin.dynamicLinkPanels.linkWorld.ChunkFinderManager;
import com.shadowking97.mystcraftplugin.dynamicLinkPanels.linkWorld.LinkWorld;
import com.shadowking97.mystcraftplugin.dynamicLinkPanels.network.ServerPacketDispatcher;
import com.shadowking97.mystcraftplugin.dynamicLinkPanels.network.impl.LinkPacketPipeline;
import com.shadowking97.mystcraftplugin.dynamicLinkPanels.network.packets.ChunkInfoPacket;
import com.shadowking97.mystcraftplugin.dynamicLinkPanels.network.packets.LinkInfoPacket;
import com.shadowking97.mystcraftplugin.dynamicLinkPanels.network.packets.RequestPacket;
import com.shadowking97.mystcraftplugin.dynamicLinkPanels.network.packets.TileEntityNBTPacket;
import com.shadowking97.mystcraftplugin.dynamicLinkPanels.proxies.CommonProxy;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@Mod( modid = ModInfo.ID, name = ModInfo.NAME, version = ModInfo.VERSION, dependencies = "required-after:Mystcraft" )



public class DynamicLinkPanels {
	
	public static final boolean debug = true;
	
	//public static Minecraft mc;
	//public static final DLPNetworkWrapper networkWrapper = new DLPNetworkWrapper("MystcraftDLP");
	public static final LinkPacketPipeline packetPipeline = new LinkPacketPipeline();
	
	@SideOnly(Side.CLIENT)
	public LinkWorld linkWorld;

	@SideOnly(Side.CLIENT)
	public RenderGlobal linkRenderGlobal;

	@SideOnly(Side.CLIENT)
	public EffectRenderer linkEffectRenderer;

	@Mod.Instance(ModInfo.ID)
	public static DynamicLinkPanels instance;
	
	@SidedProxy( clientSide = ModInfo.PROXY_LOC+".ClientProxy", serverSide = ModInfo.PROXY_LOC+".CommonProxy" )
	public static CommonProxy proxy;
	
	//132/83 - Mystcraft panel size. x2
	public static int height = 166;

	public static int width = 264;
	public static byte renderDistance = 7;
	public static float FOV = 0f;
	
	public static int ticketDepth = 149;
	
	public long openTime = -1;
	
	public int shaderARB;
	public int vertexARB;
	public int fragmentARB;
	
	public int textureLoc;
	public int timeLoc;
	public int resLoc;
	public int damageLoc;
	public int colorScaleLoc;
	public int waveScaleLoc;
	public int linkColorLoc;
	
	public int frameBuffer;
	public int colorTexture;
	public int depthBuffer;
	
	public static int dataRate;
	
	public long last_time;
		
	public static final void debug(String s){
		if(debug)
			FMLLog.info("DLP debug: "+s);
	}
	
	@EventHandler
	public static void preInit( FMLPreInitializationEvent event ) {
		debug("Preinit");
		DLPConfig config = new DLPConfig(new File(event.getSuggestedConfigurationFile().getParentFile(),"mystcraft/dynamiclinkpanels.cfg"));
		Property w = config.get(config.CATAGORY_RENDER, "width", 264);
		w.comment = "The width of the panel render. Default is 264, recommened multiples of 132.";
		width = w.getInt(264);
		Property h = config.get(config.CATAGORY_RENDER, "height", 166);
		h.comment = "The height of the panel render. Default is 166, recommened multiples of 83.";
		height = h.getInt(166);
		Property r = config.get(config.CATAGORY_RENDER, "renderDistance", 7);
		r.comment = "Render distance in RADIUS. The default of 7 can cause over to 150 chunks to load. Has a maximum value of 15, and a minimum of 3.";
		renderDistance = (byte) (r.getInt(7) & 255);
		if(renderDistance>15)
			r.set(15);
		else if(renderDistance<3)
			r.set(3);
		int i = 0;
		for(int j = -renderDistance; j<=renderDistance;j++)
			for(int k = -renderDistance; k<renderDistance; k++)
				if(Util.withinDistance2D(0, 0, j, k, renderDistance))
					i++;
		ticketDepth = i;
		File chunkFile = new File(event.getSuggestedConfigurationFile().getParentFile(),"forgeChunkLoading.cfg");
		if(chunkFile.exists())
		{
			Configuration chunkLoading = new Configuration(chunkFile);
			ConfigCategory cat = chunkLoading.getCategory(ModInfo.ID);
			if(cat.containsKey("maximumChunksPerTicket"))
				cat.get("maximumChunksPerTicket").set(i);
			else
				cat.put("maximumChunksPerTicket", new Property("maximumChunksPerTicket",Integer.toString(i),Property.Type.INTEGER));
			if(chunkLoading.hasChanged()){
				FMLLog.log(Level.INFO, "Dynamic Link Panels: Interjected Self into Forge Chunk Loading.");
				chunkLoading.save();
			}
		}
		Property f = config.get(config.CATAGORY_RENDER, "FOV", 0.0f);
		f.comment = "The field of view for the render. Default: 0";
		FOV = (float) f.getDouble(0.0);
		Property d = config.get(config.CATAGORY_SERVER, "datarate", 2048);
		d.comment = "The number of bytes to send per tick before the server cuts off sending. Default: 2048";
		dataRate = f.getInt(2048);
		if(config.hasChanged())
			config.save();
		ServerPacketDispatcher.instance.start();
	}

	@EventHandler
	public static void init( FMLInitializationEvent event ) {
		debug("Init");
		packetPipeline.initialise();
		//add packets
		FMLCommonHandler.instance().bus().register(new DLPTickEventHandler());
		proxy.registerNetHandlers();
	}

	@EventHandler
	public static void postInit( FMLPostInitializationEvent event ) {
		debug("PostInit");
		proxy.registerRenderers();
		proxy.startNetHandlers();
		packetPipeline.postInitialise();
		EntityRegistry.registerModEntity(EntityCamera.class, "entityCamera", 0, instance, 350, 20, false);
	}

	@SideOnly(Side.CLIENT)
	public final void renderWorldToTexture(float renderTime)
	  {
		debug("renderWorldToTexture");
		Minecraft mc = Minecraft.getMinecraft();
	    if(mc.skipRenderWorld)
	    	return;
	    EntityLivingBase player = linkWorld.lCamera;
	    
	    GameSettings settings = mc.gameSettings;
	    EntityRenderer entityRenderer = mc.entityRenderer;
	    EntityLivingBase viewportBackup = mc.renderViewEntity;
	    int heightBackup = mc.displayHeight;
	    int widthBackup = mc.displayWidth;
	    WorldClient worldBackup = mc.theWorld;
	    RenderGlobal renderBackup = mc.renderGlobal;
	    EffectRenderer effectBackup = mc.effectRenderer;
	    
	    int thirdPersonBackup = settings.thirdPersonView;
	    boolean hideGuiBackup = settings.hideGUI;
	    int particleBackup = mc.gameSettings.particleSetting;
	    mc.gameSettings.particleSetting = 2;
	    float FOVbackup = mc.gameSettings.fovSetting;
	    int width = DynamicLinkPanels.instance.width;
	    int height = DynamicLinkPanels.instance.height;
	    

	    mc.renderViewEntity = player;
	    mc.displayHeight = height;
	    mc.displayWidth = width;
	    mc.gameSettings.fovSetting = FOV;
	    mc.theWorld = (WorldClient) linkWorld;
	    mc.renderGlobal = linkRenderGlobal;
	    mc.effectRenderer = linkEffectRenderer;
	    boolean anaglyphBackup = settings.anaglyph;
	    int renderDistanceBackup = settings.renderDistanceChunks;
	    RenderManager.instance.set(linkWorld);
	    
	    
	    settings.thirdPersonView = 0;
	    settings.hideGUI = true;
	    settings.anaglyph=false;
	    settings.renderDistanceChunks = renderDistance;

	    GL11.glViewport (0, 0, width, height);
	    GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
	    EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, DynamicLinkPanels.instance.frameBuffer);
	    GL11.glClearColor(1.0f, 0.0f, 0.0f, 0.5f);
	    GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
	    
	    entityRenderer.updateRenderer();
	    int i1 = mc.gameSettings.limitFramerate;
	    try{
		    if (mc.isFramerateLimitBelowMax()) {
		      entityRenderer.renderWorld(renderTime, (long)(1000000000 / i1));
		    } else {
		      entityRenderer.renderWorld(renderTime, 0L);
		    }
	    } catch(ArrayIndexOutOfBoundsException e){
	    }
	    GL11.glEnable(GL11.GL_TEXTURE_2D);
	    EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, 0);
	    GL11.glClearColor(0.f,1f,0f,0.5f);
	    GL11.glClear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);
	    
	    GL11.glBindTexture(GL11.GL_TEXTURE_2D, DynamicLinkPanels.instance.colorTexture);
	    GL11.glViewport(0, 0, widthBackup, heightBackup);
	    GL11.glLoadIdentity();
	    
	    settings.thirdPersonView = thirdPersonBackup;
	    settings.hideGUI = hideGuiBackup;
	    settings.anaglyph = anaglyphBackup;
	    settings.renderDistanceChunks = renderDistanceBackup;
	    
	    mc.displayHeight = heightBackup;
	    mc.displayWidth = widthBackup;
	    
	    mc.renderViewEntity = viewportBackup;
	    mc.gameSettings.particleSetting = particleBackup;
	    mc.gameSettings.fovSetting = FOVbackup;
	    mc.theWorld = worldBackup;
	    mc.renderGlobal = renderBackup;
	    mc.effectRenderer = effectBackup;
	    RenderManager.instance.set(worldBackup);	    

	    entityRenderer.updateRenderer();
	    	    
	  }
}