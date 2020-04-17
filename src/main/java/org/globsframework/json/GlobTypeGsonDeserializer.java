package org.globsframework.json;

import com.google.gson.*;
import org.globsframework.metamodel.GlobType;
import org.globsframework.metamodel.GlobTypeBuilder;
import org.globsframework.metamodel.impl.DefaultGlobTypeBuilder;
import org.globsframework.model.Glob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

class GlobTypeGsonDeserializer {
    private static Logger LOGGER = LoggerFactory.getLogger(GlobGSonDeserializer.class);
    private final GlobGSonDeserializer globGSonDeserializer;
    private final GlobTypeResolver globTypeResolver;
    private boolean ignoreUnknownAnnotation;
    private final Map<String, GlobType> types = new ConcurrentHashMap<>(); // pour gérer la recursivitée liée au Union/GlobField

    GlobTypeGsonDeserializer(GlobGSonDeserializer globGSonDeserializer, GlobTypeResolver globTypeResolver, boolean ignoreUnknownAnnotation) {
        this.globGSonDeserializer = globGSonDeserializer;
        this.globTypeResolver = name -> {
            GlobType globType = types.get(name);
            if (globType != null) {
                return globType;
            }
            return globTypeResolver.find(name);
        };
        this.ignoreUnknownAnnotation = ignoreUnknownAnnotation;
    }

    GlobType deserialize(JsonElement json) throws JsonParseException {
        if (json == null || json instanceof JsonNull) {
            return null;
        }
        Runnable clean = () -> {};
        try {
            JsonObject jsonObject = (JsonObject) json;
            JsonElement typeElement = jsonObject.get(GlobsGson.TYPE_NAME);
            if (typeElement == null) {
                throw new RuntimeException("Missing " + GlobsGson.TYPE_NAME + " missing on " + jsonObject);
            }
            String name = typeElement.getAsString();
            GlobTypeBuilder globTypeBuilder = DefaultGlobTypeBuilder.init(name);
            GlobType globType = globTypeBuilder.unCompleteType();
            types.put(name, globType);
            clean = () -> types.remove(name);
            JsonElement fields = jsonObject.get(GlobsGson.FIELDS);
            if (fields != null) {
                if (fields instanceof JsonObject) {
                    for (Map.Entry<String, JsonElement> entry : ((JsonObject) fields).entrySet()) {
                        readField(globTypeBuilder, entry.getKey(), (JsonObject) entry.getValue());
                    }
                } else if (fields instanceof JsonArray) {
                    for (JsonElement jsonElement : ((JsonArray) fields)) {
                        if (jsonElement instanceof JsonObject) {
                            readField(globTypeBuilder, ((JsonObject) jsonElement).get(GlobsGson.FIELD_NAME).getAsString(), ((JsonObject) jsonElement));
                        }
                    }
                }
            }
            List<Glob> globAnnotations = readAnnotations(jsonObject);
            for (Glob globAnnotation : globAnnotations) {
                globTypeBuilder.addAnnotation(globAnnotation);
            }
            return globTypeBuilder.get();
        } catch (JsonParseException e) {
            Gson gson = new Gson();
            LOGGER.error("Fail to parse : " + gson.toJson(json));
            throw e;
        } finally {
            clean.run();
        }
    }

    private void readField(GlobTypeBuilder globTypeBuilder, String attrName, JsonObject value) {
        JsonObject fieldContent = value;
        String type = fieldContent.get(GlobsGson.FIELD_TYPE).getAsString();
        List<Glob> globList = readAnnotations(fieldContent);
        switch (type) {
            case GlobsGson.INT_TYPE:
                globTypeBuilder.declareIntegerField(attrName, globList);
                break;
            case GlobsGson.INT_ARRAY_TYPE:
                globTypeBuilder.declareIntegerArrayField(attrName, globList);
                break;
            case GlobsGson.DOUBLE_TYPE:
                globTypeBuilder.declareDoubleField(attrName, globList);
                break;
            case GlobsGson.DOUBLE_ARRAY_TYPE:
                globTypeBuilder.declareDoubleArrayField(attrName, globList);
                break;
            case GlobsGson.STRING_TYPE:
                globTypeBuilder.declareStringField(attrName, globList);
                break;
            case GlobsGson.STRING_ARRAY_TYPE:
                globTypeBuilder.declareStringArrayField(attrName, globList);
                break;
            case GlobsGson.BOOLEAN_TYPE:
                globTypeBuilder.declareBooleanField(attrName, globList);
                break;
            case GlobsGson.BOOLEAN_ARRAY_TYPE:
                globTypeBuilder.declareBooleanArrayField(attrName, globList);
                break;
            case GlobsGson.LONG_TYPE:
                globTypeBuilder.declareLongField(attrName, globList);
                break;
            case GlobsGson.LONG_ARRAY_TYPE:
                globTypeBuilder.declareLongArrayField(attrName, globList);
                break;
            case GlobsGson.BIG_DECIMAL_TYPE:
                globTypeBuilder.declareBigDecimalField(attrName, globList);
                break;
            case GlobsGson.BIG_DECIMAL_ARRAY_TYPE:
                globTypeBuilder.declareBigDecimalArrayField(attrName, globList);
                break;
            case GlobsGson.DATE_TYPE:
                globTypeBuilder.declareDateField(attrName, globList);
                break;
            case GlobsGson.DATE_TIME_TYPE:
                globTypeBuilder.declareDateTimeField(attrName, globList);
                break;
            case GlobsGson.BLOB_TYPE:
                globTypeBuilder.declareBlobField(attrName, globList);
                break;
            case GlobsGson.GLOB_TYPE:
                globTypeBuilder.declareGlobField(attrName, globTypeResolver.get(fieldContent.get(GlobsGson.GLOB_TYPE_KIND).getAsString()), globList);
                break;
            case GlobsGson.GLOB_ARRAY_TYPE:
                globTypeBuilder.declareGlobArrayField(attrName, globTypeResolver.get(fieldContent.get(GlobsGson.GLOB_TYPE_KIND).getAsString()), globList);
                break;
            case GlobsGson.GLOB_UNION_TYPE: {
                JsonArray kind = fieldContent.get(GlobsGson.GLOB_UNION_KINDS).getAsJsonArray();
                globTypeBuilder.declareGlobUnionField(attrName,
                        StreamSupport.stream(Spliterators.spliterator(kind.iterator(), kind.size(), 0), false)
                                .map(JsonElement::getAsString)
                                .map(globTypeResolver::get).collect(Collectors.toList()), globList);
                break;
            }
            case GlobsGson.GLOB_UNION_ARRAY_TYPE: {
                JsonArray kind = fieldContent.get(GlobsGson.GLOB_UNION_KINDS).getAsJsonArray();
                globTypeBuilder.declareGlobUnionArrayField(attrName,
                        StreamSupport.stream(Spliterators.spliterator(kind.iterator(), kind.size(), 0), false)
                                .map(JsonElement::getAsString)
                                .map(globTypeResolver::get).collect(Collectors.toList()), globList);
                break;
            }
            default:
                throw new RuntimeException(type + " not managed");
        }
    }

    private List<Glob> readAnnotations(JsonObject fieldContent) {
        JsonArray annotations = fieldContent.getAsJsonArray(GlobsGson.ANNOTATIONS);
        List<Glob> globList = Collections.emptyList();
        if (annotations != null) {
            globList = new ArrayList<>();
            for (JsonElement annotation : annotations) {
                if (annotation != null) {
                    globList.add(globGSonDeserializer.deserialize(annotation, globTypeResolver, ignoreUnknownAnnotation));
                }
            }
        }
        return globList;
    }
}
