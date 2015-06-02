package org.openforis.collect.earth.core.handlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.openforis.collect.earth.core.model.PlacemarkCodedItem;
import org.openforis.collect.earth.core.model.PlacemarkInputFieldInfo;
import org.openforis.collect.model.CollectRecord;
import org.openforis.collect.model.NodeChangeMap;
import org.openforis.collect.model.NodeChangeSet;
import org.openforis.collect.model.RecordValidationReportGenerator;
import org.openforis.collect.model.RecordValidationReportItem;
import org.openforis.idm.metamodel.AttributeDefinition;
import org.openforis.idm.metamodel.CodeAttributeDefinition;
import org.openforis.idm.metamodel.CodeList;
import org.openforis.idm.metamodel.CodeListItem;
import org.openforis.idm.metamodel.CodeListService;
import org.openforis.idm.metamodel.EntityDefinition;
import org.openforis.idm.metamodel.NodeDefinition;
import org.openforis.idm.metamodel.NodeDefinitionVisitor;
import org.openforis.idm.model.Attribute;
import org.openforis.idm.model.CodeAttribute;
import org.openforis.idm.model.Entity;
import org.openforis.idm.model.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 
 * @author Alfonso Sanchez-Paus Diaz
 *
 */
@Component
public class BalloonInputFieldsUtils {

	private static final String NOT_APPLICABLE_ITEM_CODE = "-1";
	private static final String NOT_APPLICABLE_ITEM_LABEL = "N/A";

	public static final String PARAMETER_SEPARATOR = "===";
	public static final String MULTIPLE_VALUES_SEPARATOR = ";";

	private static final String COLLECT_PREFIX = "collect_";
	private final Logger logger = LoggerFactory.getLogger(BalloonInputFieldsUtils.class);

	private final List<AbstractAttributeHandler<?>> handlers = Arrays.<AbstractAttributeHandler<?>>asList(
			new BooleanAttributeHandler(),
			new CodeAttributeHandler(),
			new CoordinateAttributeHandler(),
			new DateAttributeHandler(),
			new EntityHandler(this),
			new IntegerAttributeHandler(),
			new RangeAttributeHandler(),
			new RealAttributeHandler(),
			new TextAttributeHandler(),
			new TimeAttributeHandler()
		);
	
	public Map<String, PlacemarkInputFieldInfo> extractFieldInfoByParameterName(CollectRecord record, String language) {
		Map<String, String> htmlParameterNameByNodePath = getHtmlParameterNameByNodePath(record);
		Set<String> parameterNames = new HashSet<String>(htmlParameterNameByNodePath.values());
		Map<String, String> validationMessageByPath = generateValidationMessages(record);
		Entity rootEntity = record.getRootEntity();

		Map<String, PlacemarkInputFieldInfo> result = new HashMap<String, PlacemarkInputFieldInfo>(parameterNames.size());
		for (String parameterName : parameterNames) {
			String cleanName = cleanUpParameterName(parameterName);
			AbstractAttributeHandler<?> handler = findHandler(cleanName);
			if (handler == null) {
				logger.warn("Cannot find handler for parameter: " + parameterName);
			} else if (handler instanceof EntityHandler) {
				Entity currentEntity = ((EntityHandler) handler).getChildEntity(cleanName, rootEntity);
				String childAttributeParameterName = ((EntityHandler) handler).extractNestedAttributeParameterName(parameterName);
				AbstractAttributeHandler<?> childHandler = findHandler(childAttributeParameterName);
				PlacemarkInputFieldInfo info = generateAttributeFieldInfo(
						record, validationMessageByPath, currentEntity, childAttributeParameterName,
						childHandler, language);
				result.put(parameterName, info);
			} else {
				PlacemarkInputFieldInfo info = generateAttributeFieldInfo(
						record, validationMessageByPath, rootEntity, cleanName,
						handler, language);
				result.put(parameterName, info);
			}
		}
		return result;
	}

	private PlacemarkInputFieldInfo generateAttributeFieldInfo(
			CollectRecord record, Map<String, String> validationMessageByPath,
			Entity rootEntity, String cleanName,
			AbstractAttributeHandler<?> handler, String language) {
		PlacemarkInputFieldInfo info = new PlacemarkInputFieldInfo();

		List<Attribute<?, ?>> attributes = handler.getAttributeNodesFromParameter(cleanName, rootEntity);
		Attribute<?, ?> firstAttribute = attributes.get(0);

		String value = handler.getValueFromParameter(cleanName, rootEntity);

		info.setValue(value);
		info.setVisible(firstAttribute.isRelevant());
		
		String errorMessage = validationMessageByPath.get(firstAttribute.getPath());
		if (errorMessage != null) {
			info.setInError(true);
			info.setErrorMessage(errorMessage);
		}
		if (firstAttribute instanceof CodeAttribute) {
			CodeAttributeDefinition attrDef = (CodeAttributeDefinition) firstAttribute.getDefinition();
			CodeListService codeListService = record.getSurveyContext().getCodeListService();
			List<CodeListItem> validCodeListItems = codeListService.loadValidItems(firstAttribute.getParent(), attrDef);
			CodeListItem selectedCodeListItem = getCodeListItem(validCodeListItems, value);
			info.setCodeItemId(selectedCodeListItem == null ? null: selectedCodeListItem.getId());
			List<PlacemarkCodedItem> possibleCodedItems = new ArrayList<PlacemarkCodedItem>(validCodeListItems.size() + 1);
			possibleCodedItems.add(new PlacemarkCodedItem(NOT_APPLICABLE_ITEM_CODE, NOT_APPLICABLE_ITEM_LABEL));
			for (CodeListItem item : validCodeListItems) {
				String label = item.getLabel( language );
				// Tries to get the label for the specified language, if not gets the label for the default language 
				if( label == null && !language.equals( record.getSurvey().getDefaultLanguage() ) ){
					label = item.getLabel();
				}
				possibleCodedItems.add(new PlacemarkCodedItem(item.getCode(), label));
			}
			info.setPossibleCodedItems(possibleCodedItems);
		}
		return info;
	}

	private CodeListItem getCodeListItem(List<CodeListItem> items, String code) {
		for (CodeListItem item : items) {
			if (item.getCode().equals(code)) {
				return item;
			}
		}
		return null;
	}

	private Map<String, String> generateValidationMessages(CollectRecord record) {
		RecordValidationReportGenerator validationReportGenerator = new RecordValidationReportGenerator(record);
		List<RecordValidationReportItem> validationItems = validationReportGenerator.generateValidationItems();
		Map<String, String> validationMessageByPath = new HashMap<String, String>(validationItems.size());
		for (RecordValidationReportItem validationItem : validationItems) {
			validationMessageByPath.put(validationItem.getPath(), validationItem.getMessage());
		}
		return validationMessageByPath;
	}
	
	private AbstractAttributeHandler<?> findHandler(String cleanParameterName) {
		for (AbstractAttributeHandler<?> handler : handlers) {
			if (handler.isParameterParseable(cleanParameterName)) {
				return handler;
			}
		}
		logger.warn("Handler not found for the given parameter name: " + cleanParameterName);
		return null;
	}

	private AbstractAttributeHandler<?> findHandler(Node<?> node) {
		return findHandler(node.getDefinition());
	}

	private AbstractAttributeHandler<?> findHandler(NodeDefinition def) {
		for (AbstractAttributeHandler<?> handler : handlers) {
			if (handler.isParseable(def)) {
				return handler;
			}
		}
		logger.warn("Handler not found for the given node type: " + def.getClass().getName());
		return null;
	}
	
	private String cleanUpParameterName(String parameterName) {
		String cleanParameter = removeArraySuffix(parameterName);
		cleanParameter = removePrefix(cleanParameter);
		return cleanParameter;
	}

	public Map<String, String> getValuesByHtmlParameters(Entity plotEntity) {
		Map<String, String> valuesByHTMLParameterName = new HashMap<String, String>();
		
		List<Node<?>> children = plotEntity.getChildren();

		for (Node<?> node : children) {
			getHTMLParameterName(plotEntity, valuesByHTMLParameterName,  node);
		}
		return valuesByHTMLParameterName;
	}
	
	public Map<String, String> getHtmlParameterNameByNodePath(CollectRecord record) {
		return getHtmlParameterNameByNodePath(record.getRootEntity().getDefinition());
	}
	
	public Map<String, String> getHtmlParameterNameByNodePath(final EntityDefinition rootEntity) {
		final CodeListService codeListService = rootEntity.getSurvey().getContext().getCodeListService();

		final Map<String, String> result = new HashMap<String, String>();
		
		rootEntity.traverse(new NodeDefinitionVisitor() {
			public void visit(NodeDefinition def) {
				if (def instanceof AttributeDefinition) {
					EntityDefinition parentDef = def.getParentEntityDefinition();
					if (parentDef == rootEntity) {
						String collectParamName = getCollectParameterBaseName(def);
						if( collectParamName != null ){
							result.put(def.getPath(), collectParamName);
						}
					} else {
						List<NodeDefinition> childDefs = parentDef.getChildDefinitions();
						if (parentDef.isMultiple()) {
							//multiple (enumerated) entity
							CodeAttributeDefinition keyCodeAttribute = parentDef.getEnumeratingKeyCodeAttribute();
							if (keyCodeAttribute == null) {
								throw new IllegalStateException("Enumerating code attribute expected for entity " + parentDef.getPath());
							} else {
								CodeList enumeratingList = keyCodeAttribute.getList();
								List<CodeListItem> enumeratingItems = codeListService.loadRootItems(enumeratingList);
								for (int i = 0; i < enumeratingItems.size(); i++) {
									CodeListItem enumeratingItem = enumeratingItems.get(i);
									String collectParameterBaseName = getCollectParameterBaseName(parentDef) + "[" + enumeratingItem.getCode() + "].";
									
									for (NodeDefinition childDef : childDefs) {
										AbstractAttributeHandler<?> childHandler = findHandler(childDef);
										if( childHandler != null ){
											String collectParameterName = collectParameterBaseName + childHandler.getPrefix() + childDef.getName();
											String enumeratingItemPath = parentDef.getPath() + "[" + (i+1) + "]/" + childDef.getName();
											result.put(enumeratingItemPath, collectParameterName);
										}
									}
								}
							}
						} else {
							//single entity
							String collectParameterBaseName = getCollectParameterBaseName(parentDef) + ".";
							for (NodeDefinition childDef : childDefs) {
								AbstractAttributeHandler<?> childHandler = findHandler(childDef);
								if( childHandler != null ){
									String collectParameterName = collectParameterBaseName + childHandler.getPrefix() + childDef.getName();
									String enumeratingItemPath = parentDef.getPath() + "/" + childDef.getName();
									result.put(enumeratingItemPath, collectParameterName);
								}
							}
						}
					}
				}
			}
		});
		return result;
	}
	
	private String getCollectParameterBaseName(NodeDefinition def) {
		AbstractAttributeHandler<?> handler = findHandler(def);

		if( handler != null ){
			// builds ie. "text_parameter"
			String paramName = handler.getPrefix() + def.getName();
	
			// Saves into "collect_text_parameter"
			return COLLECT_PREFIX + paramName;
		}else{
			return null;
		}
	}

	protected void getHTMLParameterName(Entity plotEntity, Map<String,String> valuesByHtmlParameterName,
			Node<?> node) {
		AbstractAttributeHandler<?> handler = findHandler(node);
		
		if( handler == null ){
			return;
		}
		
		// builds ie. "text_parameter"
		String paramName = handler.getPrefix() + node.getName();

		// Saves into "collect_text_parameter"
		String collectParamName = COLLECT_PREFIX + paramName;
		if (node instanceof Attribute) {
			String value = valuesByHtmlParameterName.get(collectParamName);
			if (value == null) {
				valuesByHtmlParameterName.put(collectParamName, handler.getValueFromParameter(paramName, plotEntity, 0));
			} else {
				int index = StringUtils.countMatches(value, PARAMETER_SEPARATOR) + 1;
				try {
					String newValue = value + PARAMETER_SEPARATOR + handler.getValueFromParameter(paramName, plotEntity, index);
					valuesByHtmlParameterName.put(collectParamName, newValue);
				} catch (Exception e) {
					logger.error("Exception when getting parameters for entity ", e);
				}
			}

		} else if (node instanceof Entity) {
			Entity entity = (Entity) node;
			List<Node<?>> entityChildren = entity.getChildren();
			
			EntityDefinition entityDef = entity.getDefinition();
			if (entityDef.isMultiple()) {
				if (! entityDef.isEnumerable()) {
					throw new IllegalArgumentException("Multiple not enumerated entity found: " + entityDef.getPath());
				}
				// result should be
				// collect_entity_NAME[KEY].code_attribute
				String entityKey = entity.getKeyValues()[0];
				collectParamName += "[" + entityKey + "]";
			}
//			int index = 0;
			for (Node<?> child : entityChildren) {
				AbstractAttributeHandler<?> handlerEntityAttribute = findHandler(child);
				String parameterName = getMultipleParameterName( collectParamName, child, handlerEntityAttribute );
				String parameterValue = getMultipleParameterValue(child,handlerEntityAttribute, entity);
				if( StringUtils.isNotBlank( parameterValue ) ){
//					String previousValue = valuesByHtmlParameterName.get(parameterName);
//					String newValue = previousValue == null ? null: previousValue + PARAMETER_SEPARATOR + parameterValue;
					valuesByHtmlParameterName.put(parameterName, parameterValue);
				}
//				index++;
			}
		}
	}

//	private String getMultipleParameterValue(Node<?> child, AbstractAttributeHandler<?> cah, Entity entity, int index) {
//		return cah.getValueFromParameter(cah.getPrefix() + child.getName(), entity, index);
//	}

	private String getMultipleParameterValue(Node<?> child, AbstractAttributeHandler<?> cah, Entity entity) {
		return cah.getValueFromParameter(cah.getPrefix() + child.getName(), entity);
	}

	private String getMultipleParameterName( String collectParamName, 
			Node<?> child, AbstractAttributeHandler<?> cah ) {
		
		return collectParamName + "." + cah.getPrefix() + child.getName();
	}

	
	private String removeArraySuffix(String parameterName) {

		String cleanParamater = parameterName;
		int lastUnderscore = cleanParamater.lastIndexOf("_");
		String passibleInteger = cleanParamater.substring(lastUnderscore + 1);

		try {
			Integer.parseInt(passibleInteger);

			cleanParamater = cleanParamater.substring(0, lastUnderscore);

		} catch (NumberFormatException e) {
			// It is not an integer suffix, do nothing
		}

		return cleanParamater;
	}

	private String removePrefix(String parameterName) {
		if( parameterName.startsWith(COLLECT_PREFIX) ){
			return parameterName.substring(COLLECT_PREFIX.length());
		}else{
			return parameterName;
		}
	}

	public NodeChangeSet saveToEntity(Map<String, String> parameters, Entity entity) {
		NodeChangeMap result = new NodeChangeMap();
		
		Set<Entry<String, String>> parameterEntries = parameters.entrySet();

		for (Entry<String, String> entry : parameterEntries) {
			String parameterName = entry.getKey();
			String parameterValue = entry.getValue();
			String cleanName = cleanUpParameterName(parameterName);

			AbstractAttributeHandler<?> handler = findHandler(cleanName);
			if (handler != null) {
				try {
					if( handler.isMultiValueAware() ){ // EntityHandler will use the original separated parameter values while the other will take single values
						NodeChangeSet partialChangeSet = handler.addOrUpdate(cleanName, parameterValue, entity, 0);
						result.addMergeChanges(partialChangeSet);
					} else {
						String[] parameterValues = parameterValue.split(BalloonInputFieldsUtils.PARAMETER_SEPARATOR);
						AttributeDefinition attrDef = handler.getAttributeDefinition(entity, cleanName);
						if (attrDef.isMultiple() && parameterValues.length != entity.getCount(attrDef)) {
							//delete old values
							result.addMergeChanges(handler.deleteAttributes(cleanName, entity));
						}
						for (int index = 0; index < parameterValues.length; index++) {
							String parameterVal = parameterValues[index];
							NodeChangeSet partialChangeSet = handler.addOrUpdate(cleanName, parameterVal, entity, index);
							if (partialChangeSet != null) {
								result.addMergeChanges(partialChangeSet);
							}
						}
					}
				} catch (Exception e) {
					logger.error("Error while parsing parameter " + cleanName + " with value " + parameterValue, e);
				}
			} else {
				logger.error("Handler not found for parameter: " + cleanName);
			}
		}
		return result;
	}

	public Attribute<?, ?> getAttributeNodeFromParameter(Entity entity,
			String parameterName, int index) {
		String cleanName = cleanUpParameterName(parameterName);
		AbstractAttributeHandler<?> handler = findHandler(cleanName);
		return handler.getAttributeNodeFromParameter(cleanName, entity, index);
	}
	
}
