# Homework2

[Github link](https://github.com/alexandra214/Homework2)

**Team Members:**
- Oblu Alexandra-Mihaela
- Matei Eduard-Gabriel

## Project Overview
Knowledge Graph Library is a web-based application designed to parse, visualize, and intelligently query RDF/XML ontologies. The platform reads dynamically from uploaded RDF graphs using Apache Jena, and features a Retrieval-Augmented Generation (RAG) AI assistant powered by a local Large Language Model and a custom in-memory Vector Database.

## Division of Labor
We divided this project to efficiently tackle the ontology visualization, backend data extraction, and AI integration:

**Alexandra - Ontology Engineering, Graph Visualization & UI/UX (Exercises 2, 3, 5, 6)**
- Created the core knowledge graph architecture and generated the `exercise6.rdf` ontology using Protégé (Ex 5).
- Engineered the interactive network graph visualization to dynamically render RDF relationships directly in the browser (Ex 2 & 3).
- Designed the responsive Baby Blue CSS theme, unified the frontend architecture, and styled the floating chat widget (Ex 6).

**Eduard - Backend Architecture, Data Extraction & RAG AI (Exercises 1, 4, 7)**
- Implemented the file upload backend and initial Apache Jena parsing logic (Ex 1).
- Engineered the dynamic Library Dashboard using Duck-Typing extraction and built the individual dedicated resource routing (`book.html`) (Ex 4).
- Built the custom in-memory Vector Database, Cosine Similarity search, and integrated the Ollama local LLM (`llama3.2:3b`) for the RAG chatbot (Ex 7).

## Main Features

### RDF Extraction & Visualization
- **Dynamic Parsing:** Automatically processes uploaded `.rdf` files into the Java backend.
- **Graph Rendering:** Displays interactive network visualizations of the knowledge base relationships directly in the browser.

### Dynamic Knowledge Dashboard
- **Duck-Typing Search:** Finds entities based on graph relationships (like `suitableForLevel`) to bypass strict class-naming errors.
- **Dedicated Resource Pages:** Fetches and displays isolated metadata by querying a specific URL parameter (e.g., `book.html?name=Dune`).

### Intelligent RAG Chatbot
- **Custom Vector DB:** Stores extracted RDF facts as mathematical float arrays directly in Java memory.
- **Cosine Similarity Search:** Retrieves only the top most semantically relevant facts to feed the AI context window.
- **Local LLM Integration:** Communicates safely with Ollama (`llama3.2:3b`) without requiring internet APIs or paid keys.

### Context-Aware UI & Styling
- **Intuitive UI:** Features a custom Baby Blue & Sky Blue theme with responsive CSS design and zero inline HTML styling.
- **Floating Chat Widget:** A persistent, modern chat window that tracks user navigation.
- **Contextual Starters:** Generates specific chat suggestion buttons based on the exact resource page the user is currently viewing.

## Technical Stack
- **Backend:** Java 17, Spring Boot, Spring Web
- **Data & Queries:** Apache Jena, RDF/XML
- **AI & ML:** Ollama, `llama3.2:3b`, `nomic-embed-text`
- **Frontend:** HTML5, CSS3, Vanilla JavaScript
- **Server:** Maven, embedded Tomcat

## Setup & Run

**1. Install Dependencies & Local AI**
Before starting the server, ensure [Ollama](https://ollama.com/) is installed. Open your terminal and run these commands to download the necessary models:

```bash
ollama run llama3.2:3b
ollama pull nomic-embed-text

```

**2. Start the Server**

```bash
mvn clean spring-boot:run

```

**3. Launch**
Open your browser and navigate to: `http://localhost:8080/`
