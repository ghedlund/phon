package ca.phon.ui.theme;

import ca.phon.plugin.IPluginExtensionFactory;
import ca.phon.plugin.IPluginExtensionPoint;
import ca.phon.plugin.Rank;
import ca.phon.ui.theme.UIDefaults;
import ca.phon.ui.theme.UIDefaultsHandler;

import javax.swing.*;
import java.awt.*;

/**
 * Install Phon UI defaults
 */
@Rank(0)
public final class PhonUIDefaults implements UIDefaultsHandler, IPluginExtensionPoint<UIDefaultsHandler> {

    public final static String LIGHT_BLUE = "Phon.lightBlue";
    public final static Color DEFAULT_LIGHT_BLUE = new Color(240, 248, 255);

    public final static String DARK_BLUE = "Phon.darkBlue";
    public final static Color DEFAULT_DARK_BLUE = new Color(17, 75, 122);

    public final static String SELECTION_BLUE = "Phon.selectionBlue";
    public final static Color DEFAULT_SELECTION_BLUE = Color.decode("#1E88E5");

    @Override
    public void setupDefaults(UIDefaults defaults) {
        defaults.put(LIGHT_BLUE, DEFAULT_LIGHT_BLUE);
        defaults.put(DARK_BLUE, DEFAULT_DARK_BLUE);
        defaults.put(SELECTION_BLUE, DEFAULT_SELECTION_BLUE);

        defaults.put("CalloutWindow.background", Color.WHITE);
    }

    @Override
    public Class<?> getExtensionType() {
        return UIDefaultsHandler.class;
    }

    @Override
    public IPluginExtensionFactory<UIDefaultsHandler> getFactory() {
        return args -> this;
    }

}
