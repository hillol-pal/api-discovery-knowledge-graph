package com.api.discovery.model;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.*;

@Data
@Node("Parameter")
public class Parameter {

    @Id @GeneratedValue
    private Long id;

    private String name;

    /**
     * Location of the parameter: query, path, header, or body
     */
    private String in;

    private String type;           // string, integer, boolean, array, object, etc.
    private Boolean required;
    private String description;
    private String format;         // e.g. uuid, date-time, email

    /**
     * Example value (stored as string for flexibility)
     */
    private String example;

    @Relationship(type = "HAS_PARAMETER", direction = Relationship.Direction.INCOMING)
    private Endpoint endpoint;
}
