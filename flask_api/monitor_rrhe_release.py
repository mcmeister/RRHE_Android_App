#! /home/mcmeister/myflaskenv/bin/python

import os
import time
import shutil
from shutil import move
from http.server import SimpleHTTPRequestHandler
from socketserver import TCPServer
import socketserver
import threading
import requests
import socket
import logging
from google.oauth2 import service_account
from google.auth.transport.requests import Request

# Configuration
watch_folder = '/home/mcmeister/StudioProjects/RRHE_Android_App/app/release/'
version_control_folder = '/home/mcmeister/Documents/RRHE_App_Dev/Version Control/'
rrhe_apk = 'RRHE.apk'
app_release_apk = 'app-release.apk'
version_start = 1.0
host_ip = '192.168.1.200'
port = 8080
external_ip = '183.88.230.187:55005'  # External IP for release server
log_file = '/home/mcmeister/rrhe_monitor.log'  # Update the path to a directory you have write access to

# FCM Configuration
service_account_file = '/home/mcmeister/Documents/RRHE_App_Dev/rrhe-2f654-e0f933ba832f.json'  # Path to your service account JSON file
fcm_endpoint = 'https://fcm.googleapis.com/v1/projects/rrhe-2f654/messages:send'  # Replace YOUR_PROJECT_ID
fcm_topic = 'projects/rrhe-2f654/topics/new_apk_available'  # Replace YOUR_PROJECT_ID

current_server = None  # Global variable to keep track of the current server
last_version_folder = None  # Global variable to track the last processed version folder

# Configure logging
logging.basicConfig(
    filename=log_file,
    level=logging.DEBUG,
    format='%(asctime)s - %(levelname)s - %(message)s',
)

# Function to generate the next version number
def get_next_version():
    logging.debug("Generating the next version number...")
    existing_versions = [
        float(d.split('_')[1])
        for d in os.listdir(version_control_folder)
        if d.startswith('RRHE_')
    ]
    if existing_versions:
        next_version = round(max(existing_versions) + 0.1, 1)
        logging.debug(f"Next version determined: {next_version}")
        return next_version
    else:
        logging.debug(f"No existing versions found. Starting with version {version_start}")
        return version_start

# Function to copy the content of the release folder to version control
def copy_to_version_control():
    next_version = get_next_version()
    new_version_folder = f"RRHE_{next_version}"
    destination_path = os.path.join(version_control_folder, new_version_folder)

    logging.debug(f"Creating a new versioned folder: {destination_path}")
    
    # Copy the entire release folder to the new versioned folder
    shutil.copytree(watch_folder, destination_path)
    logging.debug(f"Copied contents of {watch_folder} to {destination_path}")

    return new_version_folder

# Function to check if a port is already in use
def is_port_in_use(port):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        in_use = s.connect_ex(('localhost', port)) == 0
    logging.debug(f"Port {port} in use: {in_use}")
    return in_use

# Custom TCPServer class to set SO_REUSEADDR option
class ReusableTCPServer(TCPServer):
    allow_reuse_address = True

def start_hosting():
    global current_server
    logging.debug("Checking if RRHE.apk is available for hosting...")

    if os.path.exists(os.path.join(watch_folder, rrhe_apk)):
        logging.debug(f"Found {rrhe_apk}. Starting to host...")

        # Ensure the previous server is properly shut down
        if current_server:
            logging.debug("Stopping the existing server before rehosting...")
            current_server.shutdown()
            current_server.server_close()
            current_server = None
            time.sleep(1)  # Ensure the port is fully released before trying to bind again

        # Attempt to start the server with retries
        for attempt in range(3):
            try:
                # Always use the external IP for the download link
                download_link = f"http://{external_ip}/{rrhe_apk}"
                logging.debug(f"APK will be hosted at: {download_link}")
                os.chdir(watch_folder)
                handler = SimpleHTTPRequestHandler
                httpd = ReusableTCPServer(("", port), handler)
                server_thread = threading.Thread(target=httpd.serve_forever)
                server_thread.daemon = True
                server_thread.start()
                logging.debug(f"Serving at: {download_link}")
                current_server = httpd
                return httpd
            except OSError as e:
                logging.error(f"Failed to bind to port {port}. Error: {e}")
                if attempt < 2:
                    logging.debug(f"Retrying to start the server... (Attempt {attempt + 1})")
                    time.sleep(2)  # Wait before retrying
                else:
                    logging.error("Exceeded maximum retry attempts to start the server.")
                    raise
    else:
        logging.debug(f"No {rrhe_apk} found to host.")
        return None

# Function to process the new app-release.apk file
def process_new_apk(file_path):
    global current_server, last_version_folder  # Ensure you're modifying the global variables
    logging.debug(f"Processing new APK: {file_path}")
    
    # Rename the new app-release.apk to RRHE.apk
    logging.debug(f"Renaming {app_release_apk} to {rrhe_apk}...")
    move(file_path, os.path.join(watch_folder, rrhe_apk))
    logging.debug(f"Successfully renamed {app_release_apk} to {rrhe_apk}")

    # Copy the release folder to version control
    logging.debug("Copying release folder to version control...")
    new_version_folder = copy_to_version_control()

    # If a new version folder was created, update the hosting and notify users
    if new_version_folder != last_version_folder:
        last_version_folder = new_version_folder
        logging.debug("New version detected, reloading URL and notifying users...")
        
        # Stop the current hosting server if running
        if current_server:
            logging.debug("Stopping the existing server before rehosting...")
            current_server.shutdown()
            current_server.server_close()
            current_server = None

        # Start hosting the newly renamed RRHE.apk
        logging.debug("Restarting to host the newly renamed RRHE.apk...")
        current_server = start_hosting()

        # Send a notification to all devices subscribed to the topic
        send_fcm_notification(fcm_topic, "New APK Available", "A new version of RRHE is available for download.")

# Monitor the release folder for new APK files
def monitor_release_folder():
    logging.debug("Monitoring release folder for new APK files...")
    while True:
        time.sleep(5)  # Adjust the sleep time as necessary
        if os.path.exists(os.path.join(watch_folder, app_release_apk)):
            logging.debug(f"New APK detected: {app_release_apk}")
            process_new_apk(os.path.join(watch_folder, app_release_apk))

# Monitor the version control folder for new versions
def monitor_version_control_folder():
    global last_version_folder
    logging.debug("Monitoring version control folder for new versions...")
    while True:
        time.sleep(10)  # Adjust the sleep time as necessary
        existing_folders = [
            d for d in os.listdir(version_control_folder)
            if d.startswith('RRHE_')
        ]
        if existing_folders:
            latest_version_folder = max(existing_folders, key=lambda x: float(x.split('_')[1]))
            if latest_version_folder != last_version_folder:
                logging.debug(f"New version folder detected: {latest_version_folder}")
                last_version_folder = latest_version_folder
                # Trigger the notification and hosting update
                current_apk_path = os.path.join(watch_folder, rrhe_apk)
                if os.path.exists(current_apk_path):
                    process_new_apk(current_apk_path)

# Function to send FCM notification to a topic
def send_fcm_notification(topic, title, body):
    credentials = service_account.Credentials.from_service_account_file(
        service_account_file,
        scopes=["https://www.googleapis.com/auth/firebase.messaging"]
    )
    request = Request()
    credentials.refresh(request)
    token = credentials.token

    headers = {
        'Authorization': f'Bearer {token}',
        'Content-Type': 'application/json; UTF-8',
    }
    
    download_link = f"http://{external_ip}/{rrhe_apk}"  # Always use the external IP for the download link

    message = {
        "message": {
            "topic": topic.split('/')[-1],  # Extract the topic name
            "notification": {
                "title": title,
                "body": body
            },
            "data": {
                "download_url": download_link  # Include the download link in the data payload
            }
        }
    }
    
    try:
        response = requests.post(fcm_endpoint, headers=headers, json=message)
        if response.status_code == 200:
            logging.debug("Notification sent successfully.")
        else:
            logging.error(f"Failed to send notification. Status code: {response.status_code}, Response: {response.text}")
    except Exception as e:
        logging.error(f"Error sending FCM notification: {e}")

if __name__ == "__main__":
    logging.debug("Starting the APK monitoring script...")

    try:
        # Start hosting RRHE.apk if it exists
        current_server = start_hosting()

        # Monitor the release folder and version control folder concurrently
        threading.Thread(target=monitor_release_folder, daemon=True).start()
        threading.Thread(target=monitor_version_control_folder, daemon=True).start()

        # Keep the script running
        while True:
            time.sleep(60)

    except Exception as e:
        logging.error(f"An error occurred: {e}")
        if current_server:
            current_server.shutdown()
            current_server.server_close()
        raise