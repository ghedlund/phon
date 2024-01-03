package ca.phon.ui;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;

/**
 * An icon strip for displaying FlatButton actions in a row/column.
 * Direction is determined by component orientation.
 * The icon strip will contain three sections:
 * - left/top
 * - center
 * - right/bottom
 *
 */
public class IconStrip extends JPanel {

    private static final long serialVersionUID = 1L;

    public enum IconStripPosition {
        LEFT,
        CENTER,
        RIGHT
    };

    public IconStrip() {
        this(SwingConstants.HORIZONTAL);
    }

    public IconStrip(int layout) {
        super(new LayoutManager(layout));
    }

    /**
     * Custom layout manager for icon strip
     */
    private static class LayoutManager implements LayoutManager2 {

        /**
         * Icon strip layout - horizontal or vertical
         */
        private int layout;

        private LinkedHashMap<Component, IconStripPosition> componentMap = new LinkedHashMap<>();

        public LayoutManager(int layout) {
            this.layout = layout;
        }

        public LayoutManager() {
            this(SwingConstants.HORIZONTAL);
        }

        @Override
        public void addLayoutComponent(Component comp, Object constraints) {
            if(constraints instanceof IconStripPosition) {
                componentMap.put(comp, (IconStripPosition)constraints);
            } else {
                throw new IllegalArgumentException("constraints must be one of " + IconStripPosition.values());
            }
        }

        @Override
        public Dimension maximumLayoutSize(Container target) {
            if(target instanceof IconStrip iconStrip) {
                final Insets insets = target.getInsets();
                final int insetWidth = insets.left + insets.right;
                final int insetHeight = insets.top + insets.bottom;
                int maxWidth = insetWidth;
                int maxHeight = insetHeight;
                for(Component comp:componentMap.keySet()) {
                    final Dimension prefSize = comp.getPreferredSize();
                    if(layout == SwingConstants.HORIZONTAL) {
                        maxWidth += prefSize.width;
                        maxHeight = Math.max(maxHeight, prefSize.height);
                    } else {
                        maxWidth = Math.max(maxWidth, prefSize.width);
                        maxHeight += prefSize.height;
                    }
                }
                if(layout == SwingConstants.HORIZONTAL) {
                    maxWidth = Integer.MAX_VALUE;
                } else {
                    maxHeight += Integer.MAX_VALUE;
                }
                return new Dimension(maxWidth, maxHeight);
            } else {
                throw new IllegalArgumentException("target must be an IconStrip");
            }
        }

        @Override
        public float getLayoutAlignmentX(Container target) {
            return 0;
        }

        @Override
        public float getLayoutAlignmentY(Container target) {
            return 0;
        }

        @Override
        public void invalidateLayout(Container target) {
            // ignore
        }

        @Override
        public void addLayoutComponent(String name, Component comp) {
            // ignore
        }

        @Override
        public void removeLayoutComponent(Component comp) {
            componentMap.remove(comp);
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            if(parent instanceof IconStrip iconStrip) {
                final Insets insets = parent.getInsets();
                final int insetWidth = insets.left + insets.right;
                final int insetHeight = insets.top + insets.bottom;
                int prefWidth = insetWidth;
                int prefHeight = insetHeight;
                for(Component comp:componentMap.keySet()) {
                    final Dimension prefSize = comp.getPreferredSize();
                    if(layout == SwingConstants.HORIZONTAL) {
                        prefWidth += prefSize.width;
                        prefHeight = Math.max(prefHeight, prefSize.height);
                    } else {
                        prefWidth = Math.max(prefWidth, prefSize.width);
                        prefHeight += prefSize.height;
                    }
                }
                return new Dimension(prefWidth, prefHeight);
            } else {
                throw new IllegalArgumentException("target must be an IconStrip");
            }
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            if(parent instanceof IconStrip iconStrip) {
                final Insets insets = parent.getInsets();
                final int insetWidth = insets.left + insets.right;
                final int insetHeight = insets.top + insets.bottom;
                int prefWidth = insetWidth;
                int prefHeight = insetHeight;
                for(Component comp:componentMap.keySet()) {
                    final Dimension prefSize = comp.getPreferredSize();
                    if(layout == SwingConstants.HORIZONTAL) {
                        prefWidth += prefSize.width;
                        prefHeight = Math.max(prefHeight, prefSize.height);
                    } else {
                        prefWidth = Math.max(prefWidth, prefSize.width);
                        prefHeight += prefSize.height;
                    }
                }
                return new Dimension(prefWidth, prefHeight);
            } else {
                throw new IllegalArgumentException("target must be an IconStrip");
            }
        }

        @Override
        public void layoutContainer(Container parent) {
            if(parent instanceof IconStrip iconStrip) {
                final Dimension size = parent.getSize();
                final Insets insets = parent.getInsets();

                int currentX = insets.left;
                int currentY = insets.top;
                // layout left/top components
                for(Component comp:componentMap.keySet()) {
                    final IconStripPosition position = componentMap.get(comp);
                    if(position == IconStripPosition.LEFT) {
                        final Dimension prefSize = comp.getPreferredSize();
                        // center component vertically/horizontally within left/top section
                        if(layout == SwingConstants.HORIZONTAL) {
                            currentY = insets.top + (size.height - insets.top - insets.bottom - prefSize.height) / 2;
                            comp.setBounds(currentX, currentY, prefSize.width, prefSize.height);
                            currentX += prefSize.width;
                        } else {
                            currentX = insets.left + (size.width - insets.left - insets.right - prefSize.width) / 2;
                            comp.setBounds(currentX, currentY, size.width - insets.left - insets.right, prefSize.height);
                            currentY += prefSize.height;
                        }
                    }
                }

                // layout center components
                // calculate center section width/height
                if(layout == SwingConstants.HORIZONTAL) {
                    currentX = size.width/2;
                } else {
                    currentY = size.height/2;
                }
                for(Component comp:componentMap.keySet()) {
                    final IconStripPosition position = componentMap.get(comp);
                    if(position == IconStripPosition.CENTER) {
                        final Dimension prefSize = comp.getPreferredSize();
                        if(layout == SwingConstants.HORIZONTAL) {
                            currentX -= prefSize.width/2;
                        } else {
                            currentY -= prefSize.height/2;
                        }
                    }
                }

                for(Component comp:componentMap.keySet()) {
                    final IconStripPosition position = componentMap.get(comp);
                    if(position == IconStripPosition.CENTER) {
                        final Dimension prefSize = comp.getPreferredSize();
                        if(layout == SwingConstants.HORIZONTAL) {
                            currentY = insets.top + (size.height - insets.top - insets.bottom - prefSize.height) / 2;
                            comp.setBounds(currentX, currentY, prefSize.width, prefSize.height);
                            currentX += prefSize.width;
                        } else {
                            currentX = insets.left + (size.width - insets.left - insets.right - prefSize.width) / 2;
                            comp.setBounds(currentX, currentY, prefSize.width, prefSize.height);
                            currentY += prefSize.height;
                        }
                    }
                }

                // layout right/bottom components
                // calculate right/bottom section width/height
                // layout right/bottom components
                if(layout == SwingConstants.HORIZONTAL) {
                    currentX = size.width - insets.right;
                } else {
                    currentY = size.height - insets.bottom;
                }
                for(Component comp:componentMap.keySet()) {
                    final IconStripPosition position = componentMap.get(comp);
                    if(position == IconStripPosition.RIGHT) {
                        final Dimension prefSize = comp.getPreferredSize();
                        if(layout == SwingConstants.HORIZONTAL) {
                            currentX -= prefSize.width;
                        } else {
                            currentY -= prefSize.height;
                        }
                    }
                }

                for(Component comp:componentMap.keySet()) {
                    final IconStripPosition position = componentMap.get(comp);
                    if(position == IconStripPosition.RIGHT) {
                        final Dimension prefSize = comp.getPreferredSize();
                        if(layout == SwingConstants.HORIZONTAL) {
                            currentY = insets.top + (size.height - insets.top - insets.bottom - prefSize.height) / 2;
                            comp.setBounds(currentX, currentY, prefSize.width, size.height - insets.top - insets.bottom);
                            currentX += prefSize.width;
                        } else {
                            currentX = insets.left + (size.width - insets.left - insets.right - prefSize.width) / 2;
                            comp.setBounds(currentX, currentY, size.width - insets.left - insets.right, prefSize.height);
                            currentY += prefSize.height;
                        }
                    }
                }
            } else {
                throw new IllegalArgumentException("target must be an IconStrip");
            }

        }

    }
}
