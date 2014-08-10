package com.shadowking97.mystcraftplugin.dynamicLinkPanels.entities;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import com.shadowking97.mystcraftplugin.dynamicLinkPanels.DynamicLinkPanels;
import com.shadowking97.mystcraftplugin.dynamicLinkPanels.Util;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityHanging;
import net.minecraft.entity.EntityLeashKnot;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.play.server.S1BPacketEntityAttach;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.util.Vec3Pool;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public class EntityCamera extends EntityLiving {
	
	private boolean entityActive = false;
	private boolean positionSet = false;
	
	private int xCenter;
	private int yCenter;
	private int zCenter;
	
	private int yUp = 0;
	private int radius = 0;
	private float pitch;
	
	public EntityCamera(World par1World) {

		super(par1World);

		DynamicLinkPanels.debug("EntityCamera()");
	}

	@Override
	  protected void applyEntityAttributes()
	  {
	    super.applyEntityAttributes();
	    getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(1);
	    getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.0D);
	  }
	
	public void checkFlightPath(int x, int y, int z){
		  DynamicLinkPanels.debug("EntityCamera.checkFlightPath(x,y,z)");
		  setPosition(x,y-1,z);
		  entityActive=true;
		  xCenter = x;
		  yCenter = y-1;
		  zCenter = z;
		  positionSet=true;
		  checkFlightPath();
		  DynamicLinkPanels.instance.openTime=Minecraft.getSystemTime();
	  }
	  
	  public void checkFlightPath()
	  {
		  DynamicLinkPanels.debug("EntityCamera.checkFlightPath()");
		  if(!positionSet)
			  return;
		  int x = xCenter >>4;
		  int z = zCenter >>4;
		  int y = yCenter >>4;
		  
		  boolean found = false;
		  //5-3
		  int up=0;
		  int distance=0;
		  
		  for(int i = 0; i<5&&!found; i++)
		  {
			  switch(i)
			  {
			  case 0:
				  up=2;
				  distance=5;
				  break;
			  case 1:
				  up=5;
				  distance=9;
				  break;
			  case 2:
				  up=9;
				  distance=15;
				  break;
			  case 3:
				  up=1;
				  distance=3;
				  break;
			  case 4:
				  up=0;
				  distance=1;
			  }
			  boolean ok = true;
			  if((yCenter&15)>15-up){
				  if(isAboveLayerNull(x,y,z))
					  ok=false;
				  if((xCenter&15)<distance&&ok)
				  {
					  if(isAboveLayerNull(x-1,y,z))
						  ok=false;
					  if((zCenter&15)<distance&&ok)
					  {
						  if(isAboveLayerNull(x-1,y,z-1))
							  ok=false;
						  if(isAboveLayerNull(x,y,z-1))
							  ok=false;
					  }
					  else if((zCenter&15)>15-distance&&ok)
					  {
						  if(isAboveLayerNull(x-1,y,z+1))
							  ok=false;
						  if(isAboveLayerNull(x,y,z+1))
							  ok=false;
					  }
				  }
				  else if((xCenter&15)>15-distance&&ok)
				  {
					  if(isAboveLayerNull(x+1,y,z))
						  ok=false;
					  if((zCenter&15)<distance&&ok)
					  {
						  if(isAboveLayerNull(x+1,y,z-1))
							  ok=false;
						  if(isAboveLayerNull(x,y,z-1))
							  ok=false;
					  }
					  else if((zCenter&15)>15-distance&&ok)
					  {
						  if(isAboveLayerNull(x+1,y,z+1))
							  ok=false;
						  if(isAboveLayerNull(x,y,z+1))
							  ok=false;
					  }
				  }
				  else
				  {
					  if((zCenter&15)<distance&&ok)
					  {
						  if(isAboveLayerNull(x,y,z-1))
							  ok=false;
					  }
					  else if((zCenter&15)>15-distance&&ok)
					  {
						  if(isAboveLayerNull(x,y,z+1))
							  ok=false;
					  }
				  }
			  }
			  if(ok)
			  {
				  for(int j = -distance;j<=distance&&ok;j++)
				  {
					  for(int k = -distance; k<=distance&&ok; k++)
					  {
						  if(Util.withinRange2D(0, 0, j, k, distance-1, distance+1))
							  if(!worldObj.isAirBlock(xCenter+j, yCenter+up, zCenter+k))
								  ok=false;
					  }
				  }
				  if(ok)
				  {
					  yUp = up;
					  radius = distance;
					  pitch = (float) Math.toDegrees(Math.atan(((double)-up)/distance));
					  return;
				  }
			  }
		  }
		  yUp=0;
		  radius=0;
		  pitch=0;
	  }
	  
	  private boolean isAboveLayerNull(int x, int y, int z)
	  {

		  DynamicLinkPanels.debug("EntityCamera.isAboveLayerNull()");
		  if(y+1>15)
			  return true;
		  int x2 = x<<4;
		  int z2 = z<<4;
		  int y2 = (y<<4)+15;
		  for(int i = 0; i < 15; i++)
			  for(int j = 0; j < 15; j++)
				  if(!worldObj.isBlockNormalCubeDefault(x2+i, y2, z2+i, false))
					  return false;
		  if(worldObj.getChunkFromBlockCoords(x, z).getAreLevelsEmpty((y+1)<<4, ((y+1)<<4)+15))
			  return true;
		  return false;
	  }
	  
	  public void fly(float timePassed) {

		  DynamicLinkPanels.debug("EntityCamera()");
		  this.setAngles(timePassed, 0);
		  this.prevRotationPitch = -pitch; this.rotationPitch = -pitch;
		  if(!entityActive)
			  return;
		  
		  double x = Math.cos(Math.toRadians(rotationYaw+90)) * radius;
		  double z = Math.sin(Math.toRadians(rotationYaw+90)) * radius;
		  this.setPositionAndUpdate(xCenter+0.5-x, yCenter-0.5+yUp, zCenter+0.5-z);
	  }
	  
	  
	  /*
	   * POSSIBLY UNNEEDED CODE TO PREVENT OTHER CODE FROM RUNNING
	   * will not hurt anything if unneeded
	   */
	  
	  @Override
	  public void setAttackTarget(EntityLivingBase par1EntityLivingBase){}
	  
	  @Override
	  public boolean canAttackClass(Class par1Class)
	  {
		  return false;
	  }
	  
	  @Override
	  public void playLivingSound(){}
	  
	  @Override
	  public void onEntityUpdate()
	  {
		  super.onEntityUpdate();
		  //Update Flight path?
	  }
	  
	  @Override
	  protected int getExperiencePoints(EntityPlayer par1EntityPlayer){
		  return 0;
	  }
	  
	  @Override
	  public void spawnExplosionParticle(){}
	  
	  @Override
	  public void onUpdate(){}
	  
	  @Override
	  protected boolean isAIEnabled()
	  {
		  return false;
	  }
	  
	  @Override
	  public void writeEntityToNBT(NBTTagCompound par1NBTTagCompound){}

	  @Override
	  public void readEntityFromNBT(NBTTagCompound par1NBTTagCompound){}
	  
	  @Override
	  public void setMoveForward(float par1){}

	  @Override
	  public void setAIMoveSpeed(float par1){}
	  
	  @Override
	  public void onLivingUpdate(){}
	  
	  @Override
	  protected void updateAITasks(){}
	  
	  @Override
	  protected void updateEntityActionState(){}
	  @Override
	  public void faceEntity(Entity par1Entity, float par2, float par3){}

	  @Override
	  public ItemStack getHeldItem()
	  {
	      return null;
	  }

	  @Override
	  public ItemStack getEquipmentInSlot(int par1)
	  {
	      return null;
	  }
	  
	  @Override
	  public ItemStack func_130225_q(int par1)
	  {
	      return null;
	  }
	  
	  @Override
	  public void setCurrentItemOrArmor(int par1, ItemStack par2ItemStack){}
	  
	  @Override
	  public ItemStack[] getLastActiveItems()
	  {
	      return null;
	  }
	  
	  @Override
	  protected void dropEquipment(boolean par1, int par2){}
	  
	  @Override
	  protected void addRandomArmor(){}
	  
	  @Override
	  protected void enchantEquipment(){}
	  
	  @Override
	  public IEntityLivingData onSpawnWithEgg(IEntityLivingData par1EntityLivingData)
	  {
	      return null;
	  }
	  
	  @Override
	  public void setEquipmentDropChance(int par1, float par2){}

	  @Override
	  public boolean canPickUpLoot()
	  {
	      return false;
	  }
	  
	  @Override
	  public boolean isNoDespawnRequired()
	  {
	      return true;
	  }
	  	  
	  @Override
	  protected void fall(float par1) {}
	  
	  @Override
	  protected void updateFallState(double par1, boolean par3){}
	  
	  @Override
	  protected void onDeathUpdate()
	  {
		  this.setDead();
	  }
	  
	  @Override
	  public EntityLivingBase getAITarget()
	  {
		  return null;
	  }
	  
	  @Override
	  public void setRevengeTarget(EntityLivingBase par1){}
	  
	  @Override
	  public EntityLivingBase getLastAttacker()
	  {
		  return null;
	  }
	  
	  @Override
	  public void setLastAttacker(Entity par1){}
	  
	  @Override
	  protected void updatePotionEffects(){}
	  
	  @Override
	  public void clearActivePotions(){}
	  
	  @Override
	  public boolean isPotionActive(int par1)
	  {
		  return false;
	  }
	  
	  @Override
	  public boolean isPotionActive(Potion par1)
	  {
		  return false;
	  }
	  
	  @Override
	  public PotionEffect getActivePotionEffect(Potion par1)
	  {
		  return null;
	  }
	  
	  @Override
	  public void addPotionEffect(PotionEffect par1){}
	  
	  @Override
	  public boolean isPotionApplicable(PotionEffect par1)
	  {
		  return false;
	  }
	  
	  @Override
	  public boolean isEntityUndead()
	  {
		  return false;
	  }
	  
	  @Override
	  public void removePotionEffectClient(int par1){}
	  
	  @Override
	  public void removePotionEffect(int par1){}
	  
	  @Override
	  protected void onNewPotionEffect(PotionEffect par1){}
	  
	  @Override
	  protected void onChangedPotionEffect(PotionEffect par1, boolean par2){}
	  
	  @Override
	  protected void onFinishedPotionEffect(PotionEffect par1){}
	  
	  @Override
	  public void heal(float par1){}
	  
	  @Override
	  public boolean attackEntityFrom(DamageSource par1, float par2){
		  return false;
	  }
	  
	  @Override
	  public void renderBrokenItemStack(ItemStack par1){}
	  
	  @Override
	  public void onDeath(DamageSource par1){
		  this.worldObj.setEntityState(this, (byte)3);
	  }
	  
	  @Override
	  public void knockBack(Entity par1Entity, float par2, double par3, double par5){}
	  
	  @Override
	  public boolean isOnLadder()
	  {
		  return false;
	  }
	  
	  @Override
	  public int getTotalArmorValue()
	  {
		  return 0;
	  }

	  @Override
	  protected float applyArmorCalculations(DamageSource par1DamageSource, float par2)
	  {
	      return par2;
	  }
	  
	  @Override
	  protected float applyPotionDamageCalculations(DamageSource par1DamageSource, float par2)
	  {
		  return par2;
	  }
	  
	  @Override
	  protected void damageEntity(DamageSource par1, float par2){}
	  
	  @Override
	  public void swingItem(){}
	  
	  @Override
	  protected void updateArmSwingProgress(){}
	  
	  @Override
	  public void setSprinting(boolean par1){}
	  
	  @Override
	  protected float getSoundVolume()
	  {
	      return 0F;
	  }
	  
	  @Override
	  public void dismountEntity(Entity par1Entity){}
	  
	  @Override
	  protected void jump(){}
	  
	  @Override
	  public void moveEntityWithHeading(float par1, float par2){}
	  
	  @Override
	  public void updateRidden(){}
	  
	  @Override
	  public void setJumping(boolean par1){}
	  
	  @Override
	  public void onItemPickup(Entity par1Entity, int par2){}
	  
	  @Override
	  public boolean canEntityBeSeen(Entity par1Entity){
		  return false;
	  }
	  
	  @Override
	  public boolean canBeCollidedWith()
	  {
		  return false;
	  }
	  
	  @Override
	  public boolean canBePushed()
	  {
		  return false;
	  }
	  
	  @Override
	  protected boolean canTriggerWalking()
	  {
	      return false;
	  }
	  
	  @Override
	  public boolean handleWaterMovement()
	  {
		  return false;
	  }
	  
	  @Override
	  public boolean isInsideOfMaterial(Material par1Material){
		  return false;
	  }
	  
	  @Override
	  public boolean handleLavaMovement()
	  {
		  return false;
	  }
	  
	  @Override
	  public void moveFlying(float par1, float par2, float par3){}
	  
	  @Override
	  public float getBrightness(float par1){
		  return 0;
	  }
	  
	  @Override
	  public void applyEntityCollision(Entity par1Entity){}
	  
	  @Override
	  public boolean isBurning()
	  {
		  return false;
	  }
	  
	  @Override
	  public boolean isRiding()
	  {
		  return false;
	  }
	  
	  @Override
	  public boolean isSneaking()
	  {
		  return false;
	  }
	  
	  @Override
	  public boolean isInvisible(){
		  return true;
	  }
	  
	  @Override
	  public void onStruckByLightning(EntityLightningBolt par1){}
	  
	  @Override
	  public boolean isEntityInvulnerable()
	  {
		  return true;
	  }
	  
	  @Override
	  public void travelToDimension(int par1){}
	  
	  @Override
	  public boolean shouldRenderInPass(int pass)
	  {
		  return false;
	  }
	  
	  @Override
	  protected void collideWithEntity(Entity par1Entity) {}
	  
	  @Override
	  protected void collideWithNearbyEntities() {}
	  
	  @Override
	  public boolean doesEntityNotTriggerPressurePlate()
	  {
	    return true;
	  }
	  
	  @Override
	  protected void updateLeashedState(){}
	  
	  @Override
	  public void clearLeashed(boolean par1, boolean par2){}
	  
	  @Override
	  public boolean allowLeashing()
	  {
		  return false;
	  }
	  
	  @Override
	  public boolean getLeashed()
	  {
		  return false;
	  }
	  
	  @Override
	  public Entity getLeashedToEntity()
	  {
		  return null;
	  }
	  
	  @Override
	  public void setLeashedToEntity(Entity par1Entity, boolean par2){}
}
