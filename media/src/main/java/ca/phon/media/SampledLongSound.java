package ca.phon.media;

import java.io.*;

import ca.phon.audio.*;

public class SampledLongSound extends LongSound {
	
	private Sampled sampled;
	
	private File file;
	
	public SampledLongSound(File file) throws IOException {
		super(file);
		this.file = file;
		
		AudioFile audioFile;
		try {
			audioFile = AudioIO.openAudioFile(file);
			this.sampled = new AudioFileSampled(audioFile);
		} catch (UnsupportedFormatException | InvalidHeaderException e) {
			throw new IOException(e);
		}
		
		putExtension(PlaySegment.class, new SampledPlaySegment(sampled));
		putExtension(ExportSegment.class, new SampledExportSegment(sampled, audioFile.getAudioFileType(), audioFile.getAudioFileEncoding()));
	}
	
	public Sampled getSampled() {
		return this.sampled;
	}
	
	@Override
	public int numberOfChannels() {
		return sampled.getNumberOfChannels();
	}

	@Override
	public float length() {
		return sampled.getLength();
	}

	@Override
	public synchronized Sound extractPart(float startTime, float endTime) {
		return new SampledSound(startTime, endTime);
	}

	private class SampledSound implements Sound {
		
		private float startTime;
		
		private float endTime;
		
		public SampledSound(float startTime, float endTime) {
			this.startTime = startTime;
			this.endTime = endTime;
		}

		@Override
		public int numberOfChannels() {
			return sampled.getNumberOfChannels();
		}

		@Override
		public float startTime() {
			return this.startTime;
		}

		@Override
		public float endTime() {
			return this.endTime;
		}

		@Override
		public float length() {
			return endTime - startTime;
		}

		@Override
		public double[][] getWindowExtrema(float startTime, float endTime) {
			return sampled.getWindowExtrema(startTime, endTime);
		}
		
	}
	
}
