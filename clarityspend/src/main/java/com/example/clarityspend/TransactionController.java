package com.example.clarityspend;

// Make sure to add all these new imports
import com.example.clarityspend.security.JwtUtil;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "*") 
public class TransactionController {

    @Autowired
    private TransactionRepository transactionRepository;

    // We need the UserRepository to find the logged-in user
    @Autowired
    private UserRepository userRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final String pythonApiUrl = "http://localhost:5000/predict";

    // --- Helper function to get the current user ---
    private User getLoggedInUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // --- GET ALL TRANSACTIONS (FOR LOGGED-IN USER) ---
    @GetMapping
    public List<Transaction> getAllTransactions(@AuthenticationPrincipal UserDetails userDetails) {
        // 1. Get the logged-in user
        User user = getLoggedInUser(userDetails);
        
        // 2. Find transactions BY THAT USER'S ID ONLY
        return transactionRepository.findByUserId(user.getId());
    }

    // --- POST A SINGLE TRANSACTION (FOR LOGGED-IN USER) ---
    @PostMapping
    public Transaction createTransaction(
            @RequestBody Transaction transaction,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getLoggedInUser(userDetails);

        // 1. Link this transaction to the logged-in user
        transaction.setUser(user);

        // 2. Call AI service to get category
        String category = getCategoryFromAI(transaction.getDescription());
        transaction.setCategory(category);
        
        return transactionRepository.save(transaction);
    }

    // --- PUT (UPDATE) A TRANSACTION (FOR LOGGED-IN USER) ---
    @PutMapping("/{id}")
    public Transaction updateTransactionCategory(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getLoggedInUser(userDetails);
        
        // 1. Find the transaction
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found with id: " + id));

        // 2. SECURITY CHECK: Make sure this transaction belongs to the logged-in user
        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized: You do not own this transaction");
        }

        // 3. Update the category
        String newCategory = payload.get("category");
        transaction.setCategory(newCategory);
        
        return transactionRepository.save(transaction);
    }

    // --- POST A CSV FILE (FOR LOGGED-IN USER) ---
    @PostMapping("/upload-csv")
    public ResponseEntity<Map<String, Object>> uploadCsv(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getLoggedInUser(userDetails);
        
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "File is empty"));
        }

        List<Transaction> transactionsToSave = new ArrayList<>();

        try (Reader reader = new InputStreamReader(file.getInputStream())) {
            CSVReader csvReader = new CSVReaderBuilder(reader).withSkipLines(0).build(); 
            
            String[] line;
            while ((line = csvReader.readNext()) != null) {
                if (line.length < 2) continue; 
                
                String description = line[0];
                double amount = Double.parseDouble(line[1].trim()); 
                
                Transaction tx = new Transaction();
                tx.setDescription(description);
                tx.setAmount(amount);
                
                // 1. Link this transaction to the logged-in user
                tx.setUser(user);
                
                // 2. Get AI category
                String category = getCategoryFromAI(description);
                tx.setCategory(category);
                
                transactionsToSave.add(tx);
            }

            transactionRepository.saveAll(transactionsToSave);
            
            return ResponseEntity.ok(Map.of(
                "message", "File processed successfully",
                "transactionsUploaded", transactionsToSave.size()
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("message", "Error processing file: " + e.getMessage()));
        }
    }


    // --- Private Helper Method to Call Python API ---
    private String getCategoryFromAI(String description) {
        // ... (this method is unchanged) ...
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("description", description);
        try {
            CategoryResponse response = restTemplate.postForObject(pythonApiUrl, requestBody, CategoryResponse.class);
            if (response != null && response.getCategory() != null) {
                return response.getCategory();
            }
        } catch (Exception e) {
            System.err.println("Error calling Python API: " + e.getMessage());
        }
        return "Miscellaneous";
    }

    // --- Private Helper Class for JSON Deserialization ---
    private static class CategoryResponse {
        // ... (this class is unchanged) ...
        private String category;
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
    }
}