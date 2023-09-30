package Core.Controllers.Nodes_Optimizer;

import Core.Controllers.GRID_Core.Cell;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

public class Interconnection extends Line {

    public Interconnection(Cell cell_A, Cell cell_B) {
        super(cell_A.getX_Centroid(), cell_A.getY_Centroid(), cell_B.getX_Centroid(), cell_B.getY_Centroid());
        Color c = Color.color(0.8, 1, 0.0, 1);
        setStroke(c);
    }
}
