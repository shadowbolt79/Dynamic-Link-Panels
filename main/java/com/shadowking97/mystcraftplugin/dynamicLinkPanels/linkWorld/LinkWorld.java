package com.shadowking97.mystcraftplugin.dynamicLinkPanels.linkWorld;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Set;

import com.shadowking97.mystcraftplugin.dynamicLinkPanels.DynamicLinkPanels;
import com.shadowking97.mystcraftplugin.dynamicLinkPanels.Util;
import com.shadowking97.mystcraftplugin.dynamicLinkPanels.entities.EntityCamera;
import com.shadowking97.mystcraftplugin.dynamicLinkPanels.network.ServerPacketDispatcher;
import com.shadowking97.mystcraftplugin.dynamicLinkPanels.network.packets.RequestPacket;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MovingSoundMinecart;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.particle.EntityFireworkStarterFX;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.profiler.Profiler;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.IntHashMap;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldSettings.GameType;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.SaveHandlerMP;

public class LinkWorld extends WorldClient {

	public EntityCamera lCamera;
	static Minecraft mc = Minecraft.getMinecraft();
	boolean cameraPlaced=false;
	boolean defaultSpawn=false;
	
	public short last_tick = 0;
	private int loadPause = 0;
	
	private long worldTime = 0;

    /** The packets that need to be sent to the server. */
    private NetHandlerPlayClient sendQueue;
    /** The ChunkProviderClient instance */
    private ChunkProviderClient clientChunkProvider;
    /**
     * The hash set of entities handled by this client. Uses the entity's ID as the hash set's key.
     */
    private IntHashMap entityHashSet = new IntHashMap();
    /** Contains all entities for this client, both spawned and non-spawned. */
    private Set entityList = new HashSet();
    
    /**
     * Contains all entities for this client that were not spawned due to a non-present chunk. The game will attempt to
     * spawn up to 10 pending entities with each subsequent tick until the spawn queue is empty.
     */
    private Set entitySpawnQueue = new HashSet();
	
	
	public LinkWorld(int dimensionID, ChunkCoordinates coords, float yaw) {
		super(Minecraft.getMinecraft().getNetHandler(), new WorldSettings(3242125, GameType.SURVIVAL, true, false, WorldType.DEFAULT), dimensionID, mc.gameSettings.difficulty, mc.theWorld.theProfiler);
		DynamicLinkPanels.debug("LinkWorld()");
		lCamera = new EntityCamera(this);
		if(coords==null){
			defaultSpawn = true;
			coords = this.getSpawnPoint();
		}
		if(this.clientChunkProvider==null){
			this.createChunkProvider();
			super.createChunkProvider();
		}
		spawnEntityInWorld(lCamera);
		lCamera.setPositionAndUpdate(coords.posX+0.5, coords.posY+0.5, coords.posZ+0.5);
		setActivePlayerChunksAndCheckLight();
	}
	
	public void receivedChunk(int x, int z)
	{
		DynamicLinkPanels.debug("LinkWorld.receivedChunk()");
		markBlockRangeForRenderUpdate(x << 4, 0, z << 4, (x << 4) + 15, 256, (z << 4) + 15);
		setActivePlayerChunksAndCheckLight();
		Chunk c = this.getChunkFromChunkCoords(x, z);
		if(c==null||c.isEmpty())
			return;
		int x_ = MathHelper.floor_double(lCamera.posX)>>4;
		int z_ = MathHelper.floor_double(lCamera.posZ)>>4;
		if(x==x_&&z==z_)
			checkLCameraY();
		else if(x_>=x-1&&x_<=x+1&&z_>z-1&&z_<z+1)
			lCamera.checkFlightPath();
		for(int y = 0; y<16; y++)
		{
			if(!c.getAreLevelsEmpty(y<<4, (y<<4)+15))
			{
				for(int x2 = 0; x2 < 16; x2++)
				{
					for(int z2 = 0; z2 <16; z2++)
					{
						for(int y2 = 0; y2<16; y2++){
							if(this.getBlock((x<<4)+x2, (y<<4)+y2, (z<<4)+z2).hasTileEntity(this.getBlockMetadata((x<<4)+x2, (y<<4)+y2, (z<<4)+z2)))
							{
								DynamicLinkPanels.packetPipeline.sendToServer(new RequestPacket(RequestPacket.tileEntity, (x<<4)+x2, (y<<4)+y2, (z<<4)+z2, provider.dimensionId));
							}
						}
					}
				}
			}
		}
	}
	
	public void checkLCameraY()
	{
		DynamicLinkPanels.debug("LinkWorld.checkLCameraY()");
		if(cameraPlaced)
			return;
		FMLLog.info("checkLCameraY");
		int y = MathHelper.floor_double(lCamera.posY);
		int yBackup = MathHelper.floor_double(lCamera.posY);
		int x = MathHelper.floor_double(lCamera.posX);
		int z = MathHelper.floor_double(lCamera.posZ);
		if(!this.getChunkFromBlockCoords(x, z).isEmpty()){
			if(this.isAirBlock(x, y, z))
			{
				while(isAirBlock(x,--y,z)&&y>0);
				if(y==0)
					y=yBackup;
				else
					y+=2;
			}
			else{
				while(!isAirBlock(x,++y,z)&&y<256);
				if(y==256)
					y=yBackup;
				else
					y++;
			}
			lCamera.checkFlightPath(x, y, z);

			cameraPlaced=true;
		}
	}
	
	public void setCameraToSpawn(ChunkCoordinates cc)
	{
		DynamicLinkPanels.debug("LinkWorld.setCameraToSpawn()");
		if(defaultSpawn){
			FMLLog.info("Set camera to spawn");
			lCamera.setPositionAndUpdate(cc.posX+0.5, cc.posY+0.5, cc.posZ+0.5);
			lCamera.checkFlightPath();
			Chunk c = this.getChunkFromChunkCoords(cc.posX>>4, cc.posZ>>4);
			if(c.isChunkLoaded)
				checkLCameraY();
		}
	}
	public void renderTick()
	{
		DynamicLinkPanels.debug("LinkWorld.renderTick()");
		++last_tick;
	}
	@Override
	public void tick()
    {
		DynamicLinkPanels.debug("LinkWorld.tick()");
		this.updateWeather();
		
        this.func_82738_a(this.getTotalWorldTime() + 1L);

        if (this.getGameRules().getGameRuleBooleanValue("doDaylightCycle"))
        {
            this.setWorldTime2(this.getWorldTime() + 1L);
        }

        this.theProfiler.startSection("reEntryProcessing");

        for (int i = 0; i < 10 && !this.entitySpawnQueue.isEmpty(); ++i)
        {
            Entity entity = (Entity)this.entitySpawnQueue.iterator().next();
            this.entitySpawnQueue.remove(entity);

            if (!this.loadedEntityList.contains(entity))
            {
                this.spawnEntityInWorld(entity);
            }
        }

        this.theProfiler.endStartSection("connection");
        //this.sendQueue.onNetworkTick();
        this.theProfiler.endStartSection("chunkCache");
        this.clientChunkProvider.unloadQueuedChunks();
        this.theProfiler.endStartSection("blocks");
        this.func_147456_g();
        this.theProfiler.endSection();
    }
	
	/**
     * Sets the world time.
     */
    public void setWorldTime2(long par1)
    {
        this.worldTime = par1;
        super.setWorldTime(par1);
    }
    
    @Override
    public void setWorldTime(long par1)
    {
    	super.setWorldTime(this.worldTime);
    }
	
	@Override
	public long getWorldTime()
	{
		return this.worldTime;
	}

    /**
     * Links the super chunk provider with this class's chunk provider
     */
	@Override
    protected IChunkProvider createChunkProvider()
    {
        this.clientChunkProvider = (ChunkProviderClient) super.createChunkProvider();
        return this.clientChunkProvider;
    }
    
    /**
     * Called to place all entities as part of a world
     */
	@Override
    public boolean spawnEntityInWorld(Entity par1Entity)
    {
		super.spawnEntityInWorld(par1Entity);
        boolean flag = super.spawnEntityInWorld(par1Entity);
        this.entityList.add(par1Entity);

        if (!flag)
        {
            this.entitySpawnQueue.add(par1Entity);
        }
        else if (par1Entity instanceof EntityMinecart)
        {
        }

        return flag;
    }
	
	@Override
	public void doPreChunk(int par1, int par2, boolean par3)
    {
		super.doPreChunk(par1, par2, par3);
        if (par3)
        {
            this.clientChunkProvider.loadChunk(par1, par2);
        }
        else
        {
            this.clientChunkProvider.unloadChunk(par1, par2);
        }

        if (!par3)
        {
            this.markBlockRangeForRenderUpdate(par1 * 16, 0, par2 * 16, par1 * 16 + 15, 256, par2 * 16 + 15);
        }
    }

    /**
     * Schedule the entity for removal during the next tick. Marks the entity dead in anticipation.
     */
	@Override
    public void removeEntity(Entity par1Entity)
    {
        super.removeEntity(par1Entity);
        this.entityList.remove(par1Entity);
    }
	
	@Override
    public void sendQuittingDisconnectingPacket(){}
	
	@Override
    public void playSound(double par1, double par3, double par5, String par7Str, float par8, float par9, boolean par10){}
	
	@Override
	public void makeFireworks(double par1, double par3, double par5, double par7, double par9, double par11, NBTTagCompound par13NBTTagCompound)
    {
        DynamicLinkPanels.instance.linkEffectRenderer.addEffect(new EntityFireworkStarterFX(this, par1, par3, par5, par7, par9, par11, this.mc.effectRenderer, par13NBTTagCompound));
    }
}