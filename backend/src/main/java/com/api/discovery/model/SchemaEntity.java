package com.api.discovery.model;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.*;

import java.util.ArrayList;
import java.util.List;

@Data
@Node("SchemaEntity")
public class SchemaEntity {

    @Id @GeneratedValue
    private Long id;

    private String name;
    private String type;           // object, array, string, integer, etc.
    private String description;
    private String format;

    /**
     * Whether this schema is used as a request body
     */
    private Boolean isRequestBody;

    /**
     * Whether this schema is used in a response
     */
    private Boolean isResponseBody;

    /**
     * Example JSON (stored as string)
     */
    private String example;

    /**
     * Relationship to Endpoint (this schema is used by the endpoint)
     */
    @Relationship(type = "USES_SCHEMA", direction = Relationship.Direction.INCOMING)
    private Endpoint endpoint;

    /**
     * Self-relationship for nested schemas (e.g. User contains Address)
     */
    @Relationship(type = "HAS_PROPERTY", direction = Relationship.Direction.OUTGOING)
    private List<SchemaEntity> properties = new ArrayList<>();
}
