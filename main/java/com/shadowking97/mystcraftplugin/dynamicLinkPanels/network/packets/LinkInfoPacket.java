package com.shadowking97.mystcraftplugin.dynamicLinkPanels.network.packets;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.WorldServer;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.shadowking97.mystcraftplugin.dynamicLinkPanels.DynamicLinkPanels;
import com.shadowking97.mystcraftplugin.dynamicLinkPanels.linkWorld.LinkWorld;
import com.shadowking97.mystcraftplugin.dynamicLinkPanels.network.impl.LinkPacket;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.relauncher.Side;

public class LinkInfoPacket extends LinkPacket{

	public int dimension;
	public int posX;
	public int posY;
	public int posZ;
	public int skylightSubtracted;
	public float thunderingStrength;
	public float rainingStrength;
	public long worldTime;
	
	public LinkInfoPacket(){}
	
	public LinkInfoPacket(int dimension){
		DynamicLinkPanels.debug("LinkInfoPacket(int)");
		WorldServer server = MinecraftServer.getServer().worldServerForDimension(dimension);
		if(server==null){
			
			FMLLog.warning("World server for dimension %i is null!", dimension);
			this.dimension=0;
			this.posX=0;
			this.posY=0;
			this.posZ=0;
			this.skylightSubtracted=0;
			this.thunderingStrength=0;
			this.rainingStrength=0;
			this.worldTime=0;
			return;
		}
		this.dimension = dimension;
		ChunkCoordinates cc = server.provider.getSpawnPoint();
		posX = cc.posX;
		posY = cc.posY;
		posZ = cc.posZ;
		skylightSubtracted = server.skylightSubtracted;
		thunderingStrength = server.thunderingStrength;
		rainingStrength = server.rainingStrength;
		worldTime = server.provider.getWorldTime();
	}
	
	@Override
	public void encodeInto(ChannelHandlerContext ctx, ByteBuf out) {
		out.writeInt(dimension);
		out.writeInt(posX);
		out.writeInt(posY);
		out.writeInt(posZ);
		out.writeInt(skylightSubtracted);
		out.writeFloat(thunderingStrength);
		out.writeFloat(rainingStrength);
		out.writeLong(worldTime);
	}

	@Override
	public void decodeInto(ChannelHandlerContext ctx, ByteBuf in) {
		dimension = in.readInt();
		posX = in.readInt();
		posY = in.readInt();
		posZ = in.readInt();
		skylightSubtracted = in.readInt();
		thunderingStrength = in.readFloat();
		rainingStrength = in.readFloat();
		worldTime = in.readLong();
	}

	@Override
	public int getSize() {
		// TODO Auto-generated method stub
		return 36;
	}
	
	@Override
	public void handleClientSide(EntityPlayer player){
		
	}
	
	@Override
	public void handleServerSide(EntityPlayer player){
		DynamicLinkPanels.debug("LinkInfoPacket.handleServerSide()");
		LinkWorld dlpw = DynamicLinkPanels.instance.linkWorld;
		if(dlpw==null)
			return;
		if(this.dimension==dlpw.provider.dimensionId)
		{	
			ChunkCoordinates cc = new ChunkCoordinates();
			cc.set(this.posX, this.posY, this.posZ);
			dlpw.setCameraToSpawn(cc);
			dlpw.skylightSubtracted = this.skylightSubtracted;
			dlpw.thunderingStrength = this.thunderingStrength;
			dlpw.setRainStrength(this.rainingStrength);
			dlpw.setWorldTime2(this.worldTime);
		}
	}
}