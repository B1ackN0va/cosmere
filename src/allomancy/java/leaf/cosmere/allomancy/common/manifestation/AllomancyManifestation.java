/*
 * File updated ~ 8 - 10 - 2022 ~ Leaf
 */

package leaf.cosmere.allomancy.common.manifestation;

import leaf.cosmere.allomancy.client.AllomancyKeybindings;
import leaf.cosmere.allomancy.common.capabilities.AllomancySpiritwebSubmodule;
import leaf.cosmere.allomancy.common.registries.AllomancyAttributes;
import leaf.cosmere.allomancy.common.registries.AllomancyStats;
import leaf.cosmere.api.CosmereAPI;
import leaf.cosmere.api.IHasMetalType;
import leaf.cosmere.api.Manifestations;
import leaf.cosmere.api.Metals;
import leaf.cosmere.api.manifestation.Manifestation;
import leaf.cosmere.api.spiritweb.ISpiritweb;
import leaf.cosmere.common.cap.entity.SpiritwebCapability;
import leaf.cosmere.common.charge.MetalmindChargeHelper;
import leaf.cosmere.common.registration.impl.AttributeRegistryObject;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

public class AllomancyManifestation extends Manifestation implements IHasMetalType
{
	private final Metals.MetalType metalType;

	public AllomancyManifestation(Metals.MetalType metalType)
	{
		super(Manifestations.ManifestationTypes.ALLOMANCY);
		this.metalType = metalType;
	}


	@Override
	public int getPowerID()
	{
		return metalType.getID();
	}

	@Override
	public boolean modeWraps(ISpiritweb data)
	{
		return true;
	}

	@Override
	public Metals.MetalType getMetalType()
	{
		return this.metalType;
	}

	//active or not active
	@Override
	public int modeMax(ISpiritweb data)
	{
		//1 for burning
		//2 for flaring
		return 2;
	}

	@Override
	public void onModeChange(ISpiritweb data)
	{
		super.onModeChange(data);

		if (getMode(data) > 0)
		{
			//don't reset stats while burning
			return;
		}

		if (data.getLiving() instanceof ServerPlayer serverPlayer)
		{
			serverPlayer.resetStat(Stats.CUSTOM.get(getBurnTimeStat()));
		}
	}

	@Override
	public int modeMin(ISpiritweb data)
	{
		//Allmancy doesn't have a negative, so 0 as 'off'
		return 0;
	}

	@Override
	public boolean isActive(ISpiritweb data)
	{
		return super.isActive(data) && isMetalBurning(data);
	}

	//A metal is considered burning if the user has the power and can afford the next tick of burning.
	public boolean isMetalBurning(ISpiritweb data)
	{
		int mode = getMode(data);
		AllomancySpiritwebSubmodule allo = (AllomancySpiritwebSubmodule) ((SpiritwebCapability) data).spiritwebSubmodules.get(Manifestations.ManifestationTypes.ALLOMANCY);


		//make sure the user can afford the cost of burning this metal
		while (mode > 0)
		{
			//if not then try reduce the amount that they are burning

			if (allo.adjustIngestedMetal(metalType, -mode, false))
			{
				return true;
			}
			else
			{
				mode--;
				//set that mode back to the capability.
				data.setMode(this, mode);
				//if it hits zero then return out
				//try again at a lower burn rate.
			}
		}
		return false;
	}

	@Override
	public void tick(ISpiritweb data)
	{
		if (!isActive(data))
		{
			return;
		}

		int mode = getMode(data);
		AllomancySpiritwebSubmodule allo = (AllomancySpiritwebSubmodule) ((SpiritwebCapability) data).spiritwebSubmodules.get(Manifestations.ManifestationTypes.ALLOMANCY);


		//don't check every tick.
		LivingEntity livingEntity = data.getLiving();
		boolean isActiveTick = livingEntity.tickCount % 20 == 0;
		allo.adjustIngestedMetal(metalType, -mode, isActiveTick);

		if (livingEntity instanceof ServerPlayer serverPlayer)
		{
			serverPlayer.awardStat(getBurnTimeStat());
		}


		//if we get to this point, we are in an active burn state.
		//check for compound.
		final Manifestation feruchemyManifestation = CosmereAPI.manifestationRegistry().getValue(new ResourceLocation("feruchemy", metalType.getName()));
		int feruchemyMode = data.hasManifestation(feruchemyManifestation)
		                    ? feruchemyManifestation.getMode(data)
		                    : 0;

		//feruchemy power exists and is active
		if (feruchemyMode != 0 && isActiveTick)
		{
			//todo config variable
			//eg 10 base, * 2 for flaring mode = 20
			//or from spike 7 * 2 = 14
			//then add the config value
			//max should be around 30. 50 was way too much

			int secondsOfFeruchemyToAdd = (int) Math.floor(getRange(data)) - 5;
			if (null != MetalmindChargeHelper.adjustMetalmindChargeExact(data, metalType, (secondsOfFeruchemyToAdd * mode), true, true))
			{
				//compound successful
			}
		}

		applyEffectTick(data);
	}

	private ResourceLocation getBurnTimeStat()
	{
		final ResourceLocation resourceLocation = AllomancyStats.ALLOMANCY_BURN_TIME.get(this.metalType).get();
		//force set this stat to be time related, which happens in the get function
		Stat<ResourceLocation> doot = Stats.CUSTOM.get(resourceLocation, StatFormatter.TIME);
		return resourceLocation;
	}

	protected KeyMapping getKeyBinding()
	{
		if (getMetalType().isPullMetal())
		{
			return AllomancyKeybindings.ALLOMANCY_PULL;
		}
		else if (getMetalType().isPushMetal())
		{
			return AllomancyKeybindings.ALLOMANCY_PUSH;
		}

		return null;
	}

	public double getStrength(ISpiritweb data, boolean getBaseStrength)
	{
		AttributeRegistryObject<Attribute> mistingAttribute = AllomancyAttributes.ALLOMANCY_ATTRIBUTES.get(metalType);
		AttributeInstance attribute = data.getLiving().getAttribute(mistingAttribute.getAttribute());
		if (attribute != null)
		{
			return getBaseStrength ? attribute.getBaseValue() : attribute.getValue();
		}
		return 0;
	}


	public int getRange(ISpiritweb data)
	{
		if (!isActive(data))
		{
			return 0;
		}

		//get allomantic strength
		double allomanticStrength = getStrength(data, false);
		return Mth.floor(allomanticStrength * getMode(data));

	}

}