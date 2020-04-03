package org.globsframework.json;

import com.google.gson.stream.JsonWriter;
import org.globsframework.metamodel.Field;
import org.globsframework.model.*;

import java.io.IOException;

public class ChangeValuesGsonAdapter {

    public interface ChangeValues {
        void safeVisit(ChangeSetVisitor visitor);
    }

    public static void write(JsonWriter out, ChangeValues changeValues) throws IOException {
        JsonFieldValueVisitor functor = new ChangeSetJsonFieldValueVisitor(out);
        out.beginArray();
        changeValues.safeVisit(new ChangeSetVisitor() {
            @Override
            public void visitCreation(Key key, FieldsValueScanner values) throws Exception {
                out.beginObject();
                out.name("state");
                out.value("create");

                out.name("_kind");
                out.value(key.getGlobType().getName());

                out.name("key");
                out.beginObject();
                key.safeAcceptOnKeyField(functor);
                out.endObject();

                out.name("newValue");
                out.beginObject();
                values.safeAccept(functor.withoutKey());
                out.endObject();

                out.endObject();
            }

            @Override
            public void visitUpdate(Key key, FieldsValueWithPreviousScanner values) throws Exception {
                out.beginObject();
                out.name("state");
                out.value("update");

                out.name("_kind");
                out.value(key.getGlobType().getName());

                out.name("key");
                out.beginObject();
                key.safeAcceptOnKeyField(functor);
                out.endObject();

                out.name("newValue");
                out.beginObject();
                values.safeAccept(functor.withoutKey());
                out.endObject();

                out.name("oldValue");
                out.beginObject();
                values.safeAcceptOnPrevious(functor.withoutKey());

                out.endObject();

                out.endObject();
            }

            @Override
            public void visitDeletion(Key key, FieldsValueScanner previousValues) throws Exception {
                out.beginObject();

                out.name("state");
                out.value("delete");

                out.name("_kind");
                out.value(key.getGlobType().getName());

                out.name("key");
                out.beginObject();
                key.safeAcceptOnKeyField(functor);
                out.endObject();

                out.name("oldValue");
                out.beginObject();
                previousValues.safeAccept(functor.withoutKey());
                out.endObject();

                out.endObject();
            }
        });
        out.endArray();
    }

    static public class ChangeSetJsonFieldValueVisitor extends JsonFieldValueVisitor {

        public ChangeSetJsonFieldValueVisitor(JsonWriter jsonWriter) {
            super(jsonWriter);
        }

        public void addGlobAttributes(Glob v) {
            Field[] fields = v.getType().getKeyFields();
            if (fields.length == 0) {
//                throw new RuntimeException("Only type with key are allowed in a changeSet " + GSonUtils.encode(v, true)); //Pourquoi ?
                super.addGlobAttributes(v);
            } else {
                v.getKey().safeAcceptOnKeyField(this);
            }
        }
    }


}


