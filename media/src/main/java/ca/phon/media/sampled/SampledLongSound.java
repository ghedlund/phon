package ca.phon.media.sampled;

import java.io.File;
import java.io.IOException;

import ca.phon.media.LongSound;
import ca.phon.media.Sound;
import ca.phon.plugin.IPluginExtensionFactory;
import ca.phon.plugin.IPluginExtensionPoint;

public class SampledLongSound extends LongSound implements IPluginExtensionPoint<LongSound> {
	
	private PCMSampled sampled;
	
	public SampledLongSound(File file) throws IOException {
		super(file);
		
		this.sampled = new PCMSampled(file);
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
	public Sound extractPart(float startTime, float endTime) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Class<?> getExtensionType() {
		return LongSound.class;
	}

	@Override
	public IPluginExtensionFactory<LongSound> getFactory() {
		return (args) -> {
			try {
				return new SampledLongSound((File)args[0]);
			} catch (IOException e) {
				throw new IllegalArgumentException(e);
			}
		};
	}

}
