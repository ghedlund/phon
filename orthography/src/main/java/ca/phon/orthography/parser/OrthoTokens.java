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
package ca.phon.orthography.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles conversion from dynamic Ortho token names
 * and their integer type.
 *
 */
public class OrthoTokens {
	
	private final static org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger(OrthoTokens.class.getName());
	
	private String tokenFile = "Orthography.tokens";
	
	/** Token map */
	private Map<String, Integer> tokenMap =
		new HashMap<String, Integer>();
	
	public OrthoTokens() {
		super();
		initTokenMap(getClass().getClassLoader().getResource(tokenFile));
	}
	
	private void initTokenMap(URL url) {
		try {
//			File tokenFile = new File(file);
			URLConnection uc = url.openConnection();
			
			BufferedReader in = new BufferedReader(
					new InputStreamReader(uc.getInputStream()));
			String line = null;
			while((line = in.readLine()) != null) {
				String[] vals = line.split("=");
				if(vals.length == 2) {
					String tokenName = vals[0];
					String tokenNum = vals[1];
					Integer iToken = Integer.parseInt(tokenNum);
					tokenMap.put(tokenName, iToken);
				}
			}
			in.close();
			
		} catch (IOException e) {
			LOGGER.warn(e.toString());
		}
	}
	
	/**
	 * Returns null if type is not found
	 * @param tokenName
	 * @return
	 */
	public Integer getTokenType(String tokenName) {
		Integer retVal = null;
		
		if(tokenMap.containsKey(tokenName))
			retVal = tokenMap.get(tokenName);
		
		return retVal;
	}

}
