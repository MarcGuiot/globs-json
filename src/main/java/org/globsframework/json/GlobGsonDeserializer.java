package org.globsframework.json;

import com.google.gson.*;
import org.globsframework.metamodel.Field;
import org.globsframework.metamodel.GlobType;
import org.globsframework.metamodel.fields.*;
import org.globsframework.model.FieldSetter;
import org.globsframework.model.Glob;
import org.globsframework.model.MutableGlob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

public class GlobGsonDeserializer {
    private static Logger LOGGER = LoggerFactory.getLogger(GlobGsonDeserializer.class);
    private final GlobTypeResolver globTypeResolver;
    protected final static GSonVisitor GSON_VISITOR = new GSonVisitor();

    public GlobGsonDeserializer(GlobTypeResolver globTypeResolver) {
        this.globTypeResolver = globTypeResolver;
    }

    public Glob deserialize(JsonElement json) throws JsonParseException {
        if (json == null || json instanceof JsonNull) {
            return null;
        }
        MutableGlob instantiate = null;
        try {
            JsonObject jsonObject = (JsonObject) json;
            String type = jsonObject.get(GlobsGson.KIND_NAME).getAsString();
            GlobType globType = globTypeResolver.get(type);
            instantiate = readGlob(jsonObject, globType);
        } catch (Exception e) {
            Gson gson = new Gson();
            LOGGER.error("Fail to parse : " + gson.toJson(json), e);
            throw e;
        }
        return instantiate;
    }

    public static MutableGlob readGlob(JsonObject jsonObject, GlobType globType) {
        MutableGlob instantiate;
        instantiate = globType.instantiate();
        for (Field field : globType.getFields()) {
            JsonElement jsonElement = jsonObject.get(field.getName());
            if (jsonElement != null) {
                field.safeVisit(GSON_VISITOR, jsonElement, instantiate);
            }
        }
        return instantiate;
    }

    static class GSonVisitor implements FieldVisitorWithTwoContext<JsonElement, FieldSetter> {
        private Gson gson;

        public GSonVisitor() {
            gson = new Gson();
        }

        public void visitInteger(IntegerField integerField, JsonElement element, FieldSetter fieldSetter) {
            fieldSetter.set(integerField, element.getAsInt());
        }

        @Override
        public void visitIntegerArray(IntegerArrayField field, JsonElement element, FieldSetter fieldSetter) throws Exception {
            JsonArray asJsonArray = element.getAsJsonArray();
            int[] value = new int[asJsonArray.size()];
            int i = 0;
            for (JsonElement jsonElement : asJsonArray) {
                value[i++] = jsonElement.getAsInt();
            }
            fieldSetter.set(field, value);
        }

        public void visitDouble(DoubleField doubleField, JsonElement element, FieldSetter fieldSetter) {
            fieldSetter.set(doubleField, element.getAsDouble());
        }

        @Override
        public void visitDoubleArray(DoubleArrayField field, JsonElement element, FieldSetter fieldSetter) throws Exception {
            JsonArray asJsonArray = element.getAsJsonArray();
            double[] value = new double[asJsonArray.size()];
            int i = 0;
            for (JsonElement jsonElement : asJsonArray) {
                value[i++] = jsonElement.getAsDouble();
            }
            fieldSetter.set(field, value);
        }

        public void visitString(StringField stringField, JsonElement element, FieldSetter fieldSetter) {
            if (stringField.hasAnnotation(IsJsonContentType.UNIQUE_KEY) || element.isJsonObject() || element.isJsonArray()) {
                if (element.isJsonObject()) {
                    JsonObject jsonObject = element.getAsJsonObject();
                    if (jsonObject != null) {
                        fieldSetter.set(stringField, gson.toJson(jsonObject));
                    }
                } else if (element.isJsonArray()) {
                    JsonArray jsonArray = element.getAsJsonArray();
                    if (jsonArray != null) {
                        fieldSetter.set(stringField, gson.toJson(jsonArray));
                    }
                }
            } else {
                fieldSetter.set(stringField, element.getAsString());
            }
        }

        @Override
        public void visitStringArray(StringArrayField field, JsonElement arrayElements, FieldSetter fieldSetter) throws Exception {
            JsonArray asJsonArray = arrayElements.getAsJsonArray();
            String value[] = new String[asJsonArray.size()];
            int i = 0;
            for (JsonElement element : asJsonArray) {
                if (field.hasAnnotation(IsJsonContentType.UNIQUE_KEY) || element.isJsonObject() || element.isJsonArray()) {
                    if (element.isJsonObject()) {
                        JsonObject jsonObject = element.getAsJsonObject();
                        if (jsonObject != null) {
                            value[i] = gson.toJson(jsonObject);
                        }
                    } else if (element.isJsonArray()) {
                        JsonArray jsonArray = element.getAsJsonArray();
                        if (jsonArray != null) {
                            value[i] = gson.toJson(jsonArray);
                        }
                    }
                } else {
                    value[i] = element.getAsString();
                }
                ++i;
            }
            fieldSetter.set(field, value);
        }

        public void visitBoolean(BooleanField booleanField, JsonElement element, FieldSetter fieldSetter) {
            fieldSetter.set(booleanField, element.getAsBoolean());
        }

        @Override
        public void visitBooleanArray(BooleanArrayField field, JsonElement element, FieldSetter fieldSetter) throws Exception {
            JsonArray asJsonArray = element.getAsJsonArray();
            boolean[] value = new boolean[asJsonArray.size()];
            int i = 0;
            for (JsonElement jsonElement : asJsonArray) {
                value[i++] = jsonElement.getAsBoolean();
            }
            fieldSetter.set(field, value);

        }

        @Override
        public void visitBigDecimal(BigDecimalField field, JsonElement element, FieldSetter fieldSetter) throws Exception {
            fieldSetter.set(field, element.getAsBigDecimal());
        }

        @Override
        public void visitBigDecimalArray(BigDecimalArrayField field, JsonElement element, FieldSetter fieldSetter) throws Exception {
            JsonArray asJsonArray = element.getAsJsonArray();
            BigDecimal[] value = new BigDecimal[asJsonArray.size()];
            int i = 0;
            for (JsonElement jsonElement : asJsonArray) {
                value[i++] = jsonElement.getAsBigDecimal();
            }
            fieldSetter.set(field, value);

        }

        public void visitLong(LongField longField, JsonElement element, FieldSetter fieldSetter) {
            fieldSetter.set(longField, element.getAsLong());
        }

        @Override
        public void visitLongArray(LongArrayField field, JsonElement element, FieldSetter fieldSetter) throws Exception {
            JsonArray asJsonArray = element.getAsJsonArray();
            long[] value = new long[asJsonArray.size()];
            int i = 0;
            for (JsonElement jsonElement : asJsonArray) {
                value[i++] = jsonElement.getAsLong();
            }
            fieldSetter.set(field, value);
        }

        @Override
        public void visitDate(DateField field, JsonElement element, FieldSetter fieldSetter) throws Exception {
            fieldSetter.set(field, LocalDate.from(DateTimeFormatter.ISO_DATE.parse(element.getAsString())));
        }

        @Override
        public void visitDateTime(DateTimeField field, JsonElement element, FieldSetter fieldSetter) throws Exception {
            fieldSetter.set(field, ZonedDateTime.from(DateTimeFormatter.ISO_DATE_TIME.parse(element.getAsString())));
        }

        public void visitBlob(BlobField blobField, JsonElement element, FieldSetter fieldSetter) {
            fieldSetter.set(blobField,
                    Base64.getDecoder().decode(element.getAsString()));
        }
    }
}
