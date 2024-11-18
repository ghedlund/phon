package ca.phon.ui;

import ca.phon.ui.fonts.FontPreferences;
import ca.phon.util.icons.IconManager;
import ca.phon.util.icons.IconSize;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import javax.swing.plaf.ButtonUI;
import java.awt.*;
import java.awt.event.MouseEvent;

public class FlatButtonUI extends ButtonUI {

    private FlatButton button;

    private JFrame popupFrame;

    private MouseHandler mouseHandler = new MouseHandler();

    @Override
    public void installUI(JComponent c) {
        if(!(c instanceof FlatButton))
            throw new IllegalArgumentException("Flat button UI can only be used with FlatButton");
        button = (FlatButton)c;
        button.addMouseListener(mouseHandler);
        button.addMouseMotionListener(mouseHandler);
        button.addPropertyChangeListener("iconName", e -> button.repaint());
        button.addPropertyChangeListener("iconFontName", e -> button.repaint());
        button.addPropertyChangeListener("iconSize", e -> button.repaint());
        button.addPropertyChangeListener("iconColor", e -> button.repaint());
        button.addPropertyChangeListener("iconHoverColor", e -> button.repaint());
        button.addPropertyChangeListener("iconPressedColor", e -> button.repaint());
        button.addPropertyChangeListener("iconDisabledColor", e -> button.repaint());
        button.addPropertyChangeListener("iconSelectedColor", e -> button.repaint());
        super.installUI(c);
    }

    @Override
    public void uninstallUI(JComponent c) {
        button.removeMouseListener(mouseHandler);
        button.removeMouseMotionListener(mouseHandler);
        button = null;
        super.uninstallUI(c);
    }

    private void showPopup() {
        if(popupFrame == null) {
            popupFrame = new JFrame();
            popupFrame.setUndecorated(true);
            popupFrame.setAlwaysOnTop(true);
            final JLabel popupLabel = new JLabel(button.getPopupText());
            popupLabel.setFont(FontPreferences.getTierFont().deriveFont(Font.BOLD));
            popupLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            popupFrame.setContentPane(popupLabel);
            popupFrame.setFocusable(false);
            popupFrame.setFocusableWindowState(false);
            popupFrame.pack();
            // calculate location based on button.popupPosition
            final Point location = button.getLocationOnScreen();
            switch(button.getPopupLocation()) {
                case SwingConstants.NORTH -> location.translate(0, -(int)popupFrame.getPreferredSize().getHeight());
                // center vertically
                case SwingConstants.EAST -> location.translate((int)button.getPreferredSize().getWidth(), (int)(button.getPreferredSize().getHeight() - popupFrame.getPreferredSize().getHeight()) / 2);
                case SwingConstants.WEST -> location.translate(-(int)popupFrame.getPreferredSize().getWidth(), (int)(button.getPreferredSize().getHeight() - popupFrame.getPreferredSize().getHeight()) / 2);
                default -> location.translate(0, button.getHeight());
            }

            popupFrame.setLocation(location);
            popupFrame.setVisible(true);
        }
    }

    private void hidePopup() {
        if(popupFrame != null) {
            popupFrame.setVisible(false);
            popupFrame.dispose();
            popupFrame = null;
        }
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        final Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        if(c.isOpaque()) {
            g2.setColor(c.getBackground());
            g2.fillRect(0, 0, c.getWidth(), c.getHeight());
        }

        final int width = c.getWidth();
        final int height = c.getHeight();

        final int arc = button.getBorderRadius();
        final int borderWidth = button.getBorderWidth();

        final Color bgColor = button.getBgColor();
        final Color borderColor = button.getBorderColor();

        final Color bgHoverColor = button.getBgHoverColor();
        final Color borderHoverColor = button.getBorderHoverColor();

        final Color bgPressedColor = button.getBgPressedColor();
        final Color borderPressedColor = button.getBorderPressedColor();

        final Color bgDisabledColor = button.getBgDisabledColor();
        final Color borderDisabledColor = button.getBorderDisabledColor();

        final Color bgSelectedColor = button.getBgSelectedColor();
        final Color borderSelectedColor = button.getBorderSelectedColor();

        final boolean isHover = button.getModel().isRollover();
        final boolean isPressed = button.getModel().isPressed();
        final boolean isSelected = button.isSelected();
        final boolean isDisabled = !button.isEnabled();

        final Color bg = (isDisabled ? bgDisabledColor : (isSelected ? bgSelectedColor : (isPressed ? bgPressedColor : (isHover ? bgHoverColor : bgColor))));
        final Color border = (isDisabled ? borderDisabledColor : (isSelected ? borderSelectedColor : (isPressed ? borderPressedColor : (isHover ? borderHoverColor : borderColor))));

        g2.setColor(bg);
        if(arc > 0) {
            g2.fillRoundRect(0, 0, width-1, height-1, arc, arc);
        } else {
            g2.fillRect(0, 0, width-1, height-1);
        }

        g2.setColor(border);
        g2.setStroke(new BasicStroke(borderWidth));
        g2.drawRoundRect(0, 0, width-1, height-1, arc, arc);

        final ImageIcon icon = buildIcon();
        // draw icon based on vertical and horizontal text position
        int iconX = 0;
        int iconY = 0;
        switch(button.getVerticalTextPosition()) {
            case SwingConstants.TOP -> iconY = button.getPadding() + button.getBorderWidth();
            case SwingConstants.BOTTOM -> iconY = height - button.getPadding() - button.getBorderWidth() - icon.getIconHeight();
            default -> iconY = (height - icon.getIconHeight()) / 2;
        }

        switch(button.getHorizontalTextPosition()) {
            case SwingConstants.RIGHT, SwingConstants.TRAILING -> iconX = button.getPadding() + button.getBorderWidth();
            case SwingConstants.LEFT, SwingConstants.LEADING -> iconX = width - button.getPadding() - button.getBorderWidth() - icon.getIconWidth();
            default -> iconX = (width - icon.getIconWidth()) / 2;
        }
        icon.paintIcon(button, g2, iconX, iconY);

        final String text = button.getText();
        if(text != null && !text.isEmpty()) {
            final FontMetrics fm = g2.getFontMetrics();
            final int textWidth = fm.stringWidth(text);
            final int textHeight = fm.getHeight();

            // draw text based on vertical and horizontal text position
            int textX = 0;
            int textY = 0;
            switch(button.getVerticalTextPosition()) {
                case SwingConstants.TOP -> textY = button.getPadding() + button.getBorderWidth();
                case SwingConstants.BOTTOM -> textY = height - button.getPadding() - button.getBorderWidth() - textHeight;
                default -> textY = (height - textHeight) / 2;
            }

            switch(button.getHorizontalTextPosition()) {
                case SwingConstants.LEFT -> textX = button.getPadding() + button.getBorderWidth();
                case SwingConstants.LEADING -> textX = iconX - button.getIconTextGap() - textWidth;
                case SwingConstants.RIGHT -> textX = width - button.getPadding() - button.getBorderWidth() - textWidth;
                case SwingConstants.TRAILING -> textX = iconX + icon.getIconWidth() + button.getIconTextGap();
                default -> textX = (width - textWidth) / 2;
            }

            g2.setColor(button.isRolloverEnabled() && button.getModel().isRollover() ? button.getTextHoverColor() : button.getForeground());
            if(isDisabled) {
                g2.setColor(UIManager.getColor("Button.disabledText"));
            }
            g2.drawString(text, textX, textY + fm.getAscent());
        }

        // draw underline if button is default
        if(button.isDefaultButton()) {
            g2.setColor(button.getForeground());
            g2.setStroke(new BasicStroke(2.0f));
            g2.drawLine(0, height-1, width-1, height-1);
        }
    }

    private ImageIcon buildIcon() {
        final String fontName = button.getIconFontName();
        final String iconName = button.getIconName();
        final IconSize iconSize = button.getIconSize();
        Color iconColor = button.getIconColor();
        if(button.isSelected()) {
            iconColor = button.getIconSelectedColor();
        }
        if(button.getModel().isRollover()) {
            iconColor = button.getIconHoverColor();
        }
        if(button.getModel().isPressed()) {
            iconColor = button.getIconPressedColor();
        }
        if(!button.isEnabled()) {
            iconColor = button.getIconDisabledColor();
        }
        return IconManager.getInstance().getFontIcon(fontName, iconName, iconSize, iconColor);
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        int prefWidth = button.getIconSize().getWidth() + button.getPadding() * 2 + button.getBorderWidth() * 2;
        int prefHeight = button.getIconSize().getHeight() + button.getPadding() * 2 + button.getBorderWidth() * 2;
        if(button.getText() != null) {
            final FontMetrics fm = button.getFontMetrics(button.getFont());
            if(button.getVerticalTextPosition() == SwingConstants.BOTTOM || button.getHorizontalTextPosition() == SwingConstants.TOP) {
                prefWidth = Math.max(prefWidth, fm.stringWidth(button.getText()));
                prefHeight += button.getIconTextGap() + fm.getHeight();
            } else {
                prefWidth += button.getIconTextGap() + fm.stringWidth(button.getText());
            }
        }
        return new Dimension(prefWidth, prefHeight);
    }

    private class MouseHandler extends MouseInputAdapter {

        @Override
        public void mouseEntered(MouseEvent e) {
            if(button.isRolloverEnabled()) {
                button.getModel().setRollover(true);
            }
            if(button.getPopupText() != null && !button.getPopupText().isEmpty())
                showPopup();
        }

        @Override
        public void mouseExited(MouseEvent e) {
            if(button.isRolloverEnabled())
                button.getModel().setRollover(false);
            hidePopup();
        }

        @Override
        public void mousePressed(MouseEvent e) {
            hidePopup();
            button.getModel().setPressed(true);
            button.repaint();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if(button.getModel().isPressed()) {
                button.doClick();
                button.getModel().setPressed(false);
            }
            button.repaint();
        }

    }

}
