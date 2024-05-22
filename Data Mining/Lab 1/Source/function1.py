import csv
import argparse

def extract_col_missing(file_name):
    with open(file_name) as csv_file:
        csv_reader = csv.reader(csv_file, delimiter=',')
        header = next(csv_reader)
        columns_with_missing_values = []

        for row in csv_reader:
            for idx, value in enumerate(row):
                if value.strip() == '':
                    columns_with_missing_values.append(idx)

        columns_with_missing_values = set(columns_with_missing_values)

        for idx in columns_with_missing_values:
            print(f"Column '{header[idx]}' has missing values.")

# Create an argument parser
parser = argparse.ArgumentParser(description='Extract columns with missing values from a CSV file.')

# Add an argument for the file name
parser.add_argument('file_name', type=str, help='Name of the CSV file')

# Parse the command-line arguments
args = parser.parse_args()

# Call the extract_col_missing function with the provided file name
extract_col_missing(args.file_name)