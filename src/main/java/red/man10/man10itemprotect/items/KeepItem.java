package red.man10.man10itemprotect.items;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class KeepItem {

    // ユーザー名
    private String playerName;

    // 固有ID
    private final UUID uuid;

    // アイテム
    private ItemStack item;

    public KeepItem(UUID uuid,String name,ItemStack item){
        this.uuid = uuid;
        this.playerName = name;
        this.item = item;
    }

    public String getName() {
        return playerName;
    }

    public UUID getUuid() {
        return uuid;
    }

    public ItemStack getItem() {
        return item;
    }
}
