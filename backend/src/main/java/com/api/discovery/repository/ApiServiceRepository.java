package com.api.discovery.repository;


import com.api.discovery.model.APIService;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface ApiServiceRepository extends Neo4jRepository<APIService, Long> {

    @Query("""
        MATCH (s:ApiService)-[r:HAS_ENDPOINT]->(e:Endpoint)
        RETURN 
            id(s) as sourceId, 
            s.name as sourceName,
            id(e) as targetId, 
            e.path as targetName,
            type(r) as relationship
        LIMIT 300
    """)
    List<Map<String, Object>> getGraphData();

    Optional<APIService> findByNameAndVersion(String name, String version);
}
