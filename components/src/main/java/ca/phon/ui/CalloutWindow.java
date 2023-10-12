package ca.phon.ui;

import ca.phon.ui.action.PhonUIAction;
import ca.phon.util.icons.IconManager;
import ca.phon.util.icons.IconSize;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;

public class CalloutWindow extends JDialog {
    private final static int TRIANGLE_HEIGHT = 12;
    private final static int TRIANGLE_BASE = 20;
    private final static int ARROW_EDGE_PADDING = 4;
    private Component content;
    private Shape shape;
    private Point relativeArrowPoint = null;
    private int cornerRadius = 4;
    private int arrowCornerRadius = 2;

    public CalloutWindow(JFrame frame, Component content, int sideOfWindow, int topMiddleBottom, Point pointAtPos) {
        super(frame, false);
        this.content = content;
        init(sideOfWindow, topMiddleBottom, pointAtPos);
        getContentPane().setBackground(UIManager.getColor("CalloutWindow.background"));
    }

    private void init(int sideOfWindow, int topMiddleBottom, Point pointAtPos) {
        int topOfRect = sideOfWindow == SwingConstants.NORTH ? TRIANGLE_HEIGHT : 0;
        int bottomOfRect = sideOfWindow == SwingConstants.SOUTH ? TRIANGLE_HEIGHT : 0;
        int leftOfRect = sideOfWindow == SwingConstants.WEST ? TRIANGLE_HEIGHT : 0;
        int rightOfRect = sideOfWindow == SwingConstants.EAST ? TRIANGLE_HEIGHT : 0;

        Dimension d = content.getPreferredSize();

        // region Close button and panel

        JPanel closePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        closePanel.setBorder(new EmptyBorder(topOfRect, 0, 0, rightOfRect));
        JButton closeButton = new JButton();
        PhonUIAction<Void> closeAct = PhonUIAction.eventConsumer((e) -> dispose(), null);
        final ImageIcon closeIcon =
                IconManager.getInstance().getIcon("actions/button_cancel", IconSize.XSMALL);
        closeAct.putValue(PhonUIAction.SMALL_ICON, closeIcon);
        closeButton.setAction(closeAct);
        closeButton.setPreferredSize(new Dimension(12, 12));
        closeButton.setBorder(null);
        closeButton.setOpaque(false);
        closePanel.add(closeButton);

        // endregion Close button and panel

        shape = createShape(
            (int) (d.getWidth()),
            (int) (d.getHeight() + closePanel.getPreferredSize().getHeight() + cornerRadius),
            TRIANGLE_BASE,
            TRIANGLE_HEIGHT,
            cornerRadius,
            sideOfWindow,
            topMiddleBottom
        );
        setUndecorated(true);
        setResizable(true);
        setLocationRelativeTo(null);
        setShape(shape);
        setSize(
            (int) (d.getWidth() + leftOfRect + rightOfRect),
            (int) (d.getHeight() + topOfRect + bottomOfRect + closePanel.getPreferredSize().getHeight() + cornerRadius)
        );
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setSize(d);
        contentPanel.setBorder(new EmptyBorder(
            0,
            leftOfRect,
            bottomOfRect + cornerRadius,
            rightOfRect
        ));
        contentPanel.add(content, BorderLayout.CENTER);
        add(contentPanel, BorderLayout.CENTER);


        add(closePanel, BorderLayout.NORTH);

        closePanel.setOpaque(false);
        contentPanel.setOpaque(false);

        addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowLostFocus(WindowEvent e) {
                dispose();
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
            if (!getShape().contains(e.getPoint())) {
                dispose();
            }
            }
        });

        if (pointAtPos != null) {
            setLocation(pointAtPos.x - relativeArrowPoint.x, pointAtPos.y - relativeArrowPoint.y);
        }
    }

    public static void showCallout(JFrame owner, Component content, int sideOfWindow, int topMiddleBottom, Point pointAtPos) {
        // Create a custom JDialog
        CalloutWindow dialog = new CalloutWindow(owner, content, sideOfWindow, topMiddleBottom, pointAtPos);
        dialog.setVisible(true);
    }

    private Shape createShape(int width, int height, int triangleBase, int triangleHeight, int cornerRadius, int sideOfWindow, int topMiddleBottom) {
        // Create a Path2D object
        Path2D.Double shape = new Path2D.Double();

        int topOfRect = sideOfWindow == SwingConstants.NORTH ? triangleHeight : 0;
        int leftOfRect = sideOfWindow == SwingConstants.WEST ? triangleHeight : 0;

        int horiOffset = 0;
        int vertOffset = 0;
        if (topMiddleBottom == SwingConstants.LEADING) {
            horiOffset = -(((width - triangleBase) / 2) - cornerRadius) + ARROW_EDGE_PADDING;
            vertOffset = -(((height - triangleBase) / 2) - cornerRadius) + ARROW_EDGE_PADDING;
        }
        else if (topMiddleBottom == SwingConstants.TRAILING) {
            horiOffset = (((width - triangleBase) / 2) - cornerRadius) - ARROW_EDGE_PADDING;
            vertOffset = (((height - triangleBase) / 2) - cornerRadius) - ARROW_EDGE_PADDING;
        }

        double angleToArrow = Math.atan(2 * TRIANGLE_HEIGHT / (double)TRIANGLE_BASE);
        double n = angleToArrow / (2 * Math.PI);
        double dist = 4 * Math.atan2((2 * n), Math.PI) / 3;

        System.out.println("Dist: " + dist);

        System.out.println("Angle: " + Math.toDegrees(angleToArrow));

        System.out.println("Width: " + width);

        // Start the path in the top-left corner of the rectangle, considering the corner radius
        shape.moveTo(leftOfRect + cornerRadius, topOfRect);

        // Top
        if (sideOfWindow == SwingConstants.NORTH) {
            System.out.println("Top");

            // Prev corner to start of arrow curve
            Point startArrowCurveStart = new Point(horiOffset + ((width - triangleBase) / 2), topOfRect);
            shape.lineTo(startArrowCurveStart.getX(), startArrowCurveStart.getY());

            /*double inverseAngleToArrow = Math.atan(-TRIANGLE_BASE / (2.0 * TRIANGLE_HEIGHT));

            Point startArrowCurveEnd = new Point(
                (int) (arrowCornerRadius * Math.cos(inverseAngleToArrow)) + startArrowCurveStart.x,
                (int) (arrowCornerRadius * Math.sin(inverseAngleToArrow)) + startArrowCurveStart.y + arrowCornerRadius
            );

            Point startArrowCurveControlPoint1 = new Point(
                (int) (startArrowCurveStart.x + (arrowCornerRadius * dist)),
                startArrowCurveStart.y
            );

            Point startArrowCurveControlPoint2 = new Point(
                (int) (startArrowCurveEnd.x - (Math.cos(angleToArrow) * dist)),
                (int) (startArrowCurveEnd.y - (Math.sin(angleToArrow) * dist))
            );

            System.out.println("Point: " + startArrowCurveEnd);





            // Start of arrow curve
            shape.curveTo(
                startArrowCurveControlPoint1.x,
                startArrowCurveControlPoint1.y,
                startArrowCurveControlPoint2.x,
                startArrowCurveControlPoint2.y,
                startArrowCurveEnd.x,
                startArrowCurveEnd.y
            );*/
            // Start of arrow curve to point
            shape.lineTo(horiOffset + (width / 2), 0);
            relativeArrowPoint = new Point(horiOffset + (width / 2), 0);
            // Point to end of arrow curve
            shape.lineTo(horiOffset + (width + triangleBase) / 2, topOfRect);
            // End of arrow curve
            //shape.quadTo();
        }
        shape.lineTo(leftOfRect + width - cornerRadius, topOfRect);

        // Top right corner
        shape.quadTo(leftOfRect + width - cornerRadius, topOfRect, width + leftOfRect, topOfRect + cornerRadius);

        // Right
        if (sideOfWindow == SwingConstants.EAST) {
            System.out.println("Right");
            shape.lineTo(width, vertOffset + (height - triangleBase) / 2);
            shape.lineTo(width + triangleHeight, vertOffset + height / 2);
            relativeArrowPoint = new Point(width + triangleHeight, vertOffset + height / 2);
            shape.lineTo(width, vertOffset + (height + triangleBase) / 2);
        }
        shape.lineTo(leftOfRect + width, height - cornerRadius + topOfRect);

        // Bottom right corner
        shape.quadTo(leftOfRect + width, topOfRect + height - cornerRadius, width - cornerRadius + leftOfRect, height + topOfRect);

        // Bottom
        if (sideOfWindow == SwingConstants.SOUTH) {
            System.out.println("Bottom");
            shape.lineTo(leftOfRect + horiOffset + (width + triangleBase) / 2, height);
            shape.lineTo(leftOfRect + horiOffset + (width / 2), height + triangleHeight);
            relativeArrowPoint = new Point(leftOfRect + horiOffset + (width / 2), height + triangleHeight);
            shape.lineTo(leftOfRect + horiOffset + (width - triangleBase) / 2, height);
        }
        shape.lineTo(leftOfRect + cornerRadius, topOfRect + height);

        // Bottom left corner
        shape.quadTo(leftOfRect, topOfRect + height, leftOfRect, topOfRect + height - cornerRadius);

        // Left
        if (sideOfWindow == SwingConstants.WEST) {
            shape.lineTo(triangleHeight, vertOffset + (height + triangleBase) / 2);
            shape.lineTo(0, vertOffset + height / 2);
            relativeArrowPoint = new Point(0, vertOffset + height / 2);
            shape.lineTo(triangleHeight, vertOffset + (height - triangleBase) / 2);
        }
        shape.lineTo(leftOfRect, topOfRect + cornerRadius);

        // Top left corner
        shape.quadTo(leftOfRect, topOfRect + cornerRadius, cornerRadius + leftOfRect, topOfRect);

        // Close path and return
        shape.closePath();
        return shape;
    }
}
