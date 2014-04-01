package ca.phon.app.session;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import au.com.bytecode.opencsv.CSVWriter;
import ca.phon.ipa.IPATranscript;
import ca.phon.orthography.Orthography;
import ca.phon.session.MediaSegment;
import ca.phon.session.Record;
import ca.phon.session.Tier;

/**
 * Clipboard transferable implementation for {@link Record}s.
 */
public class RecordTransferable implements Transferable {
	
	private final static Logger LOGGER = Logger
			.getLogger(RecordTransferable.class.getName());
	
	public final static DataFlavor FLAVOR = new DataFlavor(RecordTransferable.class, "RecordTransferable");
	
	private final Record record;
	
	public RecordTransferable(Record r) {
		super();
		this.record = r;
	}

	public Record getRecord() {
		return this.record;
	}
	
	@Override
	public DataFlavor[] getTransferDataFlavors() {
		return new DataFlavor[] { FLAVOR, DataFlavor.stringFlavor };
	}

	@Override
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return (flavor == FLAVOR || flavor == DataFlavor.stringFlavor);
	}

	@Override
	public Object getTransferData(DataFlavor flavor)
			throws UnsupportedFlavorException, IOException {
		if(flavor == FLAVOR) {
			return this;
		} else if (flavor == DataFlavor.stringFlavor) {
			// record to CSV
			return recordToCSV();
		} else {
			throw new UnsupportedFlavorException(flavor);
		}
	}
	
	private String recordToCSV() {
		final ByteArrayOutputStream bout = new ByteArrayOutputStream();
		try {
			final CSVWriter writer = new CSVWriter(new OutputStreamWriter(bout, "UTF-8"), ',', '\"');
			
			final String[] uuidLine = {"UUID", record.getUuid().toString()};
			writer.writeNext(uuidLine);
			
			final String[] speakerLine = {"Speaker", record.getSpeaker().getName()};
			writer.writeNext(speakerLine);
			
			final List<String> row = new ArrayList<String>();
			final Tier<Orthography> orthography = record.getOrthography();
			row.add(orthography.getName());
			for(Orthography ortho:orthography) row.add(ortho.toString());
			writer.writeNext(row.toArray(new String[0]));
			
			row.clear();
			final Tier<IPATranscript> ipaTarget = record.getIPATarget();
			row.add(ipaTarget.getName());
			for(IPATranscript t:ipaTarget) row.add(t.toString());
			writer.writeNext(row.toArray(new String[0]));
			
			row.clear();
			final Tier<IPATranscript> ipaActual = record.getIPAActual();
			row.add(ipaActual.getName());
			for(IPATranscript t:ipaActual) row.add(t.toString());
			writer.writeNext(row.toArray(new String[0]));
			
			row.clear();
			final Tier<String> notes = record.getNotes();
			row.add(notes.getName());
			row.add(notes.getGroup(0));
			writer.writeNext(row.toArray(new String[0]));
			
			row.clear();
			final Tier<MediaSegment> segment = record.getSegment();
			row.add(segment.getName());
			row.add(segment.getGroup(0).toString());
			writer.writeNext(row.toArray(new String[0]));
			
			for(String tierName:record.getExtraTierNames()) {
				row.clear();
				final Tier<String> tier = record.getTier(tierName, String.class);
				row.add(tier.getName());
				for(String v:tier) row.add(v);
				writer.writeNext(row.toArray(new String[0]));
			}
			
			writer.flush();
			writer.close();
		} catch (UnsupportedEncodingException e) {
			LOGGER.log(Level.SEVERE, e.getLocalizedMessage(), e);
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
		return new String(bout.toByteArray(), Charset.forName("UTF-8"));
	}

}
