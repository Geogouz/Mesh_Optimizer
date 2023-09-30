package Core.Controllers.Nodes_Optimizer;

import Core.Controllers.GRID_Core.Cell;
import Core.Controllers.GRID_Core.Grid;
import javafx.beans.binding.Bindings;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;

import java.util.*;
import java.util.stream.Collectors;

import static Core.Controllers.GRID_Core.Grid.get_Pixel_Distance_between_Cells;
import static Core.Controllers.GRID_Core.Grid.number_of_radial_rays;
import static java.lang.StrictMath.max;
import static java.lang.StrictMath.min;
import static javafx.scene.shape.StrokeType.CENTERED;


public class Node extends Circle {

    Group rays_to_visualize = new Group();

    int nodeID;
    LinkedHashMap<Integer, LinkedHashMap<Integer, Double>> cell_receptions = new LinkedHashMap<>();
    Double[] cell_receptions_1D;
    private LinkedHashMap<Integer, LinkedHashMap<Integer, Cell>> cell_list_2d = new LinkedHashMap<>();
    private ArrayList<int[]> obstructed_cells = new ArrayList<>();
    LinkedHashSet<Integer> sample_points_with_direct_sight = new LinkedHashSet<>();
    LinkedHashMap<Integer, ArrayList<Ray>> reachable_cells = new LinkedHashMap<>();
    HashMap<Integer, ArrayList<short[]>> connectivity_properties = new HashMap<>();
    Nodes_Optimizer_Controller opt_control;

    public Node(int node_id, double centerX, double centerY, double radius) {

        super(centerX, centerY, radius);
        nodeID = node_id;

        setFill(Color.color(1, 0, 0, 0.5));
        setStroke(Color.BLACK);
        setStrokeType(CENTERED);
        strokeWidthProperty().bind(Bindings.divide(radiusProperty(), 3));
    }

    public Node(int node_id, double centerX, double centerY, double radius,
                LinkedHashMap<Integer, LinkedHashMap<Integer, Cell>> cell_list, int number_of_cells,
                ArrayList<Cell> list_with_valuable_cells, Nodes_Optimizer_Controller parent_controller) {
        super(centerX, centerY, radius);

        cell_receptions_1D = new Double[number_of_cells];

        opt_control = parent_controller;
        cell_list_2d = cell_list;
        nodeID = node_id;

        setFill(Color.color(0, 0, 1, 0.5));
        setStroke(Color.color(0, 0, 0, 0.5));
        setStrokeType(CENTERED);
        strokeWidthProperty().bind(Bindings.divide(radiusProperty(), 4));

        // Prepare the radiomap
        for (Map.Entry<Integer, LinkedHashMap<Integer, Cell>> grid_column : cell_list_2d.entrySet()) {
            LinkedHashMap<Integer, Double> new_column = new LinkedHashMap<>();
            cell_receptions.put(grid_column.getKey(), new_column);
            for (Map.Entry<Integer, Cell> grid_row : grid_column.getValue().entrySet()) {
                // Check whether current cell interests us for calculating the RSS towards it
                if (list_with_valuable_cells.contains(grid_row.getValue())){
                    boolean some_obstruction_intersects_cell = false;
                    // Between current cell and this node, check whether there is any obstructions that intersects them
                    // Identify which cells have unobstructed sight
                    for (Map.Entry<Integer, Line> obstruction_entry : opt_control.obstructionID_to_obstruction.entrySet()) {
                        if (Grid.linesIntersect(centerX, centerY, grid_row.getValue().getX_Centroid(),
                                grid_row.getValue().getY_Centroid(), obstruction_entry.getValue().getStartX(),
                                obstruction_entry.getValue().getStartY(), obstruction_entry.getValue().getEndX(),
                                obstruction_entry.getValue().getEndY())) {

                            obstructed_cells.add(new int[]{grid_column.getKey(), grid_row.getKey()});

                            // We have found a Cell which is obstructed by some obstruction. Put a default value for that
                            cell_receptions.get(grid_column.getKey()).put(grid_row.getKey(), 100d);

                            int cell_1d_index = cell_list_2d.get(grid_column.getKey()).get(grid_row.getKey()).grid_index_int_1d;
                            cell_receptions_1D[cell_1d_index] = 100d;

                            some_obstruction_intersects_cell = true;
                            break;
                        }
                    }

                    // If there is no intersection, calculate directly the RSS value based on the FSPL model
                    if (!some_obstruction_intersects_cell) {
                        // First calculate the distance between them
                        double pixel_distance = get_Pixel_Distance_between_Cells(
                                centerX, centerY, grid_row.getValue().getX_Centroid(),
                                grid_row.getValue().getY_Centroid());

                        // Update the RSS of the given pixel
                        double rss = Grid.get_rss_based_on_pixel_distance(pixel_distance, opt_control.georeference_scale);

                        cell_receptions.get(grid_column.getKey()).put(grid_row.getKey(), rss);

                        int cell_1d_index = cell_list_2d.get(grid_column.getKey()).get(grid_row.getKey()).grid_index_int_1d;
                        cell_receptions_1D[cell_1d_index] = rss;
                    }
                }
            }
        }

        this.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> {
            this.getParent().setCursor(Cursor.HAND);
        });

        this.addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
            // Make sure we are in pan mode
            this.getParent().setCursor(Cursor.DEFAULT);
        });

        this.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            // Make sure we are in pan mode
            show_rss_map();
        });
    }

    // This function is called right after every zoom in/out to resize the snap_points for better visualisation
    void refresh_snap_points(double new_size) {
        setRadius(new_size);
    }

    private boolean grid_position_in_cell_list(int[] cell_id, ArrayList<Cell> separation_zone_border_cells){
        for (Cell cell: separation_zone_border_cells){
            if (cell_id[0]== cell.getX() && cell_id[1] == cell.getY()){ //todo check reverse..
                return true;
            }
        }
        return false;
    }

    Cell get_cell(int column_id, int row_id) {
        LinkedHashMap<Integer, Cell> column = cell_list_2d.get(column_id);

        if (column == null){
            return null;
        }
        else {
            return column.get(row_id);
        }
    }

    void launch_parent_rays_from_node(ArrayList<Cell> separation_zone_border_cells) {
        // These data structures will hold all the produced Rays
        ArrayList<FastRay> launched_rays = new ArrayList<>();

        for (int i = 0; i < number_of_radial_rays; i++) {
            double rotated_x = getCenterX() + opt_control.ids_of_all_launched_rays.get(i).get(i).offset_x;
            double rotated_y = getCenterY() + opt_control.ids_of_all_launched_rays.get(i).get(i).offset_y;

            FastRay new_ray = new FastRay(new int[]{0, 0, 0}, getCenterX(), getCenterY(), rotated_x, rotated_y,
                    null, 0, null, 0);
            launched_rays.add(new_ray);
        }

        recursively_launch_child_rays(launched_rays);
    }

    private void recursively_launch_child_rays(ArrayList<FastRay> launched_rays) {

        ArrayList<FastRay> new_launched_rays = new ArrayList<>();
        // The first time we come here, the parent rays shall be iterated
        // On the last time we come here, the rays that will be iterated will have no other child rays
        // Iterate all launched rays
        for (FastRay ray : launched_rays) {

            // Find all obstructions that intersect this ray
            // and calculate the intersection point and the distance from the launching source point
            LinkedHashMap<Integer, double[]> ordered_by_distance_intersected_obstructions =
                    getOrderedIntersectedObstructions(ray);

            // For every cell which cannot see directly the node
            for (int[] cell_id : obstructed_cells) {
                // Check whether currently iterated Ray and SamplePoint intersect
                if (ray.reaches_cell(get_cell(cell_id[0], cell_id[1]))) {

                    //First calculate the distance between the Sample Point and the origin of the Ray
                    double distance_between_SamplePoint_and_Ray = get_Pixel_Distance_between_Cells(
                            ray.start_x, ray.start_y, get_cell(cell_id[0], cell_id[1]).getX_Centroid(),
                            get_cell(cell_id[0], cell_id[1]).getY_Centroid());

                    int[] properties = new int[3];
                    properties[0] = ray.refractions_already_occurred[0];
                    properties[1] = ray.refractions_already_occurred[1];
                    properties[2] = ray.refractions_already_occurred[2];

                    for (Map.Entry<Integer, double[]> obstruction_property : ordered_by_distance_intersected_obstructions.entrySet()) {
                        if (obstruction_property.getValue()[2] < distance_between_SamplePoint_and_Ray) {
                            properties[opt_control.obstructionID_to_obstruction_type.get(obstruction_property.getKey())] =
                                    properties[opt_control.obstructionID_to_obstruction_type.get(obstruction_property.getKey())] + 1;
                        }
                    }

                    double new_rss = properties[0] * Grid.door_refraction_coefficient +
                            properties[1] * Grid.thin_wall_refraction_coefficient +
                            properties[2] * Grid.thick_wall_refraction_coefficient +
                            ray.reflections_already_occurred * Grid.reflection_coefficient +
                            Grid.get_rss_based_on_real_distance((distance_between_SamplePoint_and_Ray + ray.traversed_distance_from_node) * opt_control.georeference_scale);

                    if (new_rss < cell_receptions.get(cell_id[0]).get(cell_id[1])) {
                        cell_receptions.get(cell_id[0]).put(cell_id[1], new_rss);

                        int cell_1d_index = cell_list_2d.get(cell_id[0]).get(cell_id[1]).grid_index_int_1d;
                        cell_receptions_1D[cell_1d_index] = new_rss;
                    }
                }
            }

            // If any intersection with an obstruction has been found and we have less than 5 reflections
            if (ray.reflections_already_occurred < 5 && ordered_by_distance_intersected_obstructions.size() != 0) {
                for (Map.Entry<Integer, double[]> entry : ordered_by_distance_intersected_obstructions.entrySet()) {
                    FastRay new_reflected_ray = ray.get_reflected_ray(ray.refractions_already_occurred.clone(), entry,
                            opt_control.obstructionID_to_normal.get(entry.getKey()));
                    new_launched_rays.add(new_reflected_ray);

                    ray.refractions_already_occurred[opt_control.obstructionID_to_obstruction_type.get(entry.getKey())] =
                            ray.refractions_already_occurred[opt_control.obstructionID_to_obstruction_type.get(entry.getKey())] + 1;
                }
            }
        }

        if (new_launched_rays.size() > 0) {
            recursively_launch_child_rays(new_launched_rays);
        }
    }

    // Find the IDs of all the obstructions (in obstructionID_to_obstruction) that intersect this FastRay
    private LinkedHashMap<Integer, double[]> getOrderedIntersectedObstructions(FastRay ray) {

        Map<Integer, double[]> intersected_obstructions_points = new HashMap<>();
        Map<Integer, Double> intersected_obstructions_distances = new HashMap<>();

        for (Map.Entry<Integer, Line> entry : opt_control.obstructionID_to_obstruction.entrySet()) {
            // Avoid considering the obstruction from which the ray has bounced
            if (!entry.getKey().equals(ray.obstruction_source)) {
                if (!((opt_control.obstructionID_to_obstruction_bounds.get(entry.getKey())[0] > max(ray.start_x, ray.end_x)) ||
                        (opt_control.obstructionID_to_obstruction_bounds.get(entry.getKey())[1] < min(ray.start_x, ray.end_x)) ||
                        (opt_control.obstructionID_to_obstruction_bounds.get(entry.getKey())[2] > max(ray.start_y, ray.end_y)) ||
                        (opt_control.obstructionID_to_obstruction_bounds.get(entry.getKey())[3] < min(ray.start_y, ray.end_y)))) {

                    double[] intersection_details = ray.getRayIntersection(entry.getValue());

                    if (intersection_details != null) {
                        intersected_obstructions_points.put(entry.getKey(), intersection_details);
                        intersected_obstructions_distances.put(entry.getKey(), get_Pixel_Distance_between_Cells(
                                ray.start_x, ray.start_y, intersection_details[0], intersection_details[1]));
                    }
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

    void show_rss_map() {

        for (Beacon beacon : opt_control.localization_nodeID_to_nodeObject.values()) {
            beacon.setFill(Color.color(0, 0, 1, 0.5));
        }
        for (Node node : opt_control.coverage_nodeID_to_nodeObject.values()) {
            node.setFill(Color.color(0, 0, 1, 0.5));
        }
        setFill(Color.color(0, 1, 0, 0.8));

        // Update the colors of the GRID
        for (Map.Entry<Integer, LinkedHashMap<Integer, Cell>> cell_object_column: opt_control.grid_base.all_cells_2D_mapping.entrySet()) {
            for (Map.Entry<Integer, Cell> cell_object_row : cell_object_column.getValue().entrySet()) {
                Cell cell = get_cell(cell_object_column.getKey(), cell_object_row.getKey());

                if (cell != null){
                    cell.set_color_based_on_rss(cell_receptions.get(cell_object_column.getKey()).get(cell_object_row.getKey()));
                    cell.setStrokeWidth(0);
                    cell.setStroke(Color.color(0., 0, 0, 0));
                }
            }
        }
    }
}