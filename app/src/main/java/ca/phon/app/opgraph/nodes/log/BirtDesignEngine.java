/*
 * Phon - An open source tool for research in phonology.
 * Copyright (C) 2005 - 2016, Gregory Hedlund <ghedlund@mun.ca> and Yvan Rose <yrose@mun.ca>
 * Dept of Linguistics, Memorial University <https://phon.ca>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ca.phon.app.opgraph.nodes.log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.core.framework.Platform;
import org.eclipse.birt.report.model.api.DesignConfig;
import org.eclipse.birt.report.model.api.DesignFileException;
import org.eclipse.birt.report.model.api.IDesignEngine;
import org.eclipse.birt.report.model.api.IDesignEngineFactory;
import org.eclipse.birt.report.model.api.ReportDesignHandle;
import org.eclipse.birt.report.model.api.SessionHandle;

import com.ibm.icu.util.ULocale;

public class BirtDesignEngine {
	
	private final static Logger LOGGER = Logger.getLogger(BirtDesignEngine.class.getName());
	
	private IDesignEngine designEngine;
	
	public BirtDesignEngine() {
		super();
		designEngine = startEngine();
	}
	
	public IDesignEngine startEngine() {
		DesignConfig designConfig = new DesignConfig();
		designConfig.setBIRTHome("birt/ReportEngine");
		IDesignEngine retVal = null;
		try {
			Platform.startup(designConfig);
			IDesignEngineFactory factory = (IDesignEngineFactory) Platform.createFactoryObject( IDesignEngineFactory.EXTENSION_DESIGN_ENGINE_FACTORY );
	        retVal = factory.createDesignEngine( designConfig );
		} catch (BirtException e) {
			LOGGER.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
		return retVal;
	}
	
	public IDesignEngine getDesignEngine() {
		return this.designEngine;
	}
	
	public ReportDesignHandle openReportDesign(String name, String file) throws DesignFileException, FileNotFoundException {
		return openReportDesign(name, new File(file));
	}
	
	public ReportDesignHandle openReportDesign(String name, File designFile) throws DesignFileException, FileNotFoundException {
		return openReportDesign(name, new FileInputStream(designFile));
	}
	
	public ReportDesignHandle openReportDesign(String name, InputStream is) throws DesignFileException {
		 SessionHandle session = getDesignEngine().newSessionHandle( ULocale.ENGLISH ) ;
	     return session.openDesign(name, is);
	}

	public void saveReportDesign(ReportDesignHandle reportDesign, String file) throws IOException {
		reportDesign.saveAs(file);
	}
	
}

