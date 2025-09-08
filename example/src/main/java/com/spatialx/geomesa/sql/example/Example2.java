package com.spatialx.geomesa.sql.example;

import java.io.Console;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class Example2 {
    public static void main(String[] args) {
        String jdbcUrl = "jdbc:geomesa:hbase.catalog=geomesa;hbase.zookeepers=ds245:2181";
        Properties info = new Properties();
        info.setProperty("fun", "spatial");
        info.setProperty("caseSensitive", "false");
        try (Connection connection = DriverManager.getConnection(jdbcUrl, info)) {
            System.out.println("成功连接");
//            String sql = "explain plan for SELECT line, COUNT(1) FROM beijing_subway_station JOIN beijing_subway ON ST_Intersects(beijing_subway.geom, beijing_subway_station.geom) GROUP BY line ";
            String sql = "explain plan for SELECT line, COUNT(1) FROM beijing_subway_station where line LIKE '%线' GROUP BY line";
            try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {
                int numColumns = resultSet.getMetaData().getColumnCount();
                while (resultSet.next()) {
                    System.out.print("|");
                    for (int k = 1; k <= numColumns; k++) {
                        Object datum = resultSet.getObject(k);
                        System.out.print(String.format("%s|", datum));
                    }
                    System.out.print("\n");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            System.out.println("连接失败");
        }
    }
}
