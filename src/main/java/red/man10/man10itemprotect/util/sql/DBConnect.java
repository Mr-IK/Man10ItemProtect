package red.man10.man10itemprotect.util.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class DBConnect {

    public abstract Connection open();

    public abstract boolean checkConnection();

    public abstract void close();

    public abstract Statement getSt();

    public abstract boolean isClosed();

    public abstract void changePrepareState(String sql) throws SQLException;

    public abstract PreparedStatement getSafeSt();
}
