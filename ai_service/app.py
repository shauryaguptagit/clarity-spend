from flask import Flask, request, jsonify
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.naive_bayes import MultinomialNB
from sklearn.pipeline import make_pipeline
import pandas as pd

# --- 1. Create and Train our "Smarter" Model ---

# We've expanded the dataset to reduce confusion.
# Now "Netflix" and "Spotify" have more "Subscriptions" examples.
data = {
    'description': [
        # Food
        'Starbucks Coffee', 'Zomato Order', 'Dominos Pizza', 'Uber Eats', 'McDonalds',
        'Restaurant Bill', 'Taco Bell', 'Subway', 'Pizza Hut',
        
        # Transport
        'UBER TRIP', 'Lyft Ride', 'Shell Gas', 'Chevron', 'Gas Station',
        'Parking Garage', 'Bus Fare', 'Train Ticket', 'Metro Card Recharge',
        
        # Subscriptions
        'Netflix Subscription', 'Spotify', 'Amazon Prime', 'Hulu', 'Gym Membership',
        'Disney+', 'Apple Music', 'New York Times', 'Monthly Subscription',
        
        # Groceries
        'Whole Foods', 'Groceries Store', 'Safeway', 'Kroger', 'Milk and Eggs',
        'Fresh Vegetables', 'Local Market', 'Farmers Market',
        
        # Utilities
        'Electricity Bill', 'Rent Payment', 'Apartment Bill', 'Water Bill', 'Internet Bill',
        'Monthly Rent', 'Phone Bill', 'WiFi Payment',
        
        # Shopping
        'Amazon Purchase', 'Myntra', 'Nike Store', 'H&M', 'Zara',
        'Shopping Mall', 'Bookstore', 'Electronics'
    ],
    'category': [
        # Food
        'Food', 'Food', 'Food', 'Food', 'Food',
        'Food', 'Food', 'Food', 'Food',
        
        # Transport
        'Transport', 'Transport', 'Transport', 'Transport', 'Transport',
        'Transport', 'Transport', 'Transport', 'Transport',
        
        # Subscriptions
        'Subscriptions', 'Subscriptions', 'Subscriptions', 'Subscriptions', 'Subscriptions',
        'Subscriptions', 'Subscriptions', 'Subscriptions', 'Subscriptions',
        
        # Groceries
        'Groceries', 'Groceries', 'Groceries', 'Groceries', 'Groceries',
        'Groceries', 'Groceries', 'Groceries',
        
        # Utilities
        'Utilities', 'Utilities', 'Utilities', 'Utilities', 'Utilities',
        'Utilities', 'Utilities', 'Utilities',
        
        # Shopping
        'Shopping', 'Shopping', 'Shopping', 'Shopping', 'Shopping',
        'Shopping', 'Shopping', 'Shopping'
    ]
}
df = pd.DataFrame(data)

# Create a machine learning pipeline
model = make_pipeline(
    TfidfVectorizer(),
    MultinomialNB()
)

# Train the model
print("Training new, smarter AI model...")
model.fit(df['description'], df['category'])
print("Smarter model trained successfully!")

# --- 2. Create the Flask App ---

app = Flask(__name__)

@app.route("/predict", methods=["POST"])
def predict():
    data = request.get_json()
    description = data.get("description")
    
    if not description:
        return jsonify({"error": "No description provided"}), 400

    # 3. Use our REAL model to predict
    try:
        prediction = model.predict([description])[0]
        return jsonify({"category": prediction})
    except Exception as e:
        print(f"Error during prediction: {e}")
        return jsonify({"category": "Miscellaneous"})

if __name__ == "__main__":
    print("Starting Flask server on port 5000...")
    app.run(port=5000, debug=True)