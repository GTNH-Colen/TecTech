package com.github.technus.tectech.util;

import com.github.technus.tectech.TecTech;
import com.github.technus.tectech.mechanics.alignment.enumerable.ExtendedFacing;
import com.github.technus.tectech.thing.casing.TT_Container_Casings;
import com.github.technus.tectech.thing.metaTileEntity.multi.base.IHatchAdder;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.ObfuscationReflectionHelper;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.GregTech_API;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_TieredMachineBlock;
import gregtech.api.util.GT_OreDictUnificator;
import gregtech.api.util.GT_Utility;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.storage.IPlayerFileData;
import net.minecraft.world.storage.SaveHandler;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.technus.tectech.loader.TecTechConfig.DEBUG_MODE;
import static gregtech.api.enums.GT_Values.E;
import static java.nio.charset.Charset.forName;

/**
 * Created by Tec on 21.03.2017.
 */
public final class Util {
    private Util() {
    }

    @SuppressWarnings("ComparatorMethodParameterNotUsed")
    public static <K, V extends Comparable<? super V>> SortedSet<Map.Entry<K, V>> entriesSortedByValues(Map<K, V> map) {
        SortedSet<Map.Entry<K, V>> sortedEntries = new TreeSet<>(
                (e1, e2) -> {
                    int res = e1.getValue().compareTo(e2.getValue());
                    return res != 0 ? res : 1; // Special fix to preserve items with equal values
                }
        );
        sortedEntries.addAll(map.entrySet());
        return sortedEntries;
    }

    public static int bitStringToInt(String bits){
        if(bits==null){
            return 0;
        }
        if(bits.length() > 32){
            throw new NumberFormatException("Too long!");
        }
        return Integer.parseInt(bits,2);
    }

    public static int hexStringToInt(String hex){
        if(hex==null){
            return 0;
        }
        if(hex.length()>8){
            throw new NumberFormatException("Too long!");
        }
        return Integer.parseInt(hex,16);
    }

    public static double stringToDouble(String str){
        if(str==null){
            return 0;
        }
        return Double.parseDouble(str);
    }

    public static double getValue(String in1) {
        String str = in1.toLowerCase();
        double val;
        try {
            if (str.contains("b")) {
                String[] split = str.split("b");
                val = Util.bitStringToInt(split[0].replaceAll("[^-]", "") + split[1].replaceAll("_", ""));
            } else if (str.contains("x")) {
                String[] split = str.split("x");
                val = Util.hexStringToInt(split[0].replaceAll("[^-]", "") + split[1].replaceAll("_", ""));
            } else {
                val = Util.stringToDouble(str);
            }
            return val;
        } catch (Exception e) {
            return 0;
        }
    }


    public static String intBitsToString(int number) {
        StringBuilder result = new StringBuilder(16);

        for (int i = 31; i >= 0; i--) {
            int mask = 1 << i;
            result.append((number & mask) != 0 ? "1" : "0");

            if (i % 8 == 0) {
                result.append(' ');
            }
        }
        result.replace(result.length() - 1, result.length(), "");

        return result.toString();
    }

    public static String intBitsToShortString(int number) {
        StringBuilder result = new StringBuilder(35);

        for (int i = 31; i >= 0; i--) {
            int mask = 1 << i;
            result.append((number & mask) != 0 ? ":" : ".");

            if (i % 8 == 0) {
                result.append('|');
            }
        }
        result.replace(result.length() - 1, result.length(), "");

        return result.toString();
    }

    public static String longBitsToShortString(long number) {
        StringBuilder result = new StringBuilder(71);

        for (int i = 63; i >= 0; i--) {
            long mask = 1L << i;
            result.append((number & mask) != 0 ? ":" : ".");

            if (i % 8 == 0) {
                result.append('|');
            }
        }
        result.replace(result.length() - 1, result.length(), "");

        return result.toString();
    }

    public static float map(float x, float in_min, float in_max, float out_min, float out_max) {
        return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
    }

    public static boolean isInputEqual(boolean aDecreaseStacksizeBySuccess, boolean aDontCheckStackSizes, FluidStack[] requiredFluidInputs, ItemStack[] requiredInputs, FluidStack[] givenFluidInputs, ItemStack... givenInputs) {
        if (!GregTech_API.sPostloadFinished) {
            return false;
        }
        if (requiredFluidInputs.length > 0 && givenFluidInputs == null) {
            return false;
        }
        int amt;
        for (FluidStack tFluid : requiredFluidInputs) {
            if (tFluid != null) {
                boolean temp = true;
                amt = tFluid.amount;
                for (FluidStack aFluid : givenFluidInputs) {
                    if (aFluid != null && aFluid.isFluidEqual(tFluid)) {
                        if (aDontCheckStackSizes) {
                            temp = false;
                            break;
                        }
                        amt -= aFluid.amount;
                        if (amt < 1) {
                            temp = false;
                            break;
                        }
                    }
                }
                if (temp) {
                    return false;
                }
            }
        }

        if (requiredInputs.length > 0 && givenInputs == null) {
            return false;
        }
        for (ItemStack tStack : requiredInputs) {
            if (tStack != null) {
                amt = tStack.stackSize;
                boolean temp = true;
                for (ItemStack aStack : givenInputs) {
                    if (GT_Utility.areUnificationsEqual(aStack, tStack, true) || GT_Utility.areUnificationsEqual(GT_OreDictUnificator.get(false, aStack), tStack, true)) {
                        if (aDontCheckStackSizes) {
                            temp = false;
                            break;
                        }
                        amt -= aStack.stackSize;
                        if (amt < 1) {
                            temp = false;
                            break;
                        }
                    }
                }
                if (temp) {
                    return false;
                }
            }
        }

        if (aDecreaseStacksizeBySuccess) {
            if (givenFluidInputs != null) {
                for (FluidStack tFluid : requiredFluidInputs) {
                    if (tFluid != null) {
                        amt = tFluid.amount;
                        for (FluidStack aFluid : givenFluidInputs) {
                            if (aFluid != null && aFluid.isFluidEqual(tFluid)) {
                                if (aDontCheckStackSizes) {
                                    aFluid.amount -= amt;
                                    break;
                                }
                                if (aFluid.amount < amt) {
                                    amt -= aFluid.amount;
                                    aFluid.amount = 0;
                                } else {
                                    aFluid.amount -= amt;
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            if (givenInputs != null) {
                for (ItemStack tStack : requiredInputs) {
                    if (tStack != null) {
                        amt = tStack.stackSize;
                        for (ItemStack aStack : givenInputs) {
                            if (GT_Utility.areUnificationsEqual(aStack, tStack, true) || GT_Utility.areUnificationsEqual(GT_OreDictUnificator.get(false, aStack), tStack, true)) {
                                if (aDontCheckStackSizes) {
                                    aStack.stackSize -= amt;
                                    break;
                                }
                                if (aStack.stackSize < amt) {
                                    amt -= aStack.stackSize;
                                    aStack.stackSize = 0;
                                } else {
                                    aStack.stackSize -= amt;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }

        return true;
    }

    public static String getUniqueIdentifier(ItemStack is) {
        return GameRegistry.findUniqueIdentifierFor(is.getItem()).modId + ':' + is.getUnlocalizedName();
    }

    public static byte getTier(long l) {
        byte b = -1;

        do {
            ++b;
            if (b >= CommonValues.V.length) {
                return b;
            }
        } while (l > CommonValues.V[b]);

        return b;
    }

    public static String[] splitButDifferent(String string, String delimiter) {
        String[] strings = new String[StringUtils.countMatches(string, delimiter) + 1];
        int lastEnd = 0;
        for (int i = 0; i < strings.length - 1; i++) {
            int nextEnd = string.indexOf(delimiter, lastEnd);
            strings[i] = string.substring(lastEnd, nextEnd);
            lastEnd = nextEnd + delimiter.length();
        }
        strings[strings.length - 1] = string.substring(lastEnd);
        return strings;
    }

    public static String[] infoFromNBT(NBTTagCompound nbt) {
        String[] strings = new String[nbt.getInteger("i")];
        for (int i = 0; i < strings.length; i++) {
            strings[i] = nbt.getString(Integer.toString(i));
        }
        return strings;
    }

    public static boolean areBitsSet(int setBits, int testedValue) {
        return (testedValue & setBits) == setBits;
    }

    public static class ItemStack_NoNBT implements Comparable<ItemStack_NoNBT> {
        public final Item mItem;
        public final int mStackSize;
        public final int mMetaData;

        public ItemStack_NoNBT(Item aItem, long aStackSize, long aMetaData) {
            this.mItem = aItem;
            this.mStackSize = (byte) ((int) aStackSize);
            this.mMetaData = (short) ((int) aMetaData);
        }

        public ItemStack_NoNBT(ItemStack aStack) {
            if (aStack == null) {
                mItem = null;
                mStackSize = mMetaData = 0;
            } else {
                mItem = aStack.getItem();
                mStackSize = aStack.stackSize;
                mMetaData = Items.feather.getDamage(aStack);
            }
        }

        @Override
        public int compareTo(ItemStack_NoNBT o) {
            if (mMetaData > o.mMetaData) return 1;
            if (mMetaData < o.mMetaData) return -1;
            if (mStackSize > o.mStackSize) return 1;
            if (mStackSize < o.mStackSize) return -1;
            if (mItem != null && o.mItem != null)
                return mItem.getUnlocalizedName().compareTo(o.mItem.getUnlocalizedName());
            if (mItem == null && o.mItem == null) return 0;
            if (mItem != null) return 1;
            return -1;
        }

        @Override
        public boolean equals(Object aStack) {
            return aStack == this ||
                    (aStack instanceof ItemStack_NoNBT &&
                            ((mItem == ((ItemStack_NoNBT) aStack).mItem) || ((ItemStack_NoNBT) aStack).mItem.getUnlocalizedName().equals(this.mItem.getUnlocalizedName())) &&
                            ((ItemStack_NoNBT) aStack).mStackSize == this.mStackSize &&
                            ((ItemStack_NoNBT) aStack).mMetaData == this.mMetaData);
        }

        @Override
        public int hashCode() {
            return (mItem != null ? mItem.getUnlocalizedName().hashCode() : 0) ^ (mMetaData << 16) ^ (mStackSize << 24);
        }

        @Override
        public String toString() {
            return Integer.toString(hashCode()) + ' ' + (mItem == null ? "null" : mItem.getUnlocalizedName()) + ' ' + mMetaData + ' ' + mStackSize;
        }
    }

    public static void setTier(int tier,Object me){
        try{
            Field field=GT_MetaTileEntity_TieredMachineBlock.class.getField("mTier");
            field.setAccessible(true);
            field.set(me,(byte)tier);
        }catch (Exception e){
            //e.printStackTrace();
        }
    }

    public static StringBuilder receiveString(StringBuilder previousValue, int startIndex, int index, int value){
        int sizeReq=index-startIndex;
        if(value==0){
            previousValue.setLength(Math.min(previousValue.length(),sizeReq));
        }else {
            previousValue.setLength(Math.max(previousValue.length(),sizeReq));
            previousValue.setCharAt(sizeReq,(char)value);
        }
        return previousValue;
    }

    @Deprecated
    public static double receiveDouble(double previousValue, int startIndex, int index, int value){
        return Double.longBitsToDouble(receiveLong(Double.doubleToLongBits(previousValue),startIndex,index,value));
    }

    public static long receiveLong(long previousValue, int startIndex, int index, int value){
        value &=0xFFFF;
        switch (index-startIndex){
            case 0:
                previousValue&= 0xFFFF_FFFF_FFFF_0000L;
                previousValue|=value;
                break;
            case 1:
                previousValue&=0xFFFF_FFFF_0000_FFFFL;
                previousValue|=value<<16;
                break;
            case 2:
                previousValue&=0xFFFF_0000_FFFF_FFFFL;
                previousValue|=(long)value<<32;
                break;
            case 3:
                previousValue&=0x0000_FFFF_FFFF_FFFFL;
                previousValue|=(long)value<<48;
                break;
        }
        return previousValue;
    }

    public static void sendString(StringBuilder string,Container container, ICrafting crafter,int startIndex){
        for (int i = 0; i < string.length(); i++) {
            crafter.sendProgressBarUpdate(container,startIndex++,string.charAt(i));
        }
        crafter.sendProgressBarUpdate(container,startIndex,0);
    }

    public static void sendDouble(double value,Container container, ICrafting crafter,int startIndex){
        sendLong(Double.doubleToLongBits(value),container,crafter,startIndex);
    }

    public static void sendLong(long value,Container container, ICrafting crafter,int startIndex){
        crafter.sendProgressBarUpdate(container, startIndex++, (int)(value & 0xFFFFL));
        crafter.sendProgressBarUpdate(container, startIndex++, (int)((value & 0xFFFF0000L)>>>16));
        crafter.sendProgressBarUpdate(container, startIndex++, (int)((value & 0xFFFF00000000L)>>>32));
        crafter.sendProgressBarUpdate(container, startIndex,   (int)((value & 0xFFFF000000000000L)>>>48));
    }

    @Deprecated
    public static float receiveFloat(float previousValue, int startIndex, int index, int value){
        return Float.intBitsToFloat(receiveInteger(Float.floatToIntBits(previousValue),startIndex,index,value));
    }

    public static int receiveInteger(int previousValue, int startIndex, int index, int value){
        value &=0xFFFF;
        switch (index-startIndex){
            case 0:
                previousValue&= 0xFFFF_0000;
                previousValue|=value;
                break;
            case 1:
                previousValue&=0x0000_FFFF;
                previousValue|=value<<16;
                break;
        }
        return previousValue;
    }

    public static void sendFloat(float value,Container container, ICrafting crafter,int startIndex){
        sendInteger(Float.floatToIntBits(value),container,crafter,startIndex);
    }

    public static void sendInteger(int value,Container container, ICrafting crafter,int startIndex){
        crafter.sendProgressBarUpdate(container, startIndex++, (int)(value & 0xFFFFL));
        crafter.sendProgressBarUpdate(container, startIndex, (value & 0xFFFF0000)>>>16);
    }

    public static String doubleToString(double value){
        if(value==(long)value){
            return Long.toString((long)value);
        }
        return Double.toString(value);
    }

    public static boolean checkChunkExist(World world, ChunkCoordIntPair chunk){
        int x=chunk.getCenterXPos();
        int z=chunk.getCenterZPosition();
        return world.checkChunksExist(x, 0, z, x, 0, z);
    }

    public static NBTTagCompound getPlayerData(UUID uuid1,UUID uuid2,String extension) {
        try {
            if (FMLCommonHandler.instance().getEffectiveSide().isServer()) {
                if (uuid1 != null && uuid2!=null) {
                    IPlayerFileData playerNBTManagerObj = MinecraftServer.getServer().worldServerForDimension(0).getSaveHandler().getSaveHandler();
                    SaveHandler sh = (SaveHandler)playerNBTManagerObj;
                    File dir = ObfuscationReflectionHelper.getPrivateValue(SaveHandler.class, sh, new String[]{"playersDirectory", "field_75771_c"});
                    String id1=uuid1.toString();
                    NBTTagCompound tagCompound=read(new File(dir, id1 + "."+extension));
                    if(tagCompound!=null){
                        return tagCompound;
                    }
                    tagCompound=readBackup(new File(dir, id1 + "."+extension+"_bak"));
                    if(tagCompound!=null){
                        return tagCompound;
                    }
                    String id2=uuid2.toString();
                    tagCompound=read(new File(dir, id2 + "."+extension));
                    if(tagCompound!=null){
                        return tagCompound;
                    }
                    tagCompound=readBackup(new File(dir, id2 + "."+extension+"_bak"));
                    if(tagCompound!=null){
                        return tagCompound;
                    }
                }
            }
        } catch (Exception ignored) {}
        return new NBTTagCompound();
    }

    public static void savePlayerFile(EntityPlayer player,String extension, NBTTagCompound data) {
        try {
            if (FMLCommonHandler.instance().getEffectiveSide().isServer()) {
                if (player != null) {
                    IPlayerFileData playerNBTManagerObj = MinecraftServer.getServer().worldServerForDimension(0).getSaveHandler().getSaveHandler();
                    SaveHandler sh = (SaveHandler)playerNBTManagerObj;
                    File dir = ObfuscationReflectionHelper.getPrivateValue(SaveHandler.class, sh, new String[]{"playersDirectory", "field_75771_c"});
                    String id1=player.getUniqueID().toString();
                    write(new File(dir, id1 + "."+extension),data);
                    write(new File(dir, id1 + "."+extension+"_bak"),data);
                    String id2=UUID.nameUUIDFromBytes(player.getCommandSenderName().getBytes(StandardCharsets.UTF_8)).toString();
                    write(new File(dir, id2 + "."+extension),data);
                    write(new File(dir, id2 + "."+extension+"_bak"),data);
                }
            }
        } catch (Exception ignored) {}
    }

    private static NBTTagCompound read(File file){
        if (file != null && file.exists()) {
            try(FileInputStream fileInputStream= new FileInputStream(file)) {
                return CompressedStreamTools.readCompressed(fileInputStream);
            } catch (Exception var9) {
                TecTech.LOGGER.error("Cannot read NBT File: "+file.getAbsolutePath());
            }
        }
        return null;
    }

    private static NBTTagCompound readBackup(File file){
        if (file != null && file.exists()) {
            try(FileInputStream fileInputStream= new FileInputStream(file)) {
                return CompressedStreamTools.readCompressed(fileInputStream);
            } catch (Exception var9) {
                TecTech.LOGGER.error("Cannot read NBT File: "+file.getAbsolutePath());
                return new NBTTagCompound();
            }
        }
        return null;
    }

    private static void write(File file,NBTTagCompound tagCompound){
        if (file != null) {
            if(tagCompound==null){
                if(file.exists()) file.delete();
            }else {
                try(FileOutputStream fileOutputStream= new FileOutputStream(file)) {
                    CompressedStreamTools.writeCompressed(tagCompound,fileOutputStream);
                } catch (Exception var9) {
                    TecTech.LOGGER.error("Cannot write NBT File: "+file.getAbsolutePath());
                }
            }
        }
    }

    public static AxisAlignedBB fromChunkCoordIntPair(ChunkCoordIntPair chunkCoordIntPair){
        int x=chunkCoordIntPair.chunkXPos<<4;
        int z=chunkCoordIntPair.chunkZPos<<4;
        return AxisAlignedBB.getBoundingBox(x,-128,z,x+16,512,z+16);
    }

    public static AxisAlignedBB fromChunk(Chunk chunk){
        int x=chunk.xPosition<<4;
        int z=chunk.zPosition<<4;
        return AxisAlignedBB.getBoundingBox(x,-128,z,x+16,512,z+16);
    }
}
