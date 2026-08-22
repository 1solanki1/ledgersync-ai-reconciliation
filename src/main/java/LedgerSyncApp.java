import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;
import org.json.JSONArray;
import org.json.JSONObject;

public class LedgerSyncApp {

    static class BankRecord {
        String txnId;
        String vendorName;
        double amount;
        String date;
        String status;

        BankRecord(String txnId, String vendorName, double amount, String date, String status) {
            this.txnId = txnId;
            this.vendorName = vendorName;
            this.amount = amount;
            this.date = date;
            this.status = status;
        }
    }

    static class InvoiceData {
        String invoiceNumber;
        String vendorName;
        double subtotal;
        double tax;
        double totalAmount;
        String date;

        InvoiceData(String invoiceNumber, String vendorName, double subtotal,
                    double tax, double totalAmount, String date) {
            this.invoiceNumber = invoiceNumber;
            this.vendorName = vendorName;
            this.subtotal = subtotal;
            this.tax = tax;
            this.totalAmount = totalAmount;
            this.date = date;
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("LedgerSync - Invoice Reconciliation");
        System.out.println("Razorpay AI Builder 2026 | Track 4\n");

        List<BankRecord> bankRecords = loadBankRecords("bank_records.csv");
        System.out.println("Loaded " + bankRecords.size() + " bank records.\n");

        System.out.print("Gemini API key (press Enter for demo): ");
        String apiKey = scanner.nextLine().trim();

        InvoiceData invoice;
        if (apiKey.isEmpty()) {
            System.out.println("Using demo invoice data.\n");
            invoice = new InvoiceData(
                    "INV-2026-089", "Google India", 1271.18,
                    228.82, 1500.00, "2026-05-10");
        } else {
            System.out.print("Invoice image path: ");
            String imagePath = scanner.nextLine().trim();
            invoice = extractInvoiceWithGemini(apiKey, imagePath);
        }

        printInvoice(invoice);

        double calculatedTotal = invoice.subtotal + invoice.tax;
        boolean mathOk = Math.abs(calculatedTotal - invoice.totalAmount) < 0.05;

        System.out.println("\nNumber check");
        System.out.printf("%.2f + %.2f = %.2f%n",
                invoice.subtotal, invoice.tax, calculatedTotal);
        System.out.println(mathOk ? "Invoice calculation: OK" : "Invoice calculation: MISMATCH");

        BankRecord match = findBankRecord(bankRecords, invoice.vendorName);
        boolean amountOk = match != null
                && Math.abs(match.amount - invoice.totalAmount) < 0.05;

        System.out.println("\nBank check");
        if (match == null) {
            System.out.println("No bank record found for this vendor.");
        } else {
            System.out.printf("Found transaction %s for INR %.2f (%s)%n",
                    match.txnId, match.amount, match.status);
            System.out.println(amountOk ? "Amount check: OK" : "Amount check: MISMATCH");
        }

        System.out.println("\nResult");
        if (mathOk && amountOk) {
            System.out.println("RECONCILED");
            System.out.printf("Transaction: %s | Date: %s%n", match.txnId, match.date);
        } else {
            System.out.println("MANUAL REVIEW NEEDED");
        }

        scanner.close();
    }

    private static void printInvoice(InvoiceData invoice) {
        System.out.println("Invoice");
        System.out.println("Number : " + invoice.invoiceNumber);
        System.out.println("Vendor : " + invoice.vendorName);
        System.out.printf("Subtotal: INR %.2f%n", invoice.subtotal);
        System.out.printf("Tax     : INR %.2f%n", invoice.tax);
        System.out.printf("Total   : INR %.2f%n", invoice.totalAmount);
        System.out.println("Date    : " + invoice.date);
    }

    private static BankRecord findBankRecord(List<BankRecord> records, String vendor) {
        String invoiceVendor = vendor.toLowerCase();
        int length = Math.min(5, invoiceVendor.length());
        String prefix = invoiceVendor.substring(0, length);

        for (BankRecord record : records) {
            String bankVendor = record.vendorName.toLowerCase();
            int bankLength = Math.min(5, bankVendor.length());
            String bankPrefix = bankVendor.substring(0, bankLength);

            if (bankVendor.contains(prefix) || invoiceVendor.contains(bankPrefix)) {
                return record;
            }
        }
        return null;
    }

    private static List<BankRecord> loadBankRecords(String filePath) {
        List<BankRecord> records = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            br.readLine();
            String line;

            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length >= 5) {
                    records.add(new BankRecord(
                            values[0].trim(),
                            values[1].trim(),
                            Double.parseDouble(values[2].trim()),
                            values[3].trim(),
                            values[4].trim()));
                }
            }
        } catch (Exception e) {
            System.out.println("Could not read bank_records.csv, using demo record.");
            records.add(new BankRecord(
                    "TXN_101", "Google India", 1500.00, "2026-05-10", "Settled"));
        }

        return records;
    }

    private static InvoiceData extractInvoiceWithGemini(String apiKey, String imagePath) {
        try {
            byte[] imageBytes = Files.readAllBytes(Path.of(imagePath));
            String image = Base64.getEncoder().encodeToString(imageBytes);

            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;
            String prompt = "Read this invoice and return JSON only with these keys: "
                    + "invoice_number, vendor_name, subtotal, tax, total_amount, date. "
                    + "Use YYYY-MM-DD for the date.";

            JSONObject body = new JSONObject()
                    .put("contents", new JSONArray()
                            .put(new JSONObject().put("parts", new JSONArray()
                                    .put(new JSONObject().put("text", prompt))
                                    .put(new JSONObject().put("inline_data", new JSONObject()
                                            .put("mime_type", "image/jpeg")
                                            .put("data", image))))));

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = client.send(
                    request, HttpResponse.BodyHandlers.ofString());

            JSONObject result = new JSONObject(response.body());
            String text = result.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();

            JSONObject data = new JSONObject(text);

            return new InvoiceData(
                    data.optString("invoice_number", "INV-UNKNOWN"),
                    data.optString("vendor_name", "Unknown"),
                    data.optDouble("subtotal", 0),
                    data.optDouble("tax", 0),
                    data.optDouble("total_amount", 0),
                    data.optString("date", "Unknown"));
        } catch (Exception e) {
            System.out.println("Could not extract the invoice: " + e.getMessage());
            System.out.println("Falling back to the demo invoice.\n");
            return new InvoiceData(
                    "INV-2026-089", "Google India", 1271.18,
                    228.82, 1500.00, "2026-05-10");
        }
    }
}
