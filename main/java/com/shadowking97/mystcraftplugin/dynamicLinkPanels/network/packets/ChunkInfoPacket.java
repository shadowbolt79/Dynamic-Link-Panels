package com.shadowking97.mystcraftplugin.dynamicLinkPanels.network.packets;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S21PacketChunkData;
import net.minecraft.network.play.server.S21PacketChunkData.Extracted;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import com.shadowking97.mystcraftplugin.dynamicLinkPanels.DynamicLinkPanels;
import com.shadowking97.mystcraftplugin.dynamicLinkPanels.linkWorld.LinkWorld;
import com.shadowking97.mystcraftplugin.dynamicLinkPanels.network.impl.LinkPacket;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ChunkInfoPacket extends LinkPacket {
	
	public int dimension;
	public boolean error = false;
	
    private int xPos;
    private int zPos;
    private int yPos;
    private int yMSBPos;
    private byte[] compressedChunkData;
    private byte[] chunkData;
    private boolean includeInitialize;
    private int field_149285_h;
    private static byte[] field_149286_i = new byte[196864];
    private static final String __OBFID = "CL_00001304";
    private Semaphore deflateGate;

	public ChunkInfoPacket() {}
	
	public ChunkInfoPacket(Chunk p_i45196_1_, boolean p_i45196_2_, int p_i45196_3_, int dim) {

		DynamicLinkPanels.debug("ChunkInfoPacket()");
		dimension = dim;
        this.xPos = p_i45196_1_.xPosition;
        this.zPos = p_i45196_1_.zPosition;
        this.includeInitialize = p_i45196_2_;
        Extracted extracted = getMapChunkData(p_i45196_1_, p_i45196_2_, p_i45196_3_);
        this.yMSBPos = extracted.field_150281_c;
        this.yPos = extracted.field_150280_b;
        this.chunkData = extracted.field_150282_a;
        this.deflateGate = new Semaphore(1);
	}
	
	
	private void deflate()
    {
		DynamicLinkPanels.debug("ChunkInfoPacket.deflate()");
        Deflater deflater = new Deflater(-1);
        try
        {
            deflater.setInput(this.chunkData, 0, this.chunkData.length);
            deflater.finish();
            byte[] deflated = new byte[this.chunkData.length];
            this.field_149285_h = deflater.deflate(deflated);
            this.compressedChunkData = deflated;
        }
        finally
        {
            deflater.end();
        }
    }

    public static int func_149275_c()
    {
        return 196864;
    }

    /**
     * Returns a string formatted as comma separated [field]=[value] values. Used by Minecraft for logging purposes.
     */
    public String serialize()
    {
        return String.format("x=%d, z=%d, full=%b, sects=%d, add=%d, size=%d", new Object[] {Integer.valueOf(this.xPos), Integer.valueOf(this.zPos), Boolean.valueOf(this.includeInitialize), Integer.valueOf(this.yPos), Integer.valueOf(this.yMSBPos), Integer.valueOf(this.field_149285_h)});
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf buffer)
    {
    	if (this.compressedChunkData == null)
        {
            deflateGate.acquireUninterruptibly();
            if (this.compressedChunkData == null)
            {
                deflate();
            }
            deflateGate.release();
        }
    	buffer.writeInt(dimension);
        buffer.writeInt(this.xPos);
        buffer.writeInt(this.zPos);
        buffer.writeBoolean(this.includeInitialize);
        buffer.writeShort((short)(this.yPos & 65535));
        buffer.writeShort((short)(this.yMSBPos & 65535));
        buffer.writeInt(this.compressedChunkData.length);
        buffer.ensureWritable(this.compressedChunkData.length);
        buffer.writeBytes(this.compressedChunkData);
    }

	@Override
	public void decodeInto(ChannelHandlerContext ctx, ByteBuf in) {
		this.dimension = in.readInt();
		this.xPos = in.readInt();
        this.zPos = in.readInt();
        this.includeInitialize = in.readBoolean();
        this.yPos = in.readShort();
        this.yMSBPos = in.readShort();
        int len = in.readInt();

        if (field_149286_i.length < len)
        {
            field_149286_i = new byte[len];
        }
        
        in.readBytes(field_149286_i, 0, len);
        int i = 0;
        int j;
        int msb = 0; //BugFix: MC does not read the MSB array from the packet properly, causing issues for servers that use blocks > 256

        for (j = 0; j < 16; ++j)
        {
            i += this.yPos >> j & 1;
            msb += this.yPos >> j & 1;
        }

        j = 12288 * i;
        j += 2048 * msb;

        if (this.includeInitialize)
        {
            j += 256;
        }

        this.chunkData = new byte[j];
        Inflater inflater = new Inflater();
        inflater.setInput(field_149286_i, 0, len);

        try
        {
            inflater.inflate(this.chunkData);
        }
        catch (DataFormatException dataformatexception)
        {
            error = true;
        }
        finally
        {
            inflater.end();
        }
	}

    @SideOnly(Side.CLIENT)
    public byte[] getChunkData()
    {
        return this.chunkData;
    }
	
    public static Extracted getMapChunkData(Chunk p_149269_0_, boolean p_149269_1_, int p_149269_2_)
    {

		DynamicLinkPanels.debug("ChunkInfoPacket.getMapChunkData()");
        int j = 0;
        ExtendedBlockStorage[] aextendedblockstorage = p_149269_0_.getBlockStorageArray();
        int k = 0;
        S21PacketChunkData.Extracted extracted = new S21PacketChunkData.Extracted();
        byte[] abyte = field_149286_i;

        if (p_149269_1_)
        {
            p_149269_0_.sendUpdates = true;
        }

        int l;

        for (l = 0; l < aextendedblockstorage.length; ++l)
        {
            if (aextendedblockstorage[l] != null && (!p_149269_1_ || !aextendedblockstorage[l].isEmpty()) && (p_149269_2_ & 1 << l) != 0)
            {
                extracted.field_150280_b |= 1 << l;

                if (aextendedblockstorage[l].getBlockMSBArray() != null)
                {
                    extracted.field_150281_c |= 1 << l;
                    ++k;
                }
            }
        }

        for (l = 0; l < aextendedblockstorage.length; ++l)
        {
            if (aextendedblockstorage[l] != null && (!p_149269_1_ || !aextendedblockstorage[l].isEmpty()) && (p_149269_2_ & 1 << l) != 0)
            {
                byte[] abyte1 = aextendedblockstorage[l].getBlockLSBArray();
                System.arraycopy(abyte1, 0, abyte, j, abyte1.length);
                j += abyte1.length;
            }
        }

        NibbleArray nibblearray;

        for (l = 0; l < aextendedblockstorage.length; ++l)
        {
            if (aextendedblockstorage[l] != null && (!p_149269_1_ || !aextendedblockstorage[l].isEmpty()) && (p_149269_2_ & 1 << l) != 0)
            {
                nibblearray = aextendedblockstorage[l].getMetadataArray();
                System.arraycopy(nibblearray.data, 0, abyte, j, nibblearray.data.length);
                j += nibblearray.data.length;
            }
        }

        for (l = 0; l < aextendedblockstorage.length; ++l)
        {
            if (aextendedblockstorage[l] != null && (!p_149269_1_ || !aextendedblockstorage[l].isEmpty()) && (p_149269_2_ & 1 << l) != 0)
            {
                nibblearray = aextendedblockstorage[l].getBlocklightArray();
                System.arraycopy(nibblearray.data, 0, abyte, j, nibblearray.data.length);
                j += nibblearray.data.length;
            }
        }

        if (!p_149269_0_.worldObj.provider.hasNoSky)
        {
            for (l = 0; l < aextendedblockstorage.length; ++l)
            {
                if (aextendedblockstorage[l] != null && (!p_149269_1_ || !aextendedblockstorage[l].isEmpty()) && (p_149269_2_ & 1 << l) != 0)
                {
                    nibblearray = aextendedblockstorage[l].getSkylightArray();
                    System.arraycopy(nibblearray.data, 0, abyte, j, nibblearray.data.length);
                    j += nibblearray.data.length;
                }
            }
        }

        if (k > 0)
        {
            for (l = 0; l < aextendedblockstorage.length; ++l)
            {
                if (aextendedblockstorage[l] != null && (!p_149269_1_ || !aextendedblockstorage[l].isEmpty()) && aextendedblockstorage[l].getBlockMSBArray() != null && (p_149269_2_ & 1 << l) != 0)
                {
                    nibblearray = aextendedblockstorage[l].getBlockMSBArray();
                    System.arraycopy(nibblearray.data, 0, abyte, j, nibblearray.data.length);
                    j += nibblearray.data.length;
                }
            }
        }

        if (p_149269_1_)
        {
            byte[] abyte2 = p_149269_0_.getBiomeArray();
            System.arraycopy(abyte2, 0, abyte, j, abyte2.length);
            j += abyte2.length;
        }

        extracted.field_150282_a = new byte[j];
        System.arraycopy(abyte, 0, extracted.field_150282_a, 0, j);
        return extracted;
    }

    @SideOnly(Side.CLIENT)
    public int getXChunk()
    {
        return this.xPos;
    }

    @SideOnly(Side.CLIENT)
    public int getZChunk()
    {
        return this.zPos;
    }

    @SideOnly(Side.CLIENT)
    public int getYChunks()
    {
        return this.yPos;
    }

    @SideOnly(Side.CLIENT)
    public int getMSB()
    {
        return this.yMSBPos;
    }

    @SideOnly(Side.CLIENT)
    public boolean getIncludeInitialize()
    {
        return this.includeInitialize;
    }

	@Override
	public int getSize() {
		// TODO Auto-generated method stub
		return this.compressedChunkData.length+25;
	}
	
	public void handleServerSide(EntityPlayer player){
		
	}
	
	public void handleClientSide(EntityPlayer player){

		DynamicLinkPanels.debug("ChunkInfoPacket.handleClientSide()");
		if(error)
		{
			DynamicLinkPanels.instance.packetPipeline.sendToServer(new RequestPacket(RequestPacket.chunkRequest, this.getXChunk(), this.getYChunks(), this.getZChunk(), this.dimension));
			return;
		}
	    LinkWorld dlp = DynamicLinkPanels.instance.linkWorld;
	    if(dlp==null)
	    	return;
	    if(this.dimension!=dlp.provider.dimensionId)
    		return;
		   
	    if (this.getIncludeInitialize())
		   {
		       if (this.getYChunks() == 0)
		       {
		           dlp.doPreChunk(this.getXChunk(), this.getZChunk(), false);
		           return;
		       }
				        dlp.doPreChunk(this.getXChunk(), this.getZChunk(), true);
		   }
				    dlp.invalidateBlockReceiveRegion(this.getXChunk() << 4, 0, this.getZChunk() << 4, (this.getXChunk() << 4) + 15, 256, (this.getZChunk() << 4) + 15);
		   Chunk chunk = dlp.getChunkFromChunkCoords(this.getXChunk(), this.getZChunk());
				    if (this.getIncludeInitialize() && chunk == null)
		   {
		       dlp.doPreChunk(this.getXChunk(), this.getZChunk(), true);
		       chunk = dlp.getChunkFromChunkCoords(this.getXChunk(), this.getZChunk());
		   }
				    if (chunk != null)
		   {
		       chunk.fillChunk(this.getChunkData(), this.getYChunks(), this.getMSB(), this.getIncludeInitialize());
		       dlp.receivedChunk(this.getXChunk(), this.getZChunk());
				        if (!this.getIncludeInitialize() || !(dlp.provider instanceof WorldProviderSurface))
		       {
		           chunk.resetRelightChecks();
		       }
		   }
		return;
	}
}
