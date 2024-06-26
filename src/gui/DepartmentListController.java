package gui;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import application.Main;
import db.DbException;
import db.DbIntegrityException;
import gui.listeners.DataChangeListener;
import gui.util.Alerts;
import gui.util.Utils;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.entities.Department;
import model.services.DepartmentService;

public class DepartmentListController implements Initializable, DataChangeListener {

    private DepartmentService service;

    @FXML
    private TableView<Department> tableViewDepartment;

    @FXML
    private TableColumn<Department, Integer> tableColumnId;

    @FXML
    private TableColumn<Department, String> tableColumnName;

    @FXML
    private TableColumn<Department, Department> tableColumnEdit;

    @FXML
    private TableColumn<Department, Department> tableColumnRemove;

    private ObservableList<Department> observableList;
    @FXML
    private Button buttonNew;

    @FXML
    public void onButtonNewAction(ActionEvent event) {
        Stage parentStage = Utils.currentStage(event);
        Department obj = new Department();
        createDialogForm("/gui/DepartmentForm.fxml", parentStage, obj);
    }

    private void initializeNodes() {
        Stage stage = (Stage) Main.getMainScene().getWindow();
        tableViewDepartment.prefHeightProperty().bind(stage.heightProperty());
        tableColumnId.setCellValueFactory(new PropertyValueFactory<>("id"));
        tableColumnName.setCellValueFactory(new PropertyValueFactory<>("name"));
    }

    public void setService(DepartmentService service) {
        this.service = service;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initializeNodes();
    }

    public void updateTableView() {
        if (service == null) {
            throw new IllegalStateException("Service was null");
        }
        List<Department> serviceList = service.findAll();
        observableList = FXCollections.observableArrayList(serviceList);
        tableViewDepartment.setItems(observableList);
        initEditButtons();
        initRemoveButtons();
    }

    public void createDialogForm(String absoluteName, Stage parentStage, Department obj) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(absoluteName));
            Pane pane = loader.load();
            DepartmentFormController controller = loader.getController();
            controller.setDepartment(obj);
            controller.setDepartmentService(new DepartmentService());
            controller.subscribeDataChangeListener(this);
            controller.updateFormData();
            Stage dialogFormStage = new Stage();
            dialogFormStage.setTitle("Enter Department data");
            dialogFormStage.setScene(new Scene(pane));
            dialogFormStage.setResizable(false);
            dialogFormStage.initOwner(parentStage);
            dialogFormStage.initModality(Modality.WINDOW_MODAL);
            dialogFormStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            Alerts.showAlerts("IOException", "Error loading view", e.getMessage(), AlertType.ERROR);
        }
    }

    @Override
    public void onDataChanged() {
        updateTableView();
    }

    private void initEditButtons() {
        tableColumnEdit.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        tableColumnEdit.setCellFactory(param -> new TableCell<Department, Department>() {
            private final Button button = new Button("Edit");

            @Override
            protected void updateItem(Department obj, boolean empty) {
                super.updateItem(obj, empty);
                if (obj == null) {
                    setGraphic(null);
                    return;
                }
                setGraphic(button);
                button.setOnAction(
                        event -> createDialogForm("/gui/DepartmentForm.fxml", Utils.currentStage(event), obj));
            }
        });
    }

    private void initRemoveButtons() {
        tableColumnRemove.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        tableColumnRemove.setCellFactory(param -> new TableCell<Department, Department>() {
            private Button button = new Button("Remove");

            @Override
            protected void updateItem(Department obj, boolean empty) {
                super.updateItem(obj, empty);
                if (obj == null){
                    setGraphic(null);
                    return;
                }
                setGraphic(button);
                button.setOnAction(event -> removeEnitty(obj));
            }
        });
    }

    protected void removeEnitty(Department obj) {
        Optional<ButtonType> result = Alerts.showConfirmation("Confirmation", "Are you sure to delete?");
        if (result.get() == ButtonType.OK) {
            if (service == null) {
                throw new IllegalStateException("Service was null");
            }
            try {
                service.remove(obj);
                updateTableView();
            } catch (DbIntegrityException e) {
                Alerts.showAlerts("Error removing object", null, e.getMessage(), AlertType.ERROR);
            }
        }
    }
}
