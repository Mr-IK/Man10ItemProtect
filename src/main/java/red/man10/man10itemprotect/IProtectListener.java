package red.man10.man10itemprotect;

import me.minebuilders.clearlag.events.EntityRemoveEvent;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class IProtectListener implements Listener {

    private Man10ItemProtect plugin;

    public IProtectListener(Man10ItemProtect plugin){
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event){
        if(event.getWhoClicked().hasPermission("man10.itemprotect.op")){
            return;
        }
        if(event.getCurrentItem()!=null){
            ItemStack item = event.getCurrentItem();
            if(item==null){
                return;
            }
            if(!plugin.iKeeper.isProtectedItem(item)){
                return;
            }
            if(!plugin.iKeeper.getProtectedData(item).equals(event.getWhoClicked().getUniqueId())){
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onIntract(PlayerInteractEvent event){
        if(event.getAction()== Action.RIGHT_CLICK_BLOCK){

            Block block = event.getClickedBlock();
            if(block==null){
                return;
            }
            if(block.getState() instanceof ShulkerBox){
                ShulkerBox shulkerBox = (ShulkerBox) block.getState();
                if(!shulkerBox.getPersistentDataContainer().has(plugin.iKeeper.key,PersistentDataType.STRING)){
                    return;
                }
                UUID uuid = UUID.fromString(shulkerBox.getPersistentDataContainer().get(plugin.iKeeper.key, PersistentDataType.STRING));
                if(!event.getPlayer().getUniqueId().equals(uuid)){
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlace(BlockPlaceEvent event){
        if(event.isCancelled()){
            return;
        }
        if(!plugin.iKeeper.isProtectedItem(event.getItemInHand())){
            return;
        }
        UUID uuid = plugin.iKeeper.getProtectedData(event.getItemInHand());
        Block block = event.getBlockPlaced();
        if(block.getState() instanceof ShulkerBox){
            ShulkerBox shulkerBox = (ShulkerBox) block.getState();
            shulkerBox.getPersistentDataContainer().set(plugin.iKeeper.key, PersistentDataType.STRING, uuid.toString());
            shulkerBox.update();
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event){
        Block block = event.getBlock();
        if(block.getState() instanceof ShulkerBox){
            ShulkerBox shulkerBox = (ShulkerBox) block.getState();
            if(!shulkerBox.getPersistentDataContainer().has(plugin.iKeeper.key,PersistentDataType.STRING)){
                return;
            }
            UUID uuid = UUID.fromString(shulkerBox.getPersistentDataContainer().get(plugin.iKeeper.key, PersistentDataType.STRING));
            if(!event.getPlayer().getUniqueId().equals(uuid)){
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBreak(BlockDropItemEvent event){
        if(event.isCancelled()){
            return;
        }
        if(event.getBlockState() instanceof ShulkerBox){
            ShulkerBox shulkerBox = (ShulkerBox) event.getBlockState();
            if(!shulkerBox.getPersistentDataContainer().has(plugin.iKeeper.key,PersistentDataType.STRING)){
                return;
            }
            UUID uuid = UUID.fromString(shulkerBox.getPersistentDataContainer().get(plugin.iKeeper.key, PersistentDataType.STRING));
            for(int i = 0;i<event.getItems().size();i++){
                Item item = event.getItems().get(i);
                ItemStack it = item.getItemStack().clone();
                ItemMeta im = it.getItemMeta();
                im.getPersistentDataContainer().set(plugin.iKeeper.key,PersistentDataType.STRING,uuid.toString());
                it.setItemMeta(im);
                event.getItems().get(i).setItemStack(it);
            }
        }
    }

    @EventHandler
    public void onItemDespawn(ItemDespawnEvent event){
        // 消去されたアイテムが保護アイテムならsqlへ突っ込む
        ItemStack item = event.getEntity().getItemStack();
        if(plugin.iKeeper.isProtectedItem(item)){
            Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{
                plugin.iKeeper.addItemData(plugin.iKeeper.getProtectedData(item),item);
            });
        }
    }

    @EventHandler
    public void onItemDespawn(EntityRemoveEvent event){

        for(Entity e: event.getEntityList()){

            if(!(e instanceof Item)){
                continue;
            }

            // 消去されたアイテムが保護アイテムならsqlへ突っ込む
            ItemStack item = ((Item)e).getItemStack();
            if(plugin.iKeeper.isProtectedItem(item)){
                Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{
                    plugin.iKeeper.addItemData(plugin.iKeeper.getProtectedData(item),item);
                });
            }
        }

    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent e) {

        if(e.getEntity() instanceof Item){
            Item entity = (Item)e.getEntity();
            ItemStack item = entity.getItemStack();
            if(plugin.iKeeper.isProtectedItem(item)){
                Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{
                    plugin.iKeeper.addItemData(plugin.iKeeper.getProtectedData(item),item);
                });
                e.getEntity().remove();
            }
        }
    }

    @EventHandler
    public void onPlayerPickup(EntityPickupItemEvent event){
        // 消去されたアイテムが保護アイテムならsqlへ突っ込む
        ItemStack item = event.getItem().getItemStack();
        if(plugin.iKeeper.isProtectedItem(item)){
            if (!(event.getEntity() instanceof Player)) {
                event.setCancelled(true);
                return;
            }

            Player p = (Player) event.getEntity();
            if(p.hasPermission("man10.itemprotect.op")){
                return;
            }
            if(!plugin.iKeeper.getProtectedData(item).equals(p.getUniqueId())){
                event.setCancelled(true);
            }

        }
    }


}
