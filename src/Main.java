import com.cloudmade.api.CMClient;
import com.cloudmade.api.geometry.Geometry;
import com.cloudmade.api.geometry.Point;
import com.cloudmade.api.routing.Route;
import com.cloudmade.api.routing.RouteNotFoundException;
import org.jdesktop.swingx.JXMapKit;
import org.jdesktop.swingx.JXMapViewer;
import org.jdesktop.swingx.mapviewer.DefaultTileFactory;
import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.jdesktop.swingx.mapviewer.TileFactory;
import org.jdesktop.swingx.mapviewer.TileFactoryInfo;
import org.jdesktop.swingx.painter.Painter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;

/**
 * User: rbs
 * Date: 15.03.12
 */
public class Main extends JFrame {
    private CMClient client;
    private final ArrayList<GeoPosition> waypoints = new ArrayList<GeoPosition>();

    //private static final LatLng ATLANTA = new LatLng(33.7814790, -84.3880580);

    public Main(String target) throws RouteNotFoundException {

        client = new CMClient("a8fae8a4d89b43e6822be03011422ce6");

        Route route = client.route(
                new Point(50.802483, 12.729574),
                getFirstMatchingPointByString(target),
                CMClient.RouteType.CAR,
                null,
                null,
                "de",
                CMClient.MeasureUnit.KM
        );


        final JXMapKit mapKit = new JXMapKit();

        //mapKit.setDefaultProvider(JXMapKit.DefaultProviders.OpenStreetMaps);
        mapKit.setDataProviderCreditShown(true);
        mapKit.setDefaultProvider(JXMapKit.DefaultProviders.Custom);

        final int max = 17;
        TileFactoryInfo googlemaps = new TileFactoryInfo(0,max,max,
                256, true, true,
                "http://mt.google.com/mt?w=2.43", "x", "y", "zoom") {

            public String getTileUrl(int x, int y, int zoom) {
                zoom = max-zoom;
                return this.baseURL +"x="+x+"&y="+y+"&zoom="+(17-zoom);
            }
        };
        googlemaps.setDefaultZoomLevel(1);
        mapKit.setTileFactory(new DefaultTileFactory(googlemaps));


        add(mapKit);

        setSize(new Dimension(500, 500));
        setVisible(true);


        mapKit.setCenterPosition(new GeoPosition(50.802483, 12.729574));
        mapKit.setZoom(2);


        Painter<JXMapViewer> lineOverlay = new Painter<JXMapViewer>() {

            public void paint(Graphics2D g, JXMapViewer map, int w, int h) {
                g = (Graphics2D) g.create();
                //convert from viewport to world bitmap
                Rectangle rect = mapKit.getMainMap().getViewportBounds();
                g.translate(-rect.x, -rect.y);

                //do the drawing
                g.setColor(new Color(255, 0, 0));//, 150));
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setStroke(new BasicStroke(8));


                Polygon polygon = new Polygon();

                int lastX = -1;
                int lastY = -1;
                for (GeoPosition gp : waypoints) {
                    //convert geo to world bitmap pixel
                    Point2D pt = mapKit.getMainMap().getTileFactory().geoToPixel(gp, mapKit.getMainMap().getZoom());
                    if (lastX != -1 && lastY != -1) {
                        polygon.addPoint((int) pt.getX(), (int) pt.getY());
                        //g.drawLine(lastX, lastY, );
                    }
                    lastX = (int) pt.getX();
                    lastY = (int) pt.getY();
                }


                g.drawPolyline(polygon.xpoints, polygon.ypoints, polygon.npoints);

                g.dispose();
            }
        };

        mapKit.getMainMap().setOverlayPainter(lineOverlay);

        for (Point point : route.geometry.points) {
            waypoints.add(new GeoPosition(point.lat, point.lon));
        }

        System.out.println("km: " + route.summary.totalDistance / 1000 + "  Zeit: " + route.summary.totalTime/60);
        System.out.println(route.summary.startPoint);
        System.out.println(route.summary.endPoint);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public static void main(String[] args) {
        final JDialog dialog = new JDialog();
        dialog.setLayout(new BorderLayout());
        final JTextField target = new JTextField("15c, Landgraben, Hohenstein-Ernstthal");
        JButton button = new JButton("Start");

        dialog.add(new JLabel("Ziel?"), BorderLayout.NORTH);
        dialog.add(target, BorderLayout.CENTER);
        dialog.add(button, BorderLayout.SOUTH);
        dialog.setSize(new Dimension(300, 100));
        dialog.setVisible(true);

        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    dialog.dispose();
                    new Main(target.getText());
                } catch (RouteNotFoundException e2) {
                    e2.printStackTrace();
                }
            }
        });

    }
    
    private Point Geometry2Point(Geometry geo){
        String[] split = geo.toString().split(",");
        return new Point(Double.parseDouble(split[0]), Double.parseDouble(split[1]));
    }

    private GeoPosition Geometry2GeoPosition(Geometry geo){
        String[] split = geo.toString().split(",");
        return new GeoPosition(Double.parseDouble(split[0]), Double.parseDouble(split[1]));
    }

    
    private Point getFirstMatchingPointByString(String string){
        return Geometry2Point(client.find(string, 1, 0, null, false, true, true).results[0].centroid);
    }
}
