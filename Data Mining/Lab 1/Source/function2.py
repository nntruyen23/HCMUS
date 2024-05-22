import csv
import argparse

def count_lines_missing(file_name):
    with open(file_name) as csv_file:
        csv_reader = csv.reader(csv_file, delimiter=',')
        header = next(csv_reader)
        lines_with_missing_data = 0

        for row in csv_reader:
            has_missing_data = False
            for value in row:
                if value.strip() == '':
                    has_missing_data = True
                    break
            if has_missing_data:
                lines_with_missing_data += 1
        print(f"Number of lines with missing data: {lines_with_missing_data}")

# Create an argument parser
parser = argparse.ArgumentParser(description='Count the number of lines with missing data in a CSV file.')

# Add an argument for the file name
parser.add_argument('file_name', type=str, help='Name of the CSV file')

# Parse the command-line arguments
args = parser.parse_args()

# Call the count_lines_missing function with the provided file name
count_lines_missing(args.file_name)