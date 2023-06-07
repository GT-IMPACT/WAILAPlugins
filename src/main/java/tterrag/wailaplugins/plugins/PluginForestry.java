package tterrag.wailaplugins.plugins;

import com.enderio.core.common.Lang;
import com.enderio.core.common.util.BlockCoord;
import com.mojang.authlib.GameProfile;
import forestry.api.apiculture.EnumBeeChromosome;
import forestry.api.apiculture.IBee;
import forestry.api.arboriculture.EnumTreeChromosome;
import forestry.api.arboriculture.ITree;
import forestry.api.arboriculture.ITreeGenome;
import forestry.api.genetics.IGenome;
import forestry.apiculture.BeekeepingLogic;
import forestry.apiculture.multiblock.TileAlveary;
import forestry.arboriculture.genetics.Tree;
import forestry.arboriculture.tiles.TileLeaves;
import forestry.arboriculture.tiles.TileTreeContainer;
import forestry.core.access.IOwnable;
import forestry.core.proxy.Proxies;
import forestry.core.tiles.TileEngine;
import forestry.core.tiles.TileForestry;
import forestry.core.utils.StringUtil;
import lombok.SneakyThrows;
import mcp.mobius.waila.api.IWailaDataAccessor;
import mcp.mobius.waila.api.IWailaRegistrar;
import mcp.mobius.waila.api.SpecialChars;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import tterrag.wailaplugins.api.Plugin;

import java.lang.reflect.Field;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Plugin(name = "Forestry", deps = "Forestry")
public class PluginForestry extends PluginBase
{
    private static Field _throttle;
    private static Field _maxHeat;
    private static NumberFormat pctFmt = NumberFormat.getPercentInstance();
    private static Lang forLang = new Lang("for");

    @SneakyThrows
    public PluginForestry()
    {
        try
        {
            _throttle = BeekeepingLogic.class.getDeclaredField("throttle");
        }
        catch (NoSuchFieldException e)
        {
            // forestry update
            _throttle = BeekeepingLogic.class.getDeclaredField("queenWorkCycleThrottle");
        }

        _throttle.setAccessible(true);

        _maxHeat = TileEngine.class.getDeclaredField("maxHeat");
        _maxHeat.setAccessible(true);

        pctFmt.setMinimumFractionDigits(2);
    }

    @Override
    public void load(IWailaRegistrar registrar)
    {
        super.load(registrar);

        registerBody(TileForestry.class, TileTreeContainer.class, TileAlveary.class);

        registerNBT(TileForestry.class, TileTreeContainer.class, TileAlveary.class);

        addConfig("power");
        addConfig("heat");
        addConfig("sapling");
        addConfig("leaves");
        addConfig("apiary");
    }

    @Override
    @SneakyThrows
    @SuppressWarnings("unused")
    protected void getBody(ItemStack stack, List<String> currenttip, IWailaDataAccessor accessor)
    {
        Block block = accessor.getBlock();
        TileEntity tile = accessor.getTileEntity();
        World world = accessor.getWorld();
        EntityPlayer player = accessor.getPlayer();
        MovingObjectPosition pos = accessor.getPosition();
        NBTTagCompound tag = accessor.getNBTData();
        int x = pos.blockX, y = pos.blockY, z = pos.blockZ;

        if (tag.hasKey(ENERGY_STORED) && getConfig("power"))
        {
            currenttip.add(tag.getInteger(ENERGY_STORED) + " / " + tag.getInteger(MAX_ENERGY_STORED) + " RF");
        }

        if (tag.hasKey(HEAT) && getConfig("heat"))
        {
            currenttip.add(lang.localize("engineHeat", tag.getInteger(HEAT) / 10D + "\u00B0C"));
        }

        if (tag.hasKey(TREE) && getConfig("sapling"))
        {
            ITree tree = new Tree(tag.getCompoundTag(TREE));
            addGenomeTooltip(tag, (TileTreeContainer) tile, tree, player, currenttip);
        }

        if (tile instanceof TileLeaves && getConfig("leaves"))
        {
            if (((TileLeaves) tile).isPollinated())
            {
                currenttip.add(lang.localize("pollinated", tag.getString(LEAF_BRED_SPECIES)));
            }
        }
    }

    private void addGenomeTooltip(NBTTagCompound tag, TileTreeContainer te, ITree tree, EntityPlayer player, List<String> currenttip)
    {
        UUID owner = UUID.fromString(tag.getString(OWNER));
        // XXX Leaf isAnalyzed is not working properly, wait for forestry fix
        if (owner.equals(player.getGameProfile().getId()) && (tree.isAnalyzed() || te instanceof TileLeaves))
        {
            addTreeTooltip(tree, currenttip);
        }
        else if (tree != null)
        {
            currenttip.add(EnumChatFormatting.ITALIC + (tree.isAnalyzed() ? lang.localize("notOwner") : lang.localize("notAnalyzed")));
        }
    }

    private void addTreeTooltip(ITree tree, List<String> currenttip)
    {
        if (Proxies.common.isShiftDown())
            tree.addTooltip(currenttip);
        else
            currenttip.add(getTMI());
    }

    private String getTMI()
    {
        return EnumChatFormatting.ITALIC + "<" + StringUtil.localize("gui.tooltip.tmi") + ">";
    }

    private String getSpeciesName(IGenome genome, boolean active)
    {
        return active ? genome.getActiveAllele(EnumBeeChromosome.SPECIES).getName() : genome.getInactiveAllele(EnumBeeChromosome.SPECIES).getName();
    }

    private void addIndentedBeeInfo(IBee bee, List<String> currenttip)
    {
        if (Proxies.common.isShiftDown())
        {
            List<String> tt = new ArrayList<String>();
            bee.addTooltip(tt);
            for (int i = 0; i < tt.size(); i++)
            {
                tt.set(i, (i == 0 ? ">" : "") + SpecialChars.TAB + tt.get(i));
            }
            currenttip.addAll(tt);
        }
        else
        {
            currenttip.add(getTMI());
        }
    }

    public static final String LEAF_BRED_SPECIES = "leafBredSpecies";
    public static final String QUEEN_STACK = "queenStack";
    public static final String DRONE_STACK = "droneStack";
    public static final String ERRORS = "errors";
    public static final String BREED_PROGRESS = "breedProgress";
    public static final String TREE = "treeData";
    public static final String ENERGY_STORED = "rfStored";
    public static final String MAX_ENERGY_STORED = "maxRfStored";
    public static final String HEAT = "engineHeat";
    public static final String OWNER = "tileOwner";

    @Override
    @SneakyThrows
    protected void getNBTData(TileEntity te, NBTTagCompound tag, World world, BlockCoord pos)
    {
        if (te instanceof TileLeaves)
        {
            ITreeGenome mate = ((TileLeaves) te).getTree().getMate();
            if (mate != null)
            {
                tag.setString(LEAF_BRED_SPECIES, mate.getActiveAllele(EnumTreeChromosome.SPECIES).getName());
            }
        }
        if (te instanceof TileTreeContainer)
        {
            ITree tree = ((TileTreeContainer) te).getTree();
            NBTTagCompound treeTag = new NBTTagCompound();
            tree.writeToNBT(treeTag);
            tag.setTag(TREE, treeTag);
        }
        if (te instanceof TileEngine)
        {
            tag.setInteger(ENERGY_STORED, ((TileEngine) te).getEnergyManager().getTotalEnergyStored());
            tag.setInteger(MAX_ENERGY_STORED, ((TileEngine) te).getEnergyManager().getMaxEnergyStored());
            tag.setInteger(HEAT, ((TileEngine) te).getHeat());
        }
        if (te instanceof IOwnable)
        {
            GameProfile owner = ((IOwnable) te).getOwner();
            tag.setString(OWNER, owner.getId().toString());
        }
    }
}
