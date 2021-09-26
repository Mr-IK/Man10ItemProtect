package red.man10.man10itemprotect.items;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import red.man10.man10itemprotect.Man10ItemProtect;
import red.man10.man10itemprotect.util.ItemStringConverter;
import red.man10.man10itemprotect.util.sql.Query;
import red.man10.man10itemprotect.util.sql.SQLManager;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ItemKeeper {

    private Man10ItemProtect plugin;
    private SQLManager sql;

    public NamespacedKey key;

    public ItemKeeper(Man10ItemProtect plugin) {
        this.plugin = plugin;
        sql = new SQLManager(plugin,"MItemProtect");
        key = new NamespacedKey(plugin, "man10-item-protect");

        //  起動後1分で初回実行され以後30分ごとに自動実行
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::clearOverDayData,20*60,20*60*30);
    }

    // アイテムを保護
    public ItemStack protectedItem(UUID uuid,ItemStack item){
        ItemStack itemM = item.clone();
        ItemMeta meta = itemM.getItemMeta();
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, uuid.toString());
        itemM.setItemMeta(meta);
        return itemM;
    }

    // このアイテムは保護されているか？
    public boolean isProtectedItem(ItemStack item){
        if(item==null){
            return false;
        }
        if(!item.hasItemMeta()){
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(key , PersistentDataType.STRING);
    }

    // このアイテムに保存されたデータは？
    public UUID getProtectedData(ItemStack item){
        if(item==null){
            return null;
        }
        if(!item.hasItemMeta()){
            return null;
        }
        if(!isProtectedItem(item)){
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        return UUID.fromString(meta.getPersistentDataContainer().get(key , PersistentDataType.STRING));
    }

    // アイテムの保護解除
    public ItemStack unProtectedItem(ItemStack item){
        ItemStack itemM = item.clone();
        ItemMeta meta = itemM.getItemMeta();
        meta.getPersistentDataContainer().remove(key);
        itemM.setItemMeta(meta);
        return itemM;
    }

    // 定期的に実行される期限外データの消去
    public void clearOverDayData(){
        Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{
            sql.execute("DELETE FROM item_keeper WHERE (time < DATE_SUB(CURDATE(), INTERVAL "+plugin.boxDay+" DAY));");
        });
    }

    // アイテムデータを作成
    public void addItemData(UUID uuid, ItemStack item){
        Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{
            sql.execute("INSERT INTO item_keeper (name,uuid,item) " +
                    "VALUES ('"+getNameFromUUID(uuid)+"','"+uuid.toString()+"','"+ItemStringConverter.itemToBase64(item)+"');");
        });
    }

    // アイテムデータを削除
    public void deleteItemData(UUID uuid){
        Bukkit.getScheduler().runTaskAsynchronously(plugin,()-> {
            sql.execute("DELETE FROM item_keeper WHERE uuid = '"+uuid.toString()+"';");
        });
    }


    // アイテムデータを取得
    public List<KeepItem> getItemData(UUID uuid){
        List<KeepItem> items = new ArrayList<>();
        String exe = "SELECT * FROM item_keeper WHERE uuid = '"+uuid.toString()+"';";
        Query qu = sql.query(exe);
        if(!sql.checkQuery(qu)){
            return items;
        }
        ResultSet rs = qu.getResultSet();
        try {
            while(rs.next()) {
                KeepItem ki = new KeepItem(uuid,rs.getString("name"), ItemStringConverter.itemFromBase64(rs.getString("item")));
                items.add(ki);
            }
            qu.close();
            return items;
        } catch (SQLException e) {
            e.printStackTrace();
            qu.close();
            return items;
        }
    }

    // アイテムデータは存在するか
    public boolean existsItemData(UUID uuid){

        String exe = "SELECT * FROM item_keeper WHERE uuid = '"+uuid.toString()+"';";
        Query qu = sql.query(exe);
        if(!sql.checkQuery(qu)){
            return false;
        }
        ResultSet rs = qu.getResultSet();
        try {
            if(rs.next()) {
                qu.close();
                return true;
            }
            qu.close();
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            qu.close();
            return false;
        }
    }




    public String getNameFromUUID(UUID playerID){
        return Bukkit.getOfflinePlayer(playerID).getName();
    }

}
