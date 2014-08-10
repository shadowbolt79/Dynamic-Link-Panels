package com.shadowking97.mystcraftplugin.dynamicLinkPanels;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class DLPConfig extends Configuration {
	public static final String CATAGORY_RENDER = "rendering";
	public static final String CATAGORY_SERVER = "server";
	
	  public DLPConfig(File configfile)
	  {
	    super(configfile);
	  }
}
