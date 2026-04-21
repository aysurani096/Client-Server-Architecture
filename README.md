# Smart Campus RESTful API

## Overview of API Design

This project implements a Smart Campus RESTful API using Java, Maven, Jersey (JAX-RS), and the Grizzly embedded server.

The API allows management of campus infrastructure resources such as rooms and environmental sensors. It also supports nested resources for storing and retrieving historical sensor readings.

The design follows REST principles including:

- Resource-based URI structure
- Proper HTTP method usage
- Hierarchical resource relationships
- Query parameter filtering
- Standard HTTP status codes
- Global exception handling
- Stateless server architecture

The system uses in-memory data storage for simplicity.

==============

## Technologies Used

- Java
- Maven
- JAX-RS (Jersey)
- Grizzly HTTP Server
- Postman
- VS Code

==============

## How to Build and Run the Project

### Step 1 — Clone Repository

```bash
git clone https://github.com/your-username/smart-campus-api.git
cd smart-campus-api
Step 2 — Build Project
mvn clean compile
Step 3 — Run Server
mvn exec:java

Server will start at:

http://localhost:8080/api/v1


==============
Sample curl Commands


1️⃣ Create Room
curl -X POST http://localhost:8080/api/v1/rooms \
-H "Content-Type: application/json" \
-d '{"id":"LAB-101","name":"Networking Lab","capacity":60}'

2️⃣ Get All Rooms
curl http://localhost:8080/api/v1/rooms

3️⃣ Create Sensor
curl -X POST http://localhost:8080/api/v1/sensors \
-H "Content-Type: application/json" \
-d '{"id":"TEMP-101","type":"Temperature","status":"ACTIVE","currentValue":25.5,"roomId":"LAB-101"}'

4️⃣ Filter Sensors by Type
curl http://localhost:8080/api/v1/sensors?type=Temperature

5️⃣ Add Sensor Reading
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-101/readings \
-H "Content-Type: application/json" \
-d '{"id":"R1","timestamp":1710000000,"value":28.4}'

6️⃣ Get Sensor Readings
curl http://localhost:8080/api/v1/sensors/TEMP-101/readings

==============
Business Logic Rules

Sensor cannot be created if the room does not exist.

Room cannot be deleted if sensors are assigned.

Sensor reading updates the current sensor value.

Query parameters allow filtering sensors by type.

Data is stored in memory and resets when server restarts.

==============
Base URL:
the server will start at http://localhost:8080/api/v1
```

============

<!-- Overview
The project uses Java, Maven and JAX-RS (Jersey) to implement a RESTful Smart Campus management API.
The system operates hierarchical campus resources such as Rooms and Sensors and Sensor Readings in accordance with requirements of the REST architectural design, such as resource-based URIs, stateless communication, adequate HTTP status codes, hypermedia navigation and structured error control.
The API will model a virtual enterprise backend framework that provides facilities management and automated IoT surveillance services.

Part 1 - Service Architecture and Set up
Q1 Lifecycle of JAX-RS Resource Class
The default JAX-RS resource classes are per-request; that is, a new instance is instantiated with each HTTP request.
This enhances thread safety because no two simultaneous requests have the same object instance.
In this project, however, shared storage is done using in-memory collections (ConcurrentHashMap).
Therefore:
• The synchronisation is needed to prevent race conditions.
• Preferably, stateless resource methods are used.
• Group variable state should be managed with care.
This architecture provides scalability, request isolation and predictable behaviour at concurrent load.

Q2 – Significance of Hypermedia (HATEOAS)
The discovery endpoint (GET /api/v1) provides API metadata and links to navigation items of main resources like:
• /api/v1/rooms
• /api/v1/sensors
This proves HATEOAS (Hypermedia as the Engine of Application State).
Benefits:
• Clients dynamically learn endpoints, rather than URLs.
• It becomes simpler to version and develop API.
• Enhances developer experience and decouples the client and server.
APIs with hypermedia are viewed as more advanced implementations of REST.

Part 2 – Room Management
Q3 - Only IDs vs Full Room Objects
Sending back resource IDs only saves on network traffic, however, at the cost of more complicated clients making more API calls.
Returning full Room objects:
• Improves usability
• Reduces round-trip latency
• Simplifies frontend logic
In this implementation, full objects are returned because dataset size is small and usability is considered first.

Q4 -DELETE Operation Idempotency
DELETE /rooms/{id} is made idempotent
Behaviour:
• First request - delete resource (204 / 200)
• Further requests of the same kind → 404 Not Found.
• After initial deletion, the state of the system does not change.
This is compliant with the REST idempotency.
Also, deletion is prevented when the sensors remain connected, which guarantees the integrity of data and eliminates parentless resources.

Part 3 – Sensor Operations
Q5 - Impact of @Consumes(MediaType.APPLICATION_JSON)
POST Sensor endpoint takes up JSON explicitly.
If a client sends:
• text/plain
• application/xml
JAX-RS is sensitive to 415 Unsupported Media Type.
This creates very rigid API contracts and avoids serialization of invalid data.

Q6 – QueryParam vs PathParam for Filtering
Sensible filtering is applied as:
GET /api/v1/sensors?type=Temperature
Use of query parameters is better since:
• Filtering is not a fresh resource.
• Several filters can be used together.
• Pagination and sorting.
• Improves URI scalability
Path-based filtering would further unnecessarily increase endpoint hierarchy.

Part 4- Sub-Resource Locator Pattern.
Q7 – Architectural Benefits
Nested resources are used to implement sensor readings:
/api/v1/sensors/{id}/readings
A Sub-Resource Locator refers request processing to SensorReadingResource.
Benefits:
• Separation of concerns
• Cleaner controller design
• Better maintainability and testability.
• Representation of hierarchical domain relationships logically.
As well, POST reading updates the currentValue of the parent sensor ensuring consistency of the data.

Part 5 - Advanced Error Handling
Q8 – Why HTTP 422 Instead of 404
In the case of making a sensor where there is no roomID:
• Request syntax is valid
• Payload semantic validation is not successful.
Thus 422 Unprocessable Entity better suits than 404.
This enhances clarity of errors and debugging by clients.

Q9 - Stack Trace: Cybersecurity risks of exposing stack traces
Raw stack traces can be returned to indicate:
• Internal package structure
• File system paths
• Framework versions
• Database queries
Attackers may use this information.
This project deploys an ExceptionMapper on a global scale, which sends generic 500 responses, a secure API design.

Logging Strategy
The following are logged using a JAX-RS ContainerRequestFilter and a ContainerResponseFilter:
• HTTP Method
• Request URI
• Final response status
This is a better method than manual logging as it offers centralised monitoring of cross-cutting and follows best practices of enterprise API observability.

How to Run the Project
mvn clean package
mvn exec:java
Server starts at:
http://localhost:8080/api/v1

Sample cURL Commands
Create Room:
curl -X POST http://localhost:8080/api/v1/rooms \
-H "Content-Type: application/json" \
-d '{"id":"LAB-101","name":"Computer Lab","capacity":60}'
Filter Sensors:
curl http://localhost:8080/api/v1/sensors?type=Temperature
Add Reading:
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
-H "Content-Type: application/json" \
-d '{"id":"R-1","timestamp":1711015200000,"value":25.4}'

Conclusion
This project presents a REST API design of production style that implements:
• Resource hierarchy
• Stateless operations
• Idempotent methods
• Hypermedia discovery
• Organized exception mapping.
• Nested resources
• Logging filters
It is a standard practice of backend engineering in the industry. -->
