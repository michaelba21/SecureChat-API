class SecureChatPollingClient {
    constructor(chatRoomId, baseUrl = '/api/chatrooms') {
        this.chatRoomId = chatRoomId;
        this.baseUrl = baseUrl;
        this.lastTimestamp = null; // Track last received message timestamp
        this.isPolling = false; 
        this.pollInterval = 5000; // Poll every 5 seconds
    }

    async startPolling() {
        this.isPolling = true; // Set polling to active
        
        // Get initial latest timestamp from server
        try {
            this.lastTimestamp = await this.getLatestTimestamp();
            console.log('Starting polling from:', this.lastTimestamp);
        } catch (error) {
            console.error('Failed to get initial timestamp:', error);
            // Fallback to current time if server call fails
            this.lastTimestamp = new Date().toISOString();
        }
        
        this.poll(); // Start the polling loop
    }

    stopPolling() {
        this.isPolling = false; // Stop the polling loop
    }

    async poll() {
        if (!this.isPolling) return; // Exit if polling stopped

        try {
            // Build query parameters for polling request
            const params = new URLSearchParams();
            if (this.lastTimestamp) {
                params.append('since', this.lastTimestamp); // Get messages after last timestamp
            }
            params.append('limit', '50'); // Limit to 50 messages per poll

            // Make polling request to server
            const response = await fetch(`${this.baseUrl}/${this.chatRoomId}/messages/poll?${params}`, {
                headers: {
                    'Authorization': `Bearer ${this.getAuthToken()}`, // Include auth token
                    'Content-Type': 'application/json'
                }
            });

            if (response.ok) {
                const messages = await response.json(); // Parse new messages
                if (messages.length > 0) {
                    console.log(`Received ${messages.length} new messages`);
                    this.displayMessages(messages); // Display messages in UI
                    
                    // Update timestamp to newest message (assuming chronological order)
                    this.lastTimestamp = messages[0].timestamp;
                }
            } else {
                console.error('Polling request failed:', response.status);
            }
        } catch (error) {
            console.error('Polling error:', error);
        }

        // Schedule next poll if still active
        if (this.isPolling) {
            setTimeout(() => this.poll(), this.pollInterval);
        }
    }

    async getLatestTimestamp() {
        // Fetch the most recent message timestamp from server
        const response = await fetch(`${this.baseUrl}/${this.chatRoomId}/messages/latest-timestamp`, {
            headers: {
                'Authorization': `Bearer ${this.getAuthToken()}`
            }
        });
        
        if (response.ok) {
            return await response.text(); // Return timestamp string
        }
        throw new Error('Failed to get latest timestamp'); // Throw on failure
    }

    displayMessages(messages) {
        // Update UI with all new messages
        messages.forEach(message => {
            this.addMessageToChat(message);
        });
    }

    addMessageToChat(message) {
        // Create and append individual message element to chat UI
        const chatContainer = document.getElementById('chat-messages');
        const messageElement = document.createElement('div');
        messageElement.className = 'message';
        messageElement.innerHTML = `
            <strong>${message.username}:</strong>
            <span>${message.content}</span>
            <small>${new Date(message.timestamp).toLocaleTimeString()}</small>
        `;
        chatContainer.appendChild(messageElement);
    }

    getAuthToken() {
        // Retrieve authentication token from storage
        return localStorage.getItem('authToken');
    }

    // Send a new message to the chat room
    async sendMessage(content, messageType = 'TEXT') {
        const response = await fetch(`${this.baseUrl}/${this.chatRoomId}/messages`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${this.getAuthToken()}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                userId: this.getCurrentUserId(), // Get current user ID
                content: content, // Message content
                messageType: messageType // TEXT, IMAGE, etc.
            })
        });
        
        return response.ok; // Return true if successful
    }
}