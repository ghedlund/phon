package ca.phon.orthography;

public enum TagMarkerType {
    COMMA(',', "comma"),
    VOCATIVE('‡', "vocative"),
    TAG('„', "tag");

    private char ch;

    private String displayName;

    private TagMarkerType(char ch, String displayName) {
        this.ch = ch;
        this.displayName = displayName;
    }

    public char getChar() {
        return ch;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static TagMarkerType fromString(String text) {
        TagMarkerType retVal = null;

        for(TagMarkerType v:values()) {
            if((v.getChar() + "").equals(text) || v.getDisplayName().equals(text)) {
                retVal = v;
                break;
            }
        }

        return retVal;
    }

    @Override
    public String toString() {
        return getChar() + "";
    }

}
