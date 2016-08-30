package ca.phon.app.opgraph.wizard;

import java.util.Arrays;
import java.util.List;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.html.HtmlRenderer;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;

/**
 * Title, message, and message format.
 */
public class WizardInfo {
	
	private String title;
	
	private String message;
	
	private WizardInfoMessageFormat format = WizardInfoMessageFormat.HTML;
	
	public WizardInfo() {
		this("");
	}
	
	public WizardInfo(String title) {
		this(title, "");
	}
	
	public WizardInfo(String title, String message) {
		this(title, message, WizardInfoMessageFormat.HTML);
	}
	
	public WizardInfo(String title, String message, WizardInfoMessageFormat format) {
		super();
		this.title = title;
		this.message = message;
		this.format = format;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public WizardInfoMessageFormat getFormat() {
		return format;
	}

	public void setFormat(WizardInfoMessageFormat format) {
		this.format = format;
	}
	
	private String markdownToHTML(String md) {
		List<Extension> extensions = Arrays.asList(TablesExtension.create());

		final Parser parser = Parser.builder().extensions(extensions).build();
		final Node doc = parser.parse(md);
		final HtmlRenderer renderer = HtmlRenderer.builder().extensions(extensions).build();
		return renderer.render(doc);
	}
	
	/**
	 * Get message in HTML format
	 * @return message in HTML
	 */
	public String getMessageHTML() {
		final String message = getMessage();
		if(message == null) return "";
		
		switch(getFormat()) {
		case HTML:
			return message;
			
		case MARKDOWN:
			return markdownToHTML(message);
			
		default:
			return message;
		}
	}

}
