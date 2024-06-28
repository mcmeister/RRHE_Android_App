import csv
from datetime import datetime

input_file = 'C:/ProgramData/MySQL/MySQL Server 8.0/Uploads/RRHE_DB_MySQL.csv'
output_file = 'C:/ProgramData/MySQL/MySQL Server 8.0/Uploads/RRHE_DB_MySQL_preprocessed.csv'

# Columns that need boolean conversion
boolean_columns = ['Mother', 'Website', 'Variegated']
# Columns that need datetime conversion
datetime_columns = ['Planted', 'Pollinate Date', 'Seeds Planted', 'Seeds Harvest', 'Stamp']

def clean_value(value):
    """Convert boolean values to integers."""
    value = value.strip().upper()  # Strip any leading/trailing whitespace and convert to uppercase
    if value == 'TRUE':
        return '1'
    if value == 'FALSE' or value == '':
        return '0'
    return value

def convert_datetime(value):
    """Convert datetime values to MySQL format."""
    value = value.strip()  # Strip any leading/trailing whitespace
    if value == '':
        return ''  # Leave empty for MySQL NULL conversion
    try:
        # Convert the value to a datetime object and then format it
        dt = datetime.strptime(value, '%m/%d/%Y')
        return dt.strftime('%Y-%m-%d')
    except ValueError:
        try:
            # Convert the value to a datetime object and then format it (including time if present)
            dt = datetime.strptime(value, '%m/%d/%Y %H:%M:%S')
            return dt.strftime('%Y-%m-%d %H:%M:%S')
        except ValueError:
            return value

with open(input_file, 'r', encoding='utf-8') as infile, open(output_file, 'w', newline='', encoding='utf-8') as outfile:
    reader = csv.DictReader(infile)
    headers = reader.fieldnames
    
    writer = csv.DictWriter(outfile, fieldnames=headers)
    writer.writeheader()
    
    row_count = 0
    for row in reader:
        if row_count >= 2459:
            break
        for col in boolean_columns:
            if col in row:
                row[col] = clean_value(row[col])
        for col in datetime_columns:
            if col in row:
                row[col] = convert_datetime(row[col])
        writer.writerow(row)
        row_count += 1
