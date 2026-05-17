package com.jmix.executor.southinf;

import lombok.Data;

/**
 * Stable metadata exposed to product algorithms.
 */
@Data
public final class AlgorithmDescriptor {

    private String algorithmId;

    private String algorithmVersion;

    private String southApiVersion;
}
