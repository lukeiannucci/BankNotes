package xyz.jmatt.daos;

import xyz.jmatt.models.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CategoryDao {
    private Connection connection;

    public CategoryDao(PersonalDatabaseTransaction transaction) {
        this.connection = transaction.getConnection();
    }

    /**
     * Creates the table and all the columns for the first time
     */
    public void initializeTable() throws SQLException {
        PreparedStatement prep = connection.prepareStatement(
                "CREATE TABLE Categories (CategoryId VARCHAR(255) PRIMARY KEY, ParentId VARCHAR(255), Name VARCHAR(255));");
        prep.execute();
        prep.close();
    }

    /**
     * Gets all category objects from the database
     * @return an unsorted list of all category objects
     */
    public List<Category> getAllCategories() throws SQLException {
        PreparedStatement prep = connection.prepareStatement(
                "SELECT * FROM Categories");
        ResultSet resultSet = prep.executeQuery();

        List<Category> result = new ArrayList<>();
        while(resultSet.next()) {
            Category category = new Category();
            category.setId(resultSet.getString("CategoryId"));
            category.setParentId(resultSet.getString("ParentId"));
            category.setName(resultSet.getString("Name"));
            result.add(category);
        }
        resultSet.close();
        prep.close();
        return result;
    }

    /**
     * Adds the given category to the database
     * @param model the category model to add
     */
    public void pushCategory(Category model) throws SQLException {
        PreparedStatement prep = connection.prepareStatement(
                "INSERT INTO Categories (CategoryId,ParentId,Name) VALUES (?,?,?)");
        prep.setString(1, model.getId());
        prep.setString(2, model.getParentId());
        prep.setString(3, model.getName());

        prep.executeUpdate();
        prep.close();
    }

    /**
     * Moves a category in the database under the given parentId
     * @param id the id of the category to move
     * @param parentId the id of the new parent category
     */
    public void moveCategory(String id, String parentId) throws SQLException {
        PreparedStatement prep = connection.prepareStatement(
                "UPDATE Categories SET ParentId=? WHERE CategoryId=?;");
        prep.setString(1, parentId);
        prep.setString(2, id);

        prep.executeUpdate();
        prep.close();
    }

    /**
     * Renames a category in the database
     * @param id the id of the category to rename
     * @param name the new name of the category
     */
    public void renameCategory(String id, String name) throws SQLException {
        PreparedStatement prep = connection.prepareStatement(
                "UPDATE Categories SET Name=? WHERE CategoryId=?;");
        prep.setString(1, name);
        prep.setString(2, id);

        prep.executeUpdate();
        prep.close();
    }
}
