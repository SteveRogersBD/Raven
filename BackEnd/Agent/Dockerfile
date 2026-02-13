
# Use an official Python runtime as a parent image
FROM python:3.12-slim

# Set the working directory in the container
WORKDIR /app

# Copy the requirements file into the container at /app
COPY requirements.txt .

# Install system dependencies: ffmpeg for video/audio and nodejs as JS runtime for yt-dlp
RUN apt-get update && apt-get install -y \
    ffmpeg \
    nodejs \
    && rm -rf /var/lib/apt/lists/*

# Install any needed packages specified in requirements.txt
RUN pip install --no-cache-dir -r requirements.txt

# Copy the rest of the application code
COPY . .

# Expose the port the app runs on
EXPOSE 8080

# Define the command to run the application
# We use a shell-form CMD to allow environment variable expansion for $PORT
CMD uvicorn agent_server:app --host 0.0.0.0 --port ${PORT:-8080}
