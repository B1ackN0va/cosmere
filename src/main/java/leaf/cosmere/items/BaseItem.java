/*
 * File created ~ 24 - 4 - 2021 ~ Leaf
 */

package leaf.cosmere.items;

import leaf.cosmere.itemgroups.CosmereItemGroups;
import leaf.cosmere.properties.PropTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;

public class BaseItem extends Item
{
	public BaseItem()
	{
		super(PropTypes.Items.SIXTY_FOUR.get().tab(CosmereItemGroups.ITEMS));
	}

	public BaseItem(Item.Properties prop)
	{
		super(prop);
	}

	protected int getBarWidth(float value, float max)
	{
		final float percentage = value / max;
		final float lerp = Mth.lerp(percentage, 0, 13);
		return Math.round(lerp);
	}

	protected int getBarColour(float value, float max)
	{
		float f = 1 - Math.max(0.0F, (max - value) / max);
		return Mth.hsvToRgb(f / 3.0F, 1.0F, 1.0F);
	}

}
