#! /home/mcmeister/myflaskenv/bin/python

from flask import Flask, request, send_from_directory, abort, jsonify
import os
import requests
import logging
from werkzeug.utils import secure_filename

app = Flask(__name__)
UPLOAD_FOLDER = '/home/mcmeister/Documents/GDrive_Photos/'
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER
app.config['MAX_CONTENT_LENGTH'] = 1024 * 1024 * 100  # Limit upload size to 100MB

DB_SERVER_URL = 'http://localhost:5000'  # Update this with your actual DB server address and port

# Setup logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

def rename_file_if_exists(file_path):
    """Renames the existing file by appending '_old' or a numeric suffix to avoid conflicts."""
    name, ext = os.path.splitext(file_path)
    old_file_path = f"{name}_old{ext}"

    # Check if the _old file already exists
    counter = 1
    while os.path.exists(old_file_path):
        old_file_path = f"{name}_old_{counter}{ext}"
        counter += 1

    logger.info(f"Renaming existing file from {file_path} to {old_file_path}")
    os.rename(file_path, old_file_path)

@app.route('/upload', methods=['POST'])
def upload_file():
    logger.info("Received file upload request")
    logger.info(f"Request path: {request.path}")
    logger.info(f"Request method: {request.method}")
    logger.info(f"Request headers: {request.headers}")

    if 'file' not in request.files:
        logger.error("No file part in the request")
        return jsonify({"error": "No file part"}), 400
    
    file = request.files['file']
    if file.filename == '':
        logger.error("No selected file")
        return jsonify({"error": "No selected file"}), 400
    
    filename = secure_filename(file.filename)
    file_path = os.path.join(app.config['UPLOAD_FOLDER'], filename)
    
    # Additional logging to confirm file path
    logger.info(f"Saving file to: {file_path}")
    
    # Check if a file with the same name already exists
    if os.path.exists(file_path):
        logger.warning(f"File {filename} already exists, renaming it")
        rename_file_if_exists(file_path)
    
    # Save the new file
    try:
        file.save(file_path)
        logger.info(f"File {filename} saved successfully at {file_path}")
    except Exception as e:
        logger.error(f"Failed to save file {filename}: {str(e)}")
        return jsonify({"error": "Failed to save file"}), 500

    # Extract stockID and photoIndex from filename
    try:
        stockID, photoIndex = filename.split('_')[0], filename.split('_')[1].split('.')[0]
        photoUrl = f"http://{request.host}/{filename}"
    except Exception as e:
        logger.error(f"Error extracting stockID and photoIndex from filename {filename}: {str(e)}")
        return jsonify({"error": "Invalid filename format"}), 400

    # Send request to the DB server to update the corresponding Photo column
    try:
        response = requests.post(f"{DB_SERVER_URL}/update_photo_column", json={
            'stockID': stockID,
            'photoIndex': photoIndex,
            'photoUrl': photoUrl
        }, timeout=10)
        if response.status_code == 200:
            logger.info(f"DB updated successfully for {filename}")
            return jsonify({"message": "File uploaded and saved successfully, DB updated"}), 200
        else:
            logger.error(f"DB update failed for {filename}: {response.text}")
            return jsonify({"error": f"File uploaded but DB update failed: {response.text}"}), 500
    except requests.exceptions.RequestException as e:
        logger.error(f"Error updating DB for {filename}: {str(e)}")
        return jsonify({"error": "DB update failed"}), 500

@app.route('/delete_photo', methods=['POST'])
def delete_photo():
    data = request.json
    stockID = data.get('stockID')
    photoIndex = data.get('photoIndex')

    if not stockID or not photoIndex:
        logger.error("Missing stockID or photoIndex in request")
        return jsonify({"error": "Missing stockID or photoIndex"}), 400

    filename = f"{stockID}_{photoIndex}.jpg"
    file_path = os.path.join(app.config['UPLOAD_FOLDER'], filename)

    # Delete the file if it exists
    if os.path.exists(file_path):
        try:
            os.remove(file_path)
            logger.info(f"Photo {filename} deleted successfully")
        except Exception as e:
            logger.error(f"Failed to delete photo {filename}: {str(e)}")
            return jsonify({"error": f"Failed to delete photo: {str(e)}"}), 500
    else:
        logger.warning(f"Photo {filename} not found, skipping deletion")

    # Update the database to set the photo column to NULL
    try:
        response = requests.post(f"{DB_SERVER_URL}/update_photo_column", json={
            'stockID': stockID,
            'photoIndex': photoIndex,
            'photoUrl': None  # Set to NULL in the database
        }, timeout=10)
        if response.status_code == 200:
            logger.info(f"DB updated successfully for deletion of {filename}")
            return jsonify({"message": "Photo deleted and DB updated successfully"}), 200
        else:
            logger.error(f"DB update failed for deletion of {filename}: {response.text}")
            return jsonify({"error": f"Photo deleted but DB update failed: {response.text}"}), 500
    except requests.exceptions.RequestException as e:
        logger.error(f"Error updating DB for deletion of {filename}: {str(e)}")
        return jsonify({"error": "DB update failed"}), 500

@app.route('/<filename>', methods=['GET'])
def get_file(filename):
    try:
        # Strip query parameters (if any)
        filename = filename.split("?")[0]
        logger.info(f"Fetching file: {filename}")
        return send_from_directory(app.config['UPLOAD_FOLDER'], filename)
    except FileNotFoundError:
        logger.error(f"File not found: {filename}")
        abort(404, description="Resource not found")
    except Exception as e:
        logger.error(f"Error fetching file {filename}: {str(e)}")
        abort(500, description="Internal server error")

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8000, threaded=True)
