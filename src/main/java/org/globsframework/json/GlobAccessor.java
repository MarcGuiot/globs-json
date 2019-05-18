package org.globsframework.json;

import org.globsframework.model.Glob;
import org.globsframework.model.Key;

public interface GlobAccessor {
    Glob get(Key key);
}
