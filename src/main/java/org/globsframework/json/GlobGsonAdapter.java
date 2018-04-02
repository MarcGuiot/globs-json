package org.globsframework.json;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.globsframework.metamodel.Field;
import org.globsframework.metamodel.GlobType;
import org.globsframework.metamodel.fields.*;
import org.globsframework.model.Glob;
import org.globsframework.model.MutableGlob;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

class GlobGsonAdapter extends TypeAdapter<Glob> {
    private GlobTypeResolver resolver;
    private Gson gson = new Gson();
    private final ReadJsonWithReaderFieldVisitor readJsonWithReaderFieldVisitor = new ReadJsonWithReaderFieldVisitor();
    private final GlobGsonDeserializer globGsonDeserializer;

    public GlobGsonAdapter(GlobTypeResolver resolver) {
        this.resolver = resolver;
        globGsonDeserializer = new GlobGsonDeserializer(gson, this.resolver);
    }

    public void write(JsonWriter out, Glob value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }
        out.beginObject();
        GlobType type = value.getType();
        out.name(GlobsGson.KIND_NAME).value(type.getName());
        value.safeAccept(new JsonFieldValueVisitor(this, out));
        out.endObject();
    }

    public Glob read(JsonReader in) throws IOException {
        in.beginObject();
        if (in.hasNext() && in.peek() != JsonToken.END_OBJECT) {
            String name = in.nextName();
            if (name.equalsIgnoreCase(GlobsGson.KIND_NAME)) {
                String kind = in.nextString();
                Glob glob = readFieldsWithReader(in, resolver.get(kind));
                in.endObject();
                return glob;
            } else {
                return readFieldByField(in, name);
            }
        }
        return null;
    }

    private Glob readFieldByField(JsonReader in, String name) throws IOException {
        JsonParser jsonParser = new JsonParser();
        Map<String, JsonElement> values = new HashMap<>();
        values.put(name, jsonParser.parse(in));
        while (in.peek() != JsonToken.END_OBJECT) {
            values.put(in.nextName(), jsonParser.parse(in));
        }
        in.endObject();
        JsonElement kindElement = values.get(GlobsGson.KIND_NAME);
        if (kindElement == null) {
            throw new RuntimeException("kind not found in " + values);
        }
        GlobType type = resolver.get(kindElement.getAsString());
        MutableGlob instantiate = type.instantiate();
        for (Map.Entry<String, JsonElement> stringJsonElementEntry : values.entrySet()) {
            Field field = type.findField(stringJsonElementEntry.getKey());
            if (field != null) {
                field.safeVisit(globGsonDeserializer.getGSonVisitor(), stringJsonElementEntry.getValue(), instantiate);
            }
        }
        return instantiate;
    }

    public Glob readFieldsWithReader(JsonReader in, GlobType globType) throws IOException {
        MutableGlob instantiate = globType.instantiate();
        while (in.hasNext() && in.peek() == JsonToken.NAME) {
            String name = in.nextName();
            Field field = globType.findField(name);
            if (field != null) {
                field.safeVisit(readJsonWithReaderFieldVisitor, instantiate, in);
            }
        }
        return instantiate;
    }

    private static class JsonFieldValueVisitor implements FieldValueVisitor {
        private final GlobGsonAdapter globGsonAdapter;
        private final JsonWriter jsonWriter;

        public JsonFieldValueVisitor(GlobGsonAdapter globGsonAdapter, JsonWriter jsonWriter) {
            this.globGsonAdapter = globGsonAdapter;
            this.jsonWriter = jsonWriter;
        }

        public void visitInteger(IntegerField field, Integer value) throws Exception {
            if (value != null) {
                jsonWriter.name(field.getName());
                jsonWriter.value(value);
            }
        }

        public void visitIntegerArray(IntegerArrayField field, int[] value) throws Exception {
            if (value != null) {
                jsonWriter.name(field.getName());
                jsonWriter.beginArray();
                for (int i : value) {
                    jsonWriter.value(i);
                }
                jsonWriter.endArray();
            }
        }

        public void visitDouble(DoubleField field, Double value) throws Exception {
            if (value != null) {
                jsonWriter.name(field.getName());
                jsonWriter.value(value);
            }
        }

        @Override
        public void visitDoubleArray(DoubleArrayField field, double[] value) throws Exception {
            if (value != null) {
                jsonWriter.name(field.getName());
                jsonWriter.beginArray();
                for (double v : value) {
                    jsonWriter.value(v);
                }
                jsonWriter.endArray();
            }
        }

        @Override
        public void visitBigDecimal(BigDecimalField field, BigDecimal value) throws Exception {
            if (value != null) {
                jsonWriter.name(field.getName());
                jsonWriter.value(value);
            }
        }

        @Override
        public void visitBigDecimalArray(BigDecimalArrayField field, BigDecimal[] value) throws Exception {
            if (value != null) {
                jsonWriter.name(field.getName());
                jsonWriter.beginArray();
                for (BigDecimal v : value) {
                    jsonWriter.value(v);
                }
                jsonWriter.endArray();
            }
        }

        @Override
        public void visitString(StringField field, String value) throws Exception {
            if (value != null) {
                jsonWriter.name(field.getName());
                if (field.hasAnnotation(IsJsonContentType.UNIQUE_KEY)) {
                    jsonWriter.jsonValue(value);
                } else {
                    jsonWriter.value(value);
                }
            }
        }

        @Override
        public void visitStringArray(StringArrayField field, String[] value) throws Exception {
            if (value != null) {
                jsonWriter.name(field.getName());
                jsonWriter.beginArray();
                for (String v : value) {
                    if (field.hasAnnotation(IsJsonContentType.UNIQUE_KEY)) {
                        jsonWriter.jsonValue(v);
                    }
                    else {
                        jsonWriter.value(v);
                    }
                }
                jsonWriter.endArray();
            }
        }

        @Override
        public void visitBoolean(BooleanField field, Boolean value) throws Exception {
            if (value != null) {
                jsonWriter.name(field.getName());
                jsonWriter.value(value);
            }
        }

        @Override
        public void visitBooleanArray(BooleanArrayField field, boolean[] value) throws Exception {
            if (value != null) {
                jsonWriter.name(field.getName());
                jsonWriter.beginArray();
                for (boolean v : value) {
                    jsonWriter.value(v);
                }
                jsonWriter.endArray();
            }
        }

        @Override
        public void visitLong(LongField field, Long value) throws Exception {
            if (value != null) {
                jsonWriter.name(field.getName());
                jsonWriter.value(value);
            }
        }

        @Override
        public void visitLongArray(LongArrayField field, long[] value) throws Exception {
            if (value != null) {
                jsonWriter.name(field.getName());
                jsonWriter.beginArray();
                for (long v : value) {
                    jsonWriter.value(v);
                }
                jsonWriter.endArray();
            }
        }

        @Override
        public void visitDate(DateField field, LocalDate value) throws Exception {
            if (value != null) {
                jsonWriter.name(field.getName());
                jsonWriter.value(DateTimeFormatter.ISO_DATE.format(value));
            }
        }

        @Override
        public void visitDateTime(DateTimeField field, ZonedDateTime value) throws Exception {
            if (value != null) {
                jsonWriter.name(field.getName());
                jsonWriter.value(DateTimeFormatter.ISO_DATE_TIME.format(value));
            }

        }

        @Override
        public void visitBlob(BlobField field, byte[] value) throws Exception {
            if (value != null) {
                jsonWriter.name(field.getName());
                jsonWriter.value(Base64.getEncoder().encodeToString(value));
            }
        }

        @Override
        public void visitGlob(GlobField field, Glob value) throws Exception {
            if (value != null) {
                jsonWriter.name(field.getName());
                globGsonAdapter.write(jsonWriter, value);
            }
        }

        @Override
        public void visitGlobArray(GlobArrayField field, Glob[] value) throws Exception {
            if (value != null) {
                jsonWriter.name(field.getName());
                jsonWriter.beginArray();
                for (Glob v : value) {
                    globGsonAdapter.write(jsonWriter, v);
                }
                jsonWriter.endArray();
            }
        }
    }

    private class ReadJsonWithReaderFieldVisitor implements FieldVisitorWithTwoContext<MutableGlob, JsonReader> {
        @Override
        public void visitInteger(IntegerField field, MutableGlob mutableGlob, JsonReader jsonReader) throws Exception {
            mutableGlob.set(field, jsonReader.nextInt());
        }

        @Override
        public void visitIntegerArray(IntegerArrayField field, MutableGlob mutableGlob, JsonReader jsonReader) throws Exception {
            jsonReader.beginArray();
            int[] values = new int[16];
            int count = 0;
            while (jsonReader.peek() != JsonToken.END_ARRAY) {
                if (values.length == count) {
                    values = Arrays.copyOf(values, values.length * 2);
                }
                values[count++] = jsonReader.nextInt();
            }
            jsonReader.endArray();
            mutableGlob.set(field, Arrays.copyOf(values, count));
        }

        @Override
        public void visitDouble(DoubleField field, MutableGlob mutableGlob, JsonReader jsonReader) throws Exception {
            mutableGlob.set(field, jsonReader.nextDouble());
        }

        @Override
        public void visitDoubleArray(DoubleArrayField field, MutableGlob mutableGlob, JsonReader jsonReader) throws Exception {
            jsonReader.beginArray();
            double[] values = new double[16];
            int count = 0;
            while (jsonReader.peek() != JsonToken.END_ARRAY) {
                if (values.length == count) {
                    values = Arrays.copyOf(values, values.length * 2);
                }
                values[count++] = jsonReader.nextDouble();
            }
            jsonReader.endArray();
            mutableGlob.set(field, Arrays.copyOf(values, count));
        }

        @Override
        public void visitString(StringField field, MutableGlob mutableGlob, JsonReader jsonReader) throws Exception {
            JsonToken peek = jsonReader.peek();
            if (peek != JsonToken.NULL) {
                if (field.hasAnnotation(IsJsonContentType.UNIQUE_KEY)) {
                    JsonParser jsonParser = new JsonParser();
                    JsonElement parse = jsonParser.parse(jsonReader);
                    if (parse.isJsonArray()) {
                        mutableGlob.set(field, gson.toJson(parse.getAsJsonArray()));
                    } else if (parse.isJsonObject()) {
                        mutableGlob.set(field, gson.toJson(parse.getAsJsonObject()));
                    } else if (parse.isJsonPrimitive()) {
                        mutableGlob.set(field, gson.toJson(parse.getAsJsonPrimitive()));
                    } else {
                        throw new RuntimeException(peek + " not managed in " + field.getFullName());
                    }
                } else {
                    mutableGlob.set(field, jsonReader.nextString());
                }
            }

        }

        @Override
        public void visitStringArray(StringArrayField field, MutableGlob mutableGlob, JsonReader jsonReader) throws Exception {
            jsonReader.beginArray();
            String[] values = new String[16];
            int count = 0;
            while (jsonReader.peek() != JsonToken.END_ARRAY) {
                if (values.length == count) {
                    values = Arrays.copyOf(values, values.length * 2);
                }
                if (field.hasAnnotation(IsJsonContentType.UNIQUE_KEY)) {
                    JsonParser jsonParser = new JsonParser();
                    JsonElement parse = jsonParser.parse(jsonReader);
                    if (parse.isJsonArray()) {
                        values[count++] = gson.toJson(parse.getAsJsonArray());
                    } else if (parse.isJsonObject()) {
                        values[count++] = gson.toJson(parse.getAsJsonObject());
                    } else if (parse.isJsonPrimitive()) {
                        values[count++] = gson.toJson(parse.getAsJsonPrimitive());
                    } else {
                        throw new RuntimeException(parse + " not managed in " + field.getFullName());
                    }
                } else {
                    values[count++] = jsonReader.nextString();
                }
            }
            jsonReader.endArray();
            mutableGlob.set(field, Arrays.copyOf(values, count));
        }

        @Override
        public void visitBoolean(BooleanField field, MutableGlob mutableGlob, JsonReader jsonReader) throws Exception {
            mutableGlob.set(field, jsonReader.nextBoolean());
        }

        @Override
        public void visitBooleanArray(BooleanArrayField field, MutableGlob mutableGlob, JsonReader jsonReader) throws Exception {
            jsonReader.beginArray();
            boolean[] values = new boolean[16];
            int count = 0;
            while (jsonReader.peek() != JsonToken.END_ARRAY) {
                if (values.length == count) {
                    values = Arrays.copyOf(values, values.length * 2);
                }
                values[count++] = jsonReader.nextBoolean();
            }
            jsonReader.endArray();
            mutableGlob.set(field, Arrays.copyOf(values, count));
        }

        @Override
        public void visitBigDecimal(BigDecimalField field, MutableGlob mutableGlob, JsonReader jsonReader) throws Exception {
            mutableGlob.set(field, new BigDecimal(jsonReader.nextString()));
        }

        @Override
        public void visitBigDecimalArray(BigDecimalArrayField field, MutableGlob mutableGlob, JsonReader jsonReader) throws Exception {
            jsonReader.beginArray();
            BigDecimal[] values = new BigDecimal[16];
            int count = 0;
            while (jsonReader.peek() != JsonToken.END_ARRAY) {
                if (values.length == count) {
                    values = Arrays.copyOf(values, values.length * 2);
                }
                values[count++] = new BigDecimal(jsonReader.nextString());
            }
            jsonReader.endArray();
            mutableGlob.set(field, Arrays.copyOf(values, count));
        }

        @Override
        public void visitLong(LongField field, MutableGlob mutableGlob, JsonReader jsonReader) throws Exception {
            mutableGlob.set(field, jsonReader.nextLong());
        }

        @Override
        public void visitLongArray(LongArrayField field, MutableGlob mutableGlob, JsonReader jsonReader) throws Exception {
            jsonReader.beginArray();
            long[] values = new long[16];
            int count = 0;
            while (jsonReader.peek() != JsonToken.END_ARRAY) {
                if (values.length == count) {
                    values = Arrays.copyOf(values, values.length * 2);
                }
                values[count++] = jsonReader.nextLong();
            }
            jsonReader.endArray();
            mutableGlob.set(field, Arrays.copyOf(values, count));
        }

        @Override
        public void visitDate(DateField field, MutableGlob mutableGlob, JsonReader jsonReader) throws Exception {
            mutableGlob.set(field, LocalDate.from(DateTimeFormatter.ISO_DATE.parse(jsonReader.nextString())));
        }

        @Override
        public void visitDateTime(DateTimeField field, MutableGlob mutableGlob, JsonReader jsonReader) throws Exception {
            mutableGlob.set(field, ZonedDateTime.from(DateTimeFormatter.ISO_DATE_TIME.parse(jsonReader.nextString())));

        }

        @Override
        public void visitBlob(BlobField field, MutableGlob mutableGlob, JsonReader jsonReader) throws Exception {
            mutableGlob.set(field, Base64.getDecoder().decode(jsonReader.nextString()));
        }

        @Override
        public void visitGlob(GlobField field, MutableGlob mutableGlob, JsonReader jsonReader) throws Exception {
            mutableGlob.set(field, read(jsonReader));
        }

        @Override
        public void visitGlobArray(GlobArrayField field, MutableGlob mutableGlob, JsonReader jsonReader) throws Exception {
            jsonReader.beginArray();
            Glob[] values = new Glob[16];
            int count = 0;
            while (jsonReader.peek() != JsonToken.END_ARRAY) {
                if (values.length == count) {
                    values = Arrays.copyOf(values, values.length * 2);
                }
                values[count++] = read(jsonReader);
            }
            jsonReader.endArray();
            mutableGlob.set(field, Arrays.copyOf(values, count));
        }

    }
}
