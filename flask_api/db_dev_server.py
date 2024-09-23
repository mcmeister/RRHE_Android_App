#! /home/mcmeister/myflaskenv/bin/python

from flask import Flask, Response, request, jsonify
import json
import pymysql
from decimal import Decimal
from datetime import datetime
import re
import logging
import hashlib

app = Flask(__name__)
logging.basicConfig(level=logging.DEBUG)

def get_db_connection():
    return pymysql.connect(
        host='localhost',
        user='root',
        password='online12',
        database='rrhe_db_dev',
        charset='utf8mb4',
        use_unicode=True
    )

def hash_password(password):
    return hashlib.sha256(password.encode()).hexdigest()

@app.route('/login', methods=['POST'])
def login():
    data = request.json
    user_name = data.get('user_name')
    password = data.get('password')

    if not user_name or not password:
        return jsonify({'error': 'Username and password are required'}), 400

    connection = get_db_connection()
    try:
        with connection.cursor() as cursor:
            cursor.execute("SELECT user_id, user_name, lang_id FROM users WHERE user_name = %s AND password = %s",
                           (user_name, hash_password(password)))
            user = cursor.fetchone()

            if user:
                user_data = {
                    'user_id': user[0],
                    'user_name': user[1],
                    'lang_id': user[2]
                }
                return jsonify(user_data), 200
            else:
                return jsonify({'error': 'Invalid credentials'}), 401
    except Exception as e:
        app.logger.error(f"Error during login: {str(e)}")
        return jsonify({'error': str(e)}), 500
    finally:
        connection.close()

@app.route('/update_fcm_token', methods=['POST'])
def update_fcm_token():
    data = request.json
    app.logger.debug(f"Received data: {data}")
    
    user_name = data.get('user_name')
    fcm_token = data.get('fcm_token')

    if not user_name or not fcm_token:
        app.logger.error(f"Missing user_name or fcm_token. user_name: {user_name}, fcm_token: {fcm_token}")
        return jsonify({'error': 'Username and FCM token are required'}), 400

    connection = get_db_connection()
    try:
        with connection.cursor() as cursor:
            cursor.execute("UPDATE users SET fcm_token = %s WHERE user_name = %s", (fcm_token, user_name))
            connection.commit()
            return jsonify({'message': 'FCM token updated successfully'}), 200
    except Exception as e:
        connection.rollback()
        app.logger.error(f"Error updating FCM token: {str(e)}")
        return jsonify({'error': str(e)}), 500
    finally:
        connection.close()

def escape_special_characters(data):
    if isinstance(data, str):
        return data.encode('unicode_escape').decode('utf-8')
    return data

def validate_data(data):
    try:
        json.dumps(data)
        return True
    except (TypeError, ValueError) as e:
        app.logger.error(f"Data validation error: {str(e)} - Data: {data}")
        return False

def sanitize_data(data):
    if isinstance(data, str):
        data = re.sub(r'[^\x20-\x7E]', '', data)
        return data
    return data

def stream_rrhe_changes(last_sync_time):
    connection = get_db_connection()
    cursor = connection.cursor()
    cursor.execute("""
        SELECT StockID, M_ID, F_ID, Family, Species, Subspecies, ThaiName, NameConcat, TableName, StockQty, StockPrice, 
               Mother, Website, PlantedStart, PlantedEnd, PollinateDate, SeedsPlanted, SeedsHarvest, PlantStatus, Stamp, PlantDescription, 
               StatusNote, PurchasePrice, TotalValue, USD, EUR, Photo1, Photo2, Photo3, Photo4, PhotoLink1, PhotoLink2, 
               PhotoLink3, PhotoLink4, AddedBy, LastEditedBy, Weight, Grams, TraySize, TrayQty, Variegated
        FROM stock 
        WHERE Stamp > %s
    """, (last_sync_time,))
    
    def generate():
        yield '['
        first = True
        for row in cursor:
            if not first:
                yield ','
            first = False
            try:
                json_data = {
                    'StockID': row[0],
                    'M_ID': row[1],
                    'F_ID': row[2],
                    'Family': row[3],
                    'Species': row[4],
                    'Subspecies': row[5] if row[5] is not None else "",
                    'ThaiName': sanitize_data(escape_special_characters(row[6])),
                    'NameConcat': sanitize_data(escape_special_characters(row[7])),
                    'TableName': sanitize_data(escape_special_characters(row[8])) if row[8] is not None else None,
                    'StockQty': row[9],
                    'StockPrice': float(row[10]) if row[10] is not None else None,
                    'Mother': row[11],
                    'Website': row[12],
                    'PlantedStart': row[13].strftime('%Y-%m-%d') if row[13] is not None else None,
                    'PlantedEnd': row[14].strftime('%Y-%m-%d') if row[14] is not None else None,
                    'PollinateDate': row[15].strftime('%Y-%m-%d') if row[15] is not None else None,
                    'SeedsPlanted': row[16].strftime('%Y-%m-%d') if row[16] is not None else None,
                    'SeedsHarvest': row[17].strftime('%Y-%m-%d') if row[17] is not None else None,
                    'PlantStatus': sanitize_data(escape_special_characters(row[18])),
                    'Stamp': row[19].strftime('%Y-%m-%d %H:%M:%S'),
                    'PlantDescription': sanitize_data(escape_special_characters(row[20])) if row[20] is not None else "",
                    'StatusNote': sanitize_data(escape_special_characters(row[21])) if row[21] is not None else "",
                    'PurchasePrice': float(row[22]) if row[22] is not None else None,
                    'TotalValue': float(row[23]) if row[23] is not None else None,
                    'USD': float(row[24]) if row[24] is not None else None,
                    'EUR': float(row[25]) if row[25] is not None else None,
                    'Photo1': sanitize_data(escape_special_characters(row[26])) if row[26] is not None else None,
                    'Photo2': sanitize_data(escape_special_characters(row[27])) if row[27] is not None else None,
                    'Photo3': sanitize_data(escape_special_characters(row[28])) if row[28] is not None else None,
                    'Photo4': sanitize_data(escape_special_characters(row[29])) if row[29] is not None else None,
                    'PhotoLink1': sanitize_data(escape_special_characters(row[30])) if row[30] is not None else None,
                    'PhotoLink2': sanitize_data(escape_special_characters(row[31])) if row[31] is not None else None,
                    'PhotoLink3': sanitize_data(escape_special_characters(row[32])) if row[32] is not None else None,
                    'PhotoLink4': sanitize_data(escape_special_characters(row[33])) if row[33] is not None else None,
                    'AddedBy': sanitize_data(escape_special_characters(row[34])) if row[34] is not None else None,
                    'LastEditedBy': sanitize_data(escape_special_characters(row[35])),
                    'Weight': row[36] if row[36] is not None else None,
                    'Grams': row[37] if row[37] is not None else None,
                    'TraySize': sanitize_data(escape_special_characters(row[38])) if row[38] is not None else None,
                    'TrayQty': row[39],
                    'Variegated': row[40]
                }
                if not validate_data(json_data):
                    app.logger.error(f"Invalid data: {json_data}")
                yield json.dumps(json_data)
            except Exception as e:
                app.logger.error(f"Error serializing row: {row}, Error: {str(e)}")
                app.logger.error(f"Row data causing error: {json_data}")
        yield ']'
    
    cursor.close()
    connection.close()
    
    return Response(generate(), content_type='application/json; charset=utf-8')

@app.route('/rrhe/changes', methods=['GET'])
def get_rrhe_changes():
    last_sync_time = request.args.get('last_sync_time')
    if last_sync_time is None:
        return jsonify({'error': 'Missing last_sync_time parameter'}), 400
    return stream_rrhe_changes(last_sync_time)

@app.route('/rrhe/update', methods=['POST'])
def update_rrhe():
    data = request.json
    app.logger.debug(f"Received update request: {data}")
    if not data:
        return jsonify({'error': 'No input data provided'}), 400

    stock_id = data.get('StockID')
    stamp_str = data.get('Stamp')
    if not stock_id or not stamp_str:
        return jsonify({'error': 'StockID and Stamp are required'}), 400

    try:
        incoming_stamp = datetime.strptime(stamp_str, '%Y-%m-%d %H:%M:%S')
    except ValueError:
        return jsonify({'error': 'Invalid Stamp format. Expected yyyy-MM-dd HH:mm:ss'}), 400

    connection = get_db_connection()
    try:
        with connection.cursor() as cursor:
            cursor.execute("SELECT Stamp FROM stock WHERE StockID = %s", (stock_id,))
            result = cursor.fetchone()

            if result:
                current_stamp = result[0]
                if isinstance(current_stamp, str):
                    current_stamp = datetime.strptime(current_stamp, '%Y-%m-%d %H:%M:%S')

                if current_stamp >= incoming_stamp:
                    return jsonify({'error': 'A more recent update already exists'}), 409

            # Proceed with the update if the current stamp is older than the new stamp
            cursor.execute("""
                UPDATE stock
                SET Family=%s, Species=%s, Subspecies=%s, NameConcat=%s, StockQty=%s, StockPrice=%s, PurchasePrice=%s, PlantDescription=%s, ThaiName=%s, 
                    M_ID=%s, F_ID=%s, PlantStatus=%s, StatusNote=%s, Mother=%s, Website=%s, TableName=%s, TraySize=%s, Grams=%s, 
                    TotalValue=%s, USD=%s, EUR=%s, PlantedStart=%s, PlantedEnd=%s, PollinateDate=%s, SeedsPlanted=%s, SeedsHarvest=%s,
                    PhotoLink1=%s, PhotoLink2=%s, PhotoLink3=%s, PhotoLink4=%s, LastEditedBy=%s, AddedBy=%s, Stamp=%s
                WHERE StockID=%s
            """, (data.get('Family'), data.get('Species'), data.get('Subspecies'), data.get('NameConcat'),
                  data.get('StockQty'), data.get('StockPrice'), data.get('PurchasePrice'), data.get('PlantDescription'),
                  data.get('ThaiName'), data.get('M_ID'), data.get('F_ID'), data.get('PlantStatus'), data.get('StatusNote'),
                  data.get('Mother'), data.get('Website'), data.get('TableName'), data.get('TraySize'), data.get('Grams'),
                  data.get('TotalValue'), data.get('USD'), data.get('EUR'), data.get('PlantedStart'), data.get('PlantedEnd'),
                  data.get('PollinateDate'), data.get('SeedsPlanted'), data.get('SeedsHarvest'), data.get('PhotoLink1'),
                  data.get('PhotoLink2'), data.get('PhotoLink3'), data.get('PhotoLink4'), data.get('LastEditedBy'), data.get('AddedBy'), incoming_stamp, stock_id))

            connection.commit()
            app.logger.debug(f"Plant updated successfully for StockID: {stock_id}")
            return jsonify({'message': 'Plant updated successfully'}), 200
    except Exception as e:
        connection.rollback()
        app.logger.error(f"Error updating plant: {str(e)}")
        return jsonify({'error': str(e)}), 500
    finally:
        connection.close()

def handle_value(value):
    if isinstance(value, str):
        if value == "":
            return None
        # Only add quotes to string fields that are not already in quotes
        return value.replace("'", "''")
    return value

@app.route('/rrhe/insert', methods=['POST'])
def insert_new_plant():
    data = request.json
    app.logger.debug(f"Received insert request: {data}")
    if not data:
        return jsonify({'error': 'No input data provided'}), 400

    connection = get_db_connection()
    try:
        with connection.cursor() as cursor:
            # Start the transaction
            cursor.execute("BEGIN;")
            
            # Lock the table by selecting MAX(StockID) with FOR UPDATE to prevent concurrent inserts
            cursor.execute("SELECT MAX(StockID) FROM stock FOR UPDATE;")
            max_stock_id = cursor.fetchone()[0]

            new_stock_id = (max_stock_id or 0) + 1  # Generate the next StockID
            app.logger.debug(f"Generated new StockID: {new_stock_id}")

            # Convert empty strings to None, and properly handle strings and None values
            values = tuple(handle_value(val) for val in (
                new_stock_id,  # Use new generated StockID
                data.get('M_ID'),
                data.get('F_ID'),
                data.get('Family'),
                data.get('Species'),
                data.get('Subspecies'),
                data.get('ThaiName'),
                data.get('NameConcat'),
                data.get('TableName'),
                data.get('StockQty'),
                data.get('StockPrice'),
                1 if data.get('Mother') else 0,
                1 if data.get('Website') else 0,
                data.get('PlantedStart'),
                data.get('PlantedEnd'),
                data.get('PollinateDate'),
                data.get('SeedsPlanted'),
                data.get('SeedsHarvest'),
                data.get('PlantStatus'),
                data.get('PlantDescription'),
                data.get('StatusNote'),
                data.get('PurchasePrice'),
                data.get('TotalValue'),
                data.get('USD'),
                data.get('EUR'),
                data.get('Photo1'),
                data.get('Photo2'),
                data.get('Photo3'),
                data.get('Photo4'),
                data.get('PhotoLink1'),
                data.get('PhotoLink2'),
                data.get('PhotoLink3'),
                data.get('PhotoLink4'),
                data.get('AddedBy'),
                data.get('LastEditedBy'),
                data.get('Weight'),
                data.get('Grams'),
                data.get('TraySize'),
                data.get('TrayQty'),
                1 if data.get('Variegated') else 0,
                data.get('Stamp')
            ))

            app.logger.debug(f"Prepared values: {values}")

            # Execute the insert query
            insert_query = """
                INSERT INTO stock (
                    StockID, M_ID, F_ID, Family, Species, Subspecies, ThaiName, NameConcat, TableName, StockQty, StockPrice,
                    Mother, Website, PlantedStart, PlantedEnd, PollinateDate, SeedsPlanted, SeedsHarvest, PlantStatus,
                    PlantDescription, StatusNote, PurchasePrice, TotalValue, USD, EUR, Photo1, Photo2, Photo3, Photo4,
                    PhotoLink1, PhotoLink2, PhotoLink3, PhotoLink4, AddedBy, LastEditedBy, Weight, Grams, TraySize, TrayQty, Variegated, Stamp
                ) VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)
            """

            cursor.execute(insert_query, values)
            connection.commit()  # Commit the transaction

            app.logger.debug(f"New plant inserted successfully with StockID: {new_stock_id}")

            # Fetch the inserted plant's data to return
            cursor.execute("SELECT * FROM stock WHERE StockID = %s", (new_stock_id,))
            updated_plant = cursor.fetchone()

            if updated_plant is None:
                raise ValueError(f"Failed to retrieve the inserted plant with StockID: {new_stock_id}")

            app.logger.debug(f"Returning updated plant data: {updated_plant}")

            # Convert the result into a dictionary
            columns = [desc[0] for desc in cursor.description]
            result = dict(zip(columns, updated_plant))

            return jsonify(result), 201
    except Exception as e:
        connection.rollback()
        app.logger.error(f"Error inserting new plant: {str(e)}")
        return jsonify({'error': str(e)}), 500
    finally:
        connection.close()

@app.route('/update_photo_column', methods=['POST'])
def update_photo_column():
    data = request.json
    stock_id = data.get('stockID')
    photo_index = data.get('photoIndex')
    photo_url = data.get('photoUrl')

    if not stock_id or not photo_index:
        return jsonify({'error': 'StockID and photoIndex are required'}), 400

    photo_column = f'Photo{photo_index}'

    connection = get_db_connection()
    cursor = connection.cursor()

    try:
        cursor.execute(f"""
            UPDATE stock
            SET {photo_column} = %s
            WHERE StockID = %s
        """, (photo_url, stock_id))
        connection.commit()
        app.logger.debug(f"Updated {photo_column} for StockID: {stock_id} with URL: {photo_url}")
    except Exception as e:
        connection.rollback()
        cursor.close()
        connection.close()
        app.logger.error(f"Error updating {photo_column} for StockID {stock_id}: {str(e)}")
        return jsonify({'error': str(e)}), 500

    cursor.close()
    connection.close()
    return jsonify({'message': f'{photo_column} updated successfully for StockID {stock_id}'}), 200

@app.route('/rrhe', methods=['GET'])
def get_rrhe():
    return stream_rrhe_changes("1970-01-01T00:00:00")

@app.route('/stats', methods=['GET'])
def get_stats():
    connection = get_db_connection()
    cursor = connection.cursor()
    cursor.execute("""
        SELECT Stamp, TotalRows, TotalPlants, TotalNonM, TotalM, NonMValue, MValue, TotalValue, WebPlants, WebQty, WebValue, USD, EUR 
        FROM stats
    """)

    rows = cursor.fetchall()
    stats = []
    for row in rows:
        stats.append({
            'Stamp': row[0],
            'TotalRows': row[1],
            'TotalPlants': row[2],
            'TotalNonM': row[3],
            'TotalM': row[4],
            'NonMValue': float(row[5]) if row[5] is not None else None,
            'MValue': float(row[6]) if row[6] is not None else None,
            'TotalValue': float(row[7]) if row[7] is not None else None,
            'WebPlants': row[8],
            'WebQty': row[9],
            'WebValue': float(row[10]) if row[10] is not None else None,
            'USD': float(row[11]) if row[11] is not None else None,
            'EUR': float(row[12]) if row[12] is not None else None
        })
    cursor.close()
    connection.close()
    if stats:
        return jsonify(stats), 200
    else:
        return jsonify({'error': 'No stats found'}), 404

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5001, debug=True)