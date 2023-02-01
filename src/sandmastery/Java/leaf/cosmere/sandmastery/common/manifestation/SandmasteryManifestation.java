/*
 * File updated ~ 8 - 10 - 2022 ~ Leaf
 */

package leaf.cosmere.sandmastery.common.manifestation;

import leaf.cosmere.api.Manifestations;
import leaf.cosmere.api.Taldain;
import leaf.cosmere.api.manifestation.Manifestation;
import leaf.cosmere.api.spiritweb.ISpiritweb;
import leaf.cosmere.common.cap.entity.SpiritwebCapability;
import leaf.cosmere.sandmastery.common.capabilities.SandmasterySpiritwebSubmodule;
import leaf.cosmere.sandmastery.common.registries.SandmasteryAttributes;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

public class SandmasteryManifestation extends Manifestation
{
	protected final Taldain.Mastery mastery;

	public SandmasteryManifestation(Taldain.Mastery mastery)
	{
		super(Manifestations.ManifestationTypes.SANDMASTERY);
		this.mastery = mastery;
	}

	@Override
	public double getStrength(ISpiritweb data, boolean getBaseStrength)
	{
		AttributeInstance attribute = data.getLiving().getAttribute(getAttribute());
		if (attribute != null)
		{
			return getBaseStrength ? attribute.getBaseValue() : attribute.getValue();
		}
		return 0;
	}

	@Override
	public int getPowerID()
	{
		return mastery.getID();
	}

	@Override
	public int modeMin(ISpiritweb data) {
		return 0;
	};

	@Override
	public int modeMax(ISpiritweb data) {
		return (int) data.getSelectedManifestation().getStrength(data, false);
	};

	@Override
	public void onModeChange(ISpiritweb data, int lastMode) {
		SpiritwebCapability playerSpiritweb = (SpiritwebCapability) data;
		SandmasterySpiritwebSubmodule submodule = (SandmasterySpiritwebSubmodule) playerSpiritweb.spiritwebSubmodules.get(Manifestations.ManifestationTypes.SANDMASTERY);
		if(getMode(data) > lastMode) {
			submodule.useRibbon(data, this);
		} else if(getMode(data) < lastMode) {
			submodule.releaseRibbon(data, this);
		}
	}

	@Override
	public Attribute getAttribute() {
		return SandmasteryAttributes.RIBBONS.getAttribute();
	}

}
