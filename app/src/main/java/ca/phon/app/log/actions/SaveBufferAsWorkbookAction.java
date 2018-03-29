package ca.phon.app.log.actions;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.io.*;

import javax.swing.ImageIcon;

import ca.phon.app.hooks.HookableAction;
import ca.phon.app.log.*;
import ca.phon.ui.CommonModuleFrame;
import ca.phon.ui.nativedialogs.*;
import ca.phon.ui.nativedialogs.FileFilter;
import ca.phon.util.OpenFileLauncher;
import ca.phon.util.icons.*;
import ca.phon.worker.PhonWorker;
import jxl.Workbook;
import jxl.write.*;

public class SaveBufferAsWorkbookAction extends HookableAction {

	private static final long serialVersionUID = -2827879669257916438L;

	private final static String CMD_NAME = "Export to Excel\u2122...";

	private final static String SHORT_DESC = "Save as Excel\u2122 workbook";

	private final MultiBufferPanel container;

	private final String bufferName;

	public SaveBufferAsWorkbookAction(MultiBufferPanel bufferPanel) {
		this(bufferPanel, null);
	}

	public SaveBufferAsWorkbookAction(MultiBufferPanel bufferPanel, String bufferName) {
		putValue(NAME, CMD_NAME);
		putValue(SHORT_DESCRIPTION, SHORT_DESC);

		ImageIcon excelIcn = IconManager.getInstance().getSystemIconForFileType("xlsx", IconSize.SMALL);
		putValue(SMALL_ICON, excelIcn);

		this.bufferName = bufferName;
		this.container = bufferPanel;
	}

	@Override
	public void hookableActionPerformed(ActionEvent ae) {
		final BufferPanel panel =
				(this.bufferName == null ? this.container.getCurrentBuffer()
						: this.container.getBuffer(bufferName));
		if(panel == null) {
			Toolkit.getDefaultToolkit().beep();
			return;
		}

		final SaveDialogProperties props = new SaveDialogProperties();
		props.setParentWindow(CommonModuleFrame.getCurrentFrame());
		props.setCanCreateDirectories(true);
		props.setFileFilter(FileFilter.excelFilter);
		props.setInitialFile(panel.getBufferName() + "." + FileFilter.excelFilter.getDefaultExtension());
		props.setRunAsync(true);
		props.setListener( (e) -> {
			if(e.getDialogResult() == NativeDialogEvent.OK_OPTION && e.getDialogData() != null) {
				final String saveAs = e.getDialogData().toString();
				PhonWorker.getInstance().invokeLater( () -> saveWorkbook(saveAs) );
			}
		});

		NativeDialogs.showSaveDialog(props);
	}
	
	private void saveWorkbook(String saveAs) {
		final BufferPanel panel =
				(this.bufferName == null ? this.container.getCurrentBuffer()
						: this.container.getBuffer(bufferName));
		if(panel == null) {
			Toolkit.getDefaultToolkit().beep();
			return;
		}
		
		try {
			final WritableWorkbook workbook = Workbook.createWorkbook(new File(saveAs));
			if(panel.isShowingTable()) {
				panel.createSheetInExcelWorkbook(workbook);
			} else if(panel.isShowingHtml()) {
				String html = panel.getLogBuffer().getText();
				final HTMLToWorkbookWriter writer = new HTMLToWorkbookWriter((MultiBufferPanel)this.container);
				writer.writeToWorkbook(workbook, html);
			}
			workbook.write();
			workbook.close();

			if(this.container.isOpenAfterSaving()) {
				OpenFileLauncher.openURL((new File(saveAs)).toURI().toURL());
			}
		} catch (IOException | WriteException ex) {
			Toolkit.getDefaultToolkit().beep();
			LogUtil.severe(ex);
		}
	}
	
}
