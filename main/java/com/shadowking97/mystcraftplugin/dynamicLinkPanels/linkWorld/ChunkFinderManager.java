package com.shadowking97.mystcraftplugin.dynamicLinkPanels.linkWorld;

import java.util.LinkedList;
import java.util.List;

import com.shadowking97.mystcraftplugin.dynamicLinkPanels.DynamicLinkPanels;

public class ChunkFinderManager {
	public static ChunkFinderManager instance = new ChunkFinderManager();
	
	private List<ChunkFinder> finders;
	
	public ChunkFinderManager()
	{
		DynamicLinkPanels.debug("ChunkFinderManager()");
		finders = new LinkedList<ChunkFinder>();
	}
	
	public void addFinder(ChunkFinder f)
	{
		DynamicLinkPanels.debug("ChunkFinderManager.addFinder()");
		finders.add(f);
	}
	
	public void tick()
	{
		DynamicLinkPanels.debug("ChunkFinderManager.tick()");
		for(int i = 0; i<finders.size(); i++)
		{
			if(finders.get(i).findChunks())
			{
				finders.remove(i);
				i--;
			}
		}
	}
}
