# Genesis Infrastructure Roadmap

This document outlines the future expansion plans for the `genesis-infra` module.

## 1. File Storage (Cloudinary)
**Goal**: Manage image and file uploads.
- **Library**: `com.cloudinary:cloudinary-http44`
- **Implementation**:
  - `FileStorageService` interface.
  - `CloudinaryService` implementation.
  - Configuration via `.env` (`CLOUDINARY_URL`).

## 2. Messaging (Event Bus)
**Goal**: Decouple modules via asynchronous events.
- **Candidates**: RabbitMQ or Kafka.
- **Use Case**:
  - User registration -> Send Welcome Email.
  - Data Import -> Trigger Analysis.

## 3. Caching
**Goal**: Improve read performance.
- **Target**: Redis.
- **Usage**: Cache `User` details or frequent `Workspace` queries.

## 4. Email Service
**Goal**: Send transactional emails.
- **Target**: SMTP (SendGrid or AWS SES).
- **Abstractions**: `EmailService` interface in Infra.

## 5. Security & Auditing
- Centralized Audit Logging (Who did what?).
- Security Utilities (Key management, Encryption).
