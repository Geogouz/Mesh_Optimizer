package Core.Controllers.Nodes_Optimizer;

/**
 * This snippet finds the intersection of two line segments.
 * The intersection may either be empty, a single point or the
 * intersection is a subsegment there's an overlap.
 */

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.*;

public class LineSegmentLineSegmentIntersection {

    // Small epsilon used for double value comparison.
    private static final double EPS = 1e-5;

    static private boolean equals(double p1_1, double p1_2, double p2_1, double p2_2) {
        return abs(p1_1 - p2_1) < EPS && abs(p1_2 - p2_2) < EPS;
    }

    // Finds the orientation of point 'c' relative to the line segment (a, b)
    // Returns  0 if all three points are collinear.
    // Returns -1 if 'c' is clockwise to segment (a, b), i.e right of line formed by the segment.
    // Returns +1 if 'c' is counter clockwise to segment (a, b), i.e left of line
    // formed by the segment.
    private static int orientation(double a_1, double a_2, double b_1, double b_2, double c_1, double c_2) {   /* p1_1, p1_2, p2_1, p2_2, p3_1, p3_2, p4_1, p4_2 */
        double value = (b_2 - a_2) * (c_1 - b_1) - (b_1 - a_1) * (c_2 - b_2);
        if (abs(value) < EPS) return 0;
        return (value > 0) ? -1 : +1;
    }

    // Tests whether point 'c' is on the line segment (a, b).
    // Ensure first that point c is collinear to segment (a, b) and
    // then check whether c is within the rectangle formed by (a, b)
    private static boolean pointOnLine(double a_1, double a_2, double b_1, double b_2, double c_1, double c_2) {
        return orientation(a_1, a_2, b_1, b_2, c_1, c_2) == 0 &&
                min(a_1, b_1) <= c_1 && c_1 <= max(a_1, b_1) &&
                min(a_2, b_2) <= c_2 && c_2 <= max(a_2, b_2);
    }

    // Determines whether two segments intersect.
    private static boolean segmentsIntersect(double p1_1, double p1_2, double p2_1, double p2_2, double p3_1, double p3_2, double p4_1, double p4_2) {

        // Get the orientation of points p3 and p4 in relation
        // to the line segment (p1, p2)
        int o1 = orientation(p1_1, p1_2, p2_1, p2_2, p3_1, p3_2);
        int o2 = orientation(p1_1, p1_2, p2_1, p2_2, p4_1, p4_2);
        int o3 = orientation(p3_1, p3_2, p4_1, p4_2, p1_1, p1_2);
        int o4 = orientation(p3_1, p3_2, p4_1, p4_2, p2_1, p2_2);

        // If the points p1, p2 are on opposite sides of the infinite
        // line formed by (p3, p4) and conversly p3, p4 are on opposite
        // sides of the infinite line formed by (p1, p2) then there is
        // an intersection.
        if (o1 != o2 && o3 != o4) return true;

        // Collinear special cases (perhaps these if checks can be simplified?)
        if (o1 == 0 && pointOnLine(p1_1, p1_2, p2_1, p2_2, p3_1, p3_2)) return true;
        if (o2 == 0 && pointOnLine(p1_1, p1_2, p2_1, p2_2, p4_1, p4_2)) return true;
        if (o3 == 0 && pointOnLine(p3_1, p3_2, p4_1, p4_2, p1_1, p1_2)) return true;
        return o4 == 0 && pointOnLine(p3_1, p3_2, p4_1, p4_2, p2_1, p2_2);
    }

    private static List<double[]> getCommonEndpoints(double p1_1, double p1_2, double p2_1, double p2_2, double p3_1, double p3_2, double p4_1, double p4_2) {

        List<double[]> points = new ArrayList<>();

        if (equals(p1_1, p1_2, p3_1, p3_2)) {
            points.add(new double[]{p1_1, p1_2});
            if (equals(p2_1, p2_2, p4_1, p4_2)) points.add(new double[]{p2_1, p2_2});

        } else if (equals(p1_1, p1_2, p4_1, p4_2)) {
            points.add(new double[]{p1_1, p1_2});
            if (equals(p2_1, p2_2, p3_1, p3_2)) points.add(new double[]{p2_1, p2_2});

        } else if (equals(p2_1, p2_2, p3_1, p3_2)) {
            points.add(new double[]{p2_1, p2_2});
            if (equals(p1_1, p1_2, p4_1, p4_2)) points.add(new double[]{p1_1, p1_2});

        } else if (equals(p2_1, p2_2, p4_1, p4_2)) {
            points.add(new double[]{p2_1, p2_2});
            if (equals(p1_1, p1_2, p3_1, p3_2)) points.add(new double[]{p1_1, p1_2});
        }

        return points;
    }

    // Finds the intersection point(s) of two line segments. Unlike regular line
    // segments, segments which are points (x1 = x2 and y1 = y2) are allowed.
    public static double[] lineSegmentLineSegmentIntersection(
            double p1_1, double p1_2, double p2_1, double p2_2, double p3_1, double p3_2, double p4_1, double p4_2) {

        // No intersection.
        if (!segmentsIntersect(p1_1, p1_2, p2_1, p2_2, p3_1, p3_2, p4_1, p4_2)) return null;

        // Both segments are a single point.
        if (equals(p1_1, p1_2, p2_1, p2_2) && equals(p2_1, p2_2, p3_1, p3_2) && equals(p3_1, p3_2, p4_1, p4_2))

            return new double[]{p1_1, p1_2};

        List<double[]> endpoints = getCommonEndpoints(p1_1, p1_2, p2_1, p2_2, p3_1, p3_2, p4_1, p4_2);
        int n = endpoints.size();

        // One of the line segments is an intersecting single point.
        // NOTE: checking only n == 1 is insufficient to return early
        // because the solution might be a sub segment.
        boolean singleton = equals(p1_1, p1_2, p2_1, p2_2) || equals(p3_1, p3_2, p4_1, p4_2);
        if (n == 1 && singleton) return endpoints.get(0);

        // Segments are equal.
        if (n == 2) return endpoints.get(0);

        boolean collinearSegments = (orientation(p1_1, p1_2, p2_1, p2_2, p3_1, p3_2) == 0) &&
                (orientation(p1_1, p1_2, p2_1, p2_2, p4_1, p4_2) == 0);

        // The intersection will be a sub-segment of the two
        // segments since they overlap each other.
        if (collinearSegments) {

            // Segment #2 is enclosed in segment #1
            if (pointOnLine(p1_1, p1_2, p2_1, p2_2, p3_1, p3_2) && pointOnLine(p1_1, p1_2, p2_1, p2_2, p4_1, p4_2))
                return new double[]{p3_1, p3_2};

            // Segment #1 is enclosed in segment #2
            if (pointOnLine(p3_1, p3_2, p4_1, p4_2, p1_1, p1_2) && pointOnLine(p3_1, p3_2, p4_1, p4_2, p2_1, p2_2))
                return new double[]{p1_1, p1_2};

            // The subsegment is part of segment #1 and part of segment #2.
            // Find the middle points which correspond to this segment.
            double[] midPoint1 = pointOnLine(p1_1, p1_2, p2_1, p2_2, p3_1, p3_2) ? new double[]{p3_1, p3_2} : new double[]{p4_1, p4_2};
            double[] midPoint2 = pointOnLine(p3_1, p3_2, p4_1, p4_2, p1_1, p1_2) ? new double[]{p1_1, p1_2} : new double[]{p2_1, p2_2};

            // There is actually only one middle point!
            if (equals(midPoint1[0], midPoint1[1], midPoint2[0], midPoint2[1])) return midPoint1;

            return midPoint1;
        }

        /* Beyond this point there is a unique intersection point. */

        // Segment #1 is a vertical line.
        if (abs(p1_1 - p2_1) < EPS) {
            double m = (p4_2 - p3_2) / (p4_1 - p3_1);
            double b = p3_2 - m * p3_1;
            return new double[]{p1_1, m * p1_1 + b};
        }

        // Segment #2 is a vertical line.
        if (abs(p3_1 - p4_1) < EPS) {
            double m = (p2_2 - p1_2) / (p2_1 - p1_1);
            double b = p1_2 - m * p1_1;
            return new double[]{p3_1, m * p3_1 + b};
        }

        double m1 = (p2_2 - p1_2) / (p2_1 - p1_1);
        double m2 = (p4_2 - p3_2) / (p4_1 - p3_1);
        double b1 = p1_2 - m1 * p1_1;
        double b2 = p3_2 - m2 * p3_1;
        double x = (b2 - b1) / (m1 - m2);
        double y = (m1 * b2 - m2 * b1) / (m1 - m2);
        return new double[]{x, y};
    }

}