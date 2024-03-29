package xyz.jmatt.services;

import xyz.jmatt.Strings;
import xyz.jmatt.auth.PasswordManager;
import xyz.jmatt.daos.*;
import xyz.jmatt.models.Category;
import xyz.jmatt.models.ClientSingleton;
import xyz.jmatt.models.SimpleResult;
import xyz.jmatt.models.UserModel;

import javax.xml.bind.DatatypeConverter;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Performs tasks related to creating a new account for given user credentials
 */
public class CreateAccountService {
    public CreateAccountService() {}

    /**
     * Attempts to create a new user account with the given username and password
     * @param username the username for the new account
     * @param originalPassword the password for the new account
     * @return result of the account creation
     */
    public SimpleResult createAccount(String name, String username, char[] originalPassword) {
        MainDatabaseTransaction transaction = null;
        SimpleResult result = null;

        try {
            transaction = new MainDatabaseTransaction();
            UsersDao usersDao = new UsersDao(transaction);

            //Make sure username was not already in the database
            if(usersDao.doesUsernameExist(username)) {
                result = new SimpleResult(Strings.ERROR_USERNAME, true); //username was already taken
            } else {
                //salt & hash the user's password for secure storage
                String securePassword = PasswordManager.getInstance().getSaltedHashedPassword(originalPassword);
                //generate a new UserId
                String userId = UUID.randomUUID().toString().replaceAll("-", ""); //get rid of dashes for nicer looking string
                ClientSingleton.getINSTANCE().setUserId(userId); //save the userId for the session

                //gather all data for model
                UserModel userModel = new UserModel();
                userModel.setUsername(username);
                userModel.setPassword(securePassword);
                userModel.setUserId(userId);
                userModel.setName(name);
                //push new user to Users table
                usersDao.pushNewUser(userModel);

                //set up a new database for the new user - each user get's their own db that will be encrypted with a key generated from their salted & hashed password
                byte[] dbSalt = securePassword.split(":")[1].getBytes(); //get the salt for the database encryption key - note: this is not the same salt for the login password authentication,
                                                                                //this way the hash stored in the database for the user's password and encryption key are different but the user only needs
                                                                                //to remember one password
                //re-generate the user's database encryption key for this session using their provided password & database salt
                String dbKey = DatatypeConverter.printHexBinary(PasswordManager.getInstance().getHashForPasswordAndSalt(originalPassword, dbSalt));
                ClientSingleton.getINSTANCE().setDbKey(dbKey);
                createNewUserDatabase(); //create a new database file encrypted with their key

                originalPassword = UUID.randomUUID().toString().toCharArray(); //overwrite the original password in memory

                //if everything else worked, commit then database changes
                transaction.commit();
                transaction.close();
                transaction = null;

                result = new SimpleResult("", false);
            }
        } catch (SQLException e) {
            result = new SimpleResult(Strings.ERROR_DATABASE, true);
            e.printStackTrace();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            result = new SimpleResult(Strings.ERROR_SERVER, true);
            e.printStackTrace();
        } finally {
            if(transaction != null) {
                transaction.rollback();
                transaction.close();
            }
        }
        return result;
    }

    /**
     * Creates a new encrypted database to hold all of the user's personal data
     */
    private void createNewUserDatabase() throws SQLException {
        PersonalDatabaseTransaction transaction = new PersonalDatabaseTransaction();

        //set up transaction database
        TransactionDao transactionDao = new TransactionDao(transaction);
        transactionDao.initializeTable();
        transaction.commit();
        transaction.close();

        //setup category database
        transaction = new PersonalDatabaseTransaction();
        CategoryDao categoryDao = new CategoryDao(transaction);
        categoryDao.initializeTable();
        transaction.commit();
        transaction.close();

        //add root category to category database
        transaction = new PersonalDatabaseTransaction();
        Category rootCategory = new Category();
        rootCategory.setName("ROOT");
        categoryDao = new CategoryDao(transaction);
        categoryDao.pushCategory(rootCategory);
        transaction.commit();
        transaction.close();
    }
}
