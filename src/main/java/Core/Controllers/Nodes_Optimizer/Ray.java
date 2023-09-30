package Core.Controllers.Nodes_Optimizer;

import Core.Controllers.GRID_Core.Cell;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

import java.util.Map;

import static Core.Controllers.GRID_Core.Grid.*;
import static Core.Controllers.Nodes_Optimizer.LineSegmentLineSegmentIntersection.lineSegmentLineSegmentIntersection;

class Ray extends Line {

    double offset_x;
    double offset_y;
    int[] refractions_already_occurred;
    int[] reflections_already_occurred;
    double traversed_distance_from_node;
    Integer obstruction_source;
    private double[] ray_vector;

    Ray(int[] parent_refractions, double node_origin_x, double node_origin_y, double max_destination_x, double max_destination_y, double[] vector,
        int[] reflections_occurred, Integer obstruction_id, double traversed_distance) {
        super(node_origin_x, node_origin_y, max_destination_x, max_destination_y);

        refractions_already_occurred = parent_refractions;
        reflections_already_occurred = reflections_occurred;
        traversed_distance_from_node = traversed_distance;
        obstruction_source = obstruction_id;

        if (vector == null) {
            double[] non_normalized_ray_vector = new double[2];
            non_normalized_ray_vector[0] = max_destination_x - node_origin_x;
            non_normalized_ray_vector[1] = max_destination_y - node_origin_y;
            ray_vector = get_normal_of_vector(non_normalized_ray_vector);
        } else {
            ray_vector = vector;
        }

        Color c = Color.color(1, 0, 0, 0.5);
        setStroke(c);
        setStrokeWidth(2);
    }

    boolean reaches_cell(Cell sample_cell) {

        // First check whether this Ray intersects with the first diagonal of the sample point
        double[] intersection_point_with_diagonal_1 = lineSegmentLineSegmentIntersection(getStartX(), getStartY(), getEndX(), getEndY(), sample_cell.diagonal_1[0], sample_cell.diagonal_1[1], sample_cell.diagonal_1[2], sample_cell.diagonal_1[3]);

        if (intersection_point_with_diagonal_1 != null) {
            return true;
        }
        // If it does not, check whether this Ray intersects with the second diagonal of the sample point
        else {
            double[] intersection_point_with_diagonal_2 = lineSegmentLineSegmentIntersection(getStartX(), getStartY(), getEndX(), getEndY(), sample_cell.diagonal_2[0], sample_cell.diagonal_2[1], sample_cell.diagonal_2[2], sample_cell.diagonal_2[3]);

            return intersection_point_with_diagonal_2 != null;
        }

    }

    double[] getRayIntersection(Line obstruction) {

        double[] points = lineSegmentLineSegmentIntersection(getStartX(), getStartY(), getEndX(), getEndY(), obstruction.getStartX(), obstruction.getStartY(), obstruction.getEndX(), obstruction.getEndY());

        if (points != null) {
            //System.out.println("Ray - StartX:" + getStartX() + " StartY:" + origin_point[1] + " EndX:" + destination_point[0] + " EndY:" + destination_point[1]);
            //System.out.println("Obstruction - StartX:" + obstruction.getStartX() + " StartY:" + obstruction.getStartY() + " EndX:" + obstruction.getEndX() + " EndY:" + obstruction.getEndY());
            //System.out.println(points[0].x + " " + points[0].y);
            //System.out.println();
            return new double[]{points[0], points[1]};
        }

        return null;
    }

    Ray get_reflected_ray(int[] parent_refractions, Map.Entry<Integer, double[]> obstruction_entry, double[] obstruction_normal, int obstruction_type) {
        // Remember that the 3rd element of the double[] array that includes the properties, is the distance
        double new_traversed_distance = traversed_distance_from_node + obstruction_entry.getValue()[2];
        double new_max_distance = pixel_distance_at_100dbm - new_traversed_distance;

        double new_scalar = 2 * dot_product_of_this_ray_vector_with_another_vector(ray_vector, obstruction_normal);
        double[] intermediate_vector = vector_scalar_multiplication(obstruction_normal, new_scalar);
        double[] new_vector = subtraction_of_this_ray_vector_with_another_vector(ray_vector, intermediate_vector);

        double[] destination_point = destination_point_from_origin_point_vector_and_distance(obstruction_entry.getValue(), new_vector, new_max_distance);

        // Make a new reflection object for the new Ray
        int[] new_reflections_already_occurred = reflections_already_occurred.clone();
        new_reflections_already_occurred[obstruction_type] = new_reflections_already_occurred[obstruction_type] + 1;

        return new Ray(parent_refractions, obstruction_entry.getValue()[0], obstruction_entry.getValue()[1], destination_point[0],
                destination_point[1], new_vector, new_reflections_already_occurred, obstruction_entry.getKey(), new_traversed_distance);
    }
}
