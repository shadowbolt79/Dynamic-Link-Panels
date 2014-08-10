package com.shadowking97.mystcraftplugin.dynamicLinkPanels.network.packets;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.shadowking97.mystcraftplugin.dynamicLinkPanels.DynamicLinkPanels;
import com.shadowking97.mystcraftplugin.dynamicLinkPanels.linkWorld.LinkWorld;
import com.shadowking97.mystcraftplugin.dynamicLinkPanels.network.impl.LinkPacket;

import cpw.mods.fml.relauncher.Side;

public class TileEntityNBTPacket extends LinkPacket {
	public int dimension;
	public int xPos;
	public int yPos;
	public int zPos;
	public NBTTagCompound data;
	public boolean error = false;
	private int tempLength;
	
	public TileEntityNBTPacket(){}
	
	public TileEntityNBTPacket(int x, int y, int z, NBTTagCompound nBTTagCompound, int dim)
    {
		DynamicLinkPanels.debug("TileEntityNBTPacket()");
        this.xPos = x;
        this.yPos = y;
        this.zPos = z;
        this.data = nBTTagCompound;
        this.dimension = dim;
        this.tempLength = 0;
    }
	
	   /**
     * Reads a compressed NBTTagCompound from the InputStream
     */
    public static NBTTagCompound readNBTTagCompound(ByteBuf par0DataInput) throws IOException
    {
        short short1 = par0DataInput.readShort();

        if (short1 < 0)
        {
            return null;
        }
        else
        {
            byte[] abyte = new byte[short1];
            par0DataInput.readBytes(abyte);
            return CompressedStreamTools.decompress(abyte);
        }
    }

    /**
     * Writes a compressed NBTTagCompound to the OutputStream
     */
    protected static int writeNBTTagCompound(NBTTagCompound par0NBTTagCompound, ByteBuf par1DataOutput) throws IOException
    {
        if (par0NBTTagCompound == null)
        {
            par1DataOutput.writeShort(-1);
        }
        else
        {
            byte[] abyte = CompressedStreamTools.compress(par0NBTTagCompound);
            par1DataOutput.writeShort((short)abyte.length);
            par1DataOutput.writeBytes(abyte);
            return abyte.length;
        }
        return 0;
    }

	@Override
	public int getSize() {
		return 16+tempLength;
	}

	@Override
	public void encodeInto(ChannelHandlerContext ctx, ByteBuf out) {
		out.writeInt(dimension);
		out.writeInt(xPos);
		out.writeInt(yPos);
		out.writeInt(zPos);
		try {
			tempLength = writeNBTTagCompound(data, out);
		} catch (IOException e) {
			error = true;
		}
	}

	@Override
	public void decodeInto(ChannelHandlerContext ctx, ByteBuf in) {
		dimension = in.readInt();
		xPos = in.readInt();
		yPos = in.readInt();
		zPos = in.readInt();
		try {
			data = readNBTTagCompound(in);
		} catch (IOException e) {
			error = true;
		}
	}

	@Override
	public void handleClientSide(EntityPlayer player) {
		DynamicLinkPanels.debug("TileEntityNBTPacket.handleClientSide()");
		LinkWorld dlpw = DynamicLinkPanels.instance.linkWorld;
		if(dlpw==null)
			return;
    	if(dlpw.provider.dimensionId!=this.dimension)
    		return;
        if (dlpw.blockExists(this.xPos, this.yPos, this.zPos))
        {
            TileEntity tileentity = dlpw.getTileEntity(this.xPos, this.yPos, this.zPos);

            if (tileentity != null)
            {
                tileentity.readFromNBT(this.data);
            }
            else
            {
            	//Create tile entity from data
            	tileentity = TileEntity.createAndLoadEntity(this.data);
            	if(tileentity!=null){
                    System.out.println("Created Tile Entity");
            		dlpw.addTileEntity(tileentity);
            	}
            }
    		dlpw.markTileEntityChunkModified(this.xPos, this.yPos, this.zPos, tileentity);
    		dlpw.setTileEntity(this.xPos, this.yPos, this.zPos, tileentity);
    		dlpw.markBlockForUpdate(this.xPos, this.yPos, this.zPos);
        }
	}

	@Override
	public void handleServerSide(EntityPlayer player) {
		// TODO Auto-generated method stub
		
	}
	
}