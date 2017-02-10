/*
 * Copyright 2017 Esri, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.esri.geoportal.harvester.migration;

import com.esri.geoportal.commons.constants.MimeType;
import static com.esri.geoportal.commons.utils.UriUtils.escapeUri;
import com.esri.geoportal.harvester.api.DataReference;
import com.esri.geoportal.harvester.api.base.SimpleDataReference;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Migration data builder.
 */
/*package*/ class MigrationDataBuilder {

  private final MigrationBrokerDefinitionAdaptor definition;
  private final URI brokerUri;
  private final Map<Integer, String> userMap;
  private final Map<String, MigrationHarvestSite> sites;

  /**
   * Creates instance of the builder.
   *
   * @param definition broker definition
   * @param brokerUri broker URI
   * @param userMap user map
   * @param sites sites map
   */
  public MigrationDataBuilder(MigrationBrokerDefinitionAdaptor definition, URI brokerUri, Map<Integer, String> userMap, Map<String, MigrationHarvestSite> sites) {
    this.definition = definition;
    this.brokerUri = brokerUri;
    this.userMap = userMap;
    this.sites = sites;
  }

  public DataReference buildReference(MigrationData data, String xml) throws URISyntaxException, UnsupportedEncodingException {
    SimpleDataReference ref = new SimpleDataReference(
            createBrokerUri(data),
            definition.getEntityDefinition().getLabel(),
            data.docuuid,
            data.updateDate,
            createSourceUri(data),
            xml.getBytes("UTF-8"),
            MimeType.APPLICATION_XML
    );
    String owner = userMap.get(data.owner);
    if (owner != null) {
      ref.getAttributesMap().put("owner", owner);
    }
    return ref;
  }

  private URI createBrokerUri(MigrationData data) throws URISyntaxException {
    MigrationHarvestSite site = data.siteuuid != null ? sites.get(data.siteuuid) : null;
    if (site != null) {
      String type = StringUtils.trimToEmpty(site.type).toUpperCase();
      switch (type) {
        case "WAF":
          return new URI("WAF", escapeUri(site.host), null);
        case "CKAN":
          return new URI("CKAN", escapeUri(site.host), null);
        case "CSW":
          try {
            Document doc = strToDom(site.protocol);
            XPath xPath = XPathFactory.newInstance().newXPath();
            String protocolId = (String)xPath.evaluate("/protocol[@type='CSW']/profile", doc, XPathConstants.STRING);
            URL host = new URL(site.host);
            host = new URL(host.getProtocol(), host.getHost(), host.getPort(), host.getPath());
            return new URI("CSW", host.toExternalForm(), protocolId);
          } catch (Exception ex) {}
      }
    }
    return brokerUri;
  }

  public URI createSourceUri(MigrationData data) throws URISyntaxException, UnsupportedEncodingException {
    MigrationHarvestSite site = data.siteuuid != null ? sites.get(data.siteuuid) : null;
    if (site != null) {
      String type = StringUtils.trimToEmpty(site.type).toUpperCase();
      switch (type) {
        case "CSW":
          return new URI("uuid", escapeUri(data.sourceuri), null);
      }
    }
    return URI.create(escapeUri(data.sourceuri));
  }

  private Document strToDom(String strXml) throws ParserConfigurationException, SAXException, IOException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    return builder.parse(new InputSource(new StringReader(strXml)));
  }
}
