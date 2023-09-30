package Core.Controllers.Plan_Designer;

import Core.Controllers.Main_Controller;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.robot.Robot;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.net.URL;
import java.util.*;

import static java.lang.Math.log10;
import static javafx.scene.Cursor.*;

public class Plan_Designer_Controller implements Initializable {

    public Main_Controller main_controller;

    @FXML
    public TextField line_length_textfield_fx;
    @FXML
    public ScrollPane scrollable_plan_designer_fx;
    @FXML
    public Group plan_designer_group_fx;
    @FXML
    public ImageView Basemap_ImageView_fx;
    @FXML
    public TableView<Zone> zone_parts_table_fx;
    @FXML
    public TableView<Obstruction> obstructions_table_fx;
    @FXML
    public Pane Edit_Plan_Designer_Pane_fx;
    @FXML
    Pane main_editor_toolbox_fx;
    @FXML
    Button add_ver_obstruction_fx, add_hor_obstruction_fx, add_new_zone_part_fx;
    @FXML
    TabPane tab_pane_attribute_table_fx;
    @FXML
    Tab obstructions_tab_fx;
    @FXML
    Tab zone_parts_tab_fx;
    @FXML
    private Label georeference_status_fx;
    @FXML
    private VBox main_editor_panel_fx;
    @FXML
    private Pane georeference_editor_panel_fx;
    @FXML
    private Label georeferenced_ruler_value_fx;
    @FXML
    private ToggleButton ruler_switch_fx;
    @FXML
    private Button execute_georeference_fx, georeferencer_confirm_btn_fx, georeferencer_cancel_btn_fx;
    @FXML
    private TableColumn<Zone, Integer> zone_part_id_column_fx;
    @FXML
    private TableColumn<Zone, String> zone_part_groupid_column_fx;
    @FXML
    private TableColumn<Zone, String> remove_zone_part_column_fx;
    @FXML
    private TableColumn<Obstruction, Integer> obstruction_id_column_fx;
    @FXML
    private TableColumn<Obstruction, String> obstruction_typeid_column_fx;
    @FXML
    private TableColumn<Obstruction, String> remove_obstruction_column_fx;

    // Variables for the popup window for adding new zones
    public ChoiceBox zones_choice_box, obstruction_type_choice_box;
    private HBox new_zone_properties_layout, new_obstruction_type_properties_layout;
    public TextField new_zone_name_input, new_obstruction_type_input;
    private ColorPicker zone_color_picker;
    private Button start_zone_drawing_Button, start_obstruction_drawing_Button;

    public static double scaled_Img_MaxX;
    public static double scaled_Img_MaxY;
    static boolean ready_to_draw_a_new_obstruction, ready_to_draw_a_new_zone, ready_to_georeference,
            currently_editing_the_georeference_ruler = false;

    private boolean vertical_orientation;
    private static final double DEFAULT_DELTA = 1.2d;
    static Zone selectedZone;
    static Obstruction selectedObstruction;
    public static DoubleProperty Plan_Designer_zoomScale = new SimpleDoubleProperty(1.0d);
    public static boolean area_has_been_selected, obstruction_has_been_selected;
    public Georeference_Ruler georeference_ruler;

    // Create a map having <Zone IDs, Zone Objects> as <key, value> pairs
    public HashMap<Integer, Zone> zoneID_to_zoneObject = new HashMap<>();
    // Create a map having <Distinct Zone Names, Zone Colors> as <key, value> pairs
    public HashMap<String, Color> zoneName_to_zoneColor = new HashMap<>();

    // Create a map having <Obstruction IDs, Obstruction Objects> as <key, value> pairs
    public HashMap<Integer, Obstruction> obstructionID_to_obstructionObject = new HashMap<>();
    // Create a map having <Distinct Obstruction Types, Obstruction Radio Properties> as <key, value> pairs
    public HashMap<String, double[]> obstructionType_to_radioProperties = new HashMap<>();
    // Create a map having <Distinct Obstruction Types, Obstruction Colors> as <key, value> pairs
    public HashMap<String, Color> obstructionType_to_obstructionColor = new HashMap<>();

    private List<Object> new_zone_part_properties;
    private List<Object> new_obstruction_properties;

    private double drawable_X_Origin;
    private double drawable_Y_Origin;

    public ChangeListener<Zone> zone_part_has_been_selected_in_zone_parts_table_fx_Listener =
            (observableValue, oldValue, newValue) -> {
                //Check whether item is selected and set value of selected item to Label
                if (zone_parts_table_fx.getSelectionModel().getSelectedItem() != null) {
                    Zone selected_zone = zone_parts_table_fx.getSelectionModel().getSelectedItem();
                    if (!Zone.directly_clicked) {
                        selected_zone.zone_got_selected_action(this);
                    }

                    //System.out.println(zoneID_to_zoneObject.get(selected_zone.getZonePart_ID()));
                    //System.out.println(zone_parts_table_fx.getSelectionModel().getSelectedItem());
                    //ObservableList selectedCells = zone_parts_table_fx.getSelectionModel().getSelectedCells();
                    //System.out.println(selectedCells);
                    //TablePosition tablePosition = (TablePosition) selectedCells.get(0);
                    //System.out.println(tablePosition);
                    //Object val = tablePosition.getTableColumn().getCellData(newValue);
                    //System.out.println("Selected Value: " + val);
                }
            };

    public ChangeListener<Obstruction> obstruction_has_been_selected_in_obstructions_table_fx_Listener =
            (observableValue, oldValue, newValue) -> {
                //Check whether item is selected and set value of selected item to Label
                if (obstructions_table_fx.getSelectionModel().getSelectedItem() != null) {
                    Obstruction selected_obstruction = obstructions_table_fx.getSelectionModel().getSelectedItem();
                    if (!Obstruction.directly_clicked) {
                        selected_obstruction.obstruction_got_selected_action(this);
                    }
                }
            };

    // A listener to enable/disable the new_zone_name_input according to the user choice
    public ChangeListener<String> zones_choice_box_StringListener = (observable, oldValue, newValue) -> {
        if (zones_choice_box.getValue().equals("New Zone")) {
            new_zone_properties_layout.setDisable(false);
            new_zone_name_input.setText("");
            new_zone_name_input.setPromptText("Zone Name");
            zone_color_picker.setValue(Color.VIOLET);
        } else {
            new_zone_properties_layout.setDisable(true);
            new_zone_name_input.setText(String.valueOf(zones_choice_box.getValue()));
            // Get the name of the Zone and retrieve its corresponding color
            zone_color_picker.setValue(zoneName_to_zoneColor.get(zones_choice_box.getValue()));
        }
    };

    // A listener to enable/disable the new_obstruction_type_input according to the user choice
    public ChangeListener<String> obstruction_types_choice_box_StringListener = (observable, oldValue, newValue) -> {
        new_obstruction_type_properties_layout.setDisable(true);
        new_obstruction_type_input.setText(String.valueOf(obstruction_type_choice_box.getValue()));
    };

    // Enable/Disable the start_zone_drawing_button according to the validity of the given Zone name
    public ChangeListener<String> new_zone_name_input_StringListener = (observable, oldValue, newValue) -> {
        if (new_zone_name_input.getText().equals("")) {
            start_zone_drawing_Button.setDisable(true);
        } else {
            start_zone_drawing_Button.setDisable(false);
        }
    };

    // Enable/Disable the start_obstruction_drawing_Button according to the validity of the given Obstruction type
    public ChangeListener<String> new_obstruction_type_input_StringListener = (observable, oldValue, newValue) -> {
        if (new_obstruction_type_input.getText().equals("")) {
            start_obstruction_drawing_Button.setDisable(true);
        } else {
            start_obstruction_drawing_Button.setDisable(false);
        }
    };

    // A listener to force the text field of the ruler to be numeric only
    public ChangeListener<String> line_length_textfield_fx_StringListener = (observable, oldValue, newValue) -> {
        if (!newValue.matches("\\d*")) {
            line_length_textfield_fx.setText(newValue.replaceAll("[^\\d]", ""));
        }
    };

    //Listeners for making the scene's canvas draggable and zoomable
    private class SceneGestures {

        Plan_Designer_Controller parent_controller;
        double mouseAnchorX;
        double mouseAnchorY;
        double translateAnchorX;
        double translateAnchorY;

        SceneGestures(Plan_Designer_Controller parent_controller) {
            this.parent_controller = parent_controller;
        }

        /* The following listeners are for the actions onto ScrollablePlanDesigner */
        EventHandler<MouseEvent> getScrollablePlanDesigner_ClickedHandler() {
            return ScrollablePlanDesigner_ClickedHandler;
        }

        EventHandler<MouseEvent> getScrollablePlanDesigner_DragHandler() {
            return ScrollablePlanDesigner_DragHandler;
        }

        EventHandler<ScrollEvent> getScrollablePlanDesigner_ScrollHandler() {
            return ScrollablePlanDesigner_ScrollHandler;
        }

        // This listener is the first one who receives the signal for mouse clicks
        private EventHandler<MouseEvent> ScrollablePlanDesigner_ClickedHandler = event -> {
            // If we received a right click..
            if (event.getButton() == MouseButton.SECONDARY) {
                // If an area has been selected for editing, which is also not targeted for resize
                if ((area_has_been_selected && !selectedZone.about_to_resize) ||
                        (obstruction_has_been_selected && !selectedObstruction.about_to_resize)) {
                    // Deselect all features
                    deselect_all_features();
                    /* Since we do not consume here the right click, if this is done within an area or obstruction,
                     * it will become re-enabled. Otherwise, the process will stop.
                     */
                }
            }

            // Else, if we received a left click..
            else if (event.getButton() == MouseButton.PRIMARY) {

                // If the user does not want to add new areas
                if (!ready_to_draw_a_new_obstruction && !ready_to_draw_a_new_zone) {
                    // Initiate the panning of the Edit_Plan_Designer_Pane_fx (either during edit or pan mode)
                    mouseAnchorX = event.getX();
                    mouseAnchorY = event.getY();
                    translateAnchorX = Edit_Plan_Designer_Pane_fx.getTranslateX();
                    translateAnchorY = Edit_Plan_Designer_Pane_fx.getTranslateY();
                }
            }
        };

        // This listener is the first one who receives the signal for mouse dragging
        private EventHandler<MouseEvent> ScrollablePlanDesigner_DragHandler = event -> {
            // If we receive a right-clicked drag
            if (event.getButton() == MouseButton.SECONDARY) {
                // If we are currently not editing the georeference ruler or any obstruction/zone
                if (!obstruction_has_been_selected && !area_has_been_selected && !currently_editing_the_georeference_ruler) {
                    // Forget about the click because otherwise, the panel will move at the same time..
                    event.consume();
                }
            }
            // Also avoid the middle-clicked drags
            else if (event.getButton() == MouseButton.MIDDLE) {
                // Forget about the click
                event.consume();
            }
            // Otherwise we received a left-clicked drag
            else {
                // If the user does not want to add a new area part or a new obstruction
                if (!ready_to_draw_a_new_obstruction && !ready_to_draw_a_new_zone) {
                    // Pan the Edit_Plan_Designer_Pane_fx
                    Edit_Plan_Designer_Pane_fx.setTranslateX(translateAnchorX + event.getX() - mouseAnchorX);
                    Edit_Plan_Designer_Pane_fx.setTranslateY(translateAnchorY + event.getY() - mouseAnchorY);

                    // We have to consume this drag to avoid having other handlers reacting to it
                    event.consume();
                }
            }
        };

        private EventHandler<ScrollEvent> ScrollablePlanDesigner_ScrollHandler = new EventHandler<>() {

            @Override
            public void handle(ScrollEvent event) {
                double scale = Plan_Designer_zoomScale.get(); // currently we only use Y, same value is used for X
                double oldScale = scale;

                if (event.getDeltaY() < 0) {
                    scale /= DEFAULT_DELTA;
                } else {
                    scale *= DEFAULT_DELTA;
                }

                double f = (scale / oldScale) - 1;
                double dx = (event.getX() - (Edit_Plan_Designer_Pane_fx.getBoundsInParent().getWidth() / 2 + Edit_Plan_Designer_Pane_fx.getBoundsInParent().getMinX()));
                double dy = (event.getY() - (Edit_Plan_Designer_Pane_fx.getBoundsInParent().getHeight() / 2 + Edit_Plan_Designer_Pane_fx.getBoundsInParent().getMinY()));

                Edit_Plan_Designer_Pane_fx.setTranslateX(Edit_Plan_Designer_Pane_fx.getTranslateX() - (f * dx));
                Edit_Plan_Designer_Pane_fx.setTranslateY(Edit_Plan_Designer_Pane_fx.getTranslateY() - (f * dy));
                Plan_Designer_zoomScale.set(scale);

                // Update all snap points to become properly visible
                obstructionID_to_obstructionObject.values().forEach(
                        obstruction -> obstruction.refresh_snap_points(8 / Plan_Designer_zoomScale.getValue()));
                zoneID_to_zoneObject.values().forEach(
                        area -> area.refresh_snap_points(8 / Plan_Designer_zoomScale.getValue()));

                event.consume();
            }
        };

        /* The following listeners are for the actions onto PlanDesignerGroup (and not outside it) */
        EventHandler<MouseEvent> getPlanDesignerGroup_ClickedHandler() {
            return PlanDesignerGroup_ClickedHandler;
        }

        EventHandler<MouseEvent> getPlanDesignerGroup_DragHandler() {
            return PlanDesignerGroup_DragHandler;
        }

        EventHandler<MouseEvent> getPlanDesignerGroup_ReleasedHandler() {
            return PlanDesignerGroup_ReleasedHandler;
        }

        private EventHandler<MouseEvent> PlanDesignerGroup_ClickedHandler = event -> {
            // Consider only left clicks
            if (event.getButton() == MouseButton.PRIMARY) {

                // If the user wants to add a new obstruction
                if (ready_to_draw_a_new_obstruction) {

                    drawable_X_Origin = event.getX();
                    drawable_Y_Origin = event.getY();

                    // Generate the new obstruction object
                    selectedObstruction = new Obstruction(vertical_orientation,
                            drawable_X_Origin, drawable_Y_Origin, -1, -1,
                            (int) new_obstruction_properties.get(0),
                            (String) new_obstruction_properties.get(1),
                            (Color) new_obstruction_properties.get(2),
                            8 / Plan_Designer_zoomScale.getValue(), parent_controller);

                    obstructionID_to_obstructionObject.put(
                            (int) new_obstruction_properties.get(0), selectedObstruction);

                    Platform.runLater(() -> {
                        Robot robot = new Robot();
                        if (vertical_orientation) {
                            robot.mouseMove(event.getScreenX(), event.getScreenY() + 40);
                        } else {
                            robot.mouseMove(event.getScreenX() + 40, event.getScreenY());
                        }
                    });

                }
                // If the user wants to add a new zone
                else if (ready_to_draw_a_new_zone) {
                    drawable_X_Origin = event.getX();
                    drawable_Y_Origin = event.getY();

                    // Generate the new zone object
                    selectedZone = new Zone(drawable_X_Origin, drawable_Y_Origin, 0, 0,
                            (int) new_zone_part_properties.get(0),
                            (String) new_zone_part_properties.get(1),
                            (Color) new_zone_part_properties.get(2),
                            8 / Plan_Designer_zoomScale.getValue(), parent_controller);

                    zoneID_to_zoneObject.put((int) new_zone_part_properties.get(0), selectedZone);

                    Platform.runLater(() -> {
                        Robot robot = new Robot();
                        robot.mouseMove(event.getScreenX() + 10, event.getScreenY() + 10);
                    });
                }
            }
        };

        private EventHandler<MouseEvent> PlanDesignerGroup_DragHandler = new EventHandler<>() {

            @Override
            public void handle(MouseEvent event) {

                // Consider only left clicks
                if (event.getButton() == MouseButton.PRIMARY) {
                    // If the user wants to drag a zone
                    if (ready_to_draw_a_new_zone) {

                        double offsetX = event.getX() - drawable_X_Origin;
                        double offsetY = event.getY() - drawable_Y_Origin;

                        // Check whether the cursor has been dragged towards the right
                        if (offsetX > 0) {
                            // Check whether the position of the new cursor has reached the right end of the group view
                            if (event.getX() > Basemap_ImageView_fx.getBoundsInParent().getMaxX()) {
                                selectedZone.setWidth(Basemap_ImageView_fx.getBoundsInParent().getMaxX() - drawable_X_Origin);
                            } else {
                                selectedZone.setWidth(offsetX);
                            }
                        }
                        // Otherwise, the cursor has been dragged towards the left
                        else {
                            // Check whether the position of the new cursor has reached the left end of the group view
                            if (event.getX() < 0) {
                                selectedZone.setX(0);
                            } else {
                                selectedZone.setX(event.getX());
                            }
                            selectedZone.setWidth(drawable_X_Origin - selectedZone.getX());
                        }

                        // Check whether the cursor has been dragged towards the bottom
                        if (offsetY > 0) {
                            // Check whether the position of the new cursor has reached the bottom end of the group view
                            if (event.getY() > Basemap_ImageView_fx.getBoundsInParent().getMaxY()) {
                                selectedZone.setHeight(Basemap_ImageView_fx.getBoundsInParent().getMaxY() - drawable_Y_Origin);
                            } else {
                                selectedZone.setHeight(offsetY);
                            }
                        }
                        // Otherwise, the cursor has been dragged towards the top
                        else {
                            // Check whether the position of the new cursor has reached the top end of the group view
                            if (event.getY() < 0) {
                                selectedZone.setY(0);
                            } else {
                                selectedZone.setY(event.getY());
                            }
                            selectedZone.setHeight(drawable_Y_Origin - selectedZone.getY());
                        }

                        // Consume the left click so we do not continue with further unwanted actions
                        event.consume();
                    }
                    // Else if the user wants to drag an obstruction
                    else if (ready_to_draw_a_new_obstruction) {

                        double offsetX = event.getX() - drawable_X_Origin;
                        double offsetY = event.getY() - drawable_Y_Origin;

                        // Check whether this obstruction is a horizontal one
                        if (!selectedObstruction.vertical_orientation) {
                            // Check whether the cursor has been dragged towards the right
                            if (offsetX > 0) {
                                // Check whether the position of the new cursor has reached the right end of the group view
                                if (event.getX() > Basemap_ImageView_fx.getBoundsInParent().getMaxX()) {
                                    selectedObstruction.setEndX(Basemap_ImageView_fx.getBoundsInParent().getMaxX());
                                } else {
                                    selectedObstruction.setEndX(event.getX());
                                }
                            }
                            // Otherwise, the cursor has been dragged towards the left
                            else {
                                // Check whether the position of the new cursor has reached the left end of the group view
                                if (event.getX() < 0) {
                                    selectedObstruction.setStartX(0);
                                } else {
                                    selectedObstruction.setStartX(event.getX());
                                }
                            }
                        }
                        // If we are here, it means that we are editing a vertical obstruction
                        else {
                            // Check whether the cursor has been dragged towards the bottom
                            if (offsetY > 0) {
                                // Check whether the position of the new cursor has reached the bottom end of the group view
                                if (event.getY() > Basemap_ImageView_fx.getBoundsInParent().getMaxY()) {
                                    selectedObstruction.setEndY(Basemap_ImageView_fx.getBoundsInParent().getMaxY());
                                } else {
                                    selectedObstruction.setEndY(event.getY());
                                }
                            }
                            // Otherwise, the cursor has been dragged towards the top
                            else {
                                // Check whether the position of the new cursor has reached the top end of the group view
                                if (event.getY() < 0) {
                                    selectedObstruction.setStartY(0);
                                } else {
                                    selectedObstruction.setStartY(event.getY());
                                }
                            }
                        }
                        // Consume the left click so we do not continue with further unwanted actions
                        event.consume();
                    }
                }
            }
        };

        private EventHandler<MouseEvent> PlanDesignerGroup_ReleasedHandler = new EventHandler<>() {

            @Override
            public void handle(MouseEvent event) {
                // Ensure we received a left click within the PlanDesigner and the user wants to add a new area or zone
                if (event.getButton() == MouseButton.PRIMARY) {
                    if (ready_to_draw_a_new_obstruction) {
                        obstructions_table_fx.getItems().add(selectedObstruction);
                        finish_addition_of_new_feature();
                    } else if (ready_to_draw_a_new_zone) {
                        zone_parts_table_fx.getItems().add(selectedZone);
                        finish_addition_of_new_feature();
                    }
                }
            }
        };
    }

    private void deselect_all_features() {
        // Deselect current area
        area_has_been_selected = false;
        // Also deselect any areas from the corresponding table
        zone_parts_table_fx.getSelectionModel().clearSelection();
        try {
            selectedZone.hide_snap_points();
        } catch (Exception ignored) {
        }

        // Deselect current obstruction
        obstruction_has_been_selected = false;
        // Also deselect any obstructions from the corresponding table
        obstructions_table_fx.getSelectionModel().clearSelection();
        try {
            selectedObstruction.hide_snap_points();
        } catch (Exception ignored) {
        }

        // Reactivate the components that become usable again
        add_ver_obstruction_fx.setDisable(false);
        add_hor_obstruction_fx.setDisable(false);
        add_new_zone_part_fx.setDisable(false);
        main_editor_toolbox_fx.setDisable(false);
    }

    private double getRSS_based_on_distance(double distance) {
        /*
         * Distance is the pixel squared distance in mm
         * */
        return 10 * log10(distance) + georeference_ruler.georeference_scale_FSPL_constant;


    }

    private static <T> T[] concatAll(T[] first, T[]... rest) {
        int totalLength = first.length;
        for (T[] array : rest) {
            totalLength += array.length;
        }
        T[] result = Arrays.copyOf(first, totalLength);
        int offset = first.length;
        for (T[] array : rest) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }

    private void add_obstruction(boolean ver_orientation) {

        vertical_orientation = ver_orientation;

        // Disable the attribute table to avoid pressing any button
        tab_pane_attribute_table_fx.setDisable(true);

        Stage obstruction_settings_dialog_stage = new Stage();
        obstruction_settings_dialog_stage.initModality(Modality.APPLICATION_MODAL);
        obstruction_settings_dialog_stage.setTitle("Obstruction Properties");
        obstruction_settings_dialog_stage.setMinWidth(320);
        obstruction_settings_dialog_stage.setMinHeight(220);

        Label label_assigned_obstruction_type_prompt = new Label("Obstruction's Type:");
        label_assigned_obstruction_type_prompt.setAlignment(Pos.CENTER);

        obstruction_type_choice_box = new ChoiceBox();

        // Get the sorted types of all existing obstruction types
        obstruction_type_choice_box.getItems().add(0, "10cm Wall");
        obstruction_type_choice_box.getItems().add(1, "40cm Wall");
        obstruction_type_choice_box.getItems().add(2, "70cm Wall");

        // Create the entry of the assigned_obstruction_type
        new_obstruction_type_input = new TextField();
        new_obstruction_type_input.setMaxWidth(120);

        // Create the HBox that will contain the properties setters for the new obstruction type
        new_obstruction_type_properties_layout = new HBox(10);
        // Add the action buttons to their parent layout
        new_obstruction_type_properties_layout.getChildren().addAll(new_obstruction_type_input);
        new_obstruction_type_properties_layout.setAlignment(Pos.CENTER);
        // Enable the layout only in case the user wants to add a new obstruction. Hence, set it by default to disabled
        new_obstruction_type_properties_layout.setDisable(true);

        // A listener to enable/disable the new_obstruction_type_input according to the user choice
        obstruction_type_choice_box.valueProperty().addListener(obstruction_types_choice_box_StringListener);

        // Create the action buttons
        start_obstruction_drawing_Button = new Button("Draw Obstruction");
        Button cancel_obstruction_drawing_Button = new Button("Cancel");

        start_obstruction_drawing_Button.setDisable(true);

        // A listener to enable/disable the start_obstruction_drawing_Button
        // according to the validity of the given Obstruction type
        new_obstruction_type_input.textProperty().addListener(new_obstruction_type_input_StringListener);

        int max = 0;
        for (int var : obstructionID_to_obstructionObject.keySet()) {
            if (max < var) {
                max = var;
            }
        }
        final int new_id = max + 1;

        start_obstruction_drawing_Button.setOnAction(e -> {
            start_drawing_new_obstruction(new_id, new_obstruction_type_input.getText(),
                    obstructionType_to_obstructionColor.get(obstruction_type_choice_box.getValue()));
            obstruction_settings_dialog_stage.close();
        });

        // When user cancels, execute the following actions
        cancel_obstruction_drawing_Button.setOnAction(e -> {
            tab_pane_attribute_table_fx.setDisable(false);
            main_editor_toolbox_fx.setDisable(false);
            obstruction_settings_dialog_stage.close();
        });

        // Create the HBox that will contain the action buttons
        HBox obstruction_settings_dialog_action_buttons_layout = new HBox(10);
        // Add the action buttons to their parent layout
        obstruction_settings_dialog_action_buttons_layout.getChildren().addAll(
                start_obstruction_drawing_Button, cancel_obstruction_drawing_Button);
        obstruction_settings_dialog_action_buttons_layout.setAlignment(Pos.CENTER);

        VBox obstruction_settings_dialog_layout = new VBox(10);
        // Add everything to the main obstruction_settings_dialog_layout
        obstruction_settings_dialog_layout.getChildren().addAll(label_assigned_obstruction_type_prompt, obstruction_type_choice_box,
                new_obstruction_type_properties_layout, obstruction_settings_dialog_action_buttons_layout);
        obstruction_settings_dialog_layout.setAlignment(Pos.CENTER);

        Scene add_new_obstruction_scene = new Scene(obstruction_settings_dialog_layout);
        obstruction_settings_dialog_stage.setScene(add_new_obstruction_scene);
        obstruction_settings_dialog_stage.show();

        // Set a listener to identify the exit state
        obstruction_settings_dialog_stage.setOnCloseRequest(we -> {
            tab_pane_attribute_table_fx.setDisable(false);
            main_editor_toolbox_fx.setDisable(false);
        });
    }

    private void add_new_zone_part() {

        // Disable the attribute table to avoid pressing any button
        tab_pane_attribute_table_fx.setDisable(true);

        Stage zone_settings_dialog_stage = new Stage();
        zone_settings_dialog_stage.initModality(Modality.APPLICATION_MODAL);
        zone_settings_dialog_stage.setTitle("Zone Properties");
        zone_settings_dialog_stage.setMinWidth(320);
        zone_settings_dialog_stage.setMinHeight(220);

        Label label_assigned_zone_prompt = new Label("Assigned Zone:");
        label_assigned_zone_prompt.setAlignment(Pos.CENTER);

        zones_choice_box = new ChoiceBox();

        // Get the sorted names of all existing zones
        // from the zoneName_to_zoneColor map and add them as to the choice box
        zones_choice_box.getItems().addAll(new TreeSet(zoneName_to_zoneColor.keySet()));
        zones_choice_box.getItems().addAll(new Separator(), "New Zone");

        // Create the entry of the assigned_zone
        new_zone_name_input = new TextField();
        new_zone_name_input.setMaxWidth(120);

        //Web color value set as the currently selected color
        zone_color_picker = new ColorPicker(Color.BLUE);
        zone_color_picker.setMaxWidth(50);
        zone_color_picker.getStyleClass().add("split-button");

        // Create the HBox that will contain the properties setters for the new zone
        new_zone_properties_layout = new HBox(10);
        // Add the action buttons to their parent layout
        new_zone_properties_layout.getChildren().addAll(new_zone_name_input, zone_color_picker);
        new_zone_properties_layout.setAlignment(Pos.CENTER);
        // Enable the layout only in case the user wants to add a new zone. Hence, set it by default to disabled
        new_zone_properties_layout.setDisable(true);

        // A listener to enable/disable the new_zone_name_input according to the user choice
        zones_choice_box.valueProperty().addListener(zones_choice_box_StringListener);

        // Create the action buttons
        start_zone_drawing_Button = new Button("Draw Zone Part");
        Button cancel_zone_drawing_Button = new Button("Cancel");

        start_zone_drawing_Button.setDisable(true);

        // A listener to enable/disable the start_zone_drawing_button according to the validity of the given Zone name
        new_zone_name_input.textProperty().addListener(new_zone_name_input_StringListener);

        int max = 0;
        for (int var : zoneID_to_zoneObject.keySet()) {
            if (max < var) {
                max = var;
            }
        }
        final int new_id = max + 1;

        start_zone_drawing_Button.setOnAction(e -> {
            // Check if the input Zone is new
            if (!zoneName_to_zoneColor.containsKey(new_zone_name_input.getText())) {
                zoneName_to_zoneColor.put(new_zone_name_input.getText(), zone_color_picker.getValue());
                start_drawing_new_zone(new_id, new_zone_name_input.getText(), zone_color_picker.getValue());
                zone_settings_dialog_stage.close();
            }

            // Check if the user tried to add an already existing Zone (with probably color conflicts)
            else if (zones_choice_box.getValue().equals("New Zone")) {
                new_zone_name_input.setPromptText("Zone exists");
                new_zone_name_input.setText("");
            }

            // The user selected an already existing Zone
            else {
                start_drawing_new_zone(new_id, new_zone_name_input.getText(), zone_color_picker.getValue());
                zone_settings_dialog_stage.close();
            }
        });

        // When user cancels, execute the following actions
        cancel_zone_drawing_Button.setOnAction(e -> {
            tab_pane_attribute_table_fx.setDisable(false);
            main_editor_toolbox_fx.setDisable(false);
            zone_settings_dialog_stage.close();
        });

        // Create the HBox that will contain the action buttons
        HBox zone_settings_dialog_action_buttons_layout = new HBox(10);
        // Add the action buttons to their parent layout
        zone_settings_dialog_action_buttons_layout.getChildren().addAll(
                start_zone_drawing_Button, cancel_zone_drawing_Button);
        zone_settings_dialog_action_buttons_layout.setAlignment(Pos.CENTER);

        VBox zone_settings_dialog_layout = new VBox(10);
        // Add everything to the main zone_settings_dialog_layout
        zone_settings_dialog_layout.getChildren().addAll(label_assigned_zone_prompt, zones_choice_box,
                new_zone_properties_layout, zone_settings_dialog_action_buttons_layout);
        zone_settings_dialog_layout.setAlignment(Pos.CENTER);

        Scene add_new_zone_part_scene = new Scene(zone_settings_dialog_layout);
        zone_settings_dialog_stage.setScene(add_new_zone_part_scene);
        zone_settings_dialog_stage.show();

        // Set a listener to identify the exit state
        zone_settings_dialog_stage.setOnCloseRequest(we -> {
            tab_pane_attribute_table_fx.setDisable(false);
            main_editor_toolbox_fx.setDisable(false);
        });
    }

    private void start_drawing_new_zone(int id, String zone_name, Color zone_color) {
        ready_to_draw_a_new_zone = true;

        // Store the properties of the new Zone part
        new_zone_part_properties = new ArrayList<>();
        new_zone_part_properties.add(id);
        new_zone_part_properties.add(zone_name);
        new_zone_part_properties.add(zone_color);

        // Disable the following buttons to avoid pressing them while having already entered new-object-creation mode
        main_editor_toolbox_fx.setDisable(true);
        tab_pane_attribute_table_fx.setDisable(true);

        // Change the cursor to indicate we are in obstruction editing mode
        Basemap_ImageView_fx.setCursor(CROSSHAIR);
    }

    private void start_drawing_new_obstruction(int id, String obstruction_type, Color zone_color) {
        ready_to_draw_a_new_obstruction = true;

        // Store the properties of the new Zone part
        new_obstruction_properties = new ArrayList<>();
        new_obstruction_properties.add(id);
        new_obstruction_properties.add(obstruction_type);
        new_obstruction_properties.add(zone_color);

        // Disable the following buttons to avoid pressing them while having already entered new-object-creation mode
        main_editor_toolbox_fx.setDisable(true);
        tab_pane_attribute_table_fx.setDisable(true);

        // Change the cursor to indicate we are in obstruction editing mode
        Basemap_ImageView_fx.setCursor(CROSSHAIR);
    }

    private void execute_georeference() {

        //remove the previous georeference in case it already exists
        if (georeference_ruler != null) {
            remove_georeferencing();
        }

        ready_to_georeference = true;

        // Switch the right panels
        main_editor_panel_fx.setVisible(false);
        georeference_editor_panel_fx.setVisible(true);

        // Change the cursor to indicate we are in obstruction editing mode
        Basemap_ImageView_fx.setCursor(DEFAULT);

        // Get the on-screen resolution of the loaded basemap
        double preview_width = Basemap_ImageView_fx.getBoundsInLocal().getWidth();
        double preview_height = Basemap_ImageView_fx.getBoundsInLocal().getHeight();

        // Generate the new georeference ruler
        georeference_ruler = new Georeference_Ruler(preview_width, preview_height, plan_designer_group_fx);

        // force the text field to be numeric only
        line_length_textfield_fx.textProperty().addListener(line_length_textfield_fx_StringListener);
    }

    // Confirm the georeferencing setup
    public void confirm_georeferencing(double real_ruler_length, double[] boundaries) {
        try {
            // If the georeferencing has been triggered from the open project process
            if (boundaries != null) {
                // Generate here the new georeference ruler
                georeference_ruler = new Georeference_Ruler(boundaries[0], boundaries[1], boundaries[2], boundaries[3],
                        plan_designer_group_fx);
            }

            // Calculate the distance based on the pythagorean theorem
            double georef_ruler_distance =
                    Math.sqrt(Math.pow(georeference_ruler.getEndX() - georeference_ruler.getStartX(), 2) +
                            Math.pow(georeference_ruler.getEndY() - georeference_ruler.getStartY(), 2));

            // Calculate the scale according to the real dimensions and update the corresponding properties
            georeference_ruler.setGeoreference_scale(real_ruler_length / georef_ruler_distance);

            // Refresh and bind the text of the distance Label, showing the distance property of the Georeference Ruler
            georeference_ruler.real_distance.set(georef_ruler_distance * georeference_ruler.getGeoreference_scale() / 1000);
            georeferenced_ruler_value_fx.textProperty().bind(georeference_ruler.real_distance.asString("%.2f"));

            georeferencing_succeeded(true);

        } catch (Exception int_exception) {
            line_length_textfield_fx.setPromptText("Invalid Length");
            // Remove any length input
            line_length_textfield_fx.setText("");
        }
    }

    // Remove any existing georeferencing
    private void remove_georeferencing() {
        georeferencing_succeeded(false);

        // Delete the 3 last elements (i.e the georeference ruler) from the view
        ObservableList group_elements = plan_designer_group_fx.getChildren();
        group_elements.remove(georeference_ruler);
        group_elements.remove(georeference_ruler.snap_point_1);
        group_elements.remove(georeference_ruler.snap_point_2);

        // This will be used to flag that no ruler has been initialized yet
        georeference_ruler = null;

        // Remove any length input
        line_length_textfield_fx.setText("");

        ready_to_georeference = false;
    }

    // Handle the states of other objects based on the success of the georeferencing
    private void georeferencing_succeeded(Boolean state) {
        // Switch the right panels
        main_editor_panel_fx.setVisible(true);
        georeference_editor_panel_fx.setVisible(false);
        ready_to_georeference = false;

        if (!state) {
            main_controller.Optimize_Nodes_button_fx.setDisable(true);
            georeference_status_fx.setText("No");
            ruler_switch_fx.setDisable(true);
            ruler_switch_fx.setText("Show");
            georeferenced_ruler_value_fx.setVisible(false);
        } else {
            main_controller.Optimize_Nodes_button_fx.setDisable(false);
            georeference_status_fx.setText("Yes");
            ruler_switch_fx.setDisable(false);
            ruler_switch_fx.setText("Hide");
            georeferenced_ruler_value_fx.setVisible(true);
        }
    }

    private void ruler_toggle() {
        if (georeferenced_ruler_value_fx.isVisible()) {
            georeferenced_ruler_value_fx.setVisible(false);
            ruler_switch_fx.setText("Show");
            georeference_ruler.hide_Ruler();
        } else {
            georeferenced_ruler_value_fx.setVisible(true);
            ruler_switch_fx.setText("Hide");
            georeference_ruler.show_Ruler();
        }
    }

    /* Set the OnMousePressed events */
    private void Set_OnMousePressed_Events() {

        ////// Handle the Add Obstruction event //////
        add_ver_obstruction_fx.setOnMousePressed(event -> add_obstruction(true));
        add_hor_obstruction_fx.setOnMousePressed(event -> add_obstruction(false));

        ////// Handle the add_new_zone_part event //////
        add_new_zone_part_fx.setOnMousePressed(event -> add_new_zone_part());

        ////// Handle the Georeferencer event //////
        execute_georeference_fx.setOnMousePressed(event -> execute_georeference());

        ////// Handle the confirmation of the georeferencing //////
        georeferencer_confirm_btn_fx.setOnMousePressed(event -> confirm_georeferencing(
                Integer.valueOf(line_length_textfield_fx.getText()), null));

        ////// Handle the cancellation of the georeferencing //////
        georeferencer_cancel_btn_fx.setOnMousePressed(event -> remove_georeferencing());

        ////// Handle the toggling of ruler_switch_fx //////
        ruler_switch_fx.setOnMousePressed(event -> ruler_toggle());
    }

    private void finish_addition_of_new_feature() {
        ready_to_draw_a_new_obstruction = false;
        ready_to_draw_a_new_zone = false;

        tab_pane_attribute_table_fx.setDisable(false);
        main_editor_toolbox_fx.setDisable(false);
        Basemap_ImageView_fx.setCursor(OPEN_HAND);
    }

    private void removeZonePart(Zone zone) {
        // Deselect all features
        deselect_all_features();

        // Remove from canvas current zones snap points
        for (Rectangle snap_point : zone.snap_points) {
            plan_designer_group_fx.getChildren().remove(snap_point);
        }

        // Remove from canvas the zone itself
        plan_designer_group_fx.getChildren().remove(zone);

        // Remove the zone from the zone parts table
        zone_parts_table_fx.getItems().remove(zone);

        // Remove any zones reference from mapping data structures
        zoneID_to_zoneObject.remove(zone.getZonePart_ID());

        // Check if this zone part was the last from its group in order to remove also the corresponding color reference
        boolean remaining_zone_parts_of_same_group = false;
        for (Zone remained_zone_part : zone_parts_table_fx.getItems()) {
            // Check whether any remaining zone part has been found, belonging to the same group
            if (zone.getGroupName().equals(remained_zone_part.getGroupName())) {
                remaining_zone_parts_of_same_group = true;
            }
        }
        if (!remaining_zone_parts_of_same_group) {
            zoneName_to_zoneColor.remove(zone.getGroupName());
        }
    }

    private void removeObstruction(Obstruction obstruction) {
        // Deselect all features
        deselect_all_features();

        // Remove from canvas current obstruction snap points
        plan_designer_group_fx.getChildren().remove(obstruction.top_left);
        plan_designer_group_fx.getChildren().remove(obstruction.bottom_right);

        // Remove from canvas the obstruction itself
        plan_designer_group_fx.getChildren().remove(obstruction);

        // Remove the obstruction from the obstructions table
        obstructions_table_fx.getItems().remove(obstruction);

        // Remove any obstructions reference from mapping data structures
        obstructionID_to_obstructionObject.remove(obstruction.getObstruction_ID());
        obstructionType_to_obstructionColor.remove(obstruction.getObstruction_Color());
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        obstructionType_to_obstructionColor.put("10cm Wall", Color.web("0xb3801aff"));
        obstructionType_to_obstructionColor.put("40cm Wall", Color.web("0xcc3333ff"));
        obstructionType_to_obstructionColor.put("70cm Wall", Color.web("0x4d001aff"));


        // Get the columns from the zone parts table and set the corresponding Cell Factories
        zone_part_id_column_fx.setCellValueFactory(new PropertyValueFactory<>("zonePart_ID"));
        zone_part_groupid_column_fx.setCellValueFactory(new PropertyValueFactory<>("groupName"));
        remove_zone_part_column_fx.setCellValueFactory(new PropertyValueFactory<>("zonePart_Color"));

        // Get the columns from the obstruction table and set the corresponding Cell Factories
        obstruction_id_column_fx.setCellValueFactory(new PropertyValueFactory<>("obstruction_ID"));
        obstruction_typeid_column_fx.setCellValueFactory(new PropertyValueFactory<>("obstruction_Type"));
        remove_obstruction_column_fx.setCellValueFactory(new PropertyValueFactory<>("obstruction_Color"));

        Callback<TableColumn<Zone, String>, TableCell<Zone, String>> remove_zone_part_cellFactory = new Callback<>() {
            @Override
            public TableCell call(final TableColumn<Zone, String> param) {
                final TableCell<Zone, String> cell = new TableCell<>() {

                    final Button btn = new Button("x");

                    @Override
                    public void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                            setText(null);
                            setStyle("");
                        } else {
                            Zone zone = getTableView().getItems().get(getIndex());
                            btn.setOnAction(event -> removeZonePart(zone));
                            setGraphic(btn);
                            setText(null);
                            setTextFill(Color.BLACK);
                            setStyle("-fx-background-color: #" + zone.getZonePart_Color().substring(2));
                        }
                    }
                };
                return cell;
            }
        };
        remove_zone_part_column_fx.setCellFactory(remove_zone_part_cellFactory);

        Callback<TableColumn<Obstruction, String>, TableCell<Obstruction, String>> remove_obstruction_cellFactory = new Callback<>() {
            @Override
            public TableCell call(final TableColumn<Obstruction, String> param) {
                final TableCell<Obstruction, String> cell = new TableCell<>() {

                    final Button btn = new Button("x");

                    @Override
                    public void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                            setText(null);
                            setStyle("");
                        } else {
                            Obstruction obstruction = getTableView().getItems().get(getIndex());
                            btn.setOnAction(event -> removeObstruction(obstruction));
                            setGraphic(btn);
                            setText(null);
                            setTextFill(Color.BLACK);
                            setStyle("-fx-background-color: #" + obstruction.getObstruction_Color().substring(2));
                        }
                    }
                };
                return cell;
            }
        };
        remove_obstruction_column_fx.setCellFactory(remove_obstruction_cellFactory);

        // Remove the background color from the tab menu
        tab_pane_attribute_table_fx.getStyleClass().add("floating");

        // This action is for normal buttons
        Set_OnMousePressed_Events();

        // Set the selection listeners for the tables
        zone_parts_table_fx.getSelectionModel().selectedItemProperty().addListener(
                zone_part_has_been_selected_in_zone_parts_table_fx_Listener);

        obstructions_table_fx.getSelectionModel().selectedItemProperty().addListener(
                obstruction_has_been_selected_in_obstructions_table_fx_Listener);

        SceneGestures sceneGestures = new SceneGestures(this);

        scrollable_plan_designer_fx.addEventFilter(MouseEvent.MOUSE_PRESSED, sceneGestures.getScrollablePlanDesigner_ClickedHandler());
        scrollable_plan_designer_fx.addEventFilter(MouseEvent.MOUSE_DRAGGED, sceneGestures.getScrollablePlanDesigner_DragHandler());
        scrollable_plan_designer_fx.addEventFilter(ScrollEvent.ANY, sceneGestures.getScrollablePlanDesigner_ScrollHandler());

        // This will handle the initiation of obstruction parts generation, where the plan_designer_group_fx is used
        plan_designer_group_fx.addEventFilter(MouseEvent.MOUSE_PRESSED, sceneGestures.getPlanDesignerGroup_ClickedHandler());
        plan_designer_group_fx.addEventFilter(MouseEvent.MOUSE_DRAGGED, sceneGestures.getPlanDesignerGroup_DragHandler());
        plan_designer_group_fx.addEventFilter(MouseEvent.MOUSE_RELEASED, sceneGestures.getPlanDesignerGroup_ReleasedHandler());
    }

}
