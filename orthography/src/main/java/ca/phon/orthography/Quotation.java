package ca.phon.orthography;

import ca.phon.util.Documentation;

/**
 * Begin or end quoted material; &#x201C; and &#x201D;
 */
@Documentation("https://talkbank.org/manuals/CHAT.html#Quotation")
public final class Quotation extends AbstractOrthographyElement {

    public final static String QUOTATION_BEGIN = "\u201c";

    public final static String QUOTATION_END = "\u201d";

    private final BeginEnd beginEnd;

    public Quotation(BeginEnd beginEnd) {
        super();
        this.beginEnd = beginEnd;
    }

    public BeginEnd getBeginEnd() {
        return beginEnd;
    }

    @Override
    public String text() {
        return getBeginEnd() == BeginEnd.BEGIN ? QUOTATION_BEGIN : QUOTATION_END;
    }

}
