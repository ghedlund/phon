package ca.phon.app.session.editor.view.timeline.actions;

import ca.phon.app.session.editor.SessionEditor;
import ca.phon.app.session.editor.undo.DeleteRecordEdit;
import ca.phon.app.session.editor.view.timeline.*;
import ca.phon.ui.CommonModuleFrame;
import ca.phon.ui.nativedialogs.*;
import ca.phon.util.icons.*;

import java.awt.event.ActionEvent;

public class DeleteRecordsAction extends TimelineAction  {

    private final static String CMD_NAME = "Delete record(s)";

    private final static String SHORT_DESC = "Delete currently selected records";

    private final static String ICON = "misc/record-delete";

    private boolean showConfirmation = true;

    public DeleteRecordsAction(TimelineView view) {
        this(view, true);
    }

    public DeleteRecordsAction(TimelineView view, boolean showConfirmation) {
        super(view);

        this.showConfirmation = showConfirmation;

        putValue(NAME, CMD_NAME);
        putValue(SHORT_DESCRIPTION, SHORT_DESC);
        putValue(SMALL_ICON, IconManager.getInstance().getIcon(ICON, IconSize.SMALL));
    }

    @Override
    public void hookableActionPerformed(ActionEvent ae) {
        TimelineRecordTier recordTier = getView().getRecordTier();
        SessionEditor editor = getView().getEditor();

        int[] recordsToDelete = recordTier.getSelectionModel().getSelectedIndices();

        if(showConfirmation) {
            final MessageDialogProperties props = new MessageDialogProperties();
            props.setRunAsync(false);
            props.setParentWindow(CommonModuleFrame.getCurrentFrame());
            props.setTitle("Delete record" + (recordsToDelete.length > 0 ? "s" : ""));
            props.setHeader("Confirm delete record" + (recordsToDelete.length > 0 ? "s" : ""));
            props.setMessage("Delete record " + String.format("%d", editor.getCurrentRecordIndex() + 1)
                    + (recordsToDelete.length > 1 ? String.format(" and %d others", recordsToDelete.length - 1) : "") + "?");
            props.setOptions(MessageDialogProperties.okCancelOptions);
            int retVal = NativeDialogs.showMessageDialog(props);
            if (retVal == 1) return;
        }

        editor.getUndoSupport().beginUpdate();
        for(int i = recordsToDelete.length-1; i >= 0; i--) {
            int recordIdx = recordsToDelete[i];
            final DeleteRecordEdit edit = new DeleteRecordEdit(editor, recordIdx);
            edit.setFireEvent(i == recordsToDelete.length-1);
            editor.getUndoSupport().postEdit(edit);
        }
        editor.getUndoSupport().endUpdate();
    }

}
