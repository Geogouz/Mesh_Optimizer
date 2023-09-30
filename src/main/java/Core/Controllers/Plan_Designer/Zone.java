package Core.Controllers.Plan_Designer;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Cursor;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import org.json.simple.JSONObject;

import static Core.Controllers.Plan_Designer.Plan_Designer_Controller.*;

public class Zone extends Area {

    private SimpleIntegerProperty zonePart_ID;
    private SimpleStringProperty groupName;
    private SimpleStringProperty zonePart_Color;

    static boolean directly_clicked = false;

    public Zone(double x, double y, double width, double height, int zone_part_id, String zone_name, Color zone_color,
                double rss, Plan_Designer_Controller Plan_Designer_panel_fxController) {
        super(x, y, width, height, Plan_Designer_panel_fxController.plan_designer_group_fx, rss);

        zonePart_ID = new SimpleIntegerProperty(zone_part_id);
        groupName = new SimpleStringProperty(zone_name);
        zonePart_Color = new SimpleStringProperty(zone_color.toString());

        // We add this object to the top
        group.getChildren().add(1, this);

        setFill(zone_color);

        addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            // Consider here the right click
            if (event.getButton() == MouseButton.SECONDARY) {
                directly_clicked = true;
                zone_got_selected_action(Plan_Designer_panel_fxController);
                mouseClickPozX = event.getX();
                mouseClickPozY = event.getY();

                Area_maxX_Allowed = scaled_Img_MaxX - super.getWidth();
                Area_maxY_Allowed = scaled_Img_MaxY - super.getHeight();
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

                    // Update the furthest points of the rectangle
                    rectangleEndX = super.getX() + super.getWidth();
                    rectangleEndY = super.getY() + super.getHeight();
                }
            }
        });
    }

    void zone_got_selected_action(Plan_Designer_Controller Plan_Designer_panel_fxController) {
        // If there is any Zone or Obstruction selected, hide its snap points.
        if (selectedZone != null) {
            selectedZone.hide_snap_points();
        }
        if (selectedObstruction != null) {
            selectedObstruction.hide_snap_points();
        }

        // The next two need to have this specific order
        // since the listener of area_has_been_selected uses the selectedZone
        selectedZone = this;
        Plan_Designer_Controller.area_has_been_selected = true;

        Plan_Designer_panel_fxController.tab_pane_attribute_table_fx.getSelectionModel().select(
                Plan_Designer_panel_fxController.zone_parts_tab_fx);

        try {
            int zone_part_id = selectedZone.getZonePart_ID();

            // Find the corresponding entry index within the table, of the directly selected zone part
            int zone_part_table_index = 0;
            for (Zone zone_part : Plan_Designer_panel_fxController.zone_parts_table_fx.getItems()) {
                if (zone_part_id == zone_part.getZonePart_ID()) {
                    break;
                }
                zone_part_table_index++;
            }

            Plan_Designer_panel_fxController.zone_parts_table_fx.getSelectionModel().select(zone_part_table_index);
            Plan_Designer_panel_fxController.zone_parts_table_fx.requestFocus();

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

    public JSONObject get_as_JSON_obj() {

        JSONObject obj = new JSONObject();
        obj.put("zonePart_ID", zonePart_ID.getValue());
        obj.put("zonePart_Color", zonePart_Color.getValue());
        obj.put("groupName", groupName.getValue());
        obj.put("x", this.getX());
        obj.put("y", this.getY());
        obj.put("width", this.getWidth());
        obj.put("height", this.getHeight());

        return obj;
    }

    // All following public setters and getters are required
    public int getZonePart_ID() {
        return zonePart_ID.get();
    }

    public void setZonePart_ID(int zonePart_ID) {
        this.zonePart_ID = new SimpleIntegerProperty(zonePart_ID);
    }

    public String getZonePart_Color() {
        return zonePart_Color.get();
    }

    public void setZonePart_Color(Color zonePart_Color) {
        this.zonePart_Color = new SimpleStringProperty(zonePart_Color.toString());
    }

    public String getGroupName() {
        return groupName.get();
    }

    public void setGroupName(String groupName) {
        this.groupName = new SimpleStringProperty(groupName);
    }
}