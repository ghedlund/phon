package ca.phon.ui;

import ca.phon.ui.action.PhonUIAction;
import ca.phon.util.OSInfo;
import ca.phon.util.icons.IconManager;
import ca.phon.util.icons.IconSize;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;

/**
 * Window with a callout arrow that can be shown at a specific location on the screen.
 * This class is used to create a custom JDialog with a callout arrow that can point to a specific location on the screen.
 * The callout arrow can be positioned on the north, south, east, or west side of the window.
 */
public class CalloutWindow extends JDialog {
    private final static int TRIANGLE_HEIGHT = 12;
    private final static int TRIANGLE_BASE = 20;
    private final static int ARROW_EDGE_PADDING = 4;
    private JComponent content;
    private Shape shape;
    private Shape borderShape;
    private JPanel contentPanel;
    private JPanel closePanel;

    private Point relativeArrowPoint = null;
    private int cornerRadius = 3;
    private int arrowCornerRadius = 2;

    public CalloutWindow(JFrame frame, JComponent content, int sideOfWindow, Point pointAtPos) {
        this(frame, content, sideOfWindow, new Rectangle(pointAtPos));
    }

    public CalloutWindow(JFrame frame, JComponent content, int sideOfWindow, Rectangle pointAtRect) {
        super(frame, false);
        this.content = content;
        this.content.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
        init(sideOfWindow, pointAtRect);

        final KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        final PhonUIAction<Void> closeAct = PhonUIAction.runnable(this::dispose);

        final JComponent contentPanel = (JComponent)getContentPane();
        contentPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks, "close");
        contentPanel.getActionMap().put("close", closeAct);

        getContentPane().setBackground(UIManager.getColor("CalloutWindow.background"));
    }

    private void init(int sideOfWindow, Rectangle pointAtRect) {
        closePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        PhonUIAction<Void> closeAct = PhonUIAction.eventConsumer((e) -> dispose(), null);
        closeAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Close");
        closeAct.putValue(FlatButton.ICON_NAME_PROP, "close");
        closeAct.putValue(FlatButton.ICON_FONT_NAME_PROP, IconManager.GoogleMaterialDesignIconsFontName);
        closeAct.putValue(FlatButton.ICON_SIZE_PROP, IconSize.SMALL);
        FlatButton closeButton = new FlatButton(closeAct);
        closeButton.setBackground(UIManager.getColor("CalloutWindow.background"));
        closeButton.setBgColor(UIManager.getColor("CalloutWindow.background"));
        closeButton.setPadding(0);
        closePanel.add(closeButton);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        contentPanel = new JPanel(new BorderLayout());
//        contentPanel.setBorder(new EmptyBorder(
//                topOfRect,
//                leftOfRect,
//                bottomOfRect,
//                rightOfRect
//        ));
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

        setUndecorated(true);
        setResizable(false);
        setLocationRelativeTo(null);

        pointAtRect(sideOfWindow, pointAtRect);
    }

    /**
     * Sets shape and position of the window on screen.  The window will be positioned so that the arrow points at the
     * given rectangle on the specified side of the window.  If the window would be off screen, the side of the window
     * will be flipped.
     *
     * @param sideOfWindow
     * @param pointAtRect
     */
    public void pointAtRect(int sideOfWindow, Rectangle pointAtRect) {
        // get screen dimensions
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension d = content.getPreferredSize();

        // region Close button and panel
        final int prefWidth = (int) (d.getWidth() + (sideOfWindow == SwingConstants.WEST || sideOfWindow == SwingConstants.EAST ? TRIANGLE_HEIGHT : 0) + getInsets().left + getInsets().right);
        final int prefHeight = (int) (d.getHeight() + (sideOfWindow == SwingConstants.NORTH || sideOfWindow == SwingConstants.SOUTH ? TRIANGLE_HEIGHT : 0) + IconSize.SMALL.getHeight() + getInsets().top + getInsets().bottom);

        // flip side of window if needed
        if(sideOfWindow == SwingConstants.NORTH) {
            if(pointAtRect.getMaxY() + prefHeight > screenSize.getHeight()) {
                sideOfWindow = SwingConstants.SOUTH;
            }
        } else if(sideOfWindow == SwingConstants.SOUTH) {
            if(pointAtRect.getMinY() - prefHeight < 0) {
                sideOfWindow = SwingConstants.NORTH;
            }
        } else if(sideOfWindow == SwingConstants.WEST) {
            if(pointAtRect.getMaxX() + prefWidth > screenSize.getWidth()) {
                sideOfWindow = SwingConstants.EAST;
            }
        } else if(sideOfWindow == SwingConstants.EAST) {
            if(pointAtRect.getMinX() - prefWidth < 0) {
                sideOfWindow = SwingConstants.WEST;
            }
        }

        int triangleOffset = 0;
        // calculate window bounds including triangle
        Rectangle windowBounds = new Rectangle();
        if(sideOfWindow == SwingConstants.NORTH) {
            windowBounds = new Rectangle(
                    (int)(pointAtRect.getCenterX() - (prefWidth / 2)),
                    (int)(pointAtRect.getMaxY()),
                    prefWidth, prefHeight);
            // check horizontal bounds and correct
            if(windowBounds.getMaxX() > screenSize.getWidth()) {
                final int diff = (int)(windowBounds.getMaxX() - screenSize.getWidth());
                windowBounds.x -= diff;
                triangleOffset = diff;
            } else if(windowBounds.getMinX() < 0) {
                int diff = windowBounds.x;
                windowBounds.x = 0;
                triangleOffset = diff;
            }
        } else if(sideOfWindow == SwingConstants.SOUTH) {
            windowBounds = new Rectangle(
                    (int)(pointAtRect.getCenterX() - (prefWidth / 2)),
                    (int)(pointAtRect.getMinY() - prefHeight),
                    prefWidth, prefHeight);
            // check horizontal bounds and correct
            if(windowBounds.getMaxX() > screenSize.getWidth()) {
                final int diff = (int)(windowBounds.getMaxX() - screenSize.getWidth());
                windowBounds.x -= diff;
                triangleOffset = diff;
            } else if(windowBounds.getMinX() < 0) {
                final int diff = windowBounds.x;
                windowBounds.x = 0;
                triangleOffset = diff;
            }
        } else if(sideOfWindow == SwingConstants.WEST) {
            windowBounds = new Rectangle(
                    (int)(pointAtRect.getMaxX()),
                    (int)(pointAtRect.getCenterY() - (prefHeight / 2)),
                    prefWidth, prefHeight);
            // check vertical bounds and correct
            if(windowBounds.getMaxY() < screenSize.getHeight()) {
                final int diff = (int)(windowBounds.getMaxY() - screenSize.getHeight());
                windowBounds.y -= diff;
                triangleOffset = diff;
            } else if(windowBounds.getMinY() < 0) {
                final int diff = windowBounds.y;
                windowBounds.y = 0;
                triangleOffset = diff;
            }
        } else if(sideOfWindow == SwingConstants.EAST) {
            windowBounds = new Rectangle(
                    (int)(pointAtRect.getMinX() - prefWidth),
                    (int)(pointAtRect.getCenterY() - (prefHeight / 2)),
                    prefWidth, prefHeight);
            // check vertical bounds and correct
            if(windowBounds.getMaxY() > screenSize.getHeight()) {
                final int diff = (int)(windowBounds.getMaxY() - screenSize.getHeight());
                windowBounds.y -= diff;
                triangleOffset = diff;
            } else if(windowBounds.getMinY() < 0) {
                final int diff = windowBounds.y;
                windowBounds.y = 0;
                triangleOffset = diff;
            }
        }

        int topOfRect = sideOfWindow == SwingConstants.NORTH ? TRIANGLE_HEIGHT : 0;
        int bottomOfRect = sideOfWindow == SwingConstants.SOUTH ? TRIANGLE_HEIGHT : 0;
        int leftOfRect = sideOfWindow == SwingConstants.WEST ? TRIANGLE_HEIGHT : 0;
        int rightOfRect = sideOfWindow == SwingConstants.EAST ? TRIANGLE_HEIGHT : 0;

        if(sideOfWindow == SwingConstants.NORTH) {
            closePanel.setBorder(new EmptyBorder(topOfRect, leftOfRect, 0, rightOfRect));
        } else if (sideOfWindow == SwingConstants.SOUTH) {
            contentPanel.setBorder(new EmptyBorder(0, leftOfRect, bottomOfRect, rightOfRect));
        } else {
            closePanel.setBorder(new EmptyBorder(topOfRect, leftOfRect, bottomOfRect, rightOfRect));
            contentPanel.setBorder(new EmptyBorder(topOfRect, leftOfRect, bottomOfRect, rightOfRect));
        }
        // endregion Close button and panel

        shape = createShape(
                prefWidth,
                prefHeight,
                TRIANGLE_BASE,
                TRIANGLE_HEIGHT,
                cornerRadius,
                sideOfWindow,
                triangleOffset
        );

        borderShape = createShape(
                prefWidth-1,
                prefHeight-1,
                TRIANGLE_BASE,
                TRIANGLE_HEIGHT,
                cornerRadius,
                sideOfWindow,
                triangleOffset
        );

//        setSize(prefWidth, prefHeight);
        setShape(shape);

        if (pointAtRect != null) {
            final Point pos = windowBounds.getLocation();
            setLocation(pos);

        }
        repaint();
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        // macos provides its own border
        if(!OSInfo.isMacOs() && borderShape != null) {
            final Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g2d.setColor(Color.black);
            g2d.setStroke(new BasicStroke(2));
            g2d.draw(borderShape);
        }
    }

    public static CalloutWindow showCallout(JFrame owner, boolean modal, JComponent content, int sideOfWindow, Point pointAtPos) {
        return showCallout(owner, modal, content, sideOfWindow, new Rectangle(pointAtPos));
    }

    public static CalloutWindow showCallout(JFrame owner, boolean modal, JComponent content, int sideOfWindow, Rectangle pointAtRect) {
        // Create a custom JDialog
        CalloutWindow dialog = new CalloutWindow(owner, content, sideOfWindow, pointAtRect);
        dialog.pack();
        dialog.setVisible(true);
        dialog.setModal(modal);

        return dialog;
    }

    public static CalloutWindow showNonFocusableCallout(JFrame owner, JComponent content, int sideOfWindow, Point pointAtPos) {
        return showNonFocusableCallout(owner, content, sideOfWindow, new Rectangle(pointAtPos));
    }

    public static CalloutWindow showNonFocusableCallout(JFrame owner, JComponent content, int sideOfWindow, Rectangle pointAtRect) {
        // Create a custom JDialog
        CalloutWindow dialog = new CalloutWindow(owner, content, sideOfWindow, pointAtRect);
        dialog.setFocusable(false);
        dialog.setFocusableWindowState(false);
        dialog.pack();
        dialog.setVisible(true);

        return dialog;
    }

    public Point getRelativeArrowPoint() {
        return relativeArrowPoint;
    }

    private Shape createShape(int width, int height, int triangleBase, int triangleHeight, int cornerRadius, int sideOfWindow, int triangleOffset) {
        // Create a Path2D object
        Path2D.Double shape = new Path2D.Double();

        int topOfRect = sideOfWindow == SwingConstants.NORTH ? triangleHeight : 0;
        int leftOfRect = sideOfWindow == SwingConstants.WEST ? triangleHeight : 0;
        int rightOfRect = sideOfWindow == SwingConstants.EAST ? triangleHeight : 0;
        int bottomOfRect = sideOfWindow == SwingConstants.SOUTH ? triangleHeight : 0;

//        int horiOffset = 0;
//        int vertOffset = 0;
//        if (topMiddleBottom == SwingConstants.LEADING) {
//            horiOffset = -(((width - triangleBase) / 2) - cornerRadius) + ARROW_EDGE_PADDING;
//            vertOffset = -(((height - triangleBase) / 2) - cornerRadius) + ARROW_EDGE_PADDING;
//        }
//        else if (topMiddleBottom == SwingConstants.TRAILING) {
//            horiOffset = (((width - triangleBase) / 2) - cornerRadius) - ARROW_EDGE_PADDING;
//            vertOffset = (((height - triangleBase) / 2) - cornerRadius) - ARROW_EDGE_PADDING;
//        }

        double angleToArrow = Math.atan(2 * TRIANGLE_HEIGHT / (double)TRIANGLE_BASE);
        double n = angleToArrow / (2 * Math.PI);
        double dist = 4 * Math.atan2((2 * n), Math.PI) / 3;

        // Start the path in the top-left corner of the rectangle, considering the corner radius
        shape.moveTo(leftOfRect + cornerRadius, topOfRect);

        // Top
        if (sideOfWindow == SwingConstants.NORTH) {
            // Prev corner to start of arrow curve
            Point startArrowCurveStart = new Point(triangleOffset + ((width - triangleBase) / 2), topOfRect);
            shape.lineTo(startArrowCurveStart.getX(), startArrowCurveStart.getY());

            // Start of arrow curve to point
            shape.lineTo(triangleOffset + (width / 2), 0);
            relativeArrowPoint = new Point(triangleOffset + (width / 2), 0);
            // Point to end of arrow curve
            shape.lineTo(triangleOffset + (width + triangleBase) / 2, topOfRect);
            // End of arrow curve
            //shape.quadTo();
        }
        shape.lineTo(leftOfRect + width - cornerRadius, topOfRect);

        // Top right corner
        shape.quadTo(leftOfRect + width - cornerRadius, topOfRect, width + leftOfRect, topOfRect + cornerRadius);

        // Right
        if (sideOfWindow == SwingConstants.EAST) {
            shape.lineTo(width, triangleOffset + (height - triangleBase) / 2);
            shape.lineTo(width + triangleHeight, triangleOffset + height / 2);
            relativeArrowPoint = new Point(width + triangleHeight, triangleOffset + height / 2);
            shape.lineTo(width, triangleOffset + (height + triangleBase) / 2);
        }
        shape.lineTo(leftOfRect + width, height - cornerRadius + topOfRect);

        // Bottom right corner
        shape.quadTo(leftOfRect + width, topOfRect + height - cornerRadius, width - cornerRadius + leftOfRect, height + topOfRect);

        // Bottom
        if (sideOfWindow == SwingConstants.SOUTH) {
            shape.lineTo(leftOfRect + triangleOffset + (width + triangleBase) / 2, height);
            shape.lineTo(leftOfRect + triangleOffset + (width / 2), height + triangleHeight);
            relativeArrowPoint = new Point(leftOfRect + triangleOffset + (width / 2), height + triangleHeight);
            shape.lineTo(leftOfRect + triangleOffset + (width - triangleBase) / 2, height);
        }
        shape.lineTo(leftOfRect + cornerRadius, topOfRect + height);

        // Bottom left corner
        shape.quadTo(leftOfRect, topOfRect + height, leftOfRect, topOfRect + height - cornerRadius);

        // Left
        if (sideOfWindow == SwingConstants.WEST) {
            shape.lineTo(triangleHeight, triangleOffset + (height + triangleBase) / 2);
            shape.lineTo(0, triangleOffset + height / 2);
            relativeArrowPoint = new Point(0, triangleOffset + height / 2);
            shape.lineTo(triangleHeight, triangleOffset + (height - triangleBase) / 2);
        }
        shape.lineTo(leftOfRect, topOfRect + cornerRadius);

        // Top left corner
        shape.quadTo(leftOfRect, topOfRect + cornerRadius, cornerRadius + leftOfRect, topOfRect);

        // Close path and return
        shape.closePath();
        return shape;
    }
}
