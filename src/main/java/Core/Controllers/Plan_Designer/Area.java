package Core.Controllers.Plan_Designer;

import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import static Core.Controllers.Plan_Designer.Plan_Designer_Controller.*;

public class Area extends Rectangle {

    boolean about_to_resize = false;
    double mouseClickPozX;
    double mouseClickPozY;
    private double minimumAllowedWidth;
    private double minimumAllowedHeight;
    double rectangleEndX;
    double rectangleEndY;
    private double snap_point_size;
    double Area_maxX_Allowed;
    double Area_maxY_Allowed;
    public double[] centroid = new double[2];
    Group group;

    // This array will contain the snap points that are used for the area reshaping
    Rectangle[] snap_points = new Rectangle[8];

    Area(double x, double y, double width, double height, Group group, double sps) {
        super(x, y, width, height);
        super.setStrokeWidth(0);

        centroid[0] = x + width / 2;
        centroid[1] = y + height / 2;

        this.group = group;
        snap_point_size = sps;

        // Update the furthest points of the rectangle
        rectangleEndX = x + width;
        rectangleEndY = y + height;

        minimumAllowedWidth = 3 * snap_point_size;
        minimumAllowedHeight = 3 * snap_point_size;

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

        // Handle the movement of the area
        this.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {

            // Make sure we are in pan mode
            if (!ready_to_draw_a_new_obstruction && !ready_to_draw_a_new_zone) {

                // Consider here the right click
                if (event.getButton() == MouseButton.SECONDARY) {

                    double offsetX = event.getX() - mouseClickPozX;
                    double offsetY = event.getY() - mouseClickPozY;

                    double newX = super.getX() + offsetX;
                    double newY = super.getY() + offsetY;

                    // Handle the horizontal movement
                    // Check whether the right side of the area has exceeded the right side of the basemap
                    if (Area_maxX_Allowed <= newX) {
                        super.setX(Area_maxX_Allowed);
                    }
                    // Check whether the left side of the area has exceeded the left side of the basemap
                    else if (newX < 0) {
                        super.setX(0);
                    }
                    // Being here means that the borders of the basemap have not been reached
                    else {
                        super.setX(newX);
                        // Update the reference point (of dimension x) for the continuation of the dragging
                        mouseClickPozX = event.getX();
                    }

                    // Handle the vertical movement
                    // Check whether the bottom side of the area has exceeded the bottom side of the basemap
                    if (Area_maxY_Allowed <= newY) {
                        super.setY(Area_maxY_Allowed);
                    }
                    // Check whether the bottom side of the area has exceeded the bottom side of the basemap
                    else if (newY < 0) {
                        super.setY(0);
                    }

                    // Being here means that the borders of the basemap have not been reached
                    else {
                        super.setY(newY);

                        // Update the reference point (of dimension y) for the continuation of the dragging
                        mouseClickPozY = event.getY();
                    }
                }
            }
            // This is the last custom listener so we can consume the clicks and avoid further consideration of them
            event.consume();
        });

        snap_points[0] = makeNWResizerSquare(group);
        snap_points[1] = makeCWResizerSquare(group);
        snap_points[2] = makeSWResizerSquare(group);
        snap_points[3] = makeSCResizerSquare(group);
        snap_points[4] = makeSEResizerSquare(group);
        snap_points[5] = makeCEResizerSquare(group);
        snap_points[6] = makeNEResizerSquare(group);
        snap_points[7] = makeNCResizerSquare(group);
    }

    private Rectangle makeNWResizerSquare(Group group) {
        Rectangle squareNW = new Rectangle(snap_point_size, snap_point_size);

        // Hide it by default
        squareNW.setVisible(false);

        squareNW.xProperty().bind(super.xProperty());
        squareNW.yProperty().bind(super.yProperty());
        group.getChildren().add(1, squareNW);

        squareNW.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> {
            squareNW.getParent().setCursor(Cursor.NW_RESIZE);
            about_to_resize = true;
        });
        prepareResizerSquare(squareNW);

        squareNW.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {

            // Consider only the right click
            if (event.getButton() == MouseButton.SECONDARY) {
                double offsetX = event.getX() - super.getX();
                double offsetY = event.getY() - super.getY();
                double newX = super.getX() + offsetX;
                double newY = super.getY() + offsetY;

                if (newX >= 0 && newX <= super.getX() + super.getWidth()) {
                    super.setX(newX);
                    super.setWidth(super.getWidth() - offsetX);
                }

                if (newY >= 0 && newY <= super.getY() + super.getHeight()) {
                    super.setY(newY);
                    super.setHeight(super.getHeight() - offsetY);
                }
            }

            // This is the last custom listener so we can consume the clicks and avoid further consideration of them
            event.consume();
        });

        return squareNW;
    }

    private Rectangle makeCWResizerSquare(Group group) {
        Rectangle squareCW = new Rectangle(snap_point_size, snap_point_size);

        // Hide it by default
        squareCW.setVisible(false);

        squareCW.xProperty().bind(super.xProperty());
        squareCW.yProperty().bind(super.yProperty().add(super.heightProperty().divide(2.0).subtract(
                squareCW.heightProperty().divide(2.0))));
        group.getChildren().add(1, squareCW);

        squareCW.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> {
            squareCW.getParent().setCursor(Cursor.W_RESIZE);
            about_to_resize = true;
        });
        prepareResizerSquare(squareCW);

        squareCW.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {

            // Consider only the right click
            if (event.getButton() == MouseButton.SECONDARY) {

                double newWidth = rectangleEndX - event.getX();

                // Check whether the new width is less than the minimum allowed
                if (newWidth <= minimumAllowedWidth) {
                    super.setX(rectangleEndX - minimumAllowedWidth);
                    super.setWidth(minimumAllowedWidth);
                }

                // Ensure that the resizing did not exceed the basemap's left borders
                else if (event.getX() > 0) {
                    super.setX(event.getX());
                    super.setWidth(rectangleEndX - event.getX());
                }

                // In this case, we have reached the left borders of the basemap
                else {
                    super.setX(0);
                    super.setWidth(rectangleEndX);
                }
            }

            // This is the last custom listener so we can consume the clicks and avoid further consideration of them
            event.consume();
        });

        return squareCW;
    }

    private Rectangle makeSWResizerSquare(Group group) {
        Rectangle squareSW = new Rectangle(snap_point_size, snap_point_size);

        // Hide it by default
        squareSW.setVisible(false);

        squareSW.xProperty().bind(super.xProperty());
        squareSW.yProperty().bind(super.yProperty().add(super.heightProperty().subtract(
                squareSW.heightProperty())));
        group.getChildren().add(1, squareSW);

        squareSW.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> {
            squareSW.getParent().setCursor(Cursor.SW_RESIZE);
            about_to_resize = true;
        });
        prepareResizerSquare(squareSW);

        squareSW.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {

            // Consider only the right click
            if (event.getButton() == MouseButton.SECONDARY) {
                double offsetX = event.getX() - super.getX();
                double offsetY = event.getY() - super.getY();
                double newX = super.getX() + offsetX;

                if (newX >= 0 && newX <= super.getX() + super.getWidth()) {
                    super.setX(newX);
                    super.setWidth(super.getWidth() - offsetX);
                }

                if (offsetY >= 0 && offsetY <= super.getY() + super.getHeight()) {
                    super.setHeight(offsetY);
                }
            }

            // This is the last custom listener so we can consume the clicks and avoid further consideration of them
            event.consume();
        });

        return squareSW;
    }

    private Rectangle makeSCResizerSquare(Group group) {
        Rectangle squareSC = new Rectangle(snap_point_size, snap_point_size);

        // Hide it by default
        squareSC.setVisible(false);

        squareSC.xProperty().bind(super.xProperty().add(super.widthProperty().divide(2.0).subtract(
                squareSC.widthProperty().divide(2.0))));
        squareSC.yProperty().bind(super.yProperty().add(super.heightProperty().subtract(
                squareSC.heightProperty())));
        group.getChildren().add(1, squareSC);

        squareSC.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> {
            squareSC.getParent().setCursor(Cursor.S_RESIZE);
            about_to_resize = true;
        });
        prepareResizerSquare(squareSC);

        squareSC.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {

            // Consider only the right click
            if (event.getButton() == MouseButton.SECONDARY) {

                double newHeight = event.getY() - super.getY();

                // Check whether the new height is less than the minimum allowed
                if (newHeight <= minimumAllowedHeight) {
                    super.setHeight(minimumAllowedHeight);
                }

                // Ensure that the resizing did not exceed the basemap's bottom borders
                else if (event.getY() < scaled_Img_MaxY) {
                    super.setHeight(newHeight);
                }

                // In this case, we have reached the bottom borders of the basemap
                else {
                    super.setHeight(scaled_Img_MaxY - super.getY());
                }

                // Update the furthest point of the rectangle
                rectangleEndY = super.getY() + super.getHeight();
            }

            // This is the last custom listener so we can consume the clicks and avoid further consideration of them
            event.consume();
        });

        return squareSC;
    }

    private Rectangle makeSEResizerSquare(Group group) {
        Rectangle squareSE = new Rectangle(snap_point_size, snap_point_size);

        // Hide it by default
        squareSE.setVisible(false);

        squareSE.xProperty().bind(super.xProperty().add(super.widthProperty()).subtract(
                squareSE.widthProperty()));
        squareSE.yProperty().bind(super.yProperty().add(super.heightProperty().subtract(
                squareSE.heightProperty())));
        group.getChildren().add(1, squareSE);

        squareSE.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> {
            squareSE.getParent().setCursor(Cursor.SE_RESIZE);
            about_to_resize = true;
        });
        prepareResizerSquare(squareSE);

        squareSE.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {

            // Consider only the right click
            if (event.getButton() == MouseButton.SECONDARY) {
                double offsetX = event.getX() - super.getX();
                double offsetY = event.getY() - super.getY();

                if (offsetX >= 0 && offsetX <= super.getX() + super.getWidth()) {
                    super.setWidth(offsetX);
                }

                if (offsetY >= 0 && offsetY <= super.getY() + super.getHeight()) {
                    super.setHeight(offsetY);
                }
            }

            // This is the last custom listener so we can consume the clicks and avoid further consideration of them
            event.consume();
        });

        return squareSE;
    }

    private Rectangle makeCEResizerSquare(Group group) {
        Rectangle squareCE = new Rectangle(snap_point_size, snap_point_size);

        // Hide it by default
        squareCE.setVisible(false);

        squareCE.xProperty().bind(super.xProperty().add(super.widthProperty()).subtract(
                squareCE.widthProperty()));
        squareCE.yProperty().bind(super.yProperty().add(super.heightProperty().divide(2.0).subtract(
                squareCE.heightProperty().divide(2.0))));
        group.getChildren().add(1, squareCE);

        squareCE.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> {
            squareCE.getParent().setCursor(Cursor.E_RESIZE);
            about_to_resize = true;
        });
        prepareResizerSquare(squareCE);

        squareCE.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {

            // Consider only the right click
            if (event.getButton() == MouseButton.SECONDARY) {

                double newWidth = event.getX() - super.getX();

                // Check whether the new width is less than the minimum allowed
                if (newWidth <= minimumAllowedWidth) {
                    super.setWidth(minimumAllowedWidth);
                }

                // Ensure that the resizing did not exceed the basemap's right borders
                else if (event.getX() < scaled_Img_MaxX) {
                    super.setWidth(newWidth);
                }

                // In this case, we have reached the right borders of the basemap
                else {
                    super.setWidth(scaled_Img_MaxX - super.getX());
                }

                // Update the furthest point of the rectangle
                rectangleEndX = super.getX() + super.getWidth();
            }

            // This is the last custom listener so we can consume the clicks and avoid further consideration of them
            event.consume();
        });

        return squareCE;
    }

    private Rectangle makeNEResizerSquare(Group group) {
        Rectangle squareNE = new Rectangle(snap_point_size, snap_point_size);

        // Hide it by default
        squareNE.setVisible(false);

        squareNE.xProperty().bind(super.xProperty().add(super.widthProperty()).subtract(
                squareNE.widthProperty()));
        squareNE.yProperty().bind(super.yProperty());
        group.getChildren().add(1, squareNE);

        squareNE.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> {
            squareNE.getParent().setCursor(Cursor.NE_RESIZE);
            about_to_resize = true;
        });
        prepareResizerSquare(squareNE);

        squareNE.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {

            // Consider only the right click
            if (event.getButton() == MouseButton.SECONDARY) {
                double offsetX = event.getX() - super.getX();
                double offsetY = event.getY() - super.getY();
                double newY = super.getY() + offsetY;

                if (offsetX >= 0 && offsetX <= super.getX() + super.getWidth()) {
                    super.setWidth(offsetX);
                }

                if (newY >= 0 && newY <= super.getY() + super.getHeight()) {
                    super.setY(newY);
                    super.setHeight(super.getHeight() - offsetY);
                }
            }

            // This is the last custom listener so we can consume the clicks and avoid further consideration of them
            event.consume();
        });

        return squareNE;
    }

    private Rectangle makeNCResizerSquare(Group group) {
        Rectangle squareNC = new Rectangle(snap_point_size, snap_point_size);

        // Hide it by default
        squareNC.setVisible(false);

        squareNC.xProperty().bind(super.xProperty().add(super.widthProperty().divide(2.0).subtract(
                squareNC.widthProperty().divide(2.0))));
        squareNC.yProperty().bind(super.yProperty());
        group.getChildren().add(1, squareNC);

        squareNC.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> {
            squareNC.getParent().setCursor(Cursor.N_RESIZE);
            about_to_resize = true;
        });
        prepareResizerSquare(squareNC);

        squareNC.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {

            // Consider only the right click
            if (event.getButton() == MouseButton.SECONDARY) {

                double newHeight = rectangleEndY - event.getY();

                // Check whether the new height is less than the minimum allowed
                if (newHeight <= minimumAllowedHeight) {
                    super.setY(rectangleEndY - minimumAllowedHeight);
                    super.setHeight(minimumAllowedHeight);
                }

                // Ensure that the resizing did not exceed the basemap's top borders
                else if (event.getY() > 0) {
                    super.setY(event.getY());
                    super.setHeight(rectangleEndY - event.getY());
                }

                // In this case, we have reached the top borders of the basemap
                else {
                    super.setY(0);
                    super.setHeight(rectangleEndY);
                }
            }

            // This is the last custom listener so we can consume the clicks and avoid further consideration of them
            event.consume();
        });

        return squareNC;
    }

    private void prepareResizerSquare(Rectangle rect) {
        rect.setFill(Color.BLACK);

        rect.addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
            rect.getParent().setCursor(Cursor.DEFAULT);
            about_to_resize = false;
        });
    }

    // This function is called right after every zoom in/out to resize the snap_points for better visualisation
    void refresh_snap_points(double new_size) {
        for (Rectangle snap_point : snap_points) {
            // accessing each element of array
            snap_point.setWidth(new_size);
            snap_point.setHeight(new_size);
            snap_point_size = new_size;
            minimumAllowedWidth = 3 * snap_point_size;
            minimumAllowedHeight = 3 * snap_point_size;
        }
    }

    // Reveal the corresponding snap points
    void show_snap_points() {
        for (Rectangle snap_point : snap_points) {
            snap_point.setVisible(true);
            //Place the snap points also to the top of the canvas so they are selectable
            snap_point.toFront();
        }
    }

    // Hide the corresponding snap points
    void hide_snap_points() {
        for (Rectangle snap_point : snap_points) {
            snap_point.setVisible(false);
        }
    }
}
