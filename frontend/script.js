// Wait for the DOM to be fully loaded before running the script
document.addEventListener('DOMContentLoaded', () => {

    // --- CONFIGURATION ---
    const AUTH_API_URL = 'http://localhost:8080/api/auth';
    const TX_API_URL = 'http://localhost:8080/api/transactions';

    // --- DOM ELEMENTS ---
    // Auth
    const authContainer = document.getElementById('auth-container');
    const loginForm = document.getElementById('login-form');
    const registerForm = document.getElementById('register-form');
    const loginStatus = document.getElementById('login-status');
    const registerStatus = document.getElementById('register-status');

    // Dashboard
    const dashboardContainer = document.getElementById('dashboard-container');
    const logoutButton = document.getElementById('logout-button');
    const addForm = document.getElementById('add-transaction-form');
    const reviewForm = document.getElementById('review-form');
    const listPlaceholder = document.getElementById('list-placeholder');
    const csvForm = document.getElementById('upload-csv-form');
    const csvFile = document.getElementById('csv-file');
    const uploadStatus = document.getElementById('upload-status');
    const chartCtx = document.getElementById('spending-chart')?.getContext('2d');
    
    let spendingChart;

    // --- STATE ---
    let jwtToken = null;

    // --- HELPER FUNCTIONS ---
    
    /**
     * Saves the JWT token to localStorage
     */
    function saveToken(token) {
        jwtToken = token;
        localStorage.setItem('claritySpendToken', token);
    }

    /**
     * Loads the JWT token from localStorage
     */
    function loadToken() {
        const token = localStorage.getItem('claritySpendToken');
        if (token) {
            jwtToken = token;
            return true;
        }
        return false;
    }

    /**
     * Clears the token and logs the user out
     */
    function logout() {
        jwtToken = null;
        localStorage.removeItem('claritySpendToken');
        showLoginView();
    }

    /**
     * Creates the Authorization header for API requests
     */
    function getAuthHeaders() {
        if (!jwtToken) throw new Error("No token found");
        return {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${jwtToken}`
        };
    }
    
    /**
     * Shows the dashboard and hides the login view
     */
    function showDashboardView() {
        authContainer.classList.add('hidden');
        dashboardContainer.classList.remove('hidden');
        // Load all dashboard data
        fetchTransactions();
    }

    /**
     * Shows the login view and hides the dashboard
     */
    function showLoginView() {
        dashboardContainer.classList.add('hidden');
        authContainer.classList.remove('hidden');
    }

    // --- API FUNCTIONS ---

    async function fetchTransactions() {
        try {
            const response = await fetch(TX_API_URL, {
                method: 'GET',
                headers: getAuthHeaders() // Send token
            });
            if (!response.ok) {
                if(response.status === 401 || response.status === 403) logout(); // Token is bad, log out
                throw new Error(`Network response was not ok`);
            }
            const transactions = await response.json();
            renderReviewList(transactions);
            renderChart(transactions);
        } catch (error) {
            console.error('Error fetching transactions:', error);
            reviewForm.innerHTML = '<p class="py-3 text-red-500 text-center">Error loading transactions. Is the server running?</p>';
        }
    }

    async function addTransaction(description, amount) {
        const transactionData = { description, amount };
        try {
            const response = await fetch(TX_API_URL, {
                method: 'POST',
                headers: getAuthHeaders(), // Send token
                body: JSON.stringify(transactionData),
            });
            if (!response.ok) throw new Error(`Failed to create transaction`);
            addForm.reset();
            await fetchTransactions();
        } catch (error) {
            console.error('Error adding transaction:', error);
            alert('Error adding transaction. Are both servers running?');
        }
    }

    async function updateCategory(id, newCategory, button) {
        button.disabled = true;
        button.textContent = '...';
        try {
            const response = await fetch(`${TX_API_URL}/${id}`, {
                method: 'PUT',
                headers: getAuthHeaders(), // Send token
                body: JSON.stringify({ category: newCategory })
            });
            if (!response.ok) throw new Error('Failed to update category');
            await fetchTransactions(); 
        } catch (error) {
            console.error('Error updating category:', error);
            alert('Error updating category.');
            button.disabled = false;
            button.textContent = 'Save';
        }
    }

    async function handleCsvUpload(event) {
        event.preventDefault();
        const file = csvFile.files[0];
        if (!file) {
            uploadStatus.textContent = 'Please select a file.';
            uploadStatus.className = 'status-text text-red-600';
            return;
        }

        const formData = new FormData();
        formData.append('file', file);
        
        const button = event.target.querySelector('button');
        button.disabled = true;
        button.textContent = 'Uploading...';
        uploadStatus.textContent = 'Processing...';
        uploadStatus.className = 'status-text text-indigo-600';

        try {
            // FormData uses a different header, so we build it manually
            const headers = {
                'Authorization': `Bearer ${jwtToken}`
                // DO NOT set 'Content-Type', browser does it for FormData
            };
            
            const response = await fetch(`${TX_API_URL}/upload-csv`, {
                method: 'POST',
                headers: headers, // Send token
                body: formData,
            });

            const result = await response.json();
            if (!response.ok) throw new Error(result.message || 'CSV upload failed');

            uploadStatus.textContent = `Success! ${result.transactionsUploaded} transactions uploaded.`;
            uploadStatus.className = 'status-text text-green-700';
            csvForm.reset();
            await fetchTransactions(); 
        } catch (error) {
            console.error('Error uploading CSV:', error);
            uploadStatus.textContent = `Error: ${error.message}`;
            uploadStatus.className = 'status-text text-red-600';
        } finally {
            button.disabled = false;
            button.textContent = 'Upload & Auto-Categorize';
        }
    }
    
    // --- RENDERING FUNCTIONS (Unchanged from before) ---
    function renderReviewList(transactions) {
        reviewForm.innerHTML = ''; 
        if (transactions.length === 0) {
            reviewForm.appendChild(listPlaceholder);
            return;
        }
        transactions.slice().reverse().forEach(tx => {
            const item = document.createElement('div');
            item.className = "py-3 grid grid-cols-3 gap-2 items-center";
            item.innerHTML = `
                <div class="col-span-3">
                    <p class="font-medium text-slate-800">${tx.description}</p>
                    <p class="text-sm text-slate-500">$${tx.amount.toFixed(2)}</p>
                </div>
                <div class="col-span-2">
                    <input type="text" value="${tx.category || 'Uncategorized'}" id="category-${tx.id}" 
                           class="category-input form-input text-sm p-2">
                </div>
                <button data-id="${tx.id}" 
                        class="update-btn bg-indigo-600 text-white text-sm font-medium py-2 px-3 rounded-lg hover:bg-indigo-700">
                    Save
                </button>
            `;
            reviewForm.appendChild(item);
        });
    }

    function renderChart(transactions) {
        if (!chartCtx) return; 
        const categoryTotals = {}; 
        transactions.forEach(tx => {
            const category = tx.category || 'Uncategorized';
            const amount = tx.amount || 0;
            categoryTotals[category] = (categoryTotals[category] || 0) + amount;
        });
        const labels = Object.keys(categoryTotals);
        const data = Object.values(categoryTotals);
        if (spendingChart) spendingChart.destroy();
        spendingChart = new Chart(chartCtx, {
            type: 'doughnut',
            data: {
                labels: labels,
                datasets: [{
                    label: 'Amount Spent',
                    data: data,
                    backgroundColor: ['#312e81', '#3730a3', '#4338ca', '#4f46e5', '#6366f1', '#818cf8', '#a5b4fc'],
                    borderColor: '#ffffff',
                    borderWidth: 2,
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { position: 'bottom', labels: { padding: 20, font: { family: 'Inter', size: 14 } } } }
            }
        });
    }
    
    // --- AUTH EVENT LISTENERS ---

    loginForm.addEventListener('submit', async (event) => {
        event.preventDefault();
        const username = document.getElementById('login-username').value;
        const password = document.getElementById('login-password').value;
        
        try {
            const response = await fetch(`${AUTH_API_URL}/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, password })
            });

            if (!response.ok) {
                loginStatus.textContent = "Incorrect username or password";
                loginStatus.className = "status-text text-red-600";
                throw new Error('Login failed');
            }

            const data = await response.json();
            saveToken(data.jwt); // Save the token
            showDashboardView(); // Show the app

        } catch (error) {
            console.error('Login error:', error);
        }
    });

    registerForm.addEventListener('submit', async (event) => {
        event.preventDefault();
        const username = document.getElementById('register-username').value;
        const password = document.getElementById('register-password').value;
        
        try {
            const response = await fetch(`${AUTH_API_URL}/register`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, password })
            });
            
            if (!response.ok) {
                registerStatus.textContent = "Username may be taken";
                registerStatus.className = "status-text text-red-600";
                throw new Error('Registration failed');
            }
            
            registerStatus.textContent = "Registration successful! Please log in.";
            registerStatus.className = "status-text text-green-700";
            registerForm.reset();

        } catch (error) {
            console.error('Register error:', error);
        }
    });
    
    logoutButton.addEventListener('click', logout);

    // --- DASHBOARD EVENT LISTENERS (Unchanged) ---
    addForm.addEventListener('submit', async (event) => {
        event.preventDefault(); 
        const description = document.getElementById('description').value;
        const amount = parseFloat(document.getElementById('amount').value);
        await addTransaction(description, amount);
    });

    reviewForm.addEventListener('click', (event) => {
        if (event.target.classList.contains('update-btn')) {
            event.preventDefault(); 
            const id = event.target.dataset.id;
            const input = document.getElementById(`category-${id}`);
            updateCategory(id, newCategory, event.target);
        }
    });

    csvForm.addEventListener('submit', handleCsvUpload);

    // --- INITIALIZATION ---
    if (loadToken()) {
        // If we found a token, show the dashboard
        showDashboardView();
    } else {
        // Otherwise, show the login page
        showLoginView();
    }
});