import csv
import argparse

def delete_row_with_threshold(data_file, output_file, threshold=0.1):
    with open(data_file) as csv_file:
        csv_reader = csv.reader(csv_file, delimiter=',')
        header = next(csv_reader)

        max_missing_values = len(header) * threshold

        valid_rows = []

        for row in csv_reader:
            missing_values = row.count('')
            if missing_values <= max_missing_values:
                valid_rows.append(row)

    with open(output_file, 'w', newline='') as csvfile:
        csv_writer = csv.writer(csvfile)

        csv_writer.writerow(header)

        csv_writer.writerows(valid_rows)

# Create an argument parser
parser = argparse.ArgumentParser(description='Delete rows with a high percentage of missing values from a CSV file.')

# Add an argument for the data file
parser.add_argument('data_file', type=str, help='Name of the data CSV file')

# Add an argument for the threshold
parser.add_argument('--threshold', type=float, default=0.1, help='Threshold for the maximum allowed percentage of missing values per row')

# Add an argument for the output file
parser.add_argument('--output_file', type=str, default='output_dataset_4.csv', help='Name of the output CSV file')

# Parse the command-line arguments
args = parser.parse_args()

# Call the delete_row_with_threshold function with the provided arguments
delete_row_with_threshold(args.data_file, args.output_file, args.threshold)