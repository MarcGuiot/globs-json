package org.globsframework.json;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.globsframework.metamodel.Field;
import org.globsframework.model.*;

import java.io.IOException;

public class ChangeSetGsonAdapter extends TypeAdapter<ChangeSet> {

    public ChangeSetGsonAdapter() {
    }

    public void write(JsonWriter out, ChangeSet changeSet) throws IOException {
        ChangeValuesGsonAdapter.write(out, changeSet::safeVisit);
    }

    public ChangeSet read(JsonReader in) throws IOException {
        throw new RuntimeException("A changet is not readable, use PreChangeSet.");
    }
}


