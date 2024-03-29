package xyz.jmatt.MainForm;

import com.sun.javafx.scene.control.skin.TableHeaderRow;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBoxBuilder;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import xyz.jmatt.Main;
import xyz.jmatt.Strings;
import xyz.jmatt.dialogbox.DialogController;
import xyz.jmatt.models.Category;
import xyz.jmatt.models.SortableDate;
import xyz.jmatt.models.TransactionModel;
import xyz.jmatt.services.CategoryService;
import xyz.jmatt.services.TransactionService;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

public class MainFormController extends MenuItem implements Initializable {
    private static final DataFormat SERIALIZED_MIME_TYPE = new DataFormat("application/x-java-serialized-object");
    @FXML
    private BorderPane MainForm;
    @FXML
    private MenuBar MainMenuBar;
    @FXML
    private TableView TransactionTable;
    @FXML
    private TableColumn Name;
    @FXML
    private TableColumn Category;
    @FXML
    private TableColumn InAmount;
    @FXML
    private TableColumn OutAmount;
    @FXML
    private TableColumn Date;
    @FXML
    private TableColumn Total;
    @FXML
    private Button AddTransaction;
    @FXML
    private TextField TransIn;
    @FXML
    private TextField CategoryIn;
    @FXML
    private TextField AmountIn;
    @FXML
    private DatePicker DateIn;
    @FXML
    private TreeTableView CategoryTreeTableView;
    @FXML
    private TreeTableColumn TreeCategory;
    @FXML
    private Label AddedCategory;
    @FXML
    private TextField CategoryInput;

    private TreeItem<Category> root;
    private ObservableList<TransactionModel> transactionData;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTransactionTable();
        setupCategoryTree();
    }

    /**
     * Sets up the table of transactions
     */
    @SuppressWarnings("unchecked")
    private void setupTransactionTable() {
        //get all transactions from the database into a list
        transactionData = FXCollections.observableArrayList(
                new TransactionService().getAllTransactions()
        );

        //setup columns
        Name.setCellValueFactory(
                new PropertyValueFactory<TransactionModel, String>("Name"));
        Category.setCellValueFactory(
                new PropertyValueFactory<TransactionModel, String>("Category"));
        InAmount.setCellValueFactory(
                new PropertyValueFactory<TransactionModel, BigDecimal>("InAmount"));
        InAmount.setCellValueFactory(
                new PropertyValueFactory<TransactionModel, BigDecimal>("OutAmount"));
        Date.setCellValueFactory(
                new PropertyValueFactory<TransactionModel, SortableDate>("FormattedDate"));
        Total.setCellValueFactory(
                new PropertyValueFactory<TransactionModel, String>("Total"));

        //disable column reordering
        TransactionTable.skinProperty().addListener((observable, oldValue, newValue) -> {
            final TableHeaderRow header = (TableHeaderRow)TransactionTable.lookup("TableHeaderRow");
            header.reorderingProperty().addListener((observable1, oldValue1, newValue1) -> header.setReordering(false));
        });

        Name.prefWidthProperty().bind(TransactionTable.widthProperty().multiply(0.40));
        Category.prefWidthProperty().bind(TransactionTable.widthProperty().multiply(0.16));
        InAmount.prefWidthProperty().bind(TransactionTable.widthProperty().multiply(0.10));
        OutAmount.prefWidthProperty().bind(TransactionTable.widthProperty().multiply(0.10));
        Date.prefWidthProperty().bind(TransactionTable.widthProperty().multiply(0.12));
        Total.prefWidthProperty().bind(TransactionTable.widthProperty().multiply(0.12));

        //add list of transactions to table
        TransactionTable.setItems(transactionData);

        resetTransactionFields();
    }

    /**
     * Builds a tree of categories and prepares for drag events
     */
    @SuppressWarnings("unchecked")
    private void setupCategoryTree() {
        //get the tree of categories from the database
        Category categoryTree = new CategoryService().getAllCategories();
        TreeItem<Category> returnTree = new TreeItem<>(categoryTree);
        List<TreeItem<Category>> findNode = new ArrayList<>();
        root = ReadCategory(categoryTree, returnTree, findNode);
        CategoryTreeTableView.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);
        CategoryTreeTableView.setRowFactory(new Callback<TreeTableView, TreeTableRow<Category>>() {
            @Override
            public TreeTableRow<Category> call(TreeTableView param) {
                final TreeTableRow<Category> row = new TreeTableRow<>();
                row.setOnDragDetected(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        //get the item that was dragged
                        TreeItem<Category> selectedItem = (TreeItem<Category>)CategoryTreeTableView.getSelectionModel().getSelectedItem();
                        if(selectedItem != null) {
                            //serialize the category that is being dragged and put in on the clipboard
                            Dragboard db = CategoryTreeTableView.startDragAndDrop(TransferMode.MOVE);
                            db.setDragView(row.snapshot(null, null));
                            ClipboardContent content = new ClipboardContent();
                            content.put(SERIALIZED_MIME_TYPE, row.getIndex());
                            db.setContent(content);
                            event.consume();
                        }
                    }
                });
                row.setOnDragOver(new EventHandler<DragEvent>() {
                    @Override
                    public void handle(DragEvent event) {
                        if(event.getDragboard().hasContent(SERIALIZED_MIME_TYPE)){
                            event.acceptTransferModes(TransferMode.MOVE);
                        }
                        event.consume();
                    }
                });
                row.setOnDragDropped(new EventHandler<DragEvent>() {
                    @Override
                    public void handle(DragEvent event) {
                        Dragboard db = event.getDragboard();
                        boolean success = false;
                        if(event.getDragboard().hasContent(SERIALIZED_MIME_TYPE)) {
                            int index = (Integer)db.getContent(SERIALIZED_MIME_TYPE);
                            //int dropIndex = row.getIndex();
                            TreeItem<Category> droppedOn = row.getTreeItem();
                            if(droppedOn != null){
                                TreeItem<Category> moveItem = CategoryTreeTableView.getTreeItem(index);
                                moveItem.getParent().getChildren().remove(moveItem);
                                System.out.println(droppedOn.getValue().getName());
                                droppedOn.getChildren().add(moveItem);
                                droppedOn.getChildren().sort(Comparator.comparing(t->t.getValue().getName()));
                                //CategoryTreeTableView.sort();//droppedon.getChildren().sorted();
                                moveItem.getValue().setParentId(droppedOn.getValue().getId());
                                success = CategoryService.moveCategory(moveItem.getValue());
                            } else {
                                success = true;
                            }
                        }
                        event.setDropCompleted(success);
                        event.consume();
                    }

                });
                CategoryTreeTableView.refresh();
                return row;
            }
        });
        TreeCategory.setCellValueFactory(new TreeItemPropertyValueFactory<Category, String>("name"));
        CategoryTreeTableView.setRoot(root);
    }
    /**
     * Creates a new TransactionModel based on user input and adds it to the database
     */
    @FXML
    private void AddTransactionModel() {
        if(TransIn.getText().trim().isEmpty() || CategoryIn.getText().trim().isEmpty() || AmountIn.getText().trim().isEmpty()) {
            Main.showDialog(Strings.ERROR_EMPTY_FIELD);
        } else {
            LocalDate date = DateIn.getValue();
            long dateMillis = date.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
            TransactionModel trans = new TransactionModel(TransIn.getText(),CategoryIn.getText(),new BigDecimal(AmountIn.getText()), dateMillis);
            if(!new TransactionService().addTransaction(trans)) {
                Main.showDialog(Strings.ERROR_TRANSACTION_ADD);
            } else {
                transactionData.add(trans); //only add it to the chart if it saved properly so the user doesn't get confused
            }
            resetTransactionFields();
        }

    }

    /**
     * Resets the input fields for a new transaction
     */
    private void resetTransactionFields() {
        TransIn.setText("");
        CategoryIn.setText("");
        AmountIn.setText("");
        DateIn.setValue(LocalDate.now()); //default date to today
    }

    /**
     * Creates a new CategoryModel based on the user's input and adds it to the database
     */
    @FXML
    private void AddCategory()
    {
        Category addedCategory = new Category(CategoryInput.getText(), root.getValue().getId());
        if(!CategoryService.addCategory(addedCategory)) {
            Main.showDialog(Strings.ERROR_CATEGORY_ADD);
        } else {
            root.getChildren().add(new TreeItem<Category>(addedCategory)); //only add the category to the on-screen model if it saved correctly
        }
    }

    private TreeItem<Category> ReadCategory(Category category, TreeItem<Category> returnTree, List<TreeItem<Category>> findNode){
        if(findNode.size() > 0){
            findNode.remove(0);
        }
        if(category.getSubcategories().size() > 0){
            List<TreeItem<Category>> childrenOfCategory = new ArrayList<>();
            int i = 0;
            for(Category cat1 : category.getSubcategories()){
                childrenOfCategory.add(new TreeItem<>(cat1));
                if(cat1.getSubcategories().size() > 0){
                    findNode.add(childrenOfCategory.get(i));
                }
                i++;
            }
            returnTree.getChildren().setAll(childrenOfCategory);
            if(findNode.size() > 0){
                ReadCategory(findNode.get(0).getValue(), findNode.get(0), findNode);
            }
        }
        return returnTree;
    }
}

