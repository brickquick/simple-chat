package quick.brick;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BaseAuthService implements AuthService {
    private static class Entry {
        private final String login;
        private final String pass;
        private final String nick;
//        private int id;

        public Entry(String login, String pass, String nick) {
            this.login = login;
            this.pass = pass;
            this.nick = nick;
        }
    }

    private static final String CON_STR = "jdbc:sqlite:server/db/entry.db";
    private static final Logger LOGGER = LogManager.getLogger(BaseAuthService.class);

    private static Connection connection;
    private static Statement stmt;
    private static ResultSet resultSet;

    private List<Entry> entries = new ArrayList<>();

    public BaseAuthService() {

        start();

        try {
            createTable();
        } catch (SQLException e) {
            LOGGER.info("Таблица уже существует");
        }
        try {
            addEntry();
        } catch (SQLException e) {
            LOGGER.info("Таблица уже заполнена");
        }


        entries = new ArrayList<>(getAllEntries());

        showTable();
    }


    @Override
    public void showTable() {
        try {
            connection.setAutoCommit(false);
            ResultSet rs = stmt.executeQuery("SELECT * FROM entries;");

            while (rs.next()) {
                int id = rs.getInt("id");
                String login = rs.getString("login");
                String pass = rs.getString("pass");
                String nick = rs.getString("nick");

                System.out.println("id = " + id);
                System.out.println("login = " + login);
                System.out.println("pass = " + pass);
                System.out.println("nick = " + nick);
                System.out.println();
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            LOGGER.error("Ошибка с базой данных");
        }
    }

    @Override
    public void changeNick(String oldNick, String newNick) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "UPDATE entries set nick = '" + newNick + "' where nick = '" + oldNick + "' ;")) {
            connection.setAutoCommit(false);
            preparedStatement.execute();
            connection.commit();

            entries.clear();
            entries.addAll(getAllEntries());
            LOGGER.info("Изменения в БД произведены успешно");
        } catch (SQLException e) {
            e.printStackTrace();
            LOGGER.error("Ошибка с базой данных");
        }
    }

    @Override
    public boolean isNickBusy(String newNick) {
        try {
            resultSet = stmt.executeQuery("SELECT * FROM entries;");
            while (resultSet.next()) {
                String nick = resultSet.getString("nick");
                if (nick.equals(newNick)) {
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            LOGGER.error("Ошибка связанная с базой данных");
        }
        return false;
    }

    @Override
    public String getNickByLoginPass(String login, String pass) {
        for (Entry o : entries) {
            if (o.login.equals(login) && o.pass.equals(pass)) {
                return o.nick;
            }
        }
        return null;
    }

    @Override
    public void start() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(CON_STR);
            stmt = connection.createStatement();
            LOGGER.info("Успешное подключение к базе данных");
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            LOGGER.error("Ошибка подключения к базе дынных");
        }
    }

    @Override
    public void stop() {
        try {
            stmt.close();
            connection.close();
            LOGGER.info("Сервис аутентификации остановлен");
        } catch (SQLException e) {
            e.printStackTrace();
            LOGGER.error("Ошибка с БД");
        }
    }

    private List<Entry> getAllEntries() {
        try {
            resultSet = stmt.executeQuery("SELECT login, pass, nick FROM entries");
            while (resultSet.next()) {
                entries.add(new Entry(
                        resultSet.getString("login"),
                        resultSet.getString("pass"),
                        resultSet.getString("nick")));
            }
            return entries;
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private static void createTable() throws SQLException {
        String sql = "CREATE TABLE entries " +
                "(ID       INT PRIMARY KEY NOT NULL," +
                " login    CHAR(50)        NOT NULL, " +
                " pass     CHAR(50)        NOT NULL, " +
                " nick     CHAR(50)        NOT NULL)";
        stmt.executeUpdate(sql);
        LOGGER.info("Таблица создана успешно");
    }

    private void addEntry() throws SQLException {
        connection.setAutoCommit(false);
        String sql = "INSERT INTO entries (ID,login,pass,nick) VALUES (1, 'login1', 'pass1', 'nick1');";
        stmt.executeUpdate(sql);

        sql = "INSERT INTO entries (ID,login,pass,nick) VALUES (2, 'login2', 'pass2', 'nick2');";
        stmt.executeUpdate(sql);

        sql = "INSERT INTO entries (ID,login,pass,nick) VALUES (3, 'login3', 'pass3', 'nick3');";
        stmt.executeUpdate(sql);
        connection.commit();
        LOGGER.info("INSERT INTO entries successfully");
    }

}
