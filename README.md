# ClaritySpend: Intelligent Expense Categorizer

A full-stack, microservice-based web application that automatically categorizes financial transactions from a CSV file and provides a dashboard for review.

---

## 🚀 Features

* **Secure Authentication:** Full user registration and login system using Spring Security and JWT.
* **AI-Powered Categorization:** A Python/Flask microservice with a `scikit-learn` model that predicts the category for each new transaction.
* **Bulk CSV Upload:** Users can upload a 2-column CSV of their bank transactions for instant, automated processing.
* **Review & Correct Dashboard:** A JavaScript frontend that displays all transactions and allows users to manually "review and correct" any category, with changes saved back to the database.

## 🏛️ Architecture (Microservice)

This project is built using a microservice architecture to separate the core application logic from the AI model.

1.  **Core Backend (Java/Spring Boot):** A secure REST API that handles user accounts, data storage (in Postgres/SQL), and security.
2.  **AI Service (Python/Flask):** A separate, lightweight service whose only job is to receive a transaction description (e.g., "Starbucks") and return a predicted category (e.g., "Food").
3.  **Frontend (JavaScript):** A single-page application that communicates with the secure Java backend API.



## 💻 Tech Stack

| Category | Technology |
| :--- | :--- |
| **Backend** | Java 17, Spring Boot, Spring Security (JWT) |
| **AI Service** | Python, Flask, scikit-learn, Pandas |
| **Database** | H2 (for development), PostgreSQL (for production) |
| **Frontend** | Vanilla JavaScript (ES6+), HTML5, Tailwind CSS |
| **Libraries** | `opencsv` (for Java), `jjwt` (for Java) |

## ⚙️ How to Run Locally

1.  **Clone the repo.**
2.  **Run the Python AI Service:**
    ```bash
    cd ai_service
    pip install -r requirements.txt
    python app.py
    ```
3.  **Run the Java Backend:**
    ```bash
    cd clarityspend
    ./mvnw spring-boot:run
    ```
4.  **Open the Frontend:**
    * Open the `clarityspend_frontend/index.html` file in your browser.
