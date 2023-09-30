package Core.Controllers.Nodes_Optimizer;

import Core.Controllers.GRID_Core.Cell;
import Core.Controllers.GRID_Core.Grid;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static Core.Controllers.GRID_Core.Grid.*;
import static java.lang.StrictMath.*;

public class Nodes_Optimizer_Controller implements Initializable {
    private final boolean train_radio_propagation_model = false;
    private final boolean save_radio_propagation_train_data = false;
    private final double real_cell_point_width = 360; // This corresponds to 30cm radius around the sampling points

    // The following data structures are used specifically for our training methodology (i.e. 60 sample points/30 Nodes)
    private Cell[] samplePoint_ID_to_sample_Cell = new Cell[60];
    private Node[] node_id_ID_to_node = new Node[30];
    private int coverage_optimization_node_candidates_counter = 0;
    private int localization_optimization_node_candidates_counter = 0;
    private Nodes_Optimizer_Controller opt_control;
    private Group coverage_optimization_node_candidates = new Group();
    private Group localization_optimization_node_candidates = new Group();
    private Group optimal_coverage_node_setup = new Group();
    private Group optimal_localization_node_setup = new Group();
    static ArrayList<Beacon> beacon_set = new ArrayList<>();
    public static ArrayList<Cell> sample_position_set = new ArrayList<>();

    @FXML
    private HBox coverage_opt_node_candidates_loading_panel_fx, localization_opt_node_candidates_loading_panel_fx,
            coverage_optimization_node_candidates_loaded_panel_fx,
            localization_optimization_node_candidates_loaded_panel_fx,
            coverage_optimization_status_fx, localization_optimization_status_fx,
            coverage_optimization_node_candidates_generation_panel_fx,
            coverage_optimization_setup_size_setter_panel_fx, localization_optimization_setup_size_setter_panel_fx;

    @FXML
    private TextField grid_size_textfield_fx, node_candidates_fx,
            setup_size_for_coverage_optimization_fx, setup_size_for_localization_optimization_fx;

    @FXML
    private Label radiomap_status_for_coverage_optimization_fx, radiomap_status_for_localization_optimization_fx,
            loaded_candidates_for_coverage_opt_fx, loaded_candidates_for_localization_opt_fx,
            coverage_optimization_progress_fx, localization_optimization_progress_fx,
            cell_size_label_fx, too_many_calculations_notice_fx, custom_localization_performance_score_fx, feature_id_fx;

    @FXML
    public ScrollPane scrollable_nodes_optimizer_fx;

    @FXML
    public Group nodes_optimizer_group_fx;

    @FXML
    public Pane Edit_Nodes_Optimizer_Pane_fx;

    @FXML
    private VBox main_editor_panel_fx, node_candidates_vbox_fx;

    @FXML
    private ToggleButton ruler_switch_fx, toggle_show_separation_features_fx, inspect_radiomap_for_coverage_optimization_fx, inspect_radiomap_for_localization_optimization_fx;

    @FXML
    private Button generate_grid_fx,
            generate_coverage_optimization_node_candidates_fx, generate_localization_optimization_node_candidates_fx,
            cancel_candidate_loader_for_coverage_opt_fx, cancel_candidate_loader_for_localization_opt_fx,
            maximize_coverage_fx, maximize_localization_fx,
            stop_coverage_optimization_fx, stop_localization_optimization_fx,
            get_rss_report_fx;
    @FXML
    Pane main_editor_toolbox_fx;

    @FXML
    Button add_new_node_fx;

    @FXML
    TabPane optimization_tab_fx;

    @FXML
    private Tab best_coverage_tab_fx, best_localization_tab_fx;

    public double[] empty_cell_receptions_1d;

    private boolean kill_coverage_optimization_radiomap_preparation_flag = false;
    private boolean kill_localization_optimization_radiomap_preparation_flag = false;
    private static boolean stop_coverage_optimization_process = false;
    static boolean stop_localization_optimization_process = false;

    // This maps will index the 4 generic cardinal orientation of the rays
    // that can be used to speed up the launching mechanism of the rays at each position
    LinkedHashMap<Integer, LinkedHashMap<Integer, Ray>> ids_of_all_launched_rays = new LinkedHashMap<>();
    private LinkedHashMap<Integer, Ray> ids_of_SE_launched_rays = new LinkedHashMap<>();
    private LinkedHashMap<Integer, Ray> ids_of_NE_launched_rays = new LinkedHashMap<>();
    private LinkedHashMap<Integer, Ray> ids_of_NW_launched_rays = new LinkedHashMap<>();
    private LinkedHashMap<Integer, Ray> ids_of_SW_launched_rays = new LinkedHashMap<>();

    private Set<Ray> final_launched_rays = new HashSet<>();

    public Group clean_zones;
    public Group clean_obstructions;

    public double georeference_scale;
    public double right_boundary = 0;
    public double bottom_boundary = 0;
    public double left_boundary = 1000000000;
    public double top_boundary = 1000000000;

    // Create a map having <Distinct Zone Names, Zone Colors> as <key, value> pairs
    public static HashMap<String, Color> zoneName_to_zoneColor = new HashMap<>();
    public HashMap<Integer, Polygon> zoneID_to_zonePolygon = new HashMap<>();
    public HashMap<Integer, String> zoneID_to_zoneName = new HashMap<>();
    public HashMap<Integer, double[]> obstructionID_to_obstruction_bounds = new HashMap<>();
    public HashMap<Integer, Integer> obstructionID_to_obstruction_type = new HashMap<>();
    public HashMap<Integer, Line> obstructionID_to_obstruction = new HashMap<>();
    public HashMap<Integer, double[]> obstructionID_to_vector = new HashMap<>();
    public HashMap<Integer, double[]> obstructionID_to_normal = new HashMap<>();

    Grid grid_base;

    private static final double DEFAULT_DELTA = 1.2d;
    public static DoubleProperty Nodes_Optimizer_zoomScale = new SimpleDoubleProperty(1.0d);

    // Create a map having <Node IDs, Node Objects> as <key, value> pairs for the Coverage Optimization
    HashMap<Integer, Node> coverage_nodeID_to_nodeObject = new HashMap<>();
    // Create a map having <Node IDs, Node Objects> as <key, value> pairs for the Localization Optimization
    HashMap<Integer, Beacon> localization_nodeID_to_nodeObject = new HashMap<>();

    //Listeners for making the scene's canvas draggable and zoomable
    private class SceneGestures {

        Nodes_Optimizer_Controller parent_controller;
        double mouseAnchorX;
        double mouseAnchorY;
        double translateAnchorX;
        double translateAnchorY;

        SceneGestures(Nodes_Optimizer_Controller parent_controller) {
            this.parent_controller = parent_controller;
        }

        EventHandler<MouseEvent> getScrollableNodesOptimizer_ClickedHandler() {
            return ScrollableNodesOptimizer_ClickedHandler;
        }

        EventHandler<MouseEvent> getScrollableNodesOptimizer_DragHandler() {
            return ScrollableNodesOptimizer_DragHandler;
        }

        EventHandler<ScrollEvent> getScrollableNodesOptimizer_ScrollHandler() {
            return ScrollableNodesOptimizer_ScrollHandler;
        }

        // This listener is the first one who receives the signal for mouse clicks
        private EventHandler<MouseEvent> ScrollableNodesOptimizer_ClickedHandler = event -> {
            // If we received a left click..
            if (event.getButton() == MouseButton.PRIMARY) {
                // Initiate the panning of the Edit_Nodes_Optimizer_Pane_fx (either during edit or pan mode)
                mouseAnchorX = event.getX();
                mouseAnchorY = event.getY();
                translateAnchorX = Edit_Nodes_Optimizer_Pane_fx.getTranslateX();
                translateAnchorY = Edit_Nodes_Optimizer_Pane_fx.getTranslateY();
            }
        };

        // This listener is the first one who receives the signal for mouse dragging
        private EventHandler<MouseEvent> ScrollableNodesOptimizer_DragHandler = event -> {
            // If we received a right-clicked or a middle-clicked drag
            if (event.getButton() == MouseButton.SECONDARY || event.getButton() == MouseButton.MIDDLE) {
                // Forget about the click because otherwise, the panel will move at the same time..
                event.consume();
            }
            // Otherwise we received a left-clicked drag
            else {
                // Pan the Edit_Nodes_Optimizer_Pane_fx
                Edit_Nodes_Optimizer_Pane_fx.setTranslateX(translateAnchorX + event.getX() - mouseAnchorX);
                Edit_Nodes_Optimizer_Pane_fx.setTranslateY(translateAnchorY + event.getY() - mouseAnchorY);

                // We have to consume this drag to avoid having other handlers reacting to it
                event.consume();
            }
        };

        private EventHandler<ScrollEvent> ScrollableNodesOptimizer_ScrollHandler = new EventHandler<>() {

            @Override
            public void handle(ScrollEvent event) {
                double scale = Nodes_Optimizer_zoomScale.get(); // currently we only use Y, same value is used for X
                double oldScale = scale;

                if (event.getDeltaY() < 0) {
                    scale /= DEFAULT_DELTA;
                } else {
                    scale *= DEFAULT_DELTA;
                }

                double f = (scale / oldScale) - 1;
                double dx = (event.getX() - (Edit_Nodes_Optimizer_Pane_fx.getBoundsInParent().getWidth() / 2 + Edit_Nodes_Optimizer_Pane_fx.getBoundsInParent().getMinX()));
                double dy = (event.getY() - (Edit_Nodes_Optimizer_Pane_fx.getBoundsInParent().getHeight() / 2 + Edit_Nodes_Optimizer_Pane_fx.getBoundsInParent().getMinY()));

                Edit_Nodes_Optimizer_Pane_fx.setTranslateX(Edit_Nodes_Optimizer_Pane_fx.getTranslateX() - (f * dx));
                Edit_Nodes_Optimizer_Pane_fx.setTranslateY(Edit_Nodes_Optimizer_Pane_fx.getTranslateY() - (f * dy));
                Nodes_Optimizer_zoomScale.set(scale);

                coverage_nodeID_to_nodeObject.values().forEach(
                        node -> node.refresh_snap_points(6 / Nodes_Optimizer_zoomScale.getValue()));

                localization_nodeID_to_nodeObject.values().forEach(
                        node -> node.refresh_snap_points(6 / Nodes_Optimizer_zoomScale.getValue()));

                event.consume();
            }
        };
    }

    private void generate_GRID() {

        // Initiate the radio propagation model train mechanism
        if (train_radio_propagation_model) {
            nodes_optimizer_group_fx.getChildren().remove(0);
            train_radio_propagation_via_obstructions();
            train_Runnable myRunnable = new train_Runnable();
            Thread t = new Thread(myRunnable);
            t.start();
        }

        Stage GRID_Generation_DialogBox = new Stage();
        GRID_Generation_DialogBox.initModality(Modality.APPLICATION_MODAL);
        GRID_Generation_DialogBox.setTitle("Generate new GRID");
        GRID_Generation_DialogBox.setMinWidth(300);
        GRID_Generation_DialogBox.setMinHeight(200);

        Label label_grid_prompt = new Label("Cell size (in cm) for the new GRID:");
        label_grid_prompt.setAlignment(Pos.CENTER);

        // Create the entry of the cell size
        TextField grid_cell_size_textfield = new TextField();
        grid_cell_size_textfield.setMaxWidth(150);

        // force the text field to be numeric only
        grid_cell_size_textfield.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                grid_cell_size_textfield.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });

        // Create the action buttons
        Button grid_create_Button = new Button("Create");
        Button grid_cancel_Button = new Button("Cancel");

        grid_create_Button.setOnAction(e -> {

            // Ensure we have an integer higher than 9
            if (!grid_cell_size_textfield.getText().equals("") && Integer.parseInt(grid_cell_size_textfield.getText()) > 9) {
                Integer int_cell_size = Integer.valueOf(grid_cell_size_textfield.getText());

                cell_size_label_fx.setText(int_cell_size.toString());

                nodes_optimizer_group_fx.getChildren().remove(grid_base);
                nodes_optimizer_group_fx.getChildren().remove(optimal_coverage_node_setup);
                nodes_optimizer_group_fx.getChildren().remove(optimal_localization_node_setup);

                grid_creation_Runnable grid_runnable = new grid_creation_Runnable(int_cell_size,
                        GRID_Generation_DialogBox, grid_cell_size_textfield);
                Thread t = new Thread(grid_runnable);
                t.start();
            } else {
                grid_cell_size_textfield.setPromptText("Wrong Cell Size");
                grid_cell_size_textfield.setText("");
            }
        });

        grid_cancel_Button.setOnAction(e -> GRID_Generation_DialogBox.close());

        // Create the HBox that will contain the action buttons
        HBox grid_action_buttons_layout = new HBox(10);
        // Add the action buttons to their parent layout
        grid_action_buttons_layout.getChildren().addAll(grid_create_Button, grid_cancel_Button);
        grid_action_buttons_layout.setAlignment(Pos.CENTER);

        VBox grid_layout = new VBox(10);
        // Add everything to the main layout
        grid_layout.getChildren().addAll(label_grid_prompt, grid_cell_size_textfield, grid_action_buttons_layout);
        grid_layout.setAlignment(Pos.CENTER);

        Scene grid_scene = new Scene(grid_layout);
        GRID_Generation_DialogBox.setScene(grid_scene);
        GRID_Generation_DialogBox.showAndWait();
    }

    private void clear_localization_optimization_candidates() {
        nodes_optimizer_group_fx.getChildren().remove(localization_optimization_node_candidates);
        localization_optimization_node_candidates = new Group();
        localization_nodeID_to_nodeObject = new HashMap<>();

        localization_optimization_node_candidates_loaded_panel_fx.setVisible(false);
        localization_opt_node_candidates_loading_panel_fx.setVisible(false);
    }

    private void clear_coverage_optimization_candidates() {
        nodes_optimizer_group_fx.getChildren().remove(coverage_optimization_node_candidates);
        coverage_optimization_node_candidates = new Group();
        coverage_nodeID_to_nodeObject = new HashMap<>();

        coverage_optimization_node_candidates_loaded_panel_fx.setVisible(false);
        coverage_opt_node_candidates_loading_panel_fx.setVisible(false);
    }

    private void generate_localization_node_candidates(){
        nodes_optimizer_group_fx.getChildren().remove(optimal_localization_node_setup);
        clear_localization_optimization_candidates();
        kill_localization_optimization_radiomap_preparation_flag = false;

        localization_optimization_node_candidates_counter = 0;
        loaded_candidates_for_localization_opt_fx.setText(
                String.valueOf(localization_optimization_node_candidates_counter));

        localization_opt_node_candidates_loading_panel_fx.setVisible(true);
        localization_optimization_setup_size_setter_panel_fx.setVisible(false);
        generate_localization_optimization_node_candidates_fx.setDisable(true);
        generate_grid_fx.setDisable(true);
        best_coverage_tab_fx.setDisable(true);
        inspect_radiomap_for_localization_optimization_fx.setSelected(false);
        toggle_show_separation_features_fx.setDisable(true);
        toggle_show_separation_features_fx.setSelected(false);

        localization_optimization_radiomap_builder_waiter localization_radiomap_executor = new localization_optimization_radiomap_builder_waiter();
        Thread t = new Thread(localization_radiomap_executor);
        t.start();
    }

    private void generate_coverage_node_candidates() {
        if (!"".equals(node_candidates_fx.getText())) {

            nodes_optimizer_group_fx.getChildren().remove(optimal_coverage_node_setup);
            clear_coverage_optimization_candidates();
            kill_coverage_optimization_radiomap_preparation_flag = false;

            coverage_optimization_node_candidates_counter = 0;
            loaded_candidates_for_coverage_opt_fx.setText(
                    String.valueOf(coverage_optimization_node_candidates_counter));

            coverage_opt_node_candidates_loading_panel_fx.setVisible(true);
            coverage_optimization_setup_size_setter_panel_fx.setVisible(false);
            generate_coverage_optimization_node_candidates_fx.setDisable(true);
            generate_grid_fx.setDisable(true);
            best_localization_tab_fx.setDisable(true);
            inspect_radiomap_for_coverage_optimization_fx.setSelected(false);

            coverage_optimization_radiomap_builder_waiter radiomap_executor = new coverage_optimization_radiomap_builder_waiter();
            Thread t = new Thread(radiomap_executor);
            t.start();
        }
    }

    private void debug_test1() {
    }

    private void compute_radial_positions_offsets() {
        for (int i = 0; i < number_of_radial_rays; i++) {
            double angle = (2 * PI / number_of_radial_rays) * i;
            double rotated_x = (pixel_distance_at_100dbm) * sin(angle);
            double rotated_y = (pixel_distance_at_100dbm) * cos(angle);

            // This special unit ray will hold the x,y offset from the origin (0, 0) point
            Ray new_ray = new Ray(new int[]{0, 0, 0}, 0, 0, rotated_x, rotated_y,
                    null, new int[]{0, 0, 0}, null, 0);

            new_ray.offset_x = rotated_x;
            new_ray.offset_y = rotated_y;

            // Check whether the new rotated point is below the origin launching point
            if (rotated_y > 0) {
                if (rotated_x > 0) {
                    ids_of_SE_launched_rays.put(i, new_ray);
                    ids_of_all_launched_rays.put(i, ids_of_SE_launched_rays);
                } else {
                    ids_of_SW_launched_rays.put(i, new_ray);
                    ids_of_all_launched_rays.put(i, ids_of_SW_launched_rays);
                }
            }
            // Being here it means that the new rotated point is above the origin launching point
            else {
                if (rotated_x > 0) {
                    ids_of_NE_launched_rays.put(i, new_ray);
                    ids_of_all_launched_rays.put(i, ids_of_NE_launched_rays);
                } else {
                    ids_of_NW_launched_rays.put(i, new_ray);
                    ids_of_all_launched_rays.put(i, ids_of_NW_launched_rays);
                }
            }
        }
    }

    private void compute_obstruction_bounds() {

        for (Map.Entry<Integer, Line> entry : obstructionID_to_obstruction.entrySet()) {
            double minX = min(entry.getValue().getStartX(), entry.getValue().getEndX());
            double maxX = max(entry.getValue().getStartX(), entry.getValue().getEndX());
            double minY = min(entry.getValue().getStartY(), entry.getValue().getEndY());
            double maxY = max(entry.getValue().getStartY(), entry.getValue().getEndY());

            obstructionID_to_obstruction_bounds.put(entry.getKey(), new double[]{minX, maxX, minY, maxY});
        }
    }

    private void launch_parent_rays_from_node(Node node) {
        // These data structures will hold all the produced Rays
        ArrayList<Ray> launched_rays = new ArrayList<>();
        for (int i = 0; i < number_of_radial_rays; i++) {
            double rotated_x = node.getCenterX() + ids_of_all_launched_rays.get(i).get(i).offset_x;
            double rotated_y = node.getCenterY() + ids_of_all_launched_rays.get(i).get(i).offset_y;

            Ray new_ray = new Ray(new int[]{0, 0, 0}, node.getCenterX(), node.getCenterY(), rotated_x, rotated_y,
                    null, new int[]{0, 0, 0}, null, 0);

            launched_rays.add(new_ray);
        }
        recursively_launch_child_rays(launched_rays, node);
    }

    private void recursively_launch_child_rays(ArrayList<Ray> launched_rays, Node node) {
        ArrayList<Ray> new_launched_rays = new ArrayList<>();
        // The first time we come here, the parent rays shall be iterated
        // On the last time we come here, the rays that will be iterated will have no other child rays
        // Iterate all launched rays
        for (Ray ray : launched_rays) {

            // Find all obstructions that intersect this ray
            // and calculate the intersection point and the distance from the launching source point
            LinkedHashMap<Integer, double[]> ordered_by_distance_intersected_obstructions =
                    getOrderedIntersectedObstructions(ray);

            for (int c = 0; c < samplePoint_ID_to_sample_Cell.length; c++) {
                // Ensure that this ray does not come from a node that can directly reach this cell
                // (Since we have an FSPL model for these cases)
                if (!node.sample_points_with_direct_sight.contains(c)) {
                    // Check whether currently iterated Ray and SamplePoint intersect
                    if (ray.reaches_cell(samplePoint_ID_to_sample_Cell[c])) {
                        // If they do, we might want to consider this Ray as a candidate for training the Radio Propagation Model
                        //First calculate the distance between the Sample Point and the origin of the Ray
                        double distance_between_SamplePoint_and_Ray = get_Pixel_Distance_between_Cells(
                                ray.getStartX(), ray.getStartY(), samplePoint_ID_to_sample_Cell[c].getX_Centroid(),
                                samplePoint_ID_to_sample_Cell[c].getY_Centroid());

                        short[] properties = new short[7];
                        properties[0] = toUint16(ray.refractions_already_occurred[0]);
                        properties[1] = toUint16(ray.refractions_already_occurred[1]);
                        properties[2] = toUint16(ray.refractions_already_occurred[2]);
                        properties[3] = toUint16(ray.reflections_already_occurred[0]);
                        properties[4] = toUint16(ray.reflections_already_occurred[1]);
                        properties[5] = toUint16(ray.reflections_already_occurred[2]);
                        properties[6] = toUint16((int) ((distance_between_SamplePoint_and_Ray +
                                ray.traversed_distance_from_node) * georeference_scale));

                        for (Map.Entry<Integer, double[]> obstruction_property :
                                ordered_by_distance_intersected_obstructions.entrySet()) {
                            if (obstruction_property.getValue()[2] < distance_between_SamplePoint_and_Ray) {
                                properties[obstructionID_to_obstruction_type.get(obstruction_property.getKey())] =
                                        toUint16(toUint32(properties[obstructionID_to_obstruction_type.get(
                                                obstruction_property.getKey())]) + 1);
                            }
                        }

                        ArrayList<short[]> node_cell_connectivity_properties = node.connectivity_properties.get(c);

                        if (node_cell_connectivity_properties != null) {
                            node.connectivity_properties.get(c).add(properties);
                        } else {
                            node_cell_connectivity_properties = new ArrayList<>();
                            node_cell_connectivity_properties.add(properties);
                            node.connectivity_properties.put(c, node_cell_connectivity_properties);
                        }

                        // The snippet bellow is heavy it is only for visualising the Rays
/*
                        ArrayList<Ray> cells_that_are_indirectly_reachable_from_this_node = node.reachable_cells.get(c);

                        if (cells_that_are_indirectly_reachable_from_this_node != null){
                            node.reachable_cells.get(c).add(ray);
                        }
                        else {
                            cells_that_are_indirectly_reachable_from_this_node = new ArrayList<>();
                            cells_that_are_indirectly_reachable_from_this_node.add(ray);
                            node.reachable_cells.put(c, cells_that_are_indirectly_reachable_from_this_node);
                        }
*/
                    }
                }
            }

            // If any intersection with an obstruction has been found
            if (ordered_by_distance_intersected_obstructions.size() != 0) {
                for (Map.Entry<Integer, double[]> entry : ordered_by_distance_intersected_obstructions.entrySet()) {
                    Ray new_reflected_ray = ray.get_reflected_ray(ray.refractions_already_occurred.clone(), entry,
                            obstructionID_to_normal.get(entry.getKey()),
                            obstructionID_to_obstruction_type.get(entry.getKey()));
                    new_launched_rays.add(new_reflected_ray);

                    ray.refractions_already_occurred[obstructionID_to_obstruction_type.get(entry.getKey())] =
                            ray.refractions_already_occurred[obstructionID_to_obstruction_type.get(entry.getKey())] + 1;
                }
            }
        }

        if (new_launched_rays.size() > 0) {
            recursively_launch_child_rays(new_launched_rays, node);
        }
    }

    // Find the IDs of all the obstructions (in obstructionID_to_obstruction) that intersect this Ray
    private LinkedHashMap<Integer, double[]> getOrderedIntersectedObstructions(Ray ray) {

        Map<Integer, double[]> intersected_obstructions_points = new HashMap<>();
        Map<Integer, Double> intersected_obstructions_distances = new HashMap<>();

        for (Map.Entry<Integer, Line> entry : obstructionID_to_obstruction.entrySet()) {

            // Avoid considering the obstruction from which the ray has bounced
            if (!entry.getKey().equals(ray.obstruction_source)) {
                double[] intersection_details = ray.getRayIntersection(entry.getValue());

                if (intersection_details != null) {
                    intersected_obstructions_points.put(entry.getKey(), intersection_details);
                    intersected_obstructions_distances.put(entry.getKey(), get_Pixel_Distance_between_Cells(
                            ray.getStartX(), ray.getStartY(), intersection_details[0], intersection_details[1]));
                }
            }
        }

        // The information that the Map values shall hold are: intersection_x, intersection_y, distance
        LinkedHashMap<Integer, double[]> intersected_obstructions_ordered_by_distance = new LinkedHashMap<>();

        Map<Integer, Double> obstructions_sorted_by_distance = intersected_obstructions_distances.entrySet()
                .stream()
                .sorted((Map.Entry.comparingByValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        // Check any possible intersections with all the obstructions first
        for (Map.Entry<Integer, Double> entry : obstructions_sorted_by_distance.entrySet()) {
            intersected_obstructions_ordered_by_distance.put(entry.getKey(), new double[]{
                    intersected_obstructions_points.get(entry.getKey())[0],
                    intersected_obstructions_points.get(entry.getKey())[1],
                    entry.getValue()});
        }

        return intersected_obstructions_ordered_by_distance;
    }

    private void train_radio_propagation_via_obstructions() {
        prepare_sample_cells();
        prepare_nodes();
        compute_direct_node_sample_point_sights();
        compute_radial_positions_offsets();
    }

    private void visualise_rays() {
        int total_values = 0;

        for (Node node : node_id_ID_to_node) {
            for (int c = 0; c < samplePoint_ID_to_sample_Cell.length; c++) {
                ArrayList<Ray> rays = node.reachable_cells.get(c);
                if (rays != null) {

                    final_launched_rays.addAll(node.reachable_cells.get(c));
                }

                ArrayList<short[]> properties = node.connectivity_properties.get(c);
                if (properties != null) {
                    total_values += node.connectivity_properties.get(c).size();
                    System.out.println("Node: " + node.nodeID + " Cell: " + c + " Values: " + node.connectivity_properties.get(c).size());
                }
            }
        }

        Group launched_ray_nodes = new Group();
        launched_ray_nodes.getChildren().addAll(final_launched_rays);
        nodes_optimizer_group_fx.getChildren().add(0, launched_ray_nodes);

        if (save_radio_propagation_train_data) {
            try {
                PrintWriter writer;
                writer = new PrintWriter("radio_train_data.csv");
                for (Node node : node_id_ID_to_node) {
                    for (int c = 0; c < samplePoint_ID_to_sample_Cell.length; c++) {
                        ArrayList<short[]> ray_properties = node.connectivity_properties.get(c);
                        if (ray_properties != null) {
                            for (short[] properties : ray_properties) {
                                writer.println(node.nodeID + 1 + "-" + (c + 1) + ";" + toUint32(properties[0]) + ";" + toUint32(properties[1]) + ";" + toUint32(properties[2]) + ";" + toUint32(properties[3]) + ";" + toUint32(properties[4]) + ";" + toUint32(properties[5]) + ";" + toUint32(properties[6]));
                            }
                        }
                    }
                }
                writer.close();
            } catch (IOException ignored) {
            }
        }

        System.out.println(total_values);
    }

    private void compute_direct_node_sample_point_sights() {

        for (int n = 0; n < node_id_ID_to_node.length; n++) {
            for (int c = 0; c < samplePoint_ID_to_sample_Cell.length; c++) {

                // For this Sampling Position - Beacon Position combination,
                // check all possible obstructions whether there is a collision with it
                boolean collision_found = false;

                for (Map.Entry<Integer, Line> entry : obstructionID_to_obstruction.entrySet()) {
                    if (Grid.linesIntersect(
                            // This is the centroid of the sampling_position
                            samplePoint_ID_to_sample_Cell[c].getX_Centroid(), samplePoint_ID_to_sample_Cell[c].getY_Centroid(),
                            // This is the centroid of the beacon_position
                            node_id_ID_to_node[n].getCenterX(), node_id_ID_to_node[n].getCenterY(),
                            entry.getValue().getStartX(), entry.getValue().getStartY(),
                            entry.getValue().getEndX(), entry.getValue().getEndY())) {

                        collision_found = true;
                        break;
                    }
                }
                if (!collision_found) {
                    node_id_ID_to_node[n].sample_points_with_direct_sight.add(c);
                }
            }
        }
    }

    private void prepare_nodes() {
        Group nodes_javafx_objects = new Group();

        // For every node, get its centroid and generate a new node object
        // which will be used for the intersection checks with the radio rays
        for (Map.Entry<Integer, Polygon> entry : zoneID_to_zonePolygon.entrySet()) {

            String[] split_id = zoneID_to_zoneName.get(entry.getKey()).split("-");

            if (split_id[0].equals("B")) {

                int node_id = Integer.parseInt(split_id[1]) - 1;

                double top_left_node_zone_x = entry.getValue().getPoints().get(0);
                double top_left_node_zone_y = entry.getValue().getPoints().get(1);
                double bottom_left_node_zone_x = entry.getValue().getPoints().get(4);
                double bottom_left_node_zone_y = entry.getValue().getPoints().get(5);

                double node_zone_width = bottom_left_node_zone_x - top_left_node_zone_x;
                double node_zone_height = bottom_left_node_zone_y - top_left_node_zone_y;

                double node_centroid_x = top_left_node_zone_x + node_zone_width / 2;
                double node_centroid_y = top_left_node_zone_y + node_zone_height / 2;

                double node_width = (real_cell_point_width / georeference_scale) / 2;

                Node node = new Node(node_id, node_centroid_x, node_centroid_y, node_width);

                node_id_ID_to_node[node_id] = node;
                nodes_javafx_objects.getChildren().add(node);
            }
        }

        nodes_optimizer_group_fx.getChildren().add(nodes_javafx_objects);
    }

    private void prepare_sample_cells() {
        Group sample_cells_javafx_objects = new Group();

        // For every sample point, get its centroid and generate a new cell
        // which will be used for the intersection checks with the radio rays
        for (Map.Entry<Integer, Polygon> entry : zoneID_to_zonePolygon.entrySet()) {

            String[] split_id = zoneID_to_zoneName.get(entry.getKey()).split("-");

            if (split_id[0].equals("S")) {

                int sample_cell_id = Integer.parseInt(split_id[1]) - 1;

                double top_left_sampling_point_zone_x = entry.getValue().getPoints().get(0);
                double top_left_sampling_point_zone_y = entry.getValue().getPoints().get(1);
                double bottom_left_sampling_point_zone_x = entry.getValue().getPoints().get(4);
                double bottom_left_sampling_point_zone_y = entry.getValue().getPoints().get(5);

                double sampling_point_zone_width = bottom_left_sampling_point_zone_x - top_left_sampling_point_zone_x;
                double sampling_point_zone_height = bottom_left_sampling_point_zone_y - top_left_sampling_point_zone_y;

                double sampling_point_centroid_x = top_left_sampling_point_zone_x + sampling_point_zone_width / 2;
                double sampling_point_centroid_y = top_left_sampling_point_zone_y + sampling_point_zone_height / 2;

                double pixel_cell_point_width = real_cell_point_width / georeference_scale;

                double top_left_sampling_point_cell_x = sampling_point_centroid_x - pixel_cell_point_width / 2;
                double top_left_sampling_point_cell_y = sampling_point_centroid_y - pixel_cell_point_width / 2;
                double bottom_right_sampling_point_cell_x = sampling_point_centroid_x + pixel_cell_point_width / 2;
                double bottom_right_sampling_point_cell_y = sampling_point_centroid_y + pixel_cell_point_width / 2;

                Cell sample_cell = new Cell(top_left_sampling_point_cell_x, top_left_sampling_point_cell_y,
                        bottom_right_sampling_point_cell_x, bottom_right_sampling_point_cell_y,
                        sampling_point_centroid_x, sampling_point_centroid_y, pixel_cell_point_width, sample_cell_id);

                samplePoint_ID_to_sample_Cell[sample_cell_id] = sample_cell;
                sample_cells_javafx_objects.getChildren().add(sample_cell);
            }
        }

        nodes_optimizer_group_fx.getChildren().add(sample_cells_javafx_objects);
    }

    private static short toUint16(int i) {
        return (short) i;
    }

    private static int toUint32(short s) {
        return s & 0xFFFF;
    }

    private void kill_coverage_opt_radiomap_builder() {
        kill_coverage_optimization_radiomap_preparation_flag = true;
        coverage_opt_node_candidates_loading_panel_fx.setVisible(false);
    }

    private void kill_localization_opt_radiomap_builder(){
        kill_localization_optimization_radiomap_preparation_flag = true;
        localization_opt_node_candidates_loading_panel_fx.setVisible(false);
    }

    private void stop_coverage_optimization() {
        stop_coverage_optimization_process = true;
        coverage_optimization_status_fx.setVisible(false);
    }

    private void stop_localization_optimization() {
        stop_localization_optimization_process = true;
        localization_optimization_status_fx.setVisible(false);
    }

    public class train_Runnable implements Runnable {
        public void run() {
            // We use 30 below since the number of nodes are 30
            //launch_parent_rays_from_node(node_id_ID_to_node[1]);
            for (int n = 0; n < 30; n++) {
                launch_parent_rays_from_node(node_id_ID_to_node[n]);
                System.gc();
            }

            Platform.runLater(() -> {
                //visualise_rays();
            });
        }
    }

    public class grid_creation_Runnable implements Runnable {
        private Integer cell_size;
        private Stage GRID_Generation_DialogBox;
        private TextField grid_cell_size_textfield;

        grid_creation_Runnable(Integer cell_size, Stage GRID_Generation_DialogBox, TextField grid_cell_size_textfield) {
            this.cell_size = cell_size;
            this.GRID_Generation_DialogBox = GRID_Generation_DialogBox;
            this.grid_cell_size_textfield = grid_cell_size_textfield;
        }

        public void run() {
            // The input cell size is in mm
            grid_base = new Grid(cell_size * 10, georeference_scale, opt_control,
                    inspect_radiomap_for_localization_optimization_fx, feature_id_fx);

            if (grid_base.total_number_of_cells < 6000 & grid_base.total_number_of_cells > 100) {
                compute_radial_positions_offsets();
                compute_obstruction_bounds();
                grid_base.prepare_extra_zones();

                Platform.runLater(() -> {
                    coverage_optimization_setup_size_setter_panel_fx.setVisible(false);
                    optimization_tab_fx.setDisable(false);
                    nodes_optimizer_group_fx.getChildren().add(0, grid_base);
                    nodes_optimizer_group_fx.getChildren().remove(clean_zones);
                    GRID_Generation_DialogBox.close();

                    clear_coverage_optimization_candidates();
                    coverage_optimization_node_candidates_generation_panel_fx.setVisible(true);
                });
            } else {
                Platform.runLater(() -> {
                    grid_cell_size_textfield.setPromptText("Choose another Cell Size");
                    grid_cell_size_textfield.setText("");
                    cell_size_label_fx.setText("");
                });
            }
        }
    }

    public class coverage_optimization_radiomap_builder_waiter implements Runnable {
        public void run() {
            double homogeneous_zoom = 6 / Nodes_Optimizer_zoomScale.getValue();
            int node_candidates_percentage = Integer.parseInt(node_candidates_fx.getText());
            double estimated_number_of_node_candidates = (grid_base.total_number_of_cells * node_candidates_percentage) / 100d;

            double iter_step = grid_base.total_number_of_cells / estimated_number_of_node_candidates;

            // Create an array that will hold the 6 lists with all node candidates.
            // Each list will be used from a single thead
            ArrayList[] node_candidates_thread_lists = new ArrayList[]{
                    new ArrayList<Cell>(), new ArrayList<Cell>(), new ArrayList<Cell>(),
                    new ArrayList<Cell>(), new ArrayList<Cell>(), new ArrayList<Cell>()};

            // Create an Array list that will include the chosen amount of Cells which will be equally divided in space
            ArrayList<Cell> final_candidates_list = new ArrayList<>();
            for (double cell_index = 0; cell_index < grid_base.all_cells_list.size(); cell_index = cell_index + iter_step) {
                final_candidates_list.add(grid_base.all_cells_list.get((int) cell_index));
            }

            // Divide the iterated cells to the 6 corresponding thread lists
            for (int cell_index = 0; cell_index < final_candidates_list.size(); cell_index++) {
                node_candidates_thread_lists[cell_index % 6].add(final_candidates_list.get(cell_index));
            }

            long start_time = System.currentTimeMillis();

            ExecutorService es = Executors.newCachedThreadPool();
            for (int i = 0; i < 6; i++)
                es.execute(new coverage_optimization_radiomap_builder_worker(node_candidates_thread_lists[i], homogeneous_zoom));
            es.shutdown();

            try {
                es.awaitTermination(1, TimeUnit.DAYS);
                generate_grid_fx.setDisable(false);
                best_localization_tab_fx.setDisable(false);
                generate_coverage_optimization_node_candidates_fx.setDisable(false);

                if (!kill_coverage_optimization_radiomap_preparation_flag) {
                    long end_time = System.currentTimeMillis();
                    System.out.println("Execution time (s): " + (end_time - start_time));
                    Platform.runLater(() -> {
                        // Add all node candidates to the group for later visualisation
                        best_localization_tab_fx.setDisable(false);
                        coverage_optimization_setup_size_setter_panel_fx.setDisable(false);
                        coverage_optimization_setup_size_setter_panel_fx.setVisible(true);
                        coverage_opt_node_candidates_loading_panel_fx.setVisible(false);
                        coverage_optimization_node_candidates_loaded_panel_fx.setVisible(true);
                        coverage_optimization_node_candidates.setVisible(false);

                        coverage_optimization_node_candidates.getChildren().addAll(coverage_nodeID_to_nodeObject.values());
                        nodes_optimizer_group_fx.getChildren().add(coverage_optimization_node_candidates);

                        // At this point we have updated all cells for this node. Update the colors.
                        radiomap_status_for_coverage_optimization_fx.setText(
                                "Radiomap ready for\n" + coverage_optimization_node_candidates_counter + " node candidates");
                    });
                }
            } catch (Exception ignored) {
            }
        }
    }

    public class coverage_optimization_radiomap_builder_worker implements Runnable {
        private ArrayList<Cell> node_positions;
        private double homogeneous_zoom;

        coverage_optimization_radiomap_builder_worker(ArrayList<Cell> node_positions, double homogeneous_zoom) {
            this.node_positions = node_positions;
            this.homogeneous_zoom = homogeneous_zoom;
        }

        public void run() {
            for (Cell node_position : node_positions) {
                if (!kill_coverage_optimization_radiomap_preparation_flag) {
                    Node new_node_to_check = new Node(node_position.grid_index_int_1d,
                            node_position.getX_Centroid(), node_position.getY_Centroid(),
                            homogeneous_zoom, grid_base.all_cells_2D_mapping, grid_base.all_cells_list.size(),
                            grid_base.all_cells_list, opt_control);

                    new_node_to_check.launch_parent_rays_from_node(null);
                    coverage_nodeID_to_nodeObject.put(node_position.grid_index_int_1d, new_node_to_check);
                    coverage_optimization_node_candidates_counter++;

                    Platform.runLater(() -> {
                        loaded_candidates_for_coverage_opt_fx.setText(String.valueOf(coverage_optimization_node_candidates_counter));
                    });
                }
            }
        }
    }

    public class localization_optimization_radiomap_builder_waiter implements Runnable {
        public void run() {
            beacon_set = new ArrayList<>();
            double homogeneous_zoom = 6 / Nodes_Optimizer_zoomScale.getValue();

            // Create an array that will hold the 6 lists with all node candidates.
            // Each list will be used from a single thead
            ArrayList[] localization_node_candidates_thread_lists = new ArrayList[]{
                    new ArrayList<Cell>(), new ArrayList<Cell>(), new ArrayList<Cell>(),
                    new ArrayList<Cell>(), new ArrayList<Cell>(), new ArrayList<Cell>()
            };

            cells_for_which_to_generate_radiomap = Stream.of(cells_of_sampled_positions_in_interior_list, grid_base.zone_border_cell_list)
                    .flatMap(List::stream).collect(Collectors.toList());

            // Divide the iterated cells to the 6 corresponding thread lists
            for (int cell_index = 0; cell_index < cells_for_which_to_generate_radiomap.size(); cell_index++) {
                localization_node_candidates_thread_lists[cell_index % 6].add(cells_for_which_to_generate_radiomap.get(cell_index));
            }

            ExecutorService es = Executors.newCachedThreadPool();
            for (int i = 0; i < 6; i++)
                es.execute(new localization_optimization_radiomap_builder_worker(localization_node_candidates_thread_lists[i], homogeneous_zoom));
            es.shutdown();

            try {
                es.awaitTermination(10, TimeUnit.DAYS);
                generate_grid_fx.setDisable(false);
                best_coverage_tab_fx.setDisable(false);
                generate_localization_optimization_node_candidates_fx.setDisable(false);
                toggle_show_separation_features_fx.setDisable(false);

                if (!kill_localization_optimization_radiomap_preparation_flag) {
                    Platform.runLater(() -> {
                        // Add all node candidates to the group for later visualisation
                        best_coverage_tab_fx.setDisable(false);
                        localization_optimization_setup_size_setter_panel_fx.setDisable(false);
                        localization_optimization_setup_size_setter_panel_fx.setVisible(true);
                        localization_opt_node_candidates_loading_panel_fx.setVisible(false);
                        localization_optimization_node_candidates_loaded_panel_fx.setVisible(true);
                        localization_optimization_node_candidates.setVisible(false);

                        localization_optimization_node_candidates.getChildren().addAll(localization_nodeID_to_nodeObject.values());
                        nodes_optimizer_group_fx.getChildren().add(localization_optimization_node_candidates);

                        // At this point we have updated all cells for this node. Update the colors.
                        radiomap_status_for_localization_optimization_fx.setText(
                                "Radiomap ready for\n" + localization_optimization_node_candidates_counter + " possible places");

                        new GA_Optimizer(grid_base.zone_border_cell_list, grid_base.interconnections_list,
                                localization_nodeID_to_nodeObject, grid_base.all_cells_list);

                    });
                }
            } catch (Exception ignored) {
            }
        }
    }

    public class localization_optimization_radiomap_builder_worker implements Runnable {
        private ArrayList<Cell> node_positions_chunk;
        private double homogeneous_zoom;

        localization_optimization_radiomap_builder_worker(ArrayList<Cell> node_positions_chunk, double homogeneous_zoom) {
            this.node_positions_chunk = node_positions_chunk;
            this.homogeneous_zoom = homogeneous_zoom;
        }

        public void run() {
            for (Cell node_position : node_positions_chunk) {
                if (!kill_localization_optimization_radiomap_preparation_flag) {

                    Beacon new_node_to_check = new Beacon(node_position.grid_index_int_1d,
                            node_position.getX_Centroid(), node_position.getY_Centroid(),
                            homogeneous_zoom, grid_base.all_cells_2D_mapping, grid_base.all_cells_list.size(),
                            (ArrayList<Cell>) cells_for_which_to_generate_radiomap, opt_control,
                            inspect_radiomap_for_localization_optimization_fx,
                            custom_localization_performance_score_fx, feature_id_fx);

                    new_node_to_check.launch_parent_rays_from_node(grid_base.separation_zone_border_cell_list);
                    localization_nodeID_to_nodeObject.put(node_position.grid_index_int_1d, new_node_to_check);
                    localization_optimization_node_candidates_counter++;

                    Platform.runLater(() -> {
                        loaded_candidates_for_localization_opt_fx.setText(
                                String.valueOf(localization_optimization_node_candidates_counter));
                    });
                }
            }
        }
    }

    public class optimize_coverage_waiter implements Runnable {
        public void run() {
            ExecutorService es = Executors.newCachedThreadPool();
            es.execute(new optimize_coverage_worker());
            es.shutdown();
            try {
                es.awaitTermination(24, TimeUnit.DAYS);
                Platform.runLater(() -> {
                    best_localization_tab_fx.setDisable(false);
                    node_candidates_vbox_fx.setDisable(false);
                    maximize_coverage_fx.setDisable(false);
                    main_editor_toolbox_fx.setDisable(false);
                    coverage_optimization_status_fx.setVisible(false);
                });
            } catch (Exception ignored) {
            }
        }
    }

    public class optimize_coverage_worker implements Runnable {

        public void run() {
            int setup_size = Integer.parseInt(setup_size_for_coverage_optimization_fx.getText());

            // Calculate the estimated calculations
            BigInteger predicted_combinations = predict_combinations(coverage_nodeID_to_nodeObject.keySet().size(), setup_size);
            BigInteger total_calculations = predicted_combinations.multiply(BigInteger.valueOf(grid_base.total_number_of_cells));

            // Set a threshold of calculations
            BigInteger predicted_combinations_threshold = BigInteger.valueOf(50000000L);
            BigInteger total_calculations_threshold = BigInteger.valueOf(5000000000L);

            int previous_percentage_status = 0;

            if ((predicted_combinations_threshold.compareTo(predicted_combinations) > 0) &&
                    (total_calculations_threshold.compareTo(total_calculations) > 0)) {

                int[] nodeIDs_array = new int[coverage_nodeID_to_nodeObject.keySet().size()];
                int array_iter = 0;
                for (int nodeID : coverage_nodeID_to_nodeObject.keySet()) {
                    nodeIDs_array[array_iter] = nodeID;
                    array_iter++;
                }

                ArrayList<ArrayList<Integer>> combinations =
                        Combination.getCombination(nodeIDs_array, coverage_nodeID_to_nodeObject.keySet().size(), setup_size);

                int number_of_total_combinations = combinations.size();

                double currently_best_rss_results = 9999999999999999999999999d;
                ArrayList<Integer> currently_best_setup = null;

                int current_combo_check = 0;
                for (ArrayList<Integer> node_setup : combinations) {

                    if (!stop_coverage_optimization_process) {
                        // Get a fresh 1 dimensional rss array to start filling it with best RSS values
                        double[] cell_receptions_for_current_combo = empty_cell_receptions_1d.clone();

                        // For every single cell within the zones
                        for (int cell_iter = 0; cell_iter < cell_receptions_for_current_combo.length; cell_iter++) {
                            // Get the best RSS from the currently iterated nodes
                            for (int node : node_setup) {
                                if (coverage_nodeID_to_nodeObject.get(node).cell_receptions_1D[cell_iter] < cell_receptions_for_current_combo[cell_iter]) {
                                    cell_receptions_for_current_combo[cell_iter] = coverage_nodeID_to_nodeObject.get(node).cell_receptions_1D[cell_iter];
                                }
                            }
                        }

                        double current_score = Arrays.stream(cell_receptions_for_current_combo).sum();
                        if (current_score < currently_best_rss_results) {
                            currently_best_rss_results = current_score;

                            if (currently_best_setup != null) {
                                ArrayList<Integer> nodes_to_remove = new ArrayList<>(currently_best_setup);
                                Platform.runLater(() -> {
                                    for (Integer node_id : nodes_to_remove) {
                                        // Put the node back to the node_candidate group
                                        coverage_optimization_node_candidates.getChildren().add(coverage_nodeID_to_nodeObject.get(node_id));
                                    }
                                });
                            }
                            currently_best_setup = node_setup;
                            ArrayList<Integer> nodes_to_add = new ArrayList<>(currently_best_setup);

                            Platform.runLater(() -> {
                                for (Integer node_id : nodes_to_add) {
                                    // Get the node from the node_candidate group and put it to the optimal_node_setup
                                    optimal_coverage_node_setup.getChildren().add(coverage_nodeID_to_nodeObject.get(node_id));
                                }
                            });
                        }
                        current_combo_check++;

                        int percentage_status = (current_combo_check * 100) / number_of_total_combinations;
                        if (previous_percentage_status != percentage_status){
                            previous_percentage_status = percentage_status;
                            Platform.runLater(() -> {
                                coverage_optimization_progress_fx.setText(String.valueOf(percentage_status));
                            });
                        }

                    } else {
                        break;
                    }
                }
            } else {
                too_many_calculations_notice_fx.setVisible(true);
            }
        }
    }

    public class optimize_localization_waiter implements Runnable {

        boolean update_score_flag = false;
        double best_performance = Double.MIN_VALUE; //todo change that according to the metric
        int[] optimal_setup;
        final int number_of_threads = 5;

        public void run() {
            int setup_size = Integer.parseInt(setup_size_for_localization_optimization_fx.getText());
            optimal_setup = new int[setup_size];

            optimize_localization_worker[] workers_holder = new optimize_localization_worker[number_of_threads];
            int[] mutation_rates = new int[]{1, 2, 3, 4, 5, 6}; // How many mutations do we want every time
            //int[] mutation_rates = new int[]{x, x, x, x, x}; // 1/x mutations

            ExecutorService es = Executors.newCachedThreadPool();
            for (int i = 0; i < number_of_threads; i++){
                //Use this for fixed mutations
                int mutation_rate_for_this_case = (int)((float) grid_base.zone_border_cell_list.size())/(mutation_rates[i]);
                workers_holder[i] = new optimize_localization_worker(setup_size, mutation_rate_for_this_case);
                es.execute(workers_holder[i]);
            }
            es.shutdown();

            Timer timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {

                    System.out.println("Generation: " + GA_Optimizer.current_generation);

                    for (Nodes_Optimizer_Controller.optimize_localization_worker optimize_localization_worker : workers_holder) {
                        if (optimize_localization_worker.optimizer != null && optimize_localization_worker.optimizer.population.best_genes != null) {
                            Population pop = optimize_localization_worker.optimizer.population;
                            if (best_performance < pop.best_performance_found) { //todo change that according to the metric
                                best_performance = pop.best_performance_found;
                                System.out.println(best_performance);
                                for (int setup_iter = 0; setup_iter < pop.best_node_setup_array.length(); setup_iter++){
                                    optimal_setup[setup_iter] = pop.best_node_setup_array.get(setup_iter);
                                }
                                update_score_flag = true;
                            }
                        }
                    }

                    // If the optimizer has some optimization ready
                    if (update_score_flag){
                        Platform.runLater(() -> {
                            try {
                                for (Beacon beacon: localization_nodeID_to_nodeObject.values()){
                                    if (!localization_optimization_node_candidates.getChildren().contains(beacon)){
                                        localization_optimization_node_candidates.getChildren().add(beacon);
                                    }
                                }

                                for (Integer node_id : optimal_setup) {
                                    // Get the node from the node_candidate group and put it to the optimal_node_setup
                                    optimal_localization_node_setup.getChildren().add(localization_nodeID_to_nodeObject.get(node_id));
                                    localization_optimization_progress_fx.setText(String.valueOf(Math.round(best_performance*100.0)/100.0));
                                }
                            }
                            catch (Exception ignored){}
                        });

                        update_score_flag = false;
                    }
                }
            }, 0, 1000);

            try {
                es.awaitTermination(10, TimeUnit.DAYS);
                timer.cancel();

                Platform.runLater(() -> {
                    best_coverage_tab_fx.setDisable(false);
                    maximize_localization_fx.setDisable(false);
                    main_editor_toolbox_fx.setDisable(false);
                    localization_optimization_status_fx.setVisible(false);
                });
            } catch (Exception ignored) {
            }
        }
    }

    public class optimize_localization_worker implements Runnable {
        GA_Optimizer optimizer;
        int mutation_rate;
        int setup_size;

        optimize_localization_worker(int setup_size, int mutation_rate){
            this.mutation_rate = mutation_rate;
            this.setup_size = setup_size;
        }

        public void run() {
            Integer setup_size = Integer.parseInt(setup_size_for_localization_optimization_fx.getText());
            optimizer = new GA_Optimizer(
                    setup_size, grid_base.zone_border_cell_list, grid_base.interconnections_list,
                    localization_nodeID_to_nodeObject, mutation_rate, grid_base.all_cells_list);

            optimizer.start();
        }
    }

    private void optimize_coverage() {
        if (!"".equals(setup_size_for_coverage_optimization_fx.getText())) {
            best_localization_tab_fx.setDisable(true);
            maximize_coverage_fx.setDisable(true);
            main_editor_toolbox_fx.setDisable(true);
            node_candidates_vbox_fx.setDisable(true);
            too_many_calculations_notice_fx.setVisible(false);
            coverage_optimization_status_fx.setVisible(true);

            reset_cell_and_nodes_colors_to_default();

            stop_coverage_optimization_process = false;
            coverage_optimization_progress_fx.setText("0");

            nodes_optimizer_group_fx.getChildren().remove(optimal_coverage_node_setup);
            optimal_coverage_node_setup = new Group();
            nodes_optimizer_group_fx.getChildren().add(optimal_coverage_node_setup);

            optimize_coverage_waiter coverage_optimizer_executor = new optimize_coverage_waiter();
            Thread t = new Thread(coverage_optimizer_executor);
            t.start();
        }
    }

    // todo add elegxous gia size border list kai node setup size
    private void optimize_localization() {
        if (!"".equals(setup_size_for_localization_optimization_fx.getText())) {
            best_coverage_tab_fx.setDisable(true);
            maximize_localization_fx.setDisable(true);
            main_editor_toolbox_fx.setDisable(true);
            localization_optimization_status_fx.setVisible(true);

            reset_cell_and_nodes_colors_to_default();

            stop_localization_optimization_process = false;
            localization_optimization_progress_fx.setText("0");

            nodes_optimizer_group_fx.getChildren().remove(optimal_localization_node_setup);
            optimal_localization_node_setup = new Group();
            nodes_optimizer_group_fx.getChildren().add(optimal_localization_node_setup);

            optimize_localization_waiter localization_optimizer_waiter = new optimize_localization_waiter();
            Thread t = new Thread(localization_optimizer_waiter);
            t.start();
        }
    }

    private void inspect_coverage_radiomap() {
        if (inspect_radiomap_for_coverage_optimization_fx.isSelected()) {
            coverage_optimization_node_candidates.setVisible(false);
            reset_cell_and_nodes_colors_to_default();

            generate_grid_fx.setDisable(false);
            best_localization_tab_fx.setDisable(false);
            coverage_optimization_setup_size_setter_panel_fx.setDisable(false);
            coverage_optimization_node_candidates_generation_panel_fx.setDisable(false);
        } else {
            coverage_nodeID_to_nodeObject.values().forEach(
                    node -> node.refresh_snap_points(6 / Nodes_Optimizer_zoomScale.getValue()));
            coverage_optimization_node_candidates.setVisible(true);

            generate_grid_fx.setDisable(true);
            best_localization_tab_fx.setDisable(true);
            coverage_optimization_setup_size_setter_panel_fx.setDisable(true);
            coverage_optimization_node_candidates_generation_panel_fx.setDisable(true);
        }
    }

    private void toggle_view_separation_features() {
        if (toggle_show_separation_features_fx.isSelected()) {
            grid_base.hide_separation_features();
            generate_grid_fx.setDisable(false);
            best_coverage_tab_fx.setDisable(false);
            localization_optimization_setup_size_setter_panel_fx.setDisable(false);
            generate_localization_optimization_node_candidates_fx.setDisable(false);
            inspect_radiomap_for_localization_optimization_fx.setDisable(false);
        } else {
            grid_base.show_separation_features();
            generate_grid_fx.setDisable(true);
            best_coverage_tab_fx.setDisable(true);
            localization_optimization_setup_size_setter_panel_fx.setDisable(true);
            generate_localization_optimization_node_candidates_fx.setDisable(true);
            inspect_radiomap_for_localization_optimization_fx.setDisable(true);
        }
    }

    private void inspect_zone_border_rss_values() {
        if (inspect_radiomap_for_localization_optimization_fx.isSelected()) {
            localization_optimization_node_candidates.setVisible(false);
            reset_cell_and_nodes_colors_to_default();

            generate_grid_fx.setDisable(false);
            best_coverage_tab_fx.setDisable(false);
            localization_optimization_setup_size_setter_panel_fx.setDisable(false);
            generate_localization_optimization_node_candidates_fx.setDisable(false);
            toggle_show_separation_features_fx.setDisable(false);
        } else {
            localization_nodeID_to_nodeObject.values().forEach(
                    node -> node.refresh_snap_points(6 / Nodes_Optimizer_zoomScale.getValue()));
            localization_optimization_node_candidates.setVisible(true);

            generate_grid_fx.setDisable(true);
            best_coverage_tab_fx.setDisable(true);
            localization_optimization_setup_size_setter_panel_fx.setDisable(true);
            generate_localization_optimization_node_candidates_fx.setDisable(true);
            toggle_show_separation_features_fx.setDisable(true);

            load_currently_inspected_beacons();
        }
    }

    private void load_currently_inspected_beacons(){
        for (Beacon beacon: beacon_set){
            beacon.setFill(Color.color(0, 1, 0, 0.8));
        }
    }

    private void reset_cell_and_nodes_colors_to_default() {
        // Set the node colors back to normal
        for (Node node : coverage_nodeID_to_nodeObject.values()) {
            node.setFill(Color.color(0, 0, 1, 0.5));
        }
        for (Node node : localization_nodeID_to_nodeObject.values()) {
            node.setFill(Color.color(0, 0, 1, 0.5));
        }

        // Update the colors of the GRID cells back to the corresponding zone
        for (Cell cell : grid_base.all_cells_list) {
            cell.setFill(zoneName_to_zoneColor.get(cell.assigned_zone_name));
            cell.setStrokeWidth(0.5);
            cell.setStroke(Color.color(0.7, 0.7, 0.7, 1));
        }

        // Iterate over all cells to identify if the cell is on the perimeter
        for (Cell cell : grid_base.all_cells_list) {
            Color c = cell.color;
            // Check if all neighbor cells exist
            if (cell.on_perimeter) {
                cell.setFill(Color.color(c.getRed(), c.getGreen(), c.getBlue(), 1));
            } else {
                cell.setFill(Color.color(c.getRed(), c.getGreen(), c.getBlue(), 0.2));
            }
        }
    }

    private static BigInteger factorial(BigInteger number) {
        BigInteger result = BigInteger.valueOf(1);

        for (long factor = 2; factor <= number.longValue(); factor++) {
            result = result.multiply(BigInteger.valueOf(factor));
        }

        return result;
    }

    private static BigInteger predict_combinations(int possible_node_positions, int setup_size) {
        BigInteger bigInteger_possible_node_positions = BigInteger.valueOf(possible_node_positions);
        BigInteger bigInteger_setup_size = BigInteger.valueOf(setup_size);

        BigInteger combinations = factorial(bigInteger_possible_node_positions).divide(
                factorial(bigInteger_setup_size).multiply(
                        factorial(bigInteger_possible_node_positions.subtract(bigInteger_setup_size))));
        return combinations;
    }

    private void prepare_view(){
        if (best_localization_tab_fx.isSelected()){
            nodes_optimizer_group_fx.getChildren().remove(optimal_coverage_node_setup);
            reset_cell_and_nodes_colors_to_default();
            nodes_optimizer_group_fx.getChildren().add(optimal_localization_node_setup);
        }
        else{
            nodes_optimizer_group_fx.getChildren().remove(optimal_localization_node_setup);
            reset_cell_and_nodes_colors_to_default();
            nodes_optimizer_group_fx.getChildren().add(optimal_coverage_node_setup);
        }
    }

    private void get_rss_report(){
        for (Beacon beacon: beacon_set){
            for (int cell_id: ids_of_sampled_positions_in_interior_at_45cm_cell_size){
                System.out.print(beacon.nodeID + ";" + cell_id + ";" + beacon.cell_receptions_1D[cell_id]+"\n");
            }
        }
    }

    private void Set_OnMousePressed_Events() {
        ////// Handle the debugging execution //////
        generate_grid_fx.setOnMousePressed(event -> generate_GRID());
        generate_coverage_optimization_node_candidates_fx.setOnMousePressed(event -> generate_coverage_node_candidates());
        generate_localization_optimization_node_candidates_fx.setOnMousePressed(event -> generate_localization_node_candidates());
        cancel_candidate_loader_for_coverage_opt_fx.setOnMousePressed(event -> kill_coverage_opt_radiomap_builder());
        cancel_candidate_loader_for_localization_opt_fx.setOnMousePressed(event -> kill_localization_opt_radiomap_builder());
        stop_coverage_optimization_fx.setOnMousePressed(event -> stop_coverage_optimization());
        stop_localization_optimization_fx.setOnMousePressed(event -> stop_localization_optimization());
        inspect_radiomap_for_coverage_optimization_fx.setOnMousePressed(event -> inspect_coverage_radiomap());
        inspect_radiomap_for_localization_optimization_fx.setOnMousePressed(event -> inspect_zone_border_rss_values());
        toggle_show_separation_features_fx.setOnMousePressed(event -> toggle_view_separation_features());
        maximize_coverage_fx.setOnMousePressed(event -> optimize_coverage());
        maximize_localization_fx.setOnMousePressed(event -> optimize_localization());
        best_localization_tab_fx.setOnSelectionChanged(event -> prepare_view());
        get_rss_report_fx.setOnMousePressed(event -> get_rss_report());
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        // Set the text listeners to force the text field to be numeric only and with proper limits
        node_candidates_fx.textProperty().addListener((observable, oldValue, newValue) -> {
            // Ensure that the input characters can be converted into a string between 1 and 100
            if (!"".equals(newValue)) {
                if ("0".equals(newValue)) {
                    node_candidates_fx.setText("");
                } else {
                    if (!newValue.matches("\\d*")) {
                        node_candidates_fx.setText(newValue.replaceAll("[^\\d]", ""));
                    } else {
                        String finalValue = newValue.replaceFirst("^0+(?!$)", "");

                        Platform.runLater(() -> {
                            if (Integer.parseInt(finalValue) > 100) {
                                node_candidates_fx.setText("100");
                            } else {
                                node_candidates_fx.setText(finalValue);
                            }
                            node_candidates_fx.end();
                        });
                    }
                }
            }
        });
        setup_size_for_coverage_optimization_fx.textProperty().addListener((observable, oldValue, newValue) -> {

            // Ensure that the input characters can be converted into a string between 1 and 100
            if (!"".equals(newValue)) {
                if ("0".equals(newValue)) {
                    setup_size_for_coverage_optimization_fx.setText("");
                } else {
                    if (!newValue.matches("\\d*")) {
                        setup_size_for_coverage_optimization_fx.setText(newValue.replaceAll("[^\\d]", ""));
                    } else {
                        String finalValue = newValue.replaceFirst("^0+(?!$)", "");

                        Platform.runLater(() -> {
                            if (Integer.parseInt(finalValue) > coverage_optimization_node_candidates_counter) {
                                setup_size_for_coverage_optimization_fx.setText(String.valueOf(coverage_optimization_node_candidates_counter));
                            } else {
                                setup_size_for_coverage_optimization_fx.setText(finalValue);
                            }
                            setup_size_for_coverage_optimization_fx.end();
                        });
                    }
                }
            }
        });

        setup_size_for_localization_optimization_fx.textProperty().addListener((observable, oldValue, newValue) -> {

            // Ensure that the input characters can be converted into a string between 1 and 100
            if (!"".equals(newValue)) {
                if ("0".equals(newValue)) {
                    setup_size_for_localization_optimization_fx.setText("");
                } else {
                    if (!newValue.matches("\\d*")) {
                        setup_size_for_localization_optimization_fx.setText(newValue.replaceAll("[^\\d]", ""));
                    } else {
                        String finalValue = newValue.replaceFirst("^0+(?!$)", "");

                        Platform.runLater(() -> {
                            if (Integer.parseInt(finalValue) > localization_optimization_node_candidates_counter) {
                                setup_size_for_localization_optimization_fx.setText(String.valueOf(localization_optimization_node_candidates_counter));
                            } else {
                                setup_size_for_localization_optimization_fx.setText(finalValue);
                            }
                            setup_size_for_localization_optimization_fx.end();
                        });
                    }
                }
            }
        });

        // This action is for normal buttons
        Set_OnMousePressed_Events();

        SceneGestures sceneGestures = new SceneGestures(this);

        scrollable_nodes_optimizer_fx.addEventFilter(MouseEvent.MOUSE_PRESSED, sceneGestures.getScrollableNodesOptimizer_ClickedHandler());
        scrollable_nodes_optimizer_fx.addEventFilter(MouseEvent.MOUSE_DRAGGED, sceneGestures.getScrollableNodesOptimizer_DragHandler());
        scrollable_nodes_optimizer_fx.addEventFilter(ScrollEvent.ANY, sceneGestures.getScrollableNodesOptimizer_ScrollHandler());

        // Remove the background color from the tab menu
        optimization_tab_fx.getStyleClass().add("floating");

        opt_control = this;
    }
}
