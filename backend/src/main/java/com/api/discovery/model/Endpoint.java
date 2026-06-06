package com.api.discovery.model;


import lombok.Data;
import org.springframework.data.neo4j.core.schema.*;

import java.util.ArrayList;
import java.util.List;

@Data
@Node("Endpoint")
public class Endpoint {

    @Id @GeneratedValue
    private Long id;

    private String path;
    private String method;
    private String summary;
    private String description;
    private String operationId;

    private String embeddingText;

    @Property
    private float[] embedding;

    @Relationship(type = "HAS_PARAMETER", direction = Relationship.Direction.OUTGOING)
    private List<Parameter> parameters = new ArrayList<>();
}
