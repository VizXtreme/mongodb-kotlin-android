# MongoDB Kotlin Android Client

A lightweight, robust Kotlin Android application designed to connect to, access, and modify MongoDB databases directly using MongoDB connection strings—including Atlas `mongodb+srv://` URIs.

## Key Features

- **Direct MongoDB Atlas Connectivity (`mongodb+srv://`)**:
  - Solves the long-standing Android issue where `javax.naming` (JNDI) is absent from the Android runtime.
  - Implements a custom dual-layer DNS engine via `com.mongodb.spi.dns.DnsClient`:
    1. **DNS-over-HTTPS (DoH)** via Google DNS and Cloudflare DNS to bypass carrier UDP port 53 filtering.
    2. **Embedded Pure-Java DNS** via `dnsjava` for local/offline DNS SRV & TXT lookups.
- **CRUD Operations**:
  - **Find / Query**: Execute custom JSON queries (e.g. `{"status": "active"}`) with document limits and count metrics.
  - **Insert**: Insert single or complex JSON documents with automatic `_id` extraction.
  - **Update**: Update single or multiple documents with automatic `$set` wrapping for convenience.
  - **Delete**: Delete single or multiple documents based on custom filter criteria.
- **Schema & Collection Exploration**:
  - Dynamic discovery of databases and collections.
  - Live ping test and server version inspection.
- **Optimized Skeleton UI**:
  - High-density, minimal monochrome layout built with Jetpack Compose.
  - No bloated styling, animations, or flashy colors—focused strictly on engineering utility, stability, and speed.
  - Real-time system log console for inspecting query execution times, network events, and errors.

## MongoDB Atlas Connection String Format

```text
mongodb+srv://<username>:<password>@cluster0.ywgy3ll.mongodb.net/?appName=Cluster0
```

> **Atlas Setup Note**:
> Make sure your MongoDB Atlas cluster has network access enabled for mobile devices (Atlas Dashboard -> **Network Access** -> **Add IP Address** -> `0.0.0.0/0` for testing).

## Architecture

- **`AndroidDnsClient`**: Implements MongoDB SPI `DnsClient` using DoH and `dnsjava` to parse SRV and TXT records without `javax.naming`.
- **`MongoManager`**: Coroutine-based repository handling connection pooling, ping, database listing, and CRUD execution via `Dispatchers.IO`.
- **`MongoViewModel`**: State management using Kotlin `StateFlow` and lifecycle-aware coroutines.
- **`MongoScreen`**: Minimal skeleton UI with high-density inspection panels.

## Building the App

This project is configured with GitHub Actions CI. Every push to `main` builds the debug APK and publishes it under Workflow Artifacts.
