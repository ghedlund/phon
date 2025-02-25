package ca.phon.app.session.editor.view.transcript;

import javax.swing.*;
import javax.swing.text.AttributeSet;

public interface ComponentFactory {

    /**
     * Create a component for the given attributes
     *
     * @param attrs
     * @return component
     */
    public JComponent createComponent(AttributeSet attrs);

    /**
     * Return the previously created component (if any)
     *
     * @return component
     */
    public JComponent getComponent();

    /**
     * Focus the 'beginning' of the component
     *
     */
    public void requestFocusStart();

    /**
     * Focus the 'end' of the component
     *
     */
    public void requestFocusEnd();

    /**
     * Request focus of specific offset, meaning of offset would be
     * determined by the component.  If not possible should focus beginning/end of
     * component as appropriate.
     *
     * @param offset
     */
    public void requestFocusAtOffset(int offset);
}
