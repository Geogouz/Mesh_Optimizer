package Core.Controllers.GRID_Core;

import Core.Controllers.Nodes_Optimizer.Node;
import Core.Controllers.Nodes_Optimizer.Nodes_Optimizer_Controller;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.HashMap;

public class Cell extends Rectangle {

    // Variables to hold the coordinates of the cell elements
    private double x_Centroid;
    private double y_Centroid;
    private double x_Start;
    private double y_Start;
    private double x_End;
    private double y_End;
    String grid_index_str;
    int grid_index_x_int_2d;
    int grid_index_y_int_2d;
    public int grid_index_int_1d;
    public String assigned_zone_name;
    private int assigned_sample_point;
    public boolean on_perimeter;
    public Color color;
    private ToggleButton inspect_btn;

    public double[] diagonal_1 = new double[4];
    public double[] diagonal_2 = new double[4];

    HashMap<Cell, ArrayList<Integer>> obstruction_intersections_towards_other_cells = new HashMap<>();

    Cell(int input_grid_index_x_int, int input_grid_index_y_int, String index_str, double x0, double y0,
         double cell_size_px, Color input_fill_color, String assigned_zone, int cell_index,
         ToggleButton inspect_radiomap_for_localization_optimization_fx, Grid grid_base, Label feature_id_fx) {

        super(x0, y0, cell_size_px, cell_size_px);
        super.setStrokeWidth(0.5);
        super.setStroke(Color.color(0.7, 0.7, 0.7, 1));

        grid_index_str = index_str;
        grid_index_x_int_2d = input_grid_index_x_int;
        grid_index_y_int_2d = input_grid_index_y_int;
        assigned_zone_name = assigned_zone;

        x_Start = x0;
        y_Start = y0;
        x_End = x0 + cell_size_px;
        y_End = y0 + cell_size_px;

        diagonal_1[0] = x_Start;
        diagonal_1[1] = y_Start;
        diagonal_1[2] = x0 + cell_size_px;
        diagonal_1[3] = y_End;

        diagonal_2[0] = x_Start;
        diagonal_2[1] = y_End;
        diagonal_2[2] = x0 + cell_size_px;
        diagonal_2[3] = y_Start;

        // Calculate the centroid of the cell
        x_Centroid = x_Start + cell_size_px / 2;
        y_Centroid = y_Start + cell_size_px / 2;

        grid_index_int_1d = cell_index;
        color = input_fill_color;

        this.inspect_btn = inspect_radiomap_for_localization_optimization_fx;

        this.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> {
            if (inspect_btn.isSelected()){
                feature_id_fx.setText(String.valueOf(grid_index_int_1d));
            }
        });

        this.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            if (inspect_btn.isSelected()){
                if (Nodes_Optimizer_Controller.sample_position_set.contains(this)){
                    Nodes_Optimizer_Controller.sample_position_set.remove(this);
                }
                else{
                    Nodes_Optimizer_Controller.sample_position_set.add(this);
                }

                // Update the colors of the GRID cells back to the corresponding zone
                for (Cell cell : grid_base.all_cells_list) {
                    cell.setFill(Nodes_Optimizer_Controller.zoneName_to_zoneColor.get(cell.assigned_zone_name));
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

                // Update the colors of the GRID cells back to the corresponding zone
                for (Cell cell : Nodes_Optimizer_Controller.sample_position_set) {
                    cell.setFill(Color.color(1, 0.5, 0, 1));
                }

            }
        });
    }

    // This constructor is used specifically for the sample points which are used to train the radio propagation model
    public Cell(double top_left_sampling_point_cell_x, double top_left_sampling_point_cell_y,
                double bottom_right_sampling_point_cell_x, double bottom_right_sampling_point_cell_y,
                double sampling_point_centroid_x, double sampling_point_centroid_y, double pixel_cell_point_width,
                int sample_point_id) {

        super(top_left_sampling_point_cell_x, top_left_sampling_point_cell_y,
                pixel_cell_point_width, pixel_cell_point_width);

        diagonal_1[0] = top_left_sampling_point_cell_x;
        diagonal_1[1] = top_left_sampling_point_cell_y;
        diagonal_1[2] = bottom_right_sampling_point_cell_x;
        diagonal_1[3] = bottom_right_sampling_point_cell_y;

        diagonal_2[0] = top_left_sampling_point_cell_x;
        diagonal_2[1] = bottom_right_sampling_point_cell_y;
        diagonal_2[2] = bottom_right_sampling_point_cell_x;
        diagonal_2[3] = top_left_sampling_point_cell_y;

        super.setStrokeWidth(0);
        //super.setFill(Color.TRANSPARENT);
        super.setFill(Color.ORANGE);

        assigned_sample_point = sample_point_id;

        x_Start = top_left_sampling_point_cell_x;
        y_Start = top_left_sampling_point_cell_y;
        x_End = bottom_right_sampling_point_cell_x;
        y_End = bottom_right_sampling_point_cell_y;

        // Calculate the centroid of the cell
        x_Centroid = sampling_point_centroid_x;
        y_Centroid = sampling_point_centroid_y;
    }

    // This constructor is used specifically for the sample points which are used to train the radio propagation model
    public Cell(int input_grid_index_x_int, int input_grid_index_y_int, double x0, double y0, double cell_size_px) {

        super(x0, y0, cell_size_px, cell_size_px);
        super.setStrokeWidth(0);
        super.setFill(Color.color(0, 0, 0, 0.5));

        grid_index_str = input_grid_index_x_int + "," + input_grid_index_y_int;;
        grid_index_x_int_2d = input_grid_index_x_int;
        grid_index_y_int_2d = input_grid_index_y_int;
        assigned_zone_name = "Separation_Cell";

        x_Start = x0;
        y_Start = y0;
        x_End = x0 + cell_size_px;
        y_End = y0 + cell_size_px;

        diagonal_1[0] = x_Start;
        diagonal_1[1] = y_Start;
        diagonal_1[2] = x0 + cell_size_px;
        diagonal_1[3] = y_End;

        diagonal_2[0] = x_Start;
        diagonal_2[1] = y_End;
        diagonal_2[2] = x0 + cell_size_px;
        diagonal_2[3] = y_Start;

        // Calculate the centroid of the cell
        x_Centroid = x_Start + cell_size_px / 2;
        y_Centroid = y_Start + cell_size_px / 2;
    }

    // How to properly report a Cell object
    public String toString() {

        String line = "Cell: [" + grid_index_x_int_2d + "," + grid_index_y_int_2d + "], Zone: " + assigned_zone_name +
                ", NW: [" + String.format("%.2f", x_Start).replace(",", ".")
                + "," + String.format("%.2f", y_Start).replace(",", ".")
                + "], SE: [" + String.format("%.2f", x_End).replace(",", ".")
                + "," + String.format("%.2f", y_End).replace(",", ".")
                + "], Centroid: [" + String.format("%.2f", x_Centroid).replace(",", ".") + ","
                + String.format("%.2f", y_Centroid).replace(",", ".") + "]";

        return grid_index_int_1d + " Cell[" + grid_index_x_int_2d + "," + grid_index_y_int_2d + "]";
    }

    public double getX_Start() {
        return x_Start;
    }

    public void setX_Start(double x_Start) {
        this.x_Start = x_Start;
    }

    public double getY_Start() {
        return y_Start;
    }

    public void setY_Start(double y_Start) {
        this.y_Start = y_Start;
    }

    public double getX_End() {
        return x_End;
    }

    public void setX_End(double x_End) {
        this.x_End = x_End;
    }

    public double getY_End() {
        return y_End;
    }

    public void setY_End(double y_End) {
        this.y_End = y_End;
    }

    public String getGrid_index_str() {
        return grid_index_str;
    }

    public void setGrid_index_str(String grid_index_str) {
        this.grid_index_str = grid_index_str;
    }

    public String getAssigned_zone_name() {
        return assigned_zone_name;
    }

    public void setAssigned_zone_name(String assigned_zone_name) {
        this.assigned_zone_name = assigned_zone_name;
    }

    public int getGrid_index_x_int_2d() {
        return grid_index_x_int_2d;
    }

    public void setGrid_index_x_int_2d(int grid_index_x_int_2d) {
        this.grid_index_x_int_2d = grid_index_x_int_2d;
    }

    public int getGrid_index_y_int_2d() {
        return grid_index_y_int_2d;
    }

    public void setGrid_index_y_int_2d(int grid_index_y_int_2d) {
        this.grid_index_y_int_2d = grid_index_y_int_2d;
    }

    public double getX_Centroid() {
        return x_Centroid;
    }

    public void setX_Centroid(double x_Centroid) {
        this.x_Centroid = x_Centroid;
    }

    public double getY_Centroid() {
        return y_Centroid;
    }

    public void setY_Centroid(double y_Centroid) {
        this.y_Centroid = y_Centroid;
    }

    public void set_color_based_on_rss(double rss) {
        final double max_rss = 98;
        final double min_rss = 50;
        final double range_diff = max_rss - min_rss;

        Color c2 = Color.YELLOW;
        Color c1 = Color.RED;

        double rss_color_norm;

        if (rss > max_rss) {
            super.setFill(Color.color(0, 0, 0, 1));
        } else {
            if (rss < min_rss) {
                rss_color_norm = 1;
            } else {
                rss_color_norm = (rss - min_rss) / range_diff;
            }
            double red = c1.getRed() * rss_color_norm + c2.getRed() * (1 - rss_color_norm);
            double green = c1.getGreen() * rss_color_norm + c2.getGreen() * (1 - rss_color_norm);
            double blue = c1.getBlue() * rss_color_norm + c2.getBlue() * (1 - rss_color_norm);

            super.setFill(Color.color(red, green, blue, 1));
        }
    }
}
