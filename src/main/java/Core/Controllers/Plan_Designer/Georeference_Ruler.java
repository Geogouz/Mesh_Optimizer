package Core.Controllers.Plan_Designer;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

import static java.lang.Math.log10;


public class Georeference_Ruler extends Line {

    public double georeference_scale;
    public double georeference_scale_FSPL_constant;
    SnapEdge snap_point_1;
    SnapEdge snap_point_2;
    public SimpleDoubleProperty real_distance = new SimpleDoubleProperty();

    class SnapEdge extends Circle {
        private double dragBaseX;
        private double dragBaseY;

        SnapEdge(double centerX, double centerY, double radius) {
            super(centerX, centerY, radius);

            setOnMousePressed(event -> {
                Plan_Designer_Controller.currently_editing_the_georeference_ruler = true;
                dragBaseX = event.getX() - getCenterX();
                dragBaseY = event.getY() - getCenterY();
            });

            setOnMouseDragged(event -> {
                setCenterX(event.getX() - dragBaseX);
                setCenterY(event.getY() - dragBaseY);
                distance_updater(snap_point_1.getCenterX(), snap_point_1.getCenterY(),
                        snap_point_2.getCenterX(), snap_point_2.getCenterY());
                event.consume();
            });

            setOnMouseReleased(event -> Plan_Designer_Controller.currently_editing_the_georeference_ruler = false);
            setOnMouseEntered(event -> setCursor(Cursor.CROSSHAIR));
            setOnMouseExited(event -> setCursor(Cursor.DEFAULT));
        }

        // Reveal the corresponding snap points
        private void distance_updater(double x1, double y1, double x2, double y2) {
            // Calculate the distance based on the pythagorean theorem and update the corresponding property
            real_distance.set(Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2)) * georeference_scale / 1000);
        }
    }

    Georeference_Ruler(double basemap_width, double basemap_height, Group group) {
        // Calculate where to place the georeference ruler (we want to place it right in the middle of the basemap)
        double centerX = basemap_width / 2;
        double centerY = basemap_height / 2;
        snap_point_1 = new SnapEdge(centerX - 100, centerY, 3);
        snap_point_2 = new SnapEdge(centerX + 100, centerY, 3);
        set_visualization(group);
    }

    Georeference_Ruler(double snap_point_1x, double snap_point_1y, double snap_point_2x, double snap_point_2y,
                       Group group) {
        snap_point_1 = new SnapEdge(snap_point_1x, snap_point_1y, 3);
        snap_point_2 = new SnapEdge(snap_point_2x, snap_point_2y, 3);
        set_visualization(group);
    }

    private void set_visualization(Group group) {
        snap_point_1.setFill(Color.color(0, 0, 1, 0.5));
        snap_point_2.setFill(Color.color(0, 0, 1, 0.5));

        setStroke(Color.color(0, 0, 0, 0.5));
        setStrokeWidth(2);
        setStrokeLineCap(StrokeLineCap.BUTT);
        setStrokeLineJoin(StrokeLineJoin.MITER);

        startXProperty().bind(snap_point_1.centerXProperty());
        startYProperty().bind(snap_point_1.centerYProperty());
        endXProperty().bind(snap_point_2.centerXProperty());
        endYProperty().bind(snap_point_2.centerYProperty());

        // Add the georeference ruler (where he will also remain) to the last position (i.e. on top) of the group
        group.getChildren().add(this);
        group.getChildren().add(snap_point_1);
        group.getChildren().add(snap_point_2);
    }

    // Reveal the corresponding snap points
    void show_Ruler() {
        snap_point_1.setVisible(true);
        snap_point_2.setVisible(true);
        this.setVisible(true);
    }

    // Hide the corresponding snap points
    void hide_Ruler() {
        snap_point_1.setVisible(false);
        snap_point_2.setVisible(false);
        this.setVisible(false);
    }

    public double getGeoreference_scale() {
        return georeference_scale;
    }

    public void setGeoreference_scale(double georeference_scale) {
        this.georeference_scale = georeference_scale;
        this.georeference_scale_FSPL_constant = 20 * log10(georeference_scale);
    }
}