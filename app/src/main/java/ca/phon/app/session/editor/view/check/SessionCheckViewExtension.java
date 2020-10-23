package ca.phon.app.session.editor.view.check;

import ca.phon.app.session.editor.*;
import ca.phon.phonex.*;
import ca.phon.plugin.*;

@PhonexPlugin(name="Session Check")
@EditorViewInfo(category=EditorViewCategory.SESSION, icon=SessionCheckView.ICON_NAME, name=SessionCheckView.VIEW_NAME)
public class SessionCheckViewExtension implements IPluginExtensionPoint<EditorView> {

	@Override
	public Class<?> getExtensionType() {
		return EditorView.class;
	}

	@Override
	public IPluginExtensionFactory<EditorView> getFactory() {
		return factory;
	}
	
	private final IPluginExtensionFactory<EditorView> factory = new IPluginExtensionFactory<EditorView>() {
		
		@Override
		public EditorView createObject(Object... args) {
			if(args.length != 1 || !(args[0] instanceof SessionEditor)) {
				throw new IllegalArgumentException("Arguments must include SessionEditor reference.");
			}
			final SessionEditor editor = (SessionEditor)args[0];
			return new SessionCheckView(editor);
		}
		
	};

}
