package com.shadowking97.mystcraftplugin.dynamicLinkPanels.network.packets;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.INetHandler;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.Ticket;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.shadowking97.mystcraftplugin.dynamicLinkPanels.DynamicLinkPanels;
import com.shadowking97.mystcraftplugin.dynamicLinkPanels.linkWorld.ChunkFinder;
import com.shadowking97.mystcraftplugin.dynamicLinkPanels.linkWorld.ChunkFinderManager;
import com.shadowking97.mystcraftplugin.dynamicLinkPanels.linkWorld.ServerTicketHandler;
import com.shadowking97.mystcraftplugin.dynamicLinkPanels.linkWorld.ChunkFinder.ChunkData;
import com.shadowking97.mystcraftplugin.dynamicLinkPanels.network.ServerPacketDispatcher;
import com.shadowking97.mystcraftplugin.dynamicLinkPanels.network.impl.LinkPacket;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.relauncher.Side;

public class RequestPacket extends LinkPacket {
	
	public static final byte linkInfo = 0;
	public static final byte chunkRequest = 1;
	public static final byte keepLinkAlive = 2;
	public static final byte closeLink = 3;
	public static final byte tileEntity = 4;

	public byte requestType;
	public int xPos;
	public int yPos;
	public int zPos;
	public int dimension;
	public EntityPlayer entity;
	public byte renderDistance;
	
	public RequestPacket() {}

	public RequestPacket(byte type, int x, int y, int z, int d) {
		DynamicLinkPanels.debug("RequestPacket()");
		dimension = d;
		xPos = x;
		yPos = y;
		zPos = z;
		entity = null;
		requestType = type;
	}
	
	public RequestPacket(byte type, float x, float y, float z, int d) {
		DynamicLinkPanels.debug("RequestPacket.()");
		dimension = d;
		xPos = MathHelper.floor_double(x);
		yPos = MathHelper.floor_double(y);
		zPos = MathHelper.floor_double(z);
		requestType = type;
	}
	
	@Override
	public void encodeInto(ChannelHandlerContext ctx, ByteBuf out) {
		out.writeByte(requestType);
		out.writeInt(dimension);
		out.writeInt(xPos);
		out.writeInt(yPos);
		out.writeInt(zPos);
		out.writeByte(DynamicLinkPanels.instance.renderDistance);
	}

	@Override
	public void decodeInto(ChannelHandlerContext ctx, ByteBuf in) {
		requestType = in.readByte();
		dimension = in.readInt();
		xPos = in.readInt();
		yPos = in.readInt();
		zPos = in.readInt();
		renderDistance = in.readByte();
		if(FMLCommonHandler.instance().getEffectiveSide().isServer())
			entity = ((NetHandlerPlayServer)ctx.channel().attr(NetworkRegistry.NET_HANDLER).get()).playerEntity;
	}

	@Override
	public int getSize() {
		// TODO Auto-generated method stub
		return 18;
	}
	
	@Override
	public void handleClientSide(EntityPlayer player){
		
	}
	
	@Override
	public void handleServerSide(EntityPlayer player){
		DynamicLinkPanels.debug("RequestPacket.handleServerSide()");
		WorldServer server = MinecraftServer.getServer().worldServerForDimension(this.dimension);
		if(server==null)
			return;
		switch(this.requestType)
		{
		case RequestPacket.linkInfo:
			int x, y, z;
			if(this.yPos<0)
    		{
    			ChunkCoordinates c = server.getSpawnPoint();
    			x=c.posX>>4;
    			y=c.posY>>4;
    			z=c.posZ>>4;
    		}
			else{
				x=this.xPos;
				y=this.yPos;
				z=this.zPos;
			}
    		if(this.renderDistance>DynamicLinkPanels.instance.renderDistance)
    			this.renderDistance = DynamicLinkPanels.instance.renderDistance;
    		
    		Ticket t = ServerTicketHandler.instance.createTicket(this.entity, server);

    		t.setChunkListDepth(DynamicLinkPanels.instance.ticketDepth);
    		int x1 = x-this.renderDistance;
    		int x2 = x+this.renderDistance;
    		int z1 = z-this.renderDistance;
    		int z2 = z+this.renderDistance;
    		
    		for(;x1<=x2;x1++)
    		{
    			for(;z1<=z2;z1++)
    			{
    				int x3 = x1-this.xPos;
    				int z3 = z1-this.zPos;
    				if(x3*x3+z3*z3<=this.renderDistance)
    					ForgeChunkManager.forceChunk(t, new ChunkCoordIntPair(x1,z1));
    			}
    		}
    		
    		ChunkFinderManager.instance.addFinder(new ChunkFinder(new ChunkCoordinates(x,y,z), server.getChunkProvider(), this.renderDistance, this.dimension, this.entity));
    		DynamicLinkPanels.packetPipeline.sendTo(new LinkInfoPacket(this.dimension), (EntityPlayerMP) player);
    		return;
    	case RequestPacket.chunkRequest:
    		Chunk c = server.getChunkFromChunkCoords(this.xPos, this.zPos);
			if(!c.isChunkLoaded)
				c=server.getChunkProvider().loadChunk(this.xPos, this.zPos);
    		ServerPacketDispatcher.instance.addPacket(c, true, this.dimension, this.entity, 65535);
    		break;
    	case RequestPacket.keepLinkAlive:
    		break;
    	case RequestPacket.closeLink:
    		ServerTicketHandler.instance.releaseTicket(this.entity);
    		ServerPacketDispatcher.instance.removeAllPacketsOf(this.entity);
    		break;
    	case RequestPacket.tileEntity:
    		TileEntity tile = server.getTileEntity(this.xPos, this.yPos, this.zPos);
    		if(tile!=null)
    		{
    			NBTTagCompound tag = new NBTTagCompound();
    			tile.writeToNBT(tag);
    			ServerPacketDispatcher.instance.addPacket(new TileEntityNBTPacket(this.xPos, this.yPos, this.zPos, tag, this.dimension),this.entity);
    		}
    		break;
		}
	}
}
