package com.indolyn.rill.core.execution;

import java.io.IOException;

public interface RuntimeInfrastructureFactory {
    RuntimeInfrastructure create(String dbName) throws IOException;
}
