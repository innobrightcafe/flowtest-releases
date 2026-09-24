# Use official Node.js lightweight image
FROM node:20-alpine

# Set working directory
WORKDIR /usr/src/app

# Copy package files
COPY package*.json ./

# Install production dependencies
RUN npm ci --only=production

# Copy application source code & webapp landing pages
COPY server.js ./
COPY webapp ./webapp
COPY .build-outputs* ./.build-outputs/
COPY FlowTest.apk* ./FlowTest.apk

# Create directories for runtime data, webhook logs, and dynamic APK uploads
RUN mkdir -p .build-outputs webapp/download data

# Cloud Run injects PORT environment variable (default 8080)
EXPOSE 8080

# Start server
CMD ["node", "server.js"]
