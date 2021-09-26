package red.man10.man10itemprotect;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import red.man10.man10itemprotect.items.KeepItem;
import red.man10.man10itemprotect.util.JPYFormat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class IProtectCommand implements CommandExecutor {

    private Man10ItemProtect plugin;
    private HashMap<UUID, Consumer<Player>> confirm = new HashMap<>();
    private List<UUID> confirmCoolDown = new ArrayList<>();

    public IProtectCommand(Man10ItemProtect plugin){
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            //CONSOLE向けコマンド
            return true;
        }
        Player p = (Player) sender;
        if(!p.hasPermission("man10.itemprotect.use")){
            p.sendMessage("Unknown command. Type \"/help\" for help.");
            return true;
        }

        switch (args.length) {
            case 0: {
                showHelp(p);
                return true;
            }

            case 1:{

                if(args[0].equalsIgnoreCase("help")){
                    showHelp(p);
                    return true;
                }

                if(args[0].equalsIgnoreCase("confirm")){
                    if(confirm.containsKey(p.getUniqueId())){
                        acceptConfirm(p);
                        return true;
                    }
                }


                if(args[0].equalsIgnoreCase("check")){

                    if(p.getInventory().getItemInMainHand().getType()== Material.AIR){
                        p.sendMessage(plugin.prefix + "§cアイテムを持って実行してください！");
                        return true;
                    }

                    ItemStack item = p.getInventory().getItemInMainHand();

                    if(plugin.iKeeper.isProtectedItem(item)){
                        p.sendMessage(plugin.prefix + "§b§lこのアイテムは保護されています！");
                    }else{
                        p.sendMessage(plugin.prefix + "§4§lこのアイテムは保護されていません！");
                    }

                    return true;
                }

                if(args[0].equalsIgnoreCase("protect")){

                    if(p.getInventory().getItemInMainHand().getType()== Material.AIR){
                        p.sendMessage(plugin.prefix + "§cアイテムを持って実行してください！");
                        return true;
                    }

                    ItemStack item = p.getInventory().getItemInMainHand();

                    if(plugin.iKeeper.isProtectedItem(item)){
                        p.sendMessage(plugin.prefix + "§b§lこのアイテムは既に保護されています！");
                        return true;
                    }

                    if(plugin.vault.getBalance(p.getUniqueId())<plugin.fee){
                        p.sendMessage(plugin.prefix+"§c§l所持金が足りません！(必要: §e§l"+ JPYFormat.getText(plugin.fee)+"円§c§l)");
                        return true;
                    }

                    if(confirmCoolDown.contains(p.getUniqueId())){
                        p.sendMessage(plugin.prefix + "§c§l現在クールダウン中です");
                        return true;
                    }

                    p.sendMessage(plugin.prefix + "§a§l最終確認です！保護したいアイテムを持って確認をお願いします！");
                    p.sendMessage(plugin.prefix + "§c§lキャンセルする場合は10秒の間無視をお願いします！");
                    plugin.sendHoverText(p,"§b§l[アイテムを保護する！(ここをクリック)] §f§l/mip confirm","クリックで確認！","/mip confirm");

                    pushConfirm(p,player -> {
                        // お金を引き出す！！
                        plugin.vault.withdraw(p,plugin.fee);

                        // アイテム保護！！
                        ItemStack protectI = plugin.iKeeper.protectedItem(p.getUniqueId(),item);

                        p.getInventory().setItemInMainHand(protectI);

                        p.sendMessage(plugin.prefix + "§a§lアイテムを保護しました！");
                    },player -> {
                        p.sendMessage(plugin.prefix + "§c§lアイテム保護はキャンセルされました");
                    });
                    return true;
                }

                if(args[0].equalsIgnoreCase("unprotect")){

                    if(p.getInventory().getItemInMainHand().getType()== Material.AIR){
                        p.sendMessage(plugin.prefix + "§cアイテムを持って実行してください！");
                        return true;
                    }

                    ItemStack item = p.getInventory().getItemInMainHand();

                    if(!plugin.iKeeper.isProtectedItem(item)){
                        p.sendMessage(plugin.prefix + "§b§lこのアイテムは既に保護されていません！");
                        return true;
                    }

                    if(confirmCoolDown.contains(p.getUniqueId())){
                        p.sendMessage(plugin.prefix + "§c§l現在クールダウン中です");
                        return true;
                    }

                    p.sendMessage(plugin.prefix + "§a§l最終確認です！保護解除したいアイテムを持って確認をお願いします！");
                    p.sendMessage(plugin.prefix + "§c§lキャンセルする場合は10秒の間無視をお願いします！");
                    plugin.sendHoverText(p,"§b§l[保護を解除する！(ここをクリック)] §f§l/mip confirm","クリックで確認！","/mip confirm");

                    pushConfirm(p,player -> {
                        // アイテム保護解除！！
                        ItemStack protectI = plugin.iKeeper.unProtectedItem(item);

                        p.getInventory().setItemInMainHand(protectI);

                        p.sendMessage(plugin.prefix + "§a§lアイテムの保護を解除しました。");
                    },player -> {
                        p.sendMessage(plugin.prefix + "§c§l保護解除はキャンセルされました");
                    });
                    return true;
                }

                if(args[0].equalsIgnoreCase("box")){

                    if(confirmCoolDown.contains(p.getUniqueId())){
                        p.sendMessage(plugin.prefix + "§c§l現在クールダウン中です");
                        return true;
                    }

                    Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{
                        if(!plugin.iKeeper.existsItemData(p.getUniqueId())){
                            p.sendMessage(plugin.prefix + "§c§l受け取れるアイテムはありませんでした");
                            return;
                        }

                        p.sendMessage(plugin.prefix + "§a§l最終確認です！インベントリやエンダーチェストに空きはありますか？");
                        p.sendMessage(plugin.prefix + "§c§lキャンセルする場合は10秒の間無視をお願いします！");
                        plugin.sendHoverText(p,"§b§l[アイテムを受け取る！(ここをクリック)] §f§l/mip confirm","クリックで確認！","/mip confirm");

                        pushConfirm(p,player -> {
                            // アイテム保護解除！！
                            Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{
                                List<KeepItem> items = plugin.iKeeper.getItemData(p.getUniqueId());
                                // ここワンチャン通常スレッド[説]
                                for(KeepItem item : items){
                                    safeItemGive(p,item.getItem());
                                }

                                // アイテムデータ削除
                                plugin.iKeeper.deleteItemData(p.getUniqueId());
                                p.sendMessage(plugin.prefix + "§a§lアイテムを受け取りました！");
                            });
                        },player -> {
                            p.sendMessage(plugin.prefix + "§c§lアイテム受け取りはキャンセルされました");
                        });
                    });

                    return true;
                }

                // 運営用
                if(args[0].equalsIgnoreCase("mclear")){

                    if(!p.hasPermission("man10.itemprotect.op")){
                        p.sendMessage(plugin.prefix + "§c/mip help で正しいコマンドを確認してください！");
                        return true;
                    }

                    plugin.iKeeper.clearOverDayData();
                    p.sendMessage(plugin.prefix + "§a§l期限切れのデータを削除しました。");
                    return true;
                }

                break;
            }
            case 3:{

                // 運営用
                if(args[0].equalsIgnoreCase("setfee")){

                    if(!p.hasPermission("man10.itemprotect.op")){
                        p.sendMessage(plugin.prefix + "§c/mip help で正しいコマンドを確認してください！");
                        return true;
                    }

                    int fee;

                    try {

                        fee = Integer.parseInt(args[1]);

                    } catch (NumberFormatException mc) {
                        p.sendMessage(plugin.prefix + "§c§l金額は数字で指定してください。");
                        return true;
                    }

                    plugin.fee = fee;
                    plugin.getConfig().set("fee",fee);
                    plugin.saveConfig();
                    p.sendMessage(plugin.prefix + "§a§l手数料を設定しました。");
                    return true;
                }

                // 運営用
                if(args[0].equalsIgnoreCase("setboxday")){

                    if(!p.hasPermission("man10.itemprotect.op")){
                        p.sendMessage(plugin.prefix + "§c/mip help で正しいコマンドを確認してください！");
                        return true;
                    }

                    int day;

                    try {

                        day = Integer.parseInt(args[1]);

                    } catch (NumberFormatException mc) {
                        p.sendMessage(plugin.prefix + "§c§l金額は数字で指定してください。");
                        return true;
                    }

                    plugin.boxDay = day;
                    plugin.getConfig().set("boxday",day);
                    plugin.saveConfig();
                    p.sendMessage(plugin.prefix + "§a§l保管期間を設定しました。");
                    return true;
                }

                break;
            }

        }
        p.sendMessage(plugin.prefix + "§c/mip help で正しいコマンドを確認してください！");
        return true;
    }

    // 10秒以内に確認をお願いします！という処理用
    public void pushConfirm(Player p, Consumer<Player> confirm,Consumer<Player> cancel){
        if(this.confirm.containsKey(p.getUniqueId())){
            return;
        }
        confirmCoolDown.add(p.getUniqueId());
        this.confirm.put(p.getUniqueId(),confirm);
        Bukkit.getScheduler().runTaskLater(plugin,()->{
            confirmCoolDown.remove(p.getUniqueId());
            if(this.confirm.containsKey(p.getUniqueId())){
                cancel.accept(p);
                this.confirm.remove(p.getUniqueId());
            }
        },20*10);
    }

    // 確認します！という処理
    public void acceptConfirm(Player p){
        if(!this.confirm.containsKey(p.getUniqueId())){
            return;
        }
        this.confirm.get(p.getUniqueId()).accept(p);
        this.confirm.remove(p.getUniqueId());
    }

    public void showHelp(Player p){
        p.sendMessage("§e========" + plugin.prefix + "§e========");
        p.sendMessage("§e/mip check : 手に持ったアイテムが保険適用されているか表示します");
        p.sendMessage("§e/mip protect : 手に持ったアイテムを保険に適用します");
        p.sendMessage("§e/mip unprotect : アイテムの保険を解除します");
        p.sendMessage("§e/mip box : 保険によって戻ったアイテムを受け取ります");
        p.sendMessage("§6現在の手数料: §e"+ JPYFormat.getText(plugin.fee)+"円");
        if(p.hasPermission("man10.itemprotect.op")){
            p.sendMessage("");
            p.sendMessage("§c========" + plugin.prefix + "§c========");
            p.sendMessage("§c/mip setfee <金額> : 手数料の値段を設定します");
            p.sendMessage("§c/mip setboxday <時間> : 期限切れとなる時間を設定します");
            // mclearはManualClearの略です。さすがに長すぎるので省略
            p.sendMessage("§c/mip mclear : 期間外データを手動で削除します");
        }
    }


    public void safeItemGive(Player p,ItemStack item){
        if(p.getInventory().firstEmpty() == -1){
            //インベントリマンパン

            if(p.getEnderChest().firstEmpty() == -1){
                //エンチェスもマンパン
                p.getWorld().dropItem(p.getLocation(),item);
                return;
            }

            p.getEnderChest().addItem(item);
        }else{
            p.getInventory().addItem(item);
        }
    }
}
