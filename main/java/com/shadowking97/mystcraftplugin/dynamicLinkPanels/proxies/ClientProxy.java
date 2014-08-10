package com.shadowking97.mystcraftplugin.dynamicLinkPanels.proxies;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.EXTFramebufferSRGB;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL21;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import com.shadowking97.mystcraftplugin.dynamicLinkPanels.DynamicLinkPanels;
import com.shadowking97.mystcraftplugin.dynamicLinkPanels.linkWorld.LinkWorld;
import com.shadowking97.mystcraftplugin.dynamicLinkPanels.network.packets.ChunkInfoPacket;
import com.shadowking97.mystcraftplugin.dynamicLinkPanels.network.packets.LinkInfoPacket;
import com.shadowking97.mystcraftplugin.dynamicLinkPanels.network.packets.TileEntityNBTPacket;
import com.shadowking97.mystcraftplugin.dynamicLinkPanels.renderers.LinkPanelRenderer;
import com.xcompwiz.mystcraft.api.MystAPI;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.relauncher.Side;

public class ClientProxy extends CommonProxy {
	
	@Override
	public void registerRenderers()
	{
		DynamicLinkPanels.debug("ClientProxy.registerRenderers()");
		if (MystAPI.getInstance().render != null)
		{
			LinkPanelRenderer dest = new LinkPanelRenderer();
			MystAPI.getInstance().render.registerRenderEffect(dest);
			DynamicLinkPanels dlp = DynamicLinkPanels.instance;
			dlp.linkRenderGlobal = new RenderGlobal(Minecraft.getMinecraft());
			dlp.linkEffectRenderer = new EffectRenderer(null, Minecraft.getMinecraft().getTextureManager());
			if(OpenGlHelper.shadersSupported){
				ResourceLocation linkingpanel = (new ResourceLocation("dynamiclinkpanels","shaders/linkingpanel.frag"));
				ResourceLocation vlinkingpanel = (new ResourceLocation("dynamiclinkpanels","shaders/linkingpanel.vert"));
				
				try
				{
					System.out.println(new java.io.File("").getAbsolutePath());
					dlp.vertexARB = createShader(vlinkingpanel, GL20.GL_VERTEX_SHADER);
					dlp.fragmentARB = createShader(linkingpanel,GL20.GL_FRAGMENT_SHADER);
				}catch(Exception e)
				{
				}
				
				dlp.shaderARB = ARBShaderObjects.glCreateProgramObjectARB();
				
				if(dlp.shaderARB==0||dlp.fragmentARB==0)
					return;
				
				ARBShaderObjects.glAttachObjectARB(dlp.shaderARB, dlp.vertexARB);
				ARBShaderObjects.glAttachObjectARB(dlp.shaderARB, dlp.fragmentARB);
	
				ARBShaderObjects.glLinkProgramARB(dlp.shaderARB);
				if (ARBShaderObjects.glGetObjectParameteriARB(dlp.shaderARB, ARBShaderObjects.GL_OBJECT_LINK_STATUS_ARB) == GL11.GL_FALSE) {
					
					return;
				}
				
				ARBShaderObjects.glValidateProgramARB(dlp.shaderARB);
				if (ARBShaderObjects.glGetObjectParameteriARB(dlp.shaderARB, ARBShaderObjects.GL_OBJECT_LINK_STATUS_ARB) == GL11.GL_FALSE) {
					
					return;
				}
			}
			dlp.frameBuffer = EXTFramebufferObject.glGenFramebuffersEXT();
			dlp.colorTexture = GL11.glGenTextures();
			dlp.depthBuffer = EXTFramebufferObject.glGenRenderbuffersEXT(); 
			
			EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, dlp.frameBuffer);
			
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, dlp.colorTexture);
			GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
			GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, dlp.width, dlp.height, 0,GL11.GL_RGBA, GL11.GL_INT, (java.nio.ByteBuffer) null);
			EXTFramebufferObject.glFramebufferTexture2DEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT,GL11.GL_TEXTURE_2D, dlp.colorTexture, 0);
			
			EXTFramebufferObject.glBindRenderbufferEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, dlp.depthBuffer);
			EXTFramebufferObject.glRenderbufferStorageEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, GL14.GL_DEPTH_COMPONENT24, dlp.width, dlp.height);
			EXTFramebufferObject.glFramebufferRenderbufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT,EXTFramebufferObject.GL_RENDERBUFFER_EXT, dlp.depthBuffer);
			
			EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, 0);
		}
	}
	
	
    private int createShader(ResourceLocation resource, int shaderType) throws Exception {
    	int shader = 0;
    	try {
    		
	        shader = ARBShaderObjects.glCreateShaderObjectARB(shaderType);
	        
	        if(shader == 0)
	        	return 0;
	        ARBShaderObjects.glShaderSourceARB(shader, readFileAsString(resource));
	        ARBShaderObjects.glCompileShaderARB(shader);
	        
	        if (ARBShaderObjects.glGetObjectParameteriARB(shader, ARBShaderObjects.GL_OBJECT_COMPILE_STATUS_ARB) == GL11.GL_FALSE)
	            throw new RuntimeException("Error creating shader: " + getLogInfo(shader));
	        
	        return shader;
    	}
    	catch(Exception exc) {
    		ARBShaderObjects.glDeleteObjectARB(shader);
    		throw exc;
    	}
    }

    private static String getLogInfo(int obj) {
        return ARBShaderObjects.glGetInfoLogARB(obj, ARBShaderObjects.glGetObjectParameteriARB(obj, GL20.GL_INFO_LOG_LENGTH));
    }
    
    private String readFileAsString(ResourceLocation r) throws Exception {
        StringBuilder source = new StringBuilder();
        
        InputStream in = Minecraft.getMinecraft().getResourceManager().getResource(r).getInputStream();
        Exception exception = null;
        
        BufferedReader reader;
        try{
            reader = new BufferedReader(new InputStreamReader(in,"UTF-8"));
            
            Exception innerExc= null;
            try {
            	String line;
                while((line = reader.readLine()) != null)
                    source.append(line).append('\n');
            }
            catch(Exception exc) {
            	exception = exc;
            }
            finally {
            	try {
            		reader.close();
            	}
            	catch(Exception exc) {
            		if(innerExc == null)
            			innerExc = exc;
            		else
            			exc.printStackTrace();
            	}
            }
            
            if(innerExc != null)
            	throw innerExc;
        }
        catch(Exception exc) {
        	exception = exc;
        }
        finally {
        	try {
        		in.close();
        	}
        	catch(Exception exc) {
        		if(exception == null)
        			exception = exc;
        		else
					exc.printStackTrace();
        	}
        	
        	if(exception != null)
        		throw exception;
        }
        
        return source.toString();
    }

}
