# LedgerSync: Autonomous AI 3-Way Invoice Reconciliation Engine

LedgerSync is an agentic invoice reconciliation engine built in Java for the Razorpay AI Builder 2026 program (Track 4: AI Finance Controller). It automates the extraction, arithmetic verification, and ledger cross-matching of unstructured vendor invoices against bank settlement records.

---

## Architecture & Workflow

1. **Multimodal Extraction:** Communicates with Vision-LLMs (Gemini 1.5 Flash) via native Java `HttpClient` to parse unstructured invoice images/PDFs into structured JSON.
2. **Deterministic Arithmetic Verification:** Decouples numeric calculations from the LLM, validating `Subtotal + Tax == Total` directly in Java to prevent arithmetic hallucinations.
3. **3-Way Cross Matching:** Executes matching logic between the parsed invoice data and the bank settlement ledger (`bank_records.csv`).
4. **Audit Flagging:** Flags discrepancies in taxes, duplicate bills, and unsettled amounts with audit-ready log outputs.

---

## Tech Stack

* **Language:** Java 17+
* **Build Tool:** Apache Maven
* **JSON Processing:** `org.json`
* **AI Model Endpoint:** Gemini 1.5 Flash REST API

---

## Project Structure

```text
ledgersync-java/
├── pom.xml
├── bank_records.csv
├── .gitignore
├── LICENSE
├── README.md
└── src/
    └── main/
        └── java/
            └── LedgerSyncApp.java
```

## Getting Started

### Prerequisites

- Java Development Kit (JDK 17 or higher)
- Apache Maven 3.8+

### Setup & Execution

1. Clone the repository:

```bash
git clone https://github.com/<YOUR_GITHUB_USERNAME>/ledgersync-ai-reconciliation.git
cd ledgersync-ai-reconciliation
```

2. Compile the project:

```bash
mvn clean compile
```

3. Run the application:

```bash
mvn exec:java
```
