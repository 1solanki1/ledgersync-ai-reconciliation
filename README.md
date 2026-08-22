# LedgerSync – Invoice Reconciliation in Java

LedgerSync is a small Java project for checking vendor invoices against bank settlement records. I built it for the Razorpay AI Builder 2026 program (Track 4: AI Finance Controller).

The main idea is simple: use an AI model to read the useful fields from an invoice, then do the actual checks in Java instead of asking the model to decide whether the numbers are correct.

## What it does

The current version has three main steps:

1. Reads invoice details such as invoice number, vendor, subtotal, tax, total and date.
2. Checks `subtotal + tax = total` in Java.
3. Looks for the vendor in `bank_records.csv` and compares the invoice amount with the settled amount.

If the checks pass, the invoice is marked as reconciled. Otherwise it is sent for manual review.

## Where AI is used

When an API key is provided, the program sends an invoice image to Gemini and asks it to return the relevant fields as JSON.

The calculations and reconciliation rules are kept in Java. This is intentional because arithmetic and matching should not depend on an LLM response.

For a quick demo, pressing Enter instead of entering an API key uses a sample invoice already included in the program.

## Tech used

- Java 17+
- Maven
- `org.json`
- Gemini REST API
- CSV file for sample bank records

## Project structure

```text
ledgersync-ai-reconciliation/
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

## Running it

### Requirements

- JDK 17 or newer
- Maven 3.8+

### Build

```bash
mvn clean compile
```

### Run

```bash
mvn exec:java
```

The program first loads the sample bank records. You can then either enter a Gemini API key and an invoice image path, or press Enter to run the built-in demo.

## Example

The demo invoice is for `Google India` with:

- Subtotal: INR 1271.18
- Tax: INR 228.82
- Total: INR 1500.00

The Java calculation checks that the first two values add up to the invoice total and then compares the total with the matching bank record.

## Current limitations

This is a project/demo implementation rather than a production accounting system.

- The bank data is read from a simple CSV file.
- Vendor matching is currently basic and can be improved.
- The Gemini request currently expects an image input.
- Invoice extraction can still need human verification.
- Authentication, database storage and a proper audit database are not included yet.

## AI use during development

AI tools were used during development for brainstorming, debugging, explaining API usage and improving parts of the implementation. I reviewed and adapted the resulting code and kept the core reconciliation checks deterministic in Java.

## Future improvements

- Better vendor and invoice matching
- Support for PDF invoices
- More invoice validation rules
- Database-backed records
- A small web interface for uploading invoices
- Better error handling and audit history
