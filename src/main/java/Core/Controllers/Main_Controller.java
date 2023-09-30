package Core.Controllers;

import Core.ConfirmBox;
import Core.Controllers.GRID_Core.Grid;
import Core.Controllers.Nodes_Optimizer.Node;
import Core.Controllers.Nodes_Optimizer.Nodes_Optimizer_Controller;
import Core.Controllers.Plan_Designer.Obstruction;
import Core.Controllers.Plan_Designer.Plan_Designer_Controller;
import Core.Controllers.Plan_Designer.Zone;
import Core.Main;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;

import java.awt.*;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;

import static Core.Controllers.Nodes_Optimizer.Nodes_Optimizer_Controller.Nodes_Optimizer_zoomScale;
import static Core.Controllers.Plan_Designer.Plan_Designer_Controller.Plan_Designer_zoomScale;
import static javafx.application.Platform.exit;

public class Main_Controller implements Initializable {

    // Will hold the reference to the main controller.
    private Main main_class;
    public Stage primaryWindow;
    private String encoded_Basemap_Image, basemap_name;

    @FXML
    ImageView Project_Management_Tips_fx, Load_Map_Tip_fx;
    @FXML
    private AnchorPane work_panel_fx;
    @FXML
    private Label title_fx;
    @FXML
    public Button Resize_button_fx, test1_fx, test2_fx, new_project_fx, open_project_fx, save_project_fx,
            load_plan_fx;
    @FXML
    private AnchorPane Top_Canvas;
    @FXML
    private Button Plan_Designer_button_fx;
    @FXML
    public Button Optimize_Nodes_button_fx;
    @FXML
    private Button Settings_button_fx;
    @FXML
    private Button Minimize_button_fx;
    @FXML
    private Button Maximize_button_fx;
    @FXML
    private Button Undo_Maximize_button_fx;
    @FXML
    private Button Exit_button_fx;

    private Double x_offset_for_img_centering = null;
    private Double y_offset_for_img_centering = null;

    private BorderPane Plan_Designer_panel_fx, Optimize_Nodes_panel_fx;
    private Plan_Designer_Controller Plan_Designer_panel_fxController;
    private Nodes_Optimizer_Controller Nodes_Optimizer_panel_fxController;

    private boolean plan_designer_project_has_been_loaded = false;
    private boolean plan_has_been_loaded = false;

    private static final String css_start = "-fx-background-color: #58418e; -fx-background-image: url('/Graphics/";
    private static final String css_end = ".png') no-repeat top left;";

    private String currently_selected_scene = "Plan_Designer";

    // Styling for the main menu buttons
    private static final String PLAN_DESIGNER_BUTTON_DISABLED_STYLE = css_start + "Plan_Designer/Plan_Designer_Disabled" + css_end;
    private static final String PLAN_DESIGNER_BUTTON_DISABLED_HOVERED_STYLE = css_start + "Plan_Designer/Plan_Designer_Disabled_Selected" + css_end;
    private static final String PLAN_DESIGNER_BUTTON_ENABLED_STYLE = css_start + "Plan_Designer/Plan_Designer_Enabled" + css_end;
    private static final String PLAN_DESIGNER_BUTTON_ENABLED_HOVERED_STYLE = css_start + "Plan_Designer/Plan_Designer_Enabled_Selected" + css_end;

    private static final String OPTIMIZE_NODES_BUTTON_DISABLED_STYLE = css_start + "Optimize_Nodes/Optimize_Nodes_Disabled" + css_end;
    private static final String OPTIMIZE_NODES_BUTTON_DISABLED_HOVERED_STYLE = css_start + "Optimize_Nodes/Optimize_Nodes_Disabled_Selected" + css_end;
    private static final String OPTIMIZE_NODES_BUTTON_ENABLED_STYLE = css_start + "Optimize_Nodes/Optimize_Nodes_Enabled" + css_end;
    private static final String OPTIMIZE_NODES_BUTTON_ENABLED_HOVERED_STYLE = css_start + "Optimize_Nodes/Optimize_Nodes_Enabled_Selected" + css_end;

    private static final String SETTINGS_BUTTON_IDLE_STYLE = css_start + "Gears/Settings_Disabled" + css_end;
    private static final String SETTINGS_BUTTON_HOVERED_STYLE = css_start + "Gears/Settings_Disabled_Selected" + css_end;

    private static final String MINIMIZE_BUTTON_IDLE_STYLE = css_start + "Window_Frame_Icons/Minimize" + css_end;
    private static final String MINIMIZE_BUTTON_HOVERED_STYLE = css_start + "Window_Frame_Icons/Minimize_Selected" + css_end;
    private static final String MAXIMIZE_BUTTON_IDLE_STYLE = css_start + "Window_Frame_Icons/Maximize" + css_end;
    private static final String MAXIMIZE_BUTTON_HOVERED_STYLE = css_start + "Window_Frame_Icons/Maximize_Selected" + css_end;
    private static final String UNDO_MAXIMIZE_BUTTON_IDLE_STYLE = css_start + "Window_Frame_Icons/Undo_Maximize" + css_end;
    private static final String UNDO_MAXIMIZE_BUTTON_HOVERED_STYLE = css_start + "Window_Frame_Icons/Undo_Maximize_Selected" + css_end;
    private static final String EXIT_BUTTON_IDLE_STYLE = css_start + "Window_Frame_Icons/Exit" + css_end;
    private static final String EXIT_BUTTON_HOVERED_STYLE = css_start + "Window_Frame_Icons/Exit_Selected" + css_end;

    private static double x, y, window_width, window_height, x_button_offset_when_clicked, y_button_offset_when_clicked;

    public void closeProgram() {
        boolean exit_prompt = ConfirmBox.display("Exit Program", 270, 120,
                true, true, "Do you want to exit?");
        if (exit_prompt) {
            exit();
        }
    }

    private void minimizeWindow() {
        Stage window = (Stage) Top_Canvas.getScene().getWindow();
        window.setIconified(true);
    }

    private void maximizeWindow() {
        Stage window = (Stage) Top_Canvas.getScene().getWindow();
        Maximize_button_fx.setVisible(false);
        Undo_Maximize_button_fx.setVisible(true);
        Resize_button_fx.setVisible(false);
        window.setMaximized(true);
    }

    private void undo_maximizeWindow() {
        Stage window = (Stage) Top_Canvas.getScene().getWindow();
        Undo_Maximize_button_fx.setVisible(false);
        Maximize_button_fx.setVisible(true);
        Resize_button_fx.setVisible(true);
        window.setMaximized(false);
    }

    ////// Handle the click activity of the "Plan_Designer" button //////
    private void goto_plan_designer() {

        Load_Map_Tip_fx.setVisible(false);

        try {
            Optimize_Nodes_panel_fx.setVisible(false);
        } catch (Exception ignored) {
        }

        try {
            Plan_Designer_panel_fx.setVisible(true);
        } catch (Exception e) {
            Project_Management_Tips_fx.setVisible(true);
        }

        Plan_Designer_button_fx.setStyle(PLAN_DESIGNER_BUTTON_ENABLED_STYLE);
        Optimize_Nodes_button_fx.setStyle(OPTIMIZE_NODES_BUTTON_DISABLED_STYLE);

        currently_selected_scene = "Plan_Designer";
        load_plan_fx.setDisable(true);
        save_project_fx.setDisable(false);
        new_project_fx.setDisable(false);
        open_project_fx.setDisable(false);
    }
    ////////////

    ////// Handle the click activity of the "Optimize_Nodes" button //////
    private void goto_optimize_nodes() {

        try {
            Plan_Designer_panel_fx.setVisible(false);
            Optimize_Nodes_panel_fx.setVisible(true);
        } catch (Exception e) {
            Load_Map_Tip_fx.setVisible(true);
        }

        Optimize_Nodes_button_fx.setStyle(OPTIMIZE_NODES_BUTTON_ENABLED_STYLE);
        Plan_Designer_button_fx.setStyle(PLAN_DESIGNER_BUTTON_DISABLED_STYLE);

        currently_selected_scene = "Nodes_Optimizer";
        load_plan_fx.setDisable(false);
        save_project_fx.setDisable(true);
        new_project_fx.setDisable(true);
        open_project_fx.setDisable(true);
    }
    ////////////

    private void new_project_btn_exec() {
        // This boolean will hold the final prompt for the creation of a new project
        boolean new_project_prompt;

        // Check if a plan designer project has already been loaded
        if (plan_designer_project_has_been_loaded) {
            // Ensure that the user wants indeed to create a new project
            new_project_prompt = ConfirmBox.display("Create new Project", 300, 150,
                    true, true,
                    "Are you sure you want to create\na new Plan Designer project?\n" +
                            "Current project will be lost.");
        }
        // If no plan designer project has already been loaded, there is no need to ask the user anything
        else {
            new_project_prompt = true;
        }

        if (new_project_prompt) {

            // The user has decided to delete any previous project.
            // We re-set this flag in case the user fails to select a new floorplan image
            work_panel_fx.getChildren().remove(Plan_Designer_panel_fx);
            plan_designer_project_has_been_loaded = false;
            Project_Management_Tips_fx.setVisible(true);
            // Enable the buttons for saving current project and loading current plan for optimization
            save_project_fx.setDisable(true);
            load_plan_fx.setDisable(true);
            Optimize_Nodes_button_fx.setDisable(true);

            FileChooser fc = new FileChooser();

            // Set the valid image formats
            FileChooser.ExtensionFilter jpg_type = new FileChooser.ExtensionFilter("jpg", "*.jpg");
            FileChooser.ExtensionFilter png_type = new FileChooser.ExtensionFilter("png", "*.png");
            fc.getExtensionFilters().addAll(jpg_type, png_type);

            Stage window = (Stage) primaryWindow.getScene().getWindow();
            File fileChoice = fc.showOpenDialog(window);

            // Make sure we have indeed selected something.
            if (fileChoice != null) {
                // The user has successfully selected a floorplan. Create the new Plan_Designer_panel with this image.
                create_new_PlanDesignerProject();
                Plan_Designer_panel_fx.setVisible(true);

                set_window_size_to_default();

                // We need to run the following code on another thread,
                // so that the newly created Plan_Designer_panel_fxController has managed to load completely
                Platform.runLater(() -> {
                    try {
                        // Get the filepath and the name of the basemap image
                        String filepath = fileChoice.getAbsolutePath();
                        String raw_filename = filepath.substring(filepath.lastIndexOf("\\") + 1);
                        basemap_name = raw_filename.substring(0, raw_filename.lastIndexOf('.'));
                        title_fx.setText("Editing \"" + basemap_name + "\" basemap");

                        // Get the bytes of the basemap image and encode them
                        byte[] bytes = Files.readAllBytes(Paths.get(filepath));
                        encoded_Basemap_Image = Base64.getEncoder().encodeToString(bytes);

                        // Put the image into an Input Stream
                        InputStream Basemap_Image_inStream = new FileInputStream(filepath);

                        // Load the basemap and prepare its parent pane
                        load_basemap_img(Basemap_Image_inStream);

                        // Enable the buttons for saving current project and loading current plan for optimization
                        save_project_fx.setDisable(false);

                        System.gc(); // Try to free the Ram from the previous image

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // Mark that a new project has been loaded
                    plan_designer_project_has_been_loaded = true;
                    Project_Management_Tips_fx.setVisible(false);
                });
            }
        }
    }

    private void save_project_btn_exec() {

        // The JSON file that will hold all project information
        JSONObject project_json = new JSONObject();

        // This JSON object will hold the project's metadata
        JSONObject project_metadata = new JSONObject();
        project_metadata.put("Basemap_Name", basemap_name);
        project_metadata.put("Basemap_Data", encoded_Basemap_Image);
        project_json.put("Project_Metadata", project_metadata);

        // This JSON object will hold the obstruction data
        JSONObject obstructions = new JSONObject();
        Map<Integer, Obstruction> obstructionID_to_obstructionObject_map = Plan_Designer_panel_fxController.obstructionID_to_obstructionObject;
        for (Map.Entry<Integer, Obstruction> entry : obstructionID_to_obstructionObject_map.entrySet()) {
            obstructions.put(entry.getKey(), entry.getValue().get_as_JSON_obj());
        }
        project_json.put("Obstructions", obstructions);

        // This JSON object will hold the zone data
        JSONObject zones = new JSONObject();
        Map<Integer, Zone> zoneID_to_zoneObject_map = Plan_Designer_panel_fxController.zoneID_to_zoneObject;
        for (Map.Entry<Integer, Zone> entry : zoneID_to_zoneObject_map.entrySet()) {
            zones.put(entry.getKey(), entry.getValue().get_as_JSON_obj());
        }
        project_json.put("ZoneParts", zones);

        // This JSON object will hold the ruler data
        JSONArray ruler = new JSONArray();
        if (Plan_Designer_panel_fxController.georeference_ruler != null) {
            ruler.add(Plan_Designer_panel_fxController.georeference_ruler.real_distance.getValue() * 1000);
            ruler.add(Plan_Designer_panel_fxController.georeference_ruler.getStartX());
            ruler.add(Plan_Designer_panel_fxController.georeference_ruler.getStartY());
            ruler.add(Plan_Designer_panel_fxController.georeference_ruler.getEndX());
            ruler.add(Plan_Designer_panel_fxController.georeference_ruler.getEndY());
        }
        project_json.put("RulerData", ruler);

        Stage window = (Stage) work_panel_fx.getScene().getWindow();

        FileChooser fc = new FileChooser();

        //Set extension filter for json files
        FileChooser.ExtensionFilter json_type = new FileChooser.ExtensionFilter("Mesh optimizer project", "*.mop");
        fc.getExtensionFilters().add(json_type);

        //Show save file dialog
        File fileChoice = fc.showSaveDialog(window);

        // First make sure we have indeed selected something..
        if (fileChoice != null) {
            try {
                PrintWriter writer;
                writer = new PrintWriter(fileChoice);
                writer.println(project_json.toJSONString());
                writer.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void open_project_btn_exec() {

        // This boolean will hold the final prompt for the creation of a new project
        boolean open_project_prompt;

        // Check if a plan designer project has already been loaded
        if (plan_designer_project_has_been_loaded) {
            // Ensure that the user wants indeed to create a new project
            open_project_prompt = ConfirmBox.display("Open an existing Project", 300, 150,
                    true, true,
                    "Are you sure you want to open\nanother Plan Designer project?\n" +
                            "Current project will be lost.");
        }
        // If no plan designer project has already been loaded, there is no need to ask the user anything
        else {
            open_project_prompt = true;
        }

        if (open_project_prompt) {
            // The user has decided to delete any previous project.
            // We re-set this flag in case the user fails to select an existing project
            work_panel_fx.getChildren().remove(Plan_Designer_panel_fx);
            plan_designer_project_has_been_loaded = false;
            Project_Management_Tips_fx.setVisible(true);
            // Enable the buttons for saving current project and loading current plan for optimization
            save_project_fx.setDisable(true);
            load_plan_fx.setDisable(true);
            Optimize_Nodes_button_fx.setDisable(true);

            FileChooser fc = new FileChooser();

            // Set the valid project format
            FileChooser.ExtensionFilter project_file_format = new FileChooser.ExtensionFilter("mop", "*.mop");
            fc.getExtensionFilters().addAll(project_file_format);

            Stage window = (Stage) primaryWindow.getScene().getWindow();
            File fileChoice = fc.showOpenDialog(window);

            // Make sure we have indeed selected something.
            if (fileChoice != null) {
                // The user has successfully selected an existing project.
                // Create the new Plan_Designer_panel based on that.
                create_new_PlanDesignerProject();
                Plan_Designer_panel_fx.setVisible(true);

                set_window_size_to_default();

                // We need to run the following code on another thread,
                // so that the newly created Plan_Designer_panel_fxController has managed to load completely
                Platform.runLater(() -> {
                    try {
                        String json_path = fileChoice.getAbsolutePath();

                        JSONParser parser = new JSONParser();
                        JSONObject jsonObject = (JSONObject) parser.parse(new FileReader(json_path));

                        JSONObject obstructions = (JSONObject) jsonObject.get("Obstructions");
                        JSONObject zones = (JSONObject) jsonObject.get("ZoneParts");
                        JSONArray ruler = (JSONArray) jsonObject.get("RulerData");
                        JSONObject project_metadata = (JSONObject) jsonObject.get("Project_Metadata");

                        // Get the Basemap name from the JSON file and put it into the title
                        basemap_name = (String) project_metadata.get("Basemap_Name");
                        title_fx.setText("Editing \"" + basemap_name + "\" basemap");

                        // Get the encoded Basemap Image from the JSON file and put it into an Input Stream
                        encoded_Basemap_Image = (String) project_metadata.get("Basemap_Data");
                        byte[] decoded_basemap_bytes = Base64.getDecoder().decode(encoded_Basemap_Image);
                        InputStream Basemap_Image_inStream = new ByteArrayInputStream(decoded_basemap_bytes);

                        // Load the basemap and prepare its parent pane
                        load_basemap_img(Basemap_Image_inStream);

                        if (zones.size() != 0) {
                            load_zone_parts(zones);
                        }

                        if (ruler.size() != 0) {
                            set_georeference(ruler);
                        }

                        if (obstructions.size() != 0) {
                            load_obstructions(obstructions);
                        }

                        // Enable the buttons for saving current project and loading current plan for optimization
                        save_project_fx.setDisable(false);

                        System.gc(); // Try to free the Ram from the previous image

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // Mark that a new project has been loaded
                    plan_designer_project_has_been_loaded = true;
                    Project_Management_Tips_fx.setVisible(false);
                });
            }
        }
    }

    // This method handles the loading of current plan for the optimization
    private void load_plan_btn_exec() {

        // This boolean will hold the final prompt for the creation of a new project
        boolean load_plan_prompt;

        // Ensure that the user wants indeed to create a new project
        load_plan_prompt = ConfirmBox.display("Load current plan for optimization", 650, 250,
                true, true,
                "Be wise with the chosen options! These factors contribute exponentially to the calculation cost.\n" +
                        "\n(e.g. Finding the best position for a 1-Node setup out of all (i.e. 100%) possible positions\non a Grid with 3000 cells, requires around 30 minutes on a common i7." +
                        "\n\nTip: Try searching using the provided sample dataset for the optimal deployments\nof a 1,2 & 3-Node setups using 135cm as the Cell size and 50% for the possible positions.");

        if (load_plan_prompt) {
            // The user has decided to (re)load current plan.
            work_panel_fx.getChildren().remove(Optimize_Nodes_panel_fx);

            // Create the new Optimize_Nodes_panel based on that.
            create_new_NodesOptimizationPanel();
            Optimize_Nodes_panel_fx.setVisible(true);

            set_window_size_to_default();

            // We need to run the following code on another thread,
            // so that the newly created Nodes_Optimizer_panel_fxController has managed to load completely
            Platform.runLater(() -> {
                try {
                    prepare_parent_panes();

                    if (Plan_Designer_panel_fxController.zoneID_to_zoneObject.size() != 0) {
                        load_clean_zone_part(Plan_Designer_panel_fxController.zoneID_to_zoneObject.values(),
                                Plan_Designer_panel_fxController.zoneName_to_zoneColor);
                    }

                    if (Plan_Designer_panel_fxController.obstructionID_to_obstructionObject.size() != 0) {
                        load_clean_obstruction(Plan_Designer_panel_fxController.obstructionID_to_obstructionObject);
                    }

                    Nodes_Optimizer_panel_fxController.georeference_scale =
                            Plan_Designer_panel_fxController.georeference_ruler.getGeoreference_scale();

                    Grid.pixel_distance_at_100dbm = Grid.distance_at_100dbm / Nodes_Optimizer_panel_fxController.georeference_scale;

                    Nodes_Optimizer_panel_fxController.zoneName_to_zoneColor =
                            new HashMap<>(Plan_Designer_panel_fxController.zoneName_to_zoneColor);

                    System.gc(); // Try to free the Ram from the previous image

                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Mark that a new project has been loaded
                plan_designer_project_has_been_loaded = true;
                Project_Management_Tips_fx.setVisible(false);

            });
        }
    }

    private void load_clean_obstruction(Map<Integer, Obstruction> obstructions_map) {

        Group obstructions_view_group = new Group();

        for (Map.Entry<Integer, Obstruction> entry : obstructions_map.entrySet()) {

            // Update the boundaries of the Nodes_Optimizer panel
            if (entry.getValue().getEndX() > Nodes_Optimizer_panel_fxController.right_boundary) {
                Nodes_Optimizer_panel_fxController.right_boundary = entry.getValue().getEndX();
            }
            if (entry.getValue().getEndX() < Nodes_Optimizer_panel_fxController.left_boundary) {
                Nodes_Optimizer_panel_fxController.left_boundary = entry.getValue().getEndX();
            }

            if (entry.getValue().getEndY() > Nodes_Optimizer_panel_fxController.bottom_boundary) {
                Nodes_Optimizer_panel_fxController.bottom_boundary = entry.getValue().getEndY();
            }
            if (entry.getValue().getEndY() < Nodes_Optimizer_panel_fxController.top_boundary) {
                Nodes_Optimizer_panel_fxController.top_boundary = entry.getValue().getEndY();
            }

            Line new_obstruction = new Line(
                    entry.getValue().getStartX(), entry.getValue().getStartY(),
                    entry.getValue().getEndX(), entry.getValue().getEndY());

            // Put the obstruction on an indexed hashmap
            Nodes_Optimizer_panel_fxController.obstructionID_to_obstruction.put(
                    Nodes_Optimizer_panel_fxController.obstructionID_to_obstruction.size(), new_obstruction);

            Nodes_Optimizer_panel_fxController.obstructionID_to_obstruction_type.put(
                    Nodes_Optimizer_panel_fxController.obstructionID_to_obstruction_type.size(),
                    get_obstruction_IntegerID_from_StringType(entry.getValue().getObstruction_Type()));

            // Put the normal and the vector of the obstruction on the corresponding hashmaps

            if (entry.getValue().vertical_orientation) {
                Nodes_Optimizer_panel_fxController.obstructionID_to_normal.put(Nodes_Optimizer_panel_fxController.obstructionID_to_normal.size(), new double[]{1, 0});
                Nodes_Optimizer_panel_fxController.obstructionID_to_vector.put(Nodes_Optimizer_panel_fxController.obstructionID_to_vector.size(), new double[]{0, 1});
            } else {
                Nodes_Optimizer_panel_fxController.obstructionID_to_normal.put(Nodes_Optimizer_panel_fxController.obstructionID_to_normal.size(), new double[]{0, 1});
                Nodes_Optimizer_panel_fxController.obstructionID_to_vector.put(Nodes_Optimizer_panel_fxController.obstructionID_to_vector.size(), new double[]{1, 0});
            }
            obstructions_view_group.getChildren().add(new_obstruction);
        }

        Nodes_Optimizer_panel_fxController.clean_obstructions = obstructions_view_group;
        Nodes_Optimizer_panel_fxController.nodes_optimizer_group_fx.getChildren().add(obstructions_view_group);
    }

    private Integer get_obstruction_IntegerID_from_StringType(String type) {
        switch (type) {
            case "10cm Wall":
                return 0;
            case "40cm Wall":
                return 1;
            case "70cm Wall":
                return 2;
        }
        return null;
    }

    private void load_clean_zone_part(Collection<Zone> zones, Map<String, Color> zone_groups) {

        Group zones_view_group = new Group();

        // For every distinct zone group
        for (Map.Entry<String, Color> zone_group : zone_groups.entrySet()) {

            boolean valid_combined_zone = true;

            // Create a new polygon to represent the combination of the zone objects of same group
            org.locationtech.jts.geom.Polygon final_combined_polygon = null;

            // For every zone part
            for (Zone zone_part : zones) {
                if (zone_part.getGroupName().equals(zone_group.getKey())) {
                    org.locationtech.jts.geom.Polygon zone_part_to_jts_polygon = get_JTS_Polygon_from_JavaFX_Polygon(zone_part);
                    if (final_combined_polygon == null) {
                        final_combined_polygon = zone_part_to_jts_polygon;
                    } else {
                        Geometry combined_geometry = final_combined_polygon.union(zone_part_to_jts_polygon);
                        // If this fails, it means that the zone group is not continuous
                        try {
                            final_combined_polygon = new GeometryFactory().createPolygon(
                                    combined_geometry.getCoordinates());
                        } catch (Exception e) {
                            valid_combined_zone = false;
                            ConfirmBox.display("Zone " + zone_group.getKey() + " is not continuous..", 400, 120, true, false,
                                    "This non-continuous group will be excluded.\n" +
                                            "You can correct it and reload the plan.");

                        }
                    }
                }
            }

            if (valid_combined_zone) {
                // Create the Polygon to be drawn on the pane
                Polygon combined_zone = new Polygon();

                for (Coordinate vertex : final_combined_polygon.getCoordinates()) {
                    // Avoid closing the polygon since JavaFX closes it automatically
                    if (combined_zone.getPoints().size() != (final_combined_polygon.getCoordinates().length * 2) - 2) {
                        combined_zone.getPoints().add(vertex.x);
                        combined_zone.getPoints().add(vertex.y);

                        // Update the boundaries of the Nodes_Optimizer panel
                        if (vertex.x > Nodes_Optimizer_panel_fxController.right_boundary) {
                            Nodes_Optimizer_panel_fxController.right_boundary = vertex.x;
                        }
                        if (vertex.x < Nodes_Optimizer_panel_fxController.left_boundary) {
                            Nodes_Optimizer_panel_fxController.left_boundary = vertex.x;
                        }

                        if (vertex.y > Nodes_Optimizer_panel_fxController.bottom_boundary) {
                            Nodes_Optimizer_panel_fxController.bottom_boundary = vertex.y;
                        }
                        if (vertex.y < Nodes_Optimizer_panel_fxController.top_boundary) {
                            Nodes_Optimizer_panel_fxController.top_boundary = vertex.y;
                        }

                    }
                }

                combined_zone.setFill(zone_group.getValue());
                combined_zone.setStrokeWidth(0);


                // Put the zone attributes on indexed hashmaps
                Nodes_Optimizer_panel_fxController.zoneID_to_zonePolygon.put(
                        Nodes_Optimizer_panel_fxController.zoneID_to_zonePolygon.size(), combined_zone);

                Nodes_Optimizer_panel_fxController.zoneID_to_zoneName.put(
                        Nodes_Optimizer_panel_fxController.zoneID_to_zoneName.size(), zone_group.getKey());

                zones_view_group.getChildren().add(combined_zone);
            }
        }

        Nodes_Optimizer_panel_fxController.clean_zones = zones_view_group;
        Nodes_Optimizer_panel_fxController.nodes_optimizer_group_fx.getChildren().add(zones_view_group);
    }

    // Convert the rectangle object into a shell of coordinates object
    // that has adequate geometric calculation methods
    private org.locationtech.jts.geom.Polygon get_JTS_Polygon_from_JavaFX_Polygon(Zone zone_part) {
        // Create a basic geometry factory for the generation of the geometries
        GeometryFactory basic_geometry_factory = new GeometryFactory();

        List<Coordinate> shellCoordinates = new ArrayList<>();
        shellCoordinates.add(new Coordinate(zone_part.getX(), zone_part.getY()));
        shellCoordinates.add(new Coordinate((zone_part.getX() + zone_part.getWidth()), zone_part.getY()));
        shellCoordinates.add(new Coordinate((zone_part.getX() + zone_part.getWidth()), (zone_part.getY() + zone_part.getHeight())));
        shellCoordinates.add(new Coordinate(zone_part.getX(), (zone_part.getY() + zone_part.getHeight())));
        shellCoordinates.add(new Coordinate(zone_part.getX(), zone_part.getY()));

        LinearRing shell = basic_geometry_factory.createLinearRing(shellCoordinates.toArray(
                new Coordinate[shellCoordinates.size()]));

        // Create the Polygon based on the shell
        return basic_geometry_factory.createPolygon(shell, null);
    }

    private void load_basemap_img(InputStream Basemap_Image_inStream) {
        // Convert the non-encoded basemap image into an Image object
        Image basemap_img = new Image(Basemap_Image_inStream);

        // Below, we reduce 2 due to the unavoidable ScrollPane's borders
        double correct_ScrollPane_Width = Plan_Designer_panel_fxController.scrollable_plan_designer_fx.getWidth() - 2;
        double correct_ScrollPane_Height = Plan_Designer_panel_fxController.scrollable_plan_designer_fx.getHeight() - 2;

        // Get dimensions of the original Image
        double img_Width = basemap_img.getWidth(); // original width of the Image
        double img_Height = basemap_img.getHeight(); // original height of the Image

        // Compute the scaled width of the original Image, after fitting it vertically to the ScrollPane
        double vertical_scaling = correct_ScrollPane_Height / img_Height;
        double scaled_width = img_Width * vertical_scaling;

        // See whether the above scaled image can entirely fit within the ScrollPane
        if (correct_ScrollPane_Width >= scaled_width) {
            // Calculate the needed offset to place the image in the center
            x_offset_for_img_centering = (correct_ScrollPane_Width - scaled_width) / 2;
            y_offset_for_img_centering = null;
            Plan_Designer_panel_fxController.plan_designer_group_fx.setTranslateX(x_offset_for_img_centering);
        }
        // Otherwise, it has to be scaled differently
        else {
            // Compute the scaled height of the original Image, after fitting it horizontally to the ScrollPane
            double horizontal_scaling = correct_ScrollPane_Width / img_Width;
            double scaled_height = img_Height * horizontal_scaling;

            // Calculate the needed offset to place the image in the center
            y_offset_for_img_centering = (correct_ScrollPane_Height - scaled_height) / 2;
            x_offset_for_img_centering = null;
            Plan_Designer_panel_fxController.plan_designer_group_fx.setTranslateY(y_offset_for_img_centering);
        }

        // Add scale transform
        Plan_Designer_zoomScale = new SimpleDoubleProperty(1.0);
        // Setting the preferred size is needed for the correct zoom/pan
        Plan_Designer_panel_fxController.Edit_Plan_Designer_Pane_fx.setPrefSize(
                Plan_Designer_panel_fxController.scrollable_plan_designer_fx.getWidth(),
                Plan_Designer_panel_fxController.scrollable_plan_designer_fx.getHeight());
        Plan_Designer_panel_fxController.Edit_Plan_Designer_Pane_fx.scaleXProperty().bind(Plan_Designer_zoomScale);
        Plan_Designer_panel_fxController.Edit_Plan_Designer_Pane_fx.scaleYProperty().bind(Plan_Designer_zoomScale);

        Plan_Designer_panel_fxController.Basemap_ImageView_fx.setPreserveRatio(true);
        Plan_Designer_panel_fxController.Basemap_ImageView_fx.setFitWidth(correct_ScrollPane_Width);
        Plan_Designer_panel_fxController.Basemap_ImageView_fx.setFitHeight(correct_ScrollPane_Height);

        Plan_Designer_panel_fxController.Basemap_ImageView_fx.setImage(basemap_img);

        Plan_Designer_Controller.scaled_Img_MaxX =
                Plan_Designer_panel_fxController.Basemap_ImageView_fx.getBoundsInLocal().getMaxX();
        Plan_Designer_Controller.scaled_Img_MaxY =
                Plan_Designer_panel_fxController.Basemap_ImageView_fx.getBoundsInLocal().getMaxY();
    }

    private void prepare_parent_panes() {

        if (x_offset_for_img_centering != null) {
            Nodes_Optimizer_panel_fxController.nodes_optimizer_group_fx.setTranslateX(x_offset_for_img_centering);
        } else {
            Nodes_Optimizer_panel_fxController.nodes_optimizer_group_fx.setTranslateY(y_offset_for_img_centering);
        }

        // Add scale transform
        Nodes_Optimizer_zoomScale = new SimpleDoubleProperty(1.0);
        // Setting the preferred size is needed for the correct zoom/pan
        Nodes_Optimizer_panel_fxController.Edit_Nodes_Optimizer_Pane_fx.setPrefSize(
                Nodes_Optimizer_panel_fxController.scrollable_nodes_optimizer_fx.getWidth(),
                Nodes_Optimizer_panel_fxController.scrollable_nodes_optimizer_fx.getHeight());
        Nodes_Optimizer_panel_fxController.Edit_Nodes_Optimizer_Pane_fx.scaleXProperty().bind(Nodes_Optimizer_zoomScale);
        Nodes_Optimizer_panel_fxController.Edit_Nodes_Optimizer_Pane_fx.scaleYProperty().bind(Nodes_Optimizer_zoomScale);
    }

    private void load_zone_parts(JSONObject zones) {

        Map<String, JSONObject> ZoneParts_map = zones;
        for (Map.Entry<String, JSONObject> entry : ZoneParts_map.entrySet()) {

            JSONObject Zone_Parameters = entry.getValue();

            // Generate the new zone object
            Zone new_Zone = new Zone((double) Zone_Parameters.get("x"), (double) Zone_Parameters.get("y"),
                    (double) Zone_Parameters.get("width"), (double) Zone_Parameters.get("height"),
                    ((Long) Zone_Parameters.get("zonePart_ID")).intValue(), (String) Zone_Parameters.get("groupName"),
                    Color.web((String) Zone_Parameters.get("zonePart_Color")),
                    8 / Plan_Designer_zoomScale.getValue(),
                    Plan_Designer_panel_fxController);

            Plan_Designer_panel_fxController.zoneID_to_zoneObject.put(
                    ((Long) Zone_Parameters.get("zonePart_ID")).intValue(), new_Zone);

            Plan_Designer_panel_fxController.zone_parts_table_fx.getItems().add(new_Zone);

            // Update also the zoneName_to_zoneColor map
            Plan_Designer_panel_fxController.zoneName_to_zoneColor.put((String) Zone_Parameters.get("groupName"),
                    Color.web((String) Zone_Parameters.get("zonePart_Color")));
        }
    }

    private void load_obstructions(JSONObject obstructions) {
        Map<String, JSONObject> Obstructions_map = obstructions;
        for (Map.Entry<String, JSONObject> entry : Obstructions_map.entrySet()) {

            JSONObject Obstruction_Parameters = entry.getValue();

            // Generate the new obstruction object
            Obstruction new_Obstruction = new Obstruction(
                    (boolean) Obstruction_Parameters.get("vertical_orientation"),
                    (double) Obstruction_Parameters.get("start_x"),
                    (double) Obstruction_Parameters.get("start_y"),
                    (double) Obstruction_Parameters.get("end_x"),
                    (double) Obstruction_Parameters.get("end_y"),
                    ((Long) Obstruction_Parameters.get("obstruction_ID")).intValue(),
                    (String) Obstruction_Parameters.get("obstruction_Type"),
                    Color.web((String) Obstruction_Parameters.get("obstruction_Color")),
                    8 / Plan_Designer_zoomScale.getValue(),
                    Plan_Designer_panel_fxController);

            Plan_Designer_panel_fxController.obstructionID_to_obstructionObject.put(
                    ((Long) Obstruction_Parameters.get("obstruction_ID")).intValue(), new_Obstruction);

            Plan_Designer_panel_fxController.obstructions_table_fx.getItems().add(new_Obstruction);

            // Update also the obstructionType_to_obstructionColor map
            Plan_Designer_panel_fxController.obstructionType_to_obstructionColor.put(
                    (String) Obstruction_Parameters.get("obstruction_Type"),
                    Color.web((String) Obstruction_Parameters.get("obstruction_Color")));
        }
    }

    private void set_georeference(JSONArray ruler) {
        double[] boundaries = {(double) ruler.get(1), (double) ruler.get(2),
                (double) ruler.get(3), (double) ruler.get(4)};

        Plan_Designer_panel_fxController.confirm_georeferencing((double) ruler.get(0), boundaries);
    }

    /**
     * Set the OnMousePressed events
     */
    private void Set_OnMousePressed_Events() {

        ////// Handle the normal drag of the window //////
        Top_Canvas.setOnMousePressed(event -> {
            x = event.getSceneX();
            y = event.getSceneY();
        });

        Top_Canvas.setOnMouseDragged(event -> {
            if (Maximize_button_fx.isVisible()) {
                Top_Canvas.getScene().getWindow().setX(event.getScreenX() - x);
                Top_Canvas.getScene().getWindow().setY(event.getScreenY() - y);
            }
        });
        ////////////

        ////// Handle the custom resize of the window //////
        Resize_button_fx.setOnMousePressed(event -> {
            x_button_offset_when_clicked = Top_Canvas.getScene().getWidth() - event.getSceneX();
            y_button_offset_when_clicked = Top_Canvas.getScene().getHeight() - event.getSceneY();
        });

        Resize_button_fx.setOnMouseDragged(event -> {

            window_width = event.getSceneX() + x_button_offset_when_clicked;
            window_height = event.getSceneY() + y_button_offset_when_clicked;

            if (window_width > Top_Canvas.getPrefWidth()) {
                Top_Canvas.getScene().getWindow().setWidth(window_width);
            } else {
                Top_Canvas.getScene().getWindow().setWidth(Top_Canvas.getPrefWidth());
            }

            if (window_height > Top_Canvas.getPrefHeight()) {
                Top_Canvas.getScene().getWindow().setHeight(window_height);
            } else {
                Top_Canvas.getScene().getWindow().setHeight(Top_Canvas.getPrefHeight());
            }
        });
        ////////////

        ////// Handle the maximization/minimization of the window on double click //////
        Top_Canvas.setOnMouseClicked(event -> {
            if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
                if (Maximize_button_fx.isVisible()) {
                    maximizeWindow();
                } else {
                    undo_maximizeWindow();
                }
            }
        });
        ////////////

        ////// Handle the normal minimization of the Window //////
        Minimize_button_fx.setOnMousePressed(event -> minimizeWindow());
        ////////////

        ////// Handle the normal maximization of the Window //////
        Maximize_button_fx.setOnMousePressed(event -> maximizeWindow());
        ////////////

        ////// Handle the normal undo-maximization of the Window //////
        Undo_Maximize_button_fx.setOnMousePressed(event -> undo_maximizeWindow());
        ////////////

        ////// Handle the normal Exit of the Window //////
        Exit_button_fx.setOnMousePressed(event -> closeProgram());
        ////////////

        ////// Handle the transition to Plan Designer //////
        Plan_Designer_button_fx.setOnMousePressed(event -> goto_plan_designer());
        ////////////

        ////// Handle the transition to Nodes Optimization //////
        Optimize_Nodes_button_fx.setOnMousePressed(event -> goto_optimize_nodes());
        ////////////

        ////// Handle the events for the (new/open/save/load) buttons of the top menu bar //////
        new_project_fx.setOnMousePressed(event -> new_project_btn_exec());
        open_project_fx.setOnMousePressed(event -> open_project_btn_exec());
        save_project_fx.setOnMousePressed(event -> save_project_btn_exec());
        load_plan_fx.setOnMousePressed(event -> load_plan_btn_exec());
        ////////////

        ////// Handle the debugging events //////
        test1_fx.setOnMousePressed(event -> train_radio_propagation());
        test2_fx.setOnMousePressed(event -> train_radio_propagation_via_obstructions());
        ////////////

        Settings_button_fx.setOnMousePressed(event -> open_guide());
    }

    /**
     * Set the hover Events
     */
    private void Set_Hover_Events() {

        ////// Handle the hover activity of the "Plan_Designer" button //////
        Plan_Designer_button_fx.setStyle(PLAN_DESIGNER_BUTTON_ENABLED_STYLE);

        Plan_Designer_button_fx.setOnMouseEntered(e -> {
            Plan_Designer_button_fx.getScene().setCursor(Cursor.HAND); //Change cursor to hand

            if ("Plan_Designer".equals(currently_selected_scene)) {
                Plan_Designer_button_fx.setStyle(PLAN_DESIGNER_BUTTON_ENABLED_HOVERED_STYLE);
            } else {
                Plan_Designer_button_fx.setStyle(PLAN_DESIGNER_BUTTON_DISABLED_HOVERED_STYLE);
            }
        });

        Plan_Designer_button_fx.setOnMouseExited(e -> {
            Plan_Designer_button_fx.getScene().setCursor(Cursor.DEFAULT); //Change cursor to hand

            if ("Plan_Designer".equals(currently_selected_scene)) {
                Plan_Designer_button_fx.setStyle(PLAN_DESIGNER_BUTTON_ENABLED_STYLE);
            } else {
                Plan_Designer_button_fx.setStyle(PLAN_DESIGNER_BUTTON_DISABLED_STYLE);
            }
        });
        ////////////

        ////// Handle the hover activity of the "Optimize Nodes" button //////
        Optimize_Nodes_button_fx.setStyle(OPTIMIZE_NODES_BUTTON_DISABLED_STYLE);

        Optimize_Nodes_button_fx.setOnMouseEntered(e -> {
            Optimize_Nodes_button_fx.getScene().setCursor(Cursor.HAND); //Change cursor to hand

            if ("Nodes_Optimizer".equals(currently_selected_scene)) {
                Optimize_Nodes_button_fx.setStyle(OPTIMIZE_NODES_BUTTON_ENABLED_HOVERED_STYLE);
            } else {
                Optimize_Nodes_button_fx.setStyle(OPTIMIZE_NODES_BUTTON_DISABLED_HOVERED_STYLE);
            }
        });

        Optimize_Nodes_button_fx.setOnMouseExited(e -> {
            Optimize_Nodes_button_fx.getScene().setCursor(Cursor.DEFAULT); //Change cursor to hand

            if ("Nodes_Optimizer".equals(currently_selected_scene)) {
                Optimize_Nodes_button_fx.setStyle(OPTIMIZE_NODES_BUTTON_ENABLED_STYLE);
            } else {
                Optimize_Nodes_button_fx.setStyle(OPTIMIZE_NODES_BUTTON_DISABLED_STYLE);
            }
        });
        ////////////

        ////// Handle the hover activity of the "Settings" button //////
        Settings_button_fx.setStyle(SETTINGS_BUTTON_IDLE_STYLE);

        Settings_button_fx.setOnMouseEntered(e -> {
            Settings_button_fx.setStyle(SETTINGS_BUTTON_HOVERED_STYLE);
            Settings_button_fx.getScene().setCursor(Cursor.HAND); //Change cursor to hand
        });

        Settings_button_fx.setOnMouseExited(e -> {
            Settings_button_fx.setStyle(SETTINGS_BUTTON_IDLE_STYLE);
            Settings_button_fx.getScene().setCursor(Cursor.DEFAULT); //Change cursor to hand
        });
        ////////////

        ////// Handle the hover activity of the "Minimize" button //////
        Minimize_button_fx.setStyle(MINIMIZE_BUTTON_IDLE_STYLE);

        Minimize_button_fx.setOnMouseEntered(e -> {
            Minimize_button_fx.setStyle(MINIMIZE_BUTTON_HOVERED_STYLE);
            Minimize_button_fx.getScene().setCursor(Cursor.HAND); //Change cursor to hand
        });

        Minimize_button_fx.setOnMouseExited(e -> {
            Minimize_button_fx.setStyle(MINIMIZE_BUTTON_IDLE_STYLE);
            Minimize_button_fx.getScene().setCursor(Cursor.DEFAULT); //Change cursor to hand
        });
        ////////////

        ////// Handle the hover activity of the "Maximize" button //////
        Maximize_button_fx.setStyle(MAXIMIZE_BUTTON_IDLE_STYLE);

        Maximize_button_fx.setOnMouseEntered(e -> {
            Maximize_button_fx.setStyle(MAXIMIZE_BUTTON_HOVERED_STYLE);
            Maximize_button_fx.getScene().setCursor(Cursor.HAND); //Change cursor to hand
        });

        Maximize_button_fx.setOnMouseExited(e -> {
            Maximize_button_fx.setStyle(MAXIMIZE_BUTTON_IDLE_STYLE);
            Maximize_button_fx.getScene().setCursor(Cursor.DEFAULT); //Change cursor to hand
        });
        ////////////

        ////// Handle the hover activity of the "Undo_Maximize" button //////
        Undo_Maximize_button_fx.setStyle(UNDO_MAXIMIZE_BUTTON_IDLE_STYLE);

        Undo_Maximize_button_fx.setOnMouseEntered(e -> {
            Undo_Maximize_button_fx.setStyle(UNDO_MAXIMIZE_BUTTON_HOVERED_STYLE);
            Undo_Maximize_button_fx.getScene().setCursor(Cursor.HAND); //Change cursor to hand
        });

        Undo_Maximize_button_fx.setOnMouseExited(e -> {
            Undo_Maximize_button_fx.setStyle(UNDO_MAXIMIZE_BUTTON_IDLE_STYLE);
            Undo_Maximize_button_fx.getScene().setCursor(Cursor.DEFAULT); //Change cursor to hand
        });
        ////////////

        ////// Handle the hover activity of the "Exit" button //////
        Exit_button_fx.setStyle(EXIT_BUTTON_IDLE_STYLE);

        Exit_button_fx.setOnMouseEntered(e -> {
            Exit_button_fx.setStyle(EXIT_BUTTON_HOVERED_STYLE);
            Exit_button_fx.getScene().setCursor(Cursor.HAND); //Change cursor to hand
        });

        Exit_button_fx.setOnMouseExited(e -> {
            Exit_button_fx.setStyle(EXIT_BUTTON_IDLE_STYLE);
            Exit_button_fx.getScene().setCursor(Cursor.DEFAULT); //Change cursor to hand
        });
        ////////////
    }

    private void open_guide(){
        try {
            Desktop.getDesktop().browse(new URL("https://GIT-/Guide.pdf").toURI());
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    // This default constructor is needed for the fxmlLoader
    public Main_Controller() {
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        Set_OnMousePressed_Events();
        Set_Hover_Events();
    }

    private void create_new_PlanDesignerProject() {

        remove_PlanDesigner_Listeners();
        Plan_Designer_panel_fxController = new Plan_Designer_Controller();
        Plan_Designer_panel_fxController.main_controller = this;

        FXMLLoader Plan_Designer_panel_fx_Loader = new FXMLLoader(getClass().getResource("/Views/Plan_Designer.fxml"));
        Plan_Designer_panel_fx_Loader.setController(Plan_Designer_panel_fxController);

        try {
            Plan_Designer_panel_fx = Plan_Designer_panel_fx_Loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }

        work_panel_fx.getChildren().add(Plan_Designer_panel_fx);
    }

    private void create_new_NodesOptimizationPanel() {

        remove_NodesOptimization_Listeners();
        Nodes_Optimizer_panel_fxController = new Nodes_Optimizer_Controller();

        FXMLLoader Nodes_Optimizer_panel_fx_Loader = new FXMLLoader(getClass().getResource("/Views/Nodes_Optimizer.fxml"));
        Nodes_Optimizer_panel_fx_Loader.setController(Nodes_Optimizer_panel_fxController);

        try {
            Optimize_Nodes_panel_fx = Nodes_Optimizer_panel_fx_Loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }

        work_panel_fx.getChildren().add(Optimize_Nodes_panel_fx);
    }

    // Try to remove any previously added Listeners
    private void remove_PlanDesigner_Listeners() {

        // Try removing the zone_part_has_been_selected_in_zone_parts_table_fx_Listener from zone_parts_table_fx.getSelectionModel().selectedItemProperty()
        try {
            Plan_Designer_panel_fxController.zone_parts_table_fx.getSelectionModel().selectedItemProperty().removeListener(
                    Plan_Designer_panel_fxController.zone_part_has_been_selected_in_zone_parts_table_fx_Listener);
        } catch (Exception ignored) {
        }

        // Try removing the zones_choice_box_StringListener from zones_choice_box.valueProperty()
        try {
            Plan_Designer_panel_fxController.zones_choice_box.valueProperty().removeListener(
                    Plan_Designer_panel_fxController.zones_choice_box_StringListener);
        } catch (Exception ignored) {
        }

        // Try removing the new_zone_name_input_StringListener from new_zone_name_input.textProperty()
        try {
            Plan_Designer_panel_fxController.new_zone_name_input.textProperty().removeListener(
                    Plan_Designer_panel_fxController.new_zone_name_input_StringListener);
        } catch (Exception ignored) {
        }


        // Try removing the obstruction_has_been_selected_in_obstructions_table_fx_Listener from obstructions_table_fx.getSelectionModel().selectedItemProperty()
        try {
            Plan_Designer_panel_fxController.obstructions_table_fx.getSelectionModel().selectedItemProperty().removeListener(
                    Plan_Designer_panel_fxController.obstruction_has_been_selected_in_obstructions_table_fx_Listener);
        } catch (Exception ignored) {
        }

        // Try removing the obstruction_types_choice_box_StringListener from obstruction_type_choice_box.valueProperty()
        try {
            Plan_Designer_panel_fxController.obstruction_type_choice_box.valueProperty().removeListener(
                    Plan_Designer_panel_fxController.obstruction_types_choice_box_StringListener);
        } catch (Exception ignored) {
        }

        // Try removing the new_obstruction_type_input_StringListener from new_obstruction_type_input.textProperty()
        try {
            Plan_Designer_panel_fxController.new_obstruction_type_input.textProperty().removeListener(
                    Plan_Designer_panel_fxController.new_obstruction_type_input_StringListener);
        } catch (Exception ignored) {
        }


        // Try removing the line_length_textfield_fx_StringListener from line_length_textfield_fx.textProperty()
        try {
            Plan_Designer_panel_fxController.line_length_textfield_fx.textProperty().removeListener(
                    Plan_Designer_panel_fxController.line_length_textfield_fx_StringListener);
        } catch (Exception ignored) {
        }
    }

    // Try to remove any previously added Listeners
    private void remove_NodesOptimization_Listeners() {
        //System.out.println("Try removing old NodesOptimization listeners");
    }

    private void set_window_size_to_default() {

        // Make sure that we first revert any possible maximized window
        if (!Maximize_button_fx.isVisible()) {
            undo_maximizeWindow();
        }

        // Set the window size to the default in order to achieve a normalized scale among saves and loads
        Top_Canvas.getScene().getWindow().setHeight(720);
        Top_Canvas.getScene().getWindow().setWidth(1280);
    }






















    private void train_radio_propagation() {
        System.out.println("Training propagation");

        final double max_distance = 20000;
        final double min_distance = 500;
        final double range_diff = max_distance - min_distance;

        Group direct_line_of_sights = new Group();

        HashMap<String, Double> SamplePosBeaconPosCombo_to_RSS = parse_radio_propagation_data("radio_propagation_train_data_at10.rptd");
        HashMap<String, double[]> SamplePosID_to_SamplePos = new HashMap<>();
        HashMap<String, double[]> BeaconPosID_to_BeaconPos = new HashMap<>();

        for (Map.Entry<Integer, Zone> entry : Plan_Designer_panel_fxController.zoneID_to_zoneObject.entrySet()) {

            String[] split_id = entry.getValue().getGroupName().split("-");

            if (split_id[0].equals("S")) {
                SamplePosID_to_SamplePos.put(split_id[1], entry.getValue().centroid);
            } else if (split_id[0].equals("B")) {
                BeaconPosID_to_BeaconPos.put(split_id[1], entry.getValue().centroid);
            }
        }

        for (Map.Entry<String, double[]> sampling_position : SamplePosID_to_SamplePos.entrySet()) {
            for (Map.Entry<String, double[]> beacon_position : BeaconPosID_to_BeaconPos.entrySet()) {

                // For this Sampling Position - Beacon Position combination,
                // check all possible obstructions whether there is a collision with it

                boolean collision_found = false;

                for (Map.Entry<Integer, Obstruction> entry : Plan_Designer_panel_fxController.obstructionID_to_obstructionObject.entrySet()) {
                    if (Grid.linesIntersect(
                            sampling_position.getValue()[0], sampling_position.getValue()[1],
                            beacon_position.getValue()[0], beacon_position.getValue()[1],
                            entry.getValue().getStartX(), entry.getValue().getStartY(),
                            entry.getValue().getEndX(), entry.getValue().getEndY())) {
                        collision_found = true;
                        break;
                    }
                }
                if (!collision_found) {

                    // If we got enough RSS data gathered
                    if (SamplePosBeaconPosCombo_to_RSS.get(sampling_position.getKey() + "-" + beacon_position.getKey()) != null) {

                        double real_distance_between_points = Grid.get_real_Distance_between_Cells(
                                sampling_position.getValue()[0], sampling_position.getValue()[1],
                                beacon_position.getValue()[0], beacon_position.getValue()[1],
                                Nodes_Optimizer_panel_fxController.georeference_scale);

                        Color c1 = Color.RED;
                        Color c2 = Color.GREEN;

                        double distance_color_norm;

                        if (real_distance_between_points > max_distance) {
                            distance_color_norm = 1;
                        } else if (real_distance_between_points < min_distance) {
                            distance_color_norm = 0;
                        } else {
                            distance_color_norm = (real_distance_between_points - min_distance) / range_diff;
                        }

                        double red = c1.getRed() * distance_color_norm + c2.getRed() * (1 - distance_color_norm);
                        double green = c1.getGreen() * distance_color_norm + c2.getGreen() * (1 - distance_color_norm);
                        double blue = c1.getBlue() * distance_color_norm + c2.getBlue() * (1 - distance_color_norm);

                        Color c = Color.color(red, green, blue, 1);
                        //System.out.println(red + ", " + blue);

                        Line new_clean_line_of_sight = new Line(sampling_position.getValue()[0], sampling_position.getValue()[1],
                                beacon_position.getValue()[0], beacon_position.getValue()[1]);
                        new_clean_line_of_sight.setStroke(c);


                        direct_line_of_sights.getChildren().add(new_clean_line_of_sight);

                        /*
                        double pixel_distance_between_points = Grid.get_Pixel_Distance_between_Cells(
                                sampling_position.getValue()[0], sampling_position.getValue()[1],
                                beacon_position.getValue()[0], beacon_position.getValue()[1]);

                        double rss_based_on_pixel_distance = Grid.get_rss_based_on_pixel_distance(
                                pixel_distance_between_points, Nodes_Optimizer_panel_fxController.georeference_scale);


                        //System.out.println(sampling_position.getKey() + "-" + beacon_position.getKey());
                        System.out.println("Distance (m): " + real_distance_between_points/1000
                                + " Real:" + SamplePosBeaconPosCombo_to_RSS.get(sampling_position.getKey() + "-" + beacon_position.getKey())
                                + " Modelled:" + Grid.get_rss_based_on_real_distance(real_distance_between_points)
                                + " or " + rss_based_on_pixel_distance);
                        */
                    }
                }
            }
        }

        Nodes_Optimizer_panel_fxController.nodes_optimizer_group_fx.getChildren().add(direct_line_of_sights);
    }

    private HashMap<String, Double> parse_radio_propagation_data(String input_filepath) {

        HashMap<String, Double> SamplePosBeaconPosCombo_to_RSS = new HashMap<>();

        input_filepath = getClass().getResource("/Project_Data/" + input_filepath).getPath();

        File file = new File(input_filepath);
        try {
            Scanner input = new Scanner(file);

            while (input.hasNext()) {

                // Get the next line
                String new_line = input.nextLine();
                String[] split_line = new_line.split(";");

                double mean_rss = Double.valueOf(split_line[1]);
                SamplePosBeaconPosCombo_to_RSS.put(split_line[0], mean_rss);
            }
            input.close();

        } catch (FileNotFoundException e) {
            System.out.println("File was not found");
        }

        return SamplePosBeaconPosCombo_to_RSS;
    }



















    private void train_radio_propagation_via_obstructions() {
        HashMap<String, double[]> SamplePosID_to_SamplePos = new HashMap<>();
        HashMap<String, double[]> BeaconPosID_to_BeaconPos = new HashMap<>();

        for (Map.Entry<Integer, Zone> entry : Plan_Designer_panel_fxController.zoneID_to_zoneObject.entrySet()) {

            String[] split_id = entry.getValue().getGroupName().split("-");

            if (split_id[0].equals("S")) {
                SamplePosID_to_SamplePos.put(split_id[1], entry.getValue().centroid);
            } else if (split_id[0].equals("B")) {
                BeaconPosID_to_BeaconPos.put(split_id[1], entry.getValue().centroid);
            }
        }

        //System.out.println(SamplePosID_to_SamplePos);
        //System.out.println(BeaconPosID_to_BeaconPos);
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new FileWriter("combination_distances.csv"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (Map.Entry<String, double[]> beacon_position : BeaconPosID_to_BeaconPos.entrySet()) {
            for (Map.Entry<String, double[]> sample_position : SamplePosID_to_SamplePos.entrySet()) {

                double real_distance_between_points = Grid.get_real_Distance_between_Cells(
                        sample_position.getValue()[0], sample_position.getValue()[1],
                        beacon_position.getValue()[0], beacon_position.getValue()[1],
                        Nodes_Optimizer_panel_fxController.georeference_scale);

                if (pw != null) {
                    pw.write(beacon_position.getKey() + "_" + sample_position.getKey() + ";" + real_distance_between_points + "\n");
                }
            }
        }
        if (pw != null) {
            pw.close();
        }

        //for (Map.Entry<String, double[]> entry : BeaconPosID_to_BeaconPos.entrySet()) {
        //    System.out.println(entry.getKey() + " " + Arrays.toString(entry.getValue()));
        //    Node new_node = new Node(Integer.parseInt(entry.getKey()), entry.getValue()[0], entry.getValue()[1], 0);
        //}
    }
}
