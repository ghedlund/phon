/*
 * Phon - An open source tool for research in phonology.
 * Copyright (C) 2005 - 2017, Gregory Hedlund <ghedlund@mun.ca> and Yvan Rose <yrose@mun.ca>
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
package ca.phon.app.opgraph.nodes.query;

import java.io.*;
import java.util.logging.*;

import javax.xml.XMLConstants;
import javax.xml.bind.*;
import javax.xml.namespace.QName;
import javax.xml.stream.*;

import org.w3c.dom.*;
import org.w3c.dom.Element;

import ca.gedge.opgraph.OpGraph;
import ca.gedge.opgraph.extensions.Extendable;
import ca.gedge.opgraph.io.xml.*;
import ca.phon.query.report.io.ReportDesign;
import ca.phon.util.PrefHelper;


public class ReportDesignNodeXMLSerializer implements XMLSerializer {
	
	private final static Logger LOGGER = Logger.getLogger(ReportDesignNodeXMLSerializer.class.getName());
	
	static final String NAMESPACE = "https://phon.ca/ns/opgraph_query";
	static final String PREFIX = "opqry";
	
	static final String REPORT_NAMESPACE = "http://phon.ling.mun.ca/ns/report";
	static final String REPORT_PREFIX = "rpt";
	
	// qualified names
	static final QName REPORT_NODE_QNAME = new QName(NAMESPACE, "reportDesignNode", PREFIX);

	@Override
	public void write(XMLSerializerFactory serializerFactory, Document doc,
			Element parentElem, Object obj) throws IOException {
		if(obj == null)
			throw new IOException("Null object given to serializer");

		if(!(obj instanceof ReportDesignNode))
			throw new IOException(ReportDesignNodeXMLSerializer.class.getName() + " cannot write objects of type " + obj.getClass().getName());

		// setup namespace for document
		final Element rootEle = doc.getDocumentElement();
		rootEle.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
				XMLConstants.XMLNS_ATTRIBUTE + ":" + PREFIX, NAMESPACE);
		rootEle.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, XMLConstants.XMLNS_ATTRIBUTE + ":" + REPORT_PREFIX, REPORT_NAMESPACE);
		
		// Create node element
		final ReportDesignNode reportDesignNode = (ReportDesignNode)obj;
		final Element reportDesignNodeElem = doc.createElementNS(NAMESPACE, PREFIX + ":" + REPORT_NODE_QNAME.getLocalPart());
		
		reportDesignNodeElem.setAttribute("id", reportDesignNode.getId());
		reportDesignNodeElem.setAttribute("type", obj.getClass().getName());
		
		if(!reportDesignNode.getName().equals(reportDesignNode.getDefaultName()))
			reportDesignNodeElem.setAttribute("name", reportDesignNode.getName());
		
		final ReportDesign reportDesign = reportDesignNode.getReportDesign();
		
		if(reportDesignNode.isUseLastReport()) {
			reportDesignNodeElem.setAttribute("useLastReport", Boolean.toString(reportDesignNode.isUseLastReport()));
		} else {
			// use jaxb to save to element
			try {
				QName reportDesignQName = new QName(REPORT_NAMESPACE, "report-design", REPORT_PREFIX);
				JAXBContext ctx = JAXBContext.newInstance("ca.phon.query.report.io");
				Marshaller marshaller = ctx.createMarshaller();
				marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
				marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
				JAXBElement<ReportDesign> reportDesignEle = new JAXBElement<ReportDesign>(reportDesignQName, ReportDesign.class, reportDesign);
				marshaller.marshal(reportDesignEle, reportDesignNodeElem);
				reportDesignNodeElem.getChildNodes().item(0).setPrefix(REPORT_PREFIX);
			} catch (JAXBException e) {
				throw new IOException(e);
			}
		}
		
		// Extensions last
		if(reportDesignNode.getExtensionClasses().size() > 0) {
			final XMLSerializer serializer = serializerFactory.getHandler(Extendable.class);
			if(serializer == null)
				throw new IOException("No XML serializer for extensions");

			serializer.write(serializerFactory, doc, reportDesignNodeElem, reportDesignNode);
		}
		
		parentElem.appendChild(reportDesignNodeElem);
	}

	@Override
	public Object read(XMLSerializerFactory serializerFactory, OpGraph graph,
			Object parent, Document doc, Element elem) throws IOException {
		if(!REPORT_NODE_QNAME.equals(XMLSerializerFactory.getQName(elem)))
			throw new IOException("Incorrect element");
		
		
		boolean useLastReport = 
				(elem.hasAttribute("useLastReport") ? Boolean.parseBoolean(elem.getAttribute("useLastReport"))
						: false);
		ReportDesign reportDesign = null;
		
		if(useLastReport) {
			try {
				JAXBContext ctx = JAXBContext.newInstance("ca.phon.query.report.io");
				Unmarshaller unmarshaller = ctx.createUnmarshaller();
				XMLInputFactory factory = XMLInputFactory.newInstance();
				XMLEventReader reader = factory.createXMLEventReader(
						new FileInputStream(new File(PrefHelper.getUserDataFolder(), ReportDesignNode.AUTOSAVE_FILENAME)), "UTF-8");
				JAXBElement<ReportDesign> reportDesignTypeEle =
						unmarshaller.unmarshal(reader, ReportDesign.class);
				reportDesign = reportDesignTypeEle.getValue();
			} catch (JAXBException | XMLStreamException e) {
				LOGGER.log(Level.SEVERE, e.getLocalizedMessage(), e);
			}
		} else {
			NodeList children = elem.getElementsByTagNameNS(REPORT_NAMESPACE, "report-design");
			if(children.getLength() > 0) {
				Node reportDesignNode = children.item(0);
				
				if(reportDesignNode.getNodeName().equals(REPORT_PREFIX + ":report-design")
						&& reportDesignNode.getNamespaceURI().equals(REPORT_NAMESPACE)) {
					try {
						JAXBContext ctx = JAXBContext.newInstance("ca.phon.query.report.io");
						Unmarshaller unmarshaller = ctx.createUnmarshaller();
						JAXBElement<ReportDesign> reportDesignTypeEle = unmarshaller.unmarshal(reportDesignNode, ReportDesign.class);
						reportDesign = reportDesignTypeEle.getValue();
					} catch (JAXBException e) {
						throw new IOException(e);
					}
				}
			}
		}
		
		reportDesign.setName("Report");
		
		ReportDesignNode retVal = 
				(reportDesign != null ? new ReportDesignNode(reportDesign) : new ReportDesignNode());
		retVal.setUseLastReport(useLastReport);
		
		// setup id and other attributes
		if(elem.hasAttribute("id"))
			retVal.setId(elem.getAttribute("id"));

		if(elem.hasAttribute("name"))
			retVal.setName(elem.getAttribute("name"));
		
		// read extensions
		NodeList extensionNodes = elem.getElementsByTagName("extensions");
		if(extensionNodes.getLength() > 0) {
			Element extensionNode = (Element)extensionNodes.item(0);
			QName name = XMLSerializerFactory.getQName(extensionNode);
			final XMLSerializer serializer = serializerFactory.getHandler(name);
			if(serializer == null)
				throw new IOException("Could not get handler for element: " + name);

			// Published fields and extensions all take care of adding
			// themselves to the passed in object
			//
			serializer.read(serializerFactory, graph, retVal, doc, extensionNode);
		}
		
		return retVal;
	}

	@Override
	public boolean handles(Class<?> cls) {
		return (cls == ReportDesignNode.class);
	}

	@Override
	public boolean handles(QName name) {
		return REPORT_NODE_QNAME.equals(name);
	}

}
