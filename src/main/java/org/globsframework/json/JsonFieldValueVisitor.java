package org.globsframework.json;

import com.google.gson.stream.JsonWriter;
import org.globsframework.json.annottations.IsJsonContentType;
import org.globsframework.json.annottations.JsonAsObjectType;
import org.globsframework.json.annottations.JsonValueAsFieldType;
import org.globsframework.metamodel.Field;
import org.globsframework.metamodel.fields.*;
import org.globsframework.model.Glob;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Objects;

public class JsonFieldValueVisitor implements FieldValueVisitor {
    private final JsonWriter jsonWriter;

    public JsonFieldValueVisitor(JsonWriter jsonWriter) {
        this.jsonWriter = jsonWriter;
    }

    public void visitInteger(IntegerField field, Integer value) throws Exception {
        jsonWriter.name(field.getName());
        jsonWriter.value(value);
    }

    public void visitIntegerArray(IntegerArrayField field, int[] value) throws Exception {
        jsonWriter.name(field.getName());
        if (value != null) {
            jsonWriter.beginArray();
            for (int i : value) {
                jsonWriter.value(i);
            }
            jsonWriter.endArray();
        } else {
            jsonWriter.nullValue();
        }
    }

    public void visitDouble(DoubleField field, Double value) throws Exception {
        jsonWriter.name(field.getName());
        jsonWriter.value(value);
    }

    public void visitDoubleArray(DoubleArrayField field, double[] value) throws Exception {
        jsonWriter.name(field.getName());
        if (value != null) {
            jsonWriter.beginArray();
            for (double v : value) {
                jsonWriter.value(v);
            }
            jsonWriter.endArray();
        } else {
            jsonWriter.nullValue();
        }
    }

    public void visitBigDecimal(BigDecimalField field, BigDecimal value) throws Exception {
        jsonWriter.name(field.getName());
        jsonWriter.value(value);
    }

    public void visitBigDecimalArray(BigDecimalArrayField field, BigDecimal[] value) throws Exception {
        jsonWriter.name(field.getName());
        if (value != null) {
            jsonWriter.beginArray();
            for (BigDecimal v : value) {
                jsonWriter.value(v);
            }
            jsonWriter.endArray();
        } else {
            jsonWriter.nullValue();
        }
    }

    public void visitString(StringField field, String value) throws Exception {
        jsonWriter.name(field.getName());
        if (field.hasAnnotation(IsJsonContentType.UNIQUE_KEY)) {
            jsonWriter.jsonValue(value);
        } else {
            jsonWriter.value(value);
        }
    }

    public void visitStringArray(StringArrayField field, String[] value) throws Exception {
        jsonWriter.name(field.getName());
        if (value != null) {
            jsonWriter.beginArray();
            for (String v : value) {
                if (field.hasAnnotation(IsJsonContentType.UNIQUE_KEY)) {
                    jsonWriter.jsonValue(v);
                } else {
                    jsonWriter.value(v);
                }
            }
            jsonWriter.endArray();
        } else {
            jsonWriter.nullValue();
        }
    }

    public void visitBoolean(BooleanField field, Boolean value) throws Exception {
        jsonWriter.name(field.getName());
        jsonWriter.value(value);
    }

    public void visitBooleanArray(BooleanArrayField field, boolean[] value) throws Exception {
        jsonWriter.name(field.getName());
        if (value != null) {
            jsonWriter.beginArray();
            for (boolean v : value) {
                jsonWriter.value(v);
            }
            jsonWriter.endArray();
        } else {
            jsonWriter.nullValue();
        }
    }

    public void visitLong(LongField field, Long value) throws Exception {
        jsonWriter.name(field.getName());
        jsonWriter.value(value);
    }

    public void visitLongArray(LongArrayField field, long[] value) throws Exception {
        jsonWriter.name(field.getName());
        if (value != null) {
            jsonWriter.beginArray();
            for (long v : value) {
                jsonWriter.value(v);
            }
            jsonWriter.endArray();
        } else {
            jsonWriter.nullValue();
        }
    }

    public void visitDate(DateField field, LocalDate value) throws Exception {
        DateTimeFormatter cachedDateTimeFormatter = GSonUtils.getCachedDateFormatter(field);
        jsonWriter.name(field.getName());
        if (value != null) {
            jsonWriter.value(cachedDateTimeFormatter.format(value));
        } else {
            jsonWriter.nullValue();
        }
    }

    public void visitDateTime(DateTimeField field, ZonedDateTime value) throws Exception {
        DateTimeFormatter timeFormatter = GSonUtils.getCachedDateTimeFormatter(field);
        jsonWriter.name(field.getName());
        if (value != null) {
            jsonWriter.value(timeFormatter.format(value));
        } else {
            jsonWriter.nullValue();
        }
    }

    public void visitBlob(BlobField field, byte[] value) throws Exception {
        jsonWriter.name(field.getName());
        if (value != null) {
            jsonWriter.value(Base64.getEncoder().encodeToString(value));
        } else {
            jsonWriter.nullValue();
        }
    }

    public void visitGlob(GlobField field, Glob value) throws Exception {
        jsonWriter.name(field.getName());
        if (value != null) {
            jsonWriter.beginObject();
            addGlobAttributes(value);
            jsonWriter.endObject();
        } else {
            jsonWriter.nullValue();
        }
    }

    public void visitGlobArray(GlobArrayField field, Glob[] value) throws Exception {
        if (field.hasAnnotation(JsonAsObjectType.UNIQUE_KEY)) {
            jsonWriter.name(field.getName());
            jsonWriter.beginObject();
            Field fieldValueToUseAsName = field.getTargetType().findFieldWithAnnotation(JsonValueAsFieldType.UNIQUE_KEY);
            if (fieldValueToUseAsName == null) {
                throw new RuntimeException("A field with " + JsonValueAsFieldType.TYPE.getName() + " annotation is expected after " +
                        JsonAsObjectType.TYPE.getName() + " for " + field.getFullName());
            }
            for (Glob glob : value) {
                Object value1 = glob.getValue(fieldValueToUseAsName);
                if (value1 == null) {
                    throw new RuntimeException("Value can not be null for a JsonValueAsField field " + fieldValueToUseAsName.getFullName());
                }
                jsonWriter.name(Objects.toString(value1));
                jsonWriter.beginObject();

                glob.safeAccept(new AbstractFieldValueVisitor(){
                    public void notManaged(Field field, Object value) throws Exception {
                        if (field != fieldValueToUseAsName) {
                            field.safeVisit(JsonFieldValueVisitor.this, value);
                        }
                    }
                });

                jsonWriter.endObject();
            }
            jsonWriter.endObject();
        }
        else {
            jsonWriter.name(field.getName());
            if (value != null) {
                jsonWriter.beginArray();
                for (Glob v : value) {
                    jsonWriter.beginObject();
                    addGlobAttributes(v);
                    jsonWriter.endObject();
                }
                jsonWriter.endArray();
            } else {
                jsonWriter.nullValue();
            }
        }
    }

    public void addGlobAttributes(Glob v) {
        v.safeAccept(this);
    }

    public void visitUnionGlob(GlobUnionField field, Glob value) throws Exception {
        jsonWriter.name(field.getName());
        if (value != null) {
            jsonWriter.beginObject();
            jsonWriter.name(value.getType().getName());
            jsonWriter.beginObject();
            addGlobAttributes(value);
            jsonWriter.endObject();
            jsonWriter.endObject();
        }
        else {
            jsonWriter.nullValue();
        }
    }

    public void visitUnionGlobArray(GlobArrayUnionField field, Glob[] value) throws Exception {
        jsonWriter.name(field.getName());
        if (value != null) {
            jsonWriter.beginArray();
            for (Glob v : value) {
                jsonWriter.beginObject();
                jsonWriter.name(v.getType().getName());
                jsonWriter.beginObject();
                addGlobAttributes(v);
                jsonWriter.endObject();
                jsonWriter.endObject();
            }
            jsonWriter.endArray();
        }
        else {
            jsonWriter.nullValue();
        }
    }
}
