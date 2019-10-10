package org.globsframework.json;

import com.google.gson.stream.JsonWriter;
import org.globsframework.metamodel.fields.*;
import org.globsframework.model.Glob;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

class JsonFieldValueVisitor implements FieldValueVisitor {
    private final JsonWriter jsonWriter;

    public JsonFieldValueVisitor(JsonWriter jsonWriter) {
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

    public void visitBigDecimal(BigDecimalField field, BigDecimal value) throws Exception {
        if (value != null) {
            jsonWriter.name(field.getName());
            jsonWriter.value(value);
        }
    }

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

    public void visitStringArray(StringArrayField field, String[] value) throws Exception {
        if (value != null) {
            jsonWriter.name(field.getName());
            jsonWriter.beginArray();
            for (String v : value) {
                if (field.hasAnnotation(IsJsonContentType.UNIQUE_KEY)) {
                    jsonWriter.jsonValue(v);
                } else {
                    jsonWriter.value(v);
                }
            }
            jsonWriter.endArray();
        }
    }

    public void visitBoolean(BooleanField field, Boolean value) throws Exception {
        if (value != null) {
            jsonWriter.name(field.getName());
            jsonWriter.value(value);
        }
    }

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

    public void visitLong(LongField field, Long value) throws Exception {
        if (value != null) {
            jsonWriter.name(field.getName());
            jsonWriter.value(value);
        }
    }

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

    public void visitDate(DateField field, LocalDate value) throws Exception {
        if (value != null) {
            DateTimeFormatter cachedDateTimeFormatter = GSonUtils.getCachedDateFormatter(field);
            jsonWriter.name(field.getName());
            jsonWriter.value(cachedDateTimeFormatter.format(value));
        }
    }

    public void visitDateTime(DateTimeField field, ZonedDateTime value) throws Exception {
        if (value != null) {
            DateTimeFormatter timeFormatter = GSonUtils.getCachedDateTimeFormatter(field);
            jsonWriter.name(field.getName());
            jsonWriter.value(timeFormatter.format(value));
        }
    }

    public void visitBlob(BlobField field, byte[] value) throws Exception {
        if (value != null) {
            jsonWriter.name(field.getName());
            jsonWriter.value(Base64.getEncoder().encodeToString(value));
        }
    }

    public void visitGlob(GlobField field, Glob value) throws Exception {
        if (value != null) {
            jsonWriter.name(field.getName());
            jsonWriter.beginObject();
            addGlobAttributes(value);
            jsonWriter.endObject();
        }
    }

    public void visitGlobArray(GlobArrayField field, Glob[] value) throws Exception {
        if (value != null) {
            jsonWriter.name(field.getName());
            jsonWriter.beginArray();
            for (Glob v : value) {
                jsonWriter.beginObject();
                addGlobAttributes(v);
                jsonWriter.endObject();
            }
            jsonWriter.endArray();
        }
    }

    public void addGlobAttributes(Glob v) {
        v.safeAccept(this);
    }

    public void visitUnionGlob(GlobUnionField field, Glob value) throws Exception {
        if (value != null) {
            jsonWriter.name(field.getName());
            jsonWriter.beginObject();
            jsonWriter.name(value.getType().getName());
            jsonWriter.beginObject();
            addGlobAttributes(value);
            jsonWriter.endObject();
            jsonWriter.endObject();
        }
    }

    public void visitUnionGlobArray(GlobArrayUnionField field, Glob[] value) throws Exception {
        if (value != null) {
            jsonWriter.name(field.getName());
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

    }
}
