package red.man10.man10itemprotect;

import me.minebuilders.clearlag.events.EntityRemoveEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

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
            if(!plugin.iKeeper.getProtectedData(item).equals(event.getWhoClicked().getUniqueId())){
                event.setCancelled(true);
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
