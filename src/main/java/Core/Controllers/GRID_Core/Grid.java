package Core.Controllers.GRID_Core;

import Core.Controllers.Nodes_Optimizer.Interconnection;
import Core.Controllers.Nodes_Optimizer.Nodes_Optimizer_Controller;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;

import java.util.*;

import static java.lang.StrictMath.log;
import static java.lang.StrictMath.sqrt;

public class Grid extends Group {

    // This 2-Dimensional array holds the cell objects of the GRID.
    // Indices start from 1, therefore, each first element (i.e. index 0) is null
    public LinkedHashMap<Integer, LinkedHashMap<Integer, Cell>> all_cells_2D_mapping = new LinkedHashMap<>();
    public ArrayList<Cell> all_cells_list = new ArrayList<>();
    public LinkedHashMap<Integer, LinkedHashMap<Integer, Cell>> zone_border_cells_2D_mapping = new LinkedHashMap<>();
    public ArrayList<Cell> zone_border_cell_list = new ArrayList<>();
    public ArrayList<Cell> separation_zone_border_cell_list = new ArrayList<>();
    private ArrayList<Integer> separation_zone_border_cell_index_list = new ArrayList<>();
    public static Integer[] ids_of_sampled_positions_in_interior_at_45cm_cell_size = new Integer[] {
            25,128,1817,1619,1836,1638,1844,1647,2539,2813,2523,3025,3202,1135,1127,2251,1862,1665,2846,3051,1964,1965,
            1157,1172,1883,1687,1869,1673,2084,2572,3060,3269,3077,2085,1890,1695,2493,3415,3095,1493,1494,1404,1003,
            891,655,873,857,855,620,619,437,435,545,491,474,425,303,234,112,120};
    public static List<Integer> ids_of_sampled_positions_in_interior_at_45cm_cell_size_list = Arrays.asList(ids_of_sampled_positions_in_interior_at_45cm_cell_size);
    public static ArrayList<Cell> cells_of_sampled_positions_in_interior_list = new ArrayList<>();
    public static List<Cell> cells_for_which_to_generate_radiomap;

    public ArrayList<int[]> interconnections_list = new ArrayList<>();
    private LinkedHashMap<Integer, ArrayList<Integer>> alpha_outer_perimeter = new LinkedHashMap<>();
    private ArrayList<Cell> separation_area = new ArrayList<>();

    private Group separation_area_cells = new Group();
    private Group interconnections = new Group();

    public int total_number_of_cells = 0;
    private double cell_size_px;
    private static final double fspl_A_parameter = 13.28128178;
    private static final double fspl_B_parameter = 2162.58762079;
    private static final double fspl_C_parameter = -47.6255823;
    public static final double door_refraction_coefficient = 1.43;           // 1.45
    public static final double thin_wall_refraction_coefficient = 1.43;      // 1.45
    public static final double thick_wall_refraction_coefficient = 5.23;     // 4.95
    public static final double reflection_coefficient = 0.47;                // 0.45
    public static final double distance_at_100dbm = 60000;
    public static double pixel_distance_at_100dbm;
    public static final double number_of_radial_rays = 1080; //todo ideally 1080
    private int grid_left_bound = 9999999;
    private int grid_bottom_bound = -9999999;
    private int grid_right_bound = -9999999;
    private int grid_top_bound = 9999999;
    private Nodes_Optimizer_Controller opt_controller;

    public Grid(Integer input_cell_size, double input_georeference_scale, Nodes_Optimizer_Controller opt_controller,
                ToggleButton inspect_radiomap_for_localization_optimization_fx, Label feature_id_fx) {

        this.opt_controller = opt_controller;
        cell_size_px = input_cell_size / input_georeference_scale;

        // Calculate how many cells to generate per dimension
        int col_number = (int) Math.ceil((opt_controller.right_boundary / cell_size_px));
        int row_number = (int) Math.ceil((opt_controller.bottom_boundary / cell_size_px));

        int iterator = 0;

        // Generate the GRID
        for (int y = 0; y < row_number; y++) {
            for (int x = 0; x < col_number; x++) {

                String cell_index = x + "," + y;

                double[] x_boundaries = get_pixel_boundaries_from_row_or_column_grid_index(x, cell_size_px);
                double[] y_boundaries = get_pixel_boundaries_from_row_or_column_grid_index(y, cell_size_px);

                // Check whether a zone entirely contains this cell
                for (Map.Entry<Integer, Polygon> entry : opt_controller.zoneID_to_zonePolygon.entrySet()) {
                    if (entry.getValue().contains(x_boundaries[0], y_boundaries[0]) &
                            entry.getValue().contains(x_boundaries[0], y_boundaries[1]) &
                            entry.getValue().contains(x_boundaries[1], y_boundaries[0]) &
                            entry.getValue().contains(x_boundaries[1], y_boundaries[1])) {

                        // Update the GRID Bounds
                        update_grid_bounds(x, y);

                        String zoneName = opt_controller.zoneID_to_zoneName.get(entry.getKey());

                        total_number_of_cells++;

                        Cell new_cell = new Cell(x, y, cell_index, x_boundaries[0], y_boundaries[0], cell_size_px,
                                opt_controller.zoneName_to_zoneColor.get(zoneName), zoneName, iterator,
                                inspect_radiomap_for_localization_optimization_fx, this, feature_id_fx);

                        iterator++;

                        LinkedHashMap<Integer, Cell> cell_row = all_cells_2D_mapping.get(x);
                        // If current row hasn't been created yet
                        if (cell_row == null) {
                            // Create it and add the new row along with the new column which will hold the new cell
                            LinkedHashMap<Integer, Cell> new_cell_row = new LinkedHashMap<>();
                            new_cell_row.put(y, new_cell);
                            all_cells_2D_mapping.put(x, new_cell_row);
                        } else {
                            cell_row.put(y, new_cell);
                        }

                        // Add the cell to a separate group tha contains all to-be-shown Cells
                        getChildren().add(new_cell);
                        all_cells_list.add(new_cell);
                    }
                }
            }
        }
    }

    public void show_separation_features(){
        getChildren().add(separation_area_cells);
        getChildren().add(interconnections);
    }

    public void hide_separation_features(){
        getChildren().remove(separation_area_cells);
        getChildren().remove(interconnections);
    }

    public void prepare_extra_zones() {
        get_zone_perimeters();
        get_alpha_shape();
        get_separation_area();
        separation_area.addAll(expand_separation_area_to_zone_borders());
        separation_area_cells.getChildren().addAll(separation_area);
        build_separation_connections();

        // Prepare the 1-dimension array that will hold the default RSS values.
        // This is used during the coverage optimizations
        opt_controller.empty_cell_receptions_1d = new double[all_cells_list.size()];
        Arrays.fill(opt_controller.empty_cell_receptions_1d, 100);
    }

    private void build_separation_connections(){
        // Create a Map linking the ZoneNames to the corresponding separation_zone_borders
        HashMap<String, ArrayList<Cell>> separation_zone_borders_map = new HashMap<>();
        for (String zone_name: opt_controller.zoneName_to_zoneColor.keySet()){
            separation_zone_borders_map.put(zone_name, new ArrayList<>());
        }

        for (Cell cell_in_separation_area: separation_area){
            for (Cell zone_cell: all_cells_list){
                if (cell_in_separation_area.getX() == zone_cell.getX() && cell_in_separation_area.getY() == zone_cell.getY()){
                    separation_zone_borders_map.get(zone_cell.assigned_zone_name).add(zone_cell);
                }
            }
        }

        find_separations_connectivity(separation_zone_borders_map);
        convert_separation_zone_borders_to_cell_array(separation_zone_borders_map);
    }

    private void find_separations_connectivity(HashMap<String, ArrayList<Cell>> separation_zone_borders){
        ArrayList<Cell[]> separation_interconnections = new ArrayList<>();

        // For every separation_cell
        for (Map.Entry<String, ArrayList<Cell>> zone_entry : separation_zone_borders.entrySet()){
            for (Cell separation_cell: zone_entry.getValue()){
                ArrayList<Cell> separation_cell_neighbors = get_closest_separation_cell_neighbors_from_separation_cell(
                        separation_cell, zone_entry.getKey(), separation_zone_borders);
                // Add the found interconnections to the separation_interconnections ArrayList
                for (Cell neighbor: separation_cell_neighbors){

                    Cell[] new_interconnection = new Cell[]{separation_cell, neighbor};

                    if (!reversed_interconnection_in_separation_interconnections(new_interconnection, separation_interconnections)){
                        separation_interconnections.add(new_interconnection);
                    }
                }
            }
        }

        for (Cell[] interconnection: separation_interconnections){
            // Calculate the Cell 1-dimensional indices for the 2 Cells
            Cell cell_A = interconnection[0];
            Cell cell_B = interconnection[1];

            //System.out.println(cell_A + " " + cell_A.grid_index_int_1d + " " + (cell_A==all_cells_list.get(cell_A.grid_index_int_1d)));
            //System.out.println(cell_B + " " + cell_B.grid_index_int_1d + " " + (cell_B==all_cells_list.get(cell_B.grid_index_int_1d)));

            interconnections_list.add(new int[]{cell_A.grid_index_int_1d, cell_B.grid_index_int_1d});
            interconnections.getChildren().add(new Interconnection(interconnection[0], interconnection[1]));
        }
    }

    private ArrayList<Cell> get_closest_separation_cell_neighbors_from_separation_cell(
            Cell separation_cell, String zone_name, HashMap<String, ArrayList<Cell>> separation_zone_borders){

        ArrayList<Cell> separation_cell_neighbors = new ArrayList<>();
        double neighbor_distance = 999999999999999999999999999999d;

        for (Map.Entry<String, ArrayList<Cell>> zone_entry : separation_zone_borders.entrySet()){
            // For every separation_cell that belongs to another zone
            if (!zone_name.equals(zone_entry.getKey())){
                for (Cell separation_cell_neighbor: zone_entry.getValue()){
                    if (separation_cell_neighbors.size() == 0){
                        separation_cell_neighbors.add(separation_cell_neighbor);
                        neighbor_distance = get_Squared_Pixel_Distance_between_Cells(
                                separation_cell, separation_cell_neighbor);
                    }
                    else {

                        double new_neighbor_distance = get_Squared_Pixel_Distance_between_Cells(
                                separation_cell, separation_cell_neighbor);

                        if (new_neighbor_distance < neighbor_distance){
                            separation_cell_neighbors = new ArrayList<>();
                            separation_cell_neighbors.add(0, separation_cell_neighbor);
                            neighbor_distance = new_neighbor_distance;
                        }
                        else if (new_neighbor_distance == neighbor_distance){
                            separation_cell_neighbors.add(0, separation_cell_neighbor);
                        }
                    }
                }
            }
        }

        return separation_cell_neighbors;
    }

    private boolean reversed_interconnection_in_separation_interconnections(
            Cell[] new_interconnection, ArrayList<Cell[]> separation_interconnections){

        // Check whether an interconnection already exists in the separation_interconnections ArrayList
        for (Cell[] existing_interconnection: separation_interconnections){
            if (existing_interconnection[0].grid_index_str.equals(new_interconnection[1].grid_index_str) &&
                    existing_interconnection[1].grid_index_str.equals(new_interconnection[0].grid_index_str)){
                return true;
            }
        }
        return false;
    }

    private void convert_separation_zone_borders_to_cell_array(HashMap<String, ArrayList<Cell>> separation_zone_borders){

        separation_zone_border_cell_list = new ArrayList<>();
        LinkedHashMap<Integer, LinkedHashMap<Integer, Cell>> separation_zone_border_cells_2D_mapping = new LinkedHashMap<>();

        // For every zone border line
        for (Map.Entry<String, ArrayList<Cell>> zone_entry : separation_zone_borders.entrySet()){
            // Get an individual cell
            for (Cell separation_cell: zone_entry.getValue()){
                // add it to the 1-dimensional arrays
                separation_zone_border_cell_list.add(separation_cell);
                separation_zone_border_cell_index_list.add(separation_cell.grid_index_int_1d);

                // Update the empty 2-dimensional mapping too
                LinkedHashMap<Integer, Cell> column = separation_zone_border_cells_2D_mapping.get(
                        separation_cell.grid_index_x_int_2d);
                if (column != null){
                    separation_zone_border_cells_2D_mapping.get(separation_cell.grid_index_x_int_2d).put(
                            separation_cell.grid_index_y_int_2d, separation_cell);
                }
                else {
                    separation_zone_border_cells_2D_mapping.put(
                            separation_cell.grid_index_x_int_2d, new LinkedHashMap<>());
                }
            }
        }
    }

    private boolean has_all_neighbors(Cell cell) {

        try {
            Cell neighbor_1 = all_cells_2D_mapping.get(cell.grid_index_x_int_2d - 1).get(cell.grid_index_y_int_2d - 1);
            Cell neighbor_2 = all_cells_2D_mapping.get(cell.grid_index_x_int_2d).get(cell.grid_index_y_int_2d - 1);
            Cell neighbor_3 = all_cells_2D_mapping.get(cell.grid_index_x_int_2d + 1).get(cell.grid_index_y_int_2d - 1);
            Cell neighbor_4 = all_cells_2D_mapping.get(cell.grid_index_x_int_2d - 1).get(cell.grid_index_y_int_2d);
            Cell neighbor_5 = all_cells_2D_mapping.get(cell.grid_index_x_int_2d + 1).get(cell.grid_index_y_int_2d);
            Cell neighbor_6 = all_cells_2D_mapping.get(cell.grid_index_x_int_2d - 1).get(cell.grid_index_y_int_2d + 1);
            Cell neighbor_7 = all_cells_2D_mapping.get(cell.grid_index_x_int_2d).get(cell.grid_index_y_int_2d + 1);
            Cell neighbor_8 = all_cells_2D_mapping.get(cell.grid_index_x_int_2d + 1).get(cell.grid_index_y_int_2d + 1);

            return neighbor_1 != null && neighbor_2 != null && neighbor_3 != null && neighbor_4 != null &&
                    neighbor_5 != null && neighbor_6 != null && neighbor_7 != null && neighbor_8 != null;
        } catch (NullPointerException e) {
            return false;
        }
    }

    private double[] get_pixel_boundaries_from_row_or_column_grid_index(int index, double cell_size_px) {
        double[] bounds = new double[2];
        bounds[0] = index * cell_size_px;
        bounds[1] = (index + 1) * cell_size_px;
        return bounds;
    }

    public Cell getCell_from_index(String idx) {

        // Get the 2 indices
        String[] parted_indices = idx.split(",");

        // Convert them into int types
        int x_index = Integer.valueOf(parted_indices[0]);
        int y_index = Integer.valueOf(parted_indices[1]);

        return all_cells_2D_mapping.get(x_index).get(y_index);
    }

    public void findObstructions_between_Cells(ArrayList<HashMap<String, Double>> obstructions, Cell cell_a, Cell cell_b) {

        ArrayList<Integer> intersections = new ArrayList<>();

        // For obstructions
        for (HashMap<String, Double> obstruction : obstructions) {
            // Check whether it intersects with the segment between the 2 given centroids
            if (linesIntersect(obstruction.get("P1_x"), obstruction.get("P1_y"), obstruction.get("P2_x"), obstruction.get("P2_y"),
                    cell_a.getX_Centroid(), cell_a.getY_Centroid(), cell_b.getX_Centroid(), cell_b.getY_Centroid())) {

                intersections.add(obstruction.get("id").intValue());
            }
        }

        cell_a.obstruction_intersections_towards_other_cells.put(cell_b, intersections);
    }

    public static boolean linesIntersect(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4) {
        // Return false if either of the lines have zero length
        if (x1 == x2 && y1 == y2 || x3 == x4 && y3 == y4) {
            return false;
        }

        double ax = x2 - x1;
        double ay = y2 - y1;
        double bx = x3 - x4;
        double by = y3 - y4;
        double cx = x1 - x3;
        double cy = y1 - y3;

        double alphaNumerator = by * cx - bx * cy;
        double commonDenominator = ay * bx - ax * by;

        if (commonDenominator > 0) {
            if (alphaNumerator < 0 || alphaNumerator > commonDenominator) {
                return false;
            }
        } else if (commonDenominator < 0) {
            if (alphaNumerator > 0 || alphaNumerator < commonDenominator) {
                return false;
            }
        }

        double betaNumerator = ax * cy - ay * cx;

        if (commonDenominator > 0) {
            if (betaNumerator < 0 || betaNumerator > commonDenominator) {
                return false;
            }
        } else if (commonDenominator < 0) {
            if (betaNumerator > 0 || betaNumerator < commonDenominator) {
                return false;
            }
        }

        if (commonDenominator == 0) {
            // The lines are parallel.
            // Check if they're collinear.
            double y3LessY1 = y3 - y1;
            double collinearityTestForP3 = x1 * (y2 - y3) + x2 * (y3LessY1) + x3 * (y1 - y2);   // see http://mathworld.wolfram.com/Collinear.html

            // If p3 is collinear with p1 and p2 then p4 will also be collinear, since p1-p2 is parallel with p3-p4
            if (collinearityTestForP3 == 0) {
                // The lines are collinear. Now check if they overlap.
                if (x1 >= x3 && x1 <= x4 || x1 <= x3 && x1 >= x4 ||
                        x2 >= x3 && x2 <= x4 || x2 <= x3 && x2 >= x4 ||
                        x3 >= x1 && x3 <= x2 || x3 <= x1 && x3 >= x2) {
                    return y1 >= y3 && y1 <= y4 || y1 <= y3 && y1 >= y4 ||
                            y2 >= y3 && y2 <= y4 || y2 <= y3 && y2 >= y4 ||
                            y3 >= y1 && y3 <= y2 || y3 <= y1 && y3 >= y2;
                }
            }
            return false;
        }
        return true;
    }

    // Calculate the pixel distance based on the pythagorean theorem
    // Return its squared form to avoid heavy calculations
    public double get_Squared_Pixel_Distance_between_Cells(Cell cell_a, Cell cell_b) {

        return Math.pow(cell_b.getX_Centroid() - cell_a.getX_Centroid(), 2)
                + Math.pow(cell_b.getY_Centroid() - cell_a.getY_Centroid(), 2);
    }

    // Calculate the pixel distance based on the pythagorean theorem
    public static double get_Pixel_Distance_between_Cells(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    // Calculate the real distance based on a Pixel_Distance and a scale
    public static double get_real_Distance_between_Cells(double x1, double y1, double x2, double y2, double georeference_scale) {
        double pixel_distance_between_points = get_Pixel_Distance_between_Cells(x1, y1, x2, y2);

        return pixel_distance_between_points * georeference_scale;
    }

    // Calculated the Free Space Path Loss based on real distance
    public static double get_rss_based_on_real_distance(double real_distance) {
        return fspl_A_parameter * log(fspl_B_parameter + real_distance) + fspl_C_parameter;
    }

    // Calculated the Free Space Path Loss based on pixel distance
    public static double get_rss_based_on_pixel_distance(double pixel_distance, double georeference_scale) {
        return fspl_A_parameter * log(fspl_B_parameter + (pixel_distance * georeference_scale)) + fspl_C_parameter;
    }

    public static double[] vector_scalar_multiplication(double[] vector, double scalar) {

        double[] final_vector = new double[2];
        final_vector[0] = vector[0] * scalar;
        final_vector[1] = vector[1] * scalar;

        return final_vector;
    }

    public static double[] destination_point_from_origin_point_vector_and_distance(double[] origin_point, double[] vector, double distance) {
        return vector_addition(origin_point, vector_scalar_multiplication(vector, distance));
    }

    private static double[] vector_addition(double[] origin_point, double[] added_vector) {

        double[] new_vector = new double[2];
        new_vector[0] = origin_point[0] + added_vector[0];
        new_vector[1] = origin_point[1] + added_vector[1];

        return new_vector;
    }

    private static double get_magnitude_of_vector(double[] vector) {
        return sqrt(vector[0] * vector[0] + vector[1] * vector[1]);
    }

    public static double[] get_normal_of_vector(double[] non_normalized_ray_vector) {
        double dx = non_normalized_ray_vector[0];
        double dy = non_normalized_ray_vector[1];

        double magnitude = get_magnitude_of_vector(non_normalized_ray_vector);

        return new double[]{dx / magnitude, dy / magnitude};
    }

    public static double dot_product_of_this_ray_vector_with_another_vector(double[] ray_vector, double[] vector) {
        return ray_vector[0] * vector[0] + ray_vector[1] * vector[1];
    }

    public static double[] subtraction_of_this_ray_vector_with_another_vector(double[] ray_vector, double[] vector) {

        double[] subtracted_vector = new double[2];
        subtracted_vector[0] = ray_vector[0] - vector[0];
        subtracted_vector[1] = ray_vector[1] - vector[1];

        return subtracted_vector;
    }

    private void update_grid_bounds(int x, int y) {
        if (x < grid_left_bound) {
            grid_left_bound = x;
        }
        if (x > grid_right_bound) {
            grid_right_bound = x;
        }

        if (y < grid_top_bound) {
            grid_top_bound = y;
        }
        if (y > grid_bottom_bound) {
            grid_bottom_bound = y;
        }
    }

    private void get_alpha_shape() {

        // Get the last row index on the first column
        int left_bottom_y = Collections.max(all_cells_2D_mapping.get(grid_left_bound).keySet());

        // Get the last column index on the last row
        int bottom_right_x = 0;
        for (Map.Entry<Integer, LinkedHashMap<Integer, Cell>> column_map : all_cells_2D_mapping.entrySet()) {
            Cell cell_to_check = column_map.getValue().get(grid_bottom_bound);
            if (cell_to_check != null && column_map.getKey() > bottom_right_x) {
                bottom_right_x = column_map.getKey();
            }
        }

        // Get the first row index on the last column
        int right_top_y = Collections.min(all_cells_2D_mapping.get(grid_right_bound).keySet());

        // Get the first column on the first row
        int top_left_x = 100000000;
        for (Map.Entry<Integer, LinkedHashMap<Integer, Cell>> column_map : all_cells_2D_mapping.entrySet()) {
            Cell cell_to_check = column_map.getValue().get(grid_top_bound);
            if (cell_to_check != null && column_map.getKey() < top_left_x) {
                top_left_x = column_map.getKey();
            }
        }

        // Get the starting cell (left-bottom Cell) of the parser
        Cell left_bottom_cell = all_cells_2D_mapping.get(grid_left_bound).get(left_bottom_y);
        Cell bottom_right_cell = all_cells_2D_mapping.get(bottom_right_x).get(grid_bottom_bound);
        Cell right_top_cell = all_cells_2D_mapping.get(grid_right_bound).get(right_top_y);
        Cell top_left_cell = all_cells_2D_mapping.get(top_left_x).get(grid_top_bound);

        bottom_right_alpha_perimeter_parse(left_bottom_cell, bottom_right_cell);
        right_top_alpha_perimeter_parse(bottom_right_cell, right_top_cell);
        top_left_alpha_perimeter_parse(right_top_cell, top_left_cell);
        left_bottom_alpha_perimeter_parse(top_left_cell, left_bottom_cell);
    }

    private void bottom_right_alpha_perimeter_parse(Cell start_cell, Cell end_cell) {

        int current_column = start_cell.grid_index_x_int_2d;
        int current_row = start_cell.grid_index_y_int_2d;

        while (!(current_column == end_cell.grid_index_x_int_2d && current_row == end_cell.grid_index_y_int_2d)) {

            current_column++;

            LinkedHashMap<Integer, Cell> next_column_cells = all_cells_2D_mapping.get(current_column);

            Integer lowest_row;
            Integer row_steps_until_last_row = null;
            try {
                lowest_row = Collections.max(next_column_cells.keySet());
                row_steps_until_last_row = lowest_row - current_row;
            }
            catch (Exception ignored){}

            // If the currently iterated column has no cells at all or the cells are above the alpha perimeter
            if (row_steps_until_last_row == null || row_steps_until_last_row < 1) {
                add_cell_to_alpha_perimeter(current_column, current_row);
            } else {
                for (int row_step = 0; row_step < row_steps_until_last_row + 1; row_step++) {
                    add_cell_to_alpha_perimeter(current_column, current_row);
                    if (row_step != row_steps_until_last_row) {
                        current_row++;
                    }
                }
            }
        }
    }

    private void right_top_alpha_perimeter_parse(Cell start_cell, Cell end_cell) {

        int current_column = start_cell.grid_index_x_int_2d;
        int current_row = start_cell.grid_index_y_int_2d;

        while (!(current_column == end_cell.grid_index_x_int_2d && current_row == end_cell.grid_index_y_int_2d)) {

            int next_lowest_row;
            if (current_column != end_cell.grid_index_x_int_2d) {
                next_lowest_row = get_next_lowest_row(current_column);
            } else {
                next_lowest_row = end_cell.grid_index_y_int_2d;
            }

            int row_steps_until_next_lowest_row = next_lowest_row - current_row;

            if (row_steps_until_next_lowest_row == 0) {
                add_cell_to_alpha_perimeter(current_column, current_row);
            } else {
                for (int row_step = 0; row_step != row_steps_until_next_lowest_row - 1; row_step--) {
                    add_cell_to_alpha_perimeter(current_column, current_row);
                    if (row_step != row_steps_until_next_lowest_row) {
                        current_row--;
                    }
                }
            }

            if (!(current_column == end_cell.grid_index_x_int_2d && current_row == end_cell.grid_index_y_int_2d)) {
                current_column++;
            }
        }
    }

    private void top_left_alpha_perimeter_parse(Cell start_cell, Cell end_cell) {

        int current_column = start_cell.grid_index_x_int_2d;
        int current_row = start_cell.grid_index_y_int_2d;

        while (!(current_column == end_cell.grid_index_x_int_2d && current_row == end_cell.grid_index_y_int_2d)) {

            current_column--;

            LinkedHashMap<Integer, Cell> next_column_cells = all_cells_2D_mapping.get(current_column);

            Integer highest_row;
            Integer row_steps_until_first_row = null;
            try {
                highest_row = Collections.min(next_column_cells.keySet());
                row_steps_until_first_row = current_row - highest_row;
            }
            catch (Exception ignored){}

            if (row_steps_until_first_row == null || row_steps_until_first_row < 1) {
                add_cell_to_alpha_perimeter(current_column, current_row);
            } else {
                for (int row_step = 0; row_step < row_steps_until_first_row + 1; row_step++) {
                    add_cell_to_alpha_perimeter(current_column, current_row);
                    if (row_step != row_steps_until_first_row) {
                        current_row--;
                    }
                }
            }
        }
    }

    private void left_bottom_alpha_perimeter_parse(Cell start_cell, Cell end_cell) {

        int current_column = start_cell.grid_index_x_int_2d;
        int current_row = start_cell.grid_index_y_int_2d;

        while (!(current_column == end_cell.grid_index_x_int_2d && current_row == end_cell.grid_index_y_int_2d)) {

            int prev_highest_row;
            if (current_column != end_cell.grid_index_x_int_2d) {
                prev_highest_row = get_prev_highest_row(current_column);
            } else {
                prev_highest_row = end_cell.grid_index_y_int_2d;
            }

            int row_steps_until_prev_highest_row = prev_highest_row - current_row;

            if (row_steps_until_prev_highest_row == 0) {
                add_cell_to_alpha_perimeter(current_column, current_row);
            } else {
                for (int row_step = 0; row_step != row_steps_until_prev_highest_row + 1; row_step++) {
                    add_cell_to_alpha_perimeter(current_column, current_row);
                    if (row_step != row_steps_until_prev_highest_row) {
                        current_row++;
                    }
                }
            }

            if (!(current_column == end_cell.grid_index_x_int_2d && current_row == end_cell.grid_index_y_int_2d)) {
                current_column--;
            }
        }
    }

    private int get_next_lowest_row(int current_column) {

        int overall_lowest_cell = 0;

        for (Map.Entry<Integer, LinkedHashMap<Integer, Cell>> column_map : all_cells_2D_mapping.entrySet()) {
            if (column_map.getKey() > current_column) {

                // Get the last row index on currently iterated column
                int next_lowest_cell = Collections.max(column_map.getValue().keySet());
                if (next_lowest_cell > overall_lowest_cell) {
                    overall_lowest_cell = next_lowest_cell;
                }
            }
        }
        return overall_lowest_cell;
    }

    private int get_prev_highest_row(int current_column) {

        int overall_highest_cell = 999999999;

        for (Map.Entry<Integer, LinkedHashMap<Integer, Cell>> column_map : all_cells_2D_mapping.entrySet()) {
            if (column_map.getKey() < current_column) {

                // Get the last row index on currently iterated column
                int prev_highest_cell = Collections.min(column_map.getValue().keySet());
                if (prev_highest_cell < overall_highest_cell) {
                    overall_highest_cell = prev_highest_cell;
                }
            }
        }
        return overall_highest_cell;
    }

    private void add_cell_to_alpha_perimeter(int current_column, int current_row) {
        ArrayList<Integer> column = alpha_outer_perimeter.get(current_column);
        if (column != null) {
            column.add(current_row);
        } else {
            ArrayList<Integer> new_column = new ArrayList<>();
            new_column.add(current_row);
            alpha_outer_perimeter.put(current_column, new_column);
        }

        // This is to draw the alpha zone outer perimeter
        //try {cell_list_2d.get(current_column).get(current_row).setFill(Color.BLACK);}
        //catch (Exception ignored){}
    }

    private void get_separation_area() {
        // For every possible cell in the entire GRID
        for (int column = grid_left_bound; column < grid_right_bound + 1; column++) {
            for (int row = 0; row < grid_bottom_bound + 1; row++) {
                // If current column is included within the alpha perimeter
                if (alpha_outer_perimeter.get(column) != null) {

                    // Get the lowest and highest rows of the alpha perimeter within that column
                    int lowest_row = Collections.min(alpha_outer_perimeter.get(column));
                    int highest_row = Collections.max(alpha_outer_perimeter.get(column));

                    if (row > lowest_row && row < highest_row) {
                        if (!alpha_outer_perimeter.get(column).contains(row) && !cell_is_within_a_zone(column, row)) {

                            double[] x_boundaries = get_pixel_boundaries_from_row_or_column_grid_index(
                                    column, cell_size_px);
                            double[] y_boundaries = get_pixel_boundaries_from_row_or_column_grid_index(
                                    row, cell_size_px);

                            separation_area.add(
                                    new Cell(column, row, x_boundaries[0], y_boundaries[0], cell_size_px));
                        }
                    }
                }
            }
        }
    }

    private boolean cell_is_within_a_zone(int column, int row) {
        LinkedHashMap<Integer, Cell> column_cells = all_cells_2D_mapping.get(column);
        if (column_cells != null) {
            return column_cells.containsKey(row);
        }
        return false;
    }

    private ArrayList<Cell> expand_separation_area_to_zone_borders() {
        ArrayList<Cell> expanded_separation_area_to_zone_borders = new ArrayList<>();

        // Iterate over all cells to identify if the cell is on the perimeter
        for (Cell cell : all_cells_list) {
            if (cell.on_perimeter) {

                if (cell_in_separation_area(cell.grid_index_x_int_2d - 1, cell.grid_index_y_int_2d) ||
                        cell_in_separation_area(cell.grid_index_x_int_2d + 1, cell.grid_index_y_int_2d) ||
                        cell_in_separation_area(cell.grid_index_x_int_2d, cell.grid_index_y_int_2d + 1) ||
                        cell_in_separation_area(cell.grid_index_x_int_2d, cell.grid_index_y_int_2d - 1)) {

                    expanded_separation_area_to_zone_borders.add(
                            new Cell(cell.grid_index_x_int_2d, cell.grid_index_y_int_2d,
                                    cell.getX(), cell.getY(), cell_size_px));
                }
            }
        }

        return expanded_separation_area_to_zone_borders;
    }

    private boolean cell_in_separation_area(int x, int y) {
        for (Cell cell : separation_area) {
            if (cell.grid_index_x_int_2d == x && cell.grid_index_y_int_2d == y) {
                return true;
            }
        }
        return false;
    }

    private boolean cell_in_alpha_perimeter(int x, int y) {
        for (Map.Entry<Integer, ArrayList<Integer>> column_entry : alpha_outer_perimeter.entrySet()) {
            if (column_entry.getKey() == x && column_entry.getValue().contains(y)) {
                return true;
            }
        }
        return false;
    }

    private boolean cell_in_sampled_positions(Cell cell){
        return ids_of_sampled_positions_in_interior_at_45cm_cell_size_list.contains(cell.grid_index_int_1d);
    }

    private void get_zone_perimeters() {
        // Iterate over all cells to identify if the cell is on the perimeter
        for (Cell cell : all_cells_list) {

            Color c = cell.color;

            // Check if all neighbor cells exist
            if (!has_all_neighbors(cell)) {
                cell.on_perimeter = true;
                cell.setFill(Color.color(c.getRed(), c.getGreen(), c.getBlue(), 1));

                zone_border_cell_list.add(cell);
                // Update the empty 2-dimensional mapping too
                LinkedHashMap<Integer, Cell> column = zone_border_cells_2D_mapping.get(cell.grid_index_x_int_2d);
                if (column != null){
                    column.put(cell.grid_index_y_int_2d, cell);
                }
                else {
                    LinkedHashMap<Integer, Cell> new_row = new LinkedHashMap<>();
                    new_row.put(cell.grid_index_y_int_2d, cell);
                    zone_border_cells_2D_mapping.put(cell.grid_index_x_int_2d, new_row);
                }
            }
            else if (cell_in_sampled_positions(cell)){
                cells_of_sampled_positions_in_interior_list.add(cell);
            }
            else {
                cell.setFill(Color.color(c.getRed(), c.getGreen(), c.getBlue(), 0.2));
                cell.on_perimeter = false;
            }
        }
    }
}