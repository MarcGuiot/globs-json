package org.globsframework.json;

import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.globsframework.json.annottations.UnknownAnnotation;
import org.globsframework.metamodel.Field;
import org.globsframework.metamodel.GlobType;
import org.globsframework.metamodel.annotations.FieldNameAnnotationType;
import org.globsframework.metamodel.fields.*;
import org.globsframework.model.Glob;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class GlobTypeArrayGsonAdapter extends TypeAdapter<GlobType> {
    private final boolean forceSort;
    private GlobTypeResolver globTypeResolver;
    private GlobTypeGsonDeserializer globTypeGsonDeserializer;
    private boolean ignoreUnknownAnnotation;

    public GlobTypeArrayGsonAdapter(boolean forceSort, GlobTypeResolver globTypeResolver, boolean ignoreUnknownAnnotation) {
        this.forceSort = forceSort;
        this.globTypeResolver = globTypeResolver;
        this.globTypeGsonDeserializer = new GlobTypeGsonDeserializer(new GlobGSonDeserializer(), globTypeResolver, ignoreUnknownAnnotation);
        this.ignoreUnknownAnnotation = ignoreUnknownAnnotation;
    }

    public void write(JsonWriter out, GlobType type) throws IOException {
        if (type == null) {
            out.nullValue();
            return;
        }
        out.beginObject()
                .name(GlobsGson.TYPE_NAME).value(type.getName());
        if (type.getFieldCount() > 0) {

            out.name(GlobsGson.FIELDS)
                    .beginArray();
            for (Field field : type.getFields()) {
                field.safeVisit(new FieldVisitor() {

                    public void visitInteger(IntegerField field) throws Exception {
                        writeField(field, GlobsGson.INT_TYPE, out);
                    }

                    public void visitIntegerArray(IntegerArrayField field) throws Exception {
                        writeField(field, GlobsGson.INT_ARRAY_TYPE, out);
                    }

                    public void visitDouble(DoubleField field) throws Exception {
                        writeField(field, GlobsGson.DOUBLE_TYPE, out);
                    }

                    public void visitDoubleArray(DoubleArrayField field) throws Exception {
                        writeField(field, GlobsGson.DOUBLE_ARRAY_TYPE, out);
                    }

                    public void visitBigDecimal(BigDecimalField field) throws Exception {
                        writeField(field, GlobsGson.BIG_DECIMAL_TYPE, out);
                    }

                    public void visitBigDecimalArray(BigDecimalArrayField field) throws Exception {
                        writeField(field, GlobsGson.BIG_DECIMAL_ARRAY_TYPE, out);
                    }


                    public void visitString(StringField field) throws Exception {
                        writeField(field, GlobsGson.STRING_TYPE, out);
                    }

                    public void visitStringArray(StringArrayField field) throws Exception {
                        writeField(field, GlobsGson.STRING_ARRAY_TYPE, out);
                    }

                    public void visitBoolean(BooleanField field) throws Exception {
                        writeField(field, GlobsGson.BOOLEAN_TYPE, out);
                    }

                    public void visitBooleanArray(BooleanArrayField field) throws Exception {
                        writeField(field, GlobsGson.BOOLEAN_ARRAY_TYPE, out);
                    }

                    public void visitLong(LongField field) throws Exception {
                        writeField(field, GlobsGson.LONG_TYPE, out);
                    }

                    public void visitLongArray(LongArrayField field) throws Exception {
                        writeField(field, GlobsGson.LONG_ARRAY_TYPE, out);
                    }

                    public void visitDate(DateField field) throws Exception {
                        writeField(field, GlobsGson.DATE_TYPE, out);
                    }

                    public void visitDateTime(DateTimeField field) throws Exception {
                        writeField(field, GlobsGson.DATE_TIME_TYPE, out);
                    }

                    public void visitBlob(BlobField field) throws Exception {
                        writeField(field, GlobsGson.BLOB_TYPE, out);
                    }

                    public void visitGlob(GlobField field) throws Exception {
                        writeField(field, GlobsGson.GLOB_TYPE, out, jsonWriter -> {
                            try {
                                jsonWriter.name(GlobsGson.GLOB_TYPE_KIND).value(field.getType().getName());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    }

                    public void visitGlobArray(GlobArrayField field) throws Exception {
                        writeField(field, GlobsGson.GLOB_ARRAY_TYPE, out, jsonWriter -> {
                            try {
                                jsonWriter.name(GlobsGson.GLOB_TYPE_KIND).value(field.getType().getName());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    }

                    public void visitUnionGlob(GlobUnionField field) throws Exception {
                        writeField(field, GlobsGson.GLOB_UNION_TYPE, out, new JsonUnionFieldWriterConsumer(field.getTypes()));
                    }

                    public void visitUnionGlobArray(GlobArrayUnionField field) throws Exception {
                        writeField(field, GlobsGson.GLOB_UNION_ARRAY_TYPE, out, new JsonUnionFieldWriterConsumer(field.getTypes()));
                    }
                });
            }
            out.endArray();
        }
        writeAnnotations(out, type.streamAnnotations());
        out.endObject();
    }

    private JsonWriter writeField(Field field, String type, JsonWriter out) throws IOException {
        return writeField(field, type, out, jsonWriter -> {
        });
    }

    private JsonWriter writeField(Field field, String type, JsonWriter out, Consumer<JsonWriter> append) throws IOException {
        out.beginObject()
                .name(GlobsGson.FIELD_NAME)
                .value(field.getName())
                .name(GlobsGson.FIELD_TYPE)
                .value(type);
        append.accept(out);

        writeAnnotations(out,
                field.streamAnnotations()
                        .filter(glob -> glob.getType() != FieldNameAnnotationType.TYPE ||
                                !glob.get(FieldNameAnnotationType.NAME).equals(field.getName())));
        out.endObject();
        return out;
    }

    private void writeAnnotations(JsonWriter out, Stream<Glob> annotations) throws IOException {
        //order for test
        GlobGsonAdapter globGsonAdapter = new GlobGsonAdapter(globTypeResolver);
        Stream<Glob> sorted = annotations;
        if (forceSort) {
            sorted = annotations.sorted(Comparator.comparing(g -> g.getType().getName()));
        }
        List<Glob> collect = sorted.collect(Collectors.toList());
        if (!collect.isEmpty()) {
            out.name(GlobsGson.ANNOTATIONS);
            out.beginArray();
            for (Glob glob : collect) {
                if (glob.getType().equals(UnknownAnnotation.TYPE)) {
                    out.jsonValue(glob.get(UnknownAnnotation.CONTENT));
                }
                else {
                    globGsonAdapter.write(out, glob);
                }
            }
            out.endArray();
        }
    }

    public GlobType read(JsonReader in) {
        JsonParser jsonParser = new JsonParser();
        return globTypeGsonDeserializer.deserialize(jsonParser.parse(in));
    }

    private static class JsonUnionFieldWriterConsumer implements Consumer<JsonWriter> {
        private Collection<GlobType> types;

        public JsonUnionFieldWriterConsumer(Collection<GlobType> types) {
            this.types = types;
        }

        public void accept(JsonWriter jsonWriter) {
            try {
                jsonWriter
                        .name(GlobsGson.GLOB_UNION_KINDS)
                        .beginArray();
                for (GlobType globType : types) {
                    jsonWriter.value(globType.getName());
                }
                jsonWriter.endArray();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
