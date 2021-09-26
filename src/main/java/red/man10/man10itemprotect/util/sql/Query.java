package red.man10.man10itemprotect.util.sql;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Query {
    private ResultSet rs = null;
    private final DBConnect connect;

    public Query(ResultSet rs, DBConnect connect){
        this.connect = connect;
        this.rs = rs;
    }

    public ResultSet getResultSet() {
        return rs;
    }

    public void close(){
        try {
            rs.close();
            connect.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}