from flask import Flask, Response, request, jsonify
import pymysql
import json
from decimal import Decimal
from datetime import datetime

app = Flask(__name__)

def get_db_connection():
    return pymysql.connect(host='localhost',
                           user='mcmeister',
                           password='online',
                           database='rrhe_db')

class CustomJSONEncoder(json.JSONEncoder):
    def default(self, obj):
        if isinstance(obj, Decimal):
            return float(obj)
        if isinstance(obj, datetime):
            return obj.strftime('%Y-%m-%dT%H:%M:%S')
        return super(CustomJSONEncoder, self).default(obj)

def stream_rrhe_changes(last_sync_time):
    connection = get_db_connection()
    cursor = connection.cursor()
    cursor.execute("""
        SELECT StockID, NameConcat, StockQty, PhotoLink1, Family, Species, Subspecies, StockPrice, PlantDescription, Stamp 
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
            yield json.dumps({
                'StockID': row[0], 'NameConcat': row[1], 'StockQty': row[2], 'PhotoLink1': row[3],
                'Family': row[4], 'Species': row[5], 'Subspecies': row[6] if row[6] is not None else "",
                'StockPrice': row[7], 'PlantDescription': row[8] if row[8] is not None else "",
                'Stamp': row[9].strftime('%Y-%m-%dT%H:%M:%S')
            }, cls=CustomJSONEncoder)
        yield ']'
    
    cursor.close()
    connection.close()
    
    return Response(generate(), content_type='application/json')

@app.route('/rrhe/changes', methods=['GET'])
def get_rrhe_changes():
    last_sync_time = request.args.get('last_sync_time')
    if last_sync_time is None:
        return jsonify({'error': 'Missing last_sync_time parameter'}), 400
    return stream_rrhe_changes(last_sync_time)

@app.route('/rrhe/update', methods=['POST'])
def update_rrhe():
    data = request.json
    if not data:
        return jsonify({'error': 'No input data provided'}), 400
    
    stock_id = data.get('StockID')
    family = data.get('Family')
    species = data.get('Species')
    subspecies = data.get('Subspecies')
    stock_qty = data.get('StockQty')
    stock_price = data.get('StockPrice')
    plant_description = data.get('PlantDescription')
    stamp = datetime.now()
    
    connection = get_db_connection()
    cursor = connection.cursor()
    
    try:
        cursor.execute("""
            UPDATE stock
            SET Family=%s, Species=%s, Subspecies=%s, StockQty=%s, StockPrice=%s, PlantDescription=%s, Stamp=%s
            WHERE StockID=%s
        """, (family, species, subspecies, stock_qty, stock_price, plant_description, stamp, stock_id))
        connection.commit()
    except Exception as e:
        connection.rollback()
        cursor.close()
        connection.close()
        return jsonify({'error': str(e)}), 500
    
    cursor.close()
    connection.close()
    return jsonify({'message': 'Plant updated successfully'}), 200

@app.route('/rrhe', methods=['GET'])
def get_rrhe():
    return stream_rrhe_changes("1970-01-01T00:00:00")

@app.route('/stats', methods=['GET'])
def get_stats():
    connection = get_db_connection()
    cursor = connection.cursor()
    cursor.execute("""
        SELECT TotalRows, TotalPlants, TotalNonM, TotalM, NonMValue, MValue, TotalValue, WebPlants, WebQty, WebValue, USD, EUR 
        FROM stats
    """)
    
    row = cursor.fetchone()
    if row:
        stats = {
            'TotalRows': row[0], 'TotalPlants': row[1], 'TotalNonM': row[2], 'TotalM': row[3],
            'NonMValue': row[4], 'MValue': row[5], 'TotalValue': row[6],
            'WebPlants': row[7], 'WebQty': row[8], 'WebValue': row[9],
            'USD': row[10], 'EUR': row[11]
        }
        cursor.close()
        connection.close()
        return jsonify(stats), 200
    else:
        cursor.close()
        connection.close()
        return jsonify({'error': 'No stats found'}), 404

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
