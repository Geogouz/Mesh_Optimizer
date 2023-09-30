package Core.Controllers.Plan_Designer;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import org.json.simple.JSONObject;

import static Core.Controllers.Plan_Designer.Plan_Designer_Controller.*;

public class Obstruction extends Line {

    static boolean directly_clicked = false;
    private SimpleIntegerProperty obstruction_ID;
    private SimpleStringProperty obstruction_Type;
    private SimpleStringProperty obstruction_Color;

    private SimpleDoubleProperty snap_point_size = new SimpleDoubleProperty();
    private SimpleDoubleProperty snap_offset = new SimpleDoubleProperty();

    private double mouseClickPozX;
    private double mouseClickPozY;

    public boolean vertical_orientation;
    boolean about_to_resize = false;

    SnapEdge top_left;
    SnapEdge bottom_right;

    class SnapEdge extends Rectangle {

        private boolean most_top_left;

        SnapEdge(ImageView image_view) {
            this.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> {
                // Check the orientation of the parent obstruction
                if (vertical_orientation) {
                    if (top_left.getY() < bottom_right.getY()) {
                        // Update the correct order of the snap points
                        top_left.most_top_left = true;
                        bottom_right.most_top_left = false;
                    }
                    this.getParent().setCursor(Cursor.N_RESIZE);
                }
                // Being here means that we are about to resize a horizontal obstruction
                else {
                    if (top_left.getX() < bottom_right.getX()) {
                        // Update the correct order of the snap points
                        top_left.most_top_left = true;
                        bottom_right.most_top_left = false;
                    }
                    this.getParent().setCursor(Cursor.E_RESIZE);
                }
                about_to_resize = true;
            });

            this.addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
                this.getParent().setCursor(Cursor.DEFAULT);
                about_to_resize = false;
            });

            this.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {

                // Consider only the right click
                if (event.getButton() == MouseButton.SECONDARY) {
                    // If we are currently resizing a horizontal obstruction
                    if (!vertical_orientation) {
                        // Check whether this snap point is the most_top_left one
                        if (most_top_left) {
                            // Check whether the obstruction's start is at least 3 pixels less than its end
                            if (event.getX() < (getEndX() - 3)) {
                                // Also ensure that the obstruction's start is bigger than the beginning of the imageview
                                if (event.getX() > 0) {
                                    setStartX(event.getX());
                                } else {
                                    setStartX(0);
                                }
                            }
                            // If not, the user has reduced the obstruction too much
                            else {
                                setStartX(getEndX() - 3);
                            }
                        }
                        // Being here means that this snap point is the further one (having higher coordinate)
                        else {
                            // Check whether the obstruction's end is at least 3 pixels more than its start
                            if (event.getX() > (getStartX() + 3)) {
                                // Also ensure that the obstruction's end is less than the end of the imageview
                                if (event.getX() < image_view.getBoundsInParent().getMaxX()) {
                                    setEndX(event.getX());
                                } else {
                                    setEndX(image_view.getBoundsInParent().getMaxX());
                                }
                            }
                            // If not, the user has reduced the obstruction too much
                            else {
                                setEndX(getStartX() + 3);
                            }
                        }
                    } else {
                        // Being here means that we are currently resizing a vertical obstruction
                        // Check whether this snap point is the most_top_left one
                        if (most_top_left) {
                            // Check whether the obstruction's start is at least 3 pixels less than its end
                            if (event.getY() < (getEndY() - 3)) {
                                // Also ensure that the obstruction's start is bigger than the beginning of the imageview
                                if (event.getY() > 0) {
                                    setStartY(event.getY());
                                } else {
                                    setStartY(0);
                                }
                            }
                            // If not, the user has reduced the obstruction too much
                            else {
                                setStartY(getEndY() - 3);
                            }
                        }
                        // Being here means that this snap point is the further one (having higher coordinate)
                        else {
                            // Check whether the obstruction's end is at least 3 pixels more than its start
                            if (event.getY() > (getStartY() + 3)) {
                                // Also ensure that the obstruction's end is less than the end of the imageview
                                if (event.getY() < image_view.getBoundsInParent().getMaxY()) {
                                    setEndY(event.getY());
                                } else {
                                    setEndY(image_view.getBoundsInParent().getMaxY());
                                }
                            }
                            // If not, the user has reduced the obstruction too much
                            else {
                                setEndY(getStartY() + 3);
                            }
                        }
                    }
                }

                // This is the last custom listener so we can consume the clicks and avoid further consideration of them
                event.consume();
            });
        }
    }

    public Obstruction(boolean orientation, double x, double y, double x2, double y2, int obstruction_id,
                       String obstruction_type, Color obstruction_color,
                       double sps, Plan_Designer_Controller Plan_Designer_panel_fxController) {
        super(x, y, x, y);

        // Check whether this obstruction creation got triggered from the open project mechanism
        if (x2 != -1) {
            setEndX(x2);
            setEndY(y2);
        }

        snap_point_size.setValue(sps);
        snap_offset.bind(snap_point_size.divide(2));

        ImageView image_view = Plan_Designer_panel_fxController.Basemap_ImageView_fx;

        top_left = new SnapEdge(image_view);
        bottom_right = new SnapEdge(image_view);

        Group group = Plan_Designer_panel_fxController.plan_designer_group_fx;
        vertical_orientation = orientation;

        obstruction_ID = new SimpleIntegerProperty(obstruction_id);
        obstruction_Type = new SimpleStringProperty(obstruction_type);
        obstruction_Color = new SimpleStringProperty(obstruction_color.toString());

        setStroke(obstruction_color);
        setStrokeWidth(sps);
        setStrokeLineCap(StrokeLineCap.BUTT);
        setStrokeLineJoin(StrokeLineJoin.MITER);

        set_visualization(group);

        // We add this object to the top
        group.getChildren().add(1, this);

        this.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> {
            // Make sure we are in pan mode
            if (!ready_to_draw_a_new_obstruction && !ready_to_draw_a_new_zone) {
                this.getParent().setCursor(Cursor.HAND);
            } else {
                this.getParent().setCursor(Cursor.CROSSHAIR);
            }
        });

        this.addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
            // Make sure we are in pan mode
            if (!ready_to_draw_a_new_obstruction && !ready_to_draw_a_new_zone) {
                this.getParent().setCursor(Cursor.DEFAULT);
            }
        });

        addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            // Consider here the right click
            if (event.getButton() == MouseButton.SECONDARY) {
                directly_clicked = true;
                obstruction_got_selected_action(Plan_Designer_panel_fxController);
                mouseClickPozX = event.getX();
                mouseClickPozY = event.getY();
            }
            // This is the last custom listener so we can consume the clicks and avoid further consideration of them
            event.consume();
        });

        addEventHandler(MouseEvent.MOUSE_RELEASED, event -> {
            // Make sure we are in pan mode
            if (!ready_to_draw_a_new_obstruction && !ready_to_draw_a_new_zone) {

                // Consider here the right click
                if (event.getButton() == MouseButton.SECONDARY) {
                    getParent().setCursor(Cursor.HAND);
                }
            }
        });

        // Handle the movement of the obstruction
        this.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {

            // Make sure we are in pan mode
            if (!ready_to_draw_a_new_obstruction && !ready_to_draw_a_new_zone) {

                // Consider here the right click
                if (event.getButton() == MouseButton.SECONDARY) {

                    double movement_offset_x = event.getX() - mouseClickPozX;
                    double movement_offset_y = event.getY() - mouseClickPozY;

                    double new_start_x = getStartX() + movement_offset_x;
                    double new_start_y = getStartY() + movement_offset_y;

                    double new_end_x = getEndX() + movement_offset_x;
                    double new_end_y = getEndY() + movement_offset_y;

                    // Handle the horizontal movement
                    // Check whether the right side of the area has exceeded the right side of the basemap
                    if (scaled_Img_MaxX <= new_end_x) {
                        setEndX(scaled_Img_MaxX);
                        setStartX(scaled_Img_MaxX - new_end_x + new_start_x);
                    }
                    // Check whether the left side of the area has exceeded the left side of the basemap
                    else if (new_start_x < 0) {
                        setStartX(0);
                        setEndX(new_end_x - new_start_x);
                    }
                    // Being here means that the borders of the basemap have not been reached
                    else {
                        setStartX(new_start_x);
                        setEndX(new_end_x);
                        // Update the reference point (of dimension x) for the continuation of the dragging
                        mouseClickPozX = event.getX();
                    }

                    // Handle the vertical movement
                    // Check whether the bottom side of the area has exceeded the bottom side of the basemap
                    if (scaled_Img_MaxY <= new_end_y) {
                        setEndY(scaled_Img_MaxY);
                        setStartY(scaled_Img_MaxY - new_end_y + new_start_y);
                    }
                    // Check whether the bottom side of the area has exceeded the bottom side of the basemap
                    else if (new_start_y < 0) {
                        setStartY(0);
                        setEndY(new_end_y - new_start_y);
                    }
                    // Being here means that the borders of the basemap have not been reached
                    else {
                        setStartY(new_start_y);
                        setEndY(new_end_y);
                        // Update the reference point (of dimension y) for the continuation of the dragging
                        mouseClickPozY = event.getY();
                    }
                }
            }
            // This is the last custom listener so we can consume the clicks and avoid further consideration of them
            event.consume();
        });
    }

    void obstruction_got_selected_action(Plan_Designer_Controller Plan_Designer_panel_fxController) {
        // If there is any Zone or Obstruction selected, hide its snap points.
        if (selectedZone != null) {
            selectedZone.hide_snap_points();
        }
        if (selectedObstruction != null) {
            selectedObstruction.hide_snap_points();
        }

        // The next two need to have this specific order
        // since the listener of obstruction_has_been_selected uses the selectedObstruction
        selectedObstruction = this;
        Plan_Designer_Controller.obstruction_has_been_selected = true;

        Plan_Designer_panel_fxController.tab_pane_attribute_table_fx.getSelectionModel().select(
                Plan_Designer_panel_fxController.obstructions_tab_fx);

        try {
            int obstruction_id = selectedObstruction.getObstruction_ID();

            // Find the corresponding entry index within the table, of the directly selected obstruction
            int obstruction_table_index = 0;

            for (Obstruction obstruction : Plan_Designer_panel_fxController.obstructions_table_fx.getItems()) {
                if (obstruction_id == obstruction.getObstruction_ID()) {
                    break;
                }
                obstruction_table_index++;
            }

            Plan_Designer_panel_fxController.obstructions_table_fx.getSelectionModel().select(obstruction_table_index);
            Plan_Designer_panel_fxController.obstructions_table_fx.requestFocus();

        } catch (ClassCastException ignored) {
        }

        // Disable the buttons that the user should not press while having selected an area
        Plan_Designer_panel_fxController.add_ver_obstruction_fx.setDisable(true);
        Plan_Designer_panel_fxController.add_hor_obstruction_fx.setDisable(true);
        Plan_Designer_panel_fxController.add_new_zone_part_fx.setDisable(true);
        Plan_Designer_panel_fxController.main_editor_toolbox_fx.setDisable(true);
        show_snap_points();

        getParent().setCursor(Cursor.MOVE);

        //Reset the flag back to its default status
        directly_clicked = false;
    }

    private void set_visualization(Group group) {
        top_left.setFill(Color.color(1, 1, 0, 1));
        bottom_right.setFill(Color.color(1, 1, 0, 1));

        top_left.xProperty().bind(startXProperty().subtract(snap_offset));
        top_left.yProperty().bind(startYProperty().subtract(snap_offset));
        bottom_right.xProperty().bind(endXProperty().subtract(snap_offset));
        bottom_right.yProperty().bind(endYProperty().subtract(snap_offset));

        if (vertical_orientation) {
            top_left.xProperty().bind(startXProperty().subtract(snap_offset));
            top_left.yProperty().bind(startYProperty());
            bottom_right.xProperty().bind(endXProperty().subtract(snap_offset));
            bottom_right.yProperty().bind(endYProperty().subtract(snap_point_size));
        } else {
            top_left.xProperty().bind(startXProperty());
            top_left.yProperty().bind(startYProperty().subtract(snap_offset));
            bottom_right.xProperty().bind(endXProperty().subtract(snap_point_size));
            bottom_right.yProperty().bind(endYProperty().subtract(snap_offset));
        }

        top_left.setWidth(snap_point_size.getValue());
        top_left.setHeight(snap_point_size.getValue());
        bottom_right.setWidth(snap_point_size.getValue());
        bottom_right.setHeight(snap_point_size.getValue());

        // Hide the snap points by default
        top_left.setVisible(false);
        bottom_right.setVisible(false);
        group.getChildren().add(1, top_left);
        group.getChildren().add(1, bottom_right);
    }

    // This function is called right after every zoom in/out to resize the snap_points for better visualisation
    void refresh_snap_points(double new_size) {
        top_left.setWidth(new_size);
        top_left.setHeight(new_size);
        bottom_right.setWidth(new_size);
        bottom_right.setHeight(new_size);

        snap_point_size.setValue(new_size);
        setStrokeWidth(snap_point_size.getValue());
    }

    // Reveal the corresponding snap points
    private void show_snap_points() {
        top_left.setVisible(true);
        top_left.toFront();
        bottom_right.setVisible(true);
        bottom_right.toFront();
    }

    // Hide the corresponding snap points
    void hide_snap_points() {
        top_left.setVisible(false);
        bottom_right.setVisible(false);
    }

    public JSONObject get_as_JSON_obj() {
        JSONObject obj = new JSONObject();
        obj.put("obstruction_ID", obstruction_ID.getValue());
        obj.put("obstruction_Type", obstruction_Type.getValue());
        obj.put("obstruction_Color", obstruction_Color.getValue());
        obj.put("vertical_orientation", vertical_orientation);
        obj.put("start_x", getStartX());
        obj.put("start_y", getStartY());
        obj.put("end_x", getEndX());
        obj.put("end_y", getEndY());
        return obj;
    }

    // All following public setters and getters are required
    public int getObstruction_ID() {
        return obstruction_ID.get();
    }

    public void setObstruction_ID(int obstruction_ID) {
        this.obstruction_ID = new SimpleIntegerProperty(obstruction_ID);
    }

    public String getObstruction_Color() {
        return obstruction_Color.get();
    }

    public void setObstruction_Color(Color obstruction_Color) {
        this.obstruction_Color = new SimpleStringProperty(obstruction_Color.toString());
    }

    public String getObstruction_Type() {
        return obstruction_Type.get();
    }

    public void setObstruction_Type(String obstruction_Type) {
        this.obstruction_Type = new SimpleStringProperty(obstruction_Type);
    }
}