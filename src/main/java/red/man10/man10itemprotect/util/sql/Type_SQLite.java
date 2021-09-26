package red.man10.man10itemprotect.util.sql;

import org.bukkit.Bukkit;

import java.io.File;
import java.sql.*;
import java.util.logging.Level;

public class Type_SQLite extends DBConnect {

    String FILE_PATH = null;

    private Connection con = null;
    private Statement st = null;
    private boolean closed = false;

    public Type_SQLite(String file_path) {
        this.FILE_PATH = file_path;
        File file = new File(file_path);
        if(!file.exists()){
            createDBFile();
        }
    }

    public void createDBFile(){
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:"+FILE_PATH)) {
            if (conn != null) {
                DatabaseMetaData meta = conn.getMetaData();
                System.out.println("SQLiteデータベースファイルを新規作成しました。");
            }
        } catch (SQLException var2) {
            Bukkit.getLogger().log(Level.SEVERE, "SQLiteデータベースに接続できませんでした。 エラーコード: " + var2.getErrorCode());
        }
    }

    public Connection open() {
        try {
            Class.forName("org.sqlite.JDBC");
            this.con = DriverManager.getConnection("jdbc:sqlite:"+FILE_PATH);
            this.st = con.createStatement();
            return this.con;
        } catch (SQLException var2) {
            Bukkit.getLogger().log(Level.SEVERE, "SQLiteデータベースに接続できませんでした。 エラーコード: " + var2.getErrorCode());
        } catch (ClassNotFoundException var3) {
            Bukkit.getLogger().log(Level.SEVERE, "SQLite用JDBCドライバーがこのマシンにインストールされていません");
        }
        return this.con;
    }

    public boolean checkConnection() {
        return this.con != null;
    }

    public void close() {
        try {
            this.con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        closed = true;
    }

    public void changePrepareState(String sql) throws SQLException {
        this.st = con.prepareStatement(sql);
    }

    public Statement getSt() {
        return st;
    }

    public PreparedStatement getSafeSt() {
        if(st instanceof PreparedStatement){
            return (PreparedStatement)st;
        }
        return null;
    }

    public boolean isClosed() {
        return closed;
    }
}
