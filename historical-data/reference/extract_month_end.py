#!/usr/bin/env python3
"""
Script to extract month-end dates and adjusted close prices from S&P 500 historical data.
Reads kaggle-SPX-1927-2020-daily.csv and outputs a new CSV with only month-end data.
Rounds the adjusted close values to 2 decimal places.
"""

import csv
import math
from datetime import datetime
from collections import defaultdict

def is_month_end(date_str, next_date_str=None):
    """
    Check if a date is the last day of the month.
    We determine this by checking if the next date is in a different month.
    """
    try:
        current_date = datetime.strptime(date_str, '%m/%d/%Y')
        if next_date_str:
            next_date = datetime.strptime(next_date_str, '%m/%d/%Y')
            # If next date is in a different month or year, current date is month-end
            return (current_date.month != next_date.month) or (current_date.year != next_date.year)
        else:
            # Last row in file - treat as month-end if it's the last available date
            return True
    except ValueError:
        return False

def extract_month_end_data(input_file, output_file):
    """
    Read input CSV and write month-end dates with adjusted close to output CSV.
    """
    month_end_data = []

    with open(input_file, 'r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        rows = list(reader)

        for i, row in enumerate(rows):
            date_str = row['Date']
            adj_close = row['Adj Close']

            # Check if this is month-end by looking at next row
            next_date_str = rows[i + 1]['Date'] if i + 1 < len(rows) else None

            if is_month_end(date_str, next_date_str):
                # Round to 2 decimal places
                adj_close_rounded = round(float(adj_close), 2)
                month_end_data.append({
                    'Date': date_str,
                    'Adj Close': adj_close_rounded
                })

    # Write output CSV
    with open(output_file, 'w', newline='', encoding='utf-8') as f:
        fieldnames = ['Date', 'Adj Close']
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(month_end_data)

    print(f"Processed {len(rows)} daily records")
    print(f"Extracted {len(month_end_data)} month-end records")
    print(f"Output written to: {output_file}")

if __name__ == '__main__':
    input_file = 'kaggle-SPX-1927-2020-daily.csv'
    output_file = 'kaggle-SPX-1927-2020-monthly.csv'

    extract_month_end_data(input_file, output_file)

