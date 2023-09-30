package Core.Controllers.Nodes_Optimizer;

import Core.Controllers.GRID_Core.Cell;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

class Beacon extends Node{

    private ToggleButton inspect_btn;

    Beacon(int node_id, double centerX, double centerY, double radius,
           LinkedHashMap<Integer, LinkedHashMap<Integer, Cell>> complete_cell_list, int number_of_cells,
           ArrayList<Cell> list_with_valuable_cells,
           Nodes_Optimizer_Controller parent_controller,
           ToggleButton inspect_radiomap_for_localization_optimization_fx,
           Label custom_localization_performance_score_fx, Label feature_id_fx){

        super(node_id, centerX, centerY, radius, complete_cell_list, number_of_cells, list_with_valuable_cells, parent_controller);

        inspect_btn = inspect_radiomap_for_localization_optimization_fx;

        this.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> {
            if (inspect_btn.isSelected()){
                feature_id_fx.setText(String.valueOf(node_id));
            }
        });

        this.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            if (inspect_btn.isSelected()){
                if (Nodes_Optimizer_Controller.beacon_set.contains(this)){
                    Nodes_Optimizer_Controller.beacon_set.remove(this);
                }
                else{
                    Nodes_Optimizer_Controller.beacon_set.add(this);
                }

                if (Nodes_Optimizer_Controller.beacon_set.size()!=0){
                    double new_score = Math.round(GA_Optimizer.get_minimum_separation_distance(Nodes_Optimizer_Controller.beacon_set)*100.0)/100.0;
                    //double new_score = Math.round(GA_Optimizer.get_distances_variation(Nodes_Optimizer_Controller.beacon_set)*100.0)/100.0;

                    custom_localization_performance_score_fx.setText(String.valueOf(new_score));
                }
                else {
                    custom_localization_performance_score_fx.setText("");
                }

                for (Beacon beacon: opt_control.localization_nodeID_to_nodeObject.values()) {
                    beacon.setFill(Color.color(0, 0, 1, 0.5));
                }
                for (Beacon beacon: Nodes_Optimizer_Controller.beacon_set) {
                    beacon.setFill(Color.color(0, 1, 0, 0.8));
                }
            }
        });
    }

    @Override
    void show_rss_map() {
        if (!inspect_btn.isSelected()) {
            for (Beacon beacon : opt_control.localization_nodeID_to_nodeObject.values()) {
                beacon.setFill(Color.color(0, 0, 1, 0.5));
            }
            for (Node node : opt_control.coverage_nodeID_to_nodeObject.values()) {
                node.setFill(Color.color(0, 0, 1, 0.5));
            }
            setFill(Color.color(0, 1, 0, 0.8));

            // Update the colors of the GRID
            for (Map.Entry<Integer, LinkedHashMap<Integer, Cell>> cell_object_column : opt_control.grid_base.all_cells_2D_mapping.entrySet()) {
                for (Map.Entry<Integer, Cell> cell_object_row : cell_object_column.getValue().entrySet()) {
                    Cell cell = get_cell(cell_object_column.getKey(), cell_object_row.getKey());

                    if (cell != null) {
                        cell.set_color_based_on_rss(cell_receptions.get(cell_object_column.getKey()).get(cell_object_row.getKey()));
                        cell.setStrokeWidth(0);
                        cell.setStroke(Color.color(0., 0, 0, 0));
                    }
                }
            }
        }
    }
}
