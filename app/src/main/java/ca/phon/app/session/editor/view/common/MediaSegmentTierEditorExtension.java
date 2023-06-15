/*
 * Copyright (C) 2005-2020 Gregory Hedlund & Yvan Rose
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 *    http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.phon.app.session.editor.view.common;

import ca.phon.app.session.editor.SessionEditor;
import ca.phon.plugin.*;
import ca.phon.session.*;
import ca.phon.session.Record;

@TierEditorInfo(type=MediaSegment.class)
public class MediaSegmentTierEditorExtension implements IPluginExtensionPoint<TierEditor<MediaSegment>> {

    @Override
    public Class<?> getExtensionType() {
        return TierEditor.class;
    }

    @Override
    public IPluginExtensionFactory<TierEditor<MediaSegment>> getFactory() {
        return factory;
    }

    private final IPluginExtensionFactory<TierEditor<MediaSegment>> factory = args -> {
        final SessionEditor editor = SessionEditor.class.cast(args[TierEditorFactory.EDITOR]);
        final Record record = Record.class.cast(args[TierEditorFactory.RECORD]);
        final Tier<?> tier = Tier.class.cast(args[TierEditorFactory.TIER]);

        if(tier.getDeclaredType() != MediaSegment.class) {
            throw new IllegalArgumentException("Tier type must be " + MediaSegment.class.getName());
        }

        return new MediaSegmentTierComponent(editor, record, (Tier<MediaSegment>)tier);
    };

}