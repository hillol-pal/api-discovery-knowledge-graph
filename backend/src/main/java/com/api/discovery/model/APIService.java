package com.api.discovery.model;


import lombok.Data;
import org.springframework.data.neo4j.core.schema.*;

import java.util.ArrayList;
import java.util.List;

@Data
@Node("ApiService")
public class APIService {

    @Id @GeneratedValue
    private Long id;

    private String name;
    private String title;
    private String version;
    private String description;
    private String baseUrl;

    private String embeddingText;
    @Property
    private float[] embedding;

    @Relationship(type = "HAS_ENDPOINT", direction = Relationship.Direction.OUTGOING)
    private List<Endpoint> endpoints = new ArrayList<>();
}
