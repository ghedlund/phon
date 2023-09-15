package ca.phon.orthography;

public enum OverlapType {
    @CHATReference("https://talkbank.org/manuals/CHAT.html#OverlapPrecedes_Scope")
    OVERLAP_PRECEDES("[<", "overlap precedes"),
    @CHATReference("https://talkbank.org/manuals/CHAT.html#OverlapFollows_Scope")
    OVERLAP_FOLLOWS("[>", "overlap follows");

    private String prefix;

    private String displayName;

    private OverlapType(String prefix, String displayName) {
        this.prefix = prefix;
        this.displayName = displayName;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static OverlapType fromString(String text) {
        for(OverlapType type:values()) {
            if(type.getPrefix().equals(text) || type.getDisplayName().equals(text)) {
                return type;
            }
        }
        return null;
    }

}
