package kr.co.enjoytrip.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

public final class Database {
    private static volatile DataSource pooled;

    private Database() {}

    public static Connection open() throws SQLException {
        DataSource source = pooled;
        if (source == null) {
            synchronized (Database.class) {
                source = pooled;
                if (source == null) {
                    try {
                        source = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/EnjoyTrip");
                        pooled = source;
                    } catch (NamingException ignored) {
                        // Local development can use the environment-based JDBC connection below.
                    }
                }
            }
        }
        if (source != null) return source.getConnection();
        Properties properties = new Properties();
        properties.setProperty("user", AppConfig.required("ENJOYTRIP_DB_USER"));
        properties.setProperty("password", AppConfig.required("ENJOYTRIP_DB_PASSWORD"));
        Connection connection = new com.mysql.cj.jdbc.Driver().connect(AppConfig.required("ENJOYTRIP_DB_URL"), properties);
        if (connection == null) throw new SQLException("MySQL JDBC URL is not supported");
        return connection;
    }
}
