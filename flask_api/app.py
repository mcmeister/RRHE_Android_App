from flask import Flask, jsonify, request
import pymysql

app = Flask(__name__)

def get_db_connection():
    return pymysql.connect(host='localhost',
                           user='mcmeister',
                           password='online',
                           database='rrhe_db')

@app.route('/rrhe', methods=['GET'])
def get_rrhe():
    connection = get_db_connection()
    cursor = connection.cursor()
    cursor.execute("SELECT StockID, NameConcat, StockQty, PhotoLink1 FROM stock")
    rows = cursor.fetchall()
    cursor.close()
    connection.close()

    rrhe = [{'StockID': row[0], 'NameConcat': row[1], 'StockQty': row[2], 'PhotoLink1': row[3]} for row in rows]
    return jsonify(rrhe)

@app.route('/rrhe/update', methods=['POST'])
def update_rrhe():
    data = request.json
    StockID = data['StockID']
    Family = data['Family']
    Species = data['Species']
    Subspecies = data['Subspecies']
    StockQty = data['StockQty']
    StockPrice = data['StockPrice']
    PlantDescription = data['PlantDescription']
    
    connection = get_db_connection()
    cursor = connection.cursor()
    cursor.execute("""
        UPDATE stock 
        SET Family=%s, Species=%s, Subspecies=%s, StockQty=%s, StockPrice=%s, PlantDescription=%s
        WHERE StockID=%s
    """, (Family, Species, Subspecies, StockQty, StockPrice, PlantDescription, StockID))
    connection.commit()
    cursor.close()
    connection.close()
    
    return jsonify({'message': 'Plant updated successfully'})

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
