package ca.phon.app.query.opgraph;

import java.io.IOException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ca.gedge.opgraph.OpGraph;
import ca.gedge.opgraph.extensions.Extendable;
import ca.gedge.opgraph.io.xml.XMLSerializer;
import ca.gedge.opgraph.io.xml.XMLSerializerFactory;
import ca.phon.query.report.io.ReportDesign;


public class ReportDesignNodeXMLSerializer implements XMLSerializer {
	
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
		
		ReportDesignNode retVal = new ReportDesignNode();
		
		NodeList children = elem.getElementsByTagNameNS(REPORT_NAMESPACE, "report-design");
		if(children.getLength() > 0) {
			Node reportDesignNode = children.item(0);
			
			if(reportDesignNode.getNodeName().equals(REPORT_PREFIX + ":report-design")
					&& reportDesignNode.getNamespaceURI().equals(REPORT_NAMESPACE)) {
				try {
					JAXBContext ctx = JAXBContext.newInstance("ca.phon.query.report.io");
					Unmarshaller unmarshaller = ctx.createUnmarshaller();
					JAXBElement<ReportDesign> reportDesignTypeEle = unmarshaller.unmarshal(reportDesignNode, ReportDesign.class);

					retVal = new ReportDesignNode(reportDesignTypeEle.getValue());
				} catch (JAXBException e) {
					throw new IOException(e);
				}
			}
		}
		
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
