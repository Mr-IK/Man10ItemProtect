package red.man10.man10itemprotect.util;


import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;

public class ItemStringConverter {

    public static ItemStack itemFromBase64(String data) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack[] items = new ItemStack[dataInput.readInt()];

            // Read the serialized inventory
            for (int i = 0; i < items.length; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }

            dataInput.close();
            return items[0];
        } catch (Exception e) {
            return null;
        }
    }

    public static String itemToBase64(ItemStack item) throws IllegalStateException {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            ItemStack[] items = new ItemStack[1];
            items[0] = item;
            dataOutput.writeInt(items.length);

            for (int i = 0; i < items.length; i++) {
                dataOutput.writeObject(items[i]);
            }

            dataOutput.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save item stacks.", e);
        }
    }



    //文字列をアイテムへ変換
    public static ItemStack stringToItem(String dataSt){
        String[] datas = dataSt.split("@@");

        Material mt = Material.getMaterial(datas[0]);
        int amount = 1;
        int damage = 0;

        try{
            amount = Integer.parseInt(datas[1]);
            damage = Integer.parseInt(datas[2]);
        }catch (NumberFormatException e){

        }

        Map<Enchantment, Integer> enchants = new HashMap<>();

        if(datas.length>=4&&!datas[3].equalsIgnoreCase("")){
            String[] datass = datas[3].split("&&");
            boolean isValue = false;
            Enchantment cash = null;
            for(String data : datass){
                if(!isValue){
                    cash = Enchantment.getByKey(NamespacedKey.minecraft(data));
                    isValue = true;
                }else{
                    int value = Integer.parseInt(data);
                    enchants.put(cash,value);
                    isValue = false;
                    cash = null;
                }
            }
        }

        String itName = "";

        if(datas.length>=5&&!datas[4].equalsIgnoreCase("")){
            itName = datas[4].replaceAll("_@_","@").replaceAll("_&_","&");
        }

        List<String> lore = new ArrayList<>();

        if(datas.length>=6&&!datas[5].equalsIgnoreCase("")){
            String[] datass = datas[5].split("&&");
            for(String data : datass){
                lore.add(data.replaceAll("_@_","@").replaceAll("_&_","&"));
            }
        }

        Set<ItemFlag> flags = new HashSet<>();

        if(datas.length>=7&&!datas[6].equalsIgnoreCase("")){
            String[] datass = datas[6].split("&&");
            for(String data : datass){
                flags.add(ItemFlag.valueOf(data));
            }
        }

        PotionType ptype = null;

        List<PotionEffect> potions = new ArrayList<>();
        if(datas.length>=9&&!datas[7].equalsIgnoreCase("")){
            ptype = PotionType.valueOf(datas[7]);
            if(!datas[8].equalsIgnoreCase("")) {
                String[] datass = datas[8].split("&&");
                for (String data : datass) {
                    String[] datasss = data.split("--");
                    potions.add(PotionEffectType.getByName(datasss[0]).createEffect(Integer.parseInt(datasss[1]), Integer.parseInt(datasss[2])));
                }
            }
        }

        if(mt==null){
            return null;
        }

        ItemStack item = new ItemStack(mt,amount);
        ItemMeta meta = item.getItemMeta();
        if(meta==null){
            return null;
        }
        if (meta instanceof Damageable) {
            ((Damageable) meta).setDamage(damage);
        }
        for(Enchantment en : enchants.keySet()){
            int data = enchants.get(en);
            meta.addEnchant(en,data,false);
        }
        if(!itName.equalsIgnoreCase("")){
            meta.setDisplayName(itName);
        }
        meta.setLore(lore);
        for(ItemFlag flag : flags){
            meta.addItemFlags(flag);
        }
        if(meta instanceof PotionMeta&&ptype!=null){
            ((PotionMeta) meta).setBasePotionData(new PotionData(ptype));
            for(PotionEffect effect : potions){
                ((PotionMeta) meta).addCustomEffect(effect,true);
            }
        }
        item.setItemMeta(meta);
        return item;
    }

    //アイテムを文字列へ変換
    public static String itemToString(ItemStack item) {
        String mtName = "";
        int amount = 0;
        int damage = 0;
        Map<Enchantment, Integer> enchants = new HashMap<>();
        String itName = "";
        List<String> lore = null;
        Set<ItemFlag> flags = null;
        PotionType ptype = null;
        List<PotionEffect> potions = null;


        ItemMeta meta = item.getItemMeta();

        Material mt = item.getType();
        mtName = mt.name();
        amount = item.getAmount();

        if(item.hasItemMeta()){

            if (meta instanceof Damageable) {
                damage = ((Damageable) meta).getDamage();
            }

            if(meta.hasEnchants()){
                enchants =  meta.getEnchants();
            }

            if(meta.hasDisplayName()){
                itName = meta.getDisplayName();
            }

            if(meta.hasLore()){
                lore = meta.getLore();
            }

            if(meta instanceof PotionMeta){
                ptype = ((PotionMeta) meta).getBasePotionData().getType();
                potions = ((PotionMeta) meta).getCustomEffects();
            }

            flags = meta.getItemFlags();

        }


        StringBuilder resultSt = new StringBuilder(mtName + "@@" + amount + "@@" + damage + "@@");

        boolean first1 = true;
        for(Enchantment en : enchants.keySet()){
            if(!first1){
                resultSt.append("&&");
            }else{
                first1 = false;
            }
            int data = enchants.get(en);
            //namespacekey
            resultSt.append(en.getKey().getKey()).append("&&").append(data);
        }

        resultSt.append("@@").append(itName.replaceAll("&","_&_").replaceAll("@","_@_"))
                .append("@@");

        if(lore!=null){
            boolean first2 = true;
            for(String lor : lore){
                if(!first2){
                    resultSt.append("&&");
                }else{
                    first2 = false;
                }
                resultSt.append(lor.replaceAll("&","_&_").replaceAll("@","_@_"));
            }
        }

        resultSt.append("@@");


        if(flags!=null){
            boolean first3 = true;
            for(ItemFlag flag : flags){
                if(!first3){
                    resultSt.append("&&");
                }else{
                    first3 = false;
                }
                resultSt.append(flag.name());
            }
        }

        resultSt.append("@@");

        if(ptype!=null){

            resultSt.append(ptype.name());

            resultSt.append("@@");

            boolean first4 = true;
            for(PotionEffect ef : potions){
                if(!first4){
                    resultSt.append("&&");
                }else{
                    first4 = false;
                }
                resultSt.append(ef.getType().getName()).append("--").append(ef.getAmplifier()).append("--").append(ef.getDuration());
            }
        }
        resultSt.append("@@");
        resultSt.append("ENDDATA");
        return resultSt.toString();
    }
}
