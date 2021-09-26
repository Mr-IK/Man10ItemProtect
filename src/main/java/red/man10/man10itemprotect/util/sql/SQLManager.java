package red.man10.man10itemprotect.util.sql;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.logging.Level;

/*
　※注意事項
　MySQLとSQLiteでは少し仕様が違うようです。そのあたりを考慮したコード構成にしてください。
　
　発見できた仕様違い例:
  My:WHEREで、TEXTを検索するとき大文字小文字を区別しない (Mr_IKで登録時、mr_ikで検索するとHITする
  Lite:WHEREで、TEXTを検索するとき大文字小文字を区別する (Mr_IKで登録時、mr_ikで検索するとHITしない
 */

/**
 * SQLManager V3.3
 *  ご利用の際はDBConnect Type_MySQL,Type_SQLiteを併用してください。
 *
 * Updated by Mr_IK on 2021/09/15.
 *
 * ChangeLog(更新履歴)
 * Ver 3.3(〃): execute・queryをスレッドで使用可能にするメソッド executeThread,queryThreadを追加
 * Ver 3.2(〃): Queryのエラーチェック用簡易メソッド checkQueryを追加
 * Ver 3.1(〃): SQLインジェクション対策用メソッド executeSafeとquerySafeを追加
 * Ver 3.0(SQM): SQLiteの使用に対応+コード整理
 * Ver 2.0(V2): 内部のアクセスシステムを一部再構築+バグ修正
 *
 * ↓ Created by takatronix ↓
 * Ver 1.0(Original): MySQLへのアクセスとコマンド実行、クエリ取得など基礎機能
 */

public class SQLManager {

    //両方で使用
    private final Boolean debugMode = false;
    private final JavaPlugin plugin;
    private final String conName;

    //SQLite用
    private boolean mode_sqlite = false;
    private String FILE_PATH = null;

    //mysql用
    private boolean connected = false;
    private String HOST = null;
    private String DB = null;
    private String USER = null;
    private String PASS = null;
    private String PORT = null;

    private final HashMap<Integer, DBConnect> connects;

    ////////////////////////////////
    //      コンストラクタ
    ////////////////////////////////
    public SQLManager(JavaPlugin plugin, String name) {
        this.connects = new HashMap<>();
        this.plugin = plugin;
        this.conName = name;
        this.connected = false;
        init();
    }

    public void init(){
        loadConfig();

        int result;

        if(mode_sqlite){
            result = Connect(FILE_PATH);
        }else{
            result = Connect(HOST,DB,USER,PASS,PORT);
        }

        if(result == -1){
            plugin.getLogger().warning("データベースにアクセスできませんでした。");
        }

        //テーブル作成はここ

        // フラグデータ
        execute("CREATE TABLE if not exists item_keeper(" +
                "name VARCHAR(18),"+ // プレイヤー名
                "uuid VARCHAR(40),"+ // UUID
                "item text, " + // アイテムデータ
                "time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP);");
    }


    /////////////////////////////////
    //       設定ファイル読み込み
    /////////////////////////////////
    public void loadConfig(){
        plugin.getLogger().info("データベース設定 ロード開始");
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        boolean sqlpartern = plugin.getConfig().getBoolean("db.sqlite");
        if(sqlpartern){
            plugin.getLogger().info("タイプ: SQLite");
            mode_sqlite = true;
            FILE_PATH = plugin.getDataFolder().getAbsolutePath()+ File.separator+"sqlite.db";
        }else{
            plugin.getLogger().info("タイプ: MySQL");
            HOST = plugin.getConfig().getString("db.mysql.host");
            USER = plugin.getConfig().getString("db.mysql.user");
            PASS = plugin.getConfig().getString("db.mysql.pass");
            PORT = plugin.getConfig().getString("db.mysql.port");
            DB = plugin.getConfig().getString("db.mysql.db");
        }
        plugin.getLogger().info("設定データをロードしました");
    }


    ////////////////////////////////
    //  MySQL用接続
    ////////////////////////////////
    public int Connect(String host, String db, String user, String pass,String port) {
        this.HOST = host;
        this.DB = db;
        this.USER = user;
        this.PASS = pass;
        this.PORT = port;
        int data = connects.size()+1;
        connects.put(connects.size()+1,new Type_MySQL(host,db,user,pass,port));
        if(connects.get(data).open() == null){
            Bukkit.getLogger().warning("MySQLのオープンに失敗しました");
            return -1;
        }
        this.plugin.getLogger().info("[" + this.conName + "] データベースへ接続しました。");
        return data;
    }

    ////////////////////////////////
    //  SQLite用接続
    ////////////////////////////////
    public int Connect(String file_path) {
        this.FILE_PATH = file_path;
        int data = connects.size()+1;
        connects.put(connects.size()+1,new Type_SQLite(FILE_PATH));
        if(connects.get(data).open() == null){
            Bukkit.getLogger().warning("SQLiteのオープンに失敗しました");
            return -1;
        }
        this.plugin.getLogger().info("[" + this.conName + "] データベースへ接続しました。");
        return data;
    }

    ////////////////////////////////
    //  汎用新規コネクション作成
    ////////////////////////////////
    public DBConnect createConnection(){
        if(mode_sqlite){
            return new Type_SQLite(FILE_PATH);
        }else{
            return new Type_MySQL(HOST,DB,USER,PASS,PORT);
        }
    }

    ////////////////////////////////
    //     行数を数える
    ////////////////////////////////
    public int countRows(String table) {
        int count = 0;
        ResultSet set = this.query(String.format("SELECT * FROM %s", table)).getResultSet();

        try {
            while(set.next()) {
                ++count;
            }
        } catch (SQLException var5) {
            Bukkit.getLogger().log(Level.SEVERE, "Could not select all rows from table: " + table + ", error: " + var5.getErrorCode());
        }

        return count;
    }

    ////////////////////////////////
    //     レコード数
    ////////////////////////////////
    public int count(String table) {
        int count = 0;
        ResultSet set = this.query(String.format("SELECT count(*) from %s", table)).getResultSet();

        try {
            count = set.getInt("count(*)");

        } catch (SQLException var5) {
            Bukkit.getLogger().log(Level.SEVERE, "Could not select all rows from table: " + table + ", error: " + var5.getErrorCode());
            return -1;
        }

        return count;
    }

    ////////////////////////////////
    //      実行
    ////////////////////////////////
    public boolean execute(String query) {
        int data = connects.size()+1;
        connects.put(connects.size()+1,createConnection());
        if(connects.get(data).open() == null){
            Bukkit.getLogger().warning("データベースへのアクセスに失敗しました。");
            return false;
        }
        boolean ret = true;
        if (debugMode){
            plugin.getLogger().info("query:" + query);
        }

        try {
            connects.get(data).getSt().execute(query);
        } catch (SQLException var3) {
            this.plugin.getLogger().warning("[" + this.conName + "] データベース 実行エラー: " +var3.getErrorCode() +":"+ var3.getLocalizedMessage());
            this.plugin.getLogger().info(query);
            ret = false;
        }

        connects.get(data).close();
        return ret;
    }

    ////////////////////////////////
    //      実行(セーフ)
    ////////////////////////////////
    public boolean executeSafe(String query,String[] rep) {
        int data = connects.size()+1;
        connects.put(connects.size()+1,createConnection());
        if(connects.get(data).open() == null){
            Bukkit.getLogger().warning("データベースへのアクセスに失敗しました。");
            return false;
        }
        boolean ret = true;
        if (debugMode){
            plugin.getLogger().info("query:" + query);
        }
        try {
            connects.get(data).changePrepareState(query);
            int i = 1;
            for(String re : rep){
                connects.get(data).getSafeSt().setString(i,re);
                i++;
            }
            connects.get(data).getSafeSt().execute();
        } catch (SQLException var3) {
            this.plugin.getLogger().warning("[" + this.conName + "] データベース 実行エラー: " +var3.getErrorCode() +":"+ var3.getLocalizedMessage());
            this.plugin.getLogger().info(query);
            ret = false;
        }

        connects.get(data).close();
        return ret;
    }

    ////////////////////////////////
    //      実行(スレッド)
    ////////////////////////////////
    public void executeThread(String query, Consumer<Boolean> after){
        Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{
            if(after!=null){
                after.accept(execute(query));
            }else{
                execute(query);
            }
        });
    }

    ////////////////////////////////
    //      クエリ
    ////////////////////////////////
    public Query query(String query) {
        int data = connects.size()+1;
        connects.put(connects.size()+1,createConnection());
        if(connects.get(data).open() == null){
            Bukkit.getLogger().warning("データベースへのアクセスに失敗しました。");
            return null;
        }
        ResultSet rs = null;
        if (debugMode){
            plugin.getLogger().info("query:" + query);
        }

        try {
            rs = connects.get(data).getSt().executeQuery(query);
        } catch (SQLException var4) {
            this.plugin.getLogger().warning("[" + this.conName + "] データベース クエリエラー: " + var4.getErrorCode());
            this.plugin.getLogger().info(query);
        }

        //query.close();
        return new Query(rs,connects.get(data));
    }

    ////////////////////////////////
    //      クエリ(セーフ)
    ////////////////////////////////
    public Query querysafe(String query,String[] rep) {
        int data = connects.size()+1;
        connects.put(connects.size()+1,createConnection());
        if(connects.get(data).open() == null){
            Bukkit.getLogger().warning("データベースへのアクセスに失敗しました。");
            return null;
        }
        ResultSet rs = null;
        if (debugMode){
            plugin.getLogger().info("query:" + query);
        }

        try {
            connects.get(data).changePrepareState(query);
            int i = 1;
            for(String re : rep){
                connects.get(data).getSafeSt().setString(i,re);
                i++;
            }
            rs = connects.get(data).getSafeSt().executeQuery();
        } catch (SQLException var4) {
            this.plugin.getLogger().warning("[" + this.conName + "] データベース クエリエラー: " + var4.getErrorCode());
            this.plugin.getLogger().info(query);
        }

        //query.close();
        return new Query(rs,connects.get(data));
    }

    ////////////////////////////////
    //      クエリ(スレッド)
    //      ※nullがあり得ますのでご了承ください
    ////////////////////////////////
    public void queryThread(String query, Consumer<Query> after){
        Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{
            if(after!=null){
                after.accept(query(query));
            }else{
                query(query);
            }

        });
    }

    public boolean checkQuery(Query q){
        ResultSet rs = q.getResultSet();
        return (rs != null);
    }

    public int notClosedConnectionCount(){
        int count = 0;
        for(DBConnect connect : connects.values()){
            if(!connect.isClosed()){
                count++;
            }
        }
        return count;
    }

    public void forceCloseAllConnection(){
        for(DBConnect connect : connects.values()){
            if(!connect.isClosed()){
                connect.close();
            }
        }
    }

}