# Autonomous Robot Perception & Mapping System Simulation

## Overview
This project implements a **Java-based simulation** of an autonomous vacuum robot perception and mapping system, designed using **microservices architecture principles**.  
The system simulates how an autonomous robot ingests sensor data, processes it across independent services, and incrementally builds an internal representation of its environment.

The architecture emphasizes **modularity**, **scalability**, and **clear separation of concerns**, closely reflecting real-world distributed and robotics software systems.

---

## System Architecture
The system is composed of multiple **loosely coupled microservices**, where each service is responsible for a specific functional domain, such as:

- Sensor data ingestion  
- Perception and preprocessing  
- Mapping and state management  
- Control and coordination  

Each microservice runs independently and communicates through **well-defined data contracts**, allowing services to evolve, scale, and be tested independently.

---

## Sensor Simulation & Input Data
Sensor inputs are simulated using **JSON-based data streams**, representing common robotic sensors such as distance, position, and environmental measurements.

- Sensor data is generated at **configurable rates**, simulating real-time behavior  
- The simulation layer abstracts the data source, allowing the system to **accept real sensor data** with minimal changes  
- By adapting the ingestion layer, the system can be connected to **physical sensors** or external hardware sources without modifying the core processing logic  

---

## Data Flow & Processing Pipeline
1. Sensor microservices receive and validate incoming JSON events  
2. Perception services process raw sensor data and extract spatial information  
3. Mapping services aggregate processed data and update the internal environment map  
4. System state is maintained across time, reflecting robot movement and environment changes  

The pipeline is **deterministic and reproducible**, enabling systematic validation and debugging.

---

## Output
The primary output of the system is an **incrementally updated environment map**, representing:

- Obstacles  
- Free space  
- Robot position  

Additional outputs include structured logs and state snapshots for **debugging**, **validation**, and **analysis**.

---

## Execution & Deployment
The system is designed to run in a **Linux environment** and is containerized using **Docker** to ensure consistent execution.

- Each microservice can be started independently  
- The full system can be executed as a complete pipeline or partially for focused testing  
- Docker-based deployment ensures reproducible runs across environments  

---

## Engineering Focus
This project focuses on:

- Microservices-based system design  
- Real-time data simulation using JSON event streams  
- Clear service boundaries and interface contracts  
- Maintainability, extensibility, and testability  
- Distributed systems and robotics-oriented software challenges  

---

## Technologies Used
- **Language:** Java  
- **Architecture:** Microservices  
- **Data Format:** JSON  
- **Environment:** Linux  
- **Tools:** Docker, Git  
