import csv
import argparse

def delete_duplicate(data_file, output_file):
    with open(data_file) as csv_file:
        csv_reader = csv.reader(csv_file, delimiter=',')
        header = next(csv_reader)
        unique_rows = set()
        unique_data = [header]
        for row in csv_reader:
            row_tuple = tuple(row)
            if row_tuple not in unique_rows:
                unique_rows.add(row_tuple)
                unique_data.append(list(row))
    with open(output_file, 'w', newline='') as csvfile:
        csv_writer = csv.writer(csvfile)
        csv_writer.writerows(unique_data)

# Create an argument parser
parser = argparse.ArgumentParser(description='Delete duplicate rows from a CSV file.')

# Add an argument for the data file
parser.add_argument('data_file', type=str, help='Name of the data CSV file')

# Add an argument for the output file
parser.add_argument('--output_file', type=str, default='output_dataset_6.csv', help='Name of the output CSV file')

# Parse the command-line arguments
args = parser.parse_args()

# Call the delete_duplicate function with the provided arguments
delete_duplicate(args.data_file, args.output_file)