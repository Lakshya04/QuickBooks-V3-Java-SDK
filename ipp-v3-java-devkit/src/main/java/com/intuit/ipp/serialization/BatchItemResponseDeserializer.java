package com.intuit.ipp.serialization;

import java.io.IOException;
import java.util.Iterator;

import javax.xml.bind.JAXBElement;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.introspect.JacksonAnnotationIntrospector;
import org.codehaus.jackson.map.module.SimpleModule;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;

import com.intuit.ipp.data.BatchItemResponse;
import com.intuit.ipp.data.CDCResponse;
import com.intuit.ipp.data.CustomFieldDefinition;
import com.intuit.ipp.data.Fault;
import com.intuit.ipp.data.IntuitEntity;
import com.intuit.ipp.data.ObjectFactory;
import com.intuit.ipp.data.QueryResponse;
import com.intuit.ipp.data.Report;
import com.intuit.ipp.util.Logger;

/**
 * Custom deserializer class to handle BatchItemResponse while unmarshall
 *
 */
public class BatchItemResponseDeserializer extends JsonDeserializer<BatchItemResponse> {
    /**
     * IntuitResponseDeserializeHelper instance
     */
    private IntuitResponseDeserializerHelper intuitResponseDeserializerHelper = new IntuitResponseDeserializerHelper();

    /**
	 * logger instance
	 */
	private static final org.slf4j.Logger LOG = Logger.getLogger();
	
	/**
	 * variable FAULT
	 */
	private static final String FAULT = "Fault";
	
	/**
	 * variable REPORT
	 */
	private static final String REPORT = "Report";
	
	/**
	 * variable BID
	 */
	private static final String BID = "bId";
	
	/**
	 * variable QUERYRESPONSE
	 */
	private static final String QUERYRESPONSE = "QueryResponse";
	
	/**
	 * variable CDC_QUERY_RESPONSE
	 */
	private static final String CDC_QUERY_RESPONSE = "CDCResponse";
	
	/**
	 * variable objFactory
	 */
	private ObjectFactory objFactory = new ObjectFactory();

	/**
	 * {@inheritDoc}}
	 */
	@SuppressWarnings("deprecation")
	@Override
	public BatchItemResponse deserialize(JsonParser jp, DeserializationContext desContext) throws IOException {
		ObjectMapper mapper = new ObjectMapper();

		//Make the mapper JAXB annotations aware
		AnnotationIntrospector primary = new JaxbAnnotationIntrospector();
		AnnotationIntrospector secondary = new JacksonAnnotationIntrospector();
		AnnotationIntrospector pair = new AnnotationIntrospector.Pair(primary, secondary);
		mapper.getDeserializationConfig().setAnnotationIntrospector(pair);
		mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		//Read the QueryResponse as a tree
		JsonNode jn = jp.readValueAsTree();

		//Create the QueryResponse to be returned
		BatchItemResponse qr = new BatchItemResponse();

		//Iterate over the field names
		Iterator<String> ite = jn.getFieldNames();

		while (ite.hasNext()) {
			String key = ite.next();

			//Attributes
			if (key.equalsIgnoreCase(FAULT)) {
				qr.setFault(mapper.readValue(jn.get(FAULT), Fault.class));
				continue;
			} else if (key.equalsIgnoreCase(REPORT)) {
				qr.setReport(mapper.readValue(jn.get(REPORT), Report.class));
			} else if (key.equalsIgnoreCase(BID)) {
				qr.setBId(jn.get(BID).getTextValue());
			} else if (key.equals(QUERYRESPONSE)) {
				qr.setQueryResponse(getQueryResponse(jn.get(key)));
			} else if (key.equals(CDC_QUERY_RESPONSE)) {
				qr.setCDCResponse(getCDCQueryResponse(jn.get(key)));
			} else {
                // It has to be an IntuitEntity
				String entity = key;
				LOG.debug("entity key : " + key);
				if (JsonResourceTypeLocator.lookupType(entity) != null) {
					// set the CustomFieldDefinition deserializer
					registerModulesForCustomFieldDef(mapper);
					Object intuitType = mapper.readValue(jn.get(key), JsonResourceTypeLocator.lookupType(entity));
					if (intuitType instanceof IntuitEntity) {
                        intuitResponseDeserializerHelper.updateBigDecimalScale((IntuitEntity) intuitType);
						JAXBElement<? extends IntuitEntity> intuitObject = objFactory
								.createIntuitObject((IntuitEntity) intuitType);
						qr.setIntuitObject(intuitObject);
					}
				}
			}
		}
		return qr;
	}

	/**
	 * Method to deserialize the QueryResponse object
	 * 
	 * @param jsonNode
	 * @return QueryResponse
	 */
	private QueryResponse getQueryResponse(JsonNode jsonNode) throws IOException {
		ObjectMapper mapper = new ObjectMapper();

		SimpleModule simpleModule = new SimpleModule("QueryResponseDeserializer", new Version(1, 0, 0, null));
		simpleModule.addDeserializer(QueryResponse.class, new QueryResponseDeserializer());

		mapper.registerModule(simpleModule);

		return mapper.readValue(jsonNode, QueryResponse.class);
	}
	
	/**
	 * Method to deserialize the CDCQueryResponse object
	 * 
	 * @param jsonNode
	 * @return CDCResponse
	 */
	private CDCResponse getCDCQueryResponse(JsonNode jsonNode) throws IOException {
		ObjectMapper mapper = new ObjectMapper();

		SimpleModule simpleModule = new SimpleModule("CDCQueryResponseDeserializer", new Version(1, 0, 0, null));
		simpleModule.addDeserializer(CDCResponse.class, new CDCQueryResponseDeserializer());

		mapper.registerModule(simpleModule);

		return mapper.readValue(jsonNode, CDCResponse.class);
	}
	
	/**
	 * Method to add custom deserializer for CustomFieldDefinition
	 * 
	 * @param objectMapper the Jackson object mapper
	 */
	private void registerModulesForCustomFieldDef(ObjectMapper objectMapper) {
		SimpleModule simpleModule = new SimpleModule("CustomFieldDefinition", new Version(1, 0, 0, null));
		simpleModule.addDeserializer(CustomFieldDefinition.class, new CustomFieldDefinitionDeserializer());
		objectMapper.registerModule(simpleModule);
	}
}