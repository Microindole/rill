package com.indolyn.rill.core.storage.index;

import com.indolyn.rill.core.model.RID;
import com.indolyn.rill.core.model.Value;

public record KeyValuePair(Value key, RID rid) {
}
