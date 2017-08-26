package xyz.jmatt.daos;

import xyz.jmatt.models.UserModel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Handles connections to the Users database table
 */
public class UsersDao {
    private Connection connection;

    public UsersDao(DatabaseTransaction transaction) {
        this.connection = transaction.getConnection();
    }

    /**
     * Pushes a new User to the Users database
     * @param userModel the UserModel to push
     * @throws SQLException database error
     */
    public void pushNewUser(UserModel userModel) throws SQLException {
        PreparedStatement prep = connection.prepareStatement(
                "INSERT INTO Users (Username, Password, UserId) VALUES (?,?,?);");
        prep.setString(1, userModel.getUsername());
        prep.setString(2, userModel.getPassword());
        prep.setString(3, userModel.getUserId());
        prep.executeUpdate();
        prep.close();
    }

    /**
     * Checks to see if the given username exists in the database, saves time having to compute password hashes if the username was not unique anyway
     * @param username the username to check
     * @return whether or not the username was found in the database
     * @throws SQLException database error
     */
    public boolean doesUsernameExist(String username) throws SQLException {
        PreparedStatement prep = connection.prepareStatement(
                "SELECT * FROM Users WHERE Username = ?;");
        prep.setString(1, username);
        ResultSet resultSet = prep.executeQuery();

        if(resultSet.next()) {
            prep.close();
            resultSet.close();
            return true;
        } else {
            prep.close();
            resultSet.close();
            return false;
        }
    }
}
