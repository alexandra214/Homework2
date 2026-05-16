package com;

import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/rdf")
public class RdfController {

    private Model currentModel;

    @PostMapping("/upload")
    public Map<String, Object> uploadRdfFile(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, String>> nodes = new ArrayList<>();
        List<Map<String, String>> edges = new ArrayList<>();

        if (file.isEmpty()) {
            response.put("status", "error");
            response.put("message", "File is empty.");
            return response;
        }

        try {
            currentModel = ModelFactory.createDefaultModel();
            currentModel.read(file.getInputStream(), null, "RDF/XML");

            Set<String> processedNodeIds = new HashSet<>();

            StmtIterator iter = currentModel.listStatements();
            while (iter.hasNext()) {
                Statement stmt = iter.nextStatement();
                Resource subject = stmt.getSubject();
                Property predicate = stmt.getPredicate();
                RDFNode object = stmt.getObject();

                String subId = subject.toString();
                String objId = object.toString();

                String subLabel = subject.getLocalName() != null ? subject.getLocalName() : subId;
                String predLabel = predicate.getLocalName() != null ? predicate.getLocalName() : predicate.toString();

                String objLabel;
                if (object.isLiteral()) {
                    objLabel = object.asLiteral().getString();
                } else {
                    objLabel = object.asResource().getLocalName() != null ? object.asResource().getLocalName() : objId;
                }

                if (!processedNodeIds.contains(subId)) {
                    nodes.add(Map.of("id", subId, "label", subLabel));
                    processedNodeIds.add(subId);
                }
                if (!processedNodeIds.contains(objId)) {
                    nodes.add(Map.of("id", objId, "label", objLabel));
                    processedNodeIds.add(objId);
                }
                edges.add(Map.of("from", subId, "to", objId, "label", predLabel));
            }

            response.put("status", "success");
            response.put("nodes", nodes);
            response.put("edges", edges);

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return response;
    }
    
}