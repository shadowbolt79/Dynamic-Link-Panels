package com.shadowking97.mystcraftplugin.dynamicLinkPanels.linkWorld;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.shadowking97.mystcraftplugin.dynamicLinkPanels.DynamicLinkPanels;
import com.shadowking97.mystcraftplugin.dynamicLinkPanels.Util;
import com.shadowking97.mystcraftplugin.dynamicLinkPanels.network.ServerPacketDispatcher;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.ReportedException;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;

/**
 * Finds exposed 16x16x16 chunks exposed to the passed chunk location.
 * @author Ken Butler/shadowking97
 *
 */
public class ChunkFinder{
	
	/**
	 * Finds exposed chunks. Chunks must be loaded.
	 * @param root
	 * The chunk in chunk coordinates.
	 * @param w
	 * The world server that contains the chunks
	 * @param range
	 * The radius of the chunkfinder. 
	 * @return
	 * Sorted Chunk Data, by range. Prioritizes closest chunks.
	 */

	private final ChunkCoordinates root;
	private final IChunkProvider w;
	private final int range;
	private final int dimension;
	private final EntityPlayer player;
	private ChunkData[][] map;
	private List<ChunkCoordinates> cc;
	private final int d;
	private int step;
	private int stepRange;
	private long startTime;
	private int rootX;
	private int rootZ;
	
	public ChunkFinder(ChunkCoordinates root, IChunkProvider w, int range, int dimension, EntityPlayer player)
	{

		  DynamicLinkPanels.debug("ChunkFinder()");
		this.root = root;
		this.w = w;
		this.range = range;
		this.dimension = dimension;
		this.player = player;
		this.d = (range<<1)+1;
		this.map = new ChunkData[d][d];
		this.rootX = root.posX-range;
		this.rootZ = root.posZ-range;
		this.stepRange = 16-root.posY;
		if(root.posY>stepRange)
			stepRange=root.posY;
		startTime=System.nanoTime();
		System.out.println("Scan started at nano: " + startTime);
		for(int i = 0; i<d; i++)
		{
			for(int j = 0; j<d; j++)
			{
				map[i][j] = new ChunkData(i+rootX,j+rootZ);
				int x1 = i-range;
				int z1 = j-range;
				map[i][j].distance = x1*x1+z1*z1;
			}
		}
		cc = new LinkedList<ChunkCoordinates>();
		cc.add(new ChunkCoordinates(range,root.posY,range));
		step = 0;
		List<ChunkCoordinates> cc2 = new LinkedList<ChunkCoordinates>();
		while(step-1<stepRange&&!cc.isEmpty()){
			while(!cc.isEmpty())
			{
				ChunkCoordinates ch = cc.get(0);
				cc2.addAll(scan(w, map, cc.get(0),range));
				cc.remove(0);
			}
			step++;
			cc.addAll(cc2);
			cc2.clear();
			if(step>=stepRange){
				int range2 = step-stepRange+1;
				range2*=range2;
				int range3 = step-stepRange;
				if(range3<0)
					range3=0;
				range3*=range3;
				int minStep = range - (step - stepRange);
				int maxStep = range + (step - stepRange) + 1;
				if(minStep<0)
					minStep=0;
				if(maxStep>d)
					maxStep=d;
				for(int i = minStep; i<maxStep; i++)
				{
					for(int j = minStep; j<maxStep; j++)
					{
						int dist = map[i][j].distance;
						if(map[i][j].doAdd()&&dist<range2&&dist>=range3){
							ChunkData data = map[i][j];
							Chunk c2 = w.provideChunk(data.x,data.z);
							if(!c2.isChunkLoaded)
								c2=w.loadChunk(data.x,data.z);
							
							ServerPacketDispatcher.instance.addPacket(c2, true, dimension, player, data.levels());
						}
					}
				}
			}
		}
	}
	
	public boolean findChunks()
	{

		  DynamicLinkPanels.debug("ChunkFinder.findChunks()");
		if(!cc.isEmpty()){
			int tick = 0;
			List<ChunkCoordinates> cc2 = new LinkedList<ChunkCoordinates>();
			while(!cc.isEmpty()&&tick<15)
			{
				ChunkCoordinates ch = cc.get(0);
				cc2.addAll(scan(w, map, ch,range));
				cc.remove(0);
				tick++;
			}
			if(!cc.isEmpty())
				return false;

			step++;
			
			cc.addAll(cc2);
			cc2.clear();
			
			if(step>=stepRange){
				int range2 = step-stepRange+1;
				range2*=range2;
				int range3 = step-stepRange;
				if(range3<0)
					range3=0;
				range3*=range3;
				int minStep = range - (step - stepRange);
				int maxStep = range + (step - stepRange) + 1;
				if(minStep<0)
					minStep=0;
				if(maxStep>d)
					maxStep=d;
				for(int i = minStep; i<maxStep; i++)
				{
					for(int j = minStep; j<maxStep; j++)
					{
						int dist = map[i][j].distance;
						if(map[i][j].doAdd()&&dist<range2&&dist>=range3){
							ChunkData data = map[i][j];
							Chunk c2 = w.provideChunk(data.x,data.z);
							if(!c2.isChunkLoaded)
								c2=w.loadChunk(data.x,data.z);
							ServerPacketDispatcher.instance.addPacket(c2, true, dimension, player, data.levels());
						}
					}
				}
			}
			return false;
		}
		else{
			if(step>=stepRange){
				int range2 = step-stepRange;
				range2*=range2;
				for(int i = 0; i<d; i++)
				{
					for(int j = 0; j<d; j++)
					{
						int dist = map[i][j].distance;
						if(map[i][j].doAdd()&&dist>=range2){
							ChunkData data = map[i][j];
							Chunk c2 = w.provideChunk(data.x,data.z);
							if(!c2.isChunkLoaded)
								c2=w.loadChunk(data.x,data.z);
							ServerPacketDispatcher.instance.addPacket(c2, true, dimension, player, data.levels());
						}
					}
				}
			}
			System.out.println("Scan finished. nanoseconds: "+(System.nanoTime()-startTime));
			return true;
		}
	}
	
	/**
	 * Recursive function to find all chunk segments attached to the surface.
	 */
	private static List<ChunkCoordinates> scan(IChunkProvider chunkProvider, ChunkData[][] map, ChunkCoordinates coord, int range)
	{
		DynamicLinkPanels.debug("ChunkFinder.scan()");
		int rangeSqr = range*range;
		List<ChunkCoordinates> cc3 = new LinkedList<ChunkCoordinates>();
		int x = coord.posX;
		int y = coord.posY;
		int z = coord.posZ;
		ChunkData data = map[x][z];
		if(data.isAdded(y)||data.distance>rangeSqr)
			return cc3;
		data.add(y);
		Chunk c = chunkProvider.provideChunk(data.x, data.z);
		if(!c.isChunkLoaded){
			c=chunkProvider.loadChunk(data.x, data.z);
		}
		if(c.getAreLevelsEmpty(y<<4, (y<<4)+15)){
			data.empty(y);
			if(x<(range<<1)&&!(map[x+1][z].isAdded(y)||map[x+1][z].distance>rangeSqr||map[x+1][z].distance<map[x][z].distance))
				cc3.add(new ChunkCoordinates(x+1, y, z));
			if(x>0&&!(map[x-1][z].isAdded(y)||map[x-1][z].distance>rangeSqr||map[x-1][z].distance<map[x][z].distance))
				cc3.add(new ChunkCoordinates(x-1, y, z));
			if(y<15&&!(map[x][z].isAdded(y+1)||map[x][z].distance>rangeSqr))
				cc3.add(new ChunkCoordinates(x, y+1, z));
			if(y>0&&!(map[x][z].isAdded(y-1)||map[x][z].distance>rangeSqr))
				cc3.add(new ChunkCoordinates(x, y-1, z));;
			if(z<(range<<1)&&!(map[x][z+1].isAdded(y)||map[x][z+1].distance>rangeSqr||map[x][z+1].distance<map[x][z].distance))
				cc3.add(new ChunkCoordinates(x, y, z+1));
			if(z>0&&!(map[x][z-1].isAdded(y)||map[x][z-1].distance>rangeSqr||map[x][z-1].distance<map[x][z].distance))
				cc3.add(new ChunkCoordinates(x, y, z-1));
		}
		else
		{
			boolean ok = false;
			if(z>0&&!(map[x][z-1].isAdded(y)||map[x][z-1].distance>rangeSqr||map[x][z-1].distance<map[x][z].distance))
			{
				for(int i = 0; i<16&&!ok; i++)
				{
					for(int l = 0; l<16&&!ok; l++)
					{
						WorldServer w;
						if(!isBlockNormalCubeDefault(c,l, (y<<4)+i,0, false))
							ok=true;
					}
				}
				if(ok)
				{
					cc3.add(new ChunkCoordinates(x, y, z-1));
				}
				ok=false;
			}
			if(z<(range<<1)&&!(map[x][z+1].isAdded(y)||map[x][z+1].distance>rangeSqr||map[x][z+1].distance<map[x][z].distance))
			{
				for(int i = 0; i<16&&!ok; i++)
				{
					for(int l = 0; l<16&&!ok; l++)
					{
						if(!isBlockNormalCubeDefault(c,l,(y<<4)+i,15, false))
							ok=true;
					}
				}
				if(ok)
				{
					cc3.add(new ChunkCoordinates(x, y, z+1));
				}
				ok=false;
			}
			if(y>0&&!(map[x][z].isAdded(y-1)||map[x][z].distance>rangeSqr))
			{
				for(int i = 0; i<16&&!ok; i++)
				{
					for(int l = 0; l<16&&!ok; l++)
					{
						if(!isBlockNormalCubeDefault(c,l, (y<<4), i, false))
							ok=true;
					}
				}
				if(ok)
				{
					cc3.add(new ChunkCoordinates(x, y-1, z));
				}
				ok=false;
			}
			if(y<15&&!(map[x][z].isAdded(y+1)||map[x][z].distance>rangeSqr))
			{
				for(int i = 0; i<16&&!ok; i++)
				{
					for(int l = 0; l<16&&!ok; l++)
					{
						if(!isBlockNormalCubeDefault(c,l,(y<<4)+15,i, false))
							ok=true;
					}
				}
				if(ok)
				{
					cc3.add(new ChunkCoordinates(x, y+1, z));
				}
				ok=false;
			}
			if(x>0&&!(map[x-1][z].isAdded(y)||map[x-1][z].distance>rangeSqr||map[x-1][z].distance<map[x][z].distance))
			{
				for(int i = 0; i<16&&!ok; i++)
				{
					for(int l = 0; l<16&&!ok; l++)
					{
						if(!isBlockNormalCubeDefault(c,0, (y<<4)+l, i, false))
							ok=true;
					}
				}
				if(ok)
				{
					cc3.add(new ChunkCoordinates(x-1, y, z));
				}
				ok=false;
			}
			if(x<(range<<1)&&!(map[x+1][z].isAdded(y)||map[x+1][z].distance>rangeSqr||map[x+1][z].distance<map[x][z].distance))
			{
				for(int i = 0; i<16&&!ok; i++)
				{
					for(int l = 0; l<16&&!ok; l++)
					{
						if(!isBlockNormalCubeDefault(c,15, (y<<4)+l, i, false))
							ok=true;
					}
				}
				if(ok)
				{
					cc3.add(new ChunkCoordinates(x+1, y, z));
				}
			}
		}
		return cc3;
	}
	
	public static boolean isBlockNormalCubeDefault(Chunk chunk, int par1, int par2, int par3, boolean par4)
    {
		DynamicLinkPanels.debug("ChunkFinder.isBlockNormalCubeDefault()");
        if (par1 >= -30000000 && par3 >= -30000000 && par1 < 30000000 && par3 < 30000000)
        {
            if (chunk != null && !chunk.isEmpty())
            {
                Block block = chunk.getBlock(par1 & 15, par2, par3 & 15);
                return block.isNormalCube();
            }
            else
            {
                return par4;
            }
        }
        else
        {
            return par4;
        }
    }
	
	public class ChunkData implements Comparable<ChunkData>{
		public int x;
		public int z;
		public int added;
		public int empty;
		public int distance;
		
		public ChunkData(int x, int z)
		{
			this.x = x;
			this.z = z;
			added = 0;
		}
		
		public boolean isAdded(int level)
		{
			return (added & (1<<level)) !=0;
		}
		
		public boolean doAdd()
		{
			return (added^empty)!=0;
		}
		
		public boolean doAdd(int level)
		{
			return isAdded(level)&&!isEmpty(level);
		}
		
		public void add(int level)
		{
			added |= 1<<level;
		}
		
		public boolean isEmpty(int level)
		{
			return (empty & (1<<level)) ==0;
		}
		
		public void empty(int level)
		{
			empty |= 1<<level;
		}
		
		public int levels()
		{
			return added^empty;
		}

		@Override
		public int compareTo(ChunkData arg0) {
			
			return this.distance-arg0.distance;
		}
	}
}