package red.man10.man10itemprotect;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import red.man10.man10itemprotect.items.ItemKeeper;
import red.man10.man10itemprotect.util.VaultManager;

public final class Man10ItemProtect extends JavaPlugin {

    public final String prefix = "§f§l[§d§lアイ§f§lテ§a§lム§b§l保険§f§l]§r";

    public ItemKeeper iKeeper;
    public FileConfiguration config;
    public VaultManager vault;

    // 手数料
    public int fee = 10000;
    // 保存期間(日数)
    public int boxDay = 3;


    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();
        config = getConfig();
        fee = config.getInt("fee",10000);
        boxDay = config.getInt("boxday",3);
        vault = new VaultManager(this);

        iKeeper = new ItemKeeper(this);
        getCommand("mip").setExecutor(new IProtectCommand(this));
        Bukkit.getPluginManager().registerEvents(new IProtectListener(this),this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    ////////////////////////////////////////////////////////////////////////////////////////////
    //  マインクラフトチャットに、ホバーテキストや、クリックコマンドを設定する関数
    // [例1] sendHoverText(player,"ここをクリック",null,"/say おはまん");
    // [例2] sendHoverText(player,"カーソルをあわせて","ヘルプメッセージとか",null);
    // [例3] sendHoverText(player,"カーソルをあわせてクリック","ヘルプメッセージとか","/say おはまん");
    public void sendHoverText(Player p, String text, String hoverText, String command){
        //////////////////////////////////////////
        //      ホバーテキストとイベントを作成する
        HoverEvent hoverEvent = null;
        if(hoverText != null){
            BaseComponent[] hover = new ComponentBuilder(hoverText).create();
            hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover);
        }

        //////////////////////////////////////////
        //   クリックイベントを作成する
        ClickEvent clickEvent = null;
        if(command != null){
            clickEvent = new ClickEvent(ClickEvent.Action.RUN_COMMAND,command);
        }

        BaseComponent[] message = new ComponentBuilder(text).event(hoverEvent).event(clickEvent). create();
        p.spigot().sendMessage(message);
    }
}
