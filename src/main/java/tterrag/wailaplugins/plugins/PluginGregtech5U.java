package tterrag.wailaplugins.plugins;

import com.enderio.core.common.util.BlockCoord;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.BaseMetaPipeEntity;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.BaseTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_BasicBatteryBuffer;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_BasicMachine;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_MultiBlockBase;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Transformer;
import gregtech.api.util.GT_Utility;
import gregtech.common.covers.GT_Cover_Fluidfilter;
import gregtech.common.tileentities.boilers.GT_MetaTileEntity_Boiler_Solar;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_PrimitiveBlastFurnace;
import lombok.SneakyThrows;
import mcp.mobius.waila.api.IWailaDataAccessor;
import mcp.mobius.waila.api.IWailaRegistrar;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import tterrag.wailaplugins.api.Plugin;

import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;

import static mcp.mobius.waila.api.SpecialChars.*;
import static net.minecraft.util.StatCollector.translateToLocal;

@Plugin(name = "Gregtech5U", deps = "gregtech")
public class PluginGregtech5U extends PluginBase
{
    
    private String trans(String str) {
        return translateToLocal("wp.config.Gregtech5U." + str);
    }

    @Override
    public void load(IWailaRegistrar registrar)
    {
        super.load(registrar);

        addConfig("machineFacing");
        addConfig("transformer");
        addConfig("solar");
        addConfig("multiblock");
        addConfig("fluidFilter");
        addConfig("basicmachine");
        registerBody(BaseTileEntity.class);
        registerNBT(BaseTileEntity.class);
    }

    @Override
    @SneakyThrows
    @SuppressWarnings("unused")
    protected void getBody(ItemStack stack, List<String> currenttip, IWailaDataAccessor accessor)
    {
        final TileEntity tile = accessor.getTileEntity();
        MovingObjectPosition pos = accessor.getPosition();
        NBTTagCompound tag = accessor.getNBTData();
        final int side = (byte)accessor.getSide().ordinal();

        final IGregTechTileEntity tBaseMetaTile = tile instanceof IGregTechTileEntity ? ((IGregTechTileEntity) tile) : null;
        final IMetaTileEntity tMeta = tBaseMetaTile != null ? tBaseMetaTile.getMetaTileEntity() : null;
        final BaseMetaTileEntity mBaseMetaTileEntity = tile instanceof  BaseMetaTileEntity ? ((BaseMetaTileEntity) tile) : null;
        final GT_MetaTileEntity_MultiBlockBase multiBlockBase = tMeta instanceof GT_MetaTileEntity_MultiBlockBase ? ((GT_MetaTileEntity_MultiBlockBase) tMeta) : null;
        final GT_MetaTileEntity_BasicMachine BasicMachine = tMeta instanceof GT_MetaTileEntity_BasicMachine ? ((GT_MetaTileEntity_BasicMachine) tMeta) : null;
        final GT_MetaTileEntity_BasicBatteryBuffer bateryBuffer = tMeta instanceof GT_MetaTileEntity_BasicBatteryBuffer ? ((GT_MetaTileEntity_BasicBatteryBuffer) tMeta) : null;

        final boolean showTransformer = tMeta instanceof GT_MetaTileEntity_Transformer && getConfig("transformer");
        final boolean showSolar = tMeta instanceof GT_MetaTileEntity_Boiler_Solar && getConfig("solar");
        final boolean allowedToWork = tag.hasKey("isAllowedToWork") && tag.getBoolean("isAllowedToWork");

        if (tBaseMetaTile != null && getConfig("fluidFilter")) {
            final String filterKey = "filterInfo" + side;
            if (tag.hasKey(filterKey)) {
                currenttip.add(tag.getString(filterKey));
            }
        }

        if (tMeta != null) {
            String facingStr = trans("facing");
            if (showTransformer && tag.hasKey("isAllowedToWork")) {
                currenttip.add(
                    String.format(
                        "%s %d(%dA) -> %d(%dA)",
                        (allowedToWork ? (GREEN + trans("stepdown")) : (RED + trans("stepup"))) + RESET,
                        tag.getLong("maxEUInput"),
                        tag.getLong("maxAmperesIn"),
                        tag.getLong("maxEUOutput"),
                        tag.getLong("maxAmperesOut")
                    )
                );
                facingStr = tag.getBoolean("isAllowedToWork") ? trans("input") : trans("output");
            }
            if (showSolar && tag.hasKey("calcificationOutput")) {
                currenttip.add(String.format((GOLD + trans("solaroutput") + ": " + RESET + "%d/%d L/s"), tag.getInteger("calcificationOutput"), tag.getInteger("maxCalcificationOutput")));
            }

            if (tMeta instanceof  GT_MetaTileEntity_PrimitiveBlastFurnace){
                if(tag.getBoolean("incompleteStructurePrimitiveBlastFurnace")) {
                    currenttip.add(RED + trans("incompletestructure") + RESET);
                }

                if (tag.getInteger("progressPrimitiveBlastFurnace") <= 20 &&
                        tag.getInteger("maxProgressPrimitiveBlastFurnace") <= 20) {
                    currenttip.add(trans("progress") + String.format(": %d t / %d t",
                            tag.getInteger("progressPrimitiveBlastFurnace"),
                            tag.getInteger("maxProgressPrimitiveBlastFurnace")));
                } else {
                    currenttip.add(String.format(trans("progress") + ": %d s / %d s",
                            tag.getInteger("progressPrimitiveBlastFurnace") / 20,
                            tag.getInteger("maxProgressPrimitiveBlastFurnace") / 20));
                }

            }

            if (mBaseMetaTileEntity != null && getConfig("machineFacing")) {
                final int facing = mBaseMetaTileEntity.getFrontFacing();
                if(showTransformer) {
                    if((side == facing && allowedToWork) || (side != facing && !allowedToWork)) {
                        currenttip.add(String.format(GOLD + trans("input") + ":" + RESET + " %d(%dA)", tag.getLong("maxEUInput"), tag.getLong("maxAmperesIn")));
                    } else {
                        currenttip.add(String.format(BLUE + trans("output") + ":" + RESET + " %d(%dA)", tag.getLong("maxEUOutput"), tag.getLong("maxAmperesOut")));
                    }
                } else {
                    currenttip.add(String.format("%s: %s", facingStr, ForgeDirection.getOrientation(facing).name()));
                }
            }


            if(multiBlockBase != null && getConfig("multiblock")) {
                if(tag.getBoolean("incompleteStructure")) {
                    currenttip.add(RED + trans("incompletestructure") + RESET);
                }
                currenttip.add((tag.getBoolean("hasProblems") ? (RED + trans("maintenance")) : GREEN + trans("running")) + RESET + "  " + trans("efficiency") + " : " + tag.getFloat("efficiency") + "%");

                if (tag.getInteger("progress") <= 20 && tag.getInteger("maxProgress") <= 20 ) {
                    currenttip.add(trans("progress") + String.format(": %d t / %d t", tag.getInteger("progress"), tag.getInteger("maxProgress")));
                } else {
                    currenttip.add(trans("progress") + String.format(": %d s / %d s", tag.getInteger("progress") / 20, tag.getInteger("maxProgress") / 20));
                }
            }

            if (BasicMachine != null && getConfig("basicmachine")) {

                if (tag.getInteger("progressSingleBlock") <= 20 && tag.getInteger("maxProgressSingleBlock") <= 20 ) {
                    currenttip.add(trans("progress") + String.format(": %d t / %d t", tag.getInteger("progressSingleBlock"), tag.getInteger("maxProgressSingleBlock")));
                } else {
                    currenttip.add(trans("progress") + String.format(": %d s / %d s", tag.getInteger("progressSingleBlock") / 20, tag.getInteger("maxProgressSingleBlock") / 20));
                }
                currenttip.add(trans("consumption") + ": " + RED + tag.getInteger("EUOut") + RESET + " " + trans("eu"));
            }

            if(bateryBuffer != null && getConfig("basicmachine")) {
                currenttip.add(trans("usedcapacity") + ": " + GREEN + GT_Utility.formatNumbers(tag.getLong("nowStorage")) + RESET + " " + trans("eu"));
                currenttip.add(trans("totalcapacity") + ": " + YELLOW + GT_Utility.formatNumbers(tag.getLong("maxStorage")) + RESET + " " + trans("eu"));
                currenttip.add(trans("input") + ": " + GREEN + GT_Utility.formatNumbers(tag.getLong("energyInput")) + RESET + " " + trans("eut"));
                currenttip.add(trans("output") + ": " + RED + GT_Utility.formatNumbers(tag.getLong("energyOutput")) + RESET + " " + trans("eut"));
            }
        }
    }


    @Override
    @SneakyThrows
    protected void getNBTData(TileEntity tile, NBTTagCompound tag, World world, BlockCoord pos)
    {
        final IGregTechTileEntity tBaseMetaTile = tile instanceof IGregTechTileEntity ? ((IGregTechTileEntity) tile) : null;
        final IMetaTileEntity tMeta = tBaseMetaTile != null ? tBaseMetaTile.getMetaTileEntity() : null;
        final GT_MetaTileEntity_MultiBlockBase multiBlockBase = tMeta instanceof GT_MetaTileEntity_MultiBlockBase ? ((GT_MetaTileEntity_MultiBlockBase) tMeta) : null;
        final GT_MetaTileEntity_BasicMachine BasicMachine = tMeta instanceof GT_MetaTileEntity_BasicMachine ? ((GT_MetaTileEntity_BasicMachine) tMeta) : null;
        final GT_MetaTileEntity_BasicBatteryBuffer bateryBuffer = tMeta instanceof GT_MetaTileEntity_BasicBatteryBuffer ? ((GT_MetaTileEntity_BasicBatteryBuffer) tMeta) : null;


        if (tMeta != null) {
            if (tMeta instanceof GT_MetaTileEntity_Transformer) {
                final GT_MetaTileEntity_Transformer transformer = (GT_MetaTileEntity_Transformer)tMeta;
                tag.setBoolean("isAllowedToWork", tMeta.getBaseMetaTileEntity().isAllowedToWork());
                tag.setLong("maxEUInput", transformer.maxEUInput());
                tag.setLong("maxAmperesIn", transformer.maxAmperesIn());
                tag.setLong("maxEUOutput", transformer.maxEUOutput());
                tag.setLong("maxAmperesOut", transformer.maxAmperesOut());
            } else if (tMeta instanceof GT_MetaTileEntity_Boiler_Solar) {
                final GT_MetaTileEntity_Boiler_Solar solar = (GT_MetaTileEntity_Boiler_Solar)tMeta;
                tag.setInteger("calcificationOutput", (solar.getCalcificationOutput()*20/25));
                tag.setInteger("maxCalcificationOutput", (solar.getBasicOutput()*20/25));
            } else if (tMeta instanceof  GT_MetaTileEntity_PrimitiveBlastFurnace) {
                final GT_MetaTileEntity_PrimitiveBlastFurnace blastFurnace = (GT_MetaTileEntity_PrimitiveBlastFurnace) tMeta;
                final int progress = blastFurnace.mProgresstime;
                final int maxProgress = blastFurnace.mMaxProgresstime;
                tag.setInteger("progressPrimitiveBlastFurnace", progress);
                tag.setInteger("maxProgressPrimitiveBlastFurnace", maxProgress);
                tag.setBoolean("incompleteStructurePrimitiveBlastFurnace", !blastFurnace.mMachine);
            }


            if (multiBlockBase != null) {
                final int problems = multiBlockBase.getIdealStatus() - multiBlockBase.getRepairStatus();
                final float efficiency = multiBlockBase.mEfficiency / 100.0F;
                final int progress = multiBlockBase.mProgresstime;
                final int maxProgress = multiBlockBase.mMaxProgresstime;

                tag.setBoolean("hasProblems", problems > 0);
                tag.setFloat("efficiency", efficiency);
                tag.setInteger("progress", progress);
                tag.setInteger("maxProgress", maxProgress);
                tag.setBoolean("incompleteStructure", (tBaseMetaTile.getErrorDisplayID() & 64) != 0);
            }

            if (BasicMachine != null) {
                final int progressSingleBlock = BasicMachine.mProgresstime;
                final int maxProgressSingleBlock = BasicMachine.mMaxProgresstime;
                final int EUOut = BasicMachine.mEUt;
                tag.setInteger("progressSingleBlock", progressSingleBlock);
                tag.setInteger("maxProgressSingleBlock", maxProgressSingleBlock);
                tag.setInteger("EUOut", EUOut);
            }

            if (bateryBuffer != null) {
                long[] tmp = bateryBuffer.getStoredEnergy();
                long nowStorage = tmp[0];
                long maxStorage = tmp[1];

                long energyInput = bateryBuffer.getBaseMetaTileEntity().getAverageElectricInput();
                long energyOutput = bateryBuffer.getBaseMetaTileEntity().getAverageElectricOutput();
                tag.setLong("nowStorage", nowStorage);
                tag.setLong("maxStorage", maxStorage);
                tag.setLong("energyInput", energyInput);
                tag.setLong("energyOutput", energyOutput);
            }

        }
        if (tBaseMetaTile != null) {
            if (tBaseMetaTile instanceof BaseMetaPipeEntity) {
                for(byte side=0 ; side < 6 ; side++) {
                    if(tBaseMetaTile.getCoverBehaviorAtSide(side) instanceof GT_Cover_Fluidfilter) {
                        tag.setString("filterInfo" + side, tBaseMetaTile.getCoverBehaviorAtSide(side).getDescription(side, tBaseMetaTile.getCoverIDAtSide(side), tBaseMetaTile.getCoverDataAtSide(side), tBaseMetaTile));
                    }
                }
            }
        }

        tile.writeToNBT(tag);
    }
}
